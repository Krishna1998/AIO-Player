/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.video.player.preferences.AppearancePreferences
import app.video.player.preferences.preference.collectAsState
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File
import java.io.FileOutputStream

val LocalAppWallpaperActive = compositionLocalOf { false }
internal val LocalAppBackgroundBackdrop = staticCompositionLocalOf<Backdrop?> { null }
val LocalLightTextOnWallpaper = compositionLocalOf { false }

@Composable
@androidx.compose.runtime.ReadOnlyComposable
fun Color.onWallpaper(): Color = this

enum class WallpaperScaleMode {
  Fit,
  Fill,
}

enum class WallpaperPreset(
  val id: String,
  val displayName: String,
  val gradientColors: List<Color>,
) {
  None("none", "Solid Dark", emptyList()),
  Aurora("aurora", "Aurora", listOf(Color(0xFF041C1E), Color(0xFF0A3C36), Color(0xFF135058), Color(0xFF06141D))),
  VelvetNebula("nebula", "Velvet Nebula", listOf(Color(0xFF16082F), Color(0xFF28114B), Color(0xFF4A154B), Color(0xFF0B061A))),
  DeepOcean("ocean", "Deep Ocean", listOf(Color(0xFF021B2B), Color(0xFF073B4C), Color(0xFF114B5F), Color(0xFF01111C))),
  CyberMidnight("midnight", "Cyber Glow", listOf(Color(0xFF0D0A24), Color(0xFF1F104F), Color(0xFF003B5C), Color(0xFF080612))),
  SunsetHorizon("sunset", "Sunset Glow", listOf(Color(0xFF2B092C), Color(0xFF5A1846), Color(0xFF900C3F), Color(0xFF19061A))),
  Mist("mist", "Morning Mist", listOf(Color(0xFF141E28), Color(0xFF233240), Color(0xFF32485C), Color(0xFF0E151D))),
}

const val WallpaperDerivedThemeName = "Wallpaper"

@Composable
fun AppWallpaperHost(content: @Composable () -> Unit) {
  val context = LocalContext.current
  val preferences = koinInject<AppearancePreferences>()
  val liquidGlassEnabled by preferences.liquidGlassEnabled.collectAsState()
  val backgroundBackdrop = if (liquidGlassEnabled) rememberLayerBackdrop() else null

  val wallpaperUri by preferences.customWallpaperUri.collectAsState()
  val wallpaperScaleMode by preferences.customWallpaperScaleMode.collectAsState()
  val wallpaperBlur by preferences.customWallpaperBlur.collectAsState()
  val wallpaperAlpha by preferences.customWallpaperAlpha.collectAsState()

  val activePreset = remember(wallpaperUri) {
    if (wallpaperUri.startsWith("preset:")) {
      val key = wallpaperUri.removePrefix("preset:")
      WallpaperPreset.entries.firstOrNull { it.id == key }
    } else null
  }

  val isCustomImage = wallpaperUri.isNotBlank() && !wallpaperUri.startsWith("preset:")
  val isWallpaperActive = (activePreset != null && activePreset != WallpaperPreset.None) || isCustomImage

  var customBitmap by remember(wallpaperUri) { mutableStateOf<Bitmap?>(null) }

  LaunchedEffect(wallpaperUri) {
    if (isCustomImage) {
      customBitmap = withContext(Dispatchers.IO) {
        runCatching {
          val uri = Uri.parse(wallpaperUri)
          if (uri.scheme == "file") {
            BitmapFactory.decodeFile(uri.path)
          } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
              BitmapFactory.decodeStream(stream)
            }
          }
        }.getOrNull()
      }
    } else {
      customBitmap = null
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    // 1. Base Dark Background / Wallpaper Canvas
    Box(
      modifier =
        Modifier
          .matchParentSize()
          .then(if (backgroundBackdrop != null) Modifier.layerBackdrop(backgroundBackdrop) else Modifier)
          .background(MaterialTheme.colorScheme.background),
    ) {
      if (isWallpaperActive) {
        val blurModifier = if (wallpaperBlur > 0f) Modifier.blur(wallpaperBlur.dp) else Modifier
        val contentScale = if (wallpaperScaleMode == WallpaperScaleMode.Fill) ContentScale.Crop else ContentScale.Fit

        if (activePreset != null && activePreset.gradientColors.isNotEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .then(blurModifier)
              .background(Brush.linearGradient(activePreset.gradientColors)),
          )
        } else if (customBitmap != null) {
          Image(
            bitmap = customBitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier
              .fillMaxSize()
              .then(blurModifier),
          )
        }

        // 2. Darkening Scrim Overlay (respects transparency / alpha setting)
        val overlayDim = (1f - wallpaperAlpha.coerceIn(0.1f, 1f)).coerceIn(0f, 0.92f)
        if (overlayDim > 0f) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color.Black.copy(alpha = overlayDim)),
          )
        }
      }
    }

    CompositionLocalProvider(
      LocalAppWallpaperActive provides isWallpaperActive,
      LocalAppBackgroundBackdrop provides backgroundBackdrop,
      LocalLightTextOnWallpaper provides isWallpaperActive,
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        content()
      }
    }
  }
}

@Composable
fun wallpaperAwareBackgroundColor(): Color {
  val isWallpaperActive = LocalAppWallpaperActive.current
  return if (isWallpaperActive) Color.Transparent else MaterialTheme.colorScheme.background
}

suspend fun saveWallpaperCopy(
  context: Context,
  sourceUri: String,
): String = withContext(Dispatchers.IO) {
  if (sourceUri.isBlank() || sourceUri.startsWith("preset:")) return@withContext sourceUri
  runCatching {
    val src = Uri.parse(sourceUri)
    val wallpaperDir = File(context.filesDir, "wallpapers").apply { mkdirs() }
    val destFile = File(wallpaperDir, "app_wallpaper_${System.currentTimeMillis()}.jpg")

    context.contentResolver.openInputStream(src)?.use { input ->
      FileOutputStream(destFile).use { output ->
        input.copyTo(output)
      }
    }
    Uri.fromFile(destFile).toString()
  }.getOrDefault(sourceUri)
}
