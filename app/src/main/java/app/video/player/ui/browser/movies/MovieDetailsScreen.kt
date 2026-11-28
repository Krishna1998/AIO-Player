/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.browser.movies

import androidx.compose.ui.graphics.graphicsLayer

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.video.player.R
import app.video.player.domain.media.model.Video
import app.video.player.domain.movies.MovieClassifier
import app.video.player.domain.movies.MovieMetadata
import app.video.player.domain.movies.MovieMetadataRepository
import app.video.player.ui.mediainfo.overviewBadges
import app.video.player.preferences.BrowserPreferences
import app.video.player.presentation.Screen
import app.video.player.presentation.components.RemoteImage
import app.video.player.repository.MediaFileRepository
import app.video.player.ui.browser.dialogs.DeleteConfirmationDialog
import app.video.player.ui.components.AppToast as Toast
import app.video.player.ui.icons.Icon
import app.video.player.ui.icons.Icons
import app.video.player.ui.utils.LocalBackStack
import app.video.player.ui.utils.NavigationBackHandler as BackHandler
import app.video.player.ui.utils.popSafely
import app.video.player.utils.media.MediaUtils
import app.video.player.utils.media.PlaybackStateOps
import app.video.player.utils.permission.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import java.io.File

/**
 * Rich Movie Details Screen
 * Displays a cinematic backdrop, online metadata badges, synopsis from TMDB,
 * top action bar (Refresh, Edit, Watched Toggle, Delete) and a prominent "Play Movie" action button.
 */
@Serializable
data class MovieDetailsScreen(
  val videoPath: String,
) : Screen {

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backStack = LocalBackStack.current
    val coroutineScope = rememberCoroutineScope()
    val movieMetadataRepo = koinInject<MovieMetadataRepository>()
    val browserPreferences = koinInject<BrowserPreferences>()

    var video by remember { mutableStateOf<Video?>(null) }
    var metadata by remember { mutableStateOf<MovieMetadata?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isWatched by remember { mutableStateOf(false) }
    var hasStarted by remember { mutableStateOf(false) }
    var lastPositionSec by remember { mutableStateOf(0) }
    var seriesEpisodes by remember { mutableStateOf<List<Pair<Video, app.video.player.domain.movies.MovieClassifier.SeriesEpisodeInfo>>>(emptyList()) }
    var episodeProgressMap by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    val playbackStateRepo = koinInject<app.video.player.domain.playbackstate.repository.PlaybackStateRepository>()
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var resumeRefreshTick by remember { mutableStateOf(0) }
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
      val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
          resumeRefreshTick++
        }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Load video item and metadata (initial load)
    LaunchedEffect(videoPath) {
      withContext(Dispatchers.IO) {
        val resolved = MediaFileRepository.getVideosFromFiles(context, listOf(File(videoPath))).firstOrNull()
        video = resolved
        if (resolved != null) {
          // 1. Show cached metadata immediately without waiting for series/playback queries
          val cached = movieMetadataRepo.getCachedMetadata(resolved)
          if (cached != null) {
            metadata = cached
          }

          // 2. Fetch missing metadata, playback state, and series episodes in parallel
          kotlinx.coroutines.coroutineScope {
            val metaJob = if (cached == null) {
              async {
                isRefreshing = true
                metadata = movieMetadataRepo.getOrFetchMetadata(resolved)
                isRefreshing = false
              }
            } else null

            val playbackJob = async {
              isWatched = PlaybackStateOps.areAllWatched(
                listOf(resolved),
                browserPreferences.watchedThreshold.get(),
              )
              val savedData = playbackStateRepo.getVideoDataByTitle(resolved.displayName)
                ?: playbackStateRepo.getVideoDataByTitle(resolved.title)
                ?: playbackStateRepo.getVideoDataByTitle(File(resolved.path).name)
              val savedPos = savedData?.lastPosition ?: 0
              lastPositionSec = savedPos
              hasStarted = savedPos > 0
            }

            val seriesJob = async {
              val seriesInfo = app.video.player.domain.movies.MovieClassifier.extractSeriesEpisodeInfo(resolved)
              if (seriesInfo != null) {
                val parentDir = File(resolved.path).parentFile
                val siblingFiles = parentDir?.listFiles()?.filter { it.isFile } ?: emptyList()
                val siblingVideos = MediaFileRepository.getVideosFromFiles(context, siblingFiles)
                val matched = siblingVideos.mapNotNull { v ->
                  val info = app.video.player.domain.movies.MovieClassifier.extractSeriesEpisodeInfo(v)
                  if (info != null && info.seriesKey == seriesInfo.seriesKey) v to info else null
                }.sortedWith(compareBy({ it.second.season }, { it.second.episode }, { it.first.displayName }))
                if (matched.size > 1) {
                  seriesEpisodes = matched
                  val threshold = browserPreferences.watchedThreshold.get()
                  episodeProgressMap = matched.associate { (epVideo, _) ->
                    val epWatched = PlaybackStateOps.areAllWatched(listOf(epVideo), threshold)
                    val progress = if (epWatched) {
                      1f
                    } else {
                      val epSaved = playbackStateRepo.getVideoDataByTitle(epVideo.displayName)
                        ?: playbackStateRepo.getVideoDataByTitle(epVideo.title)
                        ?: playbackStateRepo.getVideoDataByTitle(File(epVideo.path).name)
                        ?: playbackStateRepo.getVideoDataByTitle(File(epVideo.path).nameWithoutExtension)
                      val epPos = epSaved?.lastPosition ?: 0
                      if (epPos > 0 && epVideo.duration > 0L) {
                        (epPos * 1000f / epVideo.duration.toFloat()).coerceIn(0f, 1f)
                      } else {
                        0f
                      }
                    }
                    epVideo.path to progress
                  }
                }
              }
            }

            metaJob?.await()
            playbackJob.await()
            seriesJob.await()
          }
        }
      }
    }

    // Lightweight playback-only refresh on resume (including episode progress)
    LaunchedEffect(resumeRefreshTick) {
      if (resumeRefreshTick == 0) return@LaunchedEffect
      val resolved = video ?: return@LaunchedEffect
      withContext(Dispatchers.IO) {
        val threshold = browserPreferences.watchedThreshold.get()
        isWatched = PlaybackStateOps.areAllWatched(listOf(resolved), threshold)
        val savedData = playbackStateRepo.getVideoDataByTitle(resolved.displayName)
          ?: playbackStateRepo.getVideoDataByTitle(resolved.title)
          ?: playbackStateRepo.getVideoDataByTitle(File(resolved.path).name)
        val savedPos = savedData?.lastPosition ?: 0
        lastPositionSec = savedPos
        hasStarted = savedPos > 0

        if (seriesEpisodes.isNotEmpty()) {
          episodeProgressMap = seriesEpisodes.associate { (epVideo, _) ->
            val epWatched = PlaybackStateOps.areAllWatched(listOf(epVideo), threshold)
            val progress = if (epWatched) {
              1f
            } else {
              val epSaved = playbackStateRepo.getVideoDataByTitle(epVideo.displayName)
                ?: playbackStateRepo.getVideoDataByTitle(epVideo.title)
                ?: playbackStateRepo.getVideoDataByTitle(File(epVideo.path).name)
                ?: playbackStateRepo.getVideoDataByTitle(File(epVideo.path).nameWithoutExtension)
              val epPos = epSaved?.lastPosition ?: 0
              if (epPos > 0 && epVideo.duration > 0L) {
                (epPos * 1000f / epVideo.duration.toFloat()).coerceIn(0f, 1f)
              } else {
                0f
              }
            }
            epVideo.path to progress
          }
        }
      }
    }

    BackHandler {
      backStack.popSafely()
    }

    val currentVideo = video
    val currentMeta = metadata

    val infiniteTransition = rememberInfiniteTransition(label = "refreshSpin")
    val spinAngle by infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(
        animation = tween(900, easing = LinearEasing),
        repeatMode = RepeatMode.Restart,
      ),
      label = "refreshSpinAngle",
    )

    Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(bottom = paddingValues.calculateBottomPadding()),
      ) {
        if (currentVideo == null) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          }
        } else {
          val scrollState = rememberScrollState()

          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(scrollState),
          ) {
            // Backdrop Header (mpvium-style responsive height & natural bottom-edge blend)
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isTablet = configuration.smallestScreenWidthDp >= 600
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val headerHeight = when {
              isTablet && isLandscape -> 240.dp
              isTablet -> 280.dp
              else -> 320.dp
            }
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight),
            ) {
              val backdropUrl = currentMeta?.backdropUrl ?: currentMeta?.posterUrl
              if (!backdropUrl.isNullOrBlank()) {
                RemoteImage(
                  url = backdropUrl,
                  contentDescription = currentMeta?.title ?: currentVideo.displayName,
                  contentScale = if (isLandscape && currentMeta?.backdropUrl.isNullOrBlank()) {
                    ContentScale.Fit
                  } else {
                    ContentScale.Crop
                  },
                  maxDimensionPx = 2048,
                  modifier = Modifier.fillMaxSize(),
                )
              } else {
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.RoundedFilled.Movie,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                  )
                }
              }

              // Natural edge blend at the bottom edge only: top ~65% stays vibrant
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(
                    Brush.verticalGradient(
                      0.0f to Color.Transparent,
                      0.65f to Color.Transparent,
                      0.88f to MaterialTheme.colorScheme.background.copy(alpha = 0.70f),
                      1.0f to MaterialTheme.colorScheme.background,
                    ),
                  ),
              )
            }

            // Movie Details Content
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              // Metadata Badges Row: [ Year ] [ Duration ] [ Resolution ] [ Movie toggle ]
              val isClassifiedAsMovie = remember(currentVideo, currentMeta) {
                MovieClassifier.isActualMovie(currentVideo, currentMeta)
              }
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                val year = currentMeta?.releaseYear?.takeIf { it.isNotBlank() }
                if (year != null) {
                  MovieMetaBadge(text = year)
                }

                val duration = currentMeta?.durationFormatted?.takeIf { it.isNotBlank() }
                  ?: currentVideo.durationFormatted.takeIf { it.isNotBlank() }
                if (duration != null) {
                  MovieMetaBadge(text = duration)
                }

                val resolution = currentMeta?.resolution?.takeIf { it.isNotBlank() }
                if (resolution != null) {
                  MovieMetaBadge(text = resolution)
                }

                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = if (isClassifiedAsMovie) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                  } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f)
                  },
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                      val nextState = !isClassifiedAsMovie
                      coroutineScope.launch {
                        movieMetadataRepo.setMovieOverride(currentVideo, nextState)
                        metadata = movieMetadataRepo.getCachedMetadata(currentVideo)
                        val msg = if (nextState) {
                          context.getString(R.string.movie_details_marked_toast)
                        } else {
                          context.getString(R.string.movie_details_unmarked_toast)
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                      }
                    },
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Icon(
                      imageVector = Icons.RoundedFilled.Movie,
                      contentDescription = null,
                      modifier = Modifier.size(14.dp),
                      tint = if (isClassifiedAsMovie) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                      } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                      },
                    )
                    Text(
                      text = stringResource(
                        if (isClassifiedAsMovie) R.string.movie_details_in_movies
                        else R.string.movie_details_mark_as_movie,
                      ),
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = if (isClassifiedAsMovie) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                      } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                      },
                    )
                  }
                }
              }

              var mediaInfoBadges by remember(currentVideo.path) {
                mutableStateOf(app.video.player.ui.player.videoToFormatBadges(currentVideo))
              }
              LaunchedEffect(currentVideo.path) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                  app.video.player.utils.media.MediaInfoOps
                    .getMediaInfoReport(context, currentVideo.uri, currentVideo.displayName)
                    .onSuccess { report ->
                      val overview = report.data.overviewBadges(currentVideo.displayName)
                      if (overview.isNotEmpty()) {
                        mediaInfoBadges = overview
                      }
                    }
                }
              }
              if (mediaInfoBadges.isNotEmpty()) {
                app.video.player.ui.player.controls.components.MediaFormatBadges(
                  badges = mediaInfoBadges,
                  logoHeight = 24.dp,
                  foreground = Color.White,
                  background = MaterialTheme.colorScheme.surfaceContainerHigh,
                  modifier = Modifier.padding(top = 8.dp),
                )
              }

              // Movie Title
              Text(
                text = currentMeta?.title?.takeIf { it.isNotBlank() } ?: currentVideo.displayName,
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontWeight = FontWeight.Black,
                  letterSpacing = (-0.5).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
              )

              // Synopsis / Overview
              val synopsisText = currentMeta?.synopsis?.takeIf { it.isNotBlank() }
                ?: if (isRefreshing) stringResource(R.string.movie_details_fetching)
                else stringResource(R.string.movie_details_no_synopsis)

              Text(
                text = synopsisText,
                style = MaterialTheme.typography.bodyMedium.copy(
                  lineHeight = 22.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              Spacer(modifier = Modifier.height(12.dp))

              // Full-width Prominent Play Movie Button
              Button(
                onClick = {
                  MediaUtils.playFile(
                    source = currentVideo,
                    context = context,
                    launchSource = "movie_details",
                  )
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(52.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.RoundedFilled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                  )
                  val firstEp = seriesEpisodes.firstOrNull()
                  val playLabel = if (firstEp != null) {
                    val (epVid, epInf) = firstEp
                    "Play S%02dE%02d: %s".format(epInf.season, epInf.episode, epVid.displayName.substringBeforeLast('.'))
                  } else {
                    "Play"
                  }
                  Text(
                    text = playLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                  )
                }
              }

              if (lastPositionSec > 0 && currentVideo.duration > 0L) {
                val watchedFraction = (lastPositionSec * 1000f / currentVideo.duration.toFloat()).coerceIn(0f, 1f)
                if (watchedFraction > 0.01f) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(4.dp)
                      .clip(RoundedCornerShape(2.dp))
                      .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                  ) {
                    Box(
                      modifier = Modifier
                        .fillMaxWidth(watchedFraction)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.primary),
                    )
                  }
                }
              }

              if (seriesEpisodes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "Episodes",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  seriesEpisodes.forEach { (epVideo, epInfo) ->
                    val epProgress = episodeProgressMap[epVideo.path] ?: 0f
                    val epIsWatched = epProgress >= 1f
                    Surface(
                      onClick = {
                        MediaUtils.playFile(
                          source = epVideo,
                          context = context,
                          launchSource = "movie_details_episode",
                        )
                      },
                      shape = RoundedCornerShape(16.dp),
                      color = MaterialTheme.colorScheme.surfaceContainerLow,
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                          // 16:9 Thumbnail with centered Play icon
                          Box(
                            modifier = Modifier
                              .width(96.dp)
                              .height(54.dp)
                              .clip(RoundedCornerShape(10.dp))
                              .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                          ) {
                            Box(
                              modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f)),
                              contentAlignment = Alignment.Center,
                            ) {
                              Icon(
                                imageVector = Icons.RoundedFilled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                              )
                            }
                          }

                          // Episode code, title, duration & size
                          Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                          ) {
                            Row(
                              verticalAlignment = Alignment.CenterVertically,
                              horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                              Text(
                                text = "S%02dE%02d".format(epInfo.season, epInfo.episode),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                              )
                              Text(
                                text = epVideo.displayName.substringBeforeLast('.'),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                              )
                            }
                            val metaLine = buildList {
                              epVideo.durationFormatted.takeIf { it.isNotBlank() && it != "0s" }?.let { add(it) }
                              epVideo.sizeFormatted.takeIf { it.isNotBlank() }?.let { add(it) }
                            }.joinToString(" • ")
                            if (metaLine.isNotEmpty()) {
                              Text(
                                text = metaLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                              )
                            }
                          }

                          if (epIsWatched) {
                            Icon(
                              imageVector = Icons.RoundedFilled.CheckCircle,
                              contentDescription = null,
                              tint = Color(0xFF4CAF50),
                              modifier = Modifier.size(18.dp),
                            )
                          }
                          IconButton(
                            onClick = {
                              coroutineScope.launch {
                                PermissionUtils.StorageOps.deleteVideos(context, listOf(epVideo))
                                seriesEpisodes = seriesEpisodes.filterNot { it.first.path == epVideo.path }
                              }
                            },
                            modifier = Modifier.size(36.dp),
                          ) {
                            Icon(
                              imageVector = Icons.RoundedFilled.Delete,
                              contentDescription = stringResource(R.string.delete),
                              tint = MaterialTheme.colorScheme.onSurfaceVariant,
                              modifier = Modifier.size(20.dp),
                            )
                          }
                        }
                        if (epProgress > 0f) {
                          androidx.compose.material3.LinearProgressIndicator(
                            progress = { epProgress },
                            modifier = Modifier
                              .fillMaxWidth()
                              .height(3.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          // Top Action Bar Overlay
          val topInset = WindowInsets.systemBars.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = topInset)
              .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              onClick = { backStack.popSafely() },
              modifier = Modifier
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.35f), CircleShape),
            ) {
              Icon(
                imageVector = Icons.RoundedFilled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
              )
            }

            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              // Refresh / Re-scrape TMDB metadata
              IconButton(
                onClick = {
                  coroutineScope.launch {
                    isRefreshing = true
                    val fresh = movieMetadataRepo.getOrFetchMetadata(currentVideo, forceRefresh = true)
                    metadata = fresh
                    isRefreshing = false
                    Toast.makeText(context, context.getString(R.string.movie_details_refresh), Toast.LENGTH_SHORT).show()
                  }
                },
                modifier = Modifier
                  .size(40.dp)
                  .background(Color.Black.copy(alpha = 0.35f), CircleShape),
              ) {
                Icon(
                  imageVector = Icons.RoundedFilled.Refresh,
                  contentDescription = stringResource(R.string.movie_details_refresh),
                  tint = Color.White,
                  modifier = if (isRefreshing) Modifier.rotate(spinAngle) else Modifier,
                )
              }

              // Edit metadata
              IconButton(
                onClick = { showEditDialog = true },
                modifier = Modifier
                  .size(40.dp)
                  .background(Color.Black.copy(alpha = 0.35f), CircleShape),
              ) {
                Icon(
                  imageVector = Icons.RoundedFilled.Edit,
                  contentDescription = stringResource(R.string.movie_details_edit),
                  tint = Color.White,
                )
              }

              // Watched / Unwatched toggle
              IconButton(
                onClick = {
                  val newWatched = !isWatched
                  coroutineScope.launch {
                    PlaybackStateOps.setWatched(currentVideo, newWatched)
                    isWatched = newWatched
                    val msg = if (newWatched) context.getString(R.string.movie_details_mark_watched)
                              else context.getString(R.string.movie_details_mark_unwatched)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                  }
                },
                modifier = Modifier
                  .size(40.dp)
                  .background(Color.Black.copy(alpha = 0.35f), CircleShape),
              ) {
                Icon(
                  imageVector = Icons.RoundedFilled.CheckCircle,
                  contentDescription = if (isWatched) stringResource(R.string.movie_details_mark_unwatched) else stringResource(R.string.movie_details_mark_watched),
                  tint = if (isWatched) MaterialTheme.colorScheme.primary else Color.White,
                )
              }

              // Delete button
              IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                  .size(40.dp)
                  .background(Color.Black.copy(alpha = 0.35f), CircleShape),
              ) {
                Icon(
                  imageVector = Icons.RoundedFilled.Delete,
                  contentDescription = stringResource(R.string.movie_details_delete),
                  tint = Color.White,
                )
              }
            }
          }
        }
      }
    }

    // Edit Movie Details Dialog
    if (showEditDialog && currentVideo != null) {
      EditMovieDialog(
        initialTitle = currentMeta?.title ?: currentVideo.displayName,
        initialSynopsis = currentMeta?.synopsis ?: "",
        onDismiss = { showEditDialog = false },
        onSave = { newTitle, newSynopsis ->
          showEditDialog = false
          val updated = (currentMeta ?: MovieMetadata(title = newTitle)).copy(
            title = newTitle,
            synopsis = newSynopsis,
          )
          metadata = updated
          coroutineScope.launch {
            movieMetadataRepo.saveCustomMetadata(currentVideo, updated)
          }
        },
      )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && currentVideo != null) {
      DeleteConfirmationDialog(
        isOpen = showDeleteDialog,
        itemCount = 1,
        itemType = "movie",
        itemPaths = listOf(currentVideo.uri.toString()),
        totalSizeBytes = currentVideo.size.takeIf { it > 0L },
        onDismiss = { showDeleteDialog = false },
        onConfirm = {
          showDeleteDialog = false
          coroutineScope.launch(Dispatchers.IO) {
            PermissionUtils.StorageOps.deleteVideos(context, listOf(currentVideo))
            withContext(Dispatchers.Main) {
              backStack.popSafely()
            }
          }
        },
      )
    }
  }
}

@Composable
private fun MovieMetaBadge(
  text: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    border = androidx.compose.foundation.BorderStroke(
      0.8.dp,
      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
    ),
    modifier = modifier,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
      ),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
    )
  }
}

@Composable
private fun EditMovieDialog(
  initialTitle: String,
  initialSynopsis: String,
  onDismiss: () -> Unit,
  onSave: (title: String, synopsis: String) -> Unit,
) {
  var title by remember { mutableStateOf(initialTitle) }
  var synopsis by remember { mutableStateOf(initialSynopsis) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Text(
          text = stringResource(R.string.movie_details_edit_title),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
        )

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = synopsis,
          onValueChange = { synopsis = it },
          label = { Text(stringResource(R.string.movie_details_edit_synopsis)) },
          minLines = 3,
          maxLines = 6,
          modifier = Modifier.fillMaxWidth(),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(onClick = onDismiss) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = { onSave(title.trim(), synopsis.trim()) },
            enabled = title.isNotBlank(),
          ) {
            Text(stringResource(R.string.movie_details_save))
          }
        }
      }
    }
  }
}

