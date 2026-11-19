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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.video.player.domain.media.model.Video
import app.video.player.preferences.BrowserPreferences
import app.video.player.preferences.MediaLibraryType
import app.video.player.domain.thumbnail.ThumbnailRepository
import app.video.player.ui.browser.videolist.VideoWithPlaybackInfo
import app.video.player.ui.icons.Icons
import app.video.player.ui.icons.Icon
import app.video.player.utils.storage.FileTypeUtils
import org.koin.compose.koinInject
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Bento Fusion Hub Header
 * Integrates a liquid-glass Hero Spotlight Card (with dynamic audio visualizer / video resume)
 * and dual Bento quick-action tiles for Cinema and Hi-Res Audio.
 */
@Composable
fun BentoFusionHeader(
  videosWithInfo: List<VideoWithPlaybackInfo>,
  recentlyPlayedFilePath: String?,
  onPlayMedia: (Video) -> Unit,
  onFilterSelected: ((MediaLibraryType) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  if (videosWithInfo.isEmpty()) return

  // 1. Identify hero media: prioritize recently played file, then in-progress, then first available
  val heroMedia = remember(videosWithInfo, recentlyPlayedFilePath) {
    if (recentlyPlayedFilePath != null) {
      videosWithInfo.firstOrNull { it.video.path == recentlyPlayedFilePath }
    } else {
      null
    } ?: videosWithInfo.firstOrNull { it.progressPercentage != null && it.progressPercentage > 0f && !it.isWatched }
      ?: videosWithInfo.firstOrNull()
  } ?: return

  // 2. Identify latest video and latest audio candidates for dual quick rail
  val latestVideo = remember(videosWithInfo) {
    videosWithInfo.firstOrNull { !it.video.isAudio }
  }
  val latestAudio = remember(videosWithInfo) {
    videosWithInfo.firstOrNull { it.video.isAudio }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(bottom = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Flagship Bento Spotlight Hero Card
    BentoHeroSpotlightCard(
      item = heroMedia,
      onClick = { onPlayMedia(heroMedia.video) },
    )

    // Dual-Horizon Rails (Cinematic Video Rail + Sonic Soundstage Rail)
    DualHorizonRails(
      videosWithInfo = videosWithInfo,
      onPlayMedia = onPlayMedia,
      onFilterSelected = onFilterSelected,
    )
  }
}

/**
 * Dual-Horizon Rails component
 * Provides two synchronized horizontal media rails:
 * 1. Upper Rail: Cinematic Video Horizon (16:9 thumbnails, resolution badges, duration, progress)
 * 2. Lower Rail: Sonic Soundstage Horizon (album art, lossless badges, duration)
 */
@Composable
fun DualHorizonRails(
  videosWithInfo: List<VideoWithPlaybackInfo>,
  onPlayMedia: (Video) -> Unit,
  onFilterSelected: ((MediaLibraryType) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val videoItems = remember(videosWithInfo) {
    videosWithInfo.filter { !it.video.isAudio }
  }
  val audioItems = remember(videosWithInfo) {
    videosWithInfo.filter { it.video.isAudio }
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
              onClick = { onPlayMedia(item.video) },
            )
          }
        }
      }
    }

    // Lower Rail: Sonic Soundstage Horizon
    if (audioItems.isNotEmpty()) {
      HorizonRailSection(
        title = "Sonic Soundstage Horizon",
        icon = "🎵",
        accentColor = MaterialTheme.colorScheme.tertiary,
        count = audioItems.size,
        onViewAll = { onFilterSelected?.invoke(MediaLibraryType.Audio) },
      ) {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(
            items = audioItems.take(20),
            key = { it.video.path },
          ) { item ->
            BentoHorizonAudioCard(
              item = item,
              onClick = { onPlayMedia(item.video) },
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

        // Top-left Lossless / Audio Badge
        val audioBadge = when {
          video.mimeType.contains("flac", ignoreCase = true) || video.path.endsWith(".flac", ignoreCase = true) -> "FLAC"
          video.mimeType.contains("wav", ignoreCase = true) || video.path.endsWith(".wav", ignoreCase = true) -> "WAV"
          video.mimeType.contains("alac", ignoreCase = true) || video.path.endsWith(".alac", ignoreCase = true) -> "ALAC"
          video.mimeType.contains("opus", ignoreCase = true) || video.path.endsWith(".opus", ignoreCase = true) -> "OPUS"
          else -> "HI-RES"
        }
        Surface(
          modifier = Modifier
            .padding(6.dp)
            .align(Alignment.TopStart),
          shape = RoundedCornerShape(6.dp),
          color = Color.Black.copy(alpha = 0.72f),
          border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
        ) {
          Text(
            text = audioBadge,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp),
          )
        }

        // Bottom-right Duration Pill
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
      }

      // Metadata Area
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = video.title.ifBlank { FileTypeUtils.stripExtension(video.displayName) },
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
          ),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Text(
          text = if (video.durationFormatted.isNotBlank()) video.durationFormatted else "Audio Track",
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
              "Hi-Res • ${item.video.durationFormatted}"
            } else {
              "${item.video.resolution.ifBlank { "HD" }} • ${item.video.durationFormatted}"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
          )
        }

        // Middle Row: Thumbnail Preview & Metadata
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Media Thumbnail Box
          BentoMediaThumbnail(
            video = item.video,
            modifier = Modifier
              .size(width = if (isAudio) 70.dp else 108.dp, height = 66.dp)
              .clip(RoundedCornerShape(16.dp)),
          )

          // Information & Visualizer
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = if (isAudio && item.video.title.isNotBlank()) {
                item.video.title
              } else {
                FileTypeUtils.stripExtension(item.video.displayName)
              },
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )

            Text(
              text = if (isAudio) {
                item.video.bucketDisplayName.ifBlank { "Lossless Audio" }
              } else {
                "${item.video.sizeFormatted} • ${item.video.bucketDisplayName}"
              },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )

            // Animated Visualizer Waveform or Mini Progress Track
            if (isAudio) {
              BentoEqualizerWaveform(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(18.dp)
                  .padding(top = 2.dp),
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
 * Animated harmonic equalizer bars reacting with fluid sine frequencies
 */
@Composable
private fun BentoEqualizerWaveform(
  modifier: Modifier = Modifier,
) {
  val transition = rememberInfiniteTransition(label = "eqWave")
  val phase by transition.animateFloat(
    initialValue = 0f,
    targetValue = 6.28318f,
    animationSpec = infiniteRepeatable(
      animation = tween(1800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "eqPhase",
  )

  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
    verticalAlignment = Alignment.Bottom,
  ) {
    val barCount = 20
    for (i in 0 until barCount) {
      val normalized = (sin(phase + i * 0.45f) * 0.5f + 0.5f).coerceIn(0.15f, 1f)
      val barHeight = (normalized * 16).dp.coerceAtLeast(3.dp)
      val barColor = if (i % 2 == 0) {
        MaterialTheme.colorScheme.tertiary
      } else {
        MaterialTheme.colorScheme.primary
      }

      Box(
        modifier = Modifier
          .weight(1f)
          .height(barHeight)
          .clip(RoundedCornerShape(999.dp))
          .background(barColor.copy(alpha = 0.85f)),
      )
    }
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

