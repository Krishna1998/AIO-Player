/* SPDX-License-Identifier: AGPL-3.0-or-later */
package app.video.player.ui.preferences

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import app.video.player.R

/** Backdrop of a launcher icon. Each one also needs a color in colors_app_icon.xml. */
internal enum class IconBackground(
  val key: String,
  @ColorRes val color: Int,
  @StringRes val label: Int,
) {
  Black("Black", R.color.icon_bg_black, R.string.app_icon_bg_black),
  White("White", R.color.icon_bg_white, R.string.app_icon_bg_white),
  Charcoal("Charcoal", R.color.icon_bg_charcoal, R.string.app_icon_bg_charcoal),
  Indigo("Indigo", R.color.icon_bg_indigo, R.string.app_icon_bg_indigo),
  Violet("Violet", R.color.icon_bg_violet, R.string.app_icon_bg_violet),
  Lagoon("Lagoon", R.color.icon_bg_lagoon, R.string.app_icon_bg_lagoon),
  Azure("Azure", R.color.icon_bg_azure, R.string.app_icon_bg_azure),
  Lilac("Lilac", R.color.icon_bg_lilac, R.string.app_icon_bg_lilac),
  Mint("Mint", R.color.icon_bg_mint, R.string.app_icon_bg_mint),
  Ember("Ember", R.color.icon_bg_ember, R.string.app_icon_bg_ember),
  Crimson("Crimson", R.color.icon_bg_crimson, R.string.app_icon_bg_crimson),
  Rose("Rose", R.color.icon_bg_rose, R.string.app_icon_bg_rose),
  Peach("Peach", R.color.icon_bg_peach, R.string.app_icon_bg_peach),
  Amber("Amber", R.color.icon_bg_amber, R.string.app_icon_bg_amber),
  Lemon("Lemon", R.color.icon_bg_lemon, R.string.app_icon_bg_lemon),
  Lime("Lime", R.color.icon_bg_lime, R.string.app_icon_bg_lime),
  Forest("Forest", R.color.icon_bg_forest, R.string.app_icon_bg_forest),
  Teal("Teal", R.color.icon_bg_teal, R.string.app_icon_bg_teal),
  Sky("Sky", R.color.icon_bg_sky, R.string.app_icon_bg_sky),
  Cobalt("Cobalt", R.color.icon_bg_cobalt, R.string.app_icon_bg_cobalt),
  Plum("Plum", R.color.icon_bg_plum, R.string.app_icon_bg_plum),
  Cocoa("Cocoa", R.color.icon_bg_cocoa, R.string.app_icon_bg_cocoa),
  Wallpaper("Wallpaper", R.color.icon_bg_wallpaper, R.string.app_icon_bg_wallpaper),
  VelvetVoid("VelvetVoid", R.color.icon_bg_velvet_void, R.string.app_icon_bg_velvet_void),
}

/** The ribbon drawn over a background, as a 108dp adaptive foreground layer. */
internal enum class IconMark(
  val key: String,
  @DrawableRes val layer: Int,
  @StringRes val label: Int,
) {
  Gradient("Gradient", R.drawable.ic_launcher_purple_foreground, R.string.app_icon_logo_gradient),
  StealthShutterV("StealthShutterV", R.drawable.ic_launcher_stealth_shutter_v_foreground, R.string.app_icon_logo_stealth_shutter_v),
  KiloHertzMonolith("KiloHertzMonolith", R.drawable.ic_launcher_kilo_hertz_monolith_foreground, R.string.app_icon_logo_kilo_hertz_monolith),
  QuantumAperture("QuantumAperture", R.drawable.ic_launcher_quantum_aperture_foreground, R.string.app_icon_logo_quantum_aperture),
  LiquidAcousticDroplet("LiquidAcousticDroplet", R.drawable.ic_launcher_liquid_acoustic_droplet_foreground, R.string.app_icon_logo_liquid_acoustic_droplet),
  HorizonAeroBlades("HorizonAeroBlades", R.drawable.ic_launcher_horizon_aero_blades_foreground, R.string.app_icon_logo_horizon_aero_blades),
  AuraApertureCyan("AuraApertureCyan", R.drawable.ic_launcher_aura_aperture_cyan_foreground, R.string.app_icon_logo_aura_aperture_cyan),
  EclipseHaloCyan("EclipseHaloCyan", R.drawable.ic_launcher_eclipse_halo_cyan_foreground, R.string.app_icon_logo_eclipse_halo_cyan),
  EclipseHaloLime("EclipseHaloLime", R.drawable.ic_launcher_eclipse_halo_lime_foreground, R.string.app_icon_logo_eclipse_halo_lime),
  SupersonicSplitArrow("SupersonicSplitArrow", R.drawable.ic_launcher_supersonic_split_arrow_foreground, R.string.app_icon_logo_supersonic_split_arrow),
  SupersonicSplitArrowLime("SupersonicSplitArrowLime", R.drawable.ic_launcher_supersonic_split_arrow_lime_foreground, R.string.app_icon_logo_supersonic_split_arrow_lime),
  PulseRotaryDialLime("PulseRotaryDialLime", R.drawable.ic_launcher_pulse_rotary_dial_lime_foreground, R.string.app_icon_logo_pulse_rotary_dial_lime),
  PulseRotaryDialCrimson("PulseRotaryDialCrimson", R.drawable.ic_launcher_pulse_rotary_dial_crimson_foreground, R.string.app_icon_logo_pulse_rotary_dial_crimson),
  ContinuousLigatureVMLime("ContinuousLigatureVMLime", R.drawable.ic_launcher_continuous_ligature_vm_lime_foreground, R.string.app_icon_logo_continuous_ligature_vm_lime),
  QuantumApertureLime("QuantumApertureLime", R.drawable.ic_launcher_quantum_aperture_lime_foreground, R.string.app_icon_logo_quantum_aperture_lime),
  QuantumApertureWhite("QuantumApertureWhite", R.drawable.ic_launcher_quantum_aperture_white_foreground, R.string.app_icon_logo_quantum_aperture_white),
  QuantumApertureViolet("QuantumApertureViolet", R.drawable.ic_launcher_quantum_aperture_violet_foreground, R.string.app_icon_logo_quantum_aperture_violet),
}

/**
 * One launcher icon: a background with a mark over it. Only pairs that read well together exist, so
 * the catalog below is both the list of icons and the rule for which marks suit which backgrounds.
 * A new pair needs a row here, a mipmap, a splash style and an activity-alias named [aliasName] in the manifest.
 */
internal class LauncherIcon(
  val background: IconBackground,
  val mark: IconMark,
  @DrawableRes val preview: Int,
  @StyleRes val splashTheme: Int,
) {
  val aliasName: String get() = "app.video.player.Icon${background.key}${mark.key}"

  override fun equals(other: Any?) = other is LauncherIcon && other.background == background && other.mark == mark
  override fun hashCode() = background.hashCode() * 31 + mark.hashCode()

  companion object {
    val entries: List<LauncherIcon> = listOf(
      LauncherIcon(IconBackground.Black, IconMark.Gradient, R.mipmap.ic_launcher_black_gradient, R.style.Theme_videoplayer_Splash_BlackGradient),
      LauncherIcon(IconBackground.Black, IconMark.StealthShutterV, R.mipmap.ic_launcher_black_stealth_shutter_v, R.style.Theme_videoplayer_Splash_BlackStealthShutterV),
      LauncherIcon(IconBackground.Black, IconMark.KiloHertzMonolith, R.mipmap.ic_launcher_black_kilo_hertz_monolith, R.style.Theme_videoplayer_Splash_BlackKiloHertzMonolith),
      LauncherIcon(IconBackground.Black, IconMark.QuantumAperture, R.mipmap.ic_launcher_black_quantum_aperture, R.style.Theme_videoplayer_Splash_BlackQuantumAperture),
      LauncherIcon(IconBackground.Black, IconMark.LiquidAcousticDroplet, R.mipmap.ic_launcher_black_liquid_acoustic_droplet, R.style.Theme_videoplayer_Splash_BlackLiquidAcousticDroplet),
      LauncherIcon(IconBackground.Black, IconMark.HorizonAeroBlades, R.mipmap.ic_launcher_black_horizon_aero_blades, R.style.Theme_videoplayer_Splash_BlackHorizonAeroBlades),
      LauncherIcon(IconBackground.Black, IconMark.AuraApertureCyan, R.mipmap.ic_launcher_black_aura_aperture_cyan, R.style.Theme_videoplayer_Splash_BlackAuraApertureCyan),
      LauncherIcon(IconBackground.Black, IconMark.EclipseHaloCyan, R.mipmap.ic_launcher_black_eclipse_halo_cyan, R.style.Theme_videoplayer_Splash_BlackEclipseHaloCyan),
      LauncherIcon(IconBackground.Black, IconMark.EclipseHaloLime, R.mipmap.ic_launcher_black_eclipse_halo_lime, R.style.Theme_videoplayer_Splash_BlackEclipseHaloLime),
      LauncherIcon(IconBackground.Black, IconMark.SupersonicSplitArrow, R.mipmap.ic_launcher_black_supersonic_split_arrow, R.style.Theme_videoplayer_Splash_BlackSupersonicSplitArrow),
      LauncherIcon(IconBackground.Black, IconMark.SupersonicSplitArrowLime, R.mipmap.ic_launcher_black_supersonic_split_arrow_lime, R.style.Theme_videoplayer_Splash_BlackSupersonicSplitArrowLime),
      LauncherIcon(IconBackground.Black, IconMark.PulseRotaryDialLime, R.mipmap.ic_launcher_black_pulse_rotary_dial_lime, R.style.Theme_videoplayer_Splash_BlackPulseRotaryDialLime),
      LauncherIcon(IconBackground.Black, IconMark.PulseRotaryDialCrimson, R.mipmap.ic_launcher_black_pulse_rotary_dial_crimson, R.style.Theme_videoplayer_Splash_BlackPulseRotaryDialCrimson),
      LauncherIcon(IconBackground.Black, IconMark.ContinuousLigatureVMLime, R.mipmap.ic_launcher_black_continuous_ligature_vm_lime, R.style.Theme_videoplayer_Splash_BlackContinuousLigatureVMLime),
      LauncherIcon(IconBackground.Black, IconMark.QuantumApertureLime, R.mipmap.ic_launcher_black_quantum_aperture_lime, R.style.Theme_videoplayer_Splash_BlackQuantumApertureLime),
      LauncherIcon(IconBackground.VelvetVoid, IconMark.QuantumApertureViolet, R.mipmap.ic_launcher_velvet_void_quantum_aperture_violet, R.style.Theme_videoplayer_Splash_VelvetVoidQuantumApertureViolet),
      LauncherIcon(IconBackground.VelvetVoid, IconMark.QuantumApertureWhite, R.mipmap.ic_launcher_velvet_void_quantum_aperture_white, R.style.Theme_videoplayer_Splash_VelvetVoidQuantumApertureWhite),
    )

    val allKnownAliases: List<String> = listOf(
      "app.video.player.IconBlackGradient",
      "app.video.player.IconBlackSolid",
      "app.video.player.IconBlackStealthShutterV",
      "app.video.player.IconBlackKiloHertzMonolith",
      "app.video.player.IconBlackQuantumAperture",
      "app.video.player.IconBlackLiquidAcousticDroplet",
      "app.video.player.IconBlackHorizonAeroBlades",
      "app.video.player.IconBlackAuraApertureCyan",
      "app.video.player.IconBlackEclipseHaloCyan",
      "app.video.player.IconBlackEclipseHaloLime",
      "app.video.player.IconBlackSupersonicSplitArrow",
      "app.video.player.IconBlackSupersonicSplitArrowLime",
      "app.video.player.IconBlackPulseRotaryDialLime",
      "app.video.player.IconBlackPulseRotaryDialCrimson",
      "app.video.player.IconBlackContinuousLigatureVMLime",
      "app.video.player.IconBlackQuantumApertureLime",
      "app.video.player.IconVelvetVoidQuantumApertureViolet",
      "app.video.player.IconVelvetVoidQuantumApertureWhite",
      "app.video.player.IconBlackWhite",
      "app.video.player.IconWhiteSolid",
      "app.video.player.IconWhiteGradient",
      "app.video.player.IconWhiteBlack",
      "app.video.player.IconCharcoalSolid",
      "app.video.player.IconCharcoalGradient",
      "app.video.player.IconCharcoalWhite",
      "app.video.player.IconIndigoSolid",
      "app.video.player.IconIndigoWhite",
      "app.video.player.IconVioletSolid",
      "app.video.player.IconVioletWhite",
      "app.video.player.IconLagoonWhite",
      "app.video.player.IconLagoonBlack",
      "app.video.player.IconAzureSolid",
      "app.video.player.IconAzureGradient",
      "app.video.player.IconAzureBlack",
      "app.video.player.IconLilacSolid",
      "app.video.player.IconLilacGradient",
      "app.video.player.IconLilacBlack",
      "app.video.player.IconMintSolid",
      "app.video.player.IconMintGradient",
      "app.video.player.IconMintBlack",
      "app.video.player.IconEmberWhite",
      "app.video.player.IconEmberBlack",
      "app.video.player.IconBlackClassic",
      "app.video.player.IconWhiteClassic",
      "app.video.player.IconCharcoalClassic",
      "app.video.player.IconIndigoClassic",
      "app.video.player.IconVioletClassic",
      "app.video.player.IconAzureClassic",
      "app.video.player.IconLilacClassic",
      "app.video.player.IconMintClassic",
      "app.video.player.IconCrimsonWhite",
      "app.video.player.IconCrimsonBlack",
      "app.video.player.IconRoseWhite",
      "app.video.player.IconRoseBlack",
      "app.video.player.IconPeachWhite",
      "app.video.player.IconPeachBlack",
      "app.video.player.IconAmberWhite",
      "app.video.player.IconAmberBlack",
      "app.video.player.IconLemonWhite",
      "app.video.player.IconLemonBlack",
      "app.video.player.IconLimeWhite",
      "app.video.player.IconLimeBlack",
      "app.video.player.IconForestWhite",
      "app.video.player.IconForestBlack",
      "app.video.player.IconTealWhite",
      "app.video.player.IconTealBlack",
      "app.video.player.IconSkyWhite",
      "app.video.player.IconSkyBlack",
      "app.video.player.IconCobaltWhite",
      "app.video.player.IconCobaltBlack",
      "app.video.player.IconPlumWhite",
      "app.video.player.IconPlumBlack",
      "app.video.player.IconCocoaWhite",
      "app.video.player.IconCocoaBlack",
      "app.video.player.IconWallpaperWhite",
      "app.video.player.IconWallpaperBlack",
    )

    /** Default app icon: Black background with Aura Aperture Disc (Gradient mark in Acid Lime). */
    val Default: LauncherIcon = entries.first { it.background == IconBackground.Black && it.mark == IconMark.Gradient }

    fun of(background: IconBackground, mark: IconMark): LauncherIcon? =
      entries.firstOrNull { it.background == background && it.mark == mark }

    /** Marks that read on [background], in catalog order. */
    fun marksFor(background: IconBackground): List<IconMark> =
      entries.filter { it.background == background }.map { it.mark }

    /** The icon for [background] keeping [mark] when it reads there, else the background's own mark. */
    fun resolve(background: IconBackground, mark: IconMark): LauncherIcon =
      of(background, mark) ?: entries.firstOrNull { it.background == background } ?: Default

    /** Match free-form picks to a real packaged launcher icon, not a home-screen shortcut. */
    fun closest(context: Context, color: Int, mark: IconMark): LauncherIcon {
      val bestBackground = IconBackground.entries.filter { it != IconBackground.Wallpaper }
        .map { it to ColorUtils.distanceEuclidean(
          doubleArrayOf(android.graphics.Color.red(color).toDouble(), android.graphics.Color.green(color).toDouble(), android.graphics.Color.blue(color).toDouble()),
          doubleArrayOf(android.graphics.Color.red(ContextCompat.getColor(context, it.color)).toDouble(), android.graphics.Color.green(ContextCompat.getColor(context, it.color)).toDouble(), android.graphics.Color.blue(ContextCompat.getColor(context, it.color)).toDouble())
        ) }
        .minByOrNull { it.second }?.first ?: IconBackground.Black
      return resolve(bestBackground, mark)
    }
  }
}

/**
 * The fixed icons from before the builder. They stay declared in the manifest, off, only so a
 * user's old choice can be read once and moved to the builder icon that looks the same.
 */
private enum class LegacyIcon(val phoneAlias: String, val tvAlias: String, val becomes: LauncherIcon) {
  SolidPurple(
    "app.video.player.SolidPurpleLauncher", "app.video.player.TvSolidPurpleLauncher",
    LauncherIcon.Default,
  ),
  Purple(
    "app.video.player.PurpleLauncher", "app.video.player.TvPurpleLauncher",
    LauncherIcon.Default,
  ),
  Classic(
    "app.video.player.ClassicLauncher", "app.video.player.TvClassicLauncher",
    LauncherIcon.Default,
  ),
}

internal object LauncherIconManager {
  private const val ENABLED = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
  private const val DISABLED = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
  private const val DEFAULT_STATE = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT

  @Volatile private var cachedCurrent: LauncherIcon? = null
  private var cachedArtwork: Pair<LauncherIcon, Bitmap>? = null

  private fun component(context: Context, name: String) = ComponentName(context.packageName, name)

  private fun stateOf(context: Context, name: String) =
    context.packageManager.getComponentEnabledSetting(component(context, name))

  /** What the package manager says is shown. An explicit pick beats the manifest default. */
  private fun readCurrent(context: Context): LauncherIcon {
    val explicit = LauncherIcon.entries.firstOrNull { stateOf(context, it.aliasName) == ENABLED }
    if (explicit != null) return explicit
    val legacy = LegacyIcon.entries.firstOrNull {
      stateOf(context, it.phoneAlias) == ENABLED || stateOf(context, it.tvAlias) == ENABLED
    }
    return legacy?.becomes ?: LauncherIcon.Default
  }

  fun current(context: Context): LauncherIcon =
    cachedCurrent ?: readCurrent(context).also { cachedCurrent = it }

  /**
   * Makes exactly one icon show: carries an old-style choice over to its builder icon, and repairs
   * a leftover state with none or several on. Does nothing when everything already agrees.
   */
  @Synchronized
  fun reconcile(context: Context) {
    val choice = readCurrent(context)
    val on = LauncherIcon.entries.filter {
      val state = stateOf(context, it.aliasName)
      state == ENABLED || (state == DEFAULT_STATE && it == LauncherIcon.Default)
    }
    val legacyOn = LegacyIcon.entries.any {
      stateOf(context, it.phoneAlias) == ENABLED || stateOf(context, it.tvAlias) == ENABLED
    }
    if (!legacyOn && on.size == 1 && on.first() == choice) {
      cachedCurrent = choice
      return
    }
    select(context, choice)
  }

  /**
   * Android 13+ keeps the splash theme chosen here for the next cold start, so the system splash
   * matches the launcher icon. Older versions cannot switch it and keep the default splash.
   */
  private fun applySplashTheme(activity: Activity, icon: LauncherIcon) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    runCatching { activity.splashScreen.setSplashScreenTheme(icon.splashTheme) }
  }

  /** Recents artwork failures must not turn a successful launcher change into an error. */
  @Suppress("DEPRECATION")
  fun refreshTaskIcon(activity: Activity) {
    runCatching { applySplashTheme(activity, current(activity)) }
    runCatching {
      val icon = current(activity)
      val customMode = CustomIconShortcut.appliedMode(activity)
      val bitmap = if (customMode == "color") {
        CustomIconShortcut.render(activity, CustomIconShortcut.savedColor(activity),
          CustomIconShortcut.savedMark(activity, icon.mark), size = 128) ?: return@runCatching
      } else {
        cachedArtwork?.takeIf { it.first == icon && icon.background != IconBackground.Wallpaper }?.second ?: run {
          val drawable: Drawable = ContextCompat.getDrawable(activity, icon.preview) ?: return@runCatching
          Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888).also {
            drawable.setBounds(0, 0, it.width, it.height)
            drawable.draw(Canvas(it))
            cachedArtwork = icon to it
          }
        }
      }
      activity.setTaskDescription(ActivityManager.TaskDescription(activity.getString(R.string.app_name), bitmap))
    }
  }

  private fun findActivity(context: Context): Activity? {
    var current = context
    while (current is ContextWrapper) {
      if (current is Activity) return current
      val base = current.baseContext
      if (base === current) break
      current = base
    }
    return current as? Activity
  }

  /** Every alias this manager owns, mapped to the state it should end up in for [icon]. */
  private fun targetStates(context: Context, icon: LauncherIcon): Map<ComponentName, Int> {
    val names = LauncherIcon.allKnownAliases +
      LegacyIcon.entries.flatMap { listOf(it.phoneAlias, it.tvAlias) }
    return names.associate { component(context, it) to (if (it == icon.aliasName) ENABLED else DISABLED) }
  }

  private fun applyStates(context: Context, changes: Map<ComponentName, Int>) {
    val pm = context.packageManager
    val flags = PackageManager.DONT_KILL_APP
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      pm.setComponentEnabledSettings(changes.map { (name, state) ->
        PackageManager.ComponentEnabledSetting(name, state, flags)
      })
    } else {
      // Turn the new one on before the rest go off, so the app never has no launcher entry.
      changes.filter { it.value == ENABLED }.forEach { (name, state) -> pm.setComponentEnabledSetting(name, state, flags) }
      changes.filter { it.value != ENABLED }.forEach { (name, state) -> pm.setComponentEnabledSetting(name, state, flags) }
    }
  }

  @Synchronized
  @Suppress("DEPRECATION")
  fun select(context: Context, icon: LauncherIcon) {
    // Fail before touching anything if this build lacks the alias.
    context.packageManager.getActivityInfo(component(context, icon.aliasName), PackageManager.MATCH_DISABLED_COMPONENTS)
    val changes = targetStates(context, icon)
    val previous = changes.keys.associateWith { context.packageManager.getComponentEnabledSetting(it) }
    try {
      applyStates(context, changes)
      check(changes.keys.all { context.packageManager.getComponentEnabledSetting(it) == changes.getValue(it) }) {
        "Launcher alias changes were not applied"
      }
    } catch (failure: Exception) {
      runCatching { applyStates(context, previous) }.exceptionOrNull()?.let(failure::addSuppressed)
      cachedCurrent = null
      throw failure
    }
    cachedCurrent = icon
    findActivity(context)?.let(::refreshTaskIcon)
  }
}
