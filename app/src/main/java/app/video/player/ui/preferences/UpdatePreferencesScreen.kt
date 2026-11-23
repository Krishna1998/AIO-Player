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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.video.player.BuildConfig
import app.video.player.R
import app.video.player.domain.update.AppUpdateChannel
import app.video.player.domain.update.Asset
import app.video.player.domain.update.Release
import app.video.player.presentation.Screen
import app.video.player.ui.icons.AppIcon
import app.video.player.ui.icons.Icon
import app.video.player.ui.icons.Icons
import app.video.player.ui.preferences.PreferenceCard
import app.video.player.ui.preferences.PreferenceDivider
import app.video.player.ui.preferences.components.SwitchPreference
import app.video.player.ui.theme.wallpaperAwareBackgroundColor
import app.video.player.ui.update.UpdateUiState
import app.video.player.ui.update.UpdateViewModel
import app.video.player.ui.update.formatDate
import app.video.player.ui.update.formatFileSize
import app.video.player.ui.utils.LocalBackStack
import app.video.player.ui.utils.LocalUpdateViewModel
import app.video.player.ui.utils.popSafely
import com.mikepenz.markdown.m3.Markdown
import kotlinx.serialization.Serializable

@Serializable
object UpdatePreferencesScreen : Screen {

  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val updateViewModel = LocalUpdateViewModel.current

    val isAutoUpdate = updateViewModel?.isAutoUpdateEnabled?.collectAsState()?.value ?: false
    val isAutoInstall = updateViewModel?.isAutoInstallEnabled?.collectAsState()?.value ?: false
    val updateChannel = updateViewModel?.updateChannel?.collectAsState()?.value ?: AppUpdateChannel.STABLE
    val updateState = updateViewModel?.uiState?.collectAsState()?.value ?: UpdateUiState()

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
          contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // 1. Futuristic Hero Radar Status Card
          item(key = "hero_radar_card") {
            UpdateHeroStatusCard(
              updateState = updateState,
              channel = updateChannel,
              onCheckNow = { updateViewModel?.checkForUpdate(manual = true) },
              onDownload = { updateViewModel?.downloadUpdate() },
              onCancelDownload = { updateViewModel?.cancelDownload() },
              onInstall = { updateViewModel?.installUpdate() },
              onRetry = { updateViewModel?.retryCheck() },
            )
          }

          // 2. Changelog / Release Notes Bento Card (when update available)
          if (updateState.release != null && updateState.asset != null) {
            item(key = "changelog_card") {
              AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
              ) {
                ReleaseChangelogBentoCard(
                  release = updateState.release,
                  asset = updateState.asset,
                )
              }
            }
          }

          // 3. Update Channel Section (Permanently Stable)
          item(key = "channel_selector_section") {
            Column(modifier = Modifier.fillMaxWidth()) {
              SectionHeaderLabel(stringResource(R.string.ui_update_channel))
              Spacer(Modifier.height(8.dp))
              PermanentStableChannelCard()
            }
          }

          // 4. Automation & Network Safety Suite
          item(key = "automation_section") {
            Column(modifier = Modifier.fillMaxWidth()) {
              SectionHeaderLabel(stringResource(R.string.ui_auto_check_for_updates))
              Spacer(Modifier.height(8.dp))
              PreferenceCard {
                SwitchPreference(
                  value = isAutoUpdate,
                  onValueChange = { updateViewModel?.toggleAutoUpdate(it) },
                  title = { Text(stringResource(R.string.ui_auto_check_for_updates)) },
                  summary = {
                    Text(
                      text = stringResource(R.string.ui_check_on_startup),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                  icon = {
                    Icon(
                      imageVector = Icons.RoundedFilled.Refresh,
                      contentDescription = null,
                    )
                  },
                )

                PreferenceDivider()

                SwitchPreference(
                  value = isAutoInstall,
                  onValueChange = { updateViewModel?.toggleAutoInstall(it) },
                  title = { Text(stringResource(R.string.update_auto_install)) },
                  summary = {
                    Text(
                      text = stringResource(R.string.update_auto_install_summary),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  },
                  icon = {
                    Icon(
                      imageVector = Icons.RoundedFilled.Download,
                      contentDescription = null,
                    )
                  },
                )
              }
            }
          }

          // 5. System Build Footer
          item(key = "system_footer") {
            SystemBuildFooterInfo(channel = updateChannel)
          }
        }
      }
    }
  }
}

// ═══════════════════════════════════════════════════════════
// HERO RADAR STATUS CARD
// ═══════════════════════════════════════════════════════════

@Composable
private fun UpdateHeroStatusCard(
  updateState: UpdateUiState,
  channel: AppUpdateChannel,
  onCheckNow: () -> Unit,
  onDownload: () -> Unit,
  onCancelDownload: () -> Unit,
  onInstall: () -> Unit,
  onRetry: () -> Unit,
) {
  val cs = MaterialTheme.colorScheme
  val release = updateState.release
  val asset = updateState.asset

  val isChecking = updateState.checking
  val isDownloading = updateState.isDownloading
  val isReady = updateState.readyToInstall
  val hasUpdate = release != null && asset != null
  val isError = updateState.checkError || updateState.downloadError || updateState.installError

  // Orbital Radar Animation
  val infiniteTransition = rememberInfiniteTransition(label = "updateRadar")
  val radarSpinOuter by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "outerRadarSpin",
  )
  val radarSpinInner by infiniteTransition.animateFloat(
    initialValue = 360f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "innerRadarSpin",
  )

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(
      containerColor = cs.surfaceContainerHigh.copy(alpha = 0.85f),
    ),
    border = BorderStroke(
      width = 1.dp,
      color = when {
        isReady -> cs.primary.copy(alpha = 0.6f)
        hasUpdate -> cs.primary.copy(alpha = 0.4f)
        isChecking -> cs.tertiary.copy(alpha = 0.4f)
        isError -> cs.error.copy(alpha = 0.4f)
        else -> cs.outlineVariant.copy(alpha = 0.3f)
      },
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 28.dp, horizontal = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Orbital Radar Graphic
      Box(
        modifier = Modifier
          .size(96.dp)
          .padding(bottom = 6.dp),
        contentAlignment = Alignment.Center,
      ) {
        // Outer Radar Ring
        Box(
          modifier = Modifier
            .size(96.dp)
            .rotate(if (isChecking) radarSpinOuter else 0f)
            .border(
              width = 1.5.dp,
              brush = if (isChecking) {
                Brush.sweepGradient(listOf(cs.primary, cs.tertiary, Color.Transparent))
              } else {
                Brush.linearGradient(listOf(cs.outlineVariant.copy(alpha = 0.2f), cs.outlineVariant.copy(alpha = 0.1f)))
              },
              shape = CircleShape,
            ),
        )

        // Inner Radar Ring
        Box(
          modifier = Modifier
            .size(76.dp)
            .rotate(if (isChecking) radarSpinInner else 0f)
            .border(
              width = 1.dp,
              brush = if (isChecking) {
                Brush.sweepGradient(listOf(Color.Transparent, cs.primary.copy(alpha = 0.8f)))
              } else {
                Brush.linearGradient(listOf(cs.outlineVariant.copy(alpha = 0.15f), Color.Transparent))
              },
              shape = CircleShape,
            ),
        )

        // Core Badge
        Box(
          modifier = Modifier
            .size(56.dp)
            .background(
              color = when {
                isReady -> cs.primaryContainer
                hasUpdate -> cs.primaryContainer
                isChecking -> cs.secondaryContainer
                isError -> cs.errorContainer
                else -> cs.surfaceContainerHighest
              },
              shape = CircleShape,
            ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = when {
              isReady -> Icons.RoundedFilled.SystemUpdate
              hasUpdate -> Icons.RoundedFilled.Download
              isChecking -> Icons.RoundedFilled.Refresh
              isError -> Icons.RoundedFilled.Warning
              else -> Icons.RoundedFilled.CheckCircle
            },
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = when {
              isReady -> cs.onPrimaryContainer
              hasUpdate -> cs.onPrimaryContainer
              isChecking -> cs.onSecondaryContainer
              isError -> cs.onErrorContainer
              else -> cs.primary
            },
          )
        }
      }

      Spacer(Modifier.height(12.dp))

      // Status Title
      Text(
        text = when {
          isReady -> stringResource(R.string.update_ready_to_install)
          isDownloading -> stringResource(R.string.ui_downloading) + "…"
          hasUpdate -> "Horizon ${release.tagName} Available"
          isChecking -> stringResource(R.string.ui_check_for_updates_now) + "…"
          isError -> stringResource(R.string.update_check_failed)
          else -> stringResource(R.string.ui_already_using_latest_version)
        },
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
      )

      Spacer(Modifier.height(4.dp))

      // Status Subtitle
      Text(
        text = when {
          isReady -> "Verified APK ready. Tap below to finish installation."
          isDownloading -> "${asset?.let { formatFileSize(it.size) }.orEmpty()} · Verified SHA-256 integrity"
          hasUpdate -> "${asset?.let { formatFileSize(it.size) }.orEmpty()} · Published ${formatDate(release.publishedAt)}"
          isChecking -> "Querying latest release tags for arm64-v8a architecture…"
          isError -> "Could not reach update server. Check your connection."
          else -> "You are on the latest official build with all performance optimizations."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = cs.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 8.dp),
      )

      Spacer(Modifier.height(14.dp))

      // Version Pill Chip
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = cs.surfaceContainerHighest.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.25f)),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .background(
                color = if (hasUpdate || isReady) cs.primary else cs.tertiary,
                shape = CircleShape,
              ),
          )
          Text(
            text = if (hasUpdate) {
              "v${BuildConfig.VERSION_NAME} → ${release.tagName}"
            } else {
              "v${BuildConfig.VERSION_NAME} · ${channel.name.lowercase().replaceFirstChar { it.uppercase() }}"
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
          )
        }
      }

      Spacer(Modifier.height(18.dp))

      // Action Area
      if (isDownloading) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Downloading update…",
              style = MaterialTheme.typography.labelMedium,
              color = cs.onSurfaceVariant,
            )
            if (updateState.progress >= 0f) {
              Text(
                text = "${updateState.progress.toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = cs.primary,
              )
            }
          }
          Spacer(Modifier.height(8.dp))
          if (updateState.progress >= 0f) {
            LinearProgressIndicator(
              progress = { (updateState.progress / 100f).coerceIn(0f, 1f) },
              modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
            )
          } else {
            LinearProgressIndicator(
              modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
            )
          }
          Spacer(Modifier.height(10.dp))
          TextButton(onClick = onCancelDownload) {
            Text(stringResource(R.string.generic_cancel))
          }
        }
      } else if (isReady) {
        Button(
          onClick = onInstall,
          modifier = Modifier.fillMaxWidth().height(52.dp),
          shape = RoundedCornerShape(999.dp),
        ) {
          Icon(imageVector = Icons.RoundedFilled.SystemUpdate, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(stringResource(R.string.ui_install), fontWeight = FontWeight.Bold)
        }
      } else if (hasUpdate) {
        Button(
          onClick = onDownload,
          modifier = Modifier.fillMaxWidth().height(52.dp),
          shape = RoundedCornerShape(999.dp),
        ) {
          Icon(imageVector = Icons.RoundedFilled.Download, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(
            text = "Download & Install (${formatFileSize(asset.size)})",
            fontWeight = FontWeight.Bold,
          )
        }
      } else if (isError) {
        Button(
          onClick = onRetry,
          modifier = Modifier.fillMaxWidth().height(52.dp),
          shape = RoundedCornerShape(999.dp),
        ) {
          Icon(imageVector = Icons.RoundedFilled.Refresh, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(stringResource(R.string.ui_retry), fontWeight = FontWeight.Bold)
        }
      } else {
        FilledTonalButton(
          onClick = onCheckNow,
          enabled = !isChecking,
          modifier = Modifier.fillMaxWidth().height(50.dp),
          shape = RoundedCornerShape(999.dp),
        ) {
          Icon(imageVector = Icons.RoundedFilled.Refresh, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.ui_check_for_updates_now),
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}

// ═══════════════════════════════════════════════════════════
// CHANGELOG / RELEASE DETAILS BENTO CARD
// ═══════════════════════════════════════════════════════════

@Composable
private fun ReleaseChangelogBentoCard(
  release: Release,
  asset: Asset,
) {
  val cs = MaterialTheme.colorScheme

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow),
    border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.25f)),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.RoundedFilled.AutoAwesome,
            contentDescription = null,
            tint = cs.primary,
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = "What's New in ${release.tagName}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = cs.primaryContainer.copy(alpha = 0.4f),
        ) {
          Text(
            text = asset.variantLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      HorizontalDivider(Modifier.padding(vertical = 14.dp))

      Markdown(
        content = release.body.ifBlank { stringResource(R.string.update_no_release_notes) },
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

// ═══════════════════════════════════════════════════════════
// PERMANENT STABLE CHANNEL CARD
// ═══════════════════════════════════════════════════════════

@Composable
private fun PermanentStableChannelCard() {
  val cs = MaterialTheme.colorScheme

  Surface(
    shape = RoundedCornerShape(22.dp),
    color = cs.surfaceContainerLow,
    border = BorderStroke(1.5.dp, cs.primary.copy(alpha = 0.5f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Box(
        modifier = Modifier
          .size(46.dp)
          .background(cs.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.RoundedFilled.Verified,
          contentDescription = null,
          modifier = Modifier.size(24.dp),
          tint = cs.onPrimaryContainer,
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "Stable Channel",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = cs.onSurface,
          )
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = cs.primary.copy(alpha = 0.15f),
          ) {
            Text(
              text = "PERMANENT",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.ExtraBold,
              color = cs.primary,
              letterSpacing = 0.5.sp,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }
        Spacer(Modifier.height(2.dp))
        Text(
          text = "Permanently selected. Receives official, verified milestone releases from GitHub.",
          style = MaterialTheme.typography.bodySmall,
          color = cs.outline,
        )
      }

      Box(
        modifier = Modifier
          .size(24.dp)
          .background(cs.primary, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.RoundedFilled.CheckCircle,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = cs.onPrimary,
        )
      }
    }
  }
}

// ═══════════════════════════════════════════════════════════
// HELPER COMPONENTS
// ═══════════════════════════════════════════════════════════

@Composable
private fun SectionHeaderLabel(text: String) {
  Text(
    text = text.uppercase(),
    style = MaterialTheme.typography.labelSmall,
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.2.sp,
    color = MaterialTheme.colorScheme.outline,
    modifier = Modifier.padding(start = 6.dp),
  )
}

@Composable
private fun SystemBuildFooterInfo(channel: AppUpdateChannel) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = "Horizon Player v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(2.dp))
    Text(
      text = "${channel.name.lowercase().replaceFirstChar { it.uppercase() }} Channel · arm64-v8a · Release Key",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.outline,
    )
  }
}
