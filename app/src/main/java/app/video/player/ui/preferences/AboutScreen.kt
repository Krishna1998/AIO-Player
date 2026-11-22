/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.preferences

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.ImageView
import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import app.video.player.BuildConfig
import app.video.player.R
import app.video.player.presentation.Screen
import app.video.player.presentation.crash.CrashActivity.Companion.collectDeviceInfo
import app.video.player.ui.icons.Icon
import app.video.player.ui.icons.Icons
import app.video.player.ui.liquidglass.appGlassSurface
import app.video.player.ui.liquidglass.liquidGlassActive
import app.video.player.ui.preferences.PreferenceDivider
import app.video.player.ui.preferences.PreferenceSectionHeader
import app.video.player.ui.preferences.PreferenceSectionPosition
import app.video.player.ui.preferences.PreferenceCard
import app.video.player.ui.theme.LocalAppWallpaperActive
import app.video.player.ui.theme.wallpaperAwareBackgroundColor
import app.video.player.ui.utils.LocalBackStack
import app.video.player.ui.utils.navigateTo
import app.video.player.ui.utils.popSafely
import app.video.player.utils.clipboard.SafeClipboard
import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Serializable
object AboutScreen : Screen {
  @Suppress("DEPRECATION")
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val packageManager: PackageManager = context.packageManager
    val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
    val versionName =
      packageInfo.versionName
        ?.let { value ->
          if (BuildConfig.IS_PREVIEW_BUILD || BuildConfig.IS_EXPERIMENTAL_BUILD) value else value.substringBefore('-')
        }
        ?: BuildConfig.VERSION_NAME
    val buildType = BuildConfig.BUILD_TYPE
    val githubRepoUrl = stringResource(R.string.github_repo_url)
    val isGlassActive = liquidGlassActive()
    val systemStats = remember { collectSystemStats(context) }
    val deviceInfo = remember { collectDeviceInfo() }

    Scaffold(
      containerColor = wallpaperAwareBackgroundColor(),
      topBar = {
        SettingsTopAppBar(
          title = stringResource(id = R.string.pref_about_title),
          onBack = { backstack.popSafely() },
          titleModifier = Modifier.settingsSearchTarget(R.string.pref_about_title),
        )
      },
    ) { paddingValues ->
      ProvideExpressivePreferenceLocals {
        val cs = MaterialTheme.colorScheme
        val transition = rememberInfiniteTransition(label = "quantumNebula")

        // Celestial orbital kinetic spins
        val orbitRotationOuter by transition.animateFloat(
          initialValue = 0f,
          targetValue = 360f,
          animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
          ),
          label = "orbitOuter",
        )
        val orbitRotationInner by transition.animateFloat(
          initialValue = 360f,
          targetValue = 0f,
          animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
          ),
          label = "orbitInner",
        )

        // Cosmic nebula breathing pulse
        val nebulaBreathing by transition.animateFloat(
          initialValue = 0f,
          targetValue = 1f,
          animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
          ),
          label = "nebulaBreathing",
        )

        LazyColumn(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(paddingValues),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // =========================================================================
          // 1. CONCEPT 2: QUANTUM NEBULA SPATIAL HERO CORE
          // =========================================================================
          item {
            Box(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(32.dp))
                  .then(
                    if (isGlassActive) {
                      Modifier.appGlassSurface(shape = RoundedCornerShape(32.dp), enabled = true)
                    } else {
                      Modifier.background(cs.surfaceContainerHigh)
                    },
                  )
                  .drawWithCache {
                    val cx = size.width * (0.25f + 0.50f * nebulaBreathing)
                    val cy = size.height * (0.28f + 0.35f * (1f - nebulaBreathing))
                    val deepViolet = Color(0xFF7C3AED)
                    val cosmicCyan = Color(0xFF06B6D4)
                    val cosmicMagenta = Color(0xFFD946EF)

                    val cosmicAura =
                      Brush.radialGradient(
                        colors =
                          listOf(
                            deepViolet.copy(alpha = if (isGlassActive) 0.38f else 0.28f),
                            cosmicMagenta.copy(alpha = if (isGlassActive) 0.22f else 0.16f),
                            cosmicCyan.copy(alpha = if (isGlassActive) 0.12f else 0.08f),
                            Color.Transparent,
                          ),
                        center = Offset(cx, cy),
                        radius = size.width * 0.95f,
                      )

                    // Stardust optical particle field
                    val stars = listOf(
                      Offset(size.width * 0.15f, size.height * 0.20f) to 1.8f,
                      Offset(size.width * 0.82f, size.height * 0.18f) to 2.2f,
                      Offset(size.width * 0.70f, size.height * 0.65f) to 1.5f,
                      Offset(size.width * 0.22f, size.height * 0.75f) to 2.0f,
                      Offset(size.width * 0.88f, size.height * 0.82f) to 1.6f,
                      Offset(size.width * 0.45f, size.height * 0.10f) to 1.4f,
                    )

                    onDrawBehind {
                      drawRect(brush = cosmicAura)
                      stars.forEach { (pos, radius) ->
                        val twinkle = (0.35f + 0.65f * sin((nebulaBreathing * 2 * PI + pos.x).toDouble()).toFloat()).coerceIn(0.2f, 1f)
                        drawCircle(
                          color = Color.White.copy(alpha = twinkle * 0.75f),
                          radius = radius,
                          center = pos,
                        )
                      }
                    }
                  }
                  .padding(24.dp),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                // Dual Kinetic Quantum Orbital Rings surrounding the App Icon
                Box(
                  modifier = Modifier.size(116.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  // Outer dashed orbital ring
                  Box(
                    modifier =
                      Modifier
                        .fillMaxSize()
                        .rotate(orbitRotationOuter)
                        .drawWithCache {
                          val stroke = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f),
                          )
                          val ringGradient = Brush.sweepGradient(
                            listOf(
                              Color(0xFF8B5CF6).copy(alpha = 0.8f),
                              Color(0xFF06B6D4).copy(alpha = 0.4f),
                              Color(0xFFD946EF).copy(alpha = 0.9f),
                              Color(0xFF8B5CF6).copy(alpha = 0.8f),
                            ),
                          )
                          onDrawBehind {
                            drawCircle(brush = ringGradient, style = stroke)
                          }
                        },
                  )

                  // Inner counter-rotating photon ring
                  Box(
                    modifier =
                      Modifier
                        .size(94.dp)
                        .rotate(orbitRotationInner)
                        .drawWithCache {
                          val stroke = Stroke(width = 1.5.dp.toPx())
                          val ringGradient = Brush.sweepGradient(
                            listOf(
                              Color(0xFFC084FC).copy(alpha = 0.9f),
                              Color.Transparent,
                              Color(0xFF38BDF8).copy(alpha = 0.7f),
                              Color.Transparent,
                            ),
                          )
                          onDrawBehind {
                            drawCircle(brush = ringGradient, style = stroke)
                          }
                        },
                  )

                  // Core Floating Icon with Obsidian Shield
                  Box(
                    modifier =
                      Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(cs.surfaceContainerHighest.copy(alpha = 0.85f))
                        .border(
                          width = 1.8.dp,
                          brush = Brush.linearGradient(
                            listOf(
                              Color(0xFFC084FC).copy(alpha = 0.9f),
                              Color(0xFF38BDF8).copy(alpha = 0.6f),
                            ),
                          ),
                          shape = CircleShape,
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.Center,
                  ) {
                    AndroidView(
                      modifier = Modifier.fillMaxSize(),
                      factory = { ctx ->
                        ImageView(ctx).apply {
                          scaleType = ImageView.ScaleType.FIT_CENTER
                          setImageResource(LauncherIconManager.current(ctx).preview)
                        }
                      },
                      update = { view ->
                        val selectedPreview = LauncherIconManager.current(view.context).preview
                        view.setImageResource(selectedPreview)
                      },
                    )
                  }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // App Title & Futuristic Monospace Edition Tag
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = cs.onSurface,
                  )
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.45f)),
                  ) {
                    Text(
                      text = "QUANTUM",
                      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.ExtraBold,
                      fontFamily = FontFamily.Monospace,
                      color = Color(0xFFC084FC),
                    )
                  }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Version & Build Spec Pills
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = cs.primary.copy(alpha = 0.14f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cs.primary.copy(alpha = 0.35f)),
                  ) {
                    Text(
                      text = "v$versionName",
                      modifier = Modifier.padding(horizontal = 11.dp, vertical = 3.dp),
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = cs.primary,
                      fontFamily = FontFamily.Monospace,
                    )
                  }

                  Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = cs.secondaryContainer.copy(alpha = 0.75f),
                  ) {
                    Text(
                      text = buildType.uppercase(),
                      modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.SemiBold,
                      color = cs.onSecondaryContainer,
                    )
                  }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Developer Badge with Subtle Cosmic Glow
                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color = cs.surfaceContainerHighest.copy(alpha = 0.55f),
                  border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f)),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Icon(
                      imageVector = Icons.RoundedFilled.Favorite,
                      contentDescription = null,
                      modifier = Modifier.size(14.dp),
                      tint = Color(0xFFEC4899),
                    )
                    Text(
                      text = stringResource(R.string.ui_by_ritesh_pandit),
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = cs.onSurfaceVariant,
                    )
                  }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Quick Action Buttons: OSS Matrix & GitHub
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Button(
                    onClick = { backstack.navigateTo(LibrariesScreen) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                      ButtonDefaults.buttonColors(
                        containerColor = cs.primary,
                        contentColor = cs.onPrimary,
                      ),
                  ) {
                    Icon(
                      painter = painterResource(id = R.drawable.ic_library_cube),
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = stringResource(id = R.string.pref_about_oss_libraries),
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold,
                    )
                  }

                  FilledTonalButton(
                    onClick = {
                      context.startActivity(
                        Intent(
                          Intent.ACTION_VIEW,
                          githubRepoUrl.toUri(),
                        ),
                      )
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors =
                      ButtonDefaults.filledTonalButtonColors(
                        containerColor = cs.surfaceContainerHighest,
                        contentColor = cs.onSurface,
                      ),
                  ) {
                    Icon(
                      painter = painterResource(id = R.drawable.ic_github),
                      contentDescription = null,
                      modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = stringResource(R.string.ui_github),
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
              }
            }
          }

          // =========================================================================
          // 2. CORE TELEMETRY MATRIX TILES (Realtime Engine Specs)
          // =========================================================================
          item {
            PreferenceSectionHeader(
              position = PreferenceSectionPosition.First,
              title = "Core Engine Telemetry",
            )
          }

          item {
            PreferenceCard {
              Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  QuantumTelemetryTile(
                    modifier = Modifier.weight(1f),
                    title = "GPU PIPELINE",
                    value = if (BuildConfig.MPV_SUPPORTS_VULKAN) "Vulkan + GLES" else "OpenGL ES",
                    accentColor = Color(0xFF38BDF8),
                  )
                  QuantumTelemetryTile(
                    modifier = Modifier.weight(1f),
                    title = "AUDIO ENGINE",
                    value = "384kHz / 32-bit",
                    accentColor = Color(0xFFC084FC),
                  )
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  QuantumTelemetryTile(
                    modifier = Modifier.weight(1f),
                    title = "CODEC ENGINE",
                    value = "MediaCodec Ultra",
                    accentColor = Color(0xFF34D399),
                  )
                  QuantumTelemetryTile(
                    modifier = Modifier.weight(1f),
                    title = "GLASS SHADER",
                    value = if (isGlassActive) "AGSL Active" else "Standard Blit",
                    accentColor = Color(0xFFF472B6),
                  )
                }
              }
            }
          }

          // =========================================================================
          // 3. HARDWARE ARCHITECTURE & DIAGNOSTICS CARD
          // =========================================================================
          item {
            PreferenceSectionHeader(
              position = PreferenceSectionPosition.Middle,
              title = stringResource(R.string.ui_device_info),
            )
          }

          item {
            PreferenceCard {
              Row(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .clickable {
                      SafeClipboard.copyPlainText(context, "videoplayer_device_info", deviceInfo)
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = Color(0xFF8B5CF6).copy(alpha = 0.16f),
                  modifier = Modifier.size(44.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.RoundedFilled.Info,
                      contentDescription = null,
                      tint = Color(0xFFC084FC),
                      modifier = Modifier.size(24.dp),
                    )
                  }
                }

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = stringResource(R.string.ui_device_info),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                  )
                  Spacer(Modifier.height(2.dp))
                  Text(
                    text = "Tap to copy system architecture & build specifications",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.outline,
                  )
                }

                Icon(
                  imageVector = Icons.RoundedFilled.ContentCopy,
                  contentDescription = null,
                  modifier = Modifier.size(20.dp),
                  tint = cs.onSurfaceVariant,
                )
              }
            }
          }

          // =========================================================================
          // 4. DETAILED REAL-TIME SYSTEM SPECS
          // =========================================================================
          item {
            PreferenceSectionHeader(
              position = PreferenceSectionPosition.Middle,
              title = stringResource(R.string.pref_section_system),
            )
          }

          item {
            PreferenceCard {
              systemStats.forEachIndexed { index, (label, value) ->
                SystemStatRow(label = label, value = value)
                if (index < systemStats.lastIndex) {
                  PreferenceDivider()
                }
              }
            }
          }

          // =========================================================================
          // 5. FOOTER BRANDING & LICENSE
          // =========================================================================
          item {
            Column(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .padding(vertical = 12.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = "Quantum acoustic dynamics and visual perfection",
                style = MaterialTheme.typography.labelMedium,
                color = cs.outline.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
              )
              Spacer(Modifier.height(4.dp))
              Text(
                text = "GNU Affero General Public License v3+",
                style = MaterialTheme.typography.labelSmall,
                color = cs.outline.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun QuantumTelemetryTile(
  modifier: Modifier = Modifier,
  title: String,
  value: String,
  accentColor: Color,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        color = MaterialTheme.colorScheme.outline,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = accentColor,
      )
    }
  }
}

@Composable
private fun SystemStatRow(
  label: String,
  value: String,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f).padding(end = 8.dp),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
      fontFamily = FontFamily.Monospace,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1.4f),
    )
  }
}

private fun collectSystemStats(context: Context): List<Pair<String, String>> {
  val pm = context.packageManager
  val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

  // RAM
  val memInfo = ActivityManager.MemoryInfo()
  am.getMemoryInfo(memInfo)
  val totalRamMb = memInfo.totalMem / (1024 * 1024)
  val ramStr = if (totalRamMb >= 1024) "${"%.1f".format(totalRamMb / 1024f)} GB" else "$totalRamMb MB"

  // GLES version
  val configInfo = am.deviceConfigurationInfo
  val glesVersion =
    if (configInfo.reqGlEsVersion != android.content.pm.ConfigurationInfo.GL_ES_VERSION_UNDEFINED) {
      configInfo.glEsVersion
    } else {
      "Unknown"
    }

  // Vulkan
  val vulkanStr =
    when {
      pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL, 1) -> {
        val features = pm.systemAvailableFeatures
        val vulkanVersionFeature =
          features.firstOrNull {
            it.name?.startsWith("android.hardware.vulkan.version") == true
          }
        if (vulkanVersionFeature != null && Build.VERSION.SDK_INT >= 26) {
          val ver = vulkanVersionFeature.version
          val major = (ver shr 22) and 0x3FF
          val minor = (ver shr 12) and 0x3FF
          "Vulkan $major.$minor (Level 1)"
        } else {
          "Vulkan 1.1+ (Level 1)"
        }
      }
      pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL, 0) -> "Vulkan 1.0 (Level 0)"
      pm.hasSystemFeature("android.hardware.vulkan.compute") -> "Vulkan (compute)"
      else -> "Not supported"
    }

  val abis = Build.SUPPORTED_ABIS.take(2).joinToString(", ")
  val cores = Runtime.getRuntime().availableProcessors()

  return listOf(
    "Manufacturer" to Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
    "Device" to "${Build.MODEL} (${Build.DEVICE})",
    "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    "CPU ABI" to abis,
    "CPU Cores" to "$cores cores",
    "RAM" to ramStr,
    "OpenGL ES" to glesVersion,
    "mpv Renderer Build" to
      if (BuildConfig.MPV_SUPPORTS_VULKAN) {
        "OpenGL + Vulkan"
      } else {
        "OpenGL only (non-Vulkan)"
      },
    "Vulkan" to vulkanStr,
    "GPU Hardware" to (Build.HARDWARE.ifBlank { "Unknown" }),
    "Board" to Build.BOARD,
    "Kernel" to System.getProperty("os.version", "Unknown"),
  )
}
