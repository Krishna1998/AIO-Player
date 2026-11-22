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
  val rimWidth = 1.1.dp.toPx().coerceAtMost(radius)
  val rimStroke = Stroke(rimWidth)
  val oppositeStroke = Stroke(0.65.dp.toPx().coerceAtMost(radius))
  val broadShade = Stroke(8.dp.toPx().coerceAtMost(radius))
  val middleShade = Stroke(4.5.dp.toPx().coerceAtMost(radius))
  val narrowShade = Stroke(2.0.dp.toPx().coerceAtMost(radius))

  // Upper-left specular light reflection (bright glass glint)
  val upperReflection = Brush.linearGradient(
    0f to (if (isDark) Color.White.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.95f)),
    0.28f to (if (isDark) Color.White.copy(alpha = 0.45f) else accentColor.copy(alpha = 0.55f)),
    0.58f to Color.Transparent,
    1f to Color.Transparent,
    start = Offset.Zero,
    end = Offset(size.width * 0.82f, size.height),
  )

  // Opposite bottom-right subtle reflection
  val oppositeReflection = Brush.linearGradient(
    0f to Color.Transparent,
    0.55f to Color.Transparent,
    0.80f to Color.White.copy(alpha = 0.14f),
    1f to (if (isDark) Color.White.copy(alpha = 0.42f) else accentColor.copy(alpha = 0.38f)),
    start = Offset.Zero,
    end = Offset(size.width, size.height),
  )

  // Soft inset shade for depth (curved glass meniscus)
  val lowerShade = Brush.verticalGradient(
    0f to Color.Transparent,
    0.45f to Color.Transparent,
    0.70f to Color.Black.copy(alpha = 0.05f),
    1f to Color.Black.copy(alpha = if (isDark) 0.22f else 0.14f),
    endY = size.height,
  )

  // Glassy surface sheen and light spill
  val surfaceReflection = Brush.verticalGradient(
    0f to Color.White.copy(alpha = if (isDark) 0.12f else 0.08f),
    0.35f to Color.White.copy(alpha = 0.03f),
    0.65f to Color.Transparent,
    1f to Color.White.copy(alpha = 0.04f),
    endY = size.height,
  )

  clipPath(path) {
    drawRect(surfaceReflection, alpha = strength)
    drawPath(path, lowerShade, alpha = strength * 0.50f, style = broadShade)
    drawPath(path, lowerShade, alpha = strength * 0.65f, style = middleShade)
    drawPath(path, lowerShade, alpha = strength * 0.80f, style = narrowShade)
    drawPath(path, upperReflection, alpha = strength, style = rimStroke)
    drawPath(path, oppositeReflection, alpha = strength, style = oppositeStroke)
  }
}
