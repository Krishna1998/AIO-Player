/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.player.controls.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.video.player.preferences.preference.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.video.player.R
import app.video.player.preferences.AppearancePreferences
import app.video.player.preferences.VolumeAndBrightnessHudStyle
import app.video.player.ui.icons.AppIcon
import app.video.player.ui.icons.Icon
import app.video.player.ui.icons.Icons
import app.video.player.ui.liquidglass.LocalKyantPlayerBackdrop
import app.video.player.ui.liquidglass.liquidGlassActive
import app.video.player.ui.liquidglass.liquidGlassEffects
import app.video.player.ui.liquidglass.rememberLiquidGlassSettings
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import org.koin.compose.koinInject

fun percentage(
  value: Float,
  range: ClosedFloatingPointRange<Float>,
): Float = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

private val LevelPillWidth = 42.dp
private val LevelPillHeight = 220.dp
private val LevelPillInset = 14.dp
private val LevelPillIconSize = 20.dp
private val LevelPillLabelHeight = 18.dp

/**
 * Android TV style level indicator: one capsule filled from the bottom with the theme gradient,
 * the value inside at the top and the icon inside at the bottom. Label and icon switch to the
 * fill's content colour once the level rises under them, so they stay readable at any level.
 */
@Composable
private fun LevelPill(
  fraction: Float,
  overflowFraction: Float,
  label: String,
  icon: AppIcon,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val enableLiquidGlass = liquidGlassActive()
  val glassSettings = rememberLiquidGlassSettings()
  // Glass needs the player's sampled video layer. Until it exists (first frames, renderer switches)
  // the pill keeps its regular darker form instead of drawing a glass rim around nothing.
  val glassBackdrop = LocalKyantPlayerBackdrop.current?.takeIf { enableLiquidGlass }
  val density = LocalDensity.current
  val shape = CircleShape
  val fillSpring = spring<Float>(dampingRatio = 0.85f, stiffness = 380f)
  val level by animateFloatAsState(fraction.coerceIn(0f, 1f), fillSpring, label = "level_pill_fill")
  val overflow by animateFloatAsState(overflowFraction.coerceIn(0f, 1f), fillSpring, label = "level_pill_overflow")
  val covered = maxOf(level, overflow)
  val iconCovered = covered >= (LevelPillInset + LevelPillIconSize / 2) / LevelPillHeight
  val labelCovered = covered >= 1f - (LevelPillInset + LevelPillLabelHeight / 2) / LevelPillHeight
  val overflowOnTop = overflow >= level
  val iconTint by animateColorAsState(
    when {
      !iconCovered -> Color.White
      overflowOnTop -> colors.onError
      else -> colors.onPrimary
    },
    label = "level_pill_icon",
  )
  val labelColor by animateColorAsState(
    when {
      !labelCovered -> Color.White
      overflowOnTop -> colors.onErrorContainer
      else -> colors.onPrimaryContainer
    },
    label = "level_pill_label",
  )
  // Same gradient as before: primaryContainer at the top of the fill, primary at its base.
  val fillBrush = remember(colors.primaryContainer, colors.primary) {
    Brush.verticalGradient(listOf(colors.primaryContainer, colors.primary))
  }
  val overflowBrush = remember(colors.errorContainer, colors.error) {
    Brush.verticalGradient(listOf(colors.errorContainer, colors.error))
  }

  Box(
    modifier =
      modifier
        .size(LevelPillWidth, LevelPillHeight)
        .then(
          if (glassBackdrop != null) {
            Modifier.drawBackdrop(
              backdrop = glassBackdrop,
              shape = { shape },
              effects = {
                liquidGlassEffects(
                  glassSettings,
                  with(density) { 10.dp.toPx() },
                  with(density) { 10.dp.toPx() },
                  with(density) { 16.dp.toPx() },
                )
              },
              highlight = { glassSettings.highlight(Highlight.Default) },
              shadow = { Shadow(color = Color.Black.copy(alpha = 0.25f), radius = 8.dp) },
              innerShadow = { InnerShadow(color = Color.White.copy(alpha = 0.15f), radius = 1.5.dp) },
            )
          } else {
            Modifier
          },
        ).clip(shape)
        .background(Color.Black.copy(alpha = if (glassBackdrop != null) 0.25f else 0.45f))
        .border(1.dp, Color.White.copy(alpha = 0.12f), shape),
  ) {
    Box(
      Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .fillMaxHeight(level)
        .background(fillBrush),
    )
    if (overflow > 0f) {
      Box(
        Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .fillMaxHeight(overflow)
          .background(overflowBrush),
      )
    }
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.withTabularDigits(),
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
      color = labelColor,
      maxLines = 1,
      modifier =
        Modifier
          .align(Alignment.TopCenter)
          .padding(top = LevelPillInset)
          .height(LevelPillLabelHeight)
          .stableNumericWidth(label),
    )
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = iconTint,
      modifier =
        Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = LevelPillInset)
          .size(LevelPillIconSize),
    )
  }
}

/**
 * Top floating liquid capsule (Dynamic Island style).
 */
@Composable
private fun DynamicIslandHud(
  fraction: Float,
  overflowFraction: Float,
  label: String,
  icon: AppIcon,
  isVolume: Boolean,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val enableLiquidGlass = liquidGlassActive()
  val glassSettings = rememberLiquidGlassSettings()
  val glassBackdrop = LocalKyantPlayerBackdrop.current?.takeIf { enableLiquidGlass }
  val density = LocalDensity.current
  val shape = CircleShape
  val fillSpring = spring<Float>(dampingRatio = 0.85f, stiffness = 380f)
  val level by animateFloatAsState(fraction.coerceIn(0f, 1f), fillSpring, label = "di_level")
  val overflow by animateFloatAsState(overflowFraction.coerceIn(0f, 1f), fillSpring, label = "di_overflow")

  val fillBrush = remember(colors.primaryContainer, colors.primary) {
    Brush.horizontalGradient(listOf(colors.primary, colors.primaryContainer))
  }
  val overflowBrush = remember(colors.errorContainer, colors.error) {
    Brush.horizontalGradient(listOf(colors.error, colors.errorContainer))
  }

  Box(
    modifier = modifier
      .width(324.dp)
      .height(56.dp)
      .then(
        if (glassBackdrop != null) {
          Modifier.drawBackdrop(
            backdrop = glassBackdrop,
            shape = { shape },
            effects = {
              liquidGlassEffects(
                glassSettings,
                with(density) { 10.dp.toPx() },
                with(density) { 10.dp.toPx() },
                with(density) { 16.dp.toPx() },
              )
            },
            highlight = { glassSettings.highlight(Highlight.Default) },
            shadow = { Shadow(color = Color.Black.copy(alpha = 0.35f), radius = 10.dp) },
            innerShadow = { InnerShadow(color = Color.White.copy(alpha = 0.15f), radius = 1.5.dp) },
          )
        } else {
          Modifier
        },
      )
      .clip(shape)
      .background(Color.Black.copy(alpha = if (glassBackdrop != null) 0.35f else 0.65f))
      .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
      .padding(horizontal = 18.dp),
    contentAlignment = Alignment.CenterStart,
  ) {
    Row(
      modifier = Modifier.fillMaxSize(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (overflow > 0f) colors.error else colors.primary,
        modifier = Modifier.size(24.dp),
      )

      Spacer(modifier = Modifier.width(12.dp))

      Box(
        modifier = Modifier
          .weight(1f)
          .height(10.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.14f)),
      ) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(level)
            .background(fillBrush),
        )
        if (overflow > 0f) {
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .fillMaxWidth(overflow)
              .background(overflowBrush),
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Text(
        text = label,
        style = MaterialTheme.typography.titleSmall.withTabularDigits().copy(fontSize = 15.sp),
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.End,
        color = if (overflow > 0f) colors.error else Color.White,
        maxLines = 1,
        modifier = Modifier.stableNumericWidth(label),
      )

      if (overflow > 0f && isVolume) {
        Spacer(modifier = Modifier.width(5.dp))
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(colors.error.copy(alpha = 0.22f))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
          Text(
            text = "+dB",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold),
            color = colors.error,
          )
        }
      }
    }
  }
}

/**
 * Dual-island morphology: satellite radar orb with floating glass progress pill (Hyper-Notch Matrix style).
 */
@Composable
private fun HyperNotchMatrixHud(
  fraction: Float,
  overflowFraction: Float,
  label: String,
  icon: AppIcon,
  isVolume: Boolean,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val enableLiquidGlass = liquidGlassActive()
  val glassSettings = rememberLiquidGlassSettings()
  val glassBackdrop = LocalKyantPlayerBackdrop.current?.takeIf { enableLiquidGlass }
  val density = LocalDensity.current
  val shape = CircleShape
  val fillSpring = spring<Float>(dampingRatio = 0.85f, stiffness = 380f)
  val level by animateFloatAsState(fraction.coerceIn(0f, 1f), fillSpring, label = "notch_level")
  val overflow by animateFloatAsState(overflowFraction.coerceIn(0f, 1f), fillSpring, label = "notch_overflow")

  val fillBrush = remember(colors.primaryContainer, colors.primary) {
    Brush.horizontalGradient(listOf(colors.primary, colors.primaryContainer))
  }
  val overflowBrush = remember(colors.errorContainer, colors.error) {
    Brush.horizontalGradient(listOf(colors.error, colors.errorContainer))
  }

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    // Left Satellite Orb
    Box(
      modifier = Modifier
        .size(52.dp)
        .then(
          if (glassBackdrop != null) {
            Modifier.drawBackdrop(
              backdrop = glassBackdrop,
              shape = { shape },
              effects = {
                liquidGlassEffects(
                  glassSettings,
                  with(density) { 10.dp.toPx() },
                  with(density) { 10.dp.toPx() },
                  with(density) { 16.dp.toPx() },
                )
              },
              highlight = { glassSettings.highlight(Highlight.Default) },
              shadow = { Shadow(color = Color.Black.copy(alpha = 0.35f), radius = 8.dp) },
              innerShadow = { InnerShadow(color = Color.White.copy(alpha = 0.2f), radius = 1.5.dp) },
            )
          } else {
            Modifier
          },
        )
        .clip(shape)
        .background(Color.Black.copy(alpha = if (glassBackdrop != null) 0.35f else 0.65f))
        .border(
          1.2.dp,
          if (overflow > 0f) colors.error.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.2f),
          shape,
        ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (overflow > 0f) colors.error else colors.primary,
        modifier = Modifier.size(24.dp),
      )
    }

    Spacer(modifier = Modifier.width(10.dp))

    // Right Floating Glass Pill
    Box(
      modifier = Modifier
        .width(264.dp)
        .height(52.dp)
        .then(
          if (glassBackdrop != null) {
            Modifier.drawBackdrop(
              backdrop = glassBackdrop,
              shape = { shape },
              effects = {
                liquidGlassEffects(
                  glassSettings,
                  with(density) { 10.dp.toPx() },
                  with(density) { 10.dp.toPx() },
                  with(density) { 16.dp.toPx() },
                )
              },
              highlight = { glassSettings.highlight(Highlight.Default) },
              shadow = { Shadow(color = Color.Black.copy(alpha = 0.35f), radius = 8.dp) },
              innerShadow = { InnerShadow(color = Color.White.copy(alpha = 0.15f), radius = 1.5.dp) },
            )
          } else {
            Modifier
          },
        )
        .clip(shape)
        .background(Color.Black.copy(alpha = if (glassBackdrop != null) 0.35f else 0.65f))
        .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
        .padding(horizontal = 16.dp),
      contentAlignment = Alignment.CenterStart,
    ) {
      Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .height(9.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f)),
        ) {
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .fillMaxWidth(level)
              .background(fillBrush),
          )
          if (overflow > 0f) {
            Box(
              modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(overflow)
                .background(overflowBrush),
            )
          }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
          text = label,
          style = MaterialTheme.typography.titleSmall.withTabularDigits().copy(fontSize = 14.5.sp),
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.End,
          color = if (overflow > 0f) colors.error else Color.White,
          maxLines = 1,
          modifier = Modifier.stableNumericWidth(label),
        )

        if (overflow > 0f && isVolume) {
          Spacer(modifier = Modifier.width(4.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(colors.error.copy(alpha = 0.22f))
              .padding(horizontal = 3.dp, vertical = 1.dp),
          ) {
            Text(
              text = "+dB",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold),
              color = colors.error,
            )
          }
        }
      }
    }
  }
}

/**
 * VisionOS dual-chamber glass card with separate icon badge and slider (Aero Dual-Tone style).
 */
@Composable
private fun AeroDualToneHud(
  fraction: Float,
  overflowFraction: Float,
  label: String,
  icon: AppIcon,
  isVolume: Boolean,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val enableLiquidGlass = liquidGlassActive()
  val glassSettings = rememberLiquidGlassSettings()
  val glassBackdrop = LocalKyantPlayerBackdrop.current?.takeIf { enableLiquidGlass }
  val density = LocalDensity.current
  val shape = RoundedCornerShape(20.dp)
  val fillSpring = spring<Float>(dampingRatio = 0.85f, stiffness = 380f)
  val level by animateFloatAsState(fraction.coerceIn(0f, 1f), fillSpring, label = "aero_level")
  val overflow by animateFloatAsState(overflowFraction.coerceIn(0f, 1f), fillSpring, label = "aero_overflow")

  val fillBrush = remember(colors.primaryContainer, colors.primary) {
    Brush.horizontalGradient(listOf(colors.primary, colors.primaryContainer))
  }
  val overflowBrush = remember(colors.errorContainer, colors.error) {
    Brush.horizontalGradient(listOf(colors.error, colors.errorContainer))
  }

  Box(
    modifier = modifier
      .width(336.dp)
      .height(78.dp)
      .then(
        if (glassBackdrop != null) {
          Modifier.drawBackdrop(
            backdrop = glassBackdrop,
            shape = { shape },
            effects = {
              liquidGlassEffects(
                glassSettings,
                with(density) { 12.dp.toPx() },
                with(density) { 12.dp.toPx() },
                with(density) { 18.dp.toPx() },
              )
            },
            highlight = { glassSettings.highlight(Highlight.Default) },
            shadow = { Shadow(color = Color.Black.copy(alpha = 0.35f), radius = 12.dp) },
            innerShadow = { InnerShadow(color = Color.White.copy(alpha = 0.16f), radius = 1.5.dp) },
          )
        } else {
          Modifier
        },
      )
      .clip(shape)
      .background(Color.Black.copy(alpha = if (glassBackdrop != null) 0.32f else 0.65f))
      .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
      .padding(horizontal = 14.dp, vertical = 10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxSize(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(
            if (overflow > 0f) colors.errorContainer.copy(alpha = 0.28f)
            else colors.primaryContainer.copy(alpha = 0.28f),
          )
          .border(
            1.dp,
            if (overflow > 0f) colors.error.copy(alpha = 0.35f)
            else Color.White.copy(alpha = 0.15f),
            RoundedCornerShape(16.dp),
          ),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (overflow > 0f) colors.error else colors.primary,
            modifier = Modifier.size(23.dp),
          )
          Text(
            text = if (overflow > 0f) "BST" else if (isVolume) "VOL" else "BRI",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 0.5.sp,
            ),
            color = if (overflow > 0f) colors.error else Color.White.copy(alpha = 0.7f),
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = if (isVolume) "Volume" else "Brightness",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 12.5.sp,
              fontWeight = FontWeight.SemiBold,
            ),
            color = Color.White.copy(alpha = 0.65f),
          )

          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = label,
              style = MaterialTheme.typography.titleMedium.withTabularDigits().copy(fontSize = 15.sp),
              fontWeight = FontWeight.Black,
              color = if (overflow > 0f) colors.error else Color.White,
            )

            if (overflow > 0f && isVolume) {
              Spacer(modifier = Modifier.width(4.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(colors.error.copy(alpha = 0.22f))
                  .padding(horizontal = 3.dp, vertical = 1.dp),
              ) {
                Text(
                  text = "BOOST",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                  ),
                  color = colors.error,
                )
              }
            }
          }
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(9.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f)),
        ) {
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .fillMaxWidth(level)
              .background(fillBrush),
          )
          if (overflow > 0f) {
            Box(
              modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(overflow)
                .background(overflowBrush),
            )
          }
        }
      }
    }
  }
}

/**
 * Discrete futuristic glass blocks (Segmented Blocks style).
 */
@Composable
private fun SegmentedBlocksHud(
  fraction: Float,
  overflowFraction: Float,
  label: String,
  icon: AppIcon,
  isVolume: Boolean,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val enableLiquidGlass = liquidGlassActive()
  val glassSettings = rememberLiquidGlassSettings()
  val glassBackdrop = LocalKyantPlayerBackdrop.current?.takeIf { enableLiquidGlass }
  val density = LocalDensity.current
  val shape = RoundedCornerShape(16.dp)
  val fillSpring = spring<Float>(dampingRatio = 0.85f, stiffness = 380f)
  val level by animateFloatAsState(fraction.coerceIn(0f, 1f), fillSpring, label = "seg_level")
  val overflow by animateFloatAsState(overflowFraction.coerceIn(0f, 1f), fillSpring, label = "seg_overflow")

  val totalSegments = 10
  val activeSegments = (level * totalSegments).toInt().coerceIn(0, totalSegments)
  val overflowSegments = if (overflow > 0f) (overflow * 3).toInt().coerceAtLeast(1) else 0

  Box(
    modifier = modifier
      .width(48.dp)
      .height(232.dp)
      .then(
        if (glassBackdrop != null) {
          Modifier.drawBackdrop(
            backdrop = glassBackdrop,
            shape = { shape },
            effects = {
              liquidGlassEffects(
                glassSettings,
                with(density) { 10.dp.toPx() },
                with(density) { 10.dp.toPx() },
                with(density) { 16.dp.toPx() },
              )
            },
            highlight = { glassSettings.highlight(Highlight.Default) },
            shadow = { Shadow(color = Color.Black.copy(alpha = 0.3f), radius = 8.dp) },
            innerShadow = { InnerShadow(color = Color.White.copy(alpha = 0.15f), radius = 1.5.dp) },
          )
        } else {
          Modifier
        },
      )
      .clip(shape)
      .background(Color.Black.copy(alpha = if (glassBackdrop != null) 0.30f else 0.58f))
      .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
      .padding(vertical = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.fillMaxHeight(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.withTabularDigits(),
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        color = if (overflow > 0f) colors.error else colors.primary,
        maxLines = 1,
        modifier = Modifier.stableNumericWidth(label),
      )

      Column(
        verticalArrangement = Arrangement.spacedBy(3.5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        for (i in 9 downTo 0) {
          val isActive = i < activeSegments
          val isBoosted = overflowSegments > 0 && i >= (totalSegments - overflowSegments)
          val blockColor = when {
            isBoosted -> colors.error
            isActive -> colors.primary
            else -> Color.White.copy(alpha = 0.10f)
          }

          Box(
            modifier = Modifier
              .width(30.dp)
              .height(8.5.dp)
              .clip(RoundedCornerShape(3.5.dp))
              .background(blockColor),
          )
        }
      }

      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (overflow > 0f) colors.error else colors.primary,
        modifier = Modifier.size(19.dp),
      )
    }
  }
}

/**
 * Floating glass bento card (SoundStage Bento style).
 */
@Composable
private fun SoundStageBentoHud(
  fraction: Float,
  overflowFraction: Float,
  label: String,
  icon: AppIcon,
  isVolume: Boolean,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val enableLiquidGlass = liquidGlassActive()
  val glassSettings = rememberLiquidGlassSettings()
  val glassBackdrop = LocalKyantPlayerBackdrop.current?.takeIf { enableLiquidGlass }
  val density = LocalDensity.current
  val shape = RoundedCornerShape(18.dp)
  val fillSpring = spring<Float>(dampingRatio = 0.85f, stiffness = 380f)
  val level by animateFloatAsState(fraction.coerceIn(0f, 1f), fillSpring, label = "bento_level")
  val overflow by animateFloatAsState(overflowFraction.coerceIn(0f, 1f), fillSpring, label = "bento_overflow")

  val fillBrush = remember(colors.primaryContainer, colors.primary) {
    Brush.horizontalGradient(listOf(colors.primary, colors.primaryContainer))
  }
  val overflowBrush = remember(colors.errorContainer, colors.error) {
    Brush.horizontalGradient(listOf(colors.error, colors.errorContainer))
  }

  Box(
    modifier = modifier
      .width(334.dp)
      .height(76.dp)
      .then(
        if (glassBackdrop != null) {
          Modifier.drawBackdrop(
            backdrop = glassBackdrop,
            shape = { shape },
            effects = {
              liquidGlassEffects(
                glassSettings,
                with(density) { 12.dp.toPx() },
                with(density) { 12.dp.toPx() },
                with(density) { 18.dp.toPx() },
              )
            },
            highlight = { glassSettings.highlight(Highlight.Default) },
            shadow = { Shadow(color = Color.Black.copy(alpha = 0.35f), radius = 12.dp) },
            innerShadow = { InnerShadow(color = Color.White.copy(alpha = 0.16f), radius = 1.5.dp) },
          )
        } else {
          Modifier
        },
      )
      .clip(shape)
      .background(Color.Black.copy(alpha = if (glassBackdrop != null) 0.32f else 0.62f))
      .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
      .padding(horizontal = 16.dp, vertical = 11.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(
                if (overflow > 0f) colors.errorContainer.copy(alpha = 0.35f)
                else colors.primaryContainer.copy(alpha = 0.35f)
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = if (overflow > 0f) colors.error else colors.primary,
              modifier = Modifier.size(19.dp),
            )
          }

          Text(
            text = if (isVolume) "VOLUME" else "BRIGHTNESS",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 11.5.sp,
              letterSpacing = 0.8.sp,
              fontWeight = FontWeight.Bold,
            ),
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 8.dp),
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.withTabularDigits().copy(fontSize = 15.sp),
            fontWeight = FontWeight.ExtraBold,
            color = if (overflow > 0f) colors.error else Color.White,
          )

          if (overflow > 0f && isVolume) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(colors.error.copy(alpha = 0.22f))
                .padding(horizontal = 3.dp, vertical = 1.dp),
            ) {
              Text(
                text = "BOOST",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Black),
                color = colors.error,
              )
            }
          }
        }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(9.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.14f)),
      ) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(level)
            .background(fillBrush),
        )
        if (overflow > 0f) {
          Box(
            modifier = Modifier
              .fillMaxHeight()
              .fillMaxWidth(overflow)
              .background(overflowBrush),
          )
        }
      }
    }
  }
}

@Composable
private fun MasterHud(
  fraction: Float,
  overflowFraction: Float,
  label: String,
  icon: AppIcon,
  isVolume: Boolean,
  modifier: Modifier = Modifier,
) {
  val appearancePreferences = koinInject<AppearancePreferences>()
  val hudStyle by appearancePreferences.volumeAndBrightnessHudStyle.collectAsState()

  when (hudStyle) {
    VolumeAndBrightnessHudStyle.DynamicIsland ->
      DynamicIslandHud(
        fraction = fraction,
        overflowFraction = overflowFraction,
        label = label,
        icon = icon,
        isVolume = isVolume,
        modifier = modifier,
      )
    VolumeAndBrightnessHudStyle.HyperNotchMatrix ->
      HyperNotchMatrixHud(
        fraction = fraction,
        overflowFraction = overflowFraction,
        label = label,
        icon = icon,
        isVolume = isVolume,
        modifier = modifier,
      )
    VolumeAndBrightnessHudStyle.AeroDualTone ->
      AeroDualToneHud(
        fraction = fraction,
        overflowFraction = overflowFraction,
        label = label,
        icon = icon,
        isVolume = isVolume,
        modifier = modifier,
      )
    VolumeAndBrightnessHudStyle.SegmentedBlocks ->
      SegmentedBlocksHud(
        fraction = fraction,
        overflowFraction = overflowFraction,
        label = label,
        icon = icon,
        isVolume = isVolume,
        modifier = modifier,
      )
    VolumeAndBrightnessHudStyle.SoundStageBento ->
      SoundStageBentoHud(
        fraction = fraction,
        overflowFraction = overflowFraction,
        label = label,
        icon = icon,
        isVolume = isVolume,
        modifier = modifier,
      )
    VolumeAndBrightnessHudStyle.ClassicCapsule ->
      LevelPill(
        fraction = fraction,
        overflowFraction = overflowFraction,
        label = label,
        icon = icon,
        modifier = modifier,
      )
  }
}

@Composable
fun BrightnessSlider(
  brightness: Float,
  positiveRange: ClosedFloatingPointRange<Float>,
  negativeRange: ClosedFloatingPointRange<Float>,
  modifier: Modifier = Modifier,
) {
  val coercedBrightness = brightness.coerceIn(-negativeRange.endInclusive, positiveRange.endInclusive)
  val percentInt = (coercedBrightness * 100).toInt()
  val percentText = remember(percentInt) { "$percentInt%" }
  val level = percentage(coercedBrightness.coerceIn(0f, positiveRange.endInclusive), positiveRange)
  MasterHud(
    fraction = level,
    overflowFraction = percentage((-coercedBrightness).coerceIn(0f, negativeRange.endInclusive), negativeRange),
    label = percentText,
    icon =
      when {
        coercedBrightness < 0 -> Icons.RoundedFilled.Brightness6
        level <= 0.3f -> Icons.RoundedFilled.BrightnessLow
        level <= 0.6f -> Icons.RoundedFilled.BrightnessMedium
        else -> Icons.RoundedFilled.BrightnessHigh
      },
    isVolume = false,
    modifier = modifier,
  )
}

@Composable
fun VolumeSlider(
  volume: Int,
  volumePercentage: Int,
  mpvVolume: Int,
  range: ClosedRange<Int>,
  boostRange: ClosedRange<Int>?,
  modifier: Modifier = Modifier,
  displayAsPercentage: Boolean = false,
) {
  val volumePercent = volumePercentage.coerceIn(0, 100)
  val boostVolume = mpvVolume - 100
  val textStr = getVolumeSliderText(volume, mpvVolume, boostVolume, volumePercent, displayAsPercentage)
  val volumeText = remember(textStr, displayAsPercentage) {
    textStr + if (displayAsPercentage && !textStr.contains('%')) "%" else ""
  }
  val level =
    if (displayAsPercentage) {
      volumePercent / 100f
    } else {
      percentage(volume.toFloat(), range.start.toFloat()..range.endInclusive.toFloat())
    }
  val boostLevel =
    boostRange?.let {
      percentage(boostVolume.toFloat(), it.start.toFloat()..it.endInclusive.toFloat())
    } ?: 0f
  MasterHud(
    fraction = level,
    overflowFraction = boostLevel,
    label = volumeText,
    icon =
      when (volumePercent) {
        0 -> Icons.RoundedFilled.VolumeOff
        in 0..30 -> Icons.RoundedFilled.VolumeMute
        in 30..60 -> Icons.RoundedFilled.VolumeDown
        in 60..100 -> Icons.RoundedFilled.VolumeUp
        else -> Icons.RoundedFilled.VolumeOff
      },
    isVolume = true,
    modifier = modifier,
  )
}

@Composable
fun getVolumeSliderText(
  volume: Int,
  mpvVolume: Int,
  boostVolume: Int,
  percentage: Int,
  displayAsPercentage: Boolean,
): String {
  return when {
    mpvVolume == 100 ->
      if (displayAsPercentage) {
        "$percentage"
      } else {
        "$volume"
      }

    mpvVolume > 100 -> {
      if (displayAsPercentage) {
        "${percentage + boostVolume}"
      } else {
        stringResource(R.string.volume_slider_absolute_value, volume + boostVolume)
      }
    }

    else -> {
      if (displayAsPercentage) {
        "$percentage"
      } else {
        "$volume"
      }
    }
  }
}
