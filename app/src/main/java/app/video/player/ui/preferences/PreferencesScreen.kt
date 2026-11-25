/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.preferences

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.rememberNavBackStack
import app.video.player.R
import app.video.player.preferences.AppearancePreferences
import app.video.player.preferences.DecoderPreferences
import app.video.player.preferences.SecureFolderPreferences
import app.video.player.preferences.preference.collectAsState
import app.video.player.presentation.Screen
import app.video.player.ui.browser.LocalNavigationBarHeight
import app.video.player.ui.browser.profile.ProfileAvatar
import app.video.player.ui.browser.profile.ProfileScreen
import app.video.player.ui.icons.AppIcon
import app.video.player.ui.icons.Icon
import app.video.player.ui.icons.Icons
import app.video.player.ui.player.controls.components.tvFocusGroup
import app.video.player.ui.player.controls.components.tvFocusHighlight
import app.video.player.ui.securefolder.SecureFolderSettingsScreen
import app.video.player.ui.theme.LocalAppWallpaperActive
import app.video.player.ui.theme.LocalEmphasizedTypography
import app.video.player.ui.theme.wallpaperAwareBackgroundColor
import app.video.player.ui.tv.TvFocusMemory
import app.video.player.ui.tv.isTelevision
import app.video.player.ui.tv.tryRequestFocus
import app.video.player.ui.tv.tvContentFocus
import app.video.player.ui.tv.tvFocusMemory
import app.video.player.ui.utils.LocalBackStack
import app.video.player.ui.utils.LocalShowSettingsBackArrow
import app.video.player.ui.utils.ScreenNavDisplay
import app.video.player.ui.utils.navigateTo
import app.video.player.ui.utils.popSafely
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

private data class SettingsDestination(
  val title: String,
  val summary: String,
  val icon: AppIcon,
  val screen: Screen,
  val gradient: List<Color> = listOf(Color(0xFF00E5FF), Color(0xFF0091EA)),
  val experimental: Boolean = false,
)

private data class SettingsSection(
  val title: String,
  val tint: Color,
  val items: List<SettingsDestination>,
)

@Serializable
object PreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val colorScheme = MaterialTheme.colorScheme
    val television = isTelevision()
    val sections = settingsSections().mapNotNull { section ->
      val items = section.items.filter { !television || TvSettingsPolicy.supportsScreen(it.screen) }
      section.copy(items = items).takeIf { items.isNotEmpty() }
    }

    val configuration = LocalConfiguration.current
    val useTwoPane = television || configuration.screenWidthDp >= 600
    val categoryPaneFraction = if (!television && configuration.screenWidthDp < 840) 0.5f else 0.4f

    if (useTwoPane) {
      @Suppress("UNCHECKED_CAST")
      val detailBackstack = rememberNavBackStack(AppearancePreferencesScreen) as NavBackStack<Screen>
      val selectedScreen = detailBackstack.first()
      val categoryFocusRequester = remember { FocusRequester() }
      var detailFocusRequested by remember { mutableIntStateOf(0) }
      var detailPaneFocused by remember { mutableStateOf(false) }
      var detailPaneActivated by remember { mutableStateOf(false) }
      BackHandler(enabled = television && detailPaneFocused && detailBackstack.size == 1) {
        if (!TvFocusMemory.restore(PreferencesScreen)) categoryFocusRequester.tryRequestFocus()
      }
      Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(categoryPaneFraction).tvFocusGroup()) {
          SettingsPane(
            sections = sections,
            selectedScreen = selectedScreen,
            modifier = Modifier.focusRequester(categoryFocusRequester),
            onScreenSelected = { screen ->
              if (screen != selectedScreen || (television && detailBackstack.size > 1)) {
                Snapshot.withMutableSnapshot {
                  detailBackstack[0] = screen
                  while (detailBackstack.size > 1) detailBackstack.removeAt(detailBackstack.lastIndex)
                }
              }
              if (television) {
                detailPaneActivated = true
                detailFocusRequested++
              }
            },
          )
        }
        VerticalDivider(
          modifier = Modifier.fillMaxHeight(),
          color = colorScheme.outlineVariant.copy(alpha = 0.5f),
          thickness = 1.dp,
        )
        Box(
          modifier = Modifier
            .weight(1f - categoryPaneFraction)
            .onFocusChanged { focus ->
              detailPaneFocused = focus.hasFocus
              if (focus.hasFocus) detailPaneActivated = true
            }
            .tvFocusGroup()
        ) {
          CompositionLocalProvider(LocalBackStack provides detailBackstack) {
            ScreenNavDisplay(
              backStack = detailBackstack,
              modifier = Modifier.fillMaxSize(),
              tvAutoFocus = !television || detailPaneActivated,
              tvFocusRequestKey = detailFocusRequested,
            ) { screen ->
              CompositionLocalProvider(LocalShowSettingsBackArrow provides (screen != selectedScreen)) {
                screen.Content()
              }
            }
          }
        }
      }
    } else {
      SettingsPane(
        sections = sections,
        selectedScreen = null,
        onScreenSelected = { backstack.navigateTo(it) },
      )
    }
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  private fun SettingsPane(
    sections: List<SettingsSection>,
    selectedScreen: Screen?,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier,
  ) {
    val backstack = LocalBackStack.current
    val colorScheme = MaterialTheme.colorScheme
    val emphasizedTypography = LocalEmphasizedTypography.current
    val wallpaperActive = LocalAppWallpaperActive.current

    val appearancePreferences = koinInject<AppearancePreferences>()
    val decoderPreferences = koinInject<DecoderPreferences>()
    val secureFolderPreferences = koinInject<SecureFolderPreferences>()

    val profileName by appearancePreferences.profileName.collectAsState()
    val liquidGlassEnabled by appearancePreferences.liquidGlassEnabled.collectAsState()
    val appTheme by appearancePreferences.appTheme.collectAsState()
    val tryHWDecoding by decoderPreferences.tryHWDecoding.collectAsState()
    val isSecureFolderEntryHidden by secureFolderPreferences.isEntryPointHidden.collectAsState()

    val themeBadge = remember(liquidGlassEnabled, appTheme) {
      if (liquidGlassEnabled) "Liquid Glass • ${appTheme.name}" else appTheme.name
    }

    Scaffold(
      containerColor = wallpaperAwareBackgroundColor(),
      topBar = {
        TopAppBar(
          colors = if (wallpaperActive) {
            androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
          } else androidx.compose.material3.TopAppBarDefaults.topAppBarColors(),
          title = {
            Text(
              text = stringResource(R.string.pref_preferences),
              style = emphasizedTypography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
              ),
            )
          },
          navigationIcon = {
            IconButton(onClick = { backstack.popSafely() }) {
              Icon(
                Icons.RoundedFilled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = colorScheme.secondary,
              )
            }
          },
          actions = {
            IconButton(onClick = { backstack.navigateTo(SettingsSearchScreen) }) {
              Icon(
                imageVector = Icons.RoundedFilled.Search,
                contentDescription = stringResource(R.string.generic_search),
                tint = colorScheme.onSurfaceVariant,
              )
            }
          },
        )
      },
      bottomBar = {
        // Floating bottom search pill for effortless one-handed thumb reach
        val miniPlayerClearance = LocalNavigationBarHeight.current
        val animatedBottomPadding by animateDpAsState(
          targetValue = 12.dp + miniPlayerClearance,
          label = "settings_search_bar_mini_player_clearance",
        )
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = animatedBottomPadding)
            .navigationBarsPadding(),
          contentAlignment = Alignment.Center,
        ) {
          Surface(
            modifier = Modifier
              .widthIn(max = 520.dp)
              .fillMaxWidth()
              .height(48.dp)
              .clip(RoundedCornerShape(24.dp))
              .clickable { backstack.navigateTo(SettingsSearchScreen) },
            shape = RoundedCornerShape(24.dp),
            color = if (wallpaperActive) {
              colorScheme.surfaceContainerHigh.copy(alpha = 0.76f)
            } else {
              colorScheme.surfaceContainerHigh
            },
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(
              width = 1.dp,
              color = colorScheme.outlineVariant.copy(alpha = 0.40f),
            ),
          ) {
            Row(
              modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Icon(
                Icons.RoundedFilled.Search,
                contentDescription = stringResource(R.string.generic_search),
                tint = colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = stringResource(R.string.settings_search_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      },
    ) { padding ->
      LazyColumn(
        modifier = modifier
          .fillMaxSize()
          .tvContentFocus()
          .padding(padding),
        contentPadding = PaddingValues(
          start = 16.dp,
          end = 16.dp,
          top = 10.dp,
          bottom = 80.dp + LocalNavigationBarHeight.current,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // 1. Aurora Hub: Profile & Stats Card
        item(key = "aurora_hub_hero_profile") {
          AuroraProfileHeroCard(
            profileName = profileName,
            themeBadge = themeBadge,
            onClick = { onScreenSelected(ProfileScreen) },
          )
        }

        // 2. Aurora Hub: 2x2 Quick-Look Action Grid
        item(key = "aurora_hub_quick_grid") {
          AuroraHubQuickGrid(
            onAppearanceClick = { onScreenSelected(AppearancePreferencesScreen) },
            onEngineClick = { onScreenSelected(DecoderPreferencesScreen) },
            onControlsClick = { onScreenSelected(PlayerControlsPreferencesScreen) },
            onVaultClick = {
              if (!isSecureFolderEntryHidden) {
                onScreenSelected(SecureFolderSettingsScreen)
              } else {
                onScreenSelected(FoldersPreferencesScreen)
              }
            },
            tryHwDecoding = tryHWDecoding,
            isSecureFolderHidden = isSecureFolderEntryHidden,
          )
        }

        // 3. Aurora Hub: Categorized Hubs
        items(sections, key = { it.title }) { section ->
          AuroraHubGroupedBlock(
            section = section,
            selectedScreen = selectedScreen,
            onItemClick = { destination -> onScreenSelected(destination.screen) },
          )
        }
      }
    }
  }

  @Composable
  private fun settingsSections(): List<SettingsSection> {
    val colorScheme = MaterialTheme.colorScheme
    val television = isTelevision()
    val secureFolderPreferences = koinInject<SecureFolderPreferences>()
    val isSecureFolderEntryHidden by secureFolderPreferences.isEntryPointHidden.collectAsState()

    return listOf(
      SettingsSection(
        title = stringResource(R.string.pref_section_appearance_theme),
        tint = colorScheme.primary,
        items = listOf(
          SettingsDestination(
            title = stringResource(R.string.pref_appearance_category_liquid_glass),
            summary = listOf(
              stringResource(R.string.pref_liquid_glass_optics),
              stringResource(R.string.pref_liquid_glass_colors),
              stringResource(R.string.pref_liquid_glass_highlights),
            ).joinToString(", "),
            icon = Icons.RoundedFilled.BlurOn,
            screen = LiquidGlassPreferencesScreen,
            gradient = listOf(Color(0xFF7C4DFF), Color(0xFF651FFF)),
          ),
        ),
      ),
      SettingsSection(
        title = stringResource(R.string.pref_section_player_gestures),
        tint = colorScheme.secondary,
        items = listOf(
          SettingsDestination(
            title = stringResource(R.string.pref_player),
            summary = listOf(
              stringResource(R.string.pref_section_general),
              stringResource(R.string.pref_player_seeking_title),
              stringResource(R.string.pref_section_display_controls),
            ).joinToString(", "),
            icon = Icons.RoundedFilled.Slideshow,
            screen = PlayerPreferencesScreen,
            gradient = listOf(Color(0xFF00E5FF), Color(0xFF00B0FF)),
          ),
          SettingsDestination(
            title = stringResource(R.string.pref_gesture),
            summary = stringResource(R.string.pref_gesture_summary),
            icon = Icons.RoundedFilled.Gesture,
            screen = GesturePreferencesScreen,
            gradient = listOf(Color(0xFF7C4DFF), Color(0xFF512DA8)),
          ),
          SettingsDestination(
            title = stringResource(R.string.pref_subtitles),
            summary = stringResource(R.string.pref_subtitles_summary),
            icon = Icons.RoundedFilled.Subtitles,
            screen = SubtitlesPreferencesScreen,
            gradient = listOf(Color(0xFFFF5252), Color(0xFFD50000)),
          ),
        ),
      ),
      SettingsSection(
        title = stringResource(R.string.pref_section_engine_codecs),
        tint = colorScheme.tertiary,
        items = listOf(
          SettingsDestination(
            title = stringResource(R.string.pref_audio),
            summary = stringResource(R.string.pref_audio_summary),
            icon = Icons.RoundedFilled.Audiotrack,
            screen = AudioPreferencesScreen,
            gradient = listOf(Color(0xFF00B0FF), Color(0xFF2979FF)),
          ),
          SettingsDestination(
            title = stringResource(R.string.pref_codecs_title),
            summary = stringResource(R.string.pref_codecs_summary),
            icon = Icons.RoundedFilled.Memory,
            screen = CodecCapabilitiesScreen,
            gradient = listOf(Color(0xFFFFAB00), Color(0xFFFF6D00)),
          ),
        ),
      ),
      SettingsSection(
        title = stringResource(R.string.pref_section_library_storage),
        tint = colorScheme.primary,
        items = listOfNotNull(
          SettingsDestination(
            title = stringResource(R.string.pref_folders_title),
            summary = stringResource(R.string.pref_folders_summary),
            icon = Icons.RoundedFilled.Folder,
            screen = FoldersPreferencesScreen,
            gradient = listOf(Color(0xFF2979FF), Color(0xFF1565C0)),
          ),
          SettingsDestination(
            title = stringResource(R.string.pref_advanced),
            summary = listOf(
              stringResource(R.string.pref_section_backup_restore),
              stringResource(R.string.pref_section_mpv_config),
            ).joinToString(", "),
            icon = Icons.Alternatives.AdvancedSettings,
            screen = AdvancedPreferencesScreen,
            gradient = listOf(Color(0xFFB388FF), Color(0xFF7C4DFF)),
          ),
        ),
      ),
      SettingsSection(
        title = stringResource(R.string.pref_section_about),
        tint = colorScheme.onSurfaceVariant,
        items = listOf(
          SettingsDestination(
            title = stringResource(R.string.pref_section_updates),
            summary = stringResource(R.string.ui_check_for_updates_now),
            icon = Icons.RoundedFilled.Refresh,
            screen = UpdatePreferencesScreen,
            gradient = listOf(Color(0xFF1DE9B6), Color(0xFF00BFA5)),
          ),
          SettingsDestination(
            title = stringResource(R.string.pref_about_title),
            summary = stringResource(R.string.pref_about_summary),
            icon = Icons.RoundedFilled.Info,
            screen = AboutScreen,
            gradient = listOf(Color(0xFF78909C), Color(0xFF37474F)),
          ),
        ),
      ),
    )
  }
}

/**
 * Concept 1: Aurora Hero Profile & Status Hub Card
 */
@Composable
private fun AuroraProfileHeroCard(
  profileName: String,
  themeBadge: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .tvFocusMemory(ProfileScreen)
      .tvFocusHighlight(RoundedCornerShape(26.dp), focusedScale = 1.02f)
      .clip(RoundedCornerShape(26.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(26.dp),
    color = colorScheme.surfaceContainerHigh.copy(alpha = 0.70f),
    tonalElevation = 2.dp,
    shadowElevation = 4.dp,
    border = BorderStroke(
      width = 1.dp,
      brush = Brush.linearGradient(
        colors = listOf(
          colorScheme.primary.copy(alpha = 0.50f),
          colorScheme.tertiary.copy(alpha = 0.30f),
          Color.Transparent,
        ),
      ),
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        modifier = Modifier
          .size(54.dp)
          .clip(CircleShape)
          .background(
            Brush.linearGradient(
              listOf(
                colorScheme.primary.copy(alpha = 0.35f),
                colorScheme.tertiary.copy(alpha = 0.35f),
              ),
            ),
          )
          .padding(2.5.dp),
        contentAlignment = Alignment.Center,
      ) {
        ProfileAvatar(
          size = 50.dp,
          tint = colorScheme.primary,
          contentDescription = null,
        )
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = profileName.ifBlank { stringResource(R.string.ui_profile) },
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
          ),
          color = colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Text(
          text = stringResource(R.string.profile_edit),
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.sp,
          ),
          color = colorScheme.onSurfaceVariant,
        )
      }

      Surface(
        shape = CircleShape,
        color = colorScheme.surfaceContainerHighest.copy(alpha = 0.60f),
        modifier = Modifier.size(32.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.RoundedFilled.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

/**
 * Aurora Hub: 2x2 Quick-Look Action Grid
 */
@Composable
private fun AuroraHubQuickGrid(
  onAppearanceClick: () -> Unit,
  onEngineClick: () -> Unit,
  onControlsClick: () -> Unit,
  onVaultClick: () -> Unit,
  tryHwDecoding: Boolean,
  isSecureFolderHidden: Boolean,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Max),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Tile 1: Appearance & Theme
      AuroraHubTile(
        title = stringResource(R.string.pref_appearance_title),
        subtitle = "Optics & Theme",
        statusBadge = "ACTIVE",
        badgeColor = colorScheme.primary,
        icon = Icons.RoundedFilled.Palette,
        iconGradient = listOf(Color(0xFF00E5FF), Color(0xFF0091EA)),
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight(),
        onClick = onAppearanceClick,
        bottomContent = {
          Row(
            modifier = Modifier.padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(colorScheme.primary))
            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(colorScheme.tertiary))
            Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(colorScheme.secondary))
          }
        },
      )

      // Tile 2: Video Engine / Decoder
      AuroraHubTile(
        title = stringResource(R.string.pref_decoder),
        subtitle = if (tryHwDecoding) "mpv • HW+ Direct" else "mpv • Software",
        statusBadge = if (tryHwDecoding) "HW+" else "SW",
        badgeColor = Color(0xFF00E676),
        icon = Icons.RoundedFilled.DeveloperBoard,
        iconGradient = listOf(Color(0xFF00E676), Color(0xFF00B0FF)),
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight(),
        onClick = onEngineClick,
      )
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Max),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      // Tile 3: Controls & Gestures
      AuroraHubTile(
        title = stringResource(R.string.pref_layout_title),
        subtitle = stringResource(R.string.pref_gesture),
        statusBadge = "CUSTOM",
        badgeColor = Color(0xFFFF4081),
        icon = Icons.RoundedFilled.GridView,
        iconGradient = listOf(Color(0xFFFF4081), Color(0xFFC51162)),
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight(),
        onClick = onControlsClick,
      )

      // Tile 4: Secure Folder Vault (or Media Folders)
      if (!isSecureFolderHidden) {
        AuroraHubTile(
          title = stringResource(R.string.secure_folder_settings_title),
          subtitle = "Biometric Vault",
          statusBadge = "VAULT",
          badgeColor = Color(0xFFFFB300),
          icon = Icons.RoundedFilled.Lock,
          iconGradient = listOf(Color(0xFFFFB300), Color(0xFFFF6D00)),
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
          onClick = onVaultClick,
        )
      } else {
        AuroraHubTile(
          title = stringResource(R.string.pref_folders_title),
          subtitle = "Storage Folders",
          statusBadge = "STORAGE",
          badgeColor = Color(0xFF2979FF),
          icon = Icons.RoundedFilled.Folder,
          iconGradient = listOf(Color(0xFF2979FF), Color(0xFF1565C0)),
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
          onClick = onVaultClick,
        )
      }
    }
  }
}

/**
 * Aurora Hub: Individual Action Tile
 */
@Composable
private fun AuroraHubTile(
  title: String,
  subtitle: String,
  statusBadge: String,
  badgeColor: Color,
  icon: AppIcon,
  iconGradient: List<Color>,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  bottomContent: (@Composable () -> Unit)? = null,
) {
  val colorScheme = MaterialTheme.colorScheme

  Surface(
    modifier = modifier
      .tvFocusHighlight(RoundedCornerShape(20.dp), focusedScale = 1.02f)
      .clip(RoundedCornerShape(20.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(20.dp),
    color = colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.10f)),
    tonalElevation = 1.dp,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          modifier = Modifier.size(42.dp),
          shape = RoundedCornerShape(12.dp),
          color = Color.White.copy(alpha = 0.08f),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              modifier = Modifier.size(28.dp),
              tint = colorScheme.onSurface,
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(999.dp),
          color = badgeColor.copy(alpha = 0.16f),
          border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.40f)),
        ) {
          Text(
            text = statusBadge,
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.ExtraBold,
              fontSize = 9.sp,
              letterSpacing = 0.5.sp,
            ),
            color = badgeColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
          ),
          color = colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.sp,
          ),
          color = colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      if (bottomContent != null) {
        bottomContent()
      }
    }
  }
}

/**
 * Aurora Hub: Grouped Container
 */
@Composable
private fun AuroraHubGroupedBlock(
  section: SettingsSection,
  selectedScreen: Screen?,
  onItemClick: (SettingsDestination) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Text(
      text = section.title.uppercase(),
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp,
      ),
      color = colorScheme.primary.copy(alpha = 0.85f),
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
    )

    Surface(
      shape = RoundedCornerShape(22.dp),
      color = colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
      border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.25f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        section.items.forEachIndexed { index, destination ->
          if (index > 0) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 64.dp)
                .height(0.6.dp)
                .background(Color.White.copy(alpha = 0.06f))
            )
          }

          AuroraDestinationRow(
            destination = destination,
            isSelected = selectedScreen == destination.screen,
            onClick = { onItemClick(destination) },
          )
        }
      }
    }
  }
}

/**
 * Concept 1: Destination Row with Vibrant Squircle Icon Pod
 */
@Composable
private fun AuroraDestinationRow(
  destination: SettingsDestination,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colorScheme = MaterialTheme.colorScheme
  val rowColor = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.25f) else Color.Transparent

  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(rowColor)
      .tvFocusMemory(destination.screen)
      .tvFocusHighlight(RoundedCornerShape(0.dp), focusedScale = 1.01f)
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Surface(
      modifier = Modifier.size(38.dp),
      shape = RoundedCornerShape(12.dp),
      color = Color.White.copy(alpha = 0.08f),
      border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = destination.icon,
          contentDescription = null,
          modifier = Modifier.size(26.dp),
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }
    }

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = destination.title,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
          ),
          color = if (isSelected) colorScheme.primary else colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        if (destination.experimental) {
          Surface(
            color = colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(50.dp),
          ) {
            Text(
              text = stringResource(R.string.pref_experimental_badge),
              style = MaterialTheme.typography.labelSmall,
              color = colorScheme.onTertiaryContainer,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }
      }

      Text(
        text = destination.summary,
        style = MaterialTheme.typography.bodySmall.copy(
          fontSize = 11.sp,
        ),
        color = colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Icon(
      imageVector = Icons.RoundedFilled.ChevronRight,
      contentDescription = null,
      modifier = Modifier.size(18.dp),
      tint = colorScheme.onSurfaceVariant.copy(alpha = 0.40f),
    )
  }
}
