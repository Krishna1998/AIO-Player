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
import android.net.Uri
import android.util.Log
import android.util.LruCache
import app.video.player.domain.media.model.Video
import app.video.player.network.awaitResponse
import app.video.player.preferences.BrowserPreferences
import app.video.player.ui.player.resolveLocalPath
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
private data class DeezerArtist(
  val id: Long? = null,
  val name: String? = null,
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
  val artist: DeezerArtist? = null,
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
      it.isBlank() || isGenericArtistName(it)
    }.orEmpty()

    if (artist.isBlank() && rawTitle.contains(" - ")) {
      val parts = rawTitle.split(" - ", limit = 2)
      if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
        val possibleArtist = parts[0].trim()
        val possibleTitle = parts[1].trim()
        if (!isGenericArtistName(possibleArtist) && !possibleArtist.all { it.isDigit() || it == '.' }) {
          artist = possibleArtist
          rawTitle = possibleTitle
        }
      }
    }

    val cleanTitle = rawTitle.replace(NOISE_REGEX, "").trim()
    val cleanArtist = artist.replace(NOISE_REGEX, "").trim()

    return CleanedMusicInfo(cleanTitle, cleanArtist)
  }
}

fun isGenericTrackName(title: String): Boolean {
  val t = title.lowercase().trim()
  if (t.length < 2) return true
  if (t.all { it.isDigit() || it == '-' || it == '_' || it == ' ' || it == '.' }) return true
  val genericRegex = Regex(
    "^(?:track|audio|sound|record|recording|voice|song|untitled|new track|bgm|instrumental|sample|intro|outro|interlude|audiotrack|aud|rec)?\\s*\\d*$",
    RegexOption.IGNORE_CASE,
  )
  return genericRegex.matches(t)
}

fun isGenericArtistName(artist: String): Boolean {
  val a = artist.lowercase().trim()
  if (a.length < 2) return true
  val genericArtists = setOf("unknown", "unknown artist", "various", "various artists", "various artist", "<unknown>", "artist", "singer", "audio", "music")
  return a in genericArtists || a.equals("<unknown>", ignoreCase = true)
}

internal fun normalizeForComparison(input: String): String {
  return input.lowercase()
    .replace(Regex("[^a-z0-9\\s]"), "")
    .replace(Regex("\\s+"), " ")
    .trim()
}

internal fun similarityScore(s1: String, s2: String): Double {
  if (s1 == s2) return 1.0
  if (s1.isEmpty() || s2.isEmpty()) return 0.0
  val pairs1 = wordLetterPairs(s1)
  val pairs2 = wordLetterPairs(s2)
  val union = pairs1.size + pairs2.size
  if (union == 0) return 0.0
  var intersection = 0
  for (p in pairs1) {
    if (pairs2.remove(p)) intersection++
  }
  return (2.0 * intersection) / union
}

private fun wordLetterPairs(str: String): MutableList<String> {
  val allPairs = mutableListOf<String>()
  val words = str.split(" ")
  for (w in words) {
    for (i in 0 until w.length - 1) {
      allPairs.add(w.substring(i, i + 2))
    }
  }
  return allPairs
}

internal fun matchesMusicMetadata(
  queryTitle: String,
  queryArtist: String,
  resultTitle: String?,
  resultArtist: String?,
): Boolean {
  val resTitle = resultTitle?.trim().orEmpty()
  if (resTitle.isBlank()) return false

  val normQueryTitle = normalizeForComparison(queryTitle)
  val normResTitle = normalizeForComparison(resTitle)
  if (normQueryTitle.isBlank() || normResTitle.isBlank()) return false

  val titleMatches = normQueryTitle == normResTitle ||
    normQueryTitle.contains(normResTitle) ||
    normResTitle.contains(normQueryTitle) ||
    similarityScore(normQueryTitle, normResTitle) >= 0.70

  if (!titleMatches) return false

  val normQueryArtist = normalizeForComparison(queryArtist)
  val resArtist = resultArtist?.trim().orEmpty()
  val normResArtist = normalizeForComparison(resArtist)

  if (normQueryArtist.isNotBlank() && !isGenericArtistName(queryArtist)) {
    if (normResArtist.isBlank()) return false
    val artistMatches = normQueryArtist == normResArtist ||
      normQueryArtist.contains(normResArtist) ||
      normResArtist.contains(normQueryArtist) ||
      similarityScore(normQueryArtist, normResArtist) >= 0.65
    return artistMatches
  }

  // When query artist is blank or generic, require strict title match and non-generic title
  if (isGenericTrackName(queryTitle)) return false
  return normQueryTitle == normResTitle || similarityScore(normQueryTitle, normResTitle) >= 0.85
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

  init {
    ensureCacheVersion()
  }

  private fun ensureCacheVersion() {
    val versionMarker = File(artworkDir, "cache_v2.marker")
    if (!versionMarker.exists()) {
      runCatching {
        artworkDir.listFiles()?.forEach { file ->
          if (file.isFile && file.name != "cache_v2.marker") {
            file.delete()
          }
        }
        versionMarker.createNewFile()
      }
    }
  }

  companion object {
    fun sha256(input: String): String =
      MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }
  }

  fun artworkFileForPath(path: String): File =
    File(artworkDir, "p_${sha256(path)}.jpg")

  fun artworkFileForTitleArtist(title: String, artist: String): File? {
    val cleanT = title.lowercase().trim()
    val cleanA = artist.lowercase().trim()
    if (cleanT.length < 2 || cleanA.isBlank() || isGenericTrackName(cleanT) || isGenericArtistName(cleanA)) {
      return null
    }
    return File(artworkDir, "t_${sha256(cleanT + "_" + cleanA)}.jpg")
  }

  fun hasCachedArtwork(video: Video): Boolean =
    getCachedArtworkFile(video) != null

  fun getCachedArtworkFile(video: Video): File? {
    val pathKey = video.path.ifBlank { video.uri.toString() }
    if (pathKey.isNotBlank()) {
      val fileByPath = artworkFileForPath(pathKey)
      if (fileByPath.isFile && fileByPath.length() > 500) return fileByPath
    }

    if (video.uri.toString().isNotBlank() && video.uri.toString() != pathKey) {
      val fileByUri = artworkFileForPath(video.uri.toString())
      if (fileByUri.isFile && fileByUri.length() > 500) return fileByUri
    }

    if (pathKey.startsWith("content://", ignoreCase = true) || video.uri.scheme.equals("content", ignoreCase = true)) {
      val targetUri = if (video.uri.scheme.equals("content", ignoreCase = true)) video.uri else Uri.parse(pathKey)
      val resolvedPath = runCatching { targetUri.resolveLocalPath(context) }.getOrNull()
      if (!resolvedPath.isNullOrBlank()) {
        val fileByResolved = artworkFileForPath(resolvedPath)
        if (fileByResolved.isFile && fileByResolved.length() > 500) return fileByResolved
      }
    }

    val parsed = MusicTrackInfoParser.parse(video)
    if (parsed.title.isNotBlank() && parsed.artist.isNotBlank() && !isGenericArtistName(parsed.artist)) {
      val fileByTitle = artworkFileForTitleArtist(parsed.title, parsed.artist)
      if (fileByTitle != null && fileByTitle.isFile && fileByTitle.length() > 500) return fileByTitle
    }

    val cleanTitle = video.title.ifBlank { video.displayName }.substringBeforeLast('.').trim()
    if (cleanTitle.isNotBlank() && video.artist.isNotBlank() && !isGenericArtistName(video.artist)) {
      val altFile = artworkFileForTitleArtist(cleanTitle, video.artist.trim())
      if (altFile != null && altFile.isFile && altFile.length() > 500) return altFile
    }

    return null
  }

  fun getCachedArtworkFile(
    pathOrUri: String?,
    title: String? = null,
    artist: String? = null,
  ): File? {
    if (pathOrUri.isNullOrBlank() && title.isNullOrBlank()) return null
    if (!pathOrUri.isNullOrBlank()) {
      val fileByPath = artworkFileForPath(pathOrUri)
      if (fileByPath.isFile && fileByPath.length() > 500) return fileByPath

      if (pathOrUri.startsWith("content://", ignoreCase = true)) {
        val targetUri = Uri.parse(pathOrUri)
        val resolvedPath = runCatching { targetUri.resolveLocalPath(context) }.getOrNull()
        if (!resolvedPath.isNullOrBlank()) {
          val fileByResolved = artworkFileForPath(resolvedPath)
          if (fileByResolved.isFile && fileByResolved.length() > 500) return fileByResolved
        }
      } else if (pathOrUri.startsWith("file://", ignoreCase = true)) {
        val clean = pathOrUri.removePrefix("file://")
        val fileByClean = artworkFileForPath(clean)
        if (fileByClean.isFile && fileByClean.length() > 500) return fileByClean
      }
    }

    if (!title.isNullOrBlank() && !artist.isNullOrBlank() && !isGenericArtistName(artist)) {
      val cleanTitle = title.substringBeforeLast('.').trim()
      val cleanArtist = artist.trim()
      val fileByTitle = artworkFileForTitleArtist(cleanTitle, cleanArtist)
      if (fileByTitle != null && fileByTitle.isFile && fileByTitle.length() > 500) return fileByTitle

      val dummyVideo = Video(
        id = 0L, title = title, displayName = title, path = pathOrUri.orEmpty(),
        uri = Uri.parse(pathOrUri.orEmpty()), duration = 0L, durationFormatted = "",
        size = 0L, sizeFormatted = "", dateModified = 0L, dateAdded = 0L,
        mimeType = "audio/*", bucketId = "", bucketDisplayName = "", width = 0, height = 0,
        fps = 0f, resolution = "", isAudio = true, artist = cleanArtist,
      )
      val parsed = MusicTrackInfoParser.parse(dummyVideo)
      if (parsed.title.isNotBlank() && parsed.artist.isNotBlank() && !isGenericArtistName(parsed.artist)) {
        val fileByParsed = artworkFileForTitleArtist(parsed.title, parsed.artist)
        if (fileByParsed != null && fileByParsed.isFile && fileByParsed.length() > 500) return fileByParsed
      }
    }
    return null
  }

      val dummyVideo = Video(
        id = 0L, title = title, displayName = title, path = pathOrUri.orEmpty(),
        uri = Uri.parse(pathOrUri.orEmpty()), duration = 0L, durationFormatted = "",
        size = 0L, sizeFormatted = "", dateModified = 0L, dateAdded = 0L,
        mimeType = "audio/*", bucketId = "", bucketDisplayName = "", width = 0, height = 0,
        fps = 0f, resolution = "", isAudio = true, artist = cleanArtist,
      )
      val parsed = MusicTrackInfoParser.parse(dummyVideo)
      if (parsed.title.isNotBlank()) {
        val fileByParsed = artworkFileForTitleArtist(parsed.title, parsed.artist)
        if (fileByParsed.isFile && fileByParsed.length() > 500) return fileByParsed
      }
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

  fun getCachedArtworkBitmap(
    pathOrUri: String?,
    title: String? = null,
    artist: String? = null,
    widthPx: Int = 0,
    heightPx: Int = 0,
  ): Bitmap? {
    val key = pathOrUri?.ifBlank { null } ?: (title.orEmpty() + "_" + artist.orEmpty())
    if (key.isNotBlank()) {
      memoryCache.get(key)?.let {
        if (!it.isRecycled) return it
        memoryCache.remove(key)
      }
    }
    val fileToRead = getCachedArtworkFile(pathOrUri, title, artist) ?: return null
    val bitmap = decodeBitmapFromFile(fileToRead, widthPx, heightPx) ?: return null
    if (!bitmap.isRecycled && key.isNotBlank()) {
      memoryCache.put(key, bitmap)
    }
    return bitmap
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

  suspend fun getOrFetchArtworkForTrack(
    pathOrUri: String?,
    title: String?,
    artist: String?,
    widthPx: Int = 0,
    heightPx: Int = 0,
    timeoutMs: Long = ARTWORK_FETCH_TIMEOUT_MS,
  ): Bitmap? = withContext(Dispatchers.IO) {
    if (!browserPreferences.autoFetchOnlineMusicArtwork.get()) return@withContext null
    getCachedArtworkBitmap(pathOrUri, title, artist, widthPx, heightPx)?.let { return@withContext it }

    val cleanPath = pathOrUri.orEmpty()
    val resolvedLocalPath = if (cleanPath.startsWith("content://", ignoreCase = true)) {
      runCatching { Uri.parse(cleanPath).resolveLocalPath(context) }.getOrNull() ?: cleanPath
    } else cleanPath

    val effectiveUri = runCatching { Uri.parse(cleanPath) }.getOrNull() ?: Uri.EMPTY
    val dummyVideo = Video(
      id = 0L,
      title = title.orEmpty(),
      displayName = title.orEmpty().ifBlank { effectiveUri.lastPathSegment.orEmpty() },
      path = resolvedLocalPath,
      uri = effectiveUri,
      duration = 0L,
      durationFormatted = "",
      size = 0L,
      sizeFormatted = "",
      dateModified = 0L,
      dateAdded = 0L,
      mimeType = "audio/*",
      bucketId = "",
      bucketDisplayName = "",
      width = 0,
      height = 0,
      fps = 0f,
      resolution = "",
      isAudio = true,
      artist = artist.orEmpty(),
    )
    getOrFetchArtwork(dummyVideo, widthPx, heightPx, timeoutMs)
  }

  fun fetchArtworkAsyncForTrack(
    pathOrUri: String?,
    title: String?,
    artist: String?,
    onCompleted: (Bitmap) -> Unit = {},
  ) {
    if (!browserPreferences.autoFetchOnlineMusicArtwork.get()) return
    val cleanPath = pathOrUri.orEmpty()
    val resolvedLocalPath = if (cleanPath.startsWith("content://", ignoreCase = true)) {
      runCatching { Uri.parse(cleanPath).resolveLocalPath(context) }.getOrNull() ?: cleanPath
    } else cleanPath

    val effectiveUri = runCatching { Uri.parse(cleanPath) }.getOrNull() ?: Uri.EMPTY
    val dummyVideo = Video(
      id = 0L,
      title = title.orEmpty(),
      displayName = title.orEmpty().ifBlank { effectiveUri.lastPathSegment.orEmpty() },
      path = resolvedLocalPath,
      uri = effectiveUri,
      duration = 0L,
      durationFormatted = "",
      size = 0L,
      sizeFormatted = "",
      dateModified = 0L,
      dateAdded = 0L,
      mimeType = "audio/*",
      bucketId = "",
      bucketDisplayName = "",
      width = 0,
      height = 0,
      fps = 0f,
      resolution = "",
      isAudio = true,
      artist = artist.orEmpty(),
    )
    fetchArtworkAsync(dummyVideo, onCompleted)
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
    if (isGenericTrackName(title)) return null
    val effectiveArtist = if (isGenericArtistName(artist)) "" else artist.trim()
    val query = if (effectiveArtist.isNotBlank()) "$title $effectiveArtist" else title

    // 1. Apple iTunes Search API (fast, high-resolution square cover art)
    resolveFromItunes(title, effectiveArtist, query)?.let { return it }

    // 2. Deezer Public API (1000x1000 square cover art)
    resolveFromDeezer(title, effectiveArtist, query)?.let { return it }

    // 3. MusicBrainz + Cover Art Archive (open-source community music database)
    if (effectiveArtist.isNotBlank()) {
      resolveFromMusicBrainz(title, effectiveArtist)?.let { return it }
    }

    return null
  }

  private suspend fun resolveFromItunes(
    title: String,
    artist: String,
    query: String,
  ): String? = runCatching {
    val encoded = URLEncoder.encode(query, "UTF-8")
    val url = "$ITUNES_SEARCH_URL?term=$encoded&entity=song&limit=5"
    val req = Request.Builder()
      .url(url)
      .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
      .build()

    client.newCall(req).awaitResponse().use { response ->
      if (!response.isSuccessful) return@use null
      val body = response.body.string()
      val resp = json.decodeFromString<ITunesSearchResponse>(body)
      val bestMatch = resp.results.firstOrNull { item ->
        matchesMusicMetadata(
          queryTitle = title,
          queryArtist = artist,
          resultTitle = item.trackName,
          resultArtist = item.artistName,
        )
      } ?: return@use null
      val rawArtwork = bestMatch.artworkUrl100?.trim()?.takeIf { it.isNotBlank() } ?: return@use null
      formatItunesArtworkUrl(rawArtwork)
    }
  }.getOrNull()

  private fun formatItunesArtworkUrl(rawUrl: String): String {
    return when {
      rawUrl.contains("/100x100bb.") -> rawUrl.replace("/100x100bb.", "/1400x1400bb.")
      rawUrl.contains("/60x60bb.") -> rawUrl.replace("/60x60bb.", "/1400x1400bb.")
      else -> rawUrl
    }
  }

  private suspend fun resolveFromDeezer(
    title: String,
    artist: String,
    query: String,
  ): String? = runCatching {
    val encoded = URLEncoder.encode(query, "UTF-8")
    val url = "$DEEZER_SEARCH_URL?q=$encoded&limit=5"
    val req = Request.Builder()
      .url(url)
      .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
      .build()

    client.newCall(req).awaitResponse().use { response ->
      if (!response.isSuccessful) return@use null
      val body = response.body.string()
      val resp = json.decodeFromString<DeezerSearchResponse>(body)
      val bestMatch = resp.data.firstOrNull { item ->
        matchesMusicMetadata(
          queryTitle = title,
          queryArtist = artist,
          resultTitle = item.title,
          resultArtist = item.artist?.name,
        )
      } ?: return@use null
      val album = bestMatch.album ?: return@use null
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
      val recording = resp.recordings.firstOrNull { rec ->
        matchesMusicMetadata(
          queryTitle = title,
          queryArtist = artist,
          resultTitle = rec.title,
          resultArtist = artist,
        )
      } ?: return@use null
      val releaseId = recording.releases.firstOrNull()?.id ?: return@use null
      "https://coverartarchive.org/release/$releaseId/front-1200"
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

      if (video.uri.toString().isNotBlank() && video.uri.toString() != video.path) {
        runCatching {
          val uriFile = artworkFileForPath(video.uri.toString())
          FileOutputStream(uriFile).use { it.write(bytes) }
        }
      }

      val targetUri = if (video.uri.scheme.equals("content", ignoreCase = true)) video.uri else runCatching { Uri.parse(video.path) }.getOrNull()
      if (targetUri != null && targetUri.scheme.equals("content", ignoreCase = true)) {
        runCatching {
          targetUri.resolveLocalPath(context)?.takeIf(String::isNotBlank)?.let { resolvedPath ->
            val resolvedFile = artworkFileForPath(resolvedPath)
            FileOutputStream(resolvedFile).use { it.write(bytes) }
          }
        }
      }

      val titleFile = artworkFileForTitleArtist(title, artist)
      if (titleFile != null && (!titleFile.exists() || titleFile.length() == 0L)) {
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
    val targetW = maxOf(widthPx, 512)
    val targetH = maxOf(heightPx, 512)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    val sampleSize = calculateThumbnailSampleSize(bounds.outWidth, bounds.outHeight, 1400, targetW, targetH)
    BitmapFactory.decodeFile(
      file.absolutePath,
      BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
      },
    )
  }.getOrNull()

  fun clearOnlineArtworkCache() {
    artworkDir.listFiles()?.forEach { it.delete() }
    memoryCache.evictAll()
    negativeCache.evictAll()
  }
}
