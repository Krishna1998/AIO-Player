/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.browser.cinematichorizon

import android.content.Intent
import app.video.player.ui.mediainfo.overviewBadges
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import app.video.player.R
import app.video.player.preferences.FoldersPreferences
import app.video.player.preferences.BlacklistScope
import app.video.player.ui.browser.dialogs.RenameDialog
import app.video.player.ui.browser.dialogs.DeleteConfirmationDialog
import app.video.player.ui.components.AppToast as Toast
import app.video.player.utils.permission.PermissionUtils
import app.video.player.ui.icons.AppIcon
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.compositeOver
import app.video.player.preferences.AppearancePreferences
import app.video.player.ui.player.visualizer.resolveWaveformColors
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.video.player.domain.media.model.Video
import app.video.player.domain.recentlyplayed.repository.RecentlyPlayedRepository
import android.net.Uri
import app.video.player.preferences.DualHorizonHeaderStyle
import app.video.player.preferences.HeroSpotlightSeekbarStyle
import app.video.player.preferences.BrowserPreferences
import app.video.player.preferences.MediaLibraryType
import app.video.player.preferences.preference.collectAsState
import app.video.player.domain.thumbnail.ThumbnailRepository
import app.video.player.domain.thumbnail.OnlineMusicArtworkRepository
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
 * Dual Horizon Hub Header
 * Integrates a liquid-glass Hero Spotlight Card (with dynamic audio visualizer / video resume)
 * and synchronized horizon rails for Cinema, Hi-Res Audio, and Recently Played.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DualHorizonHeader(
  videosWithInfo: List<VideoWithPlaybackInfo>,
  recentlyPlayedFilePath: String?,
  onPlayMedia: (Video, Double?, List<Video>?) -> Unit,
  onFilterSelected: ((MediaLibraryType) -> Unit)? = null,
  onRenameMedia: ((Video, String) -> Unit)? = null,
  onHideMedia: ((Video) -> Unit)? = null,
  onDeleteMedia: ((Video) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  if (videosWithInfo.isEmpty()) return

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val foldersPreferences = koinInject<FoldersPreferences>()

  var actionTargetVideo by remember { mutableStateOf<Video?>(null) }
  var isRenameOpen by remember { mutableStateOf(false) }
  var isDeleteOpen by remember { mutableStateOf(false) }
  var videoToRename by remember { mutableStateOf<Video?>(null) }
  var videoToDelete by remember { mutableStateOf<Video?>(null) }

  val recentlyPlayedRepo = koinInject<RecentlyPlayedRepository>()
  val recentEntities by recentlyPlayedRepo.observeRecentlyPlayed(limit = 50).collectAsState(initial = emptyList())
  val browserPreferences = koinInject<BrowserPreferences>()
  val dualHorizonHintDismissed by browserPreferences.dualHorizonHintDismissed.collectAsState()
  val spotlightGesturesHintSeen by browserPreferences.spotlightGesturesHintSeen.collectAsState()
  val spotlightGesturesShowEveryTime by browserPreferences.spotlightGesturesShowEveryTime.collectAsState()
  val showVideoRail by browserPreferences.dualHorizonShowVideoRail.collectAsState()
  val showAudioPulsing by browserPreferences.dualHorizonShowAudioPulsing.collectAsState()
  val showCinematicSpotlight by browserPreferences.cinematicShowHeroSpotlight.collectAsState()
  val cinematicSpotlightScale by browserPreferences.cinematicSpotlightScale.collectAsState()
  val heroSpotlightSeekbarStyle by browserPreferences.heroSpotlightSeekbarStyle.collectAsState()
  val audioPulsingScale by browserPreferences.dualHorizonAudioPulsingScale.collectAsState()
  val videoRailScale by browserPreferences.dualHorizonVideoRailScale.collectAsState()
  val musicRailScale by browserPreferences.dualHorizonMusicRailScale.collectAsState()
  val dualHorizonHeaderStyle by browserPreferences.dualHorizonHeaderStyle.collectAsState()
  val showMusicRail by browserPreferences.dualHorizonShowMusicRail.collectAsState()
  val sectionOrder by browserPreferences.dualHorizonSectionOrder.collectAsState()
  val onlineMusicArtworkRepository = koinInject<OnlineMusicArtworkRepository>()
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
    rememberedAudioPath = targetVideo.path
    browserPreferences.lastPlayedAudioPath.set(targetVideo.path)
    if (MediaUtils.shouldPlayInMiniPlayerOnly(isAudio = true) && PlaybackSession.isInitialized) {
      val audioQueue = sortedAudioByTitle.map { item ->
        val v = item.video
        val cachedArt = v.artworkUrl ?: onlineMusicArtworkRepository.getCachedArtworkFile(v)?.let { Uri.fromFile(it).toString() }
        PlaybackItem.fromUri(
          uri = v.uri.toString(),
          stableId = if (app.video.player.BuildConfig.SCOPED_STORAGE_ONLY && v.uri.scheme == "content") {
            videoPlaybackIdentifiers(v).first()
          } else PlaybackIdentity.forLocalPath(v.path),
          title = if (v.title.isNotBlank()) v.title else FileTypeUtils.stripExtension(v.displayName),
          artist = v.artist,
          mimeType = v.mimeType.ifBlank { "audio/*" },
          artworkUri = cachedArt,
          durationSeconds = (v.duration / 1000L).toInt().takeIf { it > 0 },
          videoWidth = v.width,
          videoHeight = v.height,
        )
      }
      val targetIdx = sortedAudioByTitle.indexOfFirst { it.video.path == targetVideo.path }.coerceAtLeast(0)
      MediaUtils.playInMiniPlayer(context, audioQueue, targetIdx, startPositionSeconds = startPos?.takeIf { it > 0.0 })
    } else {
      onPlayMedia(targetVideo, startPos, sortedAudioByTitle.map { it.video })
    }
  }

  val heroVideo = activeVideoFromSession
    ?: (if (recentlyPlayedFilePath != null) {
      videoItems.firstOrNull { it.video.path == recentlyPlayedFilePath }
    } else null)
    ?: videoItems.firstOrNull { it.progressPercentage != null && it.progressPercentage > 0f && !it.isWatched }
    ?: videoItems.firstOrNull()

  CinematicHorizonContent(
    modifier = modifier,
    videoItems = videoItems,
    sortedAudioByTitle = sortedAudioByTitle,
    heroAudio = heroAudio,
    heroVideo = heroVideo,
    currentSessionItem = currentSessionItem,
    sectionOrder = sectionOrder,
    showVideoRail = showVideoRail,
    showAudioPulsing = showAudioPulsing,
    showMusicRail = showMusicRail,
    showSpotlight = showCinematicSpotlight,
    spotlightScale = cinematicSpotlightScale,
    audioPulsingScale = audioPulsingScale,
    videoRailScale = videoRailScale,
    musicRailScale = musicRailScale,
    dualHorizonHeaderStyle = dualHorizonHeaderStyle,
    heroSpotlightSeekbarStyle = heroSpotlightSeekbarStyle,
    onPlayMedia = onPlayMedia,
    playAudioInBackground = { targetVideo, pos -> playAudioInBackground(targetVideo, pos) },
    onVideoAction = { actionTargetVideo = it },
  )

  // Bottom Sheet for Bento Media Long-Press Actions (Rename, Hide, Delete)
  actionTargetVideo?.let { targetVideo ->
    BentoMediaActionSheet(
      video = targetVideo,
      onDismiss = { actionTargetVideo = null },
      onRename = {
        videoToRename = targetVideo
        actionTargetVideo = null
        isRenameOpen = true
      },
      onHide = {
        actionTargetVideo = null
        val scope = if (targetVideo.isAudio) BlacklistScope.AUDIO_ONLY else BlacklistScope.VIDEO_ONLY
        foldersPreferences.addBlacklistedFolders(setOf(targetVideo.path), scope)
        onHideMedia?.invoke(targetVideo)
        Toast.makeText(context, context.getString(R.string.bento_action_hidden_msg), Toast.LENGTH_SHORT).show()
      },
      onDelete = {
        videoToDelete = targetVideo
        actionTargetVideo = null
        isDeleteOpen = true
      },
    )
  }

  // Rename Dialog
  if (isRenameOpen && videoToRename != null) {
    val target = videoToRename!!
    RenameDialog(
      isOpen = isRenameOpen,
      currentName = target.displayName,
      itemType = if (target.isAudio) "audio file" else "video",
      onDismiss = {
        isRenameOpen = false
        videoToRename = null
      },
      onConfirm = { newName ->
        if (onRenameMedia != null) {
          onRenameMedia(target, newName)
        } else {
          coroutineScope.launch(Dispatchers.IO) {
            val oldPath = target.path
            PermissionUtils.StorageOps.renameVideo(context, target, newName)
            // Migrate thumbnail/poster cache so renamed item keeps its artwork
            if (oldPath.isNotBlank()) {
              val newFile = java.io.File(java.io.File(oldPath).parentFile, newName)
              val thumbnailRepo = org.koin.core.context.GlobalContext.get()
                .get<ThumbnailRepository>(ThumbnailRepository::class)
              thumbnailRepo.onMediaRenamed(oldPath, newFile.absolutePath)
            }
          }
        }
        isRenameOpen = false
        videoToRename = null
      },
    )
  }

  // Delete Dialog
  if (isDeleteOpen && videoToDelete != null) {
    val target = videoToDelete!!
    DeleteConfirmationDialog(
      isOpen = isDeleteOpen,
      itemCount = 1,
      itemType = if (target.isAudio) "audio file" else "video",
      itemPaths = listOf(target.uri.toString()),
      totalSizeBytes = target.size.takeIf { it > 0L },
      onDismiss = {
        isDeleteOpen = false
        videoToDelete = null
      },
      onConfirm = {
        if (onDeleteMedia != null) {
          onDeleteMedia(target)
        } else {
          coroutineScope.launch(Dispatchers.IO) {
            PermissionUtils.StorageOps.deleteVideos(context, listOf(target))
          }
        }
        isDeleteOpen = false
        videoToDelete = null
      },
    )
  }
}

/**
 * Concept 1: Cinematic Horizon
 * Full-bleed panoramic hero card with dynamic action buttons and horizontal media rails
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CinematicHorizonContent(
  modifier: Modifier = Modifier,
  videoItems: List<VideoWithPlaybackInfo>,
  sortedAudioByTitle: List<VideoWithPlaybackInfo>,
  heroAudio: VideoWithPlaybackInfo?,
  heroVideo: VideoWithPlaybackInfo?,
  currentSessionItem: PlaybackItem?,
  sectionOrder: List<String> = emptyList(),
  showVideoRail: Boolean = true,
  showAudioPulsing: Boolean = true,
  showMusicRail: Boolean = true,
  showSpotlight: Boolean = true,
  spotlightScale: Float = 1.0f,
  audioPulsingScale: Float = 1.0f,
  videoRailScale: Float = 1.0f,
  musicRailScale: Float = 1.0f,
  dualHorizonHeaderStyle: DualHorizonHeaderStyle = DualHorizonHeaderStyle.LiquidCapsule,
  heroSpotlightSeekbarStyle: HeroSpotlightSeekbarStyle = HeroSpotlightSeekbarStyle.TimestampPill,
  onPlayMedia: (Video, Double?, List<Video>) -> Unit,
  playAudioInBackground: (Video, Double?) -> Unit,
  onVideoAction: (Video) -> Unit,
) {
  val context = LocalContext.current
  val browserPreferences = koinInject<BrowserPreferences>()
  val spotlightGesturesHintSeen by browserPreferences.spotlightGesturesHintSeen.collectAsState()
  val spotlightGesturesShowEveryTime by browserPreferences.spotlightGesturesShowEveryTime.collectAsState()
  val isPaused by PlaybackSession.propBoolean["pause"].collectAsState()
  val livePos = PlaybackSession.propInt["time-pos"].collectAsState().value?.toDouble()

  val spotlightItem = heroVideo ?: videoItems.firstOrNull() ?: heroAudio ?: sortedAudioByTitle.firstOrNull()
  val heroToDisplay = heroAudio ?: (if (videoItems.isEmpty()) heroVideo else null)

  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
  val isTablet = configuration.smallestScreenWidthDp >= 600

  val landscapeHeroHeight = if (isTablet) (250f * spotlightScale.coerceIn(0.7f, 1.3f)).dp else (236f * spotlightScale.coerceIn(0.7f, 1.3f)).dp
  val portraitHeroHeight = (290f * spotlightScale.coerceIn(0.7f, 1.3f)).dp

  val hasDualHeroLandscape = isLandscape && showSpotlight && spotlightItem != null && showAudioPulsing && heroToDisplay != null

  var showGestureInfoPopup by rememberSaveable { mutableStateOf(false) }

  // Automatically show the gestures hint guide ONE TIME on initial launch.
  LaunchedEffect(spotlightGesturesHintSeen) {
    if (!spotlightGesturesHintSeen) {
      showGestureInfoPopup = true
    }
  }

  @Composable
  fun RenderAudioPulsingCard(
    modifier: Modifier = Modifier,
    cardHeight: androidx.compose.ui.unit.Dp? = null,
    includeHorizontalPadding: Boolean = true,
  ) {
    if (heroToDisplay == null) return
    val isCurrentlyInSession = remember(currentSessionItem, heroToDisplay.video) {
      matchesPlaybackItem(heroToDisplay.video, currentSessionItem)
    }
    val savedPos = heroToDisplay.timeRemaining?.let { rem ->
      ((heroToDisplay.video.duration / 1000L) - rem).coerceAtLeast(0L).toDouble()
    } ?: if ((heroToDisplay.progressPercentage ?: 0f) > 0f && heroToDisplay.video.duration > 0L) {
      (((heroToDisplay.progressPercentage!! * heroToDisplay.video.duration) / 1000L).toLong()).toDouble()
    } else 0.0

    val audioResumePos = if (isCurrentlyInSession && livePos != null && livePos > 0.0) {
      livePos
    } else {
      savedPos
    }

    val nextTrackTitle = remember(sortedAudioByTitle, currentSessionItem, heroToDisplay.video) {
      if (sortedAudioByTitle.isEmpty()) {
        null
      } else {
        val currIdx = if (isCurrentlyInSession) {
          sortedAudioByTitle.indexOfFirst { matchesPlaybackItem(it.video, currentSessionItem) }
        } else {
          sortedAudioByTitle.indexOfFirst { it.video.path == heroToDisplay.video.path }
        }
        if (currIdx >= 0 && sortedAudioByTitle.size > 1) {
          val nextIdx = (currIdx + 1) % sortedAudioByTitle.size
          val nextVideo = sortedAudioByTitle[nextIdx].video
          nextVideo.title.ifBlank { FileTypeUtils.stripExtension(nextVideo.displayName) }
        } else {
          null
        }
      }
    }

    HeroSpotlightCard(
      item = heroToDisplay,
      scale = if (hasDualHeroLandscape) spotlightScale else audioPulsingScale,
      cardHeight = cardHeight,
      includeHorizontalPadding = includeHorizontalPadding,
      modifier = modifier,
      nextTrackTitle = nextTrackTitle,
      onOpenGestureHint = { showGestureInfoPopup = true },
      onClick = {
        if (heroToDisplay.video.isAudio) {
          if (isCurrentlyInSession) {
            val currentPaused = isPaused ?: false
            PlaybackSession.setPropertyBoolean("pause", !currentPaused)
          } else {
            playAudioInBackground(heroToDisplay.video, audioResumePos.takeIf { it > 0.0 })
          }
        } else {
          onPlayMedia(heroToDisplay.video, audioResumePos.takeIf { it > 0.0 }, videoItems.map { it.video })
        }
      },
      onLongClick = {
        if (heroToDisplay.video.isAudio) {
          if (PlaybackSession.isInitialized) {
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
          } else {
            onPlayMedia(heroToDisplay.video, audioResumePos.takeIf { it > 0.0 }, sortedAudioByTitle.map { it.video })
          }
        } else {
          onVideoAction(heroToDisplay.video)
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
          playAudioInBackground(heroToDisplay.video, audioResumePos.takeIf { it > 0.0 })
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

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(bottom = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // 0. Panoramic Spotlight Hero Box / Landscape Dual Hero Single Row
    if (hasDualHeroLandscape) {
      val video = spotlightItem.video
      val isSessionPlaying = remember(currentSessionItem, video) {
        matchesPlaybackItem(video, currentSessionItem)
      }
      val resumePos = if (isSessionPlaying && livePos != null && livePos > 0.0) {
        livePos
      } else {
        spotlightItem.timeRemaining?.let { rem ->
          ((video.duration / 1000L) - rem).coerceAtLeast(0L).toDouble()
        } ?: if ((spotlightItem.progressPercentage ?: 0f) > 0f && video.duration > 0L) {
          (((spotlightItem.progressPercentage!! * video.duration) / 1000L).toLong()).toDouble()
        } else 0.0
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Left: Hero Spotlight Card
        HeroSpotlightBox(
          spotlightItem = spotlightItem,
          resumePos = resumePos,
          heroSpotlightSeekbarStyle = heroSpotlightSeekbarStyle,
          onPlayMedia = onPlayMedia,
          onVideoAction = onVideoAction,
          videoItems = videoItems,
          cardHeight = landscapeHeroHeight,
          modifier = Modifier.weight(1f),
          onOpenGestureHint = { showGestureInfoPopup = true },
        )

        // Right: Audio Pulsing Hero Spotlight Card
        RenderAudioPulsingCard(
          modifier = Modifier.weight(1f),
          cardHeight = landscapeHeroHeight,
          includeHorizontalPadding = false,
        )
      }
    } else if (showSpotlight && spotlightItem != null) {
      val video = spotlightItem.video
      val isSessionPlaying = remember(currentSessionItem, video) {
        matchesPlaybackItem(video, currentSessionItem)
      }
      val resumePos = if (isSessionPlaying && livePos != null && livePos > 0.0) {
        livePos
      } else {
        spotlightItem.timeRemaining?.let { rem ->
          ((video.duration / 1000L) - rem).coerceAtLeast(0L).toDouble()
        } ?: if ((spotlightItem.progressPercentage ?: 0f) > 0f && video.duration > 0L) {
          (((spotlightItem.progressPercentage!! * video.duration) / 1000L).toLong()).toDouble()
        } else 0.0
      }

      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        HeroSpotlightBox(
          spotlightItem = spotlightItem,
          resumePos = resumePos,
          heroSpotlightSeekbarStyle = heroSpotlightSeekbarStyle,
          onPlayMedia = onPlayMedia,
          onVideoAction = onVideoAction,
          videoItems = videoItems,
          cardHeight = portraitHeroHeight,
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 800.dp)
            .padding(horizontal = 16.dp),
          onOpenGestureHint = { showGestureInfoPopup = true },
        )
      }
    }

    // Reorderable sections identical to BentoFusion
    val defaults = listOf("VIDEO", "PULSING", "MUSIC")
    val orderedSections = remember(sectionOrder) {
      (sectionOrder.filter { it in defaults } + defaults).distinct()
    }

    orderedSections.forEach { sectionId ->
      when (sectionId) {
        "VIDEO" -> {
          // Movies / Videos Carousel Rail
          if (showVideoRail && videoItems.isNotEmpty()) {
            HorizonRailSection(
              title = "Movies / Videos",
              icon = "🎬",
              accentColor = MaterialTheme.colorScheme.primary,
              count = videoItems.size,
              style = dualHorizonHeaderStyle,
            ) {
              LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                items(items = videoItems, key = { it.video.path }) { item ->
                  BentoHorizonVideoCard(
                    item = item,
                    scale = videoRailScale,
                    onClick = { onPlayMedia(item.video, null, videoItems.map { it.video }) },
                    onLongClick = { onVideoAction(item.video) },
                  )
                }
              }
            }
          }
        }
        "PULSING" -> {
          // Audio Pulsing Hero Spotlight Card (skipped in landscape if already shown in dual hero row)
          if (!hasDualHeroLandscape && showAudioPulsing && heroToDisplay != null) {
            RenderAudioPulsingCard(
              cardHeight = if (isLandscape) landscapeHeroHeight else null,
            )
          }
        }
        "MUSIC" -> {
          // Music Rail (ALL songs sorted by title)
          if (showMusicRail && sortedAudioByTitle.isNotEmpty()) {
            HorizonRailSection(
              title = "Music",
              icon = "🎵",
              accentColor = MaterialTheme.colorScheme.tertiary,
              count = sortedAudioByTitle.size,
              style = dualHorizonHeaderStyle,
            ) {
              LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                items(items = sortedAudioByTitle, key = { it.video.path }) { item ->
                  BentoHorizonAudioCard(
                    item = item,
                    scale = musicRailScale,
                    onClick = { playAudioInBackground(item.video, null) },
                    onLongClick = { onVideoAction(item.video) },
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showGestureInfoPopup) {
    SpotlightGesturesInfoDialog(
      initialShowEveryTime = spotlightGesturesShowEveryTime,
      onDismiss = { showEveryTime ->
        showGestureInfoPopup = false
        browserPreferences.spotlightGesturesHintSeen.set(true)
        browserPreferences.spotlightGesturesShowEveryTime.set(showEveryTime)
      },
    )
  }
}

/**
 * DualHorizon Key Feature Discovery Card
 * Displays an elegant Liquid Glass card introducing Audio Pulsing gestures
 * and Horizon quick navigation right after permission setup.
 */
@Composable
fun DualHorizonFeatureHintCard(
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
  val primaryColor = MaterialTheme.colorScheme.primary
  val tertiaryColor = MaterialTheme.colorScheme.tertiary

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
    tonalElevation = 4.dp,
    shadowElevation = 8.dp,
    border = BorderStroke(
      1.dp,
      Brush.linearGradient(
        colors = listOf(
          tertiaryColor.copy(alpha = 0.5f),
          primaryColor.copy(alpha = 0.25f),
          Color.Transparent,
        ),
      ),
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = tertiaryColor.copy(alpha = 0.18f),
            modifier = Modifier.size(34.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = "✨",
                fontSize = 16.sp,
              )
            }
          }
          Column {
            Text(
              text = "Fusion Features",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Audio Pulsing & Gesture Controls",
              style = MaterialTheme.typography.labelSmall,
              color = tertiaryColor,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }

        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onDismiss),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = "✕",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Feature 1: Audio Pulsing Tap & Gestures
        Row(
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "🎵",
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 2.dp),
          )
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "Live Audio Pulsing Gestures",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Tap the hero card to Play/Pause with pulsing aura. Swipe left or right anywhere on the card to skip songs instantly with haptics!",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 16.sp,
            )
          }
        }

        // Feature 2: Quick Long-Press Actions
        Row(
          verticalAlignment = Alignment.Top,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "⚡",
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 2.dp),
          )
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "Horizon Rails & Quick Actions",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "Long-press any media card to rename, hide, or delete. Long-press the Audio Pulsing hero card to expand into full player mode.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 16.sp,
            )
          }
        }
      }

      // Action button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier
            .clickable(onClick = onDismiss),
        ) {
          Text(
            text = "Got it",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        }
      }
    }
  }
}

/**
 * Dual-Horizon Rails component
 * Provides synchronized horizontal media rails:
 * 1. Upper Rail: Music (ALL songs sorted by title)
 * 2. Lower Rail: Movies / Videos
 * 3. Bottom Rail: Recently Played (recent 10 files audio + video combined)
 */
@Composable
fun DualHorizonRails(
  videosWithInfo: List<VideoWithPlaybackInfo>,
  onPlayMedia: (Video, Double?, List<Video>?) -> Unit = { _, _, _ -> },
  onItemLongClick: ((Video) -> Unit)? = null,
  onFilterSelected: ((MediaLibraryType) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val browserPreferences = koinInject<BrowserPreferences>()
  val showVideoRail by browserPreferences.dualHorizonShowVideoRail.collectAsState()
  val dualHorizonHeaderStyle by browserPreferences.dualHorizonHeaderStyle.collectAsState()
  val showMusicRail by browserPreferences.dualHorizonShowMusicRail.collectAsState()
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
    // 1. Movies / Videos
    if (showVideoRail && videoItems.isNotEmpty()) {
      HorizonRailSection(
        title = "Movies / Videos",
        icon = "🎬",
        accentColor = MaterialTheme.colorScheme.primary,
        count = videoItems.size,
        style = dualHorizonHeaderStyle,
      ) {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(
            items = videoItems,
            key = { it.video.path },
          ) { item ->
            BentoHorizonVideoCard(
              item = item,
              onClick = { onPlayMedia(item.video, null, videoItems.map { it.video }) },
              onLongClick = onItemLongClick?.let { { it(item.video) } },
            )
          }
        }
      }
    }

    // 2. Music (ALL songs sorted by title)
    if (showMusicRail && sortedAudioByTitle.isNotEmpty()) {
      HorizonRailSection(
        title = "Music",
        icon = "🎵",
        accentColor = MaterialTheme.colorScheme.tertiary,
        count = sortedAudioByTitle.size,
        style = dualHorizonHeaderStyle,
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
              onClick = { onPlayMedia(item.video, null, sortedAudioByTitle.map { it.video }) },
              onLongClick = onItemLongClick?.let { { it(item.video) } },
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
  style: DualHorizonHeaderStyle = DualHorizonHeaderStyle.LiquidCapsule,
  content: @Composable () -> Unit,
) {
  val isMusic = title.contains("Music", ignoreCase = true)
  val subtitleLabel = if (isMusic) "Lossless soundstage" else "Cinema collection"

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.Start,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      when (style) {
        // 1. Liquid Glass Aura Capsule (Original / Enhanced)
        DualHorizonHeaderStyle.LiquidCapsule -> {
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = accentColor.copy(alpha = 0.16f),
            border = BorderStroke(
              width = 1.dp,
              brush = Brush.linearGradient(
                colors = listOf(
                  accentColor.copy(alpha = 0.75f),
                  Color.White.copy(alpha = 0.35f),
                  accentColor.copy(alpha = 0.25f),
                ),
              ),
            ),
            shadowElevation = 3.dp,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
              Text(
                text = icon,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
              )
              Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 15.sp,
                  letterSpacing = 0.5.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
              )
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = accentColor.copy(alpha = 0.25f),
                border = BorderStroke(0.6.dp, accentColor.copy(alpha = 0.60f)),
              ) {
                Text(
                  text = "$count",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                  ),
                  color = accentColor,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
          }
        }

        // 2. Cyber Glow Pillar
        DualHorizonHeaderStyle.CyberPillar -> {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Box(
              modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 16.sp,
                  letterSpacing = (-0.2).sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = subtitleLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 10.sp,
                  letterSpacing = 1.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
              )
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = accentColor.copy(alpha = 0.14f),
              border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(text = icon, fontSize = 12.sp)
                Text(
                  text = "$count",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = accentColor,
                )
              }
            }
          }
        }

        // 3. Frosted Bento Strip
        DualHorizonHeaderStyle.BentoStrip -> {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = accentColor.copy(alpha = 0.20f),
                  border = BorderStroke(1.dp, accentColor.copy(alpha = 0.40f)),
                  modifier = Modifier.size(30.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(text = icon, fontSize = 14.sp)
                  }
                }
                Column {
                  Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 14.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = "$count items",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Normal,
                      fontSize = 11.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.06f),
                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f)),
              ) {
                Text(
                  text = "Browse",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }
            }
          }
        }

        // 4. Fluid Pulse Dot
        DualHorizonHeaderStyle.FluidPulse -> {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accentColor),
            )
            Text(
              text = title,
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                letterSpacing = (-0.3).sp,
              ),
              color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            ) {
              Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
              )
            }
          }
        }

        // 5. Studio Luxury Aperture
        DualHorizonHeaderStyle.StudioLuxury -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = subtitleLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 9.sp,
                  letterSpacing = 1.4.sp,
                ),
                color = accentColor,
              )
              Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                  fontWeight = FontWeight.Black,
                  fontSize = 18.sp,
                  letterSpacing = (-0.4).sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Text(
              text = "[$count]",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
              ),
              color = MaterialTheme.colorScheme.outline,
            )
          }
        }
      }
    }

    content()
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BentoHorizonVideoCard(
  item: VideoWithPlaybackInfo,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null,
  scale: Float = 1.0f,
  modifier: Modifier = Modifier,
) {
  val video = item.video
  val progress = item.progressPercentage

  val playbackSessionState by PlaybackSession.state.collectAsState()
  val livePositionSec by PlaybackSession.propInt["time-pos"].collectAsState()
  val liveDurationSec by PlaybackSession.propInt["duration"].collectAsState()

  val isCurrentlyInSession = remember(playbackSessionState.currentItem, video) {
    matchesPlaybackItem(video, playbackSessionState.currentItem)
  }

  val effectiveProgress = if (isCurrentlyInSession && livePositionSec != null && livePositionSec!! >= 0) {
    val totalSec = if (liveDurationSec != null && liveDurationSec!! > 0) liveDurationSec!!.toLong() else (video.duration / 1000L).coerceAtLeast(0L)
    if (totalSec > 0L) (livePositionSec!!.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f) else progress
  } else {
    progress
  }

  val haptics = LocalHapticFeedback.current
  val cardWidth = (216f * scale.coerceIn(0.7f, 1.3f)).dp
  val thumbHeight = (122f * scale.coerceIn(0.7f, 1.3f)).dp

  Card(
    modifier = modifier
      .width(cardWidth)
      .clip(RoundedCornerShape(18.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick?.let { action ->
          {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            action()
          }
        },
      ),
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
          .height(thumbHeight)
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
            .padding(7.dp)
            .align(Alignment.TopStart),
          shape = RoundedCornerShape(6.dp),
          color = Color.Black.copy(alpha = 0.72f),
          border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        ) {
          Text(
            text = resBadge,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }

        // Bottom-right duration pill
        Surface(
          modifier = Modifier
            .padding(7.dp)
            .align(Alignment.BottomEnd),
          shape = RoundedCornerShape(6.dp),
          color = Color.Black.copy(alpha = 0.75f),
        ) {
          Text(
            text = video.durationFormatted,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Medium,
            ),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
          )
        }

        // Bottom seek/progress indicator
        if (effectiveProgress != null && effectiveProgress > 0f) {
          LinearProgressIndicator(
            progress = { effectiveProgress },
            modifier = Modifier
              .fillMaxWidth()
              .height(3.5.dp)
              .align(Alignment.BottomCenter),
            color = if (isCurrentlyInSession) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary,
            trackColor = Color.Black.copy(alpha = 0.4f),
          )
        }
      }

      // Metadata Area
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.verticalGradient(
              colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                Color.Transparent,
              ),
            ),
          )
          .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Text(
          text = FileTypeUtils.stripExtension(video.displayName),
          style = MaterialTheme.typography.titleSmall.copy(
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 19.sp,
          ),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        if (progress != null && progress > 0f) {
          val statusText = if (item.isWatched) "Completed" else "Watched ${(progress * 100).toInt()}%"
          Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall.copy(
              fontSize = 10.5.sp,
            ),
            color = if (!item.isWatched) {
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BentoHorizonAudioCard(
  item: VideoWithPlaybackInfo,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null,
  scale: Float = 1.0f,
  modifier: Modifier = Modifier,
) {
  val video = item.video
  val progress = item.progressPercentage
  val originalCodec = remember(video) { getAudioCodec(video) }
  val displayTime = remember(video.durationFormatted) {
    video.durationFormatted.ifBlank { "00:00" }
  }
  val haptics = LocalHapticFeedback.current
  val cardWidth = (164f * scale.coerceIn(0.7f, 1.3f)).dp
  val thumbHeight = (186f * scale.coerceIn(0.7f, 1.3f)).dp

  Card(
    modifier = modifier
      .width(cardWidth)
      .clip(RoundedCornerShape(18.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick?.let { action ->
          {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            action()
          }
        },
      ),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Artwork Area - Wider Portrait Music Poster
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(thumbHeight)
          .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
      ) {
        BentoMediaThumbnail(
          video = video,
          modifier = Modifier.fillMaxSize(),
        )

        // Top-left Lossless / Original Audio Codec Badge
        Surface(
          modifier = Modifier
            .padding(7.dp)
            .align(Alignment.TopStart),
          shape = RoundedCornerShape(6.dp),
          color = Color.Black.copy(alpha = 0.72f),
          border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
        ) {
          Text(
            text = originalCodec,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
          )
        }

        // Bottom-right Duration / Remaining Time Pill
        Surface(
          modifier = Modifier
            .padding(7.dp)
            .align(Alignment.BottomEnd),
          shape = RoundedCornerShape(6.dp),
          color = Color.Black.copy(alpha = 0.75f),
        ) {
          Text(
            text = displayTime,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Medium,
            ),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
          )
        }

        // Mini progress indicator on thumbnail if track is in progress
        if (progress != null && progress > 0f && !item.isWatched) {
          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
              .fillMaxWidth()
              .height(3.5.dp)
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
          .background(
            Brush.verticalGradient(
              colors = listOf(
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                Color.Transparent,
              ),
            ),
          )
          .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        val (artist, title) = SortUtils.getAudioArtistAndTitle(video)
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 18.sp,
          ),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (artist.isNotBlank()) {
          Text(
            text = artist,
            style = MaterialTheme.typography.bodySmall.copy(
              fontSize = 12.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
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

@Composable
private fun HeroSpotlightBox(
  spotlightItem: VideoWithPlaybackInfo,
  resumePos: Double,
  heroSpotlightSeekbarStyle: HeroSpotlightSeekbarStyle,
  onPlayMedia: (Video, Double?, List<Video>) -> Unit,
  onVideoAction: (Video) -> Unit,
  videoItems: List<VideoWithPlaybackInfo>,
  cardHeight: androidx.compose.ui.unit.Dp,
  modifier: Modifier = Modifier,
  onOpenGestureHint: () -> Unit = {},
) {
  val context = LocalContext.current
  val haptics = LocalHapticFeedback.current
  val video = spotlightItem.video

  val playbackSessionState by PlaybackSession.state.collectAsState()
  val livePositionSec by PlaybackSession.propInt["time-pos"].collectAsState()
  val liveDurationSec by PlaybackSession.propInt["duration"].collectAsState()
  val isPaused by PlaybackSession.propBoolean["pause"].collectAsState()

  val isCurrentlyInSession = remember(playbackSessionState.currentItem, video) {
    matchesPlaybackItem(video, playbackSessionState.currentItem)
  }

  val totalSec = if (isCurrentlyInSession && liveDurationSec != null && liveDurationSec!! > 0) {
    liveDurationSec!!.toLong()
  } else {
    (video.duration / 1000L).coerceAtLeast(0L)
  }
  val durationMs = if (totalSec > 0L) totalSec * 1000L else video.duration.coerceAtLeast(0L)

  val currentPosSec = if (isCurrentlyInSession && livePositionSec != null && livePositionSec!! >= 0) {
    livePositionSec!!.toDouble()
  } else if (isCurrentlyInSession && resumePos > 0.0) {
    resumePos
  } else {
    null
  }

  val elapsedMs = currentPosSec?.let { (it * 1000.0).toLong().coerceIn(0L, durationMs.coerceAtLeast(1L)) }
    ?: (spotlightItem.progressPercentage?.let { (it * durationMs).toLong() }
    ?: spotlightItem.timeRemaining?.let { ((durationMs / 1000L) - it).coerceAtLeast(0L) * 1000L }
    ?: 0L)

  val progress = if (durationMs > 0L) (elapsedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
  val remainingMs = (durationMs - elapsedMs).coerceAtLeast(0L)
  val elapsedFormatted = VideoScanUtils.formatDuration(elapsedMs)
  val totalFormatted = if (totalSec > 0L) VideoScanUtils.formatDuration(totalSec * 1000L) else video.durationFormatted.ifBlank { VideoScanUtils.formatDuration(durationMs) }
  val remainingFormatted = VideoScanUtils.formatDuration(remainingMs)
  val progressPercent = (progress * 100f).roundToInt()

  val qualityBadge = remember(video.width, video.height, video.resolution, video.isAudio) {
    if (video.isAudio) {
      val ext = video.displayName.substringAfterLast('.', "").trim().uppercase()
      if (ext in listOf("FLAC", "ALAC", "WAV", "AIFF", "DSD")) "LOSSLESS" else null
    } else {
      val maxDim = maxOf(video.width, video.height)
      val minDim = minOf(video.width, video.height)
      val resUpper = video.resolution.uppercase()
      when {
        maxDim >= 3800 || minDim >= 2100 || resUpper.contains("4K") || resUpper.contains("2160") -> "4K ULTRA HD"
        maxDim >= 1900 || minDim >= 1000 || resUpper.contains("1080") || resUpper.contains("FHD") -> "1080P FHD"
        maxDim >= 1200 || minDim >= 700 || resUpper.contains("720") || resUpper.contains("HD") -> "720P HD"
        video.resolution.isNotBlank() -> video.resolution.uppercase()
        else -> null
      }
    }
  }

  val extensionBadge = remember(video.displayName) {
    val ext = video.displayName.substringAfterLast('.', "").trim().uppercase()
    if (ext.length in 2..5 && ext.all { it.isLetterOrDigit() }) ext else null
  }

  val runtimeFormatted = remember(totalSec, durationMs) {
    val sec = if (totalSec > 0L) totalSec else durationMs / 1000L
    if (sec <= 0L) ""
    else {
      val h = sec / 3600
      val m = (sec % 3600) / 60
      when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        m > 0 -> "${m}m"
        else -> "${sec}s"
      }
    }
  }

  Box(
    modifier = modifier
      .height(cardHeight)
      .clip(RoundedCornerShape(28.dp))
      .combinedClickable(
        onClick = {
          if (isCurrentlyInSession && PlaybackSession.isInitialized) {
            val intent = Intent(context, PlayerActivity::class.java).apply {
              flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            try {
              context.startActivity(intent)
            } catch (_: Exception) {
              onPlayMedia(video, resumePos.takeIf { it > 0.0 }, videoItems.map { it.video })
            }
          } else {
            onPlayMedia(video, resumePos.takeIf { it > 0.0 }, videoItems.map { it.video })
          }
        },
        onLongClick = {
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          onVideoAction(video)
        },
      )
      .background(MaterialTheme.colorScheme.surfaceContainerHighest)
      .border(
        1.dp,
        Brush.linearGradient(
          listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
            Color.Transparent,
          ),
        ),
        RoundedCornerShape(28.dp),
      ),
  ) {
    // Backdrop Media Thumbnail (High definition for hero spotlight banner)
    BentoMediaThumbnail(
      video = video,
      modifier = Modifier.fillMaxSize(),
      showPlayOverlay = false,
      highDefinition = true,
    )

    // Gradient Shade Overlays
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color.Black.copy(alpha = 0.25f),
              Color.Black.copy(alpha = 0.4f),
              Color.Black.copy(alpha = 0.92f),
            ),
          ),
        ),
    )

    // Top Glass Tag
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (isCurrentlyInSession) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .background(Color(0xFF00E5FF), CircleShape),
            )
          }
          Text(
            text = if (isCurrentlyInSession && !video.isAudio) "NOW PLAYING IN PIP" else (if (video.isAudio) "SONIC HORIZON" else "CONTINUE WATCHING"),
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 1.2.sp,
            ),
            color = Color.White,
          )
        }
      }
    }

    // Bottom Hero Info & Seekbar (Concept 1: Premier Cinema with Smart Media Badges & Depth Typography)
    Column(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .fillMaxWidth()
        .padding(
          horizontal = 16.dp,
          vertical = 12.dp,
        ),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      val context = LocalContext.current
      var formatBadges by remember(video.path) {
        mutableStateOf(if (video.isAudio) emptyList() else app.video.player.ui.player.videoToFormatBadges(video))
      }
      if (!video.isAudio) {
        LaunchedEffect(video.path) {
          kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            app.video.player.utils.media.MediaInfoOps
              .getMediaInfoReport(context, video.uri, video.displayName)
              .onSuccess { report ->
                val overview = report.data.overviewBadges(video.displayName)
                if (overview.isNotEmpty()) {
                  formatBadges = overview
                }
              }
          }
        }
      }
      if (formatBadges.isNotEmpty()) {
        app.video.player.ui.player.controls.components.MediaFormatBadges(
          badges = formatBadges,
          logoHeight = 14.dp,
          foreground = Color.White,
          background = Color.Black.copy(alpha = 0.6f),
        )
      }

      // Smart Media Badges Row
      if (video.hasEmbeddedSubtitles) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (video.hasEmbeddedSubtitles) {
            Surface(
              shape = RoundedCornerShape(5.dp),
              color = Color.White.copy(alpha = 0.12f),
              border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)),
            ) {
              Text(
                text = "CC",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 9.sp,
                  letterSpacing = 0.6.sp,
                ),
                color = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
              )
            }
          }
        }
      }

      Text(
        text = if (video.title.isNotBlank()) video.title else FileTypeUtils.stripExtension(video.displayName),
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Black,
          fontSize = 21.sp,
          letterSpacing = (-0.4).sp,
          shadow = Shadow(
            color = Color.Black.copy(alpha = 0.90f),
            offset = Offset(0f, 2f),
            blurRadius = 8f,
          ),
        ),
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      // Seekbar Placed Directly Below Media Title
      if (progress > 0f) {
        when (heroSpotlightSeekbarStyle) {
          HeroSpotlightSeekbarStyle.TimestampPill -> {
            Surface(
              modifier = Modifier.padding(top = 4.dp),
              shape = CircleShape,
              color = Color.Black.copy(alpha = 0.55f),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                ) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                  ) {
                    Icon(
                      imageVector = Icons.RoundedFilled.PlayArrow,
                      contentDescription = null,
                      modifier = Modifier
                        .size(12.dp)
                        .padding(start = 0.75.dp),
                      tint = MaterialTheme.colorScheme.onPrimary,
                    )
                  }
                }
                Text(
                  text = elapsedFormatted,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = Color.White,
                )
                Box(
                  modifier = Modifier
                    .width(80.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth(progress)
                      .fillMaxHeight()
                      .background(
                        Brush.horizontalGradient(
                          listOf(
                            MaterialTheme.colorScheme.primary,
                            Color(0xFF00E5FF),
                          ),
                        ),
                      ),
                  )
                }
                Text(
                  text = "$progressPercent%",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                  color = Color(0xFF00E5FF),
                )
                if (remainingFormatted.isNotBlank() && remainingMs > 0L) {
                  Text(
                    text = "-$remainingFormatted",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = Color.White.copy(alpha = 0.65f),
                  )
                }
              }
            }
          }

          HeroSpotlightSeekbarStyle.Disabled -> {
            // Seekbar disabled by user preference
          }
        }
      }
    }
  }
}

/**
 * Hero Spotlight Card displaying active/resuming video or pulsing audio track
 * with dynamic visualizer waves, specular highlights, and touch gestures.
 */
@Composable
private fun HeroSpotlightCard(
  item: VideoWithPlaybackInfo,
  onClick: () -> Unit,
  onLongClick: () -> Unit = {},
  onPrevious: () -> Unit = {},
  onPlayPause: () -> Unit = {},
  onNext: () -> Unit = {},
  scale: Float = 1.0f,
  modifier: Modifier = Modifier,
  cardHeight: androidx.compose.ui.unit.Dp? = null,
  includeHorizontalPadding: Boolean = true,
  nextTrackTitle: String? = null,
  onOpenGestureHint: () -> Unit = {},
) {
  val appearancePreferences = koinInject<AppearancePreferences>()
  val browserPreferences = koinInject<BrowserPreferences>()
  val audioPulsingProgressStyle by browserPreferences.audioPulsingProgressStyle.collectAsState()
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
      .then(if (includeHorizontalPadding) Modifier.padding(horizontal = 16.dp) else Modifier),
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

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isLandscapeCard = cardHeight != null || isLandscape

    Card(
      modifier = Modifier
        .fillMaxWidth()
        .then(if (cardHeight != null) Modifier.height(cardHeight) else if (isLandscapeCard) Modifier.height(236.dp) else Modifier)
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
          .then(if (cardHeight != null) Modifier.fillMaxHeight() else if (isLandscapeCard) Modifier.height(236.dp) else Modifier)
          .then(
            if (isLandscapeCard) {
              Modifier.clip(RoundedCornerShape(24.dp))
            } else {
              Modifier.background(
                Brush.linearGradient(
                  colors = listOf(
                    (if (isAudio) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary).copy(alpha = 0.12f),
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    MaterialTheme.colorScheme.surfaceContainerLowest,
                  ),
                ),
              )
            }
          ),
      ) {
        if (isLandscapeCard && isAudio) {
          // Concept 3: Cyberpunk DeX Console (Landscape Exclusive, NO BUTTONS)
          LandscapeAudioPulsingConcept3(
            item = item,
            isCurrentlyPlayingInSession = isCurrentlyPlayingInSession,
            originalCodec = originalCodec,
            totalSec = totalSec,
            totalFormatted = totalFormatted,
            remainingSec = remainingSec,
            remainingFormatted = remainingFormatted,
            pulseScale = pulseScale,
            wave1Color = wave1Color,
            wave2Color = wave2Color,
            wave3Color = wave3Color,
            scale = scale,
            nextTrackTitle = nextTrackTitle,
            onOpenGestureHint = onOpenGestureHint,
          )
        } else {
          // Clean original portrait mode Audio Pulsing OR Video Spotlight Card
          if (isLandscapeCard) {
            // Full-bleed Blurred Backdrop Thumbnail for Landscape Video Spotlight ONLY
            BentoMediaThumbnail(
              video = item.video,
              modifier = Modifier
                .fillMaxSize()
                .scale(1.25f)
                .blur(25.dp)
                .alpha(0.85f),
              showPlayOverlay = false,
              highDefinition = true,
            )

            // Dark Atmospheric Gradient Vignettes (Landscape only)
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    colors = listOf(
                      Color.Black.copy(alpha = 0.50f),
                      Color.Black.copy(alpha = 0.65f),
                      Color.Black.copy(alpha = 0.94f),
                    ),
                  ),
                ),
            )
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  Brush.horizontalGradient(
                    colors = listOf(
                      Color.Black.copy(alpha = 0.85f),
                      Color.Black.copy(alpha = 0.45f),
                      Color.Black.copy(alpha = 0.75f),
                    ),
                  ),
                ),
            )
          }

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .then(if (cardHeight != null) Modifier.fillMaxHeight() else Modifier)
              .padding(
                horizontal = 14.dp,
                vertical = (12f * scale.coerceIn(0.7f, 1.3f)).dp,
              ),
            verticalArrangement = if (cardHeight != null) Arrangement.SpaceBetween else Arrangement.spacedBy((7f * scale.coerceIn(0.7f, 1.3f)).dp),
          ) {
            // Top Header Row: Status Badge & Format Chip
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (isLandscapeCard) Color.Black.copy(alpha = 0.50f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
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

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Surface(
                  shape = RoundedCornerShape(999.dp),
                  color = if (isLandscapeCard) Color.Black.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.40f),
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
                    color = if (isLandscapeCard) Color.White.copy(alpha = 0.90f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.90f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  )
                }
              }
            }

            // Middle Row: Thumbnail Preview & Metadata
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              // Media Thumbnail Box
              Box {
                val thumbW = if (isLandscapeCard) (145f * scale.coerceIn(0.7f, 1.3f)).dp else if (isAudio) (70f * scale.coerceIn(0.7f, 1.3f)).dp else (108f * scale.coerceIn(0.7f, 1.3f)).dp
                val thumbH = if (isLandscapeCard) (145f * scale.coerceIn(0.7f, 1.3f)).dp else (66f * scale.coerceIn(0.7f, 1.3f)).dp
                BentoMediaThumbnail(
                  video = item.video,
                  modifier = Modifier
                    .size(width = thumbW, height = thumbH)
                    .clip(RoundedCornerShape(if (isLandscapeCard) 20.dp else 16.dp)),
                  showPlayOverlay = !isAudio,
                )
              }

              // Information & Visualizer
              Column(
                modifier = Modifier
                  .weight(1f)
                  .then(if (cardHeight != null) Modifier.fillMaxHeight() else Modifier),
                verticalArrangement = if (cardHeight != null) Arrangement.SpaceBetween else Arrangement.spacedBy(4.dp),
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
                  style = if (isLandscapeCard) MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = if (isLandscapeCard) Color.White else MaterialTheme.colorScheme.onSurface,
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
                  color = if (isLandscapeCard) Color.White.copy(alpha = 0.80f) else MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )

                if (isAudio) {
                  val isPaused by PlaybackSession.propBoolean["pause"].collectAsState()
                  val isPlaying = isCurrentlyPlayingInSession && (isPaused == false)
                  val targetProgress = if (isCurrentlyPlayingInSession && totalSec > 0L && livePositionSec != null) {
                    (livePositionSec!!.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
                  } else if (progress > 0f) {
                    progress
                  } else {
                    0f
                  }

                  val animatedProgress by animateFloatAsState(
                    targetValue = targetProgress,
                    animationSpec = if (isPlaying) tween(durationMillis = 1000, easing = LinearEasing) else spring(),
                    label = "audioPulsingProgress",
                  )
                  val currentProgress = if (isPlaying) animatedProgress else targetProgress

                  if (audioPulsingProgressStyle == app.video.player.preferences.AudioPulsingProgressStyle.WavyVisualizer) {
                    OneUi9WaveformVisualizer(
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isLandscapeCard) 34.dp else 24.dp)
                        .padding(top = 2.dp),
                      wave1Color = wave1Color,
                      wave2Color = wave2Color,
                      wave3Color = wave3Color,
                      isPlaying = isPlaying,
                      progress = currentProgress,
                    )
                  } else {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      LinearProgressIndicator(
                        progress = { currentProgress },
                        modifier = Modifier
                          .weight(1f)
                          .height(4.dp)
                          .clip(RoundedCornerShape(999.dp)),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                      )
                      Text(
                        text = "${(currentProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.tertiary,
                      )
                    }
                  }
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
}

/**
 * Concept 3: Cyberpunk DeX Console (Audio Pulsing Landscape Exclusive)
 * Information-rich, 3-column audio command stage with live VU meters,
 * technical telemetry, upcoming queue preview, and touch gestures (NO on-card buttons).
 */
@Composable
private fun LandscapeAudioPulsingConcept3(
  item: VideoWithPlaybackInfo,
  isCurrentlyPlayingInSession: Boolean,
  originalCodec: String,
  totalSec: Long,
  totalFormatted: String,
  remainingSec: Long,
  remainingFormatted: String,
  pulseScale: Float,
  wave1Color: Color,
  wave2Color: Color,
  wave3Color: Color,
  scale: Float = 1.0f,
  nextTrackTitle: String? = null,
  onOpenGestureHint: () -> Unit = {},
) {
  val playbackSessionState by PlaybackSession.state.collectAsState()
  val isPaused by PlaybackSession.propBoolean["pause"].collectAsState()
  val livePositionSec by PlaybackSession.propInt["time-pos"].collectAsState()
  val progress = item.progressPercentage ?: 0f

  val isPlaying = isCurrentlyPlayingInSession && (isPaused == false)

  val (artist, title) = if (isCurrentlyPlayingInSession && playbackSessionState.currentItem != null) {
    val cur = playbackSessionState.currentItem!!
    val displayTitle = cur.title?.takeIf { it.isNotBlank() }
      ?: (if (item.video.title.isNotBlank()) item.video.title else FileTypeUtils.stripExtension(item.video.displayName))
    val displayArtist = cur.artist?.takeIf { it.isNotBlank() } ?: item.video.artist
    Pair(displayArtist, displayTitle)
  } else {
    SortUtils.getAudioArtistAndTitle(item.video)
  }

  val targetProgress = if (isCurrentlyPlayingInSession && totalSec > 0L && livePositionSec != null) {
    (livePositionSec!!.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
  } else if (progress > 0f) {
    progress
  } else {
    0f
  }

  val animatedProgress by animateFloatAsState(
    targetValue = targetProgress,
    animationSpec = if (isPlaying) tween(durationMillis = 1000, easing = LinearEasing) else spring(),
    label = "audioPulsingConcept3Progress",
  )
  val currentProgress = if (isPlaying) animatedProgress else targetProgress

  val elapsedSec = if (isCurrentlyPlayingInSession && livePositionSec != null) {
    livePositionSec!!.toLong().coerceAtLeast(0L)
  } else if (progress > 0f && totalSec > 0L) {
    (progress * totalSec).toLong().coerceAtLeast(0L)
  } else {
    0L
  }
  val elapsedFormatted = VideoScanUtils.formatDuration(elapsedSec * 1000L)

  val calculatedBitrateKbps = remember(item.video.size, item.video.duration) {
    if (item.video.duration > 0L && item.video.size > 0L) {
      val durSec = item.video.duration / 1000.0
      val bits = item.video.size * 8.0
      val kbps = (bits / durSec / 1000.0).roundToInt()
      if (kbps > 0) "${kbps}k" else "Lossless"
    } else {
      "Lossless"
    }
  }

  // Real-time VU meter spring and bounce physics
  val vuTransition = rememberInfiniteTransition(label = "vuMeterTransition")
  val vuLeftAnim by vuTransition.animateFloat(
    initialValue = 0.22f,
    targetValue = 0.94f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 360, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "vuLeftAnim",
  )
  val vuRightAnim by vuTransition.animateFloat(
    initialValue = 0.18f,
    targetValue = 0.88f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 440, easing = LinearOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "vuRightAnim",
  )

  val currentVuLeft by animateFloatAsState(
    targetValue = if (isPlaying) vuLeftAnim else 0.04f,
    animationSpec = tween(durationMillis = 180),
    label = "currentVuLeft",
  )
  val currentVuRight by animateFloatAsState(
    targetValue = if (isPlaying) vuRightAnim else 0.04f,
    animationSpec = tween(durationMillis = 180),
    label = "currentVuRight",
  )

  // Backdrop: Full-bleed blurred thumbnail + Cyberpunk atmospheric gradients
  BentoMediaThumbnail(
    video = item.video,
    modifier = Modifier
      .fillMaxSize()
      .scale(1.25f)
      .blur(25.dp)
      .alpha(0.38f),
    showPlayOverlay = false,
    highDefinition = true,
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.horizontalGradient(
          colors = listOf(
            Color.Black.copy(alpha = 0.94f),
            Color.Black.copy(alpha = 0.80f),
            Color(0xFF061410).copy(alpha = 0.55f),
          ),
        ),
      ),
  )
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color.Black.copy(alpha = 0.55f),
            Color.Transparent,
            Color.Black.copy(alpha = 0.88f),
          ),
        ),
      ),
  )

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 14.dp, vertical = (12f * scale.coerceIn(0.7f, 1.3f)).dp),
  ) {
    val showColumn3 = maxWidth >= 460.dp

    Row(
      modifier = Modifier.fillMaxSize(),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // =========================================================================
      // COLUMN 1: Artwork & Spec Telemetry
      // =========================================================================
      val artSize = if (showColumn3) 124.dp else 105.dp
      Column(
        modifier = Modifier
          .width(artSize)
          .fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Squircle Artwork with Specular Glass Border & Pinned Badge
        Box(
          modifier = Modifier
            .size(artSize)
            .clip(RoundedCornerShape(20.dp))
            .border(
              width = 1.dp,
              brush = Brush.linearGradient(
                listOf(
                  Color.White.copy(alpha = 0.40f),
                  Color.White.copy(alpha = 0.10f),
                  Color.Transparent,
                ),
              ),
              shape = RoundedCornerShape(20.dp),
            ),
        ) {
          BentoMediaThumbnail(
            video = item.video,
            modifier = Modifier.fillMaxSize(),
            showPlayOverlay = false,
            highDefinition = true,
          )

          // Pinned bottom codec badge inside artwork
          Row(
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth()
              .background(
                Brush.verticalGradient(
                  listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)),
                ),
              )
              .padding(horizontal = 7.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = if (originalCodec in listOf("FLAC", "DSD", "WAV", "AIFF", "ALAC")) "HI-RES" else "STUDIO",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 7.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
              ),
              color = Color(0xFFFFD54F),
            )
            Text(
              text = originalCodec,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
              ),
              color = Color.White,
            )
          }
        }

        // Tech Spec Box below Artwork
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color.Black.copy(alpha = 0.70f),
          border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "BITRATE",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f),
              )
              Text(
                text = calculatedBitrateKbps,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 7.5.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                ),
                color = Color.White,
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "CHANNELS",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f),
              )
              Text(
                text = "2.0 CH",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 7.5.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                ),
                color = Color.White,
              )
            }
          }
        }
      }

      // =========================================================================
      // COLUMN 2: Command Core (Title, Artist, Waveform, Scrubber)
      // =========================================================================
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Top Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Status capsule
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.Black.copy(alpha = 0.60f),
            border = BorderStroke(
              width = 1.dp,
              brush = Brush.linearGradient(
                colors = listOf(
                  MaterialTheme.colorScheme.tertiary.copy(alpha = 0.80f),
                  Color.White.copy(alpha = 0.35f),
                  MaterialTheme.colorScheme.tertiary.copy(alpha = 0.20f),
                ),
              ),
            ),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .scale(pulseScale)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.tertiary),
              )
              Text(
                text = "AUDIO PULSING • LIVE",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 9.sp,
                  letterSpacing = 0.8.sp,
                ),
                color = MaterialTheme.colorScheme.tertiary,
              )
            }
          }

          if (!showColumn3 && !nextTrackTitle.isNullOrBlank()) {
            // In compact width, show next track pill in top header row
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = Color.Black.copy(alpha = 0.50f),
              border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f)),
              modifier = Modifier.widthIn(max = 160.dp),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = "NEXT:",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                  ),
                  color = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                  text = nextTrackTitle,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                  ),
                  color = Color.White.copy(alpha = 0.90f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          } else {
            // Codec spec pill
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = Color.Black.copy(alpha = 0.50f),
              border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
            ) {
              Text(
                text = if (originalCodec in listOf("FLAC", "DSD", "WAV", "AIFF", "ALAC")) "$originalCodec • 24-BIT LOSSLESS" else "$originalCodec STREAM",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 9.sp,
                ),
                color = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
        }

        // Title and Artist Block
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.ExtraBold,
              fontSize = 17.sp,
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          Text(
            text = if (artist.isNotBlank() && !artist.equals("Unknown Artist", ignoreCase = true)) {
              "$artist • ${item.video.bucketDisplayName.ifBlank { "Lossless Audio" }}"
            } else {
              item.video.bucketDisplayName.ifBlank { "Lossless Audio" }
            },
            style = MaterialTheme.typography.bodySmall.copy(
              fontWeight = FontWeight.SemiBold,
              fontSize = 11.5.sp,
            ),
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.90f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        // Waveform Visualizer Ribbon
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
          contentAlignment = Alignment.Center,
        ) {
          OneUi9WaveformVisualizer(
            modifier = Modifier
              .fillMaxWidth()
              .fillMaxHeight(),
            wave1Color = wave1Color,
            wave2Color = wave2Color,
            wave3Color = wave3Color,
            isPlaying = isPlaying,
            progress = currentProgress,
          )
        }

        // Progress Scrubber Bar Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = elapsedFormatted,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.tertiary,
          )

          Box(
            modifier = Modifier
              .weight(1f)
              .height(4.dp)
              .clip(RoundedCornerShape(999.dp))
              .background(Color.White.copy(alpha = 0.15f)),
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(currentProgress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(999.dp))
                .background(
                  Brush.horizontalGradient(
                    listOf(
                      MaterialTheme.colorScheme.tertiary,
                      Color(0xFF00E5FF),
                    ),
                  ),
                ),
            )
          }

          Text(
            text = remainingFormatted,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.SemiBold,
            ),
            color = Color.White.copy(alpha = 0.70f),
          )
        }
      }

      // =========================================================================
      // COLUMN 3: Soundstage Telemetry & Gesture Context (NO BUTTONS!)
      // =========================================================================
      if (showColumn3) {
        Column(
          modifier = Modifier
            .width(115.dp)
            .fillMaxHeight(),
          verticalArrangement = Arrangement.SpaceBetween,
        ) {
          // Stereo VU Meters
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.Black.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "VU METERS",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                  ),
                  color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.90f),
                )
                Text(
                  text = if (isPlaying) "+3 dB" else "-∞ dB",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                  ),
                  color = if (isPlaying) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.40f),
                )
              }

              // Left Channel Bar
              VuChannelBar(
                label = "L",
                level = currentVuLeft,
                accentColor = MaterialTheme.colorScheme.tertiary,
              )

              // Right Channel Bar
              VuChannelBar(
                label = "R",
                level = currentVuRight,
                accentColor = Color(0xFF00E5FF),
              )
            }
          }

          // Upcoming Track Preview Pill
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.55f),
            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Text(
                text = "NEXT UP ❯",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Black,
                  letterSpacing = 0.6.sp,
                ),
                color = MaterialTheme.colorScheme.tertiary,
              )
              Text(
                text = nextTrackTitle ?: "End of Queue",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 9.5.sp,
                  fontWeight = FontWeight.SemiBold,
                ),
                color = Color.White.copy(alpha = 0.90f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun VuChannelBar(
  label: String,
  level: Float,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 7.5.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
      ),
      color = Color.White.copy(alpha = 0.60f),
    )
    Box(
      modifier = Modifier
        .weight(1f)
        .height(5.dp)
        .clip(RoundedCornerShape(999.dp))
        .background(Color.White.copy(alpha = 0.10f)),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(level.coerceIn(0f, 1f))
          .fillMaxHeight()
          .clip(RoundedCornerShape(999.dp))
          .background(
            Brush.horizontalGradient(
              listOf(
                accentColor.copy(alpha = 0.6f),
                accentColor,
                if (level > 0.82f) Color(0xFFFF5252) else accentColor,
              ),
            ),
          ),
      )
    }
  }
}

/**
 * Spotlight Gestures Information Modal Dialog
 * Liquid-glass interactive guide explaining touch gestures for Continue Watching & Audio Pulsing cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpotlightGesturesInfoDialog(
  initialShowEveryTime: Boolean = false,
  onDismiss: (showEveryTime: Boolean) -> Unit,
) {
  var showEveryTime by remember { mutableStateOf(initialShowEveryTime) }

  BasicAlertDialog(
    onDismissRequest = { onDismiss(showEveryTime) },
    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      border = BorderStroke(
        width = 1.dp,
        brush = Brush.linearGradient(
          listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
            Color.Transparent,
          ),
        ),
      ),
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .widthIn(max = 460.dp)
        .padding(vertical = 24.dp),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Dialog Title Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                  Brush.linearGradient(
                    listOf(
                      MaterialTheme.colorScheme.primary,
                      MaterialTheme.colorScheme.tertiary,
                    ),
                  ),
                ),
              contentAlignment = Alignment.Center,
            ) {
              Text(text = "✨", fontSize = 18.sp)
            }
            Column {
              Text(
                text = "Spotlight Gestures Guide",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "Touch controls for quick playback",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          IconButton(
            onClick = { onDismiss(showEveryTime) },
            modifier = Modifier.size(32.dp),
          ) {
            Text(
              text = "✕",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // Section 1: Continue Watching (Video Card)
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLowest,
          border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "🎬 CONTINUE WATCHING",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Black,
                  letterSpacing = 0.8.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = "Video",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.sp,
                  fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              )
            }

            GestureGuideItem(
              icon = "👆",
              gestureName = "Single Tap",
              description = "Resume video from saved position",
              accentColor = MaterialTheme.colorScheme.primary,
            )

            GestureGuideItem(
              icon = "⏳",
              gestureName = "Long Press",
              description = "Options menu (Rename, Hide in Secure Folder, Delete)",
              accentColor = MaterialTheme.colorScheme.primary,
            )
          }
        }

        // Section 2: Audio Pulsing (Music Card)
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLowest,
          border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "🎵 AUDIO PULSING",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Black,
                  letterSpacing = 0.8.sp,
                ),
                color = MaterialTheme.colorScheme.tertiary,
              )
              Text(
                text = "Music",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.sp,
                  fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              )
            }

            GestureGuideItem(
              icon = "👆",
              gestureName = "Single Tap",
              description = "Play / Pause background audio",
              accentColor = MaterialTheme.colorScheme.tertiary,
            )

            GestureGuideItem(
              icon = "↔️",
              gestureName = "Horizontal Swipe",
              description = "Swipe left for next • Swipe right for previous track",
              accentColor = MaterialTheme.colorScheme.tertiary,
            )

            GestureGuideItem(
              icon = "⏳",
              gestureName = "Long Press",
              description = "Open full music player & queue screen",
              accentColor = MaterialTheme.colorScheme.tertiary,
            )
          }
        }

        // Dismiss Button
        Button(
          onClick = { onDismiss(showEveryTime) },
          shape = RoundedCornerShape(999.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        ) {
          Text(
            text = "Got It",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Bold,
            ),
          )
        }
      }
    }
  }
}

@Composable
private fun GestureGuideItem(
  icon: String,
  gestureName: String,
  description: String,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = accentColor.copy(alpha = 0.12f),
      border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.30f)),
      modifier = Modifier.size(34.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(text = icon, fontSize = 14.sp)
      }
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = gestureName,
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.Bold,
        ),
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall.copy(
          fontSize = 11.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
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
  wave1Color: Color = Color(0xFF4338CA), // One UI 9 Liquid Aura: Luminous Royal Indigo (1st wave: bass veil)
  wave2Color: Color = Color(0xFF8B5CF6), // One UI 9 Liquid Aura: Electric Violet (2nd wave: mid veil)
  wave3Color: Color = Color(0xFF38BDF8), // One UI 9 Liquid Aura: Galaxy Sky Cyan (Last wave: lead crest)
  isPlaying: Boolean = true,
  progress: Float = 0f,
) {
  var frameNanos by remember { mutableLongStateOf(0L) }
  var phase1 by remember { mutableFloatStateOf(0f) }
  var phase2 by remember { mutableFloatStateOf(1.2f) }
  var phase3 by remember { mutableFloatStateOf(2.4f) }

  val twoPi = (2.0 * PI).toFloat()

  LaunchedEffect(isPlaying) {
    if (!isPlaying) return@LaunchedEffect
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

  // Smoothly damp amplitude from flat (0f) to active wave height (1f) with a spring spec for fluid startup and flatline on pause
  val animatedAmpScale by animateFloatAsState(
    targetValue = if (isPlaying) 1f else 0f,
    animationSpec = spring(
      dampingRatio = 0.82f,
      stiffness = Spring.StiffnessLow,
    ),
    label = "waveAmpScale",
  )

  val minWaveActiveWidth = with(LocalDensity.current) { 2.dp.toPx() }
  val filledPath = remember { Path() }
  val contourPath = remember { Path() }

  Canvas(modifier = modifier) {
    @Suppress("UNUSED_VARIABLE")
    val frame = frameNanos // Subscribes Canvas draw phase to hardware vsync frames without recomposing
    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) return@Canvas

    val baselineY = height - 1.5.dp.toPx()
    val availableLift = (baselineY - 1.dp.toPx()).coerceAtLeast(0f) * animatedAmpScale
    val step = 2.dp.toPx().coerceAtLeast(1f)
    val clampedProgress = progress.coerceIn(0f, 1f)
    if (clampedProgress <= 0f && !isPlaying) return@Canvas
    val waveActiveWidth = (width * (if (isPlaying) clampedProgress.coerceAtLeast(0.01f) else clampedProgress)).coerceIn(minWaveActiveWidth, width)

    val taperInset = 12.dp.toPx().coerceAtMost(waveActiveWidth / 4f)

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
      filledPath.reset()
      contourPath.reset()

      if (lift <= 0.5f) {
        // Flat wave line across the elapsed progress at baseline (when music is paused/stopped)
        contourPath.moveTo(0f, baselineY)
        contourPath.lineTo(waveActiveWidth, baselineY)
      } else {
        fun ribbonY(x: Float): Float {
          val edgeIn = if (taperInset > 0f) smoothstep(x / taperInset) else 1f
          val edgeOut = if (taperInset > 0f) smoothstep((waveActiveWidth - x) / taperInset) else 1f
          val envelope = edgeIn * edgeOut
          val undulation = 0.50f + 0.50f * sin((x / wavelength) * twoPi - phaseOffset)
          return baselineY - lift * undulation * envelope
        }

        val startY = ribbonY(0f)
        filledPath.moveTo(0f, baselineY)
        filledPath.lineTo(0f, startY)
        contourPath.moveTo(0f, startY)

        var x = 0f
        while (x < waveActiveWidth) {
          x = min(x + step, waveActiveWidth)
          val y = ribbonY(x)
          filledPath.lineTo(x, y)
          contourPath.lineTo(x, y)
        }
        filledPath.lineTo(waveActiveWidth, baselineY)
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
      }

      // 2. Diffused Luminous Glow Halo along wave crests / flat wave
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

      // 3. Crisp Luminous Crest / Flat Wave Contour Stroke
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

    // Layer 1: Ambient Luminous Bass Veil (Back wave - goes highest at the top)
    drawWaveRibbon(
      wavelength = 92.dp.toPx(),
      lift = availableLift * 0.96f,
      phaseOffset = phase1,
      fillTopColor = wave1Color.copy(alpha = 0.38f),
      fillBottomColor = wave1Color.copy(alpha = 0.08f),
      strokeColor = wave1Color.copy(alpha = 0.78f),
      strokeWidth = 1.4.dp.toPx(),
      glowColor = wave1Color.copy(alpha = 0.40f),
      glowWidth = 5.dp.toPx(),
    )

    // Layer 2: Harmonic Mid Veil (Mid wave - slightly down from back wave)
    drawWaveRibbon(
      wavelength = 62.dp.toPx(),
      lift = availableLift * 0.78f,
      phaseOffset = phase2,
      fillTopColor = wave2Color.copy(alpha = 0.42f),
      fillBottomColor = wave1Color.copy(alpha = 0.10f),
      strokeColor = wave2Color.copy(alpha = 0.88f),
      strokeWidth = 1.6.dp.toPx(),
      glowColor = wave2Color.copy(alpha = 0.38f),
      glowWidth = 5.5.dp.toPx(),
    )

    // Layer 3: Vibrant Lead Luminous Wave with Specular Core (Front wave - slightly down from mid wave)
    drawWaveRibbon(
      wavelength = 42.dp.toPx(),
      lift = availableLift * 0.62f,
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
 * Bento Media Action Bottom Sheet
 * Triggered on long-pressing movie and music cards in BentoFusion rails.
 * Provides Rename, Hide, and Delete options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BentoMediaActionSheet(
  video: Video,
  onDismiss: () -> Unit,
  onRename: () -> Unit,
  onHide: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  @Suppress("DEPRECATION")
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val isAudio = video.isAudio
  val (artist, title) = if (isAudio) {
    SortUtils.getAudioArtistAndTitle(video)
  } else {
    "" to FileTypeUtils.stripExtension(video.displayName)
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Header preview card
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          // Thumbnail
          Box(
            modifier = Modifier
              .size(60.dp)
              .clip(RoundedCornerShape(14.dp)),
          ) {
            BentoMediaThumbnail(
              video = video,
              modifier = Modifier.fillMaxSize(),
              showPlayOverlay = false,
            )
          }

          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              if (artist.isNotBlank()) {
                Text(
                  text = artist,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = "•",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.outline,
                )
              }
              val durationText = video.durationFormatted.ifBlank { "00:00" }
              Text(
                text = durationText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              val badge = if (isAudio) getAudioCodec(video) else video.resolution.ifBlank { "VIDEO" }
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = (if (isAudio) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary).copy(alpha = 0.15f),
              ) {
                Text(
                  text = badge,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                  color = if (isAudio) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                )
              }
            }
          }
        }
      }

      // Actions Section
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // 1. Rename
        BentoActionItemRow(
          icon = Icons.RoundedFilled.DriveFileRenameOutline,
          title = stringResource(R.string.rename),
          subtitle = "Change file name",
          iconTint = MaterialTheme.colorScheme.primary,
          onClick = onRename,
        )

        // 2. Hide
        BentoActionItemRow(
          icon = Icons.RoundedFilled.VisibilityOff,
          title = stringResource(R.string.bento_action_hide),
          subtitle = "Hide from library (accessible in Folder Blacklist)",
          iconTint = MaterialTheme.colorScheme.tertiary,
          onClick = onHide,
        )

        // 3. Delete
        BentoActionItemRow(
          icon = Icons.RoundedFilled.Delete,
          title = stringResource(R.string.delete),
          subtitle = "Permanently remove from device storage",
          iconTint = MaterialTheme.colorScheme.error,
          textColor = MaterialTheme.colorScheme.error,
          onClick = onDelete,
        )
      }
    }
  }
}

@Composable
private fun BentoActionItemRow(
  icon: AppIcon,
  title: String,
  subtitle: String,
  iconTint: Color,
  textColor: Color = MaterialTheme.colorScheme.onSurface,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.6f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = iconTint.copy(alpha = 0.12f),
        modifier = Modifier.size(42.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
          )
        }
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
          color = textColor,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
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
  highDefinition: Boolean = false,
) {
  val thumbnailRepository = koinInject<ThumbnailRepository>()
  val density = LocalDensity.current
  val isAudio = video.isAudio
  val thumbWidthPx = with(density) {
    if (highDefinition) {
      if (isAudio) 640.dp.roundToPx() else 960.dp.roundToPx()
    } else {
      (if (isAudio) 328.dp else 432.dp).roundToPx()
    }
  }
  val thumbHeightPx = with(density) {
    if (highDefinition) {
      if (isAudio) 640.dp.roundToPx() else 540.dp.roundToPx()
    } else {
      (if (isAudio) 372.dp else 244.dp).roundToPx()
    }
  }

  var thumbnail by remember(video.path, highDefinition) {
    mutableStateOf<Bitmap?>(thumbnailRepository.peekThumbnailFromMemory(video, thumbWidthPx, thumbHeightPx))
  }

  LaunchedEffect(video.path, highDefinition) {
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
      val bmp = thumbnail!!
      val targetAspect = if (video.isAudio) (164f / 186f) else 16f / 9f
      val bmpAspect = bmp.width.toFloat() / bmp.height.toFloat()
      val aspectMismatch = kotlin.math.abs(bmpAspect - targetAspect) > 0.20f || (!video.isAudio && bmp.height > bmp.width * 1.05f)
      if (aspectMismatch) {
        Image(
          bitmap = bmp.asImageBitmap(),
          contentDescription = null,
          modifier = Modifier.fillMaxSize().blur(16.dp).alpha(0.55f),
          contentScale = ContentScale.Crop,
        )
        Image(
          bitmap = bmp.asImageBitmap(),
          contentDescription = null,
          modifier = Modifier.fillMaxSize(),
          contentScale = if (video.isAudio) ContentScale.Crop else ContentScale.Fit,
        )
      } else {
        Image(
          bitmap = bmp.asImageBitmap(),
          contentDescription = null,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
      }
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

