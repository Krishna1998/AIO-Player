package app.video.player.ui.browser

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

/**
 * Convex liquid bubble illumination reproducing the specular highlights and soft inset shade
 * from Clash-Projects/LastWave-Native (#565).
 */
internal fun DrawScope.drawLiquidBubbleHighlight(
  path: Path,
  isDark: Boolean,
  accentColor: Color,
  intensity: Float = 1f,
) {
  val strength = intensity.coerceIn(0f, 1f)
  if (strength <= 0f || size.width <= 0f || size.height <= 0f) return

  val radius = size.minDimension * 0.5f
  val rimWidth = 0.8.dp.toPx().coerceAtMost(radius)
  val rimStroke = Stroke(rimWidth)
  val oppositeStroke = Stroke(0.45.dp.toPx().coerceAtMost(radius))
  val broadShade = Stroke(6.dp.toPx().coerceAtMost(radius))
  val middleShade = Stroke(3.5.dp.toPx().coerceAtMost(radius))
  val narrowShade = Stroke(1.5.dp.toPx().coerceAtMost(radius))

  // Upper-left specular light reflection
  val upperReflection = Brush.linearGradient(
    0f to (if (isDark) Color.White.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.90f)),
    0.24f to (if (isDark) Color.White.copy(alpha = 0.35f) else accentColor.copy(alpha = 0.45f)),
    0.52f to Color.Transparent,
    1f to Color.Transparent,
    start = Offset.Zero,
    end = Offset(size.width * 0.78f, size.height),
  )

  // Opposite bottom-right subtle reflection
  val oppositeReflection = Brush.linearGradient(
    0f to Color.Transparent,
    0.62f to Color.Transparent,
    0.84f to Color.White.copy(alpha = 0.08f),
    1f to (if (isDark) Color.White.copy(alpha = 0.32f) else accentColor.copy(alpha = 0.30f)),
    start = Offset.Zero,
    end = Offset(size.width, size.height),
  )

  // Soft inset shade for depth
  val lowerShade = Brush.verticalGradient(
    0f to Color.Transparent,
    0.50f to Color.Transparent,
    0.74f to Color.Black.copy(alpha = 0.035f),
    1f to Color.Black.copy(alpha = if (isDark) 0.16f else 0.10f),
    endY = size.height,
  )

  // Subtle glassy surface sheen
  val surfaceReflection = Brush.verticalGradient(
    0f to Color.White.copy(alpha = if (isDark) 0.07f else 0.05f),
    0.38f to Color.White.copy(alpha = 0.01f),
    0.64f to Color.Transparent,
    1f to Color.White.copy(alpha = 0.02f),
    endY = size.height,
  )

  clipPath(path) {
    drawRect(surfaceReflection, alpha = strength)
    drawPath(path, lowerShade, alpha = strength * 0.40f, style = broadShade)
    drawPath(path, lowerShade, alpha = strength * 0.50f, style = middleShade)
    drawPath(path, lowerShade, alpha = strength * 0.65f, style = narrowShade)
    drawPath(path, upperReflection, alpha = strength, style = rimStroke)
    drawPath(path, oppositeReflection, alpha = strength, style = oppositeStroke)
  }
}
