/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.domain.movies

import android.util.LruCache
import app.video.player.domain.media.model.Video
import app.video.player.utils.media.MediaInfoParser
import java.util.Locale

enum class VideoCategory {
  MOVIE,
  TV_EPISODE,
  NORMAL_VIDEO,
  AUDIO,
}

/**
 * Automatically distinguishes actual feature-length movies from normal videos
 * (camera clips, WhatsApp/Telegram forwards, screen recordings, reels/shorts,
 * trailers, tutorials/lectures, and TV episodes) using duration, orientation,
 * folder semantics, filename parsing, release tags, and persisted user/TMDB metadata.
 */
object MovieClassifier {
  private const val MIN_MOVIE_DURATION_MS = 40L * 60L * 1000L // 40 minutes
  private const val FEATURE_DURATION_MS = 60L * 60L * 1000L // 60 minutes

  private val classificationCache = LruCache<String, VideoCategory>(1024)

  // Camera, screen recorder, messaging, and mobile editor filename prefixes
  private val PERSONAL_FILE_PREFIX_REGEX = Regex(
    """^(?:vid|pxl|img|mvi|mov|dsc|svid|wa|fb_vid|lv_0|rpreplay|screencast|screen[ _-]?rec|screenrecorder|az[ _-]?recorder|xrecorder|inshot|vn\d*|capcut|kinemaster|youcut|vivavideo|filmora|powerdirector|doc-\d{8}|ptt-\d{8}|video[_-]?\d{4})[\s._-]""",
    RegexOption.IGNORE_CASE,
  )

  private val TIMESTAMP_ONLY_PREFIX_REGEX = Regex(
    """^(?:\d{8}[_-]\d{6}|\d{4}-\d{2}-\d{2}[\s_-]\d{2}[-.]\d{2})(?:\D|$)""",
  )

  private val WHATSAPP_FILE_REGEX = Regex("""^whatsapp[\s._-]video\b""", RegexOption.IGNORE_CASE)

  // Unambiguous non-feature trailers/samples/screen recordings even when movie title & year are present
  private val HARD_NON_MOVIE_KEYWORDS_REGEX = Regex(
    """\b(?:trailer|teaser|sneak[\s._-]?peek|featurette|behind[\s._-]?the[\s._-]?scenes|making[\s._-]?of|deleted[\s._-]?scenes?|gag[\s._-]?reel|bloopers?|sample|promo|screen[\s._-]?recording|screenrecord|screenrecorder|screencast|whatsapp[\s._-]?status)\b""",
    RegexOption.IGNORE_CASE,
  )

  // Non-movie content words (disqualify unless paired with BOTH a release year and cinema/release tags)
  private val SOFT_NON_MOVIE_KEYWORDS_REGEX = Regex(
    """\b(?:tutorial|course|lecture|webinar|meeting|zoom[\s._-]?recording|gameplay|walkthrough|playthrough|livestream|live[\s._-]?stream|podcast|vlog|unboxing|match[\s._-]?highlights|highlights|wedding|birthday|anniversary|music[\s._-]?video|lyrical|lyric[\s._-]?video|full[\s._-]?song|video[\s._-]?song|jukebox|remix|mashup|sermon|khutbah|presentation|shorts|reels?|tiktok)\b""",
    RegexOption.IGNORE_CASE,
  )

  // Strong movie release, source, codec, or distribution markers
  private val MOVIE_RELEASE_TAGS_REGEX = Regex(
    """\b(?:1080p|720p|2160p|4k|uhd|bluray|blu-ray|bdrip|brrip|web-dl|webdl|webrip|hdrip|dvdrip|dvdscr|hdcam|hdts|remux|imax|x264|x265|h\.?264|h\.?265|hevc|av1|10bit|hdr10\+?|hdr|dolby|atmos|dts|aac|ac3|eac3|truehd|yts|yify|rarbg|psa|esub|esubs|dual[\s._-]?audio|multi[\s._-]?audio|extended|unrated|directors?[\s._-]?cut|remastered|theatrical|dubbed)\b""",
    RegexOption.IGNORE_CASE,
  )

  private val HEX_HASH_REGEX = Regex("""^[0-9a-fA-F]{8,}$""")

  private val TV_FOLDER_REGEX = Regex(
    """^(?:season[\s._-]*\d+|series[\s._-]*\d+|s\d{1,2})$""",
    RegexOption.IGNORE_CASE,
  )

  private val NORMAL_VIDEO_FOLDER_EXACT = setOf(
    "camera",
    "dcim",
    "screenshots",
    "screenrecorder",
    "screen recorder",
    "screen recordings",
    "screen_recordings",
    "screenrecord",
    "whatsapp video",
    "whatsapp animated gifs",
    "whatsapp statuses",
    "statuses",
    "status",
    "instagram",
    "snapchat",
    "tiktok",
    "reels",
    "shorts",
    "youtube",
    "snaptube",
    "vidmate",
    "capcut",
    "inshot",
    "kinemaster",
    "vivavideo",
    "vn",
    "recordings",
    "record",
    "messenger",
    "facebook",
    "twitter",
    "reddit",
    "sharechat",
    "moj",
    "josh",
  )

  private val NORMAL_VIDEO_FOLDER_SUBSTRINGS = listOf(
    "screen record",
    "screen_record",
    "screenrecord",
    "whatsapp",
    "instagram",
    "snapchat",
    "tiktok",
    "snaptube",
    "vidmate",
    "capcut",
    "inshot",
    "kinemaster",
  )

  private val MOVIE_FOLDER_EXACT = setOf(
    "movies",
    "movie",
    "films",
    "film",
    "cinema",
    "hollywood",
    "bollywood",
    "tollywood",
    "kollywood",
    "mollywood",
    "4k movies",
    "hd movies",
    "hindi movies",
    "english movies",
    "anime movies",
    "feature films",
  )

  fun isActualMovie(video: Video, cachedMetadata: MovieMetadata? = null): Boolean =
    classify(video, cachedMetadata) == VideoCategory.MOVIE

  fun shouldFetchOnlinePoster(video: Video, cachedMetadata: MovieMetadata? = null): Boolean =
    when (classify(video, cachedMetadata)) {
      VideoCategory.MOVIE, VideoCategory.TV_EPISODE -> true
      VideoCategory.NORMAL_VIDEO, VideoCategory.AUDIO -> false
    }

  fun classify(video: Video, cachedMetadata: MovieMetadata? = null): VideoCategory {
    if (video.isAudio) return VideoCategory.AUDIO

    val override = cachedMetadata?.isMovieOverride
    if (override == true) return VideoCategory.MOVIE
    if (override == false) return VideoCategory.NORMAL_VIDEO

    val tmdbConfirmed = cachedMetadata?.tmdbId != null
    val cacheKey = buildString {
      append(video.path)
      append('|').append(video.displayName)
      append('|').append(video.duration)
      append('|').append(video.width).append('x').append(video.height).append('r').append(video.rotation)
      append('|').append(video.bucketDisplayName)
      append('|').append(tmdbConfirmed)
    }
    synchronized(classificationCache) { classificationCache.get(cacheKey) }?.let { return it }

    val result = classifyUncached(video, tmdbConfirmed)
    synchronized(classificationCache) { classificationCache.put(cacheKey, result) }
    return result
  }

  private fun classifyUncached(video: Video, tmdbConfirmed: Boolean): VideoCategory {
    val fileName = video.displayName.ifBlank { video.title }.trim()
    val parsed = MediaInfoParser.parse(fileName)

    // 1. TV series / episode detection
    if (parsed.type.equals("tv", ignoreCase = true) || parsed.season != null || parsed.episode != null) {
      return VideoCategory.TV_EPISODE
    }

    val immediateFolder = video.bucketDisplayName.trim().ifBlank {
      video.path.replace('\\', '/').substringBeforeLast('/', "").substringAfterLast('/')
    }
    if (immediateFolder.isNotBlank() && TV_FOLDER_REGEX.matches(immediateFolder)) {
      return VideoCategory.TV_EPISODE
    }

    // 2. Orientation check (portrait or 1:1 square videos are phone clips / reels / shorts, never movies)
    val swapAxes = video.rotation == 90 || video.rotation == 270
    val effectiveWidth = if (swapAxes) video.height else video.width
    val effectiveHeight = if (swapAxes) video.width else video.height
    if (effectiveWidth > 0 && effectiveHeight > 0 && effectiveHeight >= effectiveWidth) {
      return VideoCategory.NORMAL_VIDEO
    }

    // 3. Hard non-movie filename / folder disqualifiers
    if (PERSONAL_FILE_PREFIX_REGEX.containsMatchIn(fileName) ||
      TIMESTAMP_ONLY_PREFIX_REGEX.containsMatchIn(fileName) ||
      WHATSAPP_FILE_REGEX.containsMatchIn(fileName) ||
      HARD_NON_MOVIE_KEYWORDS_REGEX.containsMatchIn(fileName)
    ) {
      return VideoCategory.NORMAL_VIDEO
    }

    if (isNormalVideoFolder(immediateFolder) || hasNormalVideoAncestorFolder(video.path)) {
      return VideoCategory.NORMAL_VIDEO
    }

    // 4. Duration gate: real feature movies are at least 40 minutes long
    if (video.duration in 1L until MIN_MOVIE_DURATION_MS) {
      return VideoCategory.NORMAL_VIDEO
    }

    // 5. Soft non-movie content keywords unless strong cinema release metadata (year + release tags) exists
    val hasYear = !parsed.year.isNullOrBlank()
    val hasReleaseTags = MOVIE_RELEASE_TAGS_REGEX.containsMatchIn(fileName)
    if (SOFT_NON_MOVIE_KEYWORDS_REGEX.containsMatchIn(fileName) && !(hasYear && hasReleaseTags)) {
      return VideoCategory.NORMAL_VIDEO
    }

    // 6. Title sanity check
    val cleanTitle = parsed.title.trim()
    if (cleanTitle.count { it.isLetter() } < 2 || HEX_HASH_REGEX.matches(cleanTitle)) {
      return VideoCategory.NORMAL_VIDEO
    }

    // 7. Weighted positive classification
    var score = 0
    if (hasYear) score += 2
    if (hasReleaseTags) score += 2
    if (isExplicitMovieFolder(immediateFolder, video.path)) score += 2
    if (tmdbConfirmed) score += 2
    if (video.duration >= FEATURE_DURATION_MS) {
      score += 2
    } else if (video.duration >= MIN_MOVIE_DURATION_MS) {
      score += 1
    }

    return if (score >= 2) VideoCategory.MOVIE else VideoCategory.NORMAL_VIDEO
  }

  private fun isNormalVideoFolder(folderName: String): Boolean {
    if (folderName.isBlank()) return false
    val lower = folderName.lowercase(Locale.ROOT).trim()
    if (lower in NORMAL_VIDEO_FOLDER_EXACT) return true
    return NORMAL_VIDEO_FOLDER_SUBSTRINGS.any { lower.contains(it) }
  }

  private fun hasNormalVideoAncestorFolder(path: String): Boolean {
    if (path.isBlank()) return false
    val segments = path.replace('\\', '/').split('/')
    // Ignore the filename at the end
    val dirCount = (segments.size - 1).coerceAtLeast(0)
    for (i in 0 until dirCount) {
      if (isNormalVideoFolder(segments[i])) return true
    }
    return false
  }

  private fun isExplicitMovieFolder(immediateFolder: String, path: String): Boolean {
    if (immediateFolder.lowercase(Locale.ROOT).trim() in MOVIE_FOLDER_EXACT) return true
    val segments = path.replace('\\', '/').split('/')
    val dirCount = (segments.size - 1).coerceAtLeast(0)
    for (i in 0 until dirCount) {
      if (segments[i].lowercase(Locale.ROOT).trim() in MOVIE_FOLDER_EXACT) return true
    }
    return false
  }
}

