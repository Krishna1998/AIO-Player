/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.browser.bentofusion

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.compositeOver
import app.video.player.preferences.AppearancePreferences
import app.video.player.ui.theme.AppTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.video.player.domain.media.model.Video
import app.video.player.domain.recentlyplayed.repository.RecentlyPlayedRepository
import android.net.Uri
import app.video.player.preferences.BrowserPreferences
import app.video.player.preferences.MediaLibraryType
import app.video.player.preferences.preference.collectAsState
import app.video.player.domain.thumbnail.ThumbnailRepository
import app.video.player.ui.browser.videolist.VideoWithPlaybackInfo
import app.video.player.ui.browser.videolist.videoPlaybackIdentifiers
import app.video.player.ui.player.PlaybackIdentity
import app.video.player.ui.player.PlaybackItem
import app.video.player.ui.player.PlaybackSession
import app.video.player.ui.player.isDefinitelyAudioOnly
import app.video.player.ui.player.MediaPlaybackService
import app.video.player.ui.player.PlayerActivity
import app.video.player.utils.media.MediaUtils
import app.video.player.ui.icons.Icons
import app.video.player.ui.icons.Icon
import app.video.player.utils.history.RecentlyPlayedOps
import app.video.player.utils.sort.SortUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import app.video.player.utils.storage.FileTypeUtils
import app.video.player.utils.storage.VideoScanUtils
import org.koin.compose.koinInject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private fun isAudioPlaybackItem(item: PlaybackItem?): Boolean {
  if (item == null) return false
  if (item.audiobook != null) return true
  if (item.isDefinitelyAudioOnly()) return true
  if (item.mimeType?.startsWith("audio/", ignoreCase = true) == true) return true
  val candidates = sequenceOf(item.originalUri, item.playableUri, item.title, item.stableId).filterNotNull()
  return candidates.any { s ->
    val ext = s.substringAfterLast('.', "").lowercase()
    ext in FileTypeUtils.AUDIO_EXTENSIONS ||
      s.contains("/audio/", ignoreCase = true) ||
      s.contains("audio%3A", ignoreCase = true)
  }
}

private fun matchesPlaybackItem(video: Video, item: PlaybackItem?): Boolean {
  if (item == null) return false
  val itemUri = item.originalUri
  val playable = item.playableUri
  val stableId = item.stableId
  val videoUriStr = video.uri.toString()
  val videoPath = video.path

  if (videoPlaybackIdentifiers(video).any { id -> id == stableId || id == itemUri || id == playable }) {
    return true
  }

  if (itemUri.isNotBlank()) {
    if (itemUri == videoUriStr || itemUri == videoPath || itemUri.endsWith(videoPath) || videoPath.endsWith(itemUri)) return true
  }
  if (playable.isNotBlank()) {
    if (playable == videoUriStr || playable == videoPath || playable.endsWith(videoPath) || videoPath.endsWith(playable)) return true
  }
  if (stableId.isNotBlank()) {
    if (stableId == videoPath || stableId == videoUriStr) return true
  }
  if (!item.title.isNullOrBlank()) {
    val cleanName = FileTypeUtils.stripExtension(video.displayName)
    if (item.title.equals(video.displayName, ignoreCase = true) ||
        item.title.equals(video.title, ignoreCase = true) ||
        item.title.equals(cleanName, ignoreCase = true)) {
      return true
    }
  }
  return false
}

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

  val context = LocalContext.current

  val recentlyPlayedRepo = koinInject<RecentlyPlayedRepository>()
  val recentEntities by recentlyPlayedRepo.observeRecentlyPlayed(limit = 50).collectAsState(initial = emptyList())
  val browserPreferences = koinInject<BrowserPreferences>()
  val minVideoMinutes by browserPreferences.minimumVideoHorizonDurationMinutes.collectAsState()
  val minVideoDurationMs = remember(minVideoMinutes) { (minVideoMinutes * 60 * 1000L).coerceAtLeast(0L) }

  // 1. Separate video items and audio items (applying video duration cut-out)
  val videoItems = remember(videosWithInfo, minVideoDurationMs) {
    videosWithInfo.filter { !it.video.isAudio && (minVideoDurationMs <= 0L || it.video.duration >= minVideoDurationMs) }
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

  // Live session tracking (active/auto-changed song or video)
  val playbackSessionState by PlaybackSession.state.collectAsState()
  val currentSessionItem = playbackSessionState.currentItem
  val isSessionPlayingAudio = remember(currentSessionItem) {
    isAudioPlaybackItem(currentSessionItem)
  }

  val activeAudioFromSession = remember(currentSessionItem, isSessionPlayingAudio, sortedAudioByTitle, allAudioItems, videosWithInfo) {
    if (currentSessionItem != null && isSessionPlayingAudio) {
      sortedAudioByTitle.firstOrNull { matchesPlaybackItem(it.video, currentSessionItem) }
        ?: allAudioItems.firstOrNull { matchesPlaybackItem(it.video, currentSessionItem) }
        ?: videosWithInfo.firstOrNull { it.video.isAudio && matchesPlaybackItem(it.video, currentSessionItem) }
    } else null
  }

  val activeVideoFromSession = remember(currentSessionItem, isSessionPlayingAudio, videoItems, videosWithInfo) {
    if (currentSessionItem != null && !isSessionPlayingAudio) {
      videoItems.firstOrNull { matchesPlaybackItem(it.video, currentSessionItem) }
        ?: videosWithInfo.firstOrNull { !it.video.isAudio && matchesPlaybackItem(it.video, currentSessionItem) }
    } else null
  }

  val syntheticAudioItem = remember(currentSessionItem, isSessionPlayingAudio, activeAudioFromSession) {
    if (currentSessionItem != null && isSessionPlayingAudio && activeAudioFromSession == null) {
      val uri = runCatching { Uri.parse(currentSessionItem.playableUri.ifBlank { currentSessionItem.originalUri }) }.getOrNull() ?: Uri.EMPTY
      val displayTitle = currentSessionItem.title?.takeIf { it.isNotBlank() } ?: currentSessionItem.originalUri.substringAfterLast('/')
      val durSec = currentSessionItem.durationSeconds?.toLong() ?: 0L
      val syntheticVideo = Video(
        id = currentSessionItem.queueEntryId,
        title = displayTitle,
        displayName = displayTitle,
        path = currentSessionItem.originalUri,
        uri = uri,
        duration = durSec * 1000L,
        durationFormatted = if (durSec > 0) VideoScanUtils.formatDuration(durSec * 1000L) else "",
        size = 0L,
        sizeFormatted = "",
        dateModified = 0L,
        dateAdded = 0L,
        mimeType = currentSessionItem.mimeType ?: "audio/*",
        bucketId = "current_session",
        bucketDisplayName = "Now Playing",
        width = currentSessionItem.videoWidth,
        height = currentSessionItem.videoHeight,
        fps = 0f,
        resolution = "",
        isAudio = true,
        artworkUrl = currentSessionItem.artworkUri,
        artist = currentSessionItem.artist ?: "",
      )
      VideoWithPlaybackInfo(syntheticVideo)
    } else null
  }

  // Live played file (automatically updates whenever a track is played or auto-advances to next)
  val livePlayingMediaItem = activeAudioFromSession ?: syntheticAudioItem ?: activeVideoFromSession

  val lastPlayedAudioPathPref by browserPreferences.lastPlayedAudioPath.collectAsState()
  var rememberedAudioPath by remember { mutableStateOf<String?>(null) }

  // Automatically save live played file to RecentlyPlayed repository whenever song changes (only for audio!)
  LaunchedEffect(currentSessionItem?.stableId, currentSessionItem?.originalUri, isSessionPlayingAudio) {
    val item = currentSessionItem ?: return@LaunchedEffect
    if (!isSessionPlayingAudio) return@LaunchedEffect
    val path = item.originalUri.takeIf { it.isNotBlank() } ?: item.playableUri
    if (path.isNotBlank()) {
      val cleanPath = if (path.startsWith("file://")) path.removePrefix("file://") else path
      rememberedAudioPath = cleanPath
      if (browserPreferences.lastPlayedAudioPath.get() != cleanPath) {
        browserPreferences.lastPlayedAudioPath.set(cleanPath)
      }
      val title = item.title?.takeIf { it.isNotBlank() } ?: cleanPath.substringAfterLast('/')
      val durationMs = (item.durationSeconds ?: 0) * 1000L
      withContext(Dispatchers.IO) {
        runCatching {
          RecentlyPlayedOps.addRecentlyPlayed(
            filePath = cleanPath,
            fileName = title,
            videoTitle = item.title,
            duration = durationMs,
            width = item.videoWidth,
            height = item.videoHeight,
            launchSource = "live_playback",
            artworkUrl = item.artworkUri,
          )
        }
      }
    }
  }

  // 3. Recently Played: Show recent 10 files (audio + video combined, applying video duration cut-out)
  val recentlyPlayedItems = remember(
    videosWithInfo,
    recentEntities,
    recentlyPlayedFilePath,
    livePlayingMediaItem,
    minVideoDurationMs,
  ) {
    val itemsByPath = videosWithInfo.associateBy { it.video.path }
    val result = mutableListOf<VideoWithPlaybackInfo>()
    val addedPaths = mutableSetOf<String>()

    fun canInclude(item: VideoWithPlaybackInfo): Boolean {
      return item.video.isAudio || minVideoDurationMs <= 0L || item.video.duration >= minVideoDurationMs
    }

    // Priority 1: LIVE currently played file (e.g. when track starts or automatically changes song!)
    if (livePlayingMediaItem != null && canInclude(livePlayingMediaItem)) {
      result.add(livePlayingMediaItem)
      addedPaths.add(livePlayingMediaItem.video.path)
    }

    // Priority 2: Files from RecentlyPlayed database in exact order of recency
    for (entity in recentEntities) {
      val item = itemsByPath[entity.filePath]
      if (item != null && canInclude(item) && addedPaths.add(item.video.path)) {
        result.add(item)
        if (result.size >= 10) break
      }
    }

    // Priority 3: Actively played file path if not already added
    if (result.size < 10 && recentlyPlayedFilePath != null) {
      val lastPlayed = itemsByPath[recentlyPlayedFilePath]
      if (lastPlayed != null && canInclude(lastPlayed) && addedPaths.add(lastPlayed.video.path)) {
        result.add(lastPlayed)
      }
    }

    // Priority 4: Media with recorded playback progress
    if (result.size < 10) {
      for (item in videosWithInfo) {
        if ((item.progressPercentage ?: 0f) > 0f && canInclude(item) && addedPaths.add(item.video.path)) {
          result.add(item)
          if (result.size >= 10) break
        }
      }
    }

    result.take(10)
  }

  // 4. Identify hero candidates: prioritizing active/recently played audio track for Audio Pulsing
  // Persistent tracking guarantees Audio Pulsing does NOT reset to the first song when playing a video!
  val heroAudio = activeAudioFromSession
    ?: syntheticAudioItem
    ?: rememberedAudioPath?.let { path ->
      sortedAudioByTitle.firstOrNull { it.video.path == path || it.video.uri.toString() == path }
        ?: allAudioItems.firstOrNull { it.video.path == path || it.video.uri.toString() == path }
    }
    ?: lastPlayedAudioPathPref.takeIf { it.isNotBlank() }?.let { path ->
      sortedAudioByTitle.firstOrNull { it.video.path == path || it.video.uri.toString() == path }
        ?: allAudioItems.firstOrNull { it.video.path == path || it.video.uri.toString() == path }
    }
    ?: (if (recentlyPlayedFilePath != null) {
      sortedAudioByTitle.firstOrNull { it.video.path == recentlyPlayedFilePath || it.video.uri.toString() == recentlyPlayedFilePath }
    } else null)
    ?: recentEntities.firstNotNullOfOrNull { entity ->
      val p = entity.filePath
      sortedAudioByTitle.firstOrNull { it.video.path == p || it.video.uri.toString() == p }
    }
    ?: recentlyPlayedItems.firstOrNull { it.video.isAudio }
    ?: sortedAudioByTitle.firstOrNull { (it.progressPercentage ?: 0f) > 0f }
    ?: sortedAudioByTitle.firstOrNull()

  fun playAudioInBackground(targetVideo: Video, startPos: Double?) {
    val audioQueue = sortedAudioByTitle.map { item ->
      val v = item.video
      PlaybackItem.fromUri(
        uri = v.uri.toString(),
        stableId = if (app.video.player.BuildConfig.SCOPED_STORAGE_ONLY && v.uri.scheme == "content") {
          videoPlaybackIdentifiers(v).first()
        } else PlaybackIdentity.forLocalPath(v.path),
        title = if (v.title.isNotBlank()) v.title else FileTypeUtils.stripExtension(v.displayName),
        artist = v.artist,
        mimeType = v.mimeType.ifBlank { "audio/*" },
        artworkUri = v.artworkUrl,
        durationSeconds = (v.duration / 1000L).toInt().takeIf { it > 0 },
        videoWidth = v.width,
        videoHeight = v.height,
      )
    }
    val targetIdx = sortedAudioByTitle.indexOfFirst { it.video.path == targetVideo.path }.coerceAtLeast(0)
    rememberedAudioPath = targetVideo.path
    browserPreferences.lastPlayedAudioPath.set(targetVideo.path)
    MediaUtils.playInMiniPlayer(context, audioQueue, targetIdx, startPositionSeconds = startPos?.takeIf { it > 0.0 })
  }

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
    // 1. Video/Movie Rail
    if (videoItems.isNotEmpty()) {
      HorizonRailSection(
        title = "Video/Movie",
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

    // 2. Music Rail (ALL songs sorted by title)
    if (sortedAudioByTitle.isNotEmpty()) {
      HorizonRailSection(
        title = "Music",
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
              onClick = {
                playAudioInBackground(item.video, null)
              },
            )
          }
        }
      }
    }

    // 3. Audio Pulsing Hero Spotlight Card
    val heroToDisplay = heroAudio ?: (if (videoItems.isEmpty()) heroVideo else null)
    if (heroToDisplay != null) {
      val isCurrentlyInSession = remember(currentSessionItem, heroToDisplay.video) {
        matchesPlaybackItem(heroToDisplay.video, currentSessionItem)
      }
      val isPaused by PlaybackSession.propBoolean["pause"].collectAsState()
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
          if (heroToDisplay.video.isAudio) {
            if (isCurrentlyInSession) {
              val currentPaused = isPaused ?: false
              PlaybackSession.setPropertyBoolean("pause", !currentPaused)
            } else {
              playAudioInBackground(heroToDisplay.video, resumePos.takeIf { it > 0.0 })
            }
          } else {
            onPlayMedia(heroToDisplay.video, resumePos.takeIf { it > 0.0 })
          }
        },
        onLongClick = {
          if (heroToDisplay.video.isAudio) {
            val intent = Intent(context, PlayerActivity::class.java).apply {
              action = MediaPlaybackService.ACTION_OPEN_PLAYER
              putExtra("is_audio", true)
              putExtra("internal_launch", true)
              putExtra("launch_source", "audio_pulsing")
              flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            try {
              context.startActivity(intent)
            } catch (_: Exception) {}
          }
        },
        onPrevious = {
          if (isCurrentlyInSession) {
            val prev = PlaybackSession.playPrevious()
            if (prev == null && sortedAudioByTitle.isNotEmpty()) {
              val currIdx = sortedAudioByTitle.indexOfFirst { matchesPlaybackItem(it.video, currentSessionItem) }
              if (currIdx >= 0) {
                val prevIdx = (currIdx - 1 + sortedAudioByTitle.size) % sortedAudioByTitle.size
                playAudioInBackground(sortedAudioByTitle[prevIdx].video, null)
              }
            }
          } else if (sortedAudioByTitle.isNotEmpty()) {
            val currIdx = sortedAudioByTitle.indexOfFirst { it.video.path == heroToDisplay.video.path }
            val prevIdx = if (currIdx >= 0) (currIdx - 1 + sortedAudioByTitle.size) % sortedAudioByTitle.size else 0
            playAudioInBackground(sortedAudioByTitle[prevIdx].video, null)
          }
        },
        onPlayPause = {
          if (isCurrentlyInSession) {
            val currentPaused = isPaused ?: false
            PlaybackSession.setPropertyBoolean("pause", !currentPaused)
          } else {
            playAudioInBackground(heroToDisplay.video, resumePos.takeIf { it > 0.0 })
          }
        },
        onNext = {
          if (isCurrentlyInSession) {
            val next = PlaybackSession.playNext()
            if (next == null && sortedAudioByTitle.isNotEmpty()) {
              val currIdx = sortedAudioByTitle.indexOfFirst { matchesPlaybackItem(it.video, currentSessionItem) }
              if (currIdx >= 0) {
                val nextIdx = (currIdx + 1) % sortedAudioByTitle.size
                playAudioInBackground(sortedAudioByTitle[nextIdx].video, null)
              }
            }
          } else if (sortedAudioByTitle.isNotEmpty()) {
            val currIdx = sortedAudioByTitle.indexOfFirst { it.video.path == heroToDisplay.video.path }
            val nextIdx = if (currIdx >= 0) (currIdx + 1) % sortedAudioByTitle.size else 0
            playAudioInBackground(sortedAudioByTitle[nextIdx].video, null)
          }
        },
      )
    }
  }
}

/**
 * Dual-Horizon Rails component
 * Provides synchronized horizontal media rails:
 * 1. Upper Rail: Music (ALL songs sorted by title)
 * 2. Lower Rail: Video/Movie
 * 3. Bottom Rail: Recently Played (recent 10 files audio + video combined)
 */
@Composable
fun DualHorizonRails(
  videosWithInfo: List<VideoWithPlaybackInfo>,
  onPlayMedia: (Video, Double?) -> Unit = { v, _ -> },
  onFilterSelected: ((MediaLibraryType) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val browserPreferences = koinInject<BrowserPreferences>()
  val minVideoMinutes by browserPreferences.minimumVideoHorizonDurationMinutes.collectAsState()
  val minVideoDurationMs = remember(minVideoMinutes) { (minVideoMinutes * 60 * 1000L).coerceAtLeast(0L) }

  val videoItems = remember(videosWithInfo, minVideoDurationMs) {
    videosWithInfo.filter { !it.video.isAudio && (minVideoDurationMs <= 0L || it.video.duration >= minVideoDurationMs) }
  }
  val allAudioItems = remember(videosWithInfo) {
    videosWithInfo.filter { it.video.isAudio }
  }

  // Music Rail: ALL songs sorted by title
  val sortedAudioByTitle = remember(allAudioItems) {
    allAudioItems.sortedWith { a, b ->
      val titleA = if (a.video.title.isNotBlank()) a.video.title else FileTypeUtils.stripExtension(a.video.displayName)
      val titleB = if (b.video.title.isNotBlank()) b.video.title else FileTypeUtils.stripExtension(b.video.displayName)
      SortUtils.NaturalOrderComparator.DEFAULT.compare(titleA, titleB)
    }
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // 1. Video/Movie
    if (videoItems.isNotEmpty()) {
      HorizonRailSection(
        title = "Video/Movie",
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

    // 2. Music (ALL songs sorted by title)
    if (sortedAudioByTitle.isNotEmpty()) {
      HorizonRailSection(
        title = "Music",
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
      // Glass UI Title Capsule
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
        border = BorderStroke(
          width = 1.dp,
          brush = Brush.linearGradient(
            colors = listOf(
              accentColor.copy(alpha = 0.65f),
              Color.White.copy(alpha = 0.25f),
              accentColor.copy(alpha = 0.15f),
            ),
          ),
        ),
        shadowElevation = 2.dp,
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          Text(
            text = icon,
            style = MaterialTheme.typography.labelMedium,
          )
          Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 0.5.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
          )
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = accentColor.copy(alpha = 0.20f),
            border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.45f)),
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
      }

      // Glass UI Action Pill
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)),
        modifier = Modifier
          .clip(RoundedCornerShape(999.dp))
          .clickable(onClick = onViewAll),
      ) {
        Text(
          text = "Swipe Deck →",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
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
      "-${VideoScanUtils.formatDuration(remainingSeconds * 1000L)}/${video.durationFormatted}"
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

private enum class BentoCardGesture {
  None,
  Tap,
  VerticalScroll,
  Drag,
  Cancelled,
}

/**
 * Hero Spotlight Card displaying active/resuming video or pulsing audio track
 * with dynamic visualizer waves, specular highlights, and touch gestures.
 */
@Composable
private fun BentoHeroSpotlightCard(
  item: VideoWithPlaybackInfo,
  onClick: () -> Unit,
  onLongClick: () -> Unit = {},
  onPrevious: () -> Unit = {},
  onPlayPause: () -> Unit = {},
  onNext: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val appearancePreferences = koinInject<AppearancePreferences>()
  val currentAppTheme by appearancePreferences.appTheme.collectAsState()
  val colorScheme = MaterialTheme.colorScheme
  val (wave1Color, wave2Color, wave3Color) = remember(currentAppTheme, colorScheme) {
    resolveWaveformColors(currentAppTheme, colorScheme)
  }

  val isAudio = item.video.isAudio
  val progress = item.progressPercentage ?: 0f

  val playbackSessionState by PlaybackSession.state.collectAsState()
  val livePositionSec by PlaybackSession.propInt["time-pos"].collectAsState()
  val liveDurationSec by PlaybackSession.propInt["duration"].collectAsState()

  val isCurrentlyPlayingInSession = remember(playbackSessionState.currentItem, item.video) {
    matchesPlaybackItem(item.video, playbackSessionState.currentItem)
  }

  val originalCodec = remember(item.video) {
    if (isAudio) getAudioCodec(item.video) else item.video.resolution.ifBlank { "HD" }
  }

  // Total duration:
  val totalSec = if (isCurrentlyPlayingInSession && liveDurationSec != null && liveDurationSec!! > 0) {
    liveDurationSec!!.toLong()
  } else {
    (item.video.duration / 1000L).coerceAtLeast(0L)
  }
  val totalFormatted = if (totalSec > 0L) {
    VideoScanUtils.formatDuration(totalSec * 1000L)
  } else {
    item.video.durationFormatted.ifBlank { "00:00" }
  }

  // Real-time synced remaining time:
  val remainingSec = if (isCurrentlyPlayingInSession && livePositionSec != null) {
    (totalSec - livePositionSec!!).coerceAtLeast(0L)
  } else {
    item.timeRemaining ?: if (progress > 0f && totalSec > 0L) {
      (((1f - progress) * totalSec)).toLong().coerceAtLeast(0L)
    } else {
      totalSec
    }
  }

  val remainingFormatted = "-${VideoScanUtils.formatDuration(remainingSec * 1000L)}"

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

  val haptics = LocalHapticFeedback.current
  val density = LocalDensity.current
  val coroutineScope = rememberCoroutineScope()
  val swipeOffset = remember { Animatable(0f) }

  val onClickState by rememberUpdatedState(onClick)
  val onLongClickState by rememberUpdatedState(onLongClick)
  val onPlayPauseState by rememberUpdatedState(onPlayPause)
  val onPreviousState by rememberUpdatedState(onPrevious)
  val onNextState by rememberUpdatedState(onNext)

  val thresholdPx = with(density) { 56.dp.toPx() }
  val maxDragPx = with(density) { 120.dp.toPx() }

  val gestureModifier = Modifier.pointerInput(isAudio) {
    val touchSlop = viewConfiguration.touchSlop
    val longPressTimeout = viewConfiguration.longPressTimeoutMillis

    awaitEachGesture {
      val down = awaitFirstDown(requireUnconsumed = false)
      val pointerId = down.id
      val downPosition = down.position
      var initialDeltaX = 0f

      val gesture = withTimeoutOrNull(longPressTimeout) {
        while (true) {
          val event = awaitPointerEvent()
          val change = event.changes.firstOrNull { it.id == pointerId } ?: return@withTimeoutOrNull BentoCardGesture.Cancelled
          if (!change.pressed) {
            return@withTimeoutOrNull BentoCardGesture.Tap
          }
          val diff = change.position - downPosition
          if (abs(diff.y) > touchSlop && abs(diff.y) > abs(diff.x)) {
            return@withTimeoutOrNull BentoCardGesture.VerticalScroll
          }
          if (isAudio && abs(diff.x) > touchSlop) {
            initialDeltaX = diff.x
            return@withTimeoutOrNull BentoCardGesture.Drag
          }
        }
        BentoCardGesture.None
      }

      when (gesture) {
        BentoCardGesture.VerticalScroll -> {
          // Allow parent to scroll vertically naturally
        }
        BentoCardGesture.Tap -> {
          if (isAudio) {
            onPlayPauseState()
          } else {
            onClickState()
          }
        }
        null -> {
          // Long press triggered
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          onLongClickState()
          while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
            if (!change.pressed) break
          }
        }
        BentoCardGesture.Drag -> {
          var currentDeltaX = initialDeltaX
          var thresholdHapticGiven = false
          coroutineScope.launch { swipeOffset.snapTo(currentDeltaX.coerceIn(-maxDragPx, maxDragPx)) }

          while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
            if (!change.pressed) {
              if (currentDeltaX <= -thresholdPx) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onNextState()
              } else if (currentDeltaX >= thresholdPx) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPreviousState()
              }
              break
            }
            change.consume()
            val deltaX = change.position.x - change.previousPosition.x
            currentDeltaX += deltaX

            if (abs(currentDeltaX) >= thresholdPx && !thresholdHapticGiven) {
              haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              thresholdHapticGiven = true
            } else if (abs(currentDeltaX) < thresholdPx) {
              thresholdHapticGiven = false
            }

            coroutineScope.launch { swipeOffset.snapTo(currentDeltaX.coerceIn(-maxDragPx, maxDragPx)) }
          }

          coroutineScope.launch {
            swipeOffset.animateTo(
              targetValue = 0f,
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
              ),
            )
          }
        }
        else -> {}
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    contentAlignment = Alignment.Center,
  ) {
    // Gesture Hint Icons (revealed behind card while swiping horizontally)
    if (isAudio && abs(swipeOffset.value) > 8f) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.tertiary.copy(
            alpha = (abs(swipeOffset.value) / thresholdPx).coerceIn(0.2f, 0.95f),
          ),
          modifier = Modifier.size(36.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.RoundedFilled.SkipPrevious,
              contentDescription = "Previous Song",
              modifier = Modifier.size(20.dp),
              tint = MaterialTheme.colorScheme.onTertiary,
            )
          }
        }

        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.tertiary.copy(
            alpha = (abs(swipeOffset.value) / thresholdPx).coerceIn(0.2f, 0.95f),
          ),
          modifier = Modifier.size(36.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.RoundedFilled.SkipNext,
              contentDescription = "Next Song",
              modifier = Modifier.size(20.dp),
              tint = MaterialTheme.colorScheme.onTertiary,
            )
          }
        }
      }
    }

    Card(
      modifier = Modifier
        .fillMaxWidth()
        .offset { IntOffset(swipeOffset.value.roundToInt(), 0) }
        .clip(RoundedCornerShape(24.dp))
        .then(gestureModifier),
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
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Top Header Row: Status Badge & Format Chip
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
              border = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                  colors = listOf(
                    (if (isAudio) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary).copy(alpha = 0.65f),
                    Color.White.copy(alpha = 0.25f),
                    (if (isAudio) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary).copy(alpha = 0.15f),
                  ),
                ),
              ),
              shadowElevation = 2.dp,
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(7.dp)
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

            Surface(
              shape = RoundedCornerShape(999.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.40f),
              border = BorderStroke(
                width = 0.8.dp,
                color = (if (isAudio) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary).copy(alpha = 0.35f),
              ),
            ) {
              Text(
                text = if (isAudio) {
                  "$originalCodec • $remainingFormatted/$totalFormatted"
                } else {
                  "$originalCodec • ${item.video.durationFormatted}"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 11.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.90f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }

          // Middle Row: Thumbnail Preview & Metadata
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            // Media Thumbnail Box (clean thumbnail: NO play button overlay, NO codec badge)
            Box {
              BentoMediaThumbnail(
                video = item.video,
                modifier = Modifier
                  .size(width = if (isAudio) 70.dp else 108.dp, height = 66.dp)
                  .clip(RoundedCornerShape(16.dp)),
                showPlayOverlay = !isAudio,
              )
            }

            // Information & Visualizer
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              val (artist, title) = if (isCurrentlyPlayingInSession && playbackSessionState.currentItem != null) {
                val cur = playbackSessionState.currentItem!!
                val displayTitle = cur.title?.takeIf { it.isNotBlank() }
                  ?: (if (item.video.title.isNotBlank()) item.video.title else FileTypeUtils.stripExtension(item.video.displayName))
                val displayArtist = cur.artist?.takeIf { it.isNotBlank() } ?: item.video.artist
                Pair(displayArtist, displayTitle)
              } else if (isAudio) {
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

              if (isAudio) {
                OneUi9WaveformVisualizer(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .padding(top = 2.dp),
                  wave1Color = wave1Color,
                  wave2Color = wave2Color,
                  wave3Color = wave3Color,
                )
              }

              if (!isAudio && progress > 0f) {
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
}

/**
 * Dynamically resolves 3-tier One UI 9 wavy visualizer colors based on active [AppTheme] and [ColorScheme]:
 * - Wave 1: Bass Sub-glow (deep ambient anchor)
 * - Wave 2: Mid Harmonic (vibrant energetic body)
 * - Wave 3: Treble Crest (luminescent lead crest with specular highlight)
 */
private fun resolveWaveformColors(
  appTheme: AppTheme,
  colorScheme: ColorScheme,
): Triple<Color, Color, Color> {
  return when (appTheme) {
    AppTheme.StealthAcidLime -> Triple(
      Color(0xFF0D3B2E), // Deep pine sub-glow
      Color(0xFF10B981), // Emerald mint harmonic
      Color(0xFFD4FF00), // Acid Lime crest
    )
    AppTheme.ElectricCyan -> Triple(
      Color(0xFF082F49), // Deep abyss
      Color(0xFF00B4D8), // Vibrant cyan
      Color(0xFF00F0FF), // Electric cyan crest
    )
    AppTheme.HyperCrimson -> Triple(
      Color(0xFF4C0519), // Wine velvet
      Color(0xFFF43F5E), // Rose coral
      Color(0xFFFF1744), // Hyper Crimson crest
    )
    AppTheme.CyberMagenta -> Triple(
      Color(0xFF4A044E), // Deep plum
      Color(0xFFE040FB), // Neon orchid
      Color(0xFFFF007F), // Cyber magenta crest
    )
    AppTheme.QuantumViolet -> Triple(
      Color(0xFF2E1065), // Deep violet
      Color(0xFF7C4DFF), // Purple harmonic
      Color(0xFFB388FF), // Quantum lavender crest
    )
    AppTheme.SolarGold -> Triple(
      Color(0xFF451A03), // Deep bronze
      Color(0xFFF59E0B), // Warm amber
      Color(0xFFFFD600), // Solar gold crest
    )
    AppTheme.CyberPrism -> Triple(
      Color(0xFF1E1B4B), // Deep indigo
      Color(0xFF8B5CF6), // Electric violet
      Color(0xFF00F5FF), // Prism cyan crest
    )
    AppTheme.RossoPrism,
    AppTheme.Doom -> Triple(
      Color(0xFF4C0519), // Wine velvet
      Color(0xFFFF2A6D), // Hot pink
      Color(0xFFFF003C), // Rosso neon crest
    )
    AppTheme.AuroraMint,
    AppTheme.GreenApple,
    AppTheme.TakoGreen,
    AppTheme.Forest -> Triple(
      Color(0xFF052E16), // Deep forest
      Color(0xFF10B981), // Emerald mint
      Color(0xFF00F5D4), // Bright neon jade crest
    )
    AppTheme.HoloOpal,
    AppTheme.Lavender -> Triple(
      Color(0xFF2E1065), // Deep violet
      Color(0xFF38BDF8), // Sky blue
      Color(0xFFA855F7), // Opal purple crest
    )
    AppTheme.CottonCandy -> Triple(
      Color(0xFF3B1238), // Deep berry
      Color(0xFFDEB0E9), // Orchid mist
      Color(0xFFFFB1D5), // Cotton candy pink crest
    )
    AppTheme.SolarAmber,
    AppTheme.Sunset,
    AppTheme.Amber,
    AppTheme.Cloudflare -> Triple(
      Color(0xFF431407), // Deep amber
      Color(0xFFEA580C), // Blaze orange
      Color(0xFFFDE047), // Radiant gold crest
    )
    AppTheme.CrimsonSonic -> Triple(
      Color(0xFF3B0764), // Deep indigo
      Color(0xFFFF2A6D), // Sonic crimson
      Color(0xFF00F5FF), // Cyan spark crest
    )
    AppTheme.Nord,
    AppTheme.TokyoNight,
    AppTheme.Catppuccin,
    AppTheme.Midnight,
    AppTheme.Sapphire,
    AppTheme.Ocean,
    AppTheme.Tidal,
    AppTheme.Aurora -> Triple(
      Color(0xFF1E1B4B), // Deep Midnight Indigo (One UI 9 Liquid Aura signature)
      Color(0xFF8B5CF6), // Electric Violet harmonic
      Color(0xFF38BDF8), // Galaxy Sky Cyan crest
    )
    AppTheme.Monochrome,
    AppTheme.Slate,
    AppTheme.YinYang -> Triple(
      Color(0xFF1E293B), // Deep slate
      Color(0xFF94A3B8), // Cool steel
      Color(0xFFF8FAFC), // Platinum white crest
    )
    AppTheme.Mocha,
    AppTheme.Gruvbox -> Triple(
      Color(0xFF2E1A11), // Deep roasted espresso
      Color(0xFFA1887F), // Soft caramel mocha
      Color(0xFFE0BB95), // Warm latte froth crest
    )
    AppTheme.RoseGold,
    AppTheme.RosePine,
    AppTheme.Strawberry -> Triple(
      Color(0xFF4A102F), // Deep wine berry
      Color(0xFFDB2777), // Vibrant rose
      Color(0xFFF472B6), // Light rose gold crest
    )
    AppTheme.Dracula -> Triple(
      Color(0xFF21222C), // Dracula dark
      Color(0xFFBD93F9), // Purple harmonic
      Color(0xFF50FA7B), // Neon green crest
    )
    AppTheme.Coral -> Triple(
      Color(0xFF431407), // Deep coral brown
      Color(0xFFF43F5E), // Rose coral
      Color(0xFFFDA4AF), // Light coral crest
    )
    AppTheme.Violet -> Triple(
      Color(0xFF2E1065), // Deep violet
      Color(0xFF8B5CF6), // Violet mid
      Color(0xFFD8B4FE), // Light violet crest
    )
    AppTheme.Kanagawa -> Triple(
      Color(0xFF1E2836), // Kanagawa deep wave
      Color(0xFF98BB6C), // Spring green
      Color(0xFF7E9CD8), // Fuji blue crest
    )
    else -> {
      // Dynamic Monet or unlisted: derive directly from runtime ColorScheme
      val w3 = colorScheme.primary
      val w2 = colorScheme.tertiary
      val w1 = colorScheme.secondary.copy(alpha = 0.50f).compositeOver(Color(0xFF0A0D14))
      Triple(w1, w2, w3)
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
  wave1Color: Color = Color(0xFF1E1B4B), // One UI 9 Liquid Aura: Deep Midnight Indigo (1st wave: bass veil)
  wave2Color: Color = Color(0xFF8B5CF6), // One UI 9 Liquid Aura: Electric Violet (2nd wave: mid veil)
  wave3Color: Color = Color(0xFF38BDF8), // One UI 9 Liquid Aura: Galaxy Sky Cyan (Last wave: lead crest)
) {
  var frameNanos by remember { mutableLongStateOf(0L) }
  var phase1 by remember { mutableFloatStateOf(0f) }
  var phase2 by remember { mutableFloatStateOf(1.2f) }
  var phase3 by remember { mutableFloatStateOf(2.4f) }

  val twoPi = (2.0 * PI).toFloat()

  LaunchedEffect(Unit) {
    var previous = 0L
    while (true) {
      withFrameNanos { now ->
        val dt = if (previous == 0L) 0f else ((now - previous) / 1_000_000_000f).coerceIn(0f, 0.1f)
        previous = now
        phase1 = (phase1 + dt * 1.5f) % twoPi
        phase2 = (phase2 + dt * 2.3f) % twoPi
        phase3 = (phase3 + dt * 3.4f) % twoPi
        frameNanos = now
      }
    }
  }

  val filledPath = remember { Path() }
  val contourPath = remember { Path() }

  Canvas(modifier = modifier) {
    @Suppress("UNUSED_VARIABLE")
    val frame = frameNanos // Subscribes Canvas draw phase to hardware vsync frames without recomposing
    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) return@Canvas

    val baselineY = height - 1.5.dp.toPx()
    val availableLift = (baselineY - 1.dp.toPx()).coerceAtLeast(0f)
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

    // Layer 1: Ambient Deep Bass Veil (soft background rolling harmonic wave - 1st wave)
    drawWaveRibbon(
      wavelength = 92.dp.toPx(),
      lift = availableLift * 0.95f,
      phaseOffset = phase1,
      fillTopColor = wave1Color.copy(alpha = 0.28f),
      fillBottomColor = wave1Color.copy(alpha = 0.04f),
      strokeColor = wave1Color.copy(alpha = 0.55f),
      strokeWidth = 1.2.dp.toPx(),
      glowColor = wave1Color.copy(alpha = 0.28f),
      glowWidth = 4.dp.toPx(),
    )

    // Layer 2: Harmonic Mid Veil (vibrant gradient curtain - 2nd wave)
    drawWaveRibbon(
      wavelength = 62.dp.toPx(),
      lift = availableLift * 0.80f,
      phaseOffset = phase2,
      fillTopColor = wave2Color.copy(alpha = 0.40f),
      fillBottomColor = wave1Color.copy(alpha = 0.06f),
      strokeColor = wave2Color.copy(alpha = 0.85f),
      strokeWidth = 1.6.dp.toPx(),
      glowColor = wave2Color.copy(alpha = 0.35f),
      glowWidth = 5.5.dp.toPx(),
    )

    // Layer 3: Vibrant Lead Luminous Wave with Specular Core (crisp foreground crest - Last wave)
    drawWaveRibbon(
      wavelength = 42.dp.toPx(),
      lift = availableLift * 0.65f,
      phaseOffset = phase3,
      fillTopColor = wave3Color.copy(alpha = 0.62f),
      fillBottomColor = wave2Color.copy(alpha = 0.12f),
      strokeColor = wave3Color.copy(alpha = 0.98f),
      strokeWidth = 2.0.dp.toPx(),
      glowColor = wave3Color.copy(alpha = 0.50f),
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
  showPlayOverlay: Boolean = true,
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

    // Play button specular overlay (can be hidden, e.g. for audio pulsing)
    if (showPlayOverlay) {
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
}

