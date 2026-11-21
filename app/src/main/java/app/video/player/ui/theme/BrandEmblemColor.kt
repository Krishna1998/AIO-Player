/* SPDX-License-Identifier: AGPL-3.0-or-later */
package app.video.player.ui.theme

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import app.video.player.R

/**
 * Brand emblem accent colour choices.
 * Controls the glow/tint of the in-app brand emblem and maps to matching launcher icon styles.
 */
enum class BrandEmblemColor(
  @StringRes val labelRes: Int,
  val color: Color,
  val matchingLauncherAlias: String? = null,
) {
  SyncTheme(
    labelRes = R.string.pref_emblem_color_sync_theme,
    color = Color.Transparent, // Dynamic: resolved from MaterialTheme.colorScheme.primary at runtime
    matchingLauncherAlias = null,
  ),
  AcidLime(
    labelRes = R.string.pref_emblem_color_lime,
    color = Color(0xFFD4FF00),
    matchingLauncherAlias = "IconBlackContinuousLigatureVMLime",
  ),
  CyberCyan(
    labelRes = R.string.pref_emblem_color_cyan,
    color = Color(0xFF00F0FF),
    matchingLauncherAlias = "IconBlackQuantumAperture",
  ),
  HyperCrimson(
    labelRes = R.string.pref_emblem_color_crimson,
    color = Color(0xFFFF1744),
    matchingLauncherAlias = "IconBlackPulseRotaryDialCrimson",
  ),
  ElectricViolet(
    labelRes = R.string.pref_emblem_color_violet,
    color = Color(0xFFA855F7),
    matchingLauncherAlias = "IconBlackGradient",
  ),
  CyberMagenta(
    labelRes = R.string.pref_emblem_color_magenta,
    color = Color(0xFFFF007F),
    matchingLauncherAlias = "IconBlackSolid",
  ),
  DiamondWhite(
    labelRes = R.string.pref_emblem_color_white,
    color = Color(0xFFFFFFFF),
    matchingLauncherAlias = "IconVelvetVoidQuantumApertureWhite",
  );

  fun resolveColor(themePrimary: Color): Color =
    if (this == SyncTheme) themePrimary else color
}

