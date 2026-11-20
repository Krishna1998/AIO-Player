/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.ui.player.visualizer

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import app.video.player.ui.theme.AppTheme

/**
 * Dynamically resolves 3-tier One UI 9 wavy visualizer colors based on active [AppTheme] and [ColorScheme]:
 * - Wave 1: Bass Sub-glow (deep ambient anchor, lightened for dark/AMOLED contrast)
 * - Wave 2: Mid Harmonic (vibrant energetic body)
 * - Wave 3: Treble Crest (luminescent lead crest with specular highlight)
 */
fun resolveWaveformColors(
  appTheme: AppTheme,
  colorScheme: ColorScheme,
): Triple<Color, Color, Color> {
  return when (appTheme) {
    AppTheme.BentoFusionAura -> Triple(
      Color(0xFF581C87), // Deep royal ultraviolet sub-bass glow
      Color(0xFF00B4D8), // Luminous electric azure harmonic body
      Color(0xFF00F5FF), // Specular prism cyan treble crest
    )
    AppTheme.StealthAcidLime -> Triple(
      Color(0xFF047857), // Luminous emerald pine sub-glow (lighter & vibrant in dark/AMOLED)
      Color(0xFF10B981), // Emerald mint harmonic
      Color(0xFFD4FF00), // Acid Lime crest
    )
    AppTheme.ElectricCyan -> Triple(
      Color(0xFF0284C7), // Luminous ocean blue (lighter in dark/AMOLED)
      Color(0xFF00B4D8), // Vibrant cyan
      Color(0xFF00F0FF), // Electric cyan crest
    )
    AppTheme.HyperCrimson -> Triple(
      Color(0xFF9F1239), // Luminous rose wine (lighter in dark/AMOLED)
      Color(0xFFF43F5E), // Rose coral
      Color(0xFFFF1744), // Hyper Crimson crest
    )
    AppTheme.CyberMagenta -> Triple(
      Color(0xFF9333EA), // Luminous neon orchid (lighter in dark/AMOLED)
      Color(0xFFE040FB), // Neon orchid
      Color(0xFFFF007F), // Cyber magenta crest
    )
    AppTheme.QuantumViolet -> Triple(
      Color(0xFF6D28D9), // Luminous royal violet (lighter in dark/AMOLED)
      Color(0xFF7C4DFF), // Purple harmonic
      Color(0xFFB388FF), // Quantum lavender crest
    )
    AppTheme.SolarGold -> Triple(
      Color(0xFFB45309), // Warm amber bronze (lighter in dark/AMOLED)
      Color(0xFFF59E0B), // Warm amber
      Color(0xFFFFD600), // Solar gold crest
    )
    AppTheme.CyberPrism -> Triple(
      Color(0xFF4338CA), // Luminous royal indigo (lighter in dark/AMOLED)
      Color(0xFF8B5CF6), // Electric violet
      Color(0xFF00F5FF), // Prism cyan crest
    )
    AppTheme.RossoPrism,
    AppTheme.Doom -> Triple(
      Color(0xFF9F1239), // Luminous crimson wine (lighter in dark/AMOLED)
      Color(0xFFFF2A6D), // Hot pink
      Color(0xFFFF003C), // Rosso neon crest
    )
    AppTheme.AuroraMint,
    AppTheme.GreenApple,
    AppTheme.TakoGreen,
    AppTheme.Forest -> Triple(
      Color(0xFF047857), // Luminous emerald jade (lighter in dark/AMOLED)
      Color(0xFF10B981), // Emerald mint
      Color(0xFF00F5D4), // Bright neon jade crest
    )
    AppTheme.HoloOpal,
    AppTheme.Lavender -> Triple(
      Color(0xFF5B21B6), // Luminous lavender violet (lighter in dark/AMOLED)
      Color(0xFF38BDF8), // Sky blue
      Color(0xFFA855F7), // Opal purple crest
    )
    AppTheme.CottonCandy -> Triple(
      Color(0xFF86198F), // Luminous berry fuchsia (lighter in dark/AMOLED)
      Color(0xFFDEB0E9), // Orchid mist
      Color(0xFFFFB1D5), // Cotton candy pink crest
    )
    AppTheme.SolarAmber,
    AppTheme.Sunset,
    AppTheme.Amber,
    AppTheme.Cloudflare -> Triple(
      Color(0xFFC2410C), // Luminous burnt amber (lighter in dark/AMOLED)
      Color(0xFFEA580C), // Blaze orange
      Color(0xFFFDE047), // Radiant gold crest
    )
    AppTheme.CrimsonSonic -> Triple(
      Color(0xFF6B21A8), // Luminous royal violet (lighter in dark/AMOLED)
      Color(0xFFFF2A6D), // Sonic crimson
      Color(0xFF00F5FF), // Cyan spark crest
    )
    AppTheme.Nord,
    AppTheme.TokyoNight,
    AppTheme.Catppuccin,
    AppTheme.Midnight,
    AppTheme.Sapphire,
    AppTheme.Ocean,
    AppTheme.Tidal,
    AppTheme.Aurora -> Triple(
      Color(0xFF4338CA), // One UI 9 Liquid Aura: Luminous Royal Indigo (lighter & visible in dark/AMOLED)
      Color(0xFF8B5CF6), // Electric Violet harmonic
      Color(0xFF38BDF8), // Galaxy Sky Cyan crest
    )
    AppTheme.Monochrome,
    AppTheme.Slate,
    AppTheme.YinYang -> Triple(
      Color(0xFF475569), // Luminous cool steel slate (lighter in dark/AMOLED)
      Color(0xFF94A3B8), // Cool steel
      Color(0xFFF8FAFC), // Platinum white crest
    )
    AppTheme.Mocha,
    AppTheme.Gruvbox -> Triple(
      Color(0xFF6D4C41), // Luminous warm caramel mocha (lighter in dark/AMOLED)
      Color(0xFFA1887F), // Soft caramel mocha
      Color(0xFFE0BB95), // Warm latte froth crest
    )
    AppTheme.RoseGold,
    AppTheme.RosePine,
    AppTheme.Strawberry -> Triple(
      Color(0xFF9D174D), // Luminous rose berry (lighter in dark/AMOLED)
      Color(0xFFDB2777), // Vibrant rose
      Color(0xFFF472B6), // Light rose gold crest
    )
    AppTheme.Dracula -> Triple(
      Color(0xFF4D5B86), // Luminous Dracula purple-slate (lighter in dark/AMOLED)
      Color(0xFFBD93F9), // Purple harmonic
      Color(0xFF50FA7B), // Neon green crest
    )
    AppTheme.Coral -> Triple(
      Color(0xFFBE123C), // Luminous rose coral (lighter in dark/AMOLED)
      Color(0xFFF43F5E), // Rose coral
      Color(0xFFFDA4AF), // Light coral crest
    )
    AppTheme.Violet -> Triple(
      Color(0xFF6D28D9), // Luminous royal violet (lighter in dark/AMOLED)
      Color(0xFF8B5CF6), // Violet mid
      Color(0xFFD8B4FE), // Light violet crest
    )
    AppTheme.Kanagawa -> Triple(
      Color(0xFF364860), // Luminous Fuji wave slate (lighter in dark/AMOLED)
      Color(0xFF98BB6C), // Spring green
      Color(0xFF7E9CD8), // Fuji blue crest
    )
    else -> {
      // Dynamic Monet or unlisted: derive directly from runtime ColorScheme with lightened contrast
      val w3 = colorScheme.primary
      val w2 = colorScheme.tertiary
      val w1 = colorScheme.secondary
      Triple(w1, w2, w3)
    }
  }
}

