/* SPDX-License-Identifier: AGPL-3.0-or-later */

package app.video.player.ui.player.controls.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.video.player.R
import app.video.player.ui.liquidglass.liquidGlassActive
import app.video.player.ui.player.BadgeArtwork
import app.video.player.ui.player.PlaybackFormatBadge
import app.video.player.ui.player.observePlaybackFormatBadges
import com.caverock.androidsvg.SVG
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

private val BadgeRailShape = RoundedCornerShape(12.dp)
private val BrandBackingShape = RoundedCornerShape(5.dp)
private val BadgeSvgBitmapCache = LruCache<String, ImageBitmap>(64)
private val MissingBadgeAssets = ConcurrentHashMap.newKeySet<String>()

@Composable
internal fun PlaybackFormatBadgeRow(
  modifier: Modifier = Modifier,
  logoHeight: Dp = 20.dp,
  tvButtonBackground: Color? = null,
) {
  val formatFlow = remember { observePlaybackFormatBadges() }
  val badges by formatFlow.collectAsStateWithLifecycle(initialValue = emptyList())
  if (badges.isEmpty()) return

  val backgroundLevel = LocalPlayerButtonsOpacity.current
  val foreground = if (tvButtonBackground == null) playerButtonContentColor() else Color.White
  val surfaceColor = if (tvButtonBackground == null) MaterialTheme.colorScheme.surfaceContainerHigh else Color(0xFF232323)
  val inkEdge = if (liquidGlassActive() || backgroundLevel < 0.45f) {
    (if (foreground.luminance() < 0.45f) Color.White else Color.Black).copy(alpha = 0.7f)
  } else Color.Unspecified
  val context = androidx.compose.ui.platform.LocalContext.current
  Surface(
    shape = BadgeRailShape,
    color = surfaceColor.copy(alpha = backgroundLevel.coerceIn(0.25f, 0.92f)),
    modifier = modifier.clickable {
      val dacName = app.video.player.audio.UsbDacMonitor.getConnectedDacName(context)
      val message = if (dacName != null) {
        "Connected DAC: $dacName"
      } else {
        "Output: Internal Speaker (No USB DAC)"
      }
      android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    },
  ) {
    MediaFormatBadges(
      badges = badges,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      logoHeight = logoHeight,
      foreground = foreground,
      background = surfaceColor,
      inkEdge = inkEdge,
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MediaFormatBadges(
  badges: List<PlaybackFormatBadge>,
  modifier: Modifier = Modifier,
  logoHeight: Dp = 20.dp,
  foreground: Color = MaterialTheme.colorScheme.onSurface,
  background: Color = MaterialTheme.colorScheme.surface,
  inkEdge: Color = Color.Unspecified,
) {
  if (badges.isEmpty()) return
  FlowRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    badges.forEach { badge ->
      FormatBadge(
        badge = badge,
        logoHeight = logoHeight,
        foreground = foreground,
        inkEdge = inkEdge,
        darkBackground = background.luminance() < 0.45f,
        modifier = Modifier.align(Alignment.CenterVertically),
      )
    }
  }
}

@Composable
private fun FormatBadge(
  badge: PlaybackFormatBadge,
  logoHeight: Dp,
  foreground: Color,
  inkEdge: Color,
  darkBackground: Boolean,
  modifier: Modifier = Modifier,
) {
  val description = if (badge.isChannelLayout) {
    "${stringResource(R.string.media_info_stat_channels)}: ${badge.detail}"
  } else {
    badge.detail
  }
  val tintArgb = if (darkBackground) badge.darkTintArgb else badge.lightTintArgb
  val badgeForeground = tintArgb?.let(::Color) ?: foreground
  val path = badge.asset
  val painter = if (path == null) null else rememberAssetSvgPainter("media_format_logos/$path", logoHeight)
  Box(modifier.heightIn(min = logoHeight + 4.dp), contentAlignment = Alignment.Center) {
    if (painter == null) {
      val shadow = if (inkEdge.isSpecified) {
        Shadow(color = inkEdge, blurRadius = with(LocalDensity.current) { 2.dp.toPx() })
      } else null
      Text(
        badge.label,
        color = badgeForeground,
        style = MaterialTheme.typography.labelMedium.copy(shadow = shadow),
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
      )
    } else {
      val intrinsic = painter.intrinsicSize
      val aspect = if (intrinsic.height > 0f && intrinsic.width.isFinite() && intrinsic.height.isFinite()) {
        (intrinsic.width / intrinsic.height).coerceIn(0.2f, 12f)
      } else 1f
      val artwork = badge.artwork
      val contrast = when (artwork) {
        BadgeArtwork.OnDark -> Color(0xFF25262B).takeUnless { darkBackground }
        BadgeArtwork.OnLight -> Color(0xFFF6F4F8).takeIf { darkBackground }
        BadgeArtwork.ThemeInk, BadgeArtwork.Original -> null
      }
      val needsBacking = artwork == BadgeArtwork.OnDark || artwork == BadgeArtwork.OnLight
      val backing =
        if (contrast == null) Modifier else Modifier.background(contrast, BrandBackingShape)
      val useThemeInk = artwork == BadgeArtwork.ThemeInk
      val edge = if (useThemeInk && inkEdge.isSpecified) Modifier.logoInkEdge(painter, inkEdge) else Modifier
      Image(
        painter = painter,
        contentDescription = description,
        colorFilter = if (useThemeInk) ColorFilter.tint(badgeForeground) else null,
        contentScale = ContentScale.Fit,
        modifier = backing
          .then(if (needsBacking) Modifier.padding(horizontal = 3.dp, vertical = 2.dp) else Modifier)
          .size(width = logoHeight * aspect, height = logoHeight)
          .then(edge),
      )
    }
  }
}

@Composable
private fun rememberAssetSvgPainter(assetPath: String, targetHeight: Dp): Painter? {
  val context = LocalContext.current
  val density = LocalDensity.current
  val heightPx = (with(density) { targetHeight.toPx() } * 2f).roundToInt().coerceIn(24, 192)
  val bitmap = remember(assetPath, heightPx) { loadAssetSvgBitmap(context, assetPath, heightPx) }
  return remember(bitmap) { bitmap?.let(::BitmapPainter) }
}

private fun loadAssetSvgBitmap(context: Context, assetPath: String, heightPx: Int): ImageBitmap? {
  if (assetPath in MissingBadgeAssets) return null
  val cacheKey = "$assetPath@$heightPx"
  synchronized(BadgeSvgBitmapCache) { BadgeSvgBitmapCache.get(cacheKey) }?.let { return it }
  val rendered = runCatching {
    val svg = SVG.getFromAsset(context.assets, assetPath)
    val viewBox = svg.documentViewBox
    val rawWidth = svg.documentWidth.takeIf { it > 0f } ?: viewBox?.width()?.takeIf { it > 0f } ?: 1f
    val rawHeight = svg.documentHeight.takeIf { it > 0f } ?: viewBox?.height()?.takeIf { it > 0f } ?: 1f
    if (viewBox == null && rawWidth > 0f && rawHeight > 0f) {
      svg.setDocumentViewBox(0f, 0f, rawWidth, rawHeight)
    }
    val aspect = (rawWidth / rawHeight).coerceIn(0.2f, 12f)
    val widthPx = (heightPx * aspect).roundToInt().coerceAtLeast(1)
    svg.setDocumentWidth(widthPx.toFloat())
    svg.setDocumentHeight(heightPx.toFloat())
    Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888).also { bitmap ->
      svg.renderToCanvas(AndroidCanvas(bitmap))
    }.asImageBitmap()
  }.getOrNull()
  if (rendered == null) {
    MissingBadgeAssets.add(assetPath)
  } else {
    synchronized(BadgeSvgBitmapCache) { BadgeSvgBitmapCache.put(cacheKey, rendered) }
  }
  return rendered
}

// Keep monochrome artwork readable over video without adding an opaque plate at low opacity.
private fun Modifier.logoInkEdge(painter: Painter, color: Color): Modifier = drawWithCache {
  val intrinsic = painter.intrinsicSize
  if (intrinsic.width <= 0f || intrinsic.height <= 0f || !intrinsic.width.isFinite() || !intrinsic.height.isFinite()) {
    return@drawWithCache onDrawWithContent { drawContent() }
  }
  val scale = minOf(size.width / intrinsic.width, size.height / intrinsic.height)
  val fitted = intrinsic * scale
  val left = (size.width - fitted.width) / 2f
  val top = (size.height - fitted.height) / 2f
  val edge = 0.65.dp.toPx()
  val tint = ColorFilter.tint(color)
  val offsets = listOf(-edge to 0f, edge to 0f, 0f to -edge, 0f to edge)
  onDrawWithContent {
    for ((x, y) in offsets) {
      translate(left + x, top + y) { with(painter) { draw(fitted, colorFilter = tint) } }
    }
    drawContent()
  }
}
