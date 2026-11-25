/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.browser.movies

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.video.player.R
import app.video.player.domain.media.model.Video
import app.video.player.domain.movies.MovieClassifier
import app.video.player.domain.movies.MovieMetadata
import app.video.player.domain.movies.MovieMetadataRepository
import app.video.player.presentation.components.RemoteImage
import app.video.player.ui.browser.cards.SelectionIndicator
import app.video.player.ui.browser.cards.animatedSelectionColor
import app.video.player.ui.browser.selection.SelectionManager
import app.video.player.ui.browser.states.EmptyState
import app.video.player.ui.browser.videolist.VideoWithPlaybackInfo
import app.video.player.ui.icons.Icon
import app.video.player.ui.icons.Icons
import app.video.player.utils.media.MediaInfoParser
import org.koin.compose.koinInject

/**
 * Dedicated Movies Library View
 * Displays filter chips (All, Unwatched, Started), total titles count,
 * and a poster grid showcasing movie art, duration tags, titles, and release years.
 */
@Composable
fun MoviesTabContent(
  videosWithInfo: List<VideoWithPlaybackInfo>,
  onOpenMovieDetails: (Video) -> Unit,
  modifier: Modifier = Modifier,
  selectionManager: SelectionManager<Video, Long>? = null,
) {
  var selectedFilterIndex by rememberSaveable { mutableIntStateOf(0) }
  val movieMetadataRepo = koinInject<MovieMetadataRepository>()
  val metadataRevision by movieMetadataRepo.metadataRevision.collectAsState()

  val movieCandidates = remember(videosWithInfo, metadataRevision) {
    videosWithInfo.filter {
      MovieClassifier.isActualMovie(it.video, movieMetadataRepo.getMemoryCachedMetadata(it.video))
    }
  }

  val filteredMovies = remember(movieCandidates, selectedFilterIndex) {
    when (selectedFilterIndex) {
      1 -> movieCandidates.filter {
        (it.progressPercentage == null || it.progressPercentage == 0f) && !it.isWatched
      }
      2 -> movieCandidates.filter {
        (it.progressPercentage ?: 0f) > 0f && !it.isWatched
      }
      else -> movieCandidates
    }
  }

  Column(
    modifier = modifier.fillMaxSize(),
  ) {
    // Filter Chips Row: [ All ] [ Unwatched ] [ Started ]
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FilterChip(
        selected = selectedFilterIndex == 0,
        onClick = { selectedFilterIndex = 0 },
        label = { Text(stringResource(R.string.ui_filter_all)) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
          selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
      )

      FilterChip(
        selected = selectedFilterIndex == 1,
        onClick = { selectedFilterIndex = 1 },
        label = { Text(stringResource(R.string.ui_filter_unwatched)) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
          selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
      )

      FilterChip(
        selected = selectedFilterIndex == 2,
        onClick = { selectedFilterIndex = 2 },
        label = { Text(stringResource(R.string.ui_filter_started)) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
          selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
      )
    }

    // Subtitle: "X titles"
    Text(
      text = "${filteredMovies.size} titles",
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
      ),
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
      modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
    )

    // Movie Posters Grid (Portrait 2:3 aspect ratio) or Empty State
    if (filteredMovies.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        EmptyState(
          icon = Icons.RoundedFilled.Movie,
          title = stringResource(R.string.movies_empty_title),
          message = stringResource(R.string.movies_empty_message),
        )
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 105.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
      ) {
        items(
          items = filteredMovies,
          key = { it.video.path },
        ) { item ->
          val isSelected = selectionManager?.isSelected(item.video) == true
          MoviePosterCard(
            item = item,
            movieMetadataRepo = movieMetadataRepo,
            isSelected = isSelected,
            onClick = {
              if (selectionManager?.isInSelectionMode == true) {
                selectionManager.toggleFromUser(item.video)
              } else {
                onOpenMovieDetails(item.video)
              }
            },
            onLongClick = { selectionManager?.handleLongClick(item.video) },
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MoviePosterCard(
  item: VideoWithPlaybackInfo,
  movieMetadataRepo: MovieMetadataRepository,
  isSelected: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val video = item.video
  var metadata by remember(video.path) {
    mutableStateOf(movieMetadataRepo.getMemoryCachedMetadata(video))
  }

  LaunchedEffect(video.path) {
    if (metadata == null) {
      metadata = movieMetadataRepo.getOrFetchMetadata(video)
    }
  }

  val parsedInfo = remember(video.displayName, video.title) {
    MediaInfoParser.parse(video.displayName.ifBlank { video.title })
  }
  val episodeBadge = remember(parsedInfo) {
    when {
      parsedInfo.season != null && parsedInfo.episode != null ->
        "S${parsedInfo.season.toString().padStart(2, '0')}E${parsedInfo.episode.toString().padStart(2, '0')}"
      parsedInfo.episode != null ->
        "EP ${parsedInfo.episode.toString().padStart(2, '0')}"
      else -> null
    }
  }

  val displayTitle = metadata?.title?.takeIf { it.isNotBlank() } ?: video.displayName
  val displayYear = metadata?.releaseYear ?: ""
  val displayDuration = metadata?.durationFormatted?.takeIf { it.isNotBlank() } ?: video.durationFormatted
  val selectionColor = animatedSelectionColor(isSelected)

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
      ),
    shape = RoundedCornerShape(12.dp),
    color = selectionColor,
    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(if (isSelected) 4.dp else 0.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      // Poster Artwork Box with 2:3 Aspect Ratio
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(2f / 3f)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHighest),
      ) {
        val posterUrl = metadata?.posterUrl ?: metadata?.backdropUrl
        if (!posterUrl.isNullOrBlank()) {
          RemoteImage(
            url = posterUrl,
            contentDescription = displayTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.RoundedFilled.Movie,
              contentDescription = null,
              modifier = Modifier.size(36.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
          }
        }

        // Episode / Series badge (top-start)
        if (episodeBadge != null) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(6.dp),
          ) {
            Text(
              text = episodeBadge,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
              ),
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            )
          }
        }

        // Selection check indicator (top-end)
        if (isSelected) {
          SelectionIndicator(
            selected = true,
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(6.dp),
          )
        }

        // Duration Tag at the bottom of the poster
        if (displayDuration.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color.Black.copy(alpha = 0.75f),
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(6.dp),
          ) {
            Text(
              text = displayDuration,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
              ),
              color = Color.White,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            )
          }
        }
      }

      // Title below poster
      Text(
        text = displayTitle,
        style = MaterialTheme.typography.bodySmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 2.dp),
      )

      // Year below title
      if (displayYear.isNotBlank()) {
        Text(
          text = displayYear,
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp,
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 2.dp),
        )
      }
    }
  }
}

