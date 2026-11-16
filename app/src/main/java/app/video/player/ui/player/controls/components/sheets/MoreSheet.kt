/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.player.controls.components.sheets

import app.video.player.ui.player.PlaybackSession

import android.content.res.Configuration
import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import app.video.player.presentation.components.AppDialogTextButton as TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.video.player.presentation.components.AppAlertDialog
import androidx.compose.ui.window.DialogProperties
import app.video.player.R
import app.video.player.domain.anime4k.Anime4KManager
import app.video.player.preferences.AdvancedPreferences
import app.video.player.preferences.preference.collectAsState
import app.video.player.presentation.components.PlayerSheet
import app.video.player.ui.icons.Icon
import app.video.player.ui.icons.Icons
import app.video.player.ui.player.anime4k.Anime4KUiState
import app.video.player.ui.theme.spacing
import org.koin.compose.koinInject

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun MoreSheet(
  remainingTime: Int,
  onStartTimer: (Int) -> Unit,
  onDismissRequest: () -> Unit,
  onEnterFiltersPanel: () -> Unit,
  onEnterLuaScriptsPanel: () -> Unit,
  onEnterEqualizerSheet: (() -> Unit)? = null,
  onEnterSettingsPanel: (() -> Unit)? = null,
  anime4KUiState: Anime4KUiState,
  onAnime4KModeSelected: (Anime4KManager.Mode) -> Unit,
  filtersEnabled: Boolean = true,
  equalizerEnabled: Boolean = true,
  anime4KEnabled: Boolean = true,
  fadeOutEnabled: Boolean = true,
  onToggleFadeOut: (Boolean) -> Unit = {},
  onCancelTimer: () -> Unit = { onStartTimer(0) },
  onExtendTimer: (Int) -> Unit = {},
  remainingTrackSeconds: Int = 0,
  modifier: Modifier = Modifier,
) {
  val advancedPreferences = koinInject<AdvancedPreferences>()
  val statisticsPage by advancedPreferences.enabledStatisticsPage.collectAsState()
  val enableLuaScripts by advancedPreferences.enableLuaScripts.collectAsState()
  val selectedLuaScripts by advancedPreferences.selectedLuaScripts.collectAsState()
  val mpvConfStorageLocation by advancedPreferences.mpvConfStorageUri.collectAsState()
  val showActionLabels = LocalConfiguration.current.orientation != Configuration.ORIENTATION_PORTRAIT

  PlayerSheet(
    onDismissRequest,
    modifier,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.player_sheets_more_title),
          style = MaterialTheme.typography.titleLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.widthIn(max = 140.dp).padding(end = 8.dp),
        )
        Row(
          modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState(), reverseScrolling = true),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.End,
        ) {
          var isSleepTimerDialogShown by remember { mutableStateOf(false) }
          TextButton(onClick = { isSleepTimerDialogShown = true }) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            ) {
              Icon(
                imageVector = Icons.RoundedFilled.Timer,
                contentDescription = stringResource(R.string.timer_title),
                tint = if (remainingTime > 0) MaterialTheme.colorScheme.primary else LocalContentColor.current,
              )
              if (showActionLabels) {
                Text(
                  text =
                    if (remainingTime == 0) {
                      stringResource(R.string.timer_title)
                    } else {
                      stringResource(
                        R.string.timer_remaining,
                        DateUtils.formatElapsedTime(remainingTime.toLong()),
                      )
                    },
                  color = if (remainingTime > 0) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                )
              }
              if (isSleepTimerDialogShown) {
                TimePickerDialog(
                  remainingTime = remainingTime,
                  onDismissRequest = { isSleepTimerDialogShown = false },
                  onTimeSelect = onStartTimer,
                  fadeOutEnabled = fadeOutEnabled,
                  onToggleFadeOut = onToggleFadeOut,
                  onCancelTimer = onCancelTimer,
                  onExtendTimer = onExtendTimer,
                  remainingTrackSeconds = remainingTrackSeconds,
                )
              }
            }
          }
          if (onEnterEqualizerSheet != null) {
            TextButton(onClick = onEnterEqualizerSheet, enabled = equalizerEnabled) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
              ) {
                Icon(
                  imageVector = Icons.RoundedFilled.Equalizer,
                  contentDescription = stringResource(id = R.string.btn_label_equalizer),
                )
                if (showActionLabels) {
                  Text(text = stringResource(id = R.string.btn_label_equalizer))
                }
              }
            }
          }
          TextButton(onClick = onEnterFiltersPanel, enabled = filtersEnabled) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            ) {
              Icon(
                imageVector = Icons.RoundedFilled.Tune,
                contentDescription = stringResource(id = R.string.player_sheets_filters_title),
              )
              if (showActionLabels) {
                Text(text = stringResource(id = R.string.player_sheets_filters_title))
              }
            }
          }
          TextButton(
            onClick = onEnterLuaScriptsPanel,
            enabled = mpvConfStorageLocation.isNotBlank(),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
            ) {
              Icon(
                imageVector = Icons.RoundedFilled.Code,
                contentDescription = "Scripts",
                tint =
                  if (enableLuaScripts && selectedLuaScripts.isNotEmpty()) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    LocalContentColor.current
                  },
              )
              if (showActionLabels) {
                Text(
                  text =
                    if (selectedLuaScripts.isEmpty()) {
                      "Scripts"
                    } else {
                      "Scripts (${selectedLuaScripts.size})"
                    },
                  color =
                    if (enableLuaScripts && selectedLuaScripts.isNotEmpty()) {
                      MaterialTheme.colorScheme.primary
                    } else {
                      LocalContentColor.current
                    },
                )
              }
            }
          }
          if (onEnterSettingsPanel != null) {
            TextButton(onClick = onEnterSettingsPanel) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
              ) {
                Icon(
                  imageVector = Icons.RoundedFilled.Settings,
                  contentDescription = stringResource(R.string.player_settings_open),
                )
                if (showActionLabels) Text(stringResource(R.string.ui_settings))
              }
            }
          }
        }
      }
      Text(
        text = stringResource(R.string.player_sheets_stats_page_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
      ) {
        items(8, key = { it }) { page ->
          FilterChip(
            label = {
              Text(
                if (page == 7) {
                  "Console"
                } else {
                  stringResource(
                    if (page == 0) {
                      R.string.player_sheets_tracks_off
                    } else {
                      R.string.player_sheets_stats_page_chip
                    },
                    page,
                  )
                },
              )
            },
            onClick = {
              val isConsoleOpen = PlaybackSession.getPropertyBoolean("user-data/mpv/console/open") == true

              // If we are choosing any page OTHER than Console, close the console if it's currently open
              if (page != 7 && isConsoleOpen) {
                PlaybackSession.command("script-message-to", "console", "disable")
              }

              when (page) {
                0 -> {
                  if (statisticsPage in 1..5) PlaybackSession.command("script-binding", "stats/display-stats-toggle")
                }
                6 -> {
                  if (statisticsPage in 1..5) PlaybackSession.command("script-binding", "stats/display-stats-toggle")
                }
                7 -> {
                  if (statisticsPage in 1..5) PlaybackSession.command("script-binding", "stats/display-stats-toggle")
                  // Enable console only if it is not already open
                  if (!isConsoleOpen) {
                    PlaybackSession.command("script-binding", "console/enable")
                  }
                }
                else -> {
                  if (statisticsPage == 0 || statisticsPage == 6 || statisticsPage == 7) {
                    PlaybackSession.command("script-binding", "stats/display-stats-toggle")
                  }
                  PlaybackSession.command("script-binding", "stats/display-page-$page")
                }
              }
              advancedPreferences.enabledStatisticsPage.set(page)
            },
            selected = statisticsPage == page,
            leadingIcon = null,
            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                  containerColor = playerSheetSelectionColor(false),
                  selectedContainerColor = playerSheetSelectionColor(true),
                  selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
          )
        }
      }


    }
  }
}

private val sleepTimerPresets = listOf(15, 30, 45, 60)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimePickerDialog(
  onDismissRequest: () -> Unit,
  onTimeSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
  remainingTime: Int = 0,
  fadeOutEnabled: Boolean = true,
  onToggleFadeOut: (Boolean) -> Unit = {},
  onCancelTimer: () -> Unit = { onTimeSelect(0) },
  onExtendTimer: (Int) -> Unit = {},
  remainingTrackSeconds: Int = 0,
) {
  val state = rememberTimePickerState(
    remainingTime / 3600,
    (remainingTime % 3600) / 60,
    is24Hour = true,
  )

  val stopTimeString = remember(remainingTime) {
    if (remainingTime > 0) {
      val stopMillis = System.currentTimeMillis() + (remainingTime * 1000L)
      val timeFormat = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, Locale.getDefault())
      timeFormat.format(Date(stopMillis))
    } else null
  }

  AppAlertDialog(
    onDismissRequest = onDismissRequest,
    modifier = modifier.widthIn(max = 380.dp),
    properties = DialogProperties(usePlatformDefaultWidth = false),
    title = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Icon(
          imageVector = Icons.RoundedFilled.Timer,
          contentDescription = null,
          tint = if (remainingTime > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text = stringResource(R.string.timer_title),
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        if (stopTimeString != null) {
          Text(
            text = stringResource(R.string.sleep_timer_stops_at, stopTimeString),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Active Timer Banner (if running)
        if (remainingTime > 0) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = stringResource(
                  R.string.timer_remaining,
                  DateUtils.formatElapsedTime(remainingTime.toLong()),
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                FilterChip(
                  selected = false,
                  onClick = { onExtendTimer(300) },
                  label = { Text(stringResource(R.string.sleep_timer_extend_5)) },
                )
                FilterChip(
                  selected = false,
                  onClick = { onExtendTimer(900) },
                  label = { Text(stringResource(R.string.sleep_timer_extend_15)) },
                )
                FilterChip(
                  selected = false,
                  onClick = {
                    onCancelTimer()
                    onDismissRequest()
                  },
                  label = { Text(stringResource(R.string.sleep_timer_turn_off)) },
                  colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    labelColor = MaterialTheme.colorScheme.error,
                  ),
                )
              }
            }
          }
        }

        // Quick Presets
        Column(
          horizontalAlignment = Alignment.Start,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(R.string.ui_quick_presets),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
          )
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            sleepTimerPresets.forEach { minutes ->
              FilterChip(
                selected = false,
                onClick = {
                  onTimeSelect(minutes * 60)
                  onDismissRequest()
                },
                label = { Text(stringResource(R.string.generic_minutes_short, minutes)) },
                leadingIcon = null,
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                  containerColor = playerSheetSelectionColor(false),
                  selectedContainerColor = playerSheetSelectionColor(true),
                  selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
              )
            }
            if (remainingTrackSeconds > 10) {
              FilterChip(
                selected = false,
                onClick = {
                  onTimeSelect(remainingTrackSeconds)
                  onDismissRequest()
                },
                label = {
                  Text(
                    stringResource(
                      R.string.sleep_timer_end_of_track
                    ) + " (${DateUtils.formatElapsedTime(remainingTrackSeconds.toLong())})"
                  )
                },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                  containerColor = playerSheetSelectionColor(false),
                  selectedContainerColor = playerSheetSelectionColor(true),
                  selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
              )
            }
          }
        }

        // Time Input
        TimeInput(state = state)

        // Gentle Audio Fade-out Toggle
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleFadeOut(!fadeOutEnabled) },
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
              Text(
                text = stringResource(R.string.sleep_timer_fade_out),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = stringResource(R.string.sleep_timer_fade_out_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Switch(
              checked = fadeOutEnabled,
              onCheckedChange = onToggleFadeOut,
            )
          }
        }
      }
    },
    dismissButton = {
      if (remainingTime > 0) {
        TextButton(onClick = {
          onCancelTimer()
          onDismissRequest()
        }) {
          Text(stringResource(id = R.string.sleep_timer_turn_off))
        }
      }
      TextButton(onClick = onDismissRequest) {
        Text(stringResource(id = R.string.generic_cancel))
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          val customSeconds = state.hour * 3600 + state.minute * 60
          if (customSeconds > 0) {
            onTimeSelect(customSeconds)
          }
          onDismissRequest()
        },
      ) {
        Text(stringResource(id = R.string.generic_ok))
      }
    },
  )
}
