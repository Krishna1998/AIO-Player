# Changelog

These notes are written in plain English and focus on what changed for real use.

## 2.1.0

- **Samsung One UI 9.5 Aurora Frosted Glass Veil Wavy Visualizer:**
  - Implemented a multi-depth 3-tier wave curtain visualizer flowing directly above the elapsed track of the seekbar in both video and audio players.
  - Three distinct harmonic wave curtains react dynamically in real-time to music frequencies:
    1. **Ambient Deep Bass Veil (First Wave):** Sweeping, deep backdrop wave with 23.5dp elevated base height and low-frequency resonance.
    2. **Harmonic Mid Veil (Mid Wave):** Fluid rolling wave with 20.0dp elevated base height reacting to vocal and mid-range energy.
    3. **Lead Luminous Wave (Last Wave):** Crisp foreground wave with 15.5dp base height, vibrant track-tinted glow, and white liquid specular highlight core.
  - Features real-time beat pulse impact, smooth spring dampening when paused or scrubbed, and expanded 70dp container clearance preventing any clipping during bass drops.
  - Tapers gracefully to zero height directly at the seekbar thumb/playhead.
- **4 Premier Seekbar Styles Added to Player Layout Settings:**
  - Introduced four new cutting-edge seekbar styles to **Settings → Player Controls & Layout → Seekbar Style**, complete with real-time interactive previews and full scrub/tap support:
    1. **Cyber Neon Laser (`SeekbarStyle.CyberLaser`):** Ultra-fine photonic laser beam track with an active neon glow aura corridor, dual-ring cyan thumb with dynamic pulse halo during scrubbing, and photonic core highlights.
    2. **Frosted Capsule (`SeekbarStyle.FrostedCapsule`):** Modern pill-capsule container with a frosted glass backdrop, vibrant inner gradient fill, ambient rim edge illumination, and dynamic thumb bounce physics.
    3. **Sonic Waveform (`SeekbarStyle.SonicWaveform`):** High-density 50-band acoustic equalizer waveform bars mapped along the seek track, lighting up dynamically with an illuminated needle playhead indicator.
    4. **Liquid Mercury (`SeekbarStyle.LiquidMercury`):** Metallic silver-chrome liquid fluid track with high-specular reflective sheen and a floating viscous mercury droplet thumb.
- **Refined Music Library & Clutter-Free "Jump Back In":**
  - **Dynamic Listening Prioritization:** The "Jump Back In" hero carousel now dynamically ranks and displays your most listened songs first, based on listening frequency, with seamless fallback to library sort order.
  - **Minimalist Cover Art Presentation:** Removed distracting play count multiplier badges (`2x`, `3x`) and audio codec badges (`FLAC`, `WAV`, `ALAC`, `OPUS`) from Jump Back In cards, keeping the artwork clean, elegant, and uninterrupted.
  - **Matched Thumbnail Shadows:** Corrected thumbnail shadow geometries to cleanly follow the rounded shape of album cover artwork.
- **Legacy & Adaptive App Icon Suite Alignment:**
  - Added legacy density-specific mipmap raster assets (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) featuring the official flagship Aura Aperture design, ensuring crystal-clear presentation on older launchers and third-party launchers.
  - Removed outdated fastlane screenshots and legacy mipmap assets.
- **Build System & Kotlin Compiler Reliability:**
  - Fixed release compilation errors in `Seekbar.kt` by properly importing `kotlin.math.PI` and `kotlin.math.sin` and declaring `secondaryColor` in `SeekbarStylePreview`.

## 2.0.0

- **Dual-Layer Ambient Cover Art Fill (Audio Player):**
  - Upgraded the audio player background to a full-screen ambient canvas that magnifies and heavily blurs the current album artwork across the entire display from status bar to navigation bar.
  - Paired with high-contrast vertical darkening scrims to maintain 100% legibility for track titles, lyrics, seekbar, and playback controls.
  - Elevated the central cover art card to an expanded 92% width hero card with a 24dp floating 3D drop shadow, top specular gradient highlight rim, and 32dp continuous corners.
  - Enabled immersive artwork by default in audio preferences.
- **Complete Removal of Mini Player for Video Playback:**
  - When watching a video and navigating back to the home or browse tabs, video playback cleanly finishes and closes without sliding down or morphing into a docked mini player.
  - Video player gesture controls now disable swipe-down-to-mini-player, maintaining mini player docking exclusively for music/audio playback.
- **Icon Sizing Refinement:**
  - Scaled **Eclipse Halo (Electric Cyan)**, **Eclipse Halo (Acid Lime)**, and **Supersonic Split Arrow (Cyber Magenta)** down to **75%** scale (`0.75`), providing lighter, sharper optical balance on launchers.
- **Slimline Modern Brightness & Volume Sliders:**
  - Replaced bulky 52dp gesture overlay pills with a thin, sleek 36dp capsule indicator featuring compact 18dp iconography, refined liquid glass blur, and crisp tabular percentage typography.
- **6 New Futuristic Flagship App Themes:**
  - Added 6 curated themes harmonized with the new launcher icon suite:
    1. **Stealth Acid Lime:** High-contrast sports aesthetic on deep nocturnal carbon with electric Acid Lime accents.
    2. **Electric Cyan:** Cyber-glow aesthetic on midnight oceanic void with luminous cyan highlights.
    3. **Hyper Crimson:** Supersonic track velocity look on obsidian ruby with pure Rosso Corsa crimson.
    4. **Cyber Magenta:** Synthwave neon energy on velvet void with vibrant laser magenta.
    5. **Quantum Violet:** Deep luxury cosmic void with soft radiant amethyst violet.
    6. **Solar Gold:** Warm studio analog aesthetic with liquid pure gold accents.
- **Default Theme & Curated Picker Hierarchy:**
  - Set **Stealth Acid Lime** as the official default app theme on fresh installs, perfectly harmonizing with the default Aura Aperture launcher icon.
  - Reordered the theme picker so the 11 premier flagship & glass themes appear first at the front of the carousel (positions 1–11), followed by classic themes.
  - Removed the obsolete legacy default theme.
- **Composable Compilation Fix in App Icon Switcher:**
  - Resolved a Kotlin compiler error (`@Composable invocations can only happen from the context of a @Composable function`) in `AppIconPreferencesScreen.kt` by evaluating `reduceMotion` safely in the composable body rather than inside non-composable `transitionSpec` lambdas.
- **Exclusive 10-Icon Elite Launcher Suite:**
  - Dedicated the **App icon** switcher in **Settings → Appearance → App icon** exclusively to the 10 premier flagship and futuristic designs, completely removing all obsolete and legacy icons.
  - The curated 10-icon collection comprises:
    1. **Aura Aperture Disc (Acid Lime):** Official default flagship luxury vinyl aperture in Acid Lime (`#D4FF00`).
    2. **Stealth Shutter 'V':** High-voltage Neon Lime & Cyan apex chevron with internal playhead.
    3. **Kilo-Hertz Monolith:** Stepped equalizer frequency pillars in Acid Lime ascending into a play apex.
    4. **Quantum Aperture:** Single-stroke particle accelerator ring emitting a focused play diamond in Electric Cyan.
    5. **Liquid Acoustic Droplet:** Organic acoustic resonance droplet triggering concentric frosted ripples in Cyber Magenta & Ultraviolet.
    6. **Horizon Aero Blades (Crimson):** Aerodynamic dual media blades gliding together in Hyper Crimson.
    7. **Aura Aperture Disc (Cyan):** Flagship audiophile vinyl disc and aperture silhouette rendered in Electric Cyan.
    8. **Eclipse Halo (Cyan):** Zen architectural glowing halo containing a floating playhead in Electric Cyan.
    9. **Eclipse Halo (Lime):** Zen architectural glowing halo containing a floating playhead in Acid Lime.
    10. **Supersonic Split Arrow:** Forward-pointing delta arrow sliced into an angular 'V' intake and rhythmic 'M' vector ripples in Cyber Magenta.
  - Streamlined the icon picker interface: replaced the 22-background carousel with a direct, responsive 2-column grid of the 10 icons with live 132dp animated previews and instant one-tap switching.
  - All 10 icons feature 87% scale balance, Android 12+ Animated Splash Screens (`ic_splash_*`), and Android 13+ Material You monochrome dynamic theming (`ic_launcher_*_monochrome`).

## 1.0.5

- **Flagship Brand Identity: "Aura Aperture Disc" (Acid Lime on Stealth Carbon at 87% Scale):**
  - Promoted the **Aura Aperture Disc** design as the official default app icon: an ultra-refined luxury vinyl and acoustic aperture silhouette featuring an outer dim aura ring, an inner solid disc, and a flawless negative-space playhead cutout in high-contrast electric **Acid Lime** (`#D4FF00`).
  - Tuned emblem geometry to **87% scale** (`scaleX="0.87" scaleY="0.87"`), delivering balanced optical breathing room on Android home-screen launchers.
  - Anchored on nocturnal **Stealth Carbon** (`#0E1017`) for striking 14:1 contrast ratio, zero noise, and unmistakable Bang & Olufsen luxury aesthetic.
  - Fully synchronized 1:1 across default adaptive launcher icon (`ic_launcher_purple_foreground`), Android 12+ Animated Splash Screen (`ic_splash_purple_foreground`), Android 13+ Material You monochrome silhouette (`ic_launcher_purple_monochrome` with native `evenOdd` transparency cutout), and project documentation branding (`videoplayer_icon.svg`).
- **In-App Launcher Icon Switcher Streamlined to Exclusive 10-Icon Elite Suite:**
  - Dedicated the **App icon** switcher in **Settings → Appearance → App icon** exclusively to the 10 premier flagship and futuristic designs, removing all obsolete and legacy icons.
  - The curated 10-icon collection comprises:
    1. **Aura Aperture Disc (Acid Lime):** Official default flagship luxury vinyl aperture in Acid Lime (`#D4FF00`).
    2. **Stealth Shutter 'V':** High-voltage Neon Lime & Cyan apex chevron with internal playhead.
    3. **Kilo-Hertz Monolith:** Stepped equalizer frequency pillars in Acid Lime ascending into a play apex.
    4. **Quantum Aperture:** Single-stroke particle accelerator ring emitting a focused play diamond in Electric Cyan.
    5. **Liquid Acoustic Droplet:** Organic acoustic resonance droplet triggering concentric frosted ripples in Cyber Magenta & Ultraviolet.
    6. **Horizon Aero Blades (Crimson):** Aerodynamic dual media blades gliding together in Hyper Crimson.
    7. **Aura Aperture Disc (Cyan):** Flagship audiophile vinyl disc and aperture silhouette rendered in Electric Cyan.
    8. **Eclipse Halo (Cyan):** Zen architectural glowing halo containing a floating playhead in Electric Cyan.
    9. **Eclipse Halo (Lime):** Zen architectural glowing halo containing a floating playhead in Acid Lime.
    10. **Supersonic Split Arrow:** Forward-pointing delta arrow sliced into an angular 'V' intake and rhythmic 'M' vector ripples in Cyber Magenta.
  - Streamlined the icon picker interface: replaced the 22-background carousel with a direct, responsive 2-column grid of the 10 icons with live 132dp animated previews and instant one-tap switching.
  - All 10 icons feature synchronized Android 12+ Animated Splash Screens (`ic_splash_*`), Android 13+ Material You monochrome silhouettes (`ic_launcher_*_monochrome`), and native `<activity-alias>` registration.
- **Video & Audio Player Glass UI Architecture Polish:**
  - **Synchronized Video Player Controls with Top-Right Glass Aesthetic:** Unified bottom-left, bottom-right, and portrait bottom video controls to inherit the exact same Liquid Glass UI as top-right controls: frosted translucent backdrop gradients, top-lit specular rim borders, floating ambient drop shadows, and elastic press feedback (`AdaptiveControlsContainer`, `ControlsButton`).
  - **Frosted Glass UI Seekbar Across Video & Audio Players:** Upgraded both Video Player and Audio Player seekbars to default to the Liquid Glass seekbar (`SeekbarStyle.Liquid`), featuring a frosted glass trough track with top specular highlight rim, optical groove depth, glowing active playback gradient with specular reflection, floating liquid glass thumb with 8dp ambient drop shadow, inner refraction glow, and top-lit white specular border.
  - **Enabled Liquid Glass by Default on Android 12+ (API 31+):** Promoted liquid glass effects to active by default on supported modern devices.
- **Audio Player Ergonomics & Layout Polish:**
  - **Full Liquid Glass UI Architecture:** Transformed the audio player interface with frosted translucent glass pods, specular top-lit gradient rims, and soft floating ambient drop shadows (`audioGlassSurface`) across bottom action pills, playback controls, header pods, lyric strips, and cover art tiles.
  - **Relocated Playback Speed to Bottom Action Row:** Moved the playback speed toggle down into the central bottom action row pill (alongside Shuffle, Repeat, and Background Playback) for effortless one-handed thumb reach during listening sessions.
  - **Upper Header Track & Chapter Counter:** Relocated the track and chapter counter (`Track X / Y` / `Chapter X / Y`) to the top header bar directly below `NOW PLAYING`, decluttering the song metadata area while keeping queue progression immediately visible.
  - **Landscape Bottom Bar Persistence:** Fixed landscape playback layout to guarantee the bottom-right Playlist / Now Playing queue button is always visible alongside the centered action pill.
- **Dynamic Profile Watch & Listen Analytics:**
  - Enhanced the Watch Stats screen in the Profile tab: the leaderboard header now dynamically switches between **"Most watched"** when browsing video playback and **"Most listened"** when the audio toggle is active, reflecting real media consumption semantics.
- **5 Brand New Glass UI App Themes:**
  - Added five modern themes engineered with frosted specular aesthetics into the `AppTheme` core engine:
    - **CyberPrism:** Electric Neon Cyan & Cyberpunk frosted glass.
    - **RossoPrism:** High-velocity Ferrari Scarlet & Velvet Cabernet dynamic glass.
    - **AuroraMint:** Arctic Emerald & Mint frost glass.
    - **HoloOpal:** Iridescent Holographic Opal & Lilac glass.
    - **SolarAmber:** Warm Golden Amber & Sunset Citrus glass.

## 1.0.4

- **Flagship Brand Identity: "Acoustic Turbine" (Icy Cyan & Cobalt Blue on Pitch Black):**
  - Modernized the official brand identity with the futuristic **Concept 4: Acoustic Turbine** at a balanced 95% scale for peak home-screen visual presence.
  - Crafted acoustic turbine blade arcs in Icy Cyan (`#00F5FF`, `#00D2FF`) and Electric Cobalt Blue (`#0051FF`, `#0038A8`) with a central white specular playhead atop an AMOLED pitch black (`#000000`) canvas.
  - Fully synchronized 1:1 across default launcher icon (`ic_launcher_black_gradient`), Android 12+ Splash Screen (`ic_splash_purple_foreground`), Android 13+ Material You monochrome silhouette, and project documentation SVG assets (`videoplayer_icon.svg`).
  - Streamlined icon management: removed alternative icon pickers and disabled all 58 legacy aliases, enforcing Concept 4 Black as the single active launcher icon across all installs and updates.
- **Sleep Timer Graceful Shutdown & Crash Fix:**
  - Resolved an unexpected error dialog that occurred when the sleep timer triggered application exit.
  - Replaced abrupt process kill (`SIGKILL`) with graceful multi-stage shutdown (`App.exitApplication`) and flagged intentional shutdowns in `CrashMonitor` to eliminate false-positive crash detection.
  - Tuned gentle audio fade-out to 5 seconds before clean application termination.
- **Settings Refinement & General Polish:**
  - Removed "Experimental" status badge and banner from Liquid Glass preferences, promoting it to standard UI status.
  - Promoted Vulkan video decoding from "Vulkan (Experimental)" to "Vulkan".
  - Cleaned up obsolete app icon settings entries from settings search indexing.

## 1.0.3

- **Default App Icon: "Solar Amber Gold" Cyber Prism (95% Scale):**
  - Upgraded the default launcher icon to the futuristic **Cyber Prism** design at a balanced 95% scale for ideal home-screen visual presence.
  - Eliminated blurry diffuse neon glow in favor of a clean, razor-sharp vector aesthetic with metallic specular bevels, dark chamfer shadows, and solid crimson equalizer bars.
  - Introduced the **Solar Amber Gold** palette: a liquid gold (`#FFD700`) and deep amber (`#FF9100`) 3D crystal prism playhead with diamond white facet lines.
  - Fully synchronized the Android 13+ Material You monochrome themed icon asset.
- **Audio Player Sleep Timer Integration & Active Playback Sync:**
  - Added dedicated Sleep Timer controls to the Audio Player controls row and top header bar.
  - Active countdown badge displayed directly in the top header bar with live elapsed time pill (e.g. `14:32`).
  - **Playback-Aware Timer:** The countdown timer now ticks exclusively while music is actively running. If playback is paused, the timer automatically pauses, preserving remaining time without draining down while the player is idle.
  - Integrated `Sheets.SleepTimer` unified player sheet with 5-second gentle audio fade-out, auto-close app completely on completion, stop time estimation, quick extension chips (+5m, +15m), and presets (15m, 30m, 45m, 60m, End of Track).
- **Video Folder & Media Browser FAB Alignment Fix:**
  - Fixed an issue where activating the music mini-player pushed the Floating Action Button (FAB) high up into the middle of the screen when browsing video folders (`VideoListScreen`), playlists, or filesystem directories.
  - Exempted full-height media browser screens from redundant host container insets, allowing video lists to scroll smoothly behind the translucent mini player while anchoring the FAB at its clean resting position 12dp above the mini player.
- **Tab-Specific Quick Play FAB:**
  - Separated Floating Action Button playback logic so tapping "Play All" or "Shuffle" on the Favorites tab plays only favorited tracks ("Play All Favorites" / "Shuffle Favorites") rather than entire library songs.

## 1.0.2

- **Music Tab Visual & UX Overhaul:**
  - **Liquid Glass Navigation Pills:** Replaced the legacy tab row with modern horizontal-scrolling liquid glass capsules featuring category icons (Songs, Albums, Artists, Playlists, Folders, Favorites) and real-time item count badges.
  - **"Jump Back In" Hero Carousel:** Added a sleek horizontal hero carousel at the top of the Songs tab featuring top rotation tracks, high-res audio tags, rounded bento cards, and instant one-tap floating play buttons.
  - **Modern Quick Actions Bar:** Introduced prominent "Shuffle All" and "Play All" action buttons at the top of the track list in both List and Grid views for effortless queue startup.
  - **High-Res Audio Format & Status Badges:** Track items in both list and grid views now display crystal-clear audio fidelity badges (`FLAC`, `WAV`, `ALAC`, `OPUS`, `AAC`, `320K`) and animated glowing `PLAYING` indicators.
  - **Enhanced Bento Album Cards:** Added specular borders and bottom-right track count badges (`X tracks`) to album grid cards for rich visual hierarchy.
- **Online Lyrics Providers & Settings Control:**
  - Surfaced all 17 available online lyrics providers (`lrc.red`, `BiniLyrics`, `BetterLyrics`, `BetterLyrics Portato`, `PaxSenix`, `PaxSenix: Spotify`, `PaxSenix: Musixmatch`, `LyricsPlus`, `SimpMusic`, `Unison`, `YouTube captions`, `YouTube Music`, `Megalobiz`, `KuGou`, `LRCLIB`, `Musixmatch`, `Genius`) directly inside Settings > Audio.
  - Added an interactive expandable card with live provider status, timing capability badges (Word-synced vs Line-synced), descriptions, and individual toggle switches.
  - Added "Enable all" and "Disable all" bulk toggles with persistent preference store sync (`disabled_lyrics_providers`).
  - Integrated full PaxSenix API key configuration and foreign lyrics auto-translation controls with target language and display style options.
  - Connected lyrics repository and racing engine to respect disabled providers during automatic and fallback lookups.
- **Official "Crimson Sonic" App Theme Preset:**
  - Added a dedicated built-in theme preset matching the Crimson Sonic Neon launcher icon.
  - Combines Radiant Neon Crimson Pink (`#FF2A6D`) accents with Electric Cyan (`#00F5FF`) secondary highlights and deep Obsidian Black (`#0D0B12`) / soft pastel lavender backgrounds.
- **Smart Sleep Timer with Gentle Audio Fade-Out:**
  - Added smart sleep timer controls accessible from player sheets with custom hours/minutes input and quick presets (15m, 30m, 45m, 60m, and End of Track).
  - Implemented 5-second gentle audio fade-out so volume gradually and smoothly decreases before automatically closing the app completely on timer completion.
  - Added active timer banner showing exact countdown, estimated stop time (e.g. "Stops at 11:45 PM"), and quick extension chips (+5m, +15m).
  - Volume is automatically restored upon completion or cancellation so future playback is never left muted.
- **Configurable Volume Step (1 to 10 Steps):**
  - Added a "Volume step" preference under Settings > Audio (and in-player settings sheet) with a 1 to 10 discrete slider.
  - Controls how much volume changes per hardware button press, scaling volume boost proportionally.
- **System-Wide Liquid Glass Expansion:**
  - Extended Liquid Glass styling across the entire app with a universal `appGlassSurface` modifier.
  - Applied frosted glass surfaces and specular rim highlights to all modal and single-choice dialogs (`AppAlertDialog`, `AppSingleChoiceDialog`).
  - Added frosted translucent glass styling to top app bars (`BrowserTopBar`, `SettingsTopAppBar`) so scrolling content blurs beneath them.
  - Converted Bento analytics dashboard cards (`WatchStatsScreen`) to liquid glass containers.
  - Added specular top-lit glass borders to thumbnail duration and media badges (`cardOverlay`).
- **Homescreen Widget Font Fix:**
  - Replaced legacy `@font/gflex_variable` layout references with system `sans-serif` typography, resolving resource linking errors.

## 1.0.1

- **Ultra-Lightweight Engine Overhaul:**
  - Stripped ~4.0 MB bundled `gflex_variable.ttf` font in favor of zero-overhead system typography (`FontFamily.SansSerif`), reducing resource footprint by over 90%.
  - Switched to uncompressed page-aligned native library packaging (`useLegacyPackaging = false`), eliminating duplicate `.so` extraction on Android 8+ and halving installed storage.
  - Pruned redundant Desktop/JVM multiplatform icon artifact (`composables-material-symbols-rounded-filled-cmp`).
  - Removed 42 unused professional studio camera log shaders (`shaders/hdr-toys/transfer-function/log/`).
  - Implemented dynamic low-RAM scaling for the streaming demuxer buffer (auto-drops to 32MB cache on budget devices to prevent memory pressure).
- **Brand & Visual Identity:**
  - Added Center-Core Electric Neon Crimson Sonic launcher icons with Material You Android 13+ themed monochrome silhouette support.
  - Perfectly balanced and centered the cyan playhead with exact 18px lateral margins.
- **Interactive Weekly Activity Chart:**
  - Made every day in the "Last 7 days" activity chart clickable to inspect individual watch times.
  - Added dynamic status pill showing exact watch time for the selected weekday, yesterday, or today.
  - Added smooth highlight animation and expanding bar indicators with accent borders on selection.
  - Added quick tap-to-reset back to Today.
- **Separate Video & Audio Statistics:**
  - Added dedicated Video and Audio tabs to the Most Watched leaderboard in Watch Stats.
  - Accurately tracks audio-only playback versus video watch time in `WatchStatsRepository`.
- **Playback Speed Improvements:**
  - Added audio playback speed persistence and configurable speed presets.
- **Configurable Volume Step:**
  - Added a new "Volume step" preference under Settings > Audio (and in-player settings sheet) with a 1 to 5 discrete slider.
  - Controls how much volume changes per hardware button press, scaling volume boost proportionally.

## 1.0.0

- Release version 1.0.0 (initial release).
- High-performance, lightweight video and music player.
