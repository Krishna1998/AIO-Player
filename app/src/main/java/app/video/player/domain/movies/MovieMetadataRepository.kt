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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
  val sourcePath: String? = null,
  val isMovieOverride: Boolean? = null,
  val mediaType: String? = null,
  val imdbId: String? = null,
  val episodeTitles: Map<String, String> = emptyMap(),
  val episodeStills: Map<String, String> = emptyMap(),
  val episodeOverviews: Map<String, String> = emptyMap(),
)

class MovieMetadataRepository(
  private val context: Context,
  private val wyzieSearchRepository: WyzieSearchRepository,
  private val json: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
  },
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val fetchSemaphore = Semaphore(6)

  suspend fun prefetchMetadata(videos: List<Video>) = withContext(Dispatchers.IO) {
    coroutineScope {
      videos.map { video ->
        async {
          fetchSemaphore.withPermit {
            runCatching { getOrFetchMetadata(video) }
          }
        }
      }.awaitAll()
    }
  }

  private val cacheDir = File(context.filesDir, "movie_metadata").apply { mkdirs() }
  private val memoryCache = ConcurrentHashMap<String, MovieMetadata>()
  private val _metadataRevision = MutableStateFlow(0)
  val metadataRevision: StateFlow<Int> = _metadataRevision.asStateFlow()

  init {
    scope.launch {
      preloadCachedMetadata()
    }
  }

  private fun preloadCachedMetadata() {
    val files = cacheDir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: return
    var loadedAny = false
    for (file in files) {
      runCatching {
        val meta = json.decodeFromString<MovieMetadata>(file.readText())
        val key = meta.sourcePath
        if (!key.isNullOrBlank()) {
          memoryCache.putIfAbsent(key, meta)
          loadedAny = true
        }
      }
    }
    if (loadedAny) {
      _metadataRevision.update { it + 1 }
    }
  }

  companion object {
    private const val TAG = "MovieMetadataRepo"

    fun sha256(input: String): String =
      MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray())
        .joinToString("") { "%02x".format(it) }

    fun episodeKey(season: Int, episode: Int): String =
      "S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}"
  }

  private fun cacheFileForPath(path: String): File =
    File(cacheDir, "m_${sha256(path)}.json")

  /** Fast main-thread-safe lookup without disk I/O. */
  fun getMemoryCachedMetadata(video: Video): MovieMetadata? {
    val pathKey = video.path.ifBlank { video.uri.toString() }
    return memoryCache[pathKey]
  }

  fun getCachedMetadata(video: Video): MovieMetadata? {
    val pathKey = video.path.ifBlank { video.uri.toString() }
    memoryCache[pathKey]?.let { return it }

    val file = cacheFileForPath(pathKey)
    if (file.isFile && file.length() > 0) {
      runCatching {
        val meta = json.decodeFromString<MovieMetadata>(file.readText())
        val enriched = if (meta.sourcePath.isNullOrBlank()) meta.copy(sourcePath = pathKey) else meta
        memoryCache[pathKey] = enriched
        return enriched
      }
    }
    return null
  }

  suspend fun getOrFetchMetadata(
    video: Video,
    forceRefresh: Boolean = false,
    seasonsToEnrich: List<Int> = emptyList(),
  ): MovieMetadata =
    withContext(Dispatchers.IO) {
      val pathKey = video.path.ifBlank { video.uri.toString() }
      val existing = getCachedMetadata(video)
      val parsed = MediaInfoParser.parse(video.displayName.ifBlank { video.title })
      val seriesInfo = MovieClassifier.extractSeriesEpisodeInfo(video)
      val isSeries = seriesInfo != null || parsed.type == "tv" || parsed.season != null || parsed.episode != null

      // If cached entry was matched as movie (or unknown pre-migration) for a series episode, re-resolve as "tv"
      val needsSeriesUpgrade = isSeries && existing != null &&
        existing.mediaType != "tv"

      if (!forceRefresh && existing != null && !needsSeriesUpgrade) {
        if (isSeries) {
          val targetSeasons = (seasonsToEnrich + listOfNotNull(seriesInfo?.season ?: parsed.season)).distinct()
          val hasMissingEpisodeData = targetSeasons.any { s ->
            existing.episodeTitles.keys.none { it.startsWith("S${s.toString().padStart(2, '0')}E") } &&
              existing.episodeOverviews.keys.none { it.startsWith("S${s.toString().padStart(2, '0')}E") }
          }
          if (hasMissingEpisodeData && existing.tmdbId != null) {
            return@withContext enrichSeriesEpisodes(video, existing, targetSeasons, forceRefresh = false)
          }
        }
        return@withContext existing
      }

      val cleanTitle = parsed.title.trim().ifBlank {
        MediaInfoParser.parse(video.title).title.trim().ifBlank {
          val name = video.displayName.substringBeforeLast('.')
          if (name.isNotBlank()) name else video.title
        }
      }
      val year = parsed.year
      val resolution = if (video.height > 0) {
        when {
          video.height >= 2160 || video.width >= 3840 -> "4K"
          video.height >= 1080 || video.width >= 1920 -> "1080p"
          video.height >= 720 || video.width >= 1280 -> "720p"
          else -> "${video.height}p"
        }
      } else {
        val match = Regex("(?i)\\b(2160p|4k|1080p|720p|480p|360p)\\b").find(video.displayName)
        match?.value?.uppercase() ?: "1080p"
      }

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
        sourcePath = pathKey,
        isMovieOverride = existing?.isMovieOverride,
        mediaType = if (isSeries) "tv" else "movie",
      )

      try {
        val expectedType = if (isSeries) "tv" else "movie"
        val matchResult = wyzieSearchRepository.findBestMediaMatch(cleanTitle, year, expectedMediaType = expectedType)
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
            voteAverage = tmdb.voteAverage?.toDouble(),
            tmdbId = tmdb.id,
            mediaType = tmdb.mediaType,
            imdbId = tmdb.imdbId,
          )
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed to fetch online movie metadata for $cleanTitle: ${e.message}")
      }

      if (isSeries && movieMeta.tmdbId != null) {
        val targetSeasons = (seasonsToEnrich + listOfNotNull(seriesInfo?.season ?: parsed.season)).distinct()
        movieMeta = fetchEpisodeDetailsInto(movieMeta, targetSeasons)
      }

      persistMetadata(pathKey, movieMeta)
      movieMeta
    }

  suspend fun enrichSeriesEpisodes(
    video: Video,
    metadata: MovieMetadata,
    seasons: List<Int>,
    forceRefresh: Boolean = false,
  ): MovieMetadata = withContext(Dispatchers.IO) {
    val pathKey = video.path.ifBlank { video.uri.toString() }
    val missingSeasons = if (forceRefresh) {
      seasons.distinct()
    } else {
      seasons.distinct().filter { s ->
        val prefix = "S${s.toString().padStart(2, '0')}E"
        metadata.episodeTitles.keys.none { it.startsWith(prefix) } &&
          metadata.episodeOverviews.keys.none { it.startsWith(prefix) }
      }
    }
    if (missingSeasons.isEmpty() || metadata.tmdbId == null) {
      return@withContext metadata
    }
    val enriched = fetchEpisodeDetailsInto(metadata, missingSeasons)
    persistMetadata(pathKey, enriched)
    enriched
  }

  private suspend fun fetchEpisodeDetailsInto(
    base: MovieMetadata,
    seasons: List<Int>,
  ): MovieMetadata {
    val tmdbId = base.tmdbId ?: return base
    val epTitles = base.episodeTitles.toMutableMap()
    val epStills = base.episodeStills.toMutableMap()
    val epOverviews = base.episodeOverviews.toMutableMap()

    for (seasonNum in seasons.distinct()) {
      runCatching {
        val episodes = wyzieSearchRepository.getSeasonEpisodes(
          id = tmdbId,
          season = seasonNum,
          imdbId = base.imdbId,
          seriesTitle = base.title,
          seriesYear = base.releaseYear,
        ).getOrNull().orEmpty()

        for (ep in episodes) {
          val key = episodeKey(seasonNum, ep.episode_number)
          ep.name?.takeIf { it.isNotBlank() }?.let { epTitles[key] = it }
          ep.still_path?.takeIf { it.isNotBlank() }?.let { path ->
            formatStillUrl(path)?.let { epStills[key] = it }
          }
          ep.overview?.takeIf { it.isNotBlank() }?.let { epOverviews[key] = it }
        }
      }
    }

    return base.copy(
      episodeTitles = epTitles,
      episodeStills = epStills,
      episodeOverviews = epOverviews,
    )
  }

  private fun persistMetadata(pathKey: String, meta: MovieMetadata) {
    memoryCache[pathKey] = meta
    runCatching {
      cacheFileForPath(pathKey).writeText(json.encodeToString(meta))
    }
    _metadataRevision.update { it + 1 }
  }

  suspend fun saveCustomMetadata(video: Video, updated: MovieMetadata) = withContext(Dispatchers.IO) {
    val pathKey = video.path.ifBlank { video.uri.toString() }
    val enriched = if (updated.sourcePath.isNullOrBlank()) updated.copy(sourcePath = pathKey) else updated
    persistMetadata(pathKey, enriched)
  }

  suspend fun setMovieOverride(video: Video, isMovie: Boolean) = withContext(Dispatchers.IO) {
    val pathKey = video.path.ifBlank { video.uri.toString() }
    val base = getCachedMetadata(video) ?: MovieMetadata(
      title = MediaInfoParser.parse(video.displayName.ifBlank { video.title }).title.ifBlank {
        video.displayName.substringBeforeLast('.').ifBlank { video.title }
      },
      durationFormatted = video.durationFormatted,
      sourcePath = pathKey,
    )
    val updated = base.copy(sourcePath = pathKey, isMovieOverride = isMovie)
    persistMetadata(pathKey, updated)
  }

  private fun formatImageUrl(path: String?, isBackdrop: Boolean): String? {
    if (path.isNullOrBlank()) return null
    val trimmed = path.trim()
    return when {
      trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
      trimmed.startsWith("/") -> {
        val size = if (isBackdrop) "w1280" else "w780"
        "https://image.tmdb.org/t/p/$size$trimmed"
      }
      else -> {
        val size = if (isBackdrop) "w1280" else "w780"
        "https://image.tmdb.org/t/p/$size/$trimmed"
      }
    }
  }

  private fun formatStillUrl(path: String?): String? {
    if (path.isNullOrBlank()) return null
    val trimmed = path.trim()
    return when {
      trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
      trimmed.startsWith("/") -> "https://image.tmdb.org/t/p/w500$trimmed"
      else -> "https://image.tmdb.org/t/p/w500/$trimmed"
    }
  }
}
