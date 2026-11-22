/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package app.video.player.ui.preferences

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import app.video.player.R
import app.video.player.ui.icons.Icon
import app.video.player.ui.icons.Icons
import app.video.player.ui.liquidglass.appGlassSurface
import app.video.player.ui.liquidglass.liquidGlassActive
import app.video.player.ui.theme.LocalAppWallpaperActive
import app.video.player.ui.utils.LocalShowSettingsBackArrow

/** The floating player panel already owns system/keyboard insets; embedded pages need only their content padding. */
@Composable
internal fun settingsContentWindowInsets(): WindowInsets =
  if (LocalPlayerSettingsPanel.current) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets

/** Standard settings-screen top bar; the back arrow follows [LocalShowSettingsBackArrow]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsTopAppBar(
  title: String,
  onBack: () -> Unit,
  titleModifier: Modifier = Modifier,
  onClose: (() -> Unit)? = null,
  windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
  if (LocalPlayerSettingsPanel.current) return
  val glassActive = liquidGlassActive()
  val wallpaperActive = LocalAppWallpaperActive.current
  TopAppBar(
    modifier = if (glassActive) {
      Modifier.appGlassSurface(
        shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        enabled = true,
      )
    } else Modifier,
    colors = if (glassActive || wallpaperActive) {
      TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
      )
    } else TopAppBarDefaults.topAppBarColors(),
    title = {
      Text(
        modifier = titleModifier,
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
      )
    },
    navigationIcon = {
      if (onClose == null && LocalShowSettingsBackArrow.current) {
        IconButton(onClick = onBack) {
          Icon(
            Icons.RoundedFilled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = MaterialTheme.colorScheme.secondary,
          )
        }
      }
    },
    actions = {
      if (onClose != null) {
        IconButton(onClick = onClose) {
          Icon(
            Icons.RoundedFilled.Close,
            contentDescription = stringResource(R.string.ui_close),
            tint = MaterialTheme.colorScheme.secondary,
          )
        }
      }
    },
    windowInsets = windowInsets,
  )
}
