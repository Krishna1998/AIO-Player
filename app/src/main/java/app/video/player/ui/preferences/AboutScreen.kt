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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import app.video.player.BuildConfig
import app.video.player.R
import app.video.player.presentation.Screen
import app.video.player.presentation.crash.CrashActivity.Companion.collectDeviceInfo
import app.video.player.ui.components.AppToast as Toast
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
    val isWallpaperActive = LocalAppWallpaperActive.current

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
        val transition = rememberInfiniteTransition(label = "aboutAura")
        val auraPhase by transition.animateFloat(
          initialValue = 0f,
          targetValue = 1f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 6500),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "auraPhase",
        )

        LazyColumn(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(paddingValues),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // 1. Futuristic Hero Glass Banner
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
                    val cx = size.width * (0.2f + 0.6f * auraPhase)
                    val cy = size.height * (0.3f + 0.4f * (1f - auraPhase))
                    val auraGradient =
                      Brush.radialGradient(
                        colors =
                          listOf(
                            cs.primary.copy(alpha = if (isGlassActive) 0.32f else 0.22f),
                            cs.tertiary.copy(alpha = if (isGlassActive) 0.18f else 0.12f),
                            Color.Transparent,
                          ),
                        center = Offset(cx, cy),
                        radius = size.width * 0.85f,
                      )
                    onDrawBehind {
                      drawRect(brush = auraGradient)
                    }
                  }
                  .padding(24.dp),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                // App Icon with breathing glowing ring
                Box(
                  modifier =
                    Modifier
                      .size(86.dp)
                      .clip(CircleShape)
                      .background(cs.surfaceContainerHighest.copy(alpha = 0.6f))
                      .border(
                        width = 2.dp,
                        brush =
                          Brush.sweepGradient(
                            listOf(
                              cs.primary,
                              cs.tertiary,
                              cs.secondary,
                              cs.primary,
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

                Spacer(modifier = Modifier.height(16.dp))

                // App Title
                Text(
                  text = stringResource(R.string.app_name),
                  style = MaterialTheme.typography.headlineMedium,
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = (-0.5).sp,
                  color = cs.onSurface,
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Version & Channel Tag
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = cs.primary.copy(alpha = 0.14f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cs.primary.copy(alpha = 0.3f)),
                  ) {
                    Text(
                      text = "v$versionName",
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold,
                      color = cs.primary,
                      fontFamily = FontFamily.Monospace,
                    )
                  }

                  Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = cs.secondaryContainer.copy(alpha = 0.7f),
                  ) {
                    Text(
                      text = buildType.uppercase(),
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.SemiBold,
                      color = cs.onSecondaryContainer,
                    )
                  }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Developer Badge
                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color = cs.surfaceContainerHighest.copy(alpha = 0.5f),
                  border = androidx.compose.foundation.BorderStroke(0.8.dp, cs.outlineVariant.copy(alpha = 0.3f)),
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
                      tint = cs.error.copy(alpha = 0.85f),
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

                // Quick Action Bar: Open-Source Libraries & GitHub Repository
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

          // 2. Hardware Architecture & Diagnostics Card
          item {
            PreferenceSectionHeader(
              position = PreferenceSectionPosition.First,
              title = stringResource(R.string.ui_device_info),
            )
          }

          item {
            PreferenceCard {
              val deviceInfo = remember { collectDeviceInfo() }
              Row(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .clickable {
                      SafeClipboard.copyPlainText(context, "videoplayer_device_info", deviceInfo)
                      Toast.makeText(context, "Device info copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = cs.primary.copy(alpha = 0.12f),
                  modifier = Modifier.size(44.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.RoundedFilled.Info,
                      contentDescription = null,
                      tint = cs.primary,
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

          // 3. Detailed Real-time System Specs
          val systemStats = remember { collectSystemStats(context) }
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

          // 4. Footer Branding & Copyright
          item {
            Column(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .padding(vertical = 12.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                text = "Crafted for high performance playback and luxury UI",
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
