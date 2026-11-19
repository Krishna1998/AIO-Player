/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package app.video.player.ui.liquidglass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import com.kyant.shapes.Capsule
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.video.player.ui.icons.AppIcon
import app.video.player.ui.icons.AnimatedIcon
import app.video.player.ui.player.controls.LocalPlayerButtonsClickEvent
import app.video.player.ui.player.controls.PlayerButtonAlpha
import app.video.player.ui.player.controls.components.LocalBorderlessPlayerControls
import app.video.player.ui.player.controls.components.LocalPlayerButtonAlpha
import app.video.player.ui.player.controls.components.LocalPlayerButtonScale
import app.video.player.ui.player.controls.components.LocalPlayerButtonsOpacity
import app.video.player.ui.player.controls.components.asPlayerButtonBackground
import app.video.player.ui.player.controls.components.playerButtonBorderColor
import app.video.player.ui.player.controls.components.playerButtonContainerColor
import app.video.player.ui.player.controls.components.playerButtonContentColor
import app.video.player.ui.player.controls.components.tvFocusHighlight
import app.video.player.ui.theme.spacing
import app.video.player.ui.theme.AppMotion
import app.video.player.ui.theme.appPressFeedback
import com.kyant.backdrop.Backdrop

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AdaptiveControlsButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: AppIcon? = null,
  onLongClick: () -> Unit = {},
  text: String? = null,
  title: String? = null,
  color: Color? = null,
  surfaceColor: Color = Color.Unspecified,
  buttonSize: Dp = PlayerLiquidTokens.ButtonSize,
  useGlass: Boolean = true,
  backdrop: Backdrop? = LocalKyantPlayerBackdrop.current,
  enabled: Boolean = true,
  onLongClickLabel: String? = null,
  borderless: Boolean? = null,
) {
  val scale = LocalPlayerButtonScale.current.coerceIn(0.7f, 1.4f)
  val effectiveButtonSize = if (buttonSize == PlayerLiquidTokens.ButtonSize) buttonSize * scale else buttonSize
  val effectiveIconSize = PlayerLiquidTokens.IconSize * scale
  AdaptiveControlsContainer(
    onClick = onClick,
    modifier = modifier.widthIn(min = effectiveButtonSize),
    onLongClick = onLongClick,
    color = color ?: Color.Unspecified,
    surfaceColor = surfaceColor,
    useGlass = useGlass,
    borderless = borderless ?: LocalBorderlessPlayerControls.current,
    buttonSize = effectiveButtonSize,
    spacing = 4.dp * scale,
    horizontalPadding = if (text != null) 8.dp * scale else 0.dp,
    backdrop = backdrop,
    enabled = enabled,
    onLongClickLabel = onLongClickLabel,
  ) {
    if (icon != null) {
      AnimatedIcon(
        imageVector = icon,
        contentDescription = title ?: text,
        tint = LocalContentColor.current,
        modifier = Modifier.size(effectiveIconSize),
        reduceMotion = AppMotion.shouldReduceMotion(),
      )
    }
    if (text != null) {
      Text(
        text = text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = LocalContentColor.current,
        maxLines = 1,
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AdaptiveControlsContainer(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: () -> Unit = {},
  color: Color = Color.Unspecified,
  surfaceColor: Color = Color.Unspecified,
  isInteractive: Boolean = true,
  useGlass: Boolean = true,
  borderless: Boolean = LocalBorderlessPlayerControls.current,
  buttonSize: Dp = 40.dp,
  spacing: Dp = 8.dp,
  horizontalPadding: Dp? = null,
  backdrop: Backdrop? = LocalKyantPlayerBackdrop.current,
  enabled: Boolean = true,
  onLongClickLabel: String? = null,
  content: @Composable RowScope.() -> Unit,
) {
  val liquidGlassEnabled = liquidGlassActive()
  val clickEvent = LocalPlayerButtonsClickEvent.current
  val interactionSource = remember { MutableInteractionSource() }
  val reduceMotion = AppMotion.playerReducedMotion()
  val resolvedTint =
    color.takeUnless { it == Color.Unspecified }
      ?: if (borderless) LocalContentColor.current
      else if (liquidGlassEnabled) PlayerLiquidTokens.contentColor else playerButtonContentColor()
  val backgroundLevel = LocalPlayerButtonsOpacity.current
  val resolvedSurface =
    if (liquidGlassEnabled) {
      // Below the default level the whole glass layer fades instead (glassLayerAlpha).
      val base = surfaceColor.takeUnless { it == Color.Unspecified } ?: PlayerLiquidTokens.surfaceColor
      base.copy(
        alpha =
          PlayerButtonAlpha.background(
            base.alpha,
            maxOf(backgroundLevel, PlayerButtonAlpha.DEFAULT_BACKGROUND_LEVEL),
          ),
      )
    } else {
      surfaceColor.takeUnless { it == Color.Unspecified }?.asPlayerButtonBackground()
        ?: playerButtonContainerColor()
    }
  val contentTint =
    if (enabled) resolvedTint else resolvedTint.copy(alpha = resolvedTint.alpha * PlayerButtonAlpha.DISABLED_CONTENT)
  val renderLiquidGlass = liquidGlassEnabled && useGlass && !borderless
  val buttonShape = if (horizontalPadding != null && horizontalPadding > 0.dp) Capsule() else CircleShape
  // Above the default level the glass tint thickens toward opaque; below it the whole glass fades.
  val glassLayerAlpha =
    (backgroundLevel / PlayerButtonAlpha.DEFAULT_BACKGROUND_LEVEL).coerceIn(0f, 1f)
  val buttonAlpha = LocalPlayerButtonAlpha.current.coerceIn(0f, 1f)
  val controlModifier =
    modifier
      .alpha(buttonAlpha)
      // LiquidButton already clips its glass to the capsule. Clipping again here, over the larger
      // minimum touch target, would cut its drop shadow into a hard-edged halo.
      .tvFocusHighlight(buttonShape, isInteractive && enabled, clipToShape = !renderLiquidGlass)
      .then(if (isInteractive) Modifier.minimumInteractiveComponentSize() else Modifier)

  if (renderLiquidGlass) {
    CompositionLocalProvider(LocalContentColor provides contentTint) {
      LiquidButton(
        onClick = {
          clickEvent()
          onClick()
        },
        onLongClick = {
          clickEvent()
          onLongClick()
        },
        onLongClickLabel = onLongClickLabel,
        modifier = controlModifier,
        enabled = isInteractive && enabled,
        surfaceColor = resolvedSurface,
        height = buttonSize,
        spacing = spacing,
        horizontalPadding = horizontalPadding ?: 8.dp,
        backdrop = backdrop,
        backgroundAlpha = glassLayerAlpha,
        content = content,
      )
    }
  } else {
    val fallbackShape = buttonShape
    val useGlassSurface = useGlass && !borderless
    val glassFill = remember(resolvedSurface) {
      Brush.verticalGradient(
        0f to resolvedSurface.copy(alpha = (resolvedSurface.alpha * 1.15f).coerceIn(0f, 1f)),
        0.4f to resolvedSurface.copy(alpha = (resolvedSurface.alpha * 0.85f).coerceIn(0f, 1f)),
        1f to resolvedSurface.copy(alpha = (resolvedSurface.alpha * 1.05f).coerceIn(0f, 1f)),
      )
    }
    val specularBorder = remember(resolvedSurface) {
      Brush.verticalGradient(
        listOf(
          Color.White.copy(alpha = 0.32f),
          Color.White.copy(alpha = 0.10f),
          Color.White.copy(alpha = 0.02f),
          resolvedSurface.copy(alpha = 0.20f),
        ),
      )
    }

    CompositionLocalProvider(LocalContentColor provides contentTint) {
      Box(
        modifier =
          controlModifier
            .heightIn(min = buttonSize)
            .then(
              if (useGlassSurface) {
                Modifier
                  .outsideShadow(fallbackShape, radius = 4.dp)
                  .clip(fallbackShape)
                  .background(glassFill, fallbackShape)
                  .border(1.dp, specularBorder, fallbackShape)
              } else {
                Modifier.clip(fallbackShape)
              },
            )
            .appPressFeedback(
              interactionSource,
              enabled = isInteractive && enabled,
              pressedScale = 0.94f,
              reduceMotion = reduceMotion,
            )
            .then(
              if (isInteractive) {
                Modifier.combinedClickable(
                  enabled = enabled,
                  role = Role.Button,
                  interactionSource = interactionSource,
                  indication = ripple(bounded = true),
                  onClick = {
                    clickEvent()
                    onClick()
                  },
                  onLongClick = {
                    clickEvent()
                    onLongClick()
                  },
                  onLongClickLabel = onLongClickLabel,
                )
              } else {
                Modifier
              },
            ),
        contentAlignment = Alignment.Center,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = horizontalPadding ?: MaterialTheme.spacing.small),
          horizontalArrangement = Arrangement.spacedBy(spacing),
          content = content,
        )
      }
    }
  }
}
