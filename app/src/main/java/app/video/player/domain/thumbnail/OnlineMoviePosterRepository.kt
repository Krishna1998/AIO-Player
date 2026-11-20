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
import app.video.player.utils.media.MediaInfoParser
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

private const val TAG = "OnlinePosterRepo"
private const val TMDB_SEARCH_URL = "https://sub.wyzie.io/api/tmdb/search"
private const val TVMAZE_SEARCH_URL = "https://api.tvmaze.com/singlesearch/shows"
private const val OMDB_SEARCH_URL = "https://www.omdbapi.com/"
private const val POSTER_FETCH_TIMEOUT_MS = 6000L
private const val NEGATIVE_CACHE_TTL_MS = 15 * 60 * 1000L // 15 mins

@Serializable
private data class TmdbSearchResultItem(
  val id: Int? = null,
  val mediaType: String? = null,
  val title: String? = null,
  val originalTitle: String? = null,
  val releaseYear: String? = null,
  val poster: String? = null,
  val backdrop: String? = null,
  val voteAverage: Double? = null,
  val popularity: Double? = null,
)

@Serializable
private data class TmdbSearchResponse(
  val results: List<TmdbSearchResultItem> = emptyList(),
)

@Serializable
private data class TvMazeImage(
  val medium: String? = null,
  val original: String? = null,
)

@Serializable
private data class TvMazeShow(
  val id: Int? = null,
  val name: String? = null,
  val image: TvMazeImage? = null,
)

@Serializable
private data class OmdbMovieResponse(
  val Title: String? = null,
  val Year: String? = null,
  val Poster: String? = null,
  val Response: String? = null,
)

class OnlineMoviePosterRepository(
  private val context: Context,
  private val client: OkHttpClient,
  private val json: Json,
  private val browserPreferences: BrowserPreferences,
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val posterDir = File(context.filesDir, "online_posters").apply { mkdirs() }
  private val memoryCache = LruCache<String, Bitmap>(100)
  private val negativeCache = LruCache<String, Long>(500)
  private val ongoingFetches = ConcurrentHashMap<String, Job>()
  private val fetchSemaphore = Semaphore(3) // at most 3 concurrent downloads

  companion object {
    fun sha256(input: String): String =
      MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }
  }

  fun posterFileForPath(path: String): File =
    File(posterDir, "p_${sha256(path)}.jpg")

  fun posterFileForTitleYear(title: String, year: String?): File =
    File(posterDir, "t_${sha256(title.lowercase().trim() + "_" + (year ?: "").trim())}.jpg")

  fun hasCachedPoster(video: Video): Boolean {
    val fileByPath = posterFileForPath(video.path.ifBlank { video.uri.toString() })
    if (fileByPath.isFile && fileByPath.length() > 500) return true
    val parsed = MediaInfoParser.parse(video.displayName.ifBlank { video.title })
    if (parsed.title.isNotBlank()) {
      val fileByTitle = posterFileForTitleYear(parsed.title, parsed.year)
      if (fileByTitle.isFile && fileByTitle.length() > 500) return true
    }
    return false
  }

  fun getCachedPosterFile(video: Video): File? {
    val fileByPath = posterFileForPath(video.path.ifBlank { video.uri.toString() })
    if (fileByPath.isFile && fileByPath.length() > 500) return fileByPath
    val parsed = MediaInfoParser.parse(video.displayName.ifBlank { video.title })
    if (parsed.title.isNotBlank()) {
      val fileByTitle = posterFileForTitleYear(parsed.title, parsed.year)
      if (fileByTitle.isFile && fileByTitle.length() > 500) return fileByTitle
    }
    return null
  }

  fun getCachedPosterBitmap(
    video: Video,
    widthPx: Int = 0,
    heightPx: Int = 0,
  ): Bitmap? {
    if (video.isAudio) return null
    val pathKey = video.path.ifBlank { video.uri.toString() }
    memoryCache.get(pathKey)?.let {
      if (!it.isRecycled) return it
      memoryCache.remove(pathKey)
    }

    val fileToRead = getCachedPosterFile(video) ?: return null

    val bitmap = decodeBitmapFromFile(fileToRead, widthPx, heightPx) ?: return null
    if (!bitmap.isRecycled) {
      memoryCache.put(pathKey, bitmap)
      return bitmap
    }
    return null
  }

  suspend fun getOrFetchPoster(
    video: Video,
    widthPx: Int = 0,
    heightPx: Int = 0,
    timeoutMs: Long = POSTER_FETCH_TIMEOUT_MS,
  ): Bitmap? = withContext(Dispatchers.IO) {
    if (video.isAudio || !browserPreferences.autoFetchOnlinePosters.get()) return@withContext null
    getCachedPosterBitmap(video, widthPx, heightPx)?.let { return@withContext it }

    val parsed = MediaInfoParser.parse(video.displayName.ifBlank { video.title })
    val cleanTitle = parsed.title.trim().ifBlank {
      MediaInfoParser.parse(video.title).title.trim()
    }
    if (cleanTitle.isBlank()) return@withContext null

    val cacheKey = cleanTitle.lowercase() + "_" + (parsed.year ?: "")
    val lastFailed = negativeCache.get(cacheKey)
    if (lastFailed != null && System.currentTimeMillis() - lastFailed < NEGATIVE_CACHE_TTL_MS) {
      return@withContext null
    }

    val fetchedBitmap = withTimeoutOrNull(timeoutMs) {
      fetchSemaphore.acquire()
      try {
        downloadPosterForMedia(cleanTitle, parsed.year, parsed.type, video)
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

  fun fetchPosterAsync(
    video: Video,
    onCompleted: (Bitmap) -> Unit = {},
  ) {
    if (video.isAudio || !browserPreferences.autoFetchOnlinePosters.get()) return
    if (hasCachedPoster(video)) return
    val parsed = MediaInfoParser.parse(video.displayName.ifBlank { video.title })
    val cleanTitle = parsed.title.trim().ifBlank {
      MediaInfoParser.parse(video.title).title.trim()
    }
    if (cleanTitle.isBlank()) return

    val cacheKey = cleanTitle.lowercase() + "_" + (parsed.year ?: "")
    val lastFailed = negativeCache.get(cacheKey)
    if (lastFailed != null && System.currentTimeMillis() - lastFailed < NEGATIVE_CACHE_TTL_MS) return

    if (ongoingFetches.containsKey(cacheKey)) return

    val job = scope.launch {
      try {
        val bitmap = getOrFetchPoster(video)
        if (bitmap != null) {
          onCompleted(bitmap)
        }
      } catch (_: Exception) {
      } finally {
        ongoingFetches.remove(cacheKey)
      }
    }
    ongoingFetches[cacheKey] = job
  }

  private suspend fun downloadPosterForMedia(
    title: String,
    year: String?,
    type: String,
    video: Video,
  ): Bitmap? {
    val posterUrl = resolvePosterUrl(title, year, type) ?: return null
    return downloadAndSavePoster(posterUrl, video, title, year)
  }

  private suspend fun resolvePosterUrl(
    title: String,
    year: String?,
    type: String,
  ): String? {
    // 1. Try TMDB via free proxy
    resolveFromTmdb(title, year, type)?.let { return it }

    // 2. If TV show, try TVMaze
    if (type.equals("tv", ignoreCase = true)) {
      resolveFromTvMaze(title)?.let { return it }
    }

    // 3. Fallback to OMDb
    resolveFromOmdb(title, year)?.let { return it }

    // 4. TVMaze general fallback
    resolveFromTvMaze(title)?.let { return it }

    return null
  }

  private suspend fun resolveFromTmdb(
    title: String,
    year: String?,
    type: String,
  ): String? = runCatching {
    val encoded = URLEncoder.encode(title, "UTF-8")
    val url = "$TMDB_SEARCH_URL?q=$encoded"
    val req = Request.Builder()
      .url(url)
      .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
      .build()

    client.newCall(req).awaitResponse().use { response ->
      if (!response.isSuccessful) return@use null
      val body = response.body.string()
      val results = json.decodeFromString<TmdbSearchResponse>(body).results
      if (results.isEmpty()) return@use null

      // Pick best match
      val match = when {
        year != null -> results.firstOrNull { it.releaseYear == year }
          ?: results.firstOrNull { it.releaseYear?.startsWith(year.take(3)) == true }
          ?: results.firstOrNull { it.mediaType.equals(type, ignoreCase = true) }
          ?: results[0]
        else -> results.firstOrNull { it.mediaType.equals(type, ignoreCase = true) } ?: results[0]
      }

      // Prefer cinematic 16:9 landscape backdrop over vertical portrait poster
      val backdrop = match.backdrop?.trim()?.takeIf { it.isNotBlank() }
      if (backdrop != null) {
        return@use formatTmdbBackdropUrl(backdrop)
      }

      val poster = match.poster?.trim()?.takeIf { it.isNotBlank() } ?: return@use null
      formatTmdbPosterUrl(poster)
    }
  }.getOrNull()

  private fun formatTmdbBackdropUrl(backdrop: String): String = when {
    backdrop.startsWith("https://image.tmdb.org/t/p/w500/") ->
      backdrop.replace("/w500/", "/w780/")
    backdrop.startsWith("https://image.tmdb.org/t/p/w300/") ->
      backdrop.replace("/w300/", "/w780/")
    backdrop.startsWith("https://image.tmdb.org/t/p/w185/") ->
      backdrop.replace("/w185/", "/w780/")
    backdrop.startsWith("http://", ignoreCase = true) || backdrop.startsWith("https://", ignoreCase = true) ->
      backdrop
    backdrop.startsWith("/") ->
      "https://image.tmdb.org/t/p/w780$backdrop"
    else ->
      "https://image.tmdb.org/t/p/w780/$backdrop"
  }

  private fun formatTmdbPosterUrl(poster: String): String = when {
    poster.startsWith("https://image.tmdb.org/t/p/w185/") ->
      poster.replace("/w185/", "/w500/")
    poster.startsWith("http://", ignoreCase = true) || poster.startsWith("https://", ignoreCase = true) ->
      poster
    poster.startsWith("/") ->
      "https://image.tmdb.org/t/p/w500$poster"
    else ->
      "https://image.tmdb.org/t/p/w500/$poster"
  }

  private suspend fun resolveFromTvMaze(title: String): String? = runCatching {
    val encoded = URLEncoder.encode(title, "UTF-8")
    val url = "$TVMAZE_SEARCH_URL?q=$encoded"
    val req = Request.Builder()
      .url(url)
      .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
      .build()

    client.newCall(req).awaitResponse().use { response ->
      if (!response.isSuccessful) return@use null
      val body = response.body.string()
      val show = json.decodeFromString<TvMazeShow>(body)
      show.image?.original ?: show.image?.medium
    }
  }.getOrNull()

  private suspend fun resolveFromOmdb(title: String, year: String?): String? = runCatching {
    val encoded = URLEncoder.encode(title, "UTF-8")
    val yearParam = if (!year.isNullOrBlank()) "&y=${URLEncoder.encode(year, "UTF-8")}" else ""
    val url = "$OMDB_SEARCH_URL?t=$encoded$yearParam&apikey=trilogy"
    val req = Request.Builder()
      .url(url)
      .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
      .build()

    client.newCall(req).awaitResponse().use { response ->
      if (!response.isSuccessful) return@use null
      val body = response.body.string()
      val omdb = json.decodeFromString<OmdbMovieResponse>(body)
      if (omdb.Response.equals("True", ignoreCase = true) && !omdb.Poster.isNullOrBlank() && omdb.Poster != "N/A") {
        omdb.Poster
      } else null
    }
  }.getOrNull()

  private suspend fun downloadAndSavePoster(
    posterUrl: String,
    video: Video,
    title: String,
    year: String?,
  ): Bitmap? = runCatching {
    val req = Request.Builder()
      .url(posterUrl)
      .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
      .build()

    client.newCall(req).awaitResponse().use { response ->
      if (!response.isSuccessful) return@use null
      val bytes = response.body.bytes()
      if (bytes.size < 500) return@use null

      // Save to file by path
      val pathFile = posterFileForPath(video.path.ifBlank { video.uri.toString() })
      FileOutputStream(pathFile).use { it.write(bytes) }

      // Also save to file by title_year for instant reuse
      val titleFile = posterFileForTitleYear(title, year)
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
      val sampleSize = calculateThumbnailSampleSize(bounds.outWidth, bounds.outHeight, 1080, widthPx, heightPx)
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

  /**
   * When a video/movie file is renamed, migrate its path-based cached poster
   * from the old path hash to the new path hash so the thumbnail doesn't vanish.
   */
  fun onMediaRenamed(oldPath: String, newPath: String) {
    if (oldPath == newPath) return
    scope.launch(Dispatchers.IO) {
      runCatching {
        val oldFile = posterFileForPath(oldPath)
        val newFile = posterFileForPath(newPath)
        if (oldFile.isFile && oldFile.length() > 500 && !newFile.exists()) {
          oldFile.copyTo(newFile, overwrite = false)
        }
        // Update memory cache: move the bitmap from oldPath key to newPath key
        synchronized(memoryCache) {
          val existing = memoryCache.get(oldPath)
          if (existing != null && !existing.isRecycled) {
            memoryCache.put(newPath, existing)
          }
        }
      }
    }
  }

  fun clearOnlinePosterCache() {
    posterDir.listFiles()?.forEach { it.delete() }
    memoryCache.evictAll()
    negativeCache.evictAll()
  }
}

