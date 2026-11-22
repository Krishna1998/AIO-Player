/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.preferences

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.video.player.R
import app.video.player.preferences.AppearancePreferences
import app.video.player.preferences.preference.collectAsState
import app.video.player.presentation.Screen
import app.video.player.ui.components.AppSlider
import app.video.player.ui.icons.Icon
import app.video.player.ui.icons.Icons
import app.video.player.ui.preferences.ExpressivePreferenceGroup
import app.video.player.ui.preferences.ExpressivePreferenceItem
import app.video.player.ui.preferences.PreferenceCard
import app.video.player.ui.preferences.PreferenceDivider
import app.video.player.ui.preferences.PreferenceSectionHeader
import app.video.player.ui.preferences.PreferenceSectionPosition
import app.video.player.ui.preferences.SettingsTopAppBar
import app.video.player.ui.preferences.components.RestartRequiredDialog
import app.video.player.ui.theme.WallpaperPreset
import app.video.player.ui.theme.WallpaperScaleMode
import app.video.player.ui.theme.saveWallpaperCopy
import app.video.player.ui.utils.LocalBackStack
import app.video.player.ui.utils.popSafely
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

object WallpaperPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = koinInject<AppearancePreferences>()

    val wallpaperUri by preferences.customWallpaperUri.collectAsState()
    val wallpaperBlur by preferences.customWallpaperBlur.collectAsState()
    val wallpaperAlpha by preferences.customWallpaperAlpha.collectAsState()
    val wallpaperScaleMode by preferences.customWallpaperScaleMode.collectAsState()

    var showRestartDialog by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
      if (uri != null) {
        scope.launch {
          val savedPath = saveWallpaperCopy(context, uri.toString())
          preferences.customWallpaperUri.set(savedPath)
          showRestartDialog = true
        }
      }
    }

    if (showRestartDialog) {
      RestartRequiredDialog(
        title = stringResource(R.string.pref_appearance_interface_restart_title),
        message = stringResource(R.string.pref_appearance_interface_restart_message),
        confirmLabel = stringResource(R.string.pref_appearance_ui_scale_restart_now),
        onDismiss = { showRestartDialog = false },
        onConfirm = {
          showRestartDialog = false
          (context as? Activity)?.recreate()
        },
      )
    }

    Scaffold(
      topBar = {
        SettingsTopAppBar(
          title = stringResource(R.string.pref_appearance_custom_wallpaper_title),
          onBack = { backstack.popSafely() },
        )
      },
    ) { innerPadding ->
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // 1. Presets Selector Section
        item {
          PreferenceSectionHeader(
            position = PreferenceSectionPosition.First,
            title = stringResource(R.string.wallpaper_presets_label),
          )
        }

        item {
          PreferenceCard {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Text(
                text = "Choose an atmospheric backdrop or upload your own image:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                // Preset Items
                items(WallpaperPreset.entries) { preset ->
                  val isSelected = if (preset == WallpaperPreset.None) {
                    wallpaperUri.isBlank() || wallpaperUri == "preset:none"
                  } else {
                    wallpaperUri == "preset:${preset.id}"
                  }

                  Surface(
                    modifier = Modifier
                      .size(width = 110.dp, height = 110.dp)
                      .clip(RoundedCornerShape(18.dp))
                      .clickable {
                        val target = if (preset == WallpaperPreset.None) "" else "preset:${preset.id}"
                        if (target != wallpaperUri) {
                          preferences.customWallpaperUri.set(target)
                          showRestartDialog = true
                        }
                      },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(
                      2.dp,
                      if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    ),
                  ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                      if (preset.gradientColors.isNotEmpty()) {
                        Box(
                          modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(preset.gradientColors)),
                        )
                      } else {
                        Box(
                          modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                          contentAlignment = Alignment.Center,
                        ) {
                          Text("🚫", fontSize = 24.sp)
                        }
                      }

                      Surface(
                        modifier = Modifier
                          .align(Alignment.BottomCenter)
                          .fillMaxWidth(),
                        color = Color.Black.copy(alpha = 0.65f),
                      ) {
                        Text(
                          text = preset.displayName,
                          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                          color = Color.White,
                          maxLines = 1,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        )
                      }
                    }
                  }
                }

                // Custom Image Tile
                item {
                  val isCustomActive = wallpaperUri.isNotBlank() && !wallpaperUri.startsWith("preset:")
                  Surface(
                    modifier = Modifier
                      .size(width = 110.dp, height = 110.dp)
                      .clip(RoundedCornerShape(18.dp))
                      .clickable { imagePicker.launch("image/*") },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(
                      2.dp,
                      if (isCustomActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                    ),
                  ) {
                    Box(
                      modifier = Modifier.fillMaxSize(),
                      contentAlignment = Alignment.Center,
                    ) {
                      Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                      ) {
                        Text("📁", fontSize = 22.sp)
                        Text(
                          text = stringResource(R.string.wallpaper_preset_custom),
                          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }

        // 2. Adjustments & Tuning (Active when any wallpaper is chosen)
        val isAnyWallpaperChosen = wallpaperUri.isNotBlank() && wallpaperUri != "preset:none"
        if (isAnyWallpaperChosen) {
          item {
            PreferenceSectionHeader(
              position = PreferenceSectionPosition.Middle,
              title = stringResource(R.string.pref_appearance_custom_wallpaper_crop_title),
            )
          }

          item {
            ExpressivePreferenceGroup {
              val itemCount = 3

              // Blur Slider
              ExpressivePreferenceItem {
                Column(modifier = Modifier.padding(16.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = stringResource(R.string.pref_appearance_custom_wallpaper_blur),
                      style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                      text = "${wallpaperBlur.roundToInt()} dp",
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                  AppSlider(
                    value = wallpaperBlur,
                    onValueChange = { preferences.customWallpaperBlur.set(it) },
                    valueRange = 0f..25f,
                    modifier = Modifier.padding(top = 8.dp),
                  )
                }
              }

              // Dimming / Darkness (Transparency)
              ExpressivePreferenceItem {
                Column(modifier = Modifier.padding(16.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "Wallpaper brightness / opacity",
                      style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                      text = "${(wallpaperAlpha * 100).roundToInt()}%",
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                  AppSlider(
                    value = wallpaperAlpha,
                    onValueChange = { preferences.customWallpaperAlpha.set(it) },
                    valueRange = 0.2f..1f,
                    modifier = Modifier.padding(top = 8.dp),
                  )
                }
              }

              // Scale Mode (Fit / Fill)
              ExpressivePreferenceItem {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "Scale mode",
                    style = MaterialTheme.typography.titleMedium,
                  )

                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = if (wallpaperScaleMode == WallpaperScaleMode.Fill) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                      modifier = Modifier.clickable { preferences.customWallpaperScaleMode.set(WallpaperScaleMode.Fill) },
                    ) {
                      Text(
                        text = stringResource(R.string.pref_appearance_custom_wallpaper_fill),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (wallpaperScaleMode == WallpaperScaleMode.Fill) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                      )
                    }

                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = if (wallpaperScaleMode == WallpaperScaleMode.Fit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                      modifier = Modifier.clickable { preferences.customWallpaperScaleMode.set(WallpaperScaleMode.Fit) },
                    ) {
                      Text(
                        text = stringResource(R.string.pref_appearance_custom_wallpaper_fit),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (wallpaperScaleMode == WallpaperScaleMode.Fit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

