/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.video.player.BuildConfig
import app.video.player.R
import app.video.player.domain.update.AppUpdateChannel
import app.video.player.presentation.Screen
import app.video.player.ui.icons.Icon
import app.video.player.ui.icons.Icons
import app.video.player.ui.preferences.components.AppPreference
import app.video.player.ui.preferences.components.PreferenceAction
import app.video.player.ui.preferences.components.PreferenceDivider
import app.video.player.ui.preferences.components.SwitchPreference
import app.video.player.ui.theme.LocalAppWallpaperActive
import app.video.player.ui.theme.wallpaperAwareBackgroundColor
import app.video.player.ui.utils.LocalBackStack
import app.video.player.ui.utils.LocalUpdateViewModel
import app.video.player.ui.utils.popSafely
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.PreferenceSectionHeader

@Serializable
object UpdatePreferencesScreen : Screen {

  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val wallpaperActive = LocalAppWallpaperActive.current
    val updateViewModel = LocalUpdateViewModel.current

    Scaffold(
      containerColor = wallpaperAwareBackgroundColor(),
      topBar = {
        SettingsTopAppBar(
          title = stringResource(R.string.pref_section_updates),
          onBack = { backstack.popSafely() },
        )
      },
    ) { padding ->
      ProvideExpressivePreferenceLocals {
        LazyColumn(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(padding),
          contentPadding = PaddingValues(bottom = 32.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          if (updateViewModel != null) {
            val isAutoUpdate by updateViewModel.isAutoUpdateEnabled.collectAsState()
            val isAutoInstall by updateViewModel.isAutoInstallEnabled.collectAsState()
            val updateChannel by updateViewModel.updateChannel.collectAsState()
            val updateState by updateViewModel.uiState.collectAsState()

            item {
              PreferenceSectionHeader(
                position = PreferenceSectionPosition.First,
                title = stringResource(R.string.ui_check_for_updates_now),
              )
            }

            item {
              PreferenceCard {
                AppPreference(
                  title = { Text(stringResource(R.string.ui_check_for_updates_now)) },
                  summary = {
                    Text(
                      text = when {
                        updateState.checking -> stringResource(R.string.ui_check_for_updates_now) + "…"
                        updateState.readyToInstall -> stringResource(R.string.update_ready_to_install)
                        updateState.release != null -> stringResource(R.string.update_available) + " (${updateState.release?.name ?: ""})"
                        else -> stringResource(R.string.ui_already_using_latest_version)
                      },
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                  icon = {
                    if (updateState.checking) {
                      CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    } else {
                      Icon(
                        imageVector = Icons.RoundedFilled.Refresh,
                        contentDescription = null,
                      )
                    }
                  },
                  action = PreferenceAction.Dialog,
                  onClick = {
                    updateViewModel.showPendingUpdate()
                  },
                )
              }
            }

            item {
              Spacer(Modifier.height(8.dp))
              PreferenceSectionHeader(
                position = PreferenceSectionPosition.Middle,
                title = stringResource(R.string.ui_auto_check_for_updates),
              )
            }

            item {
              PreferenceCard {
                SwitchPreference(
                  value = isAutoUpdate,
                  onValueChange = { updateViewModel.toggleAutoUpdate(it) },
                  title = { Text(stringResource(R.string.ui_auto_check_for_updates)) },
                  summary = {
                    Text(
                      text = stringResource(R.string.ui_check_on_startup),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                )

                PreferenceDivider()

                SwitchPreference(
                  value = isAutoInstall,
                  onValueChange = { updateViewModel.toggleAutoInstall(it) },
                  title = { Text(stringResource(R.string.update_auto_install)) },
                  summary = {
                    Text(
                      text = stringResource(R.string.update_auto_install_summary),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                )
              }
            }

            item {
              Spacer(Modifier.height(8.dp))
              PreferenceSectionHeader(
                position = PreferenceSectionPosition.Last,
                title = stringResource(R.string.ui_update_channel),
              )
            }

            item {
              PreferenceCard {
                val channelNames = listOf(
                  stringResource(R.string.ui_update_channel_stable_short),
                  stringResource(R.string.ui_update_channel_preview_short),
                  stringResource(R.string.ui_update_channel_experimental_short),
                )
                val channelValues = listOf(
                  AppUpdateChannel.STABLE.name,
                  AppUpdateChannel.PREVIEW.name,
                  AppUpdateChannel.EXPERIMENTAL.name,
                )

                AppPreference(
                  title = { Text(stringResource(R.string.ui_update_channel)) },
                  summary = {
                    Text(
                      text = when (updateChannel) {
                        AppUpdateChannel.STABLE -> stringResource(R.string.ui_update_channel_stable_short)
                        AppUpdateChannel.PREVIEW -> stringResource(R.string.ui_update_channel_preview_short)
                        AppUpdateChannel.EXPERIMENTAL -> stringResource(R.string.ui_update_channel_experimental_short)
                      } + " — " + stringResource(R.string.ui_update_channels_summary),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                  icon = {
                    Icon(
                      imageVector = Icons.RoundedFilled.Sync,
                      contentDescription = null,
                    )
                  },
                  action = PreferenceAction.Dialog,
                  onClick = {
                    // Open update sheet dialog to select channel or view updates
                    updateViewModel.showPendingUpdate()
                  },
                )
              }
            }
          } else {
            item {
              PreferenceCard {
                AppPreference(
                  title = { Text(stringResource(R.string.pref_section_updates)) },
                  summary = {
                    Text(
                      text = stringResource(R.string.ui_already_using_latest_version) + " (v${BuildConfig.VERSION_NAME})",
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                  icon = {
                    Icon(
                      imageVector = Icons.RoundedFilled.Info,
                      contentDescription = null,
                    )
                  },
                )
              }
            }
          }
        }
      }
    }
  }
}
