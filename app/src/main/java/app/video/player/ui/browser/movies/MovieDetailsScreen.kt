/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.browser.movies

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.video.player.R
import app.video.player.database.entities.PlaybackStateEntity
import app.video.player.domain.media.model.Video
import app.video.player.domain.movies.MovieClassifier
import app.video.player.domain.movies.MovieMetadata
import app.video.player.domain.movies.MovieMetadataRepository
import app.video.player.domain.playbackstate.repository.PlaybackStateRepository
import app.video.player.domain.thumbnail.ThumbnailRepository
import app.video.player.preferences.BrowserPreferences
import app.video.player.presentation.Screen
import app.video.player.presentation.components.RemoteImage
import app.video.player.repository.MediaFileRepository
import app.video.player.ui.browser.dialogs.DeleteConfirmationDialog
import app.video.player.ui.browser.videolist.videoPlaybackIdentifiers
import app.video.player.ui.components.AppToast as Toast
import app.video.player.ui.icons.Icon
import app.video.player.ui.icons.Icons
import app.video.player.ui.mediainfo.overviewBadges
import app.video.player.ui.player.controls.components.MediaFormatBadges
import app.video.player.ui.player.videoToFormatBadges
import app.video.player.ui.utils.LocalBackStack
import app.video.player.ui.utils.NavigationBackHandler as BackHandler
import app.video.player.ui.utils.popSafely
import app.video.player.utils.history.RecentlyPlayedOps
import app.video.player.utils.media.MediaInfoOps
import app.video.player.utils.media.MediaUtils
import app.video.player.utils.media.PlaybackStateEvents
import app.video.player.utils.media.PlaybackStateOps
import app.video.player.utils.permission.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import java.io.File

private data class EpisodePlaybackStatus(
  val progress: Float,
  val isWatched: Boolean,
  val lastPlayedTimestamp: Long,
)

/**
 * Rich Movie / Series Details Screen
 * Displays cinematic backdrop, online metadata badges, expandable synopsis from TMDB/Cinemeta,
 * top action bar (Refresh, Edit, Watched Toggle, Delete), prominent active-episode Play button
 * (green when all episodes are watched), season selector tabs, and episode cards with stills & overviews.
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
    val thumbnailRepository = koinInject<ThumbnailRepository>()
    val playbackStateRepo = koinInject<PlaybackStateRepository>()

    var video by remember { mutableStateOf<Video?>(null) }
    var metadata by remember { mutableStateOf<MovieMetadata?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isWatched by remember { mutableStateOf(false) }
    var seriesEpisodes by remember {
      mutableStateOf<List<Pair<Video, MovieClassifier.SeriesEpisodeInfo>>>(emptyList())
    }
    var episodeStatusMap by remember { mutableStateOf<Map<String, EpisodePlaybackStatus>>(emptyMap()) }
    var selectedSeason by remember { mutableIntStateOf(1) }
    var seasonInitialized by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeRefreshTick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
      var resumedOnce = false
      val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
          if (resumedOnce) {
            resumeRefreshTick++
          } else {
            resumedOnce = true
          }
        }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    suspend fun computePlaybackSnapshot(
      resolved: Video,
      episodes: List<Pair<Video, MovieClassifier.SeriesEpisodeInfo>>,
    ) {
      withContext(Dispatchers.IO) {
        val threshold = browserPreferences.watchedThreshold.get()
        val statesByTitle = playbackStateRepo.getAllPlaybackStates().associateBy { it.mediaTitle }
        val recentByKey = buildMap {
          RecentlyPlayedOps.getRecentlyPlayed(limit = 150).forEach { entity ->
            put(entity.filePath, entity.timestamp)
            putIfAbsent(entity.fileName, entity.timestamp)
          }
        }

        if (episodes.isNotEmpty()) {
          val newMap = episodes.associate { (epVideo, _) ->
            val state = findPlaybackStateForVideo(epVideo, statesByTitle)
            val status = computeEpisodeStatus(epVideo, state, threshold, recentByKey)
            epVideo.path to status
          }
          val allDone = newMap.values.all { it.isWatched }
          withContext(Dispatchers.Main) {
            episodeStatusMap = newMap
            isWatched = allDone
          }
        } else {
          val state = findPlaybackStateForVideo(resolved, statesByTitle)
          val status = computeEpisodeStatus(resolved, state, threshold, recentByKey)
          withContext(Dispatchers.Main) {
            isWatched = status.isWatched
          }
        }
      }
    }

    // Initial load: video, series sibling scan, cached metadata + network enrichment, and playback progress
    LaunchedEffect(videoPath) {
      withContext(Dispatchers.IO) {
        val resolved = MediaFileRepository.getVideosFromFiles(context, listOf(File(videoPath))).firstOrNull()
        withContext(Dispatchers.Main) {
          video = resolved
        }
        if (resolved == null) return@withContext

        val cached = movieMetadataRepo.getCachedMetadata(resolved)
        if (cached != null) {
          withContext(Dispatchers.Main) {
            metadata = cached
          }
        }

        coroutineScope {
          val seriesJob = async {
            val seriesInfo = MovieClassifier.extractSeriesEpisodeInfo(resolved)
            if (seriesInfo != null) {
              val parentDir = File(resolved.path).parentFile
              val siblingFiles = parentDir?.listFiles()?.filter { it.isFile } ?: emptyList()
              val siblingVideos = MediaFileRepository.getVideosFromFiles(context, siblingFiles)
              val matched = siblingVideos.mapNotNull { v ->
                val info = MovieClassifier.extractSeriesEpisodeInfo(v)
                if (info != null && info.seriesKey == seriesInfo.seriesKey) v to info else null
              }.sortedWith(compareBy({ it.second.season }, { it.second.episode }, { it.first.displayName }))
              if (matched.size > 1) matched else emptyList()
            } else {
              emptyList()
            }
          }

          val resolvedEpisodes = seriesJob.await()
          val seasonsPresent = resolvedEpisodes.map { it.second.season }.distinct().sorted()

          val metaJob = async {
            if (cached == null) {
              withContext(Dispatchers.Main) { isRefreshing = true }
            }
            val fetched = movieMetadataRepo.getOrFetchMetadata(
              video = resolved,
              forceRefresh = false,
              seasonsToEnrich = seasonsPresent,
            )
            withContext(Dispatchers.Main) {
              metadata = fetched
              isRefreshing = false
            }
          }

          val playbackJob = async {
            computePlaybackSnapshot(resolved, resolvedEpisodes)
          }

          withContext(Dispatchers.Main) {
            seriesEpisodes = resolvedEpisodes
            if (!seasonInitialized && seasonsPresent.isNotEmpty()) {
              selectedSeason = seasonsPresent.first()
              seasonInitialized = true
            }
          }

          metaJob.await()
          playbackJob.await()
        }
      }
    }

    // Refresh progress immediately when PlayerActivity notifies PlaybackStateEvents.changes
    LaunchedEffect(Unit) {
      PlaybackStateEvents.changes.collect {
        val resolved = video ?: return@collect
        computePlaybackSnapshot(resolved, seriesEpisodes)
      }
    }

    // Backup delayed refresh on ON_RESUME in case PlayerActivity onStop save finishes slightly after resume
    LaunchedEffect(resumeRefreshTick) {
      if (resumeRefreshTick == 0) return@LaunchedEffect
      val resolved = video ?: return@LaunchedEffect
      computePlaybackSnapshot(resolved, seriesEpisodes)
      delay(350)
      computePlaybackSnapshot(resolved, seriesEpisodes)
      delay(750)
      computePlaybackSnapshot(resolved, seriesEpisodes)
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
          Box(modifier = Modifier.fillMaxSize())
        } else {
          val scrollState = rememberScrollState()
          val episodesBySeason = remember(seriesEpisodes) {
            seriesEpisodes.groupBy { it.second.season }.toSortedMap()
          }

          // Compute mpvium-style active episode (most recently played unwatched, else next unwatched)
          val activeEpisode = remember(seriesEpisodes, episodeStatusMap) {
            pickActiveEpisode(seriesEpisodes, episodeStatusMap)
          }
          val allEpisodesWatched = remember(seriesEpisodes, episodeStatusMap) {
            seriesEpisodes.isNotEmpty() &&
              seriesEpisodes.all { (ep, _) -> episodeStatusMap[ep.path]?.isWatched == true }
          }

          // Switch to active episode's season on first load
          LaunchedEffect(activeEpisode, episodesBySeason.keys) {
            if (activeEpisode != null && episodesBySeason.containsKey(activeEpisode.second.season) && !seasonInitialized) {
              selectedSeason = activeEpisode.second.season
              seasonInitialized = true
            } else if (episodesBySeason.isNotEmpty() && !episodesBySeason.containsKey(selectedSeason)) {
              selectedSeason = episodesBySeason.keys.first()
            }
          }

          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(scrollState),
          ) {
            // Backdrop Header (mpvium-style responsive height & natural bottom-edge blend)
            val configuration = LocalConfiguration.current
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
                .height(headerHeight)
                .background(MaterialTheme.colorScheme.surfaceContainer),
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

            // Movie / Series Details Content
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              // Top Metadata Chips (mpvium-style: Season/Episode counts + Year for series; Year/Duration/Res for movie)
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
                if (seriesEpisodes.isNotEmpty()) {
                  val seasonCount = episodesBySeason.size
                  val episodeCount = seriesEpisodes.size
                  MovieMetaBadge(
                    text = "$seasonCount Seasons • $episodeCount Episodes",
                  )
                  val year = currentMeta?.releaseYear?.takeIf { it.isNotBlank() }
                  if (year != null) {
                    MovieMetaBadge(text = year)
                  }
                } else {
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
                }

                // In Movies / Mark as Movie toggle chip
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
                mutableStateOf(videoToFormatBadges(currentVideo))
              }
              LaunchedEffect(currentVideo.path) {
                withContext(Dispatchers.IO) {
                  MediaInfoOps
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
                MediaFormatBadges(
                  badges = mediaInfoBadges,
                  logoHeight = 24.dp,
                  foreground = Color.White,
                  background = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
              }

              // Title
              Text(
                text = currentMeta?.title?.takeIf { it.isNotBlank() } ?: currentVideo.displayName,
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontWeight = FontWeight.Black,
                  letterSpacing = (-0.5).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
              )

              // Expandable Synopsis / Overview
              val synopsisText = currentMeta?.synopsis?.takeIf { it.isNotBlank() }
                ?: if (isRefreshing) stringResource(R.string.movie_details_fetching)
                else stringResource(R.string.movie_details_no_synopsis)

              var isSynopsisExpanded by remember { mutableStateOf(false) }
              Column {
                Text(
                  text = synopsisText,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp,
                  ),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = if (isSynopsisExpanded) Int.MAX_VALUE else 4,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.animateContentSize(),
                )
                if (synopsisText.length > 180) {
                  Text(
                    text = if (isSynopsisExpanded) "Show less" else "Read more",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                      .clickable { isSynopsisExpanded = !isSynopsisExpanded }
                      .padding(top = 4.dp),
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              // Prominent Full-Width Play Button (green when all episodes watched)
              Button(
                onClick = {
                  MediaUtils.playFile(
                    source = activeEpisode?.first ?: currentVideo,
                    context = context,
                    launchSource = "movie_details",
                  )
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (allEpisodesWatched) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                  contentColor = if (allEpisodesWatched) Color.White else MaterialTheme.colorScheme.onPrimary,
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
                  val playLabel = if (activeEpisode != null) {
                    val (epVid, epInf) = activeEpisode
                    val epKey = MovieMetadataRepository.episodeKey(epInf.season, epInf.episode)
                    val epName = currentMeta?.episodeTitles?.get(epKey)?.takeIf { it.isNotBlank() }
                      ?: epVid.displayName.substringBeforeLast('.')
                    "Play S%02dE%02d: %s".format(epInf.season, epInf.episode, epName)
                  } else {
                    "Play"
                  }
                  Text(
                    text = playLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }

              // Episodes Section (mpvium SeriesDetailScreen style)
              if (seriesEpisodes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                if (episodesBySeason.size > 1) {
                  Text(
                    text = "Seasons",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    items(episodesBySeason.keys.toList()) { seasonNum ->
                      val selected = seasonNum == selectedSeason
                      FilterChip(
                        selected = selected,
                        onClick = { selectedSeason = seasonNum },
                        label = { Text("Season $seasonNum") },
                        colors = FilterChipDefaults.filterChipColors(
                          selectedContainerColor = MaterialTheme.colorScheme.primary,
                          selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        shape = RoundedCornerShape(12.dp),
                      )
                    }
                  }
                } else {
                  Text(
                    text = "Episodes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }

                AnimatedContent(
                  targetState = selectedSeason,
                  transitionSpec = {
                    (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 16 }) togetherWith
                      fadeOut(tween(140))
                  },
                  label = "season_episodes_transition",
                ) { activeSeason ->
                  val seasonEpisodes = episodesBySeason[activeSeason].orEmpty()
                  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    seasonEpisodes.forEach { (epVideo, epInfo) ->
                      val status = episodeStatusMap[epVideo.path]
                      val epProgress = status?.progress ?: 0f
                      val epIsWatched = status?.isWatched == true
                      val epKey = MovieMetadataRepository.episodeKey(epInfo.season, epInfo.episode)
                      val epTitle = currentMeta?.episodeTitles?.get(epKey)?.takeIf { it.isNotBlank() }
                        ?: epVideo.displayName.substringBeforeLast('.')
                      val epOverview = currentMeta?.episodeOverviews?.get(epKey)?.takeIf { it.isNotBlank() }
                      val epStillUrl = currentMeta?.episodeStills?.get(epKey)

                      Card(
                        onClick = {
                          MediaUtils.playFile(
                            source = epVideo,
                            context = context,
                            launchSource = "movie_details_episode",
                          )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                          containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                          Row(
                            modifier = Modifier
                              .fillMaxWidth()
                              .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                          ) {
                            EpisodeThumbnail(
                              epVideo = epVideo,
                              epStillUrl = epStillUrl,
                              epProgress = epProgress,
                              epIsWatched = epIsWatched,
                              thumbnailRepository = thumbnailRepository,
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                              modifier = Modifier.weight(1f),
                              verticalArrangement = Arrangement.Center,
                            ) {
                              Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                              ) {
                                Text(
                                  text = "S%02dE%02d".format(epInfo.season, epInfo.episode),
                                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                  color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                  text = epTitle,
                                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                  color = MaterialTheme.colorScheme.onSurface,
                                  maxLines = 1,
                                  overflow = TextOverflow.Ellipsis,
                                )
                              }

                              Spacer(modifier = Modifier.height(2.dp))

                              val metaLine = buildList {
                                epVideo.durationFormatted.takeIf { it.isNotBlank() && it != "0s" }?.let { add(it) }
                                epVideo.sizeFormatted.takeIf { it.isNotBlank() }?.let { add(it) }
                              }.joinToString(" • ")
                              if (metaLine.isNotEmpty()) {
                                Text(
                                  text = metaLine,
                                  style = MaterialTheme.typography.labelSmall,
                                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                                  maxLines = 1,
                                )
                              }

                              if (epOverview != null) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                  text = epOverview,
                                  style = MaterialTheme.typography.bodySmall,
                                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                  maxLines = 3,
                                  overflow = TextOverflow.Ellipsis,
                                )
                              }
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp),
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
              // Refresh / Re-scrape TMDB + Cinemeta metadata including all seasons
              IconButton(
                onClick = {
                  coroutineScope.launch {
                    isRefreshing = true
                    val seasonsPresent = seriesEpisodes.map { it.second.season }.distinct()
                    val fresh = movieMetadataRepo.getOrFetchMetadata(
                      video = currentVideo,
                      forceRefresh = true,
                      seasonsToEnrich = seasonsPresent,
                    )
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

              // Watched / Unwatched toggle (marks all episodes if series)
              IconButton(
                onClick = {
                  val newWatched = !isWatched
                  val targets = if (seriesEpisodes.isNotEmpty()) seriesEpisodes.map { it.first } else listOf(currentVideo)
                  coroutineScope.launch {
                    PlaybackStateOps.setWatched(targets, newWatched)
                    computePlaybackSnapshot(currentVideo, seriesEpisodes)
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
                  contentDescription = if (isWatched) {
                    stringResource(R.string.movie_details_mark_unwatched)
                  } else {
                    stringResource(R.string.movie_details_mark_watched)
                  },
                  tint = if (isWatched) Color(0xFF4CAF50) else Color.White,
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
      val deleteTargets = if (seriesEpisodes.isNotEmpty()) seriesEpisodes.map { it.first } else listOf(currentVideo)
      DeleteConfirmationDialog(
        isOpen = showDeleteDialog,
        itemCount = deleteTargets.size,
        itemType = if (seriesEpisodes.isNotEmpty()) "show" else "movie",
        itemPaths = deleteTargets.map { it.uri.toString() },
        totalSizeBytes = deleteTargets.sumOf { it.size.coerceAtLeast(0L) }.takeIf { it > 0L },
        onDismiss = { showDeleteDialog = false },
        onConfirm = {
          showDeleteDialog = false
          coroutineScope.launch(Dispatchers.IO) {
            PermissionUtils.StorageOps.deleteVideos(context, deleteTargets)
            withContext(Dispatchers.Main) {
              backStack.popSafely()
            }
          }
        },
      )
    }
  }
}

private fun findPlaybackStateForVideo(
  video: Video,
  statesByTitle: Map<String, PlaybackStateEntity>,
): PlaybackStateEntity? {
  for (id in videoPlaybackIdentifiers(video)) {
    statesByTitle[id]?.let { return it }
  }
  return statesByTitle[video.displayName]
    ?: statesByTitle[video.title]
    ?: statesByTitle[File(video.path).name]
    ?: statesByTitle[File(video.path).nameWithoutExtension]
}

private fun computeEpisodeStatus(
  video: Video,
  state: PlaybackStateEntity?,
  watchedThreshold: Int,
  recentByKey: Map<String, Long>,
): EpisodePlaybackStatus {
  val recentTs = recentByKey[video.path]
    ?: recentByKey[video.uri.toString()]
    ?: recentByKey[video.displayName]
    ?: 0L
  if (state == null) {
    return EpisodePlaybackStatus(progress = 0f, isWatched = false, lastPlayedTimestamp = recentTs)
  }

  val recordedDuration = (state.lastPosition.toLong() + state.timeRemaining.toLong()).takeIf { it > 0L }
  val durationSeconds = (video.duration / 1000L).takeIf { it > 0L } ?: recordedDuration ?: 0L
  val watchedSeconds = when {
    durationSeconds > 0L && state.timeRemaining > 0 ->
      (durationSeconds - state.timeRemaining.toLong()).coerceAtLeast(state.lastPosition.toLong())
    else -> state.lastPosition.toLong()
  }
  val rawProgress = if (durationSeconds > 0L && watchedSeconds > 0L) {
    (watchedSeconds.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)
  } else {
    0f
  }
  val isWatched = state.hasBeenWatched ||
    (watchedThreshold > 0 && rawProgress >= watchedThreshold / 100f)

  return EpisodePlaybackStatus(
    progress = if (isWatched) 1f else rawProgress,
    isWatched = isWatched,
    lastPlayedTimestamp = recentTs,
  )
}

/**
 * Matches mpvium's active-episode selection: finds the most recently played episode;
 * if still in progress returns it to resume, otherwise advances to the next unwatched episode.
 */
private fun pickActiveEpisode(
  episodes: List<Pair<Video, MovieClassifier.SeriesEpisodeInfo>>,
  statusMap: Map<String, EpisodePlaybackStatus>,
): Pair<Video, MovieClassifier.SeriesEpisodeInfo>? {
  if (episodes.isEmpty()) return null
  val lastWatched = episodes
    .filter { (ep, _) ->
      val status = statusMap[ep.path]
      status != null && (status.progress > 0f || status.isWatched)
    }
    .maxByOrNull { (ep, _) ->
      statusMap[ep.path]?.lastPlayedTimestamp ?: ep.dateModified
    }

  if (lastWatched != null) {
    val lastStatus = statusMap[lastWatched.first.path]
    if (lastStatus != null && !lastStatus.isWatched) {
      return lastWatched
    }
    val currentIndex = episodes.indexOfFirst { it.first.path == lastWatched.first.path }
    if (currentIndex >= 0) {
      return episodes.drop(currentIndex + 1).firstOrNull { (ep, _) ->
        statusMap[ep.path]?.isWatched != true
      } ?: episodes.firstOrNull { (ep, _) ->
        statusMap[ep.path]?.isWatched != true
      } ?: lastWatched
    }
  }
  return episodes.firstOrNull { (ep, _) -> statusMap[ep.path]?.isWatched != true }
    ?: episodes.firstOrNull()
}

/**
 * Episode thumbnail matching mpvium: loads TMDB/Cinemeta still with local video thumbnail fallback,
 * circular play overlay, and watched badge.
 */
@Composable
private fun EpisodeThumbnail(
  epVideo: Video,
  epStillUrl: String?,
  epProgress: Float,
  epIsWatched: Boolean,
  thumbnailRepository: ThumbnailRepository,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  val thumbWidthPx = remember(density) { with(density) { 120.dp.roundToPx() } }
  val thumbHeightPx = remember(density) { with(density) { 68.dp.roundToPx() } }

  var localThumbnail by remember(epVideo.path) {
    mutableStateOf<Bitmap?>(thumbnailRepository.peekThumbnailFromMemory(epVideo, thumbWidthPx, thumbHeightPx))
  }
  LaunchedEffect(epVideo.path, epStillUrl) {
    if (epStillUrl.isNullOrBlank() && localThumbnail == null) {
      val loaded = withContext(Dispatchers.IO) {
        thumbnailRepository.getThumbnail(epVideo, thumbWidthPx, thumbHeightPx)
      }
      if (loaded != null) {
        localThumbnail = loaded
      }
    }
  }

  Box(
    modifier = modifier
      .width(120.dp)
      .aspectRatio(16f / 9f)
      .clip(RoundedCornerShape(10.dp))
      .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    contentAlignment = Alignment.Center,
  ) {
    if (!epStillUrl.isNullOrBlank()) {
      RemoteImage(
        url = epStillUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        maxDimensionPx = 500,
        modifier = Modifier.fillMaxSize(),
      )
    } else if (localThumbnail != null) {
      Image(
        bitmap = localThumbnail!!.asImageBitmap(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    }

    Box(
      modifier = Modifier
        .size(30.dp)
        .clip(CircleShape)
        .background(Color.Black.copy(alpha = 0.50f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.RoundedFilled.PlayArrow,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(18.dp),
      )
    }

    if (epIsWatched) {
      Surface(
        shape = CircleShape,
        color = Color(0xFF4CAF50),
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(4.dp)
          .size(18.dp),
      ) {
        Icon(
          imageVector = Icons.RoundedFilled.CheckCircle,
          contentDescription = "Watched",
          tint = Color.White,
          modifier = Modifier.padding(2.dp),
        )
      }
    }

    if (epProgress > 0f && !epIsWatched) {
      LinearProgressIndicator(
        progress = { epProgress },
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .height(3.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = Color.Black.copy(alpha = 0.45f),
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
    border = BorderStroke(
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
