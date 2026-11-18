/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.preferences

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import app.video.player.presentation.components.AppTextField as TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.video.player.R
import app.video.player.domain.lyrics.LyricsProvider
import app.video.player.preferences.AudioChannels
import app.video.player.preferences.AudioPlayerOrientation
import app.video.player.preferences.AudioPreferences
import app.video.player.preferences.BrowserPreferences
import app.video.player.preferences.LyricsTranslationDisplayMode
import app.video.player.preferences.MediaLibraryType
import app.video.player.preferences.SpatialAudioMode
import app.video.player.utils.media.MediaLibraryEvents
import app.video.player.ui.player.PlaybackSession
import androidx.compose.runtime.collectAsState
import app.video.player.preferences.preference.collectAsState
import app.video.player.presentation.Screen
import app.video.player.ui.components.IconSwitch
import app.video.player.ui.icons.Icon
import app.video.player.ui.icons.Icons
import app.video.player.ui.preferences.components.SwitchPreference
import app.video.player.ui.utils.LocalBackStack
import app.video.player.ui.utils.currentMpvConfigOverrideOptions
import app.video.player.ui.utils.popSafely
import kotlinx.serialization.Serializable
import app.video.player.ui.preferences.components.AppListPreference as ListPreference
import app.video.player.ui.preferences.components.AppSliderPreference as SliderPreference
import app.video.player.ui.preferences.components.AppTextFieldPreference as TextFieldPreference
import kotlin.math.roundToInt
import org.koin.compose.koinInject
import app.video.player.ui.browser.music.MusicTab

@Serializable
object AudioPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val resources = LocalResources.current
    val backstack = LocalBackStack.current
    val playerPanel = LocalPlayerSettingsPanel.current
    val preferences = koinInject<AudioPreferences>()
    val browserPreferences = koinInject<BrowserPreferences>()
    val configOwnedOptions = currentMpvConfigOverrideOptions()

    Scaffold(
      contentWindowInsets = settingsContentWindowInsets(),
      topBar = {
        SettingsTopAppBar(
          title = stringResource(R.string.pref_audio),
          onBack = { backstack.popSafely() },
        )
      },
    ) { padding ->
      ProvideExpressivePreferenceLocals {
        val (settingsListState, settingsHighlight) =
          rememberSettingsSearchList(AudioPreferencesScreen, MaterialTheme.colorScheme.primary)
        LazyColumn(
          state = settingsListState,
          modifier =
            Modifier
              .fillMaxSize()
              .padding(padding)
              .then(settingsHighlight),
        ) {
          if (!playerPanel) {
            item {
              PreferenceSectionHeader(
                position = PreferenceSectionPosition.First,
                title = stringResource(R.string.pref_media_library_section),
                modifier = Modifier.settingsSearchTarget(R.string.pref_audio),
              )
            }

          item {
            PreferenceCard {
              val includeAudioBrowser by browserPreferences.includeAudioBrowser.collectAsState()
              SwitchPreference(
                modifier = Modifier.settingsSearchTarget(R.string.ui_include_audio_files),
                value = includeAudioBrowser,
                onValueChange = { enabled ->
                  browserPreferences.includeAudioBrowser.set(enabled)
                  if (!enabled) {
                    browserPreferences.mediaLibraryType.set(MediaLibraryType.Video)
                  }
                  // Files/folders use the same MediaScanOptions as the rest of the browser.
                  // Notify active browser screens immediately instead of waiting for a restart.
                  MediaLibraryEvents.notifyChanged()
                },
                title = {
                  Text(
                    androidx.compose.ui.res
                      .stringResource(app.video.player.R.string.ui_include_audio_files),
                  )
                },
                summary = {
                  Text(
                    androidx.compose.ui.res.stringResource(
                      app.video.player.R.string.ui_show_audio_files_in_the_browser,
                    ),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              // Minimum duration is a library filter, not a child setting of "Include audio files".
              // It always applies to the dedicated Music tab, and it also applies to audio shown
              // alongside videos in Files/Folders whenever that separate switch is enabled.
              PreferenceDivider()
              val minimumAudioDurationSeconds by browserPreferences.minimumAudioDurationSeconds.collectAsState()
              val maxMinimumDurationSeconds = 120f
              val minimumDurationLabel =
                when {
                  minimumAudioDurationSeconds <= 0 -> "Any duration"
                  minimumAudioDurationSeconds < 60 -> "${minimumAudioDurationSeconds}s and longer"
                  minimumAudioDurationSeconds % 60 == 0 -> "${minimumAudioDurationSeconds / 60} min and longer"
                  else ->
                    "${minimumAudioDurationSeconds / 60}m ${minimumAudioDurationSeconds % 60}s and longer"
                }

              Column(
                modifier =
                  Modifier
                    .settingsSearchTarget(R.string.ui_minimum_audio_duration)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
              ) {
                Text(
                  androidx.compose.ui.res
                    .stringResource(app.video.player.R.string.ui_minimum_audio_duration),
                  style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                  minimumDurationLabel,
                  color = MaterialTheme.colorScheme.outline,
                  style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                  value =
                    minimumAudioDurationSeconds
                      .toFloat()
                      .coerceIn(0f, maxMinimumDurationSeconds),
                  onValueChange = { minimumSeconds ->
                    browserPreferences.minimumAudioDurationSeconds.set(
                      minimumSeconds.toInt().coerceIn(0, maxMinimumDurationSeconds.toInt()),
                    )
                  },
                  onValueChangeFinished = { MediaLibraryEvents.notifyChanged() },
                  valueRange = 0f..maxMinimumDurationSeconds,
                  steps = 23,
                )
                Text(
                  text =
                    "Applies to Music and audio in Files • Included: ${
                      if (minimumAudioDurationSeconds <= 0) {
                        "0s"
                      } else {
                        minimumDurationLabel.removeSuffix(" and longer")
                      }
                    } – no limit",
                  color = MaterialTheme.colorScheme.outline,
                  style = MaterialTheme.typography.bodySmall,
                )
              }

            }
          }

          item {
            PreferenceSectionHeader(
              position = PreferenceSectionPosition.Middle,
              title = stringResource(R.string.pref_music_tabs_title),
              modifier = Modifier.settingsSearchTarget(R.string.pref_music_tabs_title),
            )
          }
          item {
            val enabledMusicTabs by preferences.enabledMusicTabs.collectAsState()
            val musicTabOrder by preferences.musicTabOrder.collectAsState()
            val tabMap = MusicTab.entries.associateBy { it.name }
            val orderedTabs = (musicTabOrder.mapNotNull(tabMap::get) + MusicTab.entries).distinct()
            ExpressivePreferenceGroup {
              ReorderableTabPreferences(
                tabs = orderedTabs.map { tab ->
                  ReorderableTabItem(
                    id = tab.name,
                    titleRes = tab.titleRes,
                    title = stringResource(tab.titleRes),
                    summary = "",
                    enabled = tab.name in enabledMusicTabs,
                  )
                },
                onEnabledChange = { id, enabled ->
                  val next = if (enabled) enabledMusicTabs + id else enabledMusicTabs - id
                  if (next.isNotEmpty()) preferences.enabledMusicTabs.set(next)
                },
                onMove = { id, destination ->
                  val next = orderedTabs.map { it.name }.toMutableList()
                  next.remove(id)
                  next.add(destination.coerceIn(0, next.size), id)
                  preferences.musicTabOrder.set(next)
                },
              )
            }
          }

          item {
            PreferenceSectionHeader(position = PreferenceSectionPosition.Middle, title = stringResource(R.string.pref_section_appearance))
          }

          item {
            PreferenceCard {
              DevicePreference(R.string.pref_audio_orientation_title) {
                val audioOrientation by preferences.audioOrientation.collectAsState()
                ExpressivePreferenceItem {
                ListPreference(
                  modifier = Modifier.settingsSearchTarget(R.string.pref_audio_orientation_title),
                  value = audioOrientation,
                  onValueChange = { preferences.audioOrientation.set(it) },
                  values = AudioPlayerOrientation.entries,
                  valueToText = { AnnotatedString(resources.getString(it.titleRes)) },
                  title = { Text(stringResource(R.string.pref_audio_orientation_title)) },
                  summary = {
                    Text(
                      stringResource(audioOrientation.titleRes),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                )
                }

                PreferenceDivider()
              }

              val audioAmbientMode by preferences.audioAmbientMode.collectAsState()
              SwitchPreference(
                modifier = Modifier.settingsSearchTarget(R.string.pref_audio_ambient_mode_title),
                value = audioAmbientMode,
                onValueChange = { preferences.audioAmbientMode.set(it) },
                title = { Text(stringResource(R.string.pref_audio_ambient_mode_title)) },
                summary = {
                  Text(
                    stringResource(R.string.pref_audio_ambient_mode_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()
              val audioWavySeekbar by preferences.audioWavySeekbar.collectAsState()
              SwitchPreference(
                modifier = Modifier.settingsSearchTarget(R.string.pref_audio_wavy_seekbar_title),
                value = audioWavySeekbar,
                onValueChange = { preferences.audioWavySeekbar.set(it) },
                title = { Text(stringResource(R.string.pref_audio_wavy_seekbar_title)) },
                summary = {
                  Text(
                    stringResource(R.string.pref_audio_wavy_seekbar_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )
            }
          }
        }

          item {
            PreferenceSectionHeader(
              position = PreferenceSectionPosition.Middle,
              title = stringResource(R.string.pref_audio),
              modifier = Modifier.settingsSearchTarget(R.string.pref_audio),
            )
          }

          item {
            PreferenceCard {
              val preferredLanguages by preferences.preferredLanguages.collectAsState()
              ExpressivePreferenceItem {
                TextFieldPreference(
                  modifier = Modifier.settingsSearchTarget(R.string.pref_preferred_languages),
                  value = preferredLanguages,
                  enabled = "alang" !in configOwnedOptions,
                  onValueChange = { preferences.preferredLanguages.set(it) },
                  textToValue = { input ->
                    input
                      .split(",")
                      .map { it.trim().lowercase() }
                      .filter { it.isNotEmpty() }
                      .joinToString(",")
                  },
                  title = { Text(stringResource(R.string.pref_preferred_languages)) },
                  summary = {
                    if (preferredLanguages.isNotBlank()) {
                      Text(
                        preferredLanguages,
                        color = MaterialTheme.colorScheme.outline,
                      )
                    } else {
                      Text(
                        stringResource(R.string.not_set_video_default),
                        color = MaterialTheme.colorScheme.outline,
                      )
                    }
                  },
                  textField = { value, onValueChange, _ ->
                    Column {
                      Text(stringResource(R.string.pref_audio_preferred_language))
                      TextField(
                        value,
                        onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                      )
                    }
                  },
                )
              }

              PreferenceDivider()
              val audioPitchCorrection by preferences.audioPitchCorrection.collectAsState()
              SwitchPreference(
                modifier = Modifier.settingsSearchTarget(R.string.pref_audio_pitch_correction_title),
                value = audioPitchCorrection,
                enabled = "audio-pitch-correction" !in configOwnedOptions,
                onValueChange = { preferences.audioPitchCorrection.set(it) },
                title = { Text(stringResource(R.string.pref_audio_pitch_correction_title)) },
                summary = {
                  Text(
                    stringResource(R.string.pref_audio_pitch_correction_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()
              val volumeNormalization by preferences.volumeNormalization.collectAsState()
              SwitchPreference(
                modifier = Modifier.settingsSearchTarget(R.string.pref_audio_volume_normalization_title),
                value = volumeNormalization,
                enabled = "af" !in configOwnedOptions,
                onValueChange = { preferences.volumeNormalization.set(it) },
                title = { Text(stringResource(R.string.pref_audio_volume_normalization_title)) },
                summary = {
                  Text(
                    stringResource(R.string.pref_audio_volume_normalization_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()
              val spatialAudio by preferences.spatialAudio.collectAsState()
              SwitchPreference(
                modifier = Modifier.settingsSearchTarget(R.string.pref_audio_spatial_title),
                value = spatialAudio,
                enabled = "af" !in configOwnedOptions,
                onValueChange = { preferences.spatialAudio.set(it) },
                title = { Text(stringResource(R.string.pref_audio_spatial_title)) },
                summary = {
                  Text(
                    stringResource(R.string.pref_audio_spatial_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )
              if (spatialAudio) {
                val spatialMode by preferences.spatialAudioMode.collectAsState()
                ListPreference(
                  modifier = Modifier.settingsSearchTarget(R.string.pref_audio_spatial_mode),
                  value = spatialMode,
                  onValueChange = { preferences.spatialAudioMode.set(it) },
                  values = SpatialAudioMode.entries,
                  enabled = "af" !in configOwnedOptions,
                  valueToText = { AnnotatedString(resources.getString(it.titleRes)) },
                  title = { Text(stringResource(R.string.pref_audio_spatial_mode)) },
                  summary = { Text(stringResource(spatialMode.summaryRes), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                )
              }

              PreferenceDivider()
              val immersiveArtwork by preferences.immersiveArtwork.collectAsState()
              SwitchPreference(
                modifier = Modifier.settingsSearchTarget(R.string.pref_audio_immersive_artwork_title),
                value = immersiveArtwork,
                onValueChange = { preferences.immersiveArtwork.set(it) },
                title = { Text(stringResource(R.string.pref_audio_immersive_artwork_title)) },
                summary = {
                  Text(
                    stringResource(R.string.pref_audio_immersive_artwork_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()
              val drcEnabled by preferences.drcEnabled.collectAsState()
              SwitchPreference(
                modifier = Modifier.settingsSearchTarget(R.string.pref_audio_drc_title),
                value = drcEnabled,
                enabled = "af" !in configOwnedOptions,
                onValueChange = { preferences.drcEnabled.set(it) },
                title = { Text(stringResource(R.string.pref_audio_drc_title)) },
                summary = {
                  Text(
                    stringResource(R.string.pref_audio_drc_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()
              val ignoreAudioFocus by preferences.ignoreAudioFocus.collectAsState()
              SwitchPreference(
                modifier = Modifier.settingsSearchTarget(R.string.pref_ignore_audio_focus_title),
                value = ignoreAudioFocus,
                onValueChange = preferences.ignoreAudioFocus::set,
                title = { Text(stringResource(R.string.pref_ignore_audio_focus_title)) },
                summary = {
                  Text(
                    stringResource(R.string.pref_ignore_audio_focus_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()
              val audioChannel by preferences.audioChannels.collectAsState()
              ExpressivePreferenceItem {
                ListPreference(
                  modifier = Modifier.settingsSearchTarget(R.string.pref_audio_channels),
                  value = audioChannel,
                  onValueChange = { preferences.audioChannels.set(it) },
                  values = AudioChannels.entries,
                  enabled = setOf("audio-channels", "af").none(configOwnedOptions::contains),
                  valueToText = { AnnotatedString(resources.getString(it.title)) },
                  title = { Text(text = stringResource(id = R.string.pref_audio_channels)) },
                  summary = {
                    Text(
                      text = stringResource(audioChannel.title),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                )
              }

              PreferenceDivider()
              val volumeBoostCap by preferences.volumeBoostCap.collectAsState()
              ExpressivePreferenceItem {
                SliderPreference(
                  modifier = Modifier.settingsSearchTarget(R.string.pref_audio_volume_boost_cap),
                  value = volumeBoostCap.toFloat(),
                  enabled = "volume-max" !in configOwnedOptions,
                  onValueChange = { preferences.volumeBoostCap.set(it.toInt()) },
                  title = { Text(stringResource(R.string.pref_audio_volume_boost_cap)) },
                  valueRange = 0f..200f,
                  summary = {
                    Text(
                      if (volumeBoostCap == 0) {
                        stringResource(R.string.generic_disabled)
                      } else {
                        volumeBoostCap.toString()
                      },
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                  onSliderValueChange = { preferences.volumeBoostCap.set(it.toInt()) },
                  sliderValue = volumeBoostCap.toFloat(),
                )
              }

              PreferenceDivider()
              val volumeStep by preferences.volumeStep.collectAsState()
              ExpressivePreferenceItem {
                SliderPreference(
                  modifier = Modifier.settingsSearchTarget(R.string.pref_audio_volume_step_title),
                  value = volumeStep.toFloat(),
                  onValueChange = { preferences.volumeStep.set(it.roundToInt().coerceIn(1, 10)) },
                  title = { Text(stringResource(R.string.pref_audio_volume_step_title)) },
                  valueRange = 1f..10f,
                  valueSteps = 8,
                  summary = {
                    Text(
                      if (volumeStep == 1) {
                        stringResource(R.string.pref_audio_volume_step_default)
                      } else {
                        stringResource(R.string.pref_audio_volume_step_summary, volumeStep)
                      },
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                  onSliderValueChange = { preferences.volumeStep.set(it.roundToInt().coerceIn(1, 10)) },
                  sliderValue = volumeStep.toFloat(),
                )
              }
            }
          }

          item {
            PreferenceSectionHeader(
              position = PreferenceSectionPosition.Middle,
              title = stringResource(R.string.pref_lyrics_sources_category),
              modifier = Modifier.settingsSearchTarget(R.string.pref_lyrics_sources_category),
            )
          }

          item {
            PreferenceCard {
              val lyricsAutoFetchOnline by preferences.lyricsAutoFetchOnline.collectAsState()
              SwitchPreference(
                modifier = Modifier.settingsSearchTarget(R.string.pref_lyrics_auto_fetch_online),
                value = lyricsAutoFetchOnline,
                onValueChange = { preferences.lyricsAutoFetchOnline.set(it) },
                title = { Text(stringResource(R.string.pref_lyrics_auto_fetch_online)) },
                summary = {
                  Text(
                    stringResource(R.string.pref_lyrics_auto_fetch_online_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              var providersExpanded by rememberSaveable { mutableStateOf(false) }
              val disabledLyricsProviders by preferences.disabledLyricsProviders.collectAsState()
              val availableProviders = LyricsProvider.available
              val activeCount = availableProviders.count { it.name !in disabledLyricsProviders }

              Row(
                modifier = Modifier
                  .settingsSearchTarget(R.string.pref_lyrics_providers_title)
                  .fillMaxWidth()
                  .clickable { providersExpanded = !providersExpanded }
                  .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  imageVector = Icons.RoundedFilled.Lyrics,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(24.dp),
                )
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  Text(
                    text = stringResource(R.string.pref_lyrics_providers_title),
                    style = MaterialTheme.typography.bodyLarge,
                  )
                  Text(
                    text = stringResource(
                      R.string.pref_lyrics_providers_summary,
                      activeCount,
                      availableProviders.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                  )
                }
                Icon(
                  imageVector = if (providersExpanded) Icons.RoundedFilled.ExpandLess else Icons.RoundedFilled.ExpandMore,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              AnimatedVisibility(visible = providersExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                  PreferenceDivider()
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    TextButton(
                      onClick = { preferences.disabledLyricsProviders.set(emptySet()) },
                      contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                      Text(stringResource(R.string.pref_lyrics_providers_enable_all), style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(
                      onClick = {
                        preferences.disabledLyricsProviders.set(availableProviders.map { it.name }.toSet())
                      },
                      contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                      Text(stringResource(R.string.pref_lyrics_providers_disable_all), style = MaterialTheme.typography.labelMedium)
                    }
                  }

                  availableProviders.forEachIndexed { index, provider ->
                    if (index > 0) {
                      PreferenceDivider()
                    }
                    val isEnabled = provider.name !in disabledLyricsProviders
                    LyricsProviderItem(
                      provider = provider,
                      enabled = isEnabled,
                      onEnabledChange = { enabled ->
                        val updated = if (enabled) {
                          disabledLyricsProviders - provider.name
                        } else {
                          disabledLyricsProviders + provider.name
                        }
                        preferences.disabledLyricsProviders.set(updated)
                      },
                    )
                  }
                }
              }

              PreferenceDivider()

              val paxsenixApiKey by preferences.paxsenixApiKey.collectAsState()
              ExpressivePreferenceItem {
                TextFieldPreference(
                  modifier = Modifier.settingsSearchTarget(R.string.pref_paxsenix_api_key),
                  value = paxsenixApiKey,
                  onValueChange = { preferences.paxsenixApiKey.set(it) },
                  textToValue = { it.trim() },
                  title = { Text(stringResource(R.string.pref_paxsenix_api_key)) },
                  summary = {
                    Text(
                      if (paxsenixApiKey.isNotBlank()) "••••••••"
                      else stringResource(R.string.pref_paxsenix_api_key_summary),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                  textField = { value, onValueChange, _ ->
                    Column {
                      Text(stringResource(R.string.pref_paxsenix_api_key_dialog_desc))
                      TextField(
                        value,
                        onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                      )
                    }
                  },
                )
              }
            }
          }

          item {
            PreferenceSectionHeader(
              position = PreferenceSectionPosition.Last,
              title = stringResource(R.string.pref_lyrics_translation_category),
              modifier = Modifier.settingsSearchTarget(R.string.pref_lyrics_translation_category),
            )
          }

          item {
            PreferenceCard {
              val lyricsAutoTranslate by preferences.lyricsAutoTranslate.collectAsState()
              SwitchPreference(
                modifier = Modifier.settingsSearchTarget(R.string.pref_lyrics_auto_translate),
                value = lyricsAutoTranslate,
                onValueChange = { preferences.lyricsAutoTranslate.set(it) },
                title = { Text(stringResource(R.string.pref_lyrics_auto_translate)) },
                summary = {
                  Text(
                    stringResource(R.string.pref_lyrics_auto_translate_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              if (lyricsAutoTranslate) {
                PreferenceDivider()

                val lyricsTargetLanguage by preferences.lyricsTargetLanguage.collectAsState()
                ExpressivePreferenceItem {
                  TextFieldPreference(
                    modifier = Modifier.settingsSearchTarget(R.string.pref_lyrics_target_language),
                    value = lyricsTargetLanguage,
                    onValueChange = { preferences.lyricsTargetLanguage.set(it) },
                    textToValue = { it.trim().lowercase() },
                    title = { Text(stringResource(R.string.pref_lyrics_target_language)) },
                    summary = {
                      Text(
                        lyricsTargetLanguage.uppercase(),
                        color = MaterialTheme.colorScheme.outline,
                      )
                    },
                    textField = { value, onValueChange, _ ->
                      Column {
                        Text(stringResource(R.string.pref_lyrics_target_language))
                        TextField(
                          value,
                          onValueChange,
                          modifier = Modifier.fillMaxWidth(),
                        )
                      }
                    },
                  )
                }

                PreferenceDivider()

                val lyricsTranslationDisplayMode by preferences.lyricsTranslationDisplayMode.collectAsState()
                ExpressivePreferenceItem {
                  ListPreference(
                    modifier = Modifier.settingsSearchTarget(R.string.pref_lyrics_display_mode),
                    value = lyricsTranslationDisplayMode,
                    onValueChange = { preferences.lyricsTranslationDisplayMode.set(it) },
                    values = LyricsTranslationDisplayMode.entries,
                    valueToText = { AnnotatedString(resources.getString(it.title)) },
                    title = { Text(stringResource(R.string.pref_lyrics_display_mode)) },
                    summary = {
                      Text(
                        stringResource(lyricsTranslationDisplayMode.title),
                        color = MaterialTheme.colorScheme.outline,
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
}

@Composable
private fun LyricsProviderItem(
  provider: LyricsProvider,
  enabled: Boolean,
  onEnabledChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable { onEnabledChange(!enabled) }
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = provider.label,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.SemiBold,
          color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = if (provider.wordSynced) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (enabled) 1f else 0.5f)
          } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 1f else 0.5f)
          },
        ) {
          Text(
            text = stringResource(
              if (provider.wordSynced) R.string.pref_lyrics_word_synced
              else R.string.pref_lyrics_line_synced,
            ),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (provider.wordSynced) {
              MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (enabled) 1f else 0.6f)
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.6f)
            },
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
      }

      Text(
        text = provider.detail,
        style = MaterialTheme.typography.bodySmall,
        color = if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
      )
    }

    IconSwitch(
      checked = enabled,
      onCheckedChange = onEnabledChange,
    )
  }
}
