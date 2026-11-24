/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

@file:Suppress("DEPRECATION")

package app.video.player.ui.browser.states

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
import app.video.player.ui.utils.NavigationBackHandler as BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.sp
import app.video.player.presentation.components.AppAlertDialog as AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import app.video.player.presentation.components.AppDialogTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.video.player.BuildConfig
import app.video.player.R
import app.video.player.preferences.BrowserPreferences
import app.video.player.ui.icons.Icon
import app.video.player.ui.icons.Icons
import app.video.player.ui.player.controls.components.rememberTvInitialFocusRequester
import app.video.player.ui.player.controls.components.tvFocusHighlight
import app.video.player.ui.player.controls.components.tvInitialFocus
import app.video.player.ui.theme.AppShapeScale
import app.video.player.ui.theme.AppMotion
import app.video.player.ui.theme.expressiveDisplayFontForText
import app.video.player.utils.device.DeviceFormFactor
import app.video.player.utils.permission.PermissionRecovery
import org.koin.compose.koinInject

private fun checkFilePermission(context: Context): Boolean = PermissionRecovery.isStorageGranted(context)

private fun checkNotificationPermission(context: Context): Boolean =
  PermissionRecovery.isNotificationGranted(context)

private fun openStoragePermissionSettings(context: Context): Boolean =
  PermissionRecovery.openAllFilesAccessSettings(context)

@SuppressLint("UseKtx")
@Composable
fun PermissionDeniedState(
  onRequestPermission: () -> Unit,
  modifier: Modifier = Modifier,
  onNext: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val isTelevision = DeviceFormFactor.isTelevision(context)
  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
  val scope = androidx.compose.runtime.rememberCoroutineScope()
  var showExplanationDialog by remember { mutableStateOf(false) }

  val isPlayStoreBuild = remember { BuildConfig.SCOPED_STORAGE_ONLY }

  var isFileGranted by remember { mutableStateOf(checkFilePermission(context)) }
  var isNotificationGranted by remember { mutableStateOf(checkNotificationPermission(context)) }

  // Initial check on composition
  LaunchedEffect(Unit) {
    isFileGranted = checkFilePermission(context)
    isNotificationGranted = checkNotificationPermission(context)
  }

  // Re-check permissions whenever activity resumes from system settings or permission dialogs
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        isFileGranted = checkFilePermission(context)
        isNotificationGranted = checkNotificationPermission(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  // Launcher for notification permission prompt
  val notificationLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    isNotificationGranted = granted || checkNotificationPermission(context)
    if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val activity = context as? Activity
      if (activity != null && !activity.shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
          putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        runCatching { context.startActivity(intent) }
      }
    }
  }

  val browserPreferences = koinInject<BrowserPreferences>()

  val finishSetup: () -> Unit = {
    browserPreferences.onboardingCompleted.set(true)
    onNext?.invoke()
    if (onNext == null) {
      onRequestPermission()
    }
  }

  val primaryFocusRequester = rememberTvInitialFocusRequester(requestKey = isFileGranted)
  val explanationFocusRequester = rememberTvInitialFocusRequester(
    enabled = showExplanationDialog,
    requestKey = showExplanationDialog,
  )

  BackHandler(enabled = isTelevision && !showExplanationDialog) {
    finishSetup()
  }

  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "pulseAlpha",
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    // Ambient Aurora Glow Top Background
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(280.dp)
        .align(Alignment.TopCenter)
        .background(
          Brush.radialGradient(
            colors = listOf(
              MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
              MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
              Color.Transparent,
            ),
            radius = 650f,
          ),
        ),
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(
          horizontal = if (isTelevision) 48.dp else if (isLandscape) 32.dp else 20.dp,
          vertical = if (isTelevision) 32.dp else 24.dp,
        ),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Hero Header
      Column(
        modifier = Modifier
          .widthIn(max = 840.dp)
          .fillMaxWidth()
          .padding(top = 8.dp, bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Glowing live badge
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
          modifier = Modifier.padding(bottom = 12.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .background(
                  color = MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha),
                  shape = CircleShape,
                ),
            )
            Text(
              text = "HORIZON PLAYER ONBOARDING",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
              ),
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        Text(
          text = "Elevate Your Media Universe",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            fontFamily = expressiveDisplayFontForText("Elevate Your Media Universe"),
            letterSpacing = (-0.5).sp,
          ),
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Grant storage access to unleash ultra-smooth 4K video rendering, lossless FLAC audio & dynamic shaders.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.widthIn(max = 520.dp),
        )
      }

      // Cards Area
      if (isLandscape || isTelevision) {
        // Landscape & Tablet/TV Layout: balanced row
        Row(
          modifier = Modifier
            .widthIn(max = 880.dp)
            .fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // Primary Storage Card
          AuraPermissionCard(
            title = stringResource(R.string.ui_file_permission_title),
            description = if (isPlayStoreBuild) {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                stringResource(R.string.ui_file_permission_desc_playstore_tiramisu)
              } else {
                stringResource(R.string.ui_file_permission_desc_playstore)
              }
            } else {
              stringResource(R.string.ui_file_permission_desc_all_files)
            },
            isGranted = isFileGranted,
            statusLabel = if (isFileGranted) "GRANTED ✓" else "REQUIRED • TAP TO GRANT",
            actionLabel = if (isFileGranted) "Granted ✓" else "Grant Access",
            icon = Icons.RoundedFilled.Folder,
            detailText = "All Files & Subtitles",
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = {
              if (!isFileGranted) {
                if (isPlayStoreBuild) {
                  onRequestPermission()
                } else {
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (!openStoragePermissionSettings(context)) onRequestPermission()
                  } else {
                    onRequestPermission()
                  }
                }
              }
            },
            modifier = Modifier
              .weight(1f)
              .tvInitialFocus(primaryFocusRequester),
          )

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AuraPermissionCard(
              title = stringResource(R.string.ui_notification_permission_title),
              description = stringResource(R.string.ui_notification_permission_desc),
              isGranted = isNotificationGranted,
              statusLabel = if (isNotificationGranted) "ENABLED ✓" else "OPTIONAL",
              actionLabel = if (isNotificationGranted) "Enabled ✓" else "Enable",
              icon = Icons.RoundedFilled.Notifications,
              detailText = "Lockscreen & Controls",
              accentColor = MaterialTheme.colorScheme.secondary,
              onClick = {
                if (!isNotificationGranted) {
                  notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
              },
              modifier = Modifier.weight(1f),
            )
          }
        }
      } else {
        // Portrait Layout: Linear Column
        Column(
          modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          AuraPermissionCard(
            title = stringResource(R.string.ui_file_permission_title),
            description = if (isPlayStoreBuild) {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                stringResource(R.string.ui_file_permission_desc_playstore_tiramisu)
              } else {
                stringResource(R.string.ui_file_permission_desc_playstore)
              }
            } else {
              stringResource(R.string.ui_file_permission_desc_all_files)
            },
            isGranted = isFileGranted,
            statusLabel = if (isFileGranted) "GRANTED ✓" else "REQUIRED • TAP TO GRANT",
            actionLabel = if (isFileGranted) "Granted ✓" else "Grant Access",
            icon = Icons.RoundedFilled.Folder,
            detailText = "All Files & Subtitles",
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = {
              if (!isFileGranted) {
                if (isPlayStoreBuild) {
                  onRequestPermission()
                } else {
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (!openStoragePermissionSettings(context)) onRequestPermission()
                  } else {
                    onRequestPermission()
                  }
                }
              }
            },
            modifier = Modifier.tvInitialFocus(primaryFocusRequester),
          )

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AuraPermissionCard(
              title = stringResource(R.string.ui_notification_permission_title),
              description = stringResource(R.string.ui_notification_permission_desc),
              isGranted = isNotificationGranted,
              statusLabel = if (isNotificationGranted) "ENABLED ✓" else "OPTIONAL",
              actionLabel = if (isNotificationGranted) "Enabled ✓" else "Enable",
              icon = Icons.RoundedFilled.Notifications,
              detailText = "Lockscreen & Controls",
              accentColor = MaterialTheme.colorScheme.secondary,
              onClick = {
                if (!isNotificationGranted) {
                  notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
              },
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Bottom Launch & Action Area
      Column(
        modifier = Modifier
          .widthIn(max = if (isLandscape || isTelevision) 880.dp else 560.dp)
          .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = {
            if (isFileGranted) finishSetup()
          },
          enabled = isFileGranted,
          shape = RoundedCornerShape(18.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .tvFocusHighlight(AppShapeScale.large, enabled = isFileGranted, focusedScale = 1.04f)
            .alpha(if (isFileGranted) 1f else 0.5f),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Text(
              text = "Enter Horizon Player",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
              ),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector = Icons.RoundedFilled.ArrowForward,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = { showExplanationDialog = true },
            modifier = Modifier.tvFocusHighlight(AppShapeScale.medium, focusedScale = 1.04f),
          ) {
            Icon(
              imageVector = Icons.RoundedFilled.Info,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.ui_why_do_i_see_this),
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
            )
          }

          TextButton(
            onClick = { finishSetup() },
            modifier = Modifier.tvFocusHighlight(AppShapeScale.medium, focusedScale = 1.04f),
          ) {
            Text(
              text = "${stringResource(R.string.onboarding_skip)} ›",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
            )
          }
        }
      }
    }
  }

  // Explanation Dialog
  if (showExplanationDialog) {
    val uriHandler = LocalUriHandler.current
    val githubUrl = "https://github.com/Krishna1998/Horizon-Player"

    AlertDialog(
      onDismissRequest = { showExplanationDialog = false },
      icon = {
        Icon(
          imageVector = Icons.RoundedFilled.Info,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
      },
      title = {
        Text(
          text = stringResource(R.string.ui_why_this_permission_is_needed),
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Column(
          modifier = Modifier
            .heightIn(max = 400.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          if (isPlayStoreBuild) {
            Text(
              text = stringResource(R.string.ui_videoplayer_needs_access_to_your_video_files_to_provide_its_core_funct),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                "On Android 13 and above, this permission allows the app to read video files from your device's storage, including Downloads, Movies, and DCIM folders."
              } else {
                "This permission allows the app to read media files from your device's storage to play videos and audio."
              },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = stringResource(R.string.ui_the_permission_is_used_exclusively_for),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Medium,
            )
            Text(
              text = stringResource(R.string.ui_discovering_and_displaying_your_video_files_n_playing_media_cont),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            Text(
              text = stringResource(R.string.ui_videoplayer_has_always_required_storage_access_permission_as_it_s_esse),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = stringResource(R.string.ui_however_due_to_a_change_in_security_policy_apps_built_for_androi),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = stringResource(R.string.ui_please_know_that_this_permission_is_only_used_for_the_auto_disco),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Text(
            text = stringResource(R.string.ui_videoplayer_is_an_open_source_project_you_can_review_the_source_code_a),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          val annotatedString = buildAnnotatedString {
            pushStringAnnotation(
              tag = "URL",
              annotation = githubUrl,
            )
            withStyle(
              style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline,
              ),
            ) {
              append(githubUrl)
            }
            pop()
          }

          ClickableText(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium,
            onClick = { offset ->
              annotatedString
                .getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()
                ?.let { uriHandler.openUri(it.item) }
            },
          )

          Text(
            text = stringResource(R.string.ui_be_rest_assured_your_privacy_is_our_utmost_priority_and_we_neith),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
          )
        }
      },
      confirmButton = {
        FilledTonalButton(
          onClick = { showExplanationDialog = false },
          shape = AppShapeScale.medium,
          modifier =
            Modifier
              .tvInitialFocus(explanationFocusRequester)
              .tvFocusHighlight(AppShapeScale.medium, focusedScale = 1.04f),
        ) {
          Text(stringResource(R.string.got_it))
        }
      },
      shape = AppShapeScale.extraLarge,
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AuraPermissionCard(
  title: String,
  description: String,
  isGranted: Boolean,
  statusLabel: String,
  actionLabel: String,
  icon: app.video.player.ui.icons.AppIcon,
  detailText: String? = null,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  isGrantedColor: Color = Color(0xFF00E676),
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isTelevision = DeviceFormFactor.isTelevision(LocalContext.current)
  val containerColor by animateColorAsState(
    targetValue = if (isGranted) {
      MaterialTheme.colorScheme.surfaceContainerLowest
    } else {
      MaterialTheme.colorScheme.surfaceContainer
    },
    animationSpec = if (AppMotion.shouldReduceMotion()) snap() else AppMotion.Effect.Color,
    label = "card_container",
  )

  val borderColor = if (isGranted) {
    isGrantedColor.copy(alpha = 0.45f)
  } else {
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .border(
        width = if (isGranted) 1.2.dp else 1.dp,
        color = borderColor,
        shape = RoundedCornerShape(22.dp),
      )
      .tvFocusHighlight(
        AppShapeScale.largeIncreased,
        enabled = true,
        focusedScale = 1.025f,
      )
      .clip(RoundedCornerShape(22.dp))
      .clickable(onClick = onClick),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    shape = RoundedCornerShape(22.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Top Row: Icon squircle + Status pill badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          modifier = Modifier.size(42.dp),
          shape = RoundedCornerShape(13.dp),
          color = if (isGranted) isGrantedColor.copy(alpha = 0.14f) else accentColor.copy(alpha = 0.12f),
          border = BorderStroke(
            1.dp,
            if (isGranted) isGrantedColor.copy(alpha = 0.35f) else accentColor.copy(alpha = 0.28f),
          ),
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              modifier = Modifier.size(22.dp),
              tint = if (isGranted) isGrantedColor else accentColor,
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(999.dp),
          color = if (isGranted) isGrantedColor.copy(alpha = 0.15f) else accentColor.copy(alpha = 0.10f),
          border = BorderStroke(
            1.dp,
            if (isGranted) isGrantedColor.copy(alpha = 0.35f) else accentColor.copy(alpha = 0.25f),
          ),
        ) {
          Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp,
            ),
            color = if (isGranted) isGrantedColor else accentColor,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp),
          )
        }
      }

      // Middle: Title & Description
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = title,
          style = if (isTelevision) MaterialTheme.typography.titleLargeEmphasized else MaterialTheme.typography.titleMediumEmphasized,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 16.sp,
        )
      }

      // Bottom Row: Detail / subtext + action button pill
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (!detailText.isNullOrBlank()) {
          Text(
            text = detailText,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
          )
        } else {
          Spacer(modifier = Modifier.weight(1f))
        }

        Surface(
          shape = RoundedCornerShape(999.dp),
          color = if (isGranted) isGrantedColor.copy(alpha = 0.12f) else accentColor.copy(alpha = 0.15f),
          border = BorderStroke(
            1.dp,
            if (isGranted) isGrantedColor.copy(alpha = 0.30f) else accentColor.copy(alpha = 0.30f),
          ),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = actionLabel,
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
              ),
              color = if (isGranted) isGrantedColor else accentColor,
            )
            if (isGranted) {
              Icon(
                imageVector = Icons.RoundedFilled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = isGrantedColor,
              )
            } else {
              Icon(
                imageVector = Icons.RoundedFilled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = accentColor,
              )
            }
          }
        }
      }
    }
  }
}

/** Compact in-place prompt for screens that need storage access after onboarding was skipped. */
@Composable
fun StoragePermissionPrompt(
  onRequestPermission: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val isPlayStoreBuild = remember { BuildConfig.SCOPED_STORAGE_ONLY }
  var isFileGranted by remember { mutableStateOf(checkFilePermission(context)) }
  val lifecycleOwner = LocalLifecycleOwner.current
  val isTelevision = DeviceFormFactor.isTelevision(context)
  val grantFocusRequester =
    rememberTvInitialFocusRequester(
      enabled = !isFileGranted,
      requestKey = isFileGranted,
    )

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) isFileGranted = checkFilePermission(context)
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .widthIn(max = if (isTelevision) 560.dp else 420.dp)
        .padding(horizontal = if (isTelevision) 48.dp else 24.dp),
    ) {
      Surface(
        modifier = Modifier.size(64.dp),
        shape = AppShapeScale.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
          Icon(
            imageVector = Icons.RoundedFilled.FolderOff,
            contentDescription = null,
            modifier = Modifier.size(30.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = stringResource(R.string.storage_permission_needed_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = stringResource(R.string.storage_permission_needed_desc),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(18.dp))
      Button(
        onClick = {
          if (isFileGranted) return@Button
          if (!isPlayStoreBuild && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!openStoragePermissionSettings(context)) onRequestPermission()
          } else {
            onRequestPermission()
          }
        },
        shape = AppShapeScale.large,
        modifier =
          Modifier
            .tvInitialFocus(grantFocusRequester)
            .tvFocusHighlight(AppShapeScale.large, enabled = !isFileGranted, focusedScale = 1.04f)
            .height(48.dp),
      ) {
        Text(
          text = stringResource(R.string.storage_permission_grant),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}
