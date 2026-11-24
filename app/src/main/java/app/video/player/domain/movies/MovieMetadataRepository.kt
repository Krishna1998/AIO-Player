/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.domain.movies

import android.content.Context
import android.util.Log
import app.video.player.domain.media.model.Video
import app.video.player.repository.wyzie.WyzieSearchRepository
import app.video.player.utils.media.MediaInfoParser
import app.video.player.utils.storage.VideoScanUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class MovieMetadata(
  val title: String,
  val originalTitle: String? = null,
  val releaseYear: String? = null,
  val durationFormatted: String = "",
  val resolution: String = "",
  val synopsis: String? = null,
  val posterUrl: String? = null,
  val backdropUrl: String? = null,
  val voteAverage: Double? = null,
  val tmdbId: Int? = null,
)

class MovieMetadataRepository(
  private val context: Context,
  private val wyzieSearchRepository: WyzieSearchRepository,
  private val json: Json,
) {
  private val cacheDir = File(context.filesDir, "movie_metadata").apply { mkdirs() }
  private val memoryCache = ConcurrentHashMap<String, MovieMetadata>()

  companion object {
    private const val TAG = "MovieMetadataRepo"

    fun sha256(input: String): String =
      MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }
  }

  private fun cacheFileForPath(path: String): File =
    File(cacheDir, "m_${sha256(path)}.json")

  fun getCachedMetadata(video: Video): MovieMetadata? {
    val pathKey = video.path.ifBlank { video.uri.toString() }
    memoryCache[pathKey]?.let { return it }

    val file = cacheFileForPath(pathKey)
    if (file.isFile && file.length() > 0) {
      runCatching {
        val meta = json.decodeFromString<MovieMetadata>(file.readText())
        memoryCache[pathKey] = meta
        return meta
      }
    }
    return null
  }

  suspend fun getOrFetchMetadata(video: Video, forceRefresh: Boolean = false): MovieMetadata =
    withContext(Dispatchers.IO) {
      val pathKey = video.path.ifBlank { video.uri.toString() }
      if (!forceRefresh) {
        getCachedMetadata(video)?.let { return@withContext it }
      }

      val parsed = MediaInfoParser.parse(video.displayName.ifBlank { video.title })
      val cleanTitle = parsed.title.trim().ifBlank {
        MediaInfoParser.parse(video.title).title.trim().ifBlank {
          val name = video.displayName.substringBeforeLast('.')
          if (name.isNotBlank()) name else video.title
        }
      }
      val year = parsed.year
      val resolution = if (parsed.resolution.isNotBlank()) {
        parsed.resolution
      } else if (video.height > 0) {
        when {
          video.height >= 2160 || video.width >= 3840 -> "4K"
          video.height >= 1080 || video.width >= 1920 -> "1080p"
          video.height >= 720 || video.width >= 1280 -> "720p"
          else -> "${video.height}p"
        }
      } else "1080p"

      val durationFormatted = video.durationFormatted.ifBlank {
        if (video.duration > 0) {
          VideoScanUtils.formatDuration(video.duration)
        } else ""
      }

      var movieMeta = MovieMetadata(
        title = cleanTitle,
        releaseYear = year,
        durationFormatted = durationFormatted,
        resolution = resolution,
      )

      try {
        val matchResult = wyzieSearchRepository.findBestMediaMatch(cleanTitle, year)
        val tmdb = matchResult.getOrNull()
        if (tmdb != null) {
          val posterUrl = formatImageUrl(tmdb.poster, isBackdrop = false)
          val backdropUrl = formatImageUrl(tmdb.backdrop, isBackdrop = true)
          movieMeta = movieMeta.copy(
            title = tmdb.title.takeIf { it.isNotBlank() } ?: cleanTitle,
            releaseYear = tmdb.releaseYear ?: year,
            synopsis = tmdb.overview?.takeIf { it.isNotBlank() },
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            tmdbId = tmdb.id,
          )
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed to fetch online movie metadata for $cleanTitle: ${e.message}")
      }

      memoryCache[pathKey] = movieMeta
      runCatching {
        cacheFileForPath(pathKey).writeText(json.encodeToString(movieMeta))
      }

      movieMeta
    }

  suspend fun saveCustomMetadata(video: Video, updated: MovieMetadata) = withContext(Dispatchers.IO) {
    val pathKey = video.path.ifBlank { video.uri.toString() }
    memoryCache[pathKey] = updated
    runCatching {
      cacheFileForPath(pathKey).writeText(json.encodeToString(updated))
    }
  }

  private fun formatImageUrl(path: String?, isBackdrop: Boolean): String? {
    if (path.isNullOrBlank()) return null
    val trimmed = path.trim()
    return when {
      trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
      trimmed.startsWith("/") -> {
        val size = if (isBackdrop) "w1280" else "w500"
        "https://image.tmdb.org/t/p/$size$trimmed"
      }
      else -> {
        val size = if (isBackdrop) "w1280" else "w500"
        "https://image.tmdb.org/t/p/$size/$trimmed"
      }
    }
  }
}

