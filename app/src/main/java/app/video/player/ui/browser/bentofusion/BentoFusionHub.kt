/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.browser.bentofusion

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.video.player.domain.media.model.Video
import app.video.player.domain.recentlyplayed.repository.RecentlyPlayedRepository
import app.video.player.preferences.BrowserPreferences
import app.video.player.preferences.MediaLibraryType
import app.video.player.domain.thumbnail.ThumbnailRepository
import app.video.player.ui.browser.videolist.VideoWithPlaybackInfo
import app.video.player.ui.player.PlaybackSession
import app.video.player.ui.icons.Icons
import app.video.player.ui.icons.Icon
import app.video.player.utils.sort.SortUtils
import app.video.player.utils.storage.FileTypeUtils
import app.video.player.utils.storage.VideoScanUtils
import org.koin.compose.koinInject
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Resolves the original audio codec tag (e.g. FLAC, MP3, M4A, WAV, AAC, OPUS)
 * from file extension, mimeType, or embedded audio codec metadata.
 */
fun getAudioCodec(video: Video): String {
  val ext = video.path.substringAfterLast('.', "").uppercase()
  val mime = video.mimeType.lowercase()
  return when {
    ext in listOf("FLAC", "WAV", "MP3", "AAC", "M4A", "OPUS", "OGG", "ALAC", "WMA", "AIFF", "APE", "DSD", "DSF", "DFF", "AMR") -> ext
    mime.contains("flac") -> "FLAC"
    mime.contains("wav") || mime.contains("wave") -> "WAV"
    mime.contains("mp3") || mime.contains("mpeg") -> "MP3"
    mime.contains("aac") -> "AAC"
    mime.contains("m4a") -> "M4A"
    mime.contains("opus") -> "OPUS"
    mime.contains("ogg") || mime.contains("vorbis") -> "OGG"
    mime.contains("alac") -> "ALAC"
    mime.contains("wma") -> "WMA"
    mime.contains("aiff") -> "AIFF"
    mime.contains("ape") -> "APE"
    video.videoCodec.isNotBlank() -> video.videoCodec.uppercase()
    else -> "AUDIO"
  }
}

/**
 * Bento Fusion Hub Header
 * Integrates a liquid-glass Hero Spotlight Card (with dynamic audio visualizer / video resume)
 * and synchronized horizon rails for Cinema, Hi-Res Audio, and Recently Played.
 */
@Composable
fun BentoFusionHeader(
  videosWithInfo: List<VideoWithPlaybackInfo>,
  recentlyPlayedFilePath: String?,
  onPlayMedia: (Video, Double?) -> Unit,
  onFilterSelected: ((MediaLibraryType) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  if (videosWithInfo.isEmpty()) return

  val recentlyPlayedRepo = koinInject<RecentlyPlayedRepository>()
  val recentEntities by recentlyPlayedRepo.observeRecentlyPlayed(limit = 50).collectAsState(initial = emptyList())

  // 1. Separate video items and audio items
  val videoItems = remember(videosWithInfo) {
    videosWithInfo.filter { !it.video.isAudio }
  }
  val allAudioItems = remember(videosWithInfo) {
    videosWithInfo.filter { it.video.isAudio }
  }

  // 2. Sonic Soundstage Horizon: Show ALL songs sorted by title
  val sortedAudioByTitle = remember(allAudioItems) {
    allAudioItems.sortedWith { a, b ->
      val titleA = if (a.video.title.isNotBlank()) a.video.title else FileTypeUtils.stripExtension(a.video.displayName)
      val titleB = if (b.video.title.isNotBlank()) b.video.title else FileTypeUtils.stripExtension(b.video.displayName)
      SortUtils.NaturalOrderComparator.DEFAULT.compare(titleA, titleB)
    }
  }

  // 3. Recently Played Rail: Show recent 10 files (audio + video combined)
  val recentlyPlayedItems = remember(videosWithInfo, recentEntities, recentlyPlayedFilePath) {
    val itemsByPath = videosWithInfo.associateBy { it.video.path }
    val result = mutableListOf<VideoWithPlaybackInfo>()
    val addedPaths = mutableSetOf<String>()

    // Priority 1: Files from RecentlyPlayed database in exact order of recency
    for (entity in recentEntities) {
      val item = itemsByPath[entity.filePath]
      if (item != null && addedPaths.add(item.video.path)) {
        result.add(item)
        if (result.size >= 10) break
      }
    }

    // Priority 2: Actively played file path if not already added
    if (result.size < 10 && recentlyPlayedFilePath != null) {
      val lastPlayed = itemsByPath[recentlyPlayedFilePath]
      if (lastPlayed != null && addedPaths.add(lastPlayed.video.path)) {
        result.add(0, lastPlayed)
      }
    }

    // Priority 3: Media with recorded playback progress
    if (result.size < 10) {
      for (item in videosWithInfo) {
        if ((item.progressPercentage ?: 0f) > 0f && addedPaths.add(item.video.path)) {
          result.add(item)
          if (result.size >= 10) break
        }
      }
    }

    result.take(10)
  }

  // 4. Identify hero candidates: prioritizing active/recently played audio track for Audio Pulsing
  val playbackSessionState by PlaybackSession.state.collectAsState()
  val sessionItemPath = playbackSessionState.currentItem?.let { it.stableId ?: it.originalUri }
  val heroAudio = remember(sessionItemPath, recentlyPlayedItems, sortedAudioByTitle, recentlyPlayedFilePath) {
    if (sessionItemPath != null) {
      sortedAudioByTitle.firstOrNull { it.video.path == sessionItemPath || it.video.uri.toString() == sessionItemPath }
    } else null
  } ?: (if (recentlyPlayedFilePath != null) {
    sortedAudioByTitle.firstOrNull { it.video.path == recentlyPlayedFilePath }
  } else null)
    ?: recentlyPlayedItems.firstOrNull { it.video.isAudio }
    ?: sortedAudioByTitle.firstOrNull { (it.progressPercentage ?: 0f) > 0f }
    ?: sortedAudioByTitle.firstOrNull()

  val heroVideo = remember(videoItems, recentlyPlayedFilePath) {
    if (recentlyPlayedFilePath != null) {
      videoItems.firstOrNull { it.video.path == recentlyPlayedFilePath }
    } else null
  } ?: videoItems.firstOrNull { it.progressPercentage != null && it.progressPercentage > 0f && !it.isWatched }
    ?: videoItems.firstOrNull()

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(bottom = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // 1. Cinematic Video Horizon Rail (Going UP - Top of screen)
    if (videoItems.isNotEmpty()) {
      HorizonRailSection(
        title = "Cinematic Video Horizon",
        icon = "🎬",
        accentColor = MaterialTheme.colorScheme.primary,
        count = videoItems.size,
        onViewAll = { onFilterSelected?.invoke(MediaLibraryType.Video) },
      ) {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(
            items = videoItems.take(20),
            key = { it.video.path },
          ) { item ->
            BentoHorizonVideoCard(
              item = item,
              onClick = { onPlayMedia(item.video, null) },
            )
          }
        }
      }
    }

    // 2. Audio Pulsing Hero Spotlight Card (Going DOWN - Placed below video horizon rail)
    val heroToDisplay = heroAudio ?: (if (videoItems.isEmpty()) heroVideo else null)
    if (heroToDisplay != null) {
      val isCurrentlyInSession = remember(playbackSessionState.currentItem, heroToDisplay.video) {
        val curr = playbackSessionState.currentItem
        curr != null && (
          curr.originalUri == heroToDisplay.video.uri.toString() ||
          curr.originalUri == heroToDisplay.video.path ||
          curr.stableId == heroToDisplay.video.path ||
          curr.title == heroToDisplay.video.displayName
        )
      }
      val livePos = PlaybackSession.propInt["time-pos"].collectAsState().value?.toDouble()
      val savedPos = heroToDisplay.timeRemaining?.let { rem ->
        ((heroToDisplay.video.duration / 1000L) - rem).coerceAtLeast(0L).toDouble()
      } ?: if ((heroToDisplay.progressPercentage ?: 0f) > 0f && heroToDisplay.video.duration > 0L) {
        (((heroToDisplay.progressPercentage!! * heroToDisplay.video.duration) / 1000L).toLong()).toDouble()
      } else 0.0

      val resumePos = if (isCurrentlyInSession && livePos != null && livePos > 0.0) {
        livePos
      } else {
        savedPos
      }

      BentoHeroSpotlightCard(
        item = heroToDisplay,
        onClick = {
          if (isCurrentlyInSession) {
            if (playbackSessionState.paused) {
              PlaybackSession.setPropertyBoolean("pause", false)
            }
          } else {
            onPlayMedia(heroToDisplay.video, resumePos.takeIf { it > 0.0 })
          }
        },
      )
    }

    // 3. Sonic Soundstage Horizon Rail (ALL songs sorted by title)
    if (sortedAudioByTitle.isNotEmpty()) {
      HorizonRailSection(
        title = "Sonic Soundstage Horizon",
        icon = "🎵",
        accentColor = MaterialTheme.colorScheme.tertiary,
        count = sortedAudioByTitle.size,
        onViewAll = { onFilterSelected?.invoke(MediaLibraryType.Audio) },
      ) {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(
            items = sortedAudioByTitle,
            key = { it.video.path },
          ) { item ->
            BentoHorizonAudioCard(
              item = item,
              onClick = { onPlayMedia(item.video, null) },
            )
          }
        }
      }
    }

    // 4. Recently Played Rail (Recent 10 files audio + video combined)
    if (recentlyPlayedItems.isNotEmpty()) {
      HorizonRailSection(
        title = "Recently Played",
        icon = "🕒",
        accentColor = MaterialTheme.colorScheme.secondary,
        count = recentlyPlayedItems.size,
        onViewAll = { /* noop */ },
      ) {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          items(
            items = recentlyPlayedItems,
            key = { it.video.path },
          ) { item ->
            if (item.video.isAudio) {
              BentoHorizonAudioCard(
                item = item,
                onClick = { onPlayMedia(item.video, null) },
              )
            } else {
              BentoHorizonVideoCard(
                item = item,
                onClick = { onPlayMedia(item.video, null) },
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Dual-Horizon Rails component
 * Provides synchronized horizontal media rails:
 * 1. Upper Rail: Cinematic Video Horizon
 * 2. Lower Rail: Sonic Soundstage Horizon (ALL songs sorted by title)
 * 3. Bottom Rail: Recently Played (recent 10 files audio + video combined)
 */
@Composable
fun DualHorizonRails(
  videosWithInfo: List<VideoWithPlaybackInfo>,
  onPlayMedia: (Video, Double?) -> Unit = { v, _ -> },
  onFilterSelected: ((MediaLibraryType) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val recentlyPlayedRepo = koinInject<RecentlyPlayedRepository>()
  val recentEntities by recentlyPlayedRepo.observeRecentlyPlayed(limit = 50).collectAsState(initial = emptyList())

  val videoItems = remember(videosWithInfo) {
    videosWithInfo.filter { !it.video.isAudio }
  }
  val allAudioItems = remember(videosWithInfo) {
    videosWithInfo.filter { it.video.isAudio }
  }

  // Sonic Soundstage Horizon: ALL songs sorted by title
  val sortedAudioByTitle = remember(allAudioItems) {
    allAudioItems.sortedWith { a, b ->
      val titleA = if (a.video.title.isNotBlank()) a.video.title else FileTypeUtils.stripExtension(a.video.displayName)
      val titleB = if (b.video.title.isNotBlank()) b.video.title else FileTypeUtils.stripExtension(b.video.displayName)
      SortUtils.NaturalOrderComparator.DEFAULT.compare(titleA, titleB)
    }
  }

  // Recently Played: recent 10 files (audio + video combined)
  val recentlyPlayedItems = remember(videosWithInfo, recentEntities) {
    val itemsByPath = videosWithInfo.associateBy { it.video.path }
    val result = mutableListOf<VideoWithPlaybackInfo>()
    val addedPaths = mutableSetOf<String>()

    for (entity in recentEntities) {
      val item = itemsByPath[entity.filePath]
      if (item != null && addedPaths.add(item.video.path)) {
        result.add(item)
        if (result.size >= 10) break
      }
    }

    if (result.size < 10) {
      for (item in videosWithInfo) {
        if ((item.progressPercentage ?: 0f) > 0f && addedPaths.add(item.video.path)) {
          result.add(item)
          if (result.size >= 10) break
        }
      }
    }

    result.take(10)
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Upper Rail: Cinematic Video Horizon
    if (videoItems.isNotEmpty()) {
      HorizonRailSection(
        title = "Cinematic Video Horizon",
        icon = "🎬",
        accentColor = MaterialTheme.colorScheme.primary,
        count = videoItems.size,
        onViewAll = { onFilterSelected?.invoke(MediaLibraryType.Video) },
      ) {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(
            items = videoItems.take(20),
            key = { it.video.path },
          ) { item ->
            BentoHorizonVideoCard(
              item = item,
              onClick = { onPlayMedia(item.video, null) },
            )
          }
        }
      }
    }

    // Lower Rail: Sonic Soundstage Horizon (ALL songs sorted by title)
    if (sortedAudioByTitle.isNotEmpty()) {
      HorizonRailSection(
        title = "Sonic Soundstage Horizon",
        icon = "🎵",
        accentColor = MaterialTheme.colorScheme.tertiary,
        count = sortedAudioByTitle.size,
        onViewAll = { onFilterSelected?.invoke(MediaLibraryType.Audio) },
      ) {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(
            items = sortedAudioByTitle,
            key = { it.video.path },
          ) { item ->
            BentoHorizonAudioCard(
              item = item,
              onClick = { onPlayMedia(item.video, null) },
            )
          }
        }
      }
    }

    // Recently Played Rail: Recent 10 files (audio + video combined)
    if (recentlyPlayedItems.isNotEmpty()) {
      HorizonRailSection(
        title = "Recently Played",
        icon = "🕒",
        accentColor = MaterialTheme.colorScheme.secondary,
        count = recentlyPlayedItems.size,
        onViewAll = { /* noop */ },
      ) {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          items(
            items = recentlyPlayedItems,
            key = { it.video.path },
          ) { item ->
            if (item.video.isAudio) {
              BentoHorizonAudioCard(
                item = item,
                onClick = { onPlayMedia(item.video, null) },
              )
            } else {
              BentoHorizonVideoCard(
                item = item,
                onClick = { onPlayMedia(item.video, null) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun HorizonRailSection(
  title: String,
  icon: String,
  accentColor: Color,
  count: Int,
  onViewAll: () -> Unit,
  content: @Composable () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = "$icon $title",
          style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
          ),
          color = accentColor,
        )
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = accentColor.copy(alpha = 0.15f),
        ) {
          Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp,
            ),
            color = accentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
      }

      Text(
        text = "Swipe Deck →",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.SemiBold,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .clickable(onClick = onViewAll)
          .padding(horizontal = 6.dp, vertical = 2.dp),
      )
    }

    content()
  }
}

@Composable
private fun BentoHorizonVideoCard(
  item: VideoWithPlaybackInfo,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val video = item.video
  val progress = item.progressPercentage

  Card(
    modifier = modifier
      .width(180.dp)
      .clip(RoundedCornerShape(18.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // 16:9 Thumbnail Area
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(102.dp)
          .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
      ) {
        BentoMediaThumbnail(
          video = video,
          modifier = Modifier.fillMaxSize(),
        )

        // Top-left resolution or format badge
        val resBadge = when {
          video.width >= 3840 -> "4K HDR"
          video.width >= 1920 -> "1080p"
          video.width >= 1280 -> "720p"
          video.duration > 0 -> video.durationFormatted
          else -> "VIDEO"
        }
        Surface(
          modifier = Modifier
            .padding(6.dp)
            .align(Alignment.TopStart),
          shape = RoundedCornerShape(6.dp),
          color = Color.Black.copy(alpha = 0.72f),
          border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        ) {
          Text(
            text = resBadge,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
          )
        }

        // Bottom-right duration pill
        Surface(
          modifier = Modifier
            .padding(6.dp)
            .align(Alignment.BottomEnd),
          shape = RoundedCornerShape(6.dp),
          color = Color.Black.copy(alpha = 0.75f),
        ) {
          Text(
            text = video.durationFormatted,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.Medium,
            ),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
          )
        }

        // Bottom seek/progress indicator
        if (progress != null && progress > 0f) {
          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
              .fillMaxWidth()
              .height(3.dp)
              .align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Black.copy(alpha = 0.4f),
          )
        }
      }

      // Metadata Area
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = FileTypeUtils.stripExtension(video.displayName),
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
          ),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        val statusText = if (progress != null && progress > 0f) {
          if (item.isWatched) "Completed" else "Watched ${(progress * 100).toInt()}%"
        } else {
          video.durationFormatted
        }
        Text(
          text = statusText,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 10.sp,
          ),
          color = if (progress != null && progress > 0f && !item.isWatched) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun BentoHorizonAudioCard(
  item: VideoWithPlaybackInfo,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val video = item.video
  val progress = item.progressPercentage
  val originalCodec = remember(video) { getAudioCodec(video) }
  val remainingSeconds = item.timeRemaining ?: if (progress != null && progress > 0f && video.duration > 0L) {
    (((1f - progress) * video.duration) / 1000L).toLong().coerceAtLeast(0L)
  } else null
  val displayTime = remember(remainingSeconds, video.durationFormatted) {
    if (remainingSeconds != null && remainingSeconds > 0L) {
      "-${VideoScanUtils.formatDuration(remainingSeconds * 1000L)}"
    } else {
      video.durationFormatted
    }
  }

  Card(
    modifier = modifier
      .width(140.dp)
      .clip(RoundedCornerShape(18.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Artwork Area
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(115.dp)
          .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
      ) {
        BentoMediaThumbnail(
          video = video,
          modifier = Modifier.fillMaxSize(),
        )

        // Top-left Lossless / Original Audio Codec Badge
        Surface(
          modifier = Modifier
            .padding(6.dp)
            .align(Alignment.TopStart),
          shape = RoundedCornerShape(6.dp),
          color = Color.Black.copy(alpha = 0.72f),
          border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
        ) {
          Text(
            text = originalCodec,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp),
          )
        }

        // Bottom-right Duration / Remaining Time Pill
        Surface(
          modifier = Modifier
            .padding(6.dp)
            .align(Alignment.BottomEnd),
          shape = RoundedCornerShape(6.dp),
          color = Color.Black.copy(alpha = 0.75f),
        ) {
          Text(
            text = displayTime,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.Medium,
            ),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
          )
        }

        // Mini progress indicator on thumbnail if track is in progress
        if (progress != null && progress > 0f && !item.isWatched) {
          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
              .fillMaxWidth()
              .height(3.dp)
              .align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = Color.Transparent,
          )
        }
      }

      // Metadata Area
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        val (artist, title) = SortUtils.getAudioArtistAndTitle(video)
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
          ),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Text(
          text = if (artist.isNotBlank() && !artist.equals("Unknown Artist", ignoreCase = true)) {
            "$artist • ${if (video.durationFormatted.isNotBlank()) video.durationFormatted else "Audio"}"
          } else {
            if (video.durationFormatted.isNotBlank()) video.durationFormatted else "Audio Track"
          },
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 10.sp,
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

/**
 * Hero Spotlight Card displaying active/resuming video or pulsing audio track
 * with dynamic visualizer waves and specular highlights.
 */
@Composable
private fun BentoHeroSpotlightCard(
  item: VideoWithPlaybackInfo,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isAudio = item.video.isAudio
  val progress = item.progressPercentage ?: 0f

  val playbackSessionState by PlaybackSession.state.collectAsState()
  val livePositionSec by PlaybackSession.propInt["time-pos"].collectAsState()
  val liveDurationSec by PlaybackSession.propInt["duration"].collectAsState()

  val isCurrentlyPlayingInSession = remember(playbackSessionState.currentItem, item.video) {
    val curr = playbackSessionState.currentItem
    curr != null && (
      curr.originalUri == item.video.uri.toString() ||
      curr.originalUri == item.video.path ||
      curr.stableId == item.video.path ||
      curr.title == item.video.displayName
    )
  }

  val originalCodec = remember(item.video) {
    if (isAudio) getAudioCodec(item.video) else item.video.resolution.ifBlank { "HD" }
  }

  // Real-time synced remaining time:
  val remainingSec = if (isCurrentlyPlayingInSession && livePositionSec != null) {
    val totalDur = liveDurationSec?.takeIf { it > 0 } ?: (item.video.duration / 1000L).toInt()
    (totalDur - livePositionSec!!).coerceAtLeast(0).toLong()
  } else {
    item.timeRemaining ?: if (progress > 0f && item.video.duration > 0L) {
      (((1f - progress) * item.video.duration) / 1000L).toLong().coerceAtLeast(0L)
    } else {
      (item.video.duration / 1000L).coerceAtLeast(0L)
    }
  }

  val remainingFormatted = if (remainingSec > 0L) {
    "-${VideoScanUtils.formatDuration(remainingSec * 1000L)}"
  } else {
    item.video.durationFormatted
  }

  val infiniteTransition = rememberInfiniteTransition(label = "bentoPulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "pulseScale",
  )

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .clip(RoundedCornerShape(24.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    border = BorderStroke(
      width = 1.dp,
      color = if (isAudio) {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
      } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
      },
    ),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.linearGradient(
            colors = listOf(
              (if (isAudio) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary).copy(alpha = 0.12f),
              MaterialTheme.colorScheme.surfaceContainerLow,
              MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
          ),
        ),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Top Header Row: Status Badge & Format Chip
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (isAudio) {
              MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
            } else {
              MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            },
            border = BorderStroke(
              width = 1.dp,
              color = if (isAudio) {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
              } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
              },
            ),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .scale(pulseScale)
                  .clip(CircleShape)
                  .background(if (isAudio) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary),
              )
              Text(
                text = if (isAudio) "AUDIO PULSING" else if (progress > 0f) "RESUME PLAYING" else "SPOTLIGHT",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp,
                ),
                color = if (isAudio) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
              )
            }
          }

          Text(
            text = if (isAudio) {
              "$originalCodec • $remainingFormatted remaining"
            } else {
              "$originalCodec • ${item.video.durationFormatted}"
            },
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
          )
        }

        // Middle Row: Thumbnail Preview & Metadata
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Media Thumbnail Box
          Box {
            BentoMediaThumbnail(
              video = item.video,
              modifier = Modifier
                .size(width = if (isAudio) 70.dp else 108.dp, height = 66.dp)
                .clip(RoundedCornerShape(16.dp)),
            )
            if (isAudio) {
              Surface(
                modifier = Modifier
                  .padding(4.dp)
                  .align(Alignment.BottomStart),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.75f),
              ) {
                Text(
                  text = originalCodec,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                  ),
                  color = MaterialTheme.colorScheme.tertiary,
                  modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                )
              }
            }
          }

          // Information & Visualizer
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            val (artist, title) = if (isAudio) {
              SortUtils.getAudioArtistAndTitle(item.video)
            } else {
              Pair("", if (item.video.title.isNotBlank()) item.video.title else FileTypeUtils.stripExtension(item.video.displayName))
            }

            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )

            Text(
              text = if (isAudio) {
                if (artist.isNotBlank() && !artist.equals("Unknown Artist", ignoreCase = true)) {
                  "$artist • ${item.video.bucketDisplayName.ifBlank { "Lossless Audio" }}"
                } else {
                  item.video.bucketDisplayName.ifBlank { "Lossless Audio" }
                }
              } else {
                "${item.video.sizeFormatted} • ${item.video.bucketDisplayName}"
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )

            // One UI 9 Fluid Wavy Waveform Visualizer (No seek bar in audio pulsing)
            if (isAudio) {
              OneUi9WaveformVisualizer(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(24.dp)
                  .padding(top = 2.dp),
                primaryColor = MaterialTheme.colorScheme.tertiary,
                secondaryColor = MaterialTheme.colorScheme.primary,
              )
            } else if (progress > 0f) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                LinearProgressIndicator(
                  progress = { progress },
                  modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp)),
                  color = MaterialTheme.colorScheme.primary,
                  trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )
                Text(
                  text = "${(progress * 100).toInt()}%",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Samsung One UI 9 / 9.5 style fluid wavy waveform visualizer
 * Features 3-tier multi-depth aurora liquid glass ribbons:
 * 1. Ambient Deep Bass Veil (soft background rolling harmonic wave)
 * 2. Harmonic Mid Veil (vibrant gradient curtain down to baseline)
 * 3. Lead Luminous Crest with Specular Highlight Core (crisp foreground wave with white liquid reflection)
 */
@Composable
fun OneUi9WaveformVisualizer(
  modifier: Modifier = Modifier,
  primaryColor: Color = MaterialTheme.colorScheme.tertiary,
  secondaryColor: Color = MaterialTheme.colorScheme.primary,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "oneUiWaveTransition")
  val phase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2.0 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(2400, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "oneUiWavePhase",
  )

  val filledPath = remember { Path() }
  val contourPath = remember { Path() }

  Canvas(modifier = modifier) {
    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) return@Canvas

    val baselineY = height - 1.5.dp.toPx()
    val availableLift = (baselineY - 1.dp.toPx()).coerceAtLeast(0f)
    val twoPi = (2.0 * PI).toFloat()
    val step = 2.dp.toPx().coerceAtLeast(1f)
    val taperInset = 16.dp.toPx().coerceAtMost(width / 4f)

    fun smoothstep(v: Float): Float {
      val c = v.coerceIn(0f, 1f)
      return c * c * (3f - 2f * c)
    }

    fun drawWaveRibbon(
      wavelength: Float,
      lift: Float,
      phaseOffset: Float,
      fillTopColor: Color,
      fillBottomColor: Color,
      strokeColor: Color,
      strokeWidth: Float,
      glowColor: Color? = null,
      glowWidth: Float = 0f,
      hasSpecularHighlight: Boolean = false,
    ) {
      if (lift <= 0.5f) return

      filledPath.reset()
      contourPath.reset()

      fun ribbonY(x: Float): Float {
        val edgeIn = if (taperInset > 0f) smoothstep(x / taperInset) else 1f
        val edgeOut = if (taperInset > 0f) smoothstep((width - x) / taperInset) else 1f
        val envelope = edgeIn * edgeOut
        val undulation = 0.50f + 0.50f * sin((x / wavelength) * twoPi - phaseOffset)
        return baselineY - lift * undulation * envelope
      }

      val startY = ribbonY(0f)
      filledPath.moveTo(0f, baselineY)
      filledPath.lineTo(0f, startY)
      contourPath.moveTo(0f, startY)

      var x = 0f
      while (x < width) {
        x = min(x + step, width)
        val y = ribbonY(x)
        filledPath.lineTo(x, y)
        contourPath.lineTo(x, y)
      }
      filledPath.lineTo(width, baselineY)
      filledPath.close()

      // 1. One UI 9 Frosted Glass Gradient Body Fill (curtain down to baseline)
      drawPath(
        path = filledPath,
        brush = Brush.verticalGradient(
          colors = listOf(fillTopColor, fillBottomColor),
          startY = baselineY - lift,
          endY = baselineY,
        ),
      )

      // 2. Diffused Luminous Glow Halo along wave crests
      if (glowColor != null && glowWidth > 0f) {
        drawPath(
          path = contourPath,
          color = glowColor,
          style = Stroke(
            width = glowWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
          ),
        )
      }

      // 3. Crisp Luminous Crest Contour Stroke
      drawPath(
        path = contourPath,
        color = strokeColor,
        style = Stroke(
          width = strokeWidth,
          cap = StrokeCap.Round,
          join = StrokeJoin.Round,
        ),
      )

      // 4. Foreground White Specular Highlight Core for crisp liquid glass reflection
      if (hasSpecularHighlight) {
        drawPath(
          path = contourPath,
          color = Color.White.copy(alpha = 0.75f),
          style = Stroke(
            width = 1.0.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
          ),
        )
      }
    }

    // Layer 1: Ambient Deep Bass Veil (soft background rolling harmonic wave)
    drawWaveRibbon(
      wavelength = 92.dp.toPx(),
      lift = availableLift * 0.95f,
      phaseOffset = phase * 1.0f,
      fillTopColor = secondaryColor.copy(alpha = 0.20f),
      fillBottomColor = secondaryColor.copy(alpha = 0.02f),
      strokeColor = secondaryColor.copy(alpha = 0.40f),
      strokeWidth = 1.2.dp.toPx(),
      glowColor = secondaryColor.copy(alpha = 0.20f),
      glowWidth = 4.dp.toPx(),
    )

    // Layer 2: Harmonic Mid Veil (vibrant gradient curtain)
    drawWaveRibbon(
      wavelength = 62.dp.toPx(),
      lift = availableLift * 0.80f,
      phaseOffset = phase * 1.45f + 1.2f,
      fillTopColor = primaryColor.copy(alpha = 0.35f),
      fillBottomColor = secondaryColor.copy(alpha = 0.05f),
      strokeColor = primaryColor.copy(alpha = 0.75f),
      strokeWidth = 1.6.dp.toPx(),
      glowColor = primaryColor.copy(alpha = 0.30f),
      glowWidth = 5.5.dp.toPx(),
    )

    // Layer 3: Vibrant Lead Luminous Wave with Specular Core (crisp foreground crest)
    drawWaveRibbon(
      wavelength = 42.dp.toPx(),
      lift = availableLift * 0.65f,
      phaseOffset = phase * 2.1f + 2.4f,
      fillTopColor = primaryColor.copy(alpha = 0.55f),
      fillBottomColor = primaryColor.copy(alpha = 0.12f),
      strokeColor = primaryColor.copy(alpha = 0.95f),
      strokeWidth = 2.0.dp.toPx(),
      glowColor = primaryColor.copy(alpha = 0.45f),
      glowWidth = 7.dp.toPx(),
      hasSpecularHighlight = true,
    )
  }
}

/**
 * Compact Bento quick-action card for Cinema and Hi-Res Audio
 */
@Composable
private fun BentoQuickTile(
  title: String,
  subtitle: String,
  badgeText: String,
  badgeIcon: String,
  badgeColor: Color,
  modifier: Modifier = Modifier,
  progress: Float? = null,
  isAudio: Boolean = false,
  onClick: () -> Unit,
) {
  Card(
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.2f)),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "$badgeIcon $badgeText",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = badgeColor,
        )
        if (isAudio) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = badgeColor.copy(alpha = 0.15f),
          ) {
            Text(
              text = "FLAC",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
              color = badgeColor,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            )
          }
        }
      }

      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      if (progress != null && progress > 0f) {
        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .padding(top = 2.dp)
            .clip(RoundedCornerShape(999.dp)),
          color = badgeColor,
          trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        )
      }
    }
  }
}

/**
 * Thumbnail loader for Bento cards
 */
@Composable
private fun BentoMediaThumbnail(
  video: Video,
  modifier: Modifier = Modifier,
) {
  val thumbnailRepository = koinInject<ThumbnailRepository>()
  val density = LocalDensity.current
  val thumbWidthPx = with(density) { 120.dp.roundToPx() }
  val thumbHeightPx = with(density) { 70.dp.roundToPx() }

  var thumbnail by remember(video.path) {
    mutableStateOf<Bitmap?>(thumbnailRepository.peekThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx))
  }

  LaunchedEffect(video.path) {
    if (thumbnail == null) {
      val loaded = thumbnailRepository.getThumbnail(video, thumbWidthPx, thumbHeightPx)
      if (loaded != null) thumbnail = loaded
    }
  }

  Box(
    modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
    contentAlignment = Alignment.Center,
  ) {
    if (thumbnail != null) {
      Image(
        bitmap = thumbnail!!.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
      )
    } else {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.radialGradient(
              colors = listOf(
                (if (video.isAudio) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary).copy(alpha = 0.3f),
                MaterialTheme.colorScheme.surfaceContainerHighest,
              ),
            ),
          ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = if (video.isAudio) Icons.RoundedFilled.Audiotrack else Icons.RoundedFilled.PlayArrow,
          contentDescription = null,
          modifier = Modifier.size(28.dp),
          tint = if (video.isAudio) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        )
      }
    }

    // Play button specular overlay
    Box(
      modifier = Modifier
        .size(28.dp)
        .clip(CircleShape)
        .background(Color.Black.copy(alpha = 0.45f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.RoundedFilled.PlayArrow,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = Color.White,
      )
    }
  }
}

