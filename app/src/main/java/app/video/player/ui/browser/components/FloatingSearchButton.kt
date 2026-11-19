/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.browser.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import app.video.player.ui.icons.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.video.player.R
import app.video.player.ui.browser.NavigationGlassSurface
import app.video.player.ui.browser.navigationGlassRim
import app.video.player.ui.icons.Icons
import app.video.player.ui.liquidglass.drawShapeTint
import app.video.player.ui.liquidglass.liquidGlassEffects
import app.video.player.ui.liquidglass.rememberLiquidGlassSettings
import app.video.player.ui.player.controls.components.tvFocusHighlight
import app.video.player.ui.theme.AppMotion
import app.video.player.ui.utils.rememberAppHaptics
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import dev.chrisbanes.haze.HazeState

/**
 * Circular floating search button matching the fluid glass aesthetics of the floating navigation pill.
 */
@Composable
fun FloatingSearchButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  hazeBackdrop: HazeState? = null,
  kyantBackdrop: Backdrop? = null,
  liquidGlassEnabled: Boolean = false,
  glowStrength: Float = 0f,
  size: Dp = 52.dp,
) {
  val haptics = rememberAppHaptics()
  val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
  val glassSettings = rememberLiquidGlassSettings()
  val reducedMotion = AppMotion.shouldReduceMotion()

  Box(
    modifier =
      modifier
        .size(size)
        .tvFocusHighlight(CircleShape, focusedScale = 1.08f)
        .clip(CircleShape)
        .clickable(
          role = Role.Button,
          interactionSource = remember { MutableInteractionSource() },
          indication = ripple(bounded = true, radius = size / 2),
        ) {
          haptics.selection(true)
          onClick()
        },
    contentAlignment = Alignment.Center,
  ) {
    if (liquidGlassEnabled && kyantBackdrop != null) {
      Box(
        modifier =
          Modifier
            .matchParentSize()
            .drawBackdrop(
              backdrop = kyantBackdrop,
              shape = { CircleShape },
              effects = {
                liquidGlassEffects(
                  glassSettings,
                  8.dp.toPx(),
                  24.dp.toPx(),
                  24.dp.toPx(),
                  vibrant = true,
                  refractionEnabled = !reducedMotion,
                )
              },
              highlight = {
                glassSettings.highlight(Highlight.Ambient.copy(alpha = 0.58f * glowStrength))
              },
              shadow = {
                Shadow(
                  radius = 8.dp,
                  color = Color.Black.copy(alpha = 0.14f * glowStrength),
                )
              },
              onDrawSurface = {
                drawShapeTint(CircleShape, glassSettings.surfaceColor(surfaceColor.copy(alpha = 0.34f)))
              },
            )
            .navigationGlassRim(glowStrength * glassSettings.highlightStrength.coerceAtMost(1f), CircleShape, 0f),
      )
    } else {
      Box(
        modifier =
          Modifier
            .matchParentSize()
            .clip(CircleShape),
      ) {
        NavigationGlassSurface(
          backdrop = hazeBackdrop,
          surfaceColor = surfaceColor,
          glowStrength = glowStrength,
          modifier = Modifier.matchParentSize(),
          shape = CircleShape,
          connectionProgress = 0f,
        )
      }
    }

    Icon(
      icon = Icons.RoundedFilled.Search,
      contentDescription = stringResource(R.string.settings_search_title),
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(24.dp),
    )
  }
}

