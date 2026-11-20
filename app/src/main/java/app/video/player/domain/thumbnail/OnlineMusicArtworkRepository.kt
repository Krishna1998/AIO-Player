/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.domain.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import app.video.player.domain.media.model.Video
import app.video.player.network.awaitResponse
import app.video.player.preferences.BrowserPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "OnlineMusicArtworkRepo"
private const val ITUNES_SEARCH_URL = "https://itunes.apple.com/search"
private const val DEEZER_SEARCH_URL = "https://api.deezer.com/search"
private const val MUSICBRAINZ_SEARCH_URL = "https://musicbrainz.org/ws/2/recording"
private const val ARTWORK_FETCH_TIMEOUT_MS = 6000L
private const val NEGATIVE_CACHE_TTL_MS = 15 * 60 * 1000L // 15 mins

@Serializable
private data class ITunesSong(
  val trackName: String? = null,
  val artistName: String? = null,
  val collectionName: String? = null,
  val artworkUrl100: String? = null,
)

@Serializable
private data class ITunesSearchResponse(
  val resultCount: Int = 0,
  val results: List<ITunesSong> = emptyList(),
)

@Serializable
private data class DeezerAlbum(
  val id: Long? = null,
  val title: String? = null,
  val cover_medium: String? = null,
  val cover_big: String? = null,
  val cover_xl: String? = null,
)

@Serializable
private data class DeezerTrack(
  val id: Long? = null,
  val title: String? = null,
  val album: DeezerAlbum? = null,
)

@Serializable
private data class DeezerSearchResponse(
  val data: List<DeezerTrack> = emptyList(),
)

@Serializable
private data class MusicBrainzRelease(
  val id: String? = null,
  val title: String? = null,
)

@Serializable
private data class MusicBrainzRecording(
  val id: String? = null,
  val title: String? = null,
  val releases: List<MusicBrainzRelease> = emptyList(),
)

@Serializable
private data class MusicBrainzRecordingResponse(
  val recordings: List<MusicBrainzRecording> = emptyList(),
)

object MusicTrackInfoParser {
  private val NOISE_REGEX = Regex(
    "(?i)\\s*[\\[(](?:official\\s*(?:music\\s*)?video|official\\s*audio|lyric\\s*video|audio|video|hq|hd|4k|remastered(?:\\s*\\d{4})?|remaster|bonus\\s*track|live|320kbps|flac)[\\])]"
  )
  private val TRACK_NUMBER_REGEX = Regex("^\\d{1,3}[.\\-\\s]+")
  private val EXTENSION_REGEX = Regex("\\.[a-zA-Z0-9]{2,5}$")

  data class CleanedMusicInfo(
    val title: String,
    val artist: String,
  )

  fun parse(video: Video): CleanedMusicInfo {
    var rawTitle = video.title.ifBlank { video.displayName }
    rawTitle = rawTitle.replace(EXTENSION_REGEX, "")
    rawTitle = rawTitle.replace(TRACK_NUMBER_REGEX, "")
    rawTitle = rawTitle.replace('_', ' ')

    var artist = video.artist.trim().takeUnless {
      it.isBlank() || it.equals("<unknown>", ignoreCase = true) || it.equals("unknown artist", ignoreCase = true)
    }.orEmpty()

    if (artist.isBlank() && rawTitle.contains(" - ")) {
      val parts = rawTitle.split(" - ", limit = 2)
      if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
        artist = parts[0].trim()
        rawTitle = parts[1].trim()
      }
    }

    val cleanTitle = rawTitle.replace(NOISE_REGEX, "").trim()
    val cleanArtist = artist.replace(NOISE_REGEX, "").trim()

    return CleanedMusicInfo(cleanTitle, cleanArtist)
  }
}

class OnlineMusicArtworkRepository(
  private val context: Context,
  private val client: OkHttpClient,
  private val json: Json,
  private val browserPreferences: BrowserPreferences,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val artworkDir = File(context.filesDir, "online_music_artwork").apply { mkdirs() }
  private val memoryCache = LruCache<String, Bitmap>(100)
  private val negativeCache = LruCache<String, Long>(500)
  private val ongoingFetches = ConcurrentHashMap<String, Job>()
  private val fetchSemaphore = Semaphore(3)

  companion object {
    fun sha256(input: String): String =
      MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }
  }

  fun artworkFileForPath(path: String): File =
    File(artworkDir, "p_${sha256(path)}.jpg")

  fun artworkFileForTitleArtist(title: String, artist: String): File =
    File(artworkDir, "t_${sha256(title.lowercase().trim() + "_" + artist.lowercase().trim())}.jpg")

  fun hasCachedArtwork(video: Video): Boolean {
    val fileByPath = artworkFileForPath(video.path.ifBlank { video.uri.toString() })
    if (fileByPath.isFile && fileByPath.length() > 500) return true
    val parsed = MusicTrackInfoParser.parse(video)
    if (parsed.title.isNotBlank()) {
      val fileByTitle = artworkFileForTitleArtist(parsed.title, parsed.artist)
      if (fileByTitle.isFile && fileByTitle.length() > 500) return true
    }
    return false
  }

  fun getCachedArtworkFile(video: Video): File? {
    val fileByPath = artworkFileForPath(video.path.ifBlank { video.uri.toString() })
    if (fileByPath.isFile && fileByPath.length() > 500) return fileByPath
    val parsed = MusicTrackInfoParser.parse(video)
    if (parsed.title.isNotBlank()) {
      val fileByTitle = artworkFileForTitleArtist(parsed.title, parsed.artist)
      if (fileByTitle.isFile && fileByTitle.length() > 500) return fileByTitle
    }
    return null
  }

  fun getCachedArtworkBitmap(
    video: Video,
    widthPx: Int = 0,
    heightPx: Int = 0,
  ): Bitmap? {
    if (!video.isAudio) return null
    val pathKey = video.path.ifBlank { video.uri.toString() }
    memoryCache.get(pathKey)?.let {
      if (!it.isRecycled) return it
      memoryCache.remove(pathKey)
    }

    val fileToRead = getCachedArtworkFile(video) ?: return null

    val bitmap = decodeBitmapFromFile(fileToRead, widthPx, heightPx) ?: return null
    if (!bitmap.isRecycled) {
      memoryCache.put(pathKey, bitmap)
      return bitmap
    }
    return null
  }

  suspend fun getOrFetchArtwork(
    video: Video,
    widthPx: Int = 0,
    heightPx: Int = 0,
    timeoutMs: Long = ARTWORK_FETCH_TIMEOUT_MS,
  ): Bitmap? = withContext(Dispatchers.IO) {
    if (!video.isAudio || !browserPreferences.autoFetchOnlineMusicArtwork.get()) return@withContext null
    getCachedArtworkBitmap(video, widthPx, heightPx)?.let { return@withContext it }

    val parsed = MusicTrackInfoParser.parse(video)
    val cleanTitle = parsed.title.trim().ifBlank { video.displayName.substringBeforeLast('.').trim() }
    if (cleanTitle.isBlank()) return@withContext null

    val cacheKey = cleanTitle.lowercase() + "_" + parsed.artist.lowercase()
    val lastFailed = negativeCache.get(cacheKey)
    if (lastFailed != null && System.currentTimeMillis() - lastFailed < NEGATIVE_CACHE_TTL_MS) {
      return@withContext null
    }

    val fetchedBitmap = withTimeoutOrNull(timeoutMs) {
      fetchSemaphore.acquire()
      try {
        downloadArtworkForTrack(cleanTitle, parsed.artist, video)
      } finally {
        fetchSemaphore.release()
      }
    }

    if (fetchedBitmap != null && !fetchedBitmap.isRecycled) {
      memoryCache.put(video.path.ifBlank { video.uri.toString() }, fetchedBitmap)
      return@withContext fetchedBitmap
    } else {
      negativeCache.put(cacheKey, System.currentTimeMillis())
      return@withContext null
    }
  }

  fun fetchArtworkAsync(
    video: Video,
    onCompleted: (Bitmap) -> Unit = {},
  ) {
    if (!video.isAudio || !browserPreferences.autoFetchOnlineMusicArtwork.get()) return
    if (hasCachedArtwork(video)) return
    val parsed = MusicTrackInfoParser.parse(video)
    val cleanTitle = parsed.title.trim().ifBlank { video.displayName.substringBeforeLast('.').trim() }
    if (cleanTitle.isBlank()) return

    val cacheKey = cleanTitle.lowercase() + "_" + parsed.artist.lowercase()
    val lastFailed = negativeCache.get(cacheKey)
    if (lastFailed != null && System.currentTimeMillis() - lastFailed < NEGATIVE_CACHE_TTL_MS) return

    if (ongoingFetches.containsKey(cacheKey)) return

    val job = scope.launch {
      try {
        val bitmap = getOrFetchArtwork(video)
        if (bitmap != null && !bitmap.isRecycled) {
          onCompleted(bitmap)
        }
      } catch (_: Exception) {
      } finally {
        ongoingFetches.remove(cacheKey)
      }
    }
    ongoingFetches[cacheKey] = job
  }

  private suspend fun downloadArtworkForTrack(
    title: String,
    artist: String,
    video: Video,
  ): Bitmap? {
    val artworkUrl = resolveArtworkUrl(title, artist) ?: return null
    return downloadAndSaveArtwork(artworkUrl, video, title, artist)
  }

  private suspend fun resolveArtworkUrl(
    title: String,
    artist: String,
  ): String? {
    val query = if (artist.isNotBlank()) "$title $artist" else title

    // 1. Apple iTunes Search API (fast, high-resolution square cover art)
    resolveFromItunes(query)?.let { return it }

    // 2. Deezer Public API (1000x1000 square cover art)
    resolveFromDeezer(query)?.let { return it }

    // 3. MusicBrainz + Cover Art Archive (open-source community music database)
    resolveFromMusicBrainz(title, artist)?.let { return it }

    return null
  }

  private suspend fun resolveFromItunes(query: String): String? = runCatching {
    val encoded = URLEncoder.encode(query, "UTF-8")
    val url = "$ITUNES_SEARCH_URL?term=$encoded&entity=song&limit=1"
    val req = Request.Builder()
      .url(url)
      .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
      .build()

    client.newCall(req).awaitResponse().use { response ->
      if (!response.isSuccessful) return@use null
      val body = response.body.string()
      val resp = json.decodeFromString<ITunesSearchResponse>(body)
      val item = resp.results.firstOrNull() ?: return@use null
      val rawArtwork = item.artworkUrl100?.trim()?.takeIf { it.isNotBlank() } ?: return@use null
      formatItunesArtworkUrl(rawArtwork)
    }
  }.getOrNull()

  private fun formatItunesArtworkUrl(rawUrl: String): String {
    return when {
      rawUrl.contains("/100x100bb.") -> rawUrl.replace("/100x100bb.", "/800x800bb.")
      rawUrl.contains("/60x60bb.") -> rawUrl.replace("/60x60bb.", "/800x800bb.")
      else -> rawUrl
    }
  }

  private suspend fun resolveFromDeezer(query: String): String? = runCatching {
    val encoded = URLEncoder.encode(query, "UTF-8")
    val url = "$DEEZER_SEARCH_URL?q=$encoded&limit=1"
    val req = Request.Builder()
      .url(url)
      .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
      .build()

    client.newCall(req).awaitResponse().use { response ->
      if (!response.isSuccessful) return@use null
      val body = response.body.string()
      val resp = json.decodeFromString<DeezerSearchResponse>(body)
      val track = resp.data.firstOrNull() ?: return@use null
      val album = track.album ?: return@use null
      album.cover_xl ?: album.cover_big ?: album.cover_medium
    }
  }.getOrNull()

  private suspend fun resolveFromMusicBrainz(title: String, artist: String): String? = runCatching {
    val queryBuilder = StringBuilder("recording:\"$title\"")
    if (artist.isNotBlank()) {
      queryBuilder.append(" AND artist:\"$artist\"")
    }
    val encoded = URLEncoder.encode(queryBuilder.toString(), "UTF-8")
    val url = "$MUSICBRAINZ_SEARCH_URL?query=$encoded&fmt=json&limit=1"
    val req = Request.Builder()
      .url(url)
      .header("User-Agent", "VMPlayer/1.0.0 (https://github.com/v-m-player)")
      .build()

    client.newCall(req).awaitResponse().use { response ->
      if (!response.isSuccessful) return@use null
      val body = response.body.string()
      val resp = json.decodeFromString<MusicBrainzRecordingResponse>(body)
      val recording = resp.recordings.firstOrNull() ?: return@use null
      val releaseId = recording.releases.firstOrNull()?.id ?: return@use null
      "https://coverartarchive.org/release/$releaseId/front-500"
    }
  }.getOrNull()

  private suspend fun downloadAndSaveArtwork(
    artworkUrl: String,
    video: Video,
    title: String,
    artist: String,
  ): Bitmap? = runCatching {
    val req = Request.Builder()
      .url(artworkUrl)
      .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
      .build()

    client.newCall(req).awaitResponse().use { response ->
      if (!response.isSuccessful) return@use null
      val bytes = response.body.bytes()
      if (bytes.size < 500) return@use null

      val pathFile = artworkFileForPath(video.path.ifBlank { video.uri.toString() })
      FileOutputStream(pathFile).use { it.write(bytes) }

      val titleFile = artworkFileForTitleArtist(title, artist)
      if (!titleFile.exists() || titleFile.length() == 0L) {
        FileOutputStream(titleFile).use { it.write(bytes) }
      }

      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
  }.getOrNull()

  private fun decodeBitmapFromFile(
    file: File,
    widthPx: Int = 0,
    heightPx: Int = 0,
  ): Bitmap? = runCatching {
    if (!file.canRead() || file.length() < 500) return null
    if (widthPx > 0 && heightPx > 0) {
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeFile(file.absolutePath, bounds)
      val sampleSize = calculateThumbnailSampleSize(bounds.outWidth, bounds.outHeight, 800, widthPx, heightPx)
      BitmapFactory.decodeFile(
        file.absolutePath,
        BitmapFactory.Options().apply {
          inSampleSize = sampleSize
          inPreferredConfig = Bitmap.Config.ARGB_8888
        },
      )
    } else {
      BitmapFactory.decodeFile(
        file.absolutePath,
        BitmapFactory.Options().apply {
          inPreferredConfig = Bitmap.Config.ARGB_8888
        },
      )
    }
  }.getOrNull()

  fun clearOnlineArtworkCache() {
    artworkDir.listFiles()?.forEach { it.delete() }
    memoryCache.evictAll()
    negativeCache.evictAll()
  }
}
