# Changelog

These notes are written in plain English and focus on what changed for real use.

## 1.0.4

### 🚀 Horizon Player Rebranding & Evolution
- **Official Identity Rebranding to Horizon Player:**
  - Standardized the app name to **Horizon Player** across the entire UI, package strings, build outputs, and documentation.
  - Linked GitHub OTA updates, release workflow asset generation, and in-app update checks directly to `Krishna1998/Horizon-Player`.

### 🎛️ Next-Gen Volume & Brightness HUD Styles
- **6 Hand-Crafted Selectable HUD Styles:**
  - Added full visual HUD customization under **Settings > Appearance > Volume & Brightness HUD Style**:
    - **Dynamic Island**: Sleek top-docked liquid capsule with smooth spring physics and real-time numeric readouts.
    - **Hyper-Notch Matrix**: Dual-body floating architecture featuring a standalone satellite radar orb and frosted glass pill slider.
    - **Aero Dual-Tone**: Apple VisionOS-inspired dual-chamber glass card with an isolated tactile icon chamber and illuminated slider.
    - **Segmented Blocks**: Cyberpunk-inspired discrete vertical 10-block glass ladder with progressive glow and overboost indicators.
    - **Soundstage**: Expansive horizontal bento card with separate circular icon badge, tabular digit readouts, and overboost banner.
    - **Classic Capsule**: Iconic vertical rounded pill filled with the active theme gradient.
- **Enlarged High-Visibility Dimensions:**
  - Significantly expanded horizontal sliders (up to `336.dp` width, `78.dp` height, and `9–10.dp` thick progress tracks) for effortless glanceability and one-handed thumb reach.
  - Extended vertical slider length to `220.dp` and increased width to `42.dp` for wider swipe travel and finer volume/brightness precision.

### 🌊 Visualizer & Aesthetic Refinements
- **Pure Floating Audio Pulsing Waveform:**
  - Removed the slim horizontal baseline line from the Cinematic Horizon audio visualizer for an organic, floating waveform aesthetic.

## 1.0.3

### 📱 Picture-in-Picture Lock Automation & Persistence
- **Automated Pause PiP on Screen Lock:**
  - Added dedicated screen-off broadcast receiver in `MediaPlaybackService` and enhanced `PlayerActivity` PiP lifecycle tracking.
  - Automatically pauses video/audio playback when the phone is locked or screen turns off while in Picture-in-Picture mode, keeping playback state and notification controls intact so playback can be manually resumed whenever desired.
- **PiP Aspect Ratio & Geometry Stability:**
  - Guaranteed persistent stretch and crop aspect ratios across lock/unlock cycles in PiP mode without pillarboxing squishing.

### ⏱️ Sleep Timer Perfection & Streamlined Settings
- **In-Sheet Fade-Out Duration Slider (3s to 30s):**
  - Integrated the gentle audio fade-out slider (configurable from 3s to 30s) directly into the Sleep Timer dialog (`MoreSheet.kt`).
  - Removed duplicate fade-out slider from Audio Preferences to consolidate all sleep timer controls in one intuitive location.
- **Perfect End-of-Track Synchronization:**
  - Synchronized track completion so the sleep timer stops playback cleanly 1 second before track end without premature cut-offs or 1–2 second bleed into the next track.
- **Granular `HH:MM:SS` Time Input:**
  - Upgraded custom duration input with dedicated Hours, Minutes, and Seconds fields for precise countdown configuration.

### 🌅 Cinematic Horizon Streamlined Experience
- **Dedicated Cinematic Horizon Home:**
  - Completely unified the home experience around **Cinematic Horizon**, removing legacy Dual Horizon layout options and simplifying Home preferences.
  - Added **Show Continue Watching** toggle in **Settings > Appearance > Home & Media Library**.

### 📦 Build & Release
- **Standardized Release Artifacts:**
  - Updated release workflow artifact naming to `V&MPlayer-arm64-v8a-<version>.apk` with full backward and forward compatibility in the in-app OTA update manager.

### 🎨 About Screen
- **Author Badge Polish:**
  - Highlighted "By Krishna" badge with enlarged bold styling (`titleMedium`) and refined heart icon styling.

## 1.0.2

### 📱 Picture-in-Picture & Stretch Mode Fixes
- **Persistent Stretch Aspect Ratio in PiP:**
  - Resolved an issue where selecting Stretch aspect ratio and entering Picture-in-Picture (PiP) mode would shrink or squish the video into a narrow portrait pillarbox frame upon locking and unlocking the device.
  - PiP now preserves the correct landscape stretch geometry across lockscreen and orientation changes.
  - Kept PiP actions, receivers, and playback transitions active across lock/unlock cycles.
- **Automated Pause PiP on Screen Lock:**
  - Added **Pause PiP on screen lock** setting under **Settings > Player Preferences**.
  - Automatically pauses playback when locking the phone in Picture-in-Picture mode while preserving the paused frame and controls, allowing users to manually resume playback via the lockscreen notification or PiP play button.

### 🌅 Cinematic Horizon & Dual Horizon Evolution
- **Cinematic Horizon Default Home Layout:**
  - Introduced **Cinematic Horizon** as the flagship home layout with customizable Hero Spotlight cards and smooth media rails.
  - Renamed BentoFusion cleanly across settings, themes, and views to **Dual Horizon**.
  - Added full section reordering and independent card/rail scale controls (Hero spotlight, Video rail, and Audio Pulsing).
  - Added **Show Continue Watching** toggle and **Home Screen layout** selector under **Settings > Appearance > Home & Media Library**.

### ⏱️ Intelligent Sleep Timer & Audio Fade-Out
- **Configurable Fade-Out Slider (3s to 30s):**
  - Added a gentle audio fade-out duration slider (configurable from 3s to 30s, default 10s) directly inside the Sleep Timer dialog (`MoreSheet.kt`) and in **Audio Preferences**.
  - Gradually attenuates playback volume down over the selected seconds before the timer expires or track ends.
- **Precise End-of-Track Synchronization:**
  - Guaranteed exact track ending without spillover or bleeding into the next track (stops cleanly 1.0s before EOF or on EOF boundary).
- **Hours : Minutes : Seconds (`HH:MM:SS`) Custom Input:**
  - Upgraded the sleep timer custom time input with dedicated Hours, Minutes, and Seconds fields for granular countdown precision.

### 🧭 Navigation & Fluid Experience
- **Liquid Bubble Glass Floating Navigation Bar:**
  - Upgraded floating bottom navigation pill with frosted specular reflections, convex rim lighting, and theme-synced colorful icons.
  - Fully removed the obsolete legacy navigation bar in favor of modern edge-to-edge floating navigation.
  - Enforced permanent visibility for essential tabs (Home & Streaming) with customizable optional tabs.
  - Pre-warmed `NetworkStreamingViewModel` at launch to eliminate lag when switching to the Streaming tab.

### 🌌 Visualizer & Customization Upgrades
- **Galaxy Singularity (Dual Event Horizon) Wavy Visualizer:**
  - Revamped audio visualizer with Galaxy Singularity dual ribbon event horizon aesthetics and smooth vsync animations.
- **Custom App Wallpaper System:**
  - Added background wallpaper engine with customizable preset gradients, custom image picker, and ambient blur fallbacks.
- **Samsung One UI Style Floating Bottom Search Pill:**
  - Moved settings search to an ergonomic, one-handed bottom floating pill that neatly docks above the audio mini-player.

### ℹ️ Futuristic Quantum Spatial About Screen & OTA Updates
- **Futuristic About Hub:**
  - Completely redesigned About Screen featuring interactive cosmic nebula canvas with spatial gyroscope tilt interactions.
  - Integrated in-app OTA update checker directly connected with GitHub releases.
- **Enhanced Secure Folder Protection:**
  - Added prominent data loss safety warnings and one-tap restore functionality for Xiaomi/HyperOS dual space uninstallation safety.

## 1.0.1

### 🔊 Audio Player Vertical Volume Gestures & Floating HUD Pill
- **Vertical Volume Swipe on Album Cover:**
  - Implemented vertical touch gesture control directly on the Audio Player cover art with seamless horizontal swipe track-switching coexistence.
  - Smoothly drives master volume (0–100%) and seamlessly engages MPV volume boost (up to the configured boost cap) when volume reaches 100%.
  - Added tactile haptic ticks via `volumeHaptics` and real-time floating `VolumeSlider` liquid glass HUD pill with auto-hide timer.
  - Added user toggle `Audio Volume Gesture` in both **Audio Preferences** and **Gesture Preferences**.

### 🎨 Fusion Rail Header Styles & Customization
- **5 Attractive Rail Header Aesthetics:**
  - Added `Fusion rail header style` setting under **Settings > Appearance > Fusion & Media Library**:
    - **Liquid Glass Capsule (Default)**: Floating frosted capsule with glowing circular glyph and luminous pill counter.
    - **Cyber Glow Pillar**: Laser accent bar with uppercase subtitle track label and high-contrast badge.
    - **Frosted Bento Strip**: Enclosed glass card bar with count subtitle and shortcut action chip.
    - **Fluid Pulse Dot**: Minimalist living pulse dot indicator with soft pill counter.
    - **Studio Luxury Aperture**: Micro-eyebrow category title with bold editorial headline and item bracket.
  - Dynamically updates section titles for Movies/Videos and Music across the Fusion hub.

### 🔄 Interface Restart Prompt for Smooth Experience
- **Restart Recommended Dialog:**
  - Automatically prompts an intuitive restart dialog whenever changing key visual appearance settings (App Theme, Custom Themes, Fusion Rail Header Style, Audio Pulsing Progress Style) to keep app performance and UI rendering silky smooth across all activities.

### ⚙️ Clean Default Preferences Alignment
- **Optimized Defaults for Modern Media Playback:**
  - Set default `showRepeatShuffleOverlay` = **Off**.
  - Set default `showActionFeedbackOverlay` = **Off**.
  - Set default `showResumeIndicatorOverlay` = **Off**.
  - Set default `pinchToZoomSubtitles` = **Off**.
  - Set default `swipeSubtitlesToSeekDialog` = **Off**.
  - Set default `batterySaver` = **On**.
  - Set default `thumbnailGenerator` = **Auto** (tries MediaStore first with Native fallback for maximum coverage).

### 🚀 Performance & Visual Enhancements
- **Ultra-Smooth Audio Pulsing Seekbar:**
  - Eliminated 1-second step ticking on the Audio Pulsing hero card seekbar with continuous vsync frame-clock animation.
- **Branding Assets Clean-up:**
  - Streamlined branding SVG assets and updated primary identity icon to the Velvet Nebula Aura Aperture design.

## 1.0.0

### 🔮 Fusion Experience, Music Sub-Categories & Performance Architecture
- **Unified "Fusion" Branding & Clean Interface:**
  - Transitioned all user-facing BentoFusion terminology cleanly to **Fusion** throughout the app (browser headers, sub-tabs, settings, and cards).
  - Streamlined horizontal media rail naming: updated `Video/Movie` to **Movies / Videos**.
  - Completely removed the redundant standalone Music tab from the bottom navigation bar and navigation bar preferences in favor of the rich, unified Fusion hub.
  - Removed duplicate Clear Thumbnail Cache option in Appearance Settings in favor of Advanced Settings.
  - Dedicated Liquid Glass preference to the main Settings page, eliminating duplicate navigation entries in Appearance Settings.
- **Fusion Music Sub-Category System & Custom Ordering:**
  - Expanded Fusion Music with full sub-category filtering and browsing: **All Music**, **Favorites**, **Playlists**, **Folders**, **Albums**, and **Artists**.
  - Added dedicated interactive bottom sheets (`AlbumDetailSheet` and `ArtistDetailSheet`) with full tracklists, album art, and direct track playback.
  - Added full drag-and-drop reordering and on/off toggles for Music Sub-Tabs in **Audio Settings**.
  - Dynamic default selection: whichever sub-category is placed 1st in the reorder list is automatically selected as the default view upon entering the Music tab.
- **Glass UI Watch Analytics Separation:**
  - Separated Profile Watch Analytics into dedicated, elevated glass cards: **Most Watched** for video media and **Most Listened** for audio tracks, each with specialized iconography, custom containers, and stats.
- **Hardware-Accelerated Panel Performance:**
  - Optimized real-time panel transitions (`PlayerPanels` and `DraggablePanel`) using hardware-accelerated render layers (`graphicsLayer`) to eliminate animation jitter and dropped frames when opening panels with Liquid Glass enabled.
- **Curated Theme Presets Renaming:**
  - Refined flagship themes: `Deep Midnight Cyan` &rarr; **Deep Midnight**, `Nordic Glacial Mint` &rarr; **Nordic Glacial**, and `Velvet Nebula Violet` &rarr; **Velvet Nebula**.

### 🎵 Live Audio Pulsing Sync, All Media Mini-Player Coordination, Duration Cut-Out Slider & Videos Folder View
- **Real-Time Audio Pulsing Live Sync:**
  - Resolved track sync delay when changing music in the audio player (next, previous, or auto-advancing queue).
  - Implemented multi-criteria session matching (`originalUri`, `playableUri`, `stableId`, `title`, and local path variants) with dynamic synthesis of active session items so Audio Pulsing instantly reflects the currently playing song title, artist, codec, live duration, and real-time remaining time without requiring a manual refresh.
  - Linked interactive Previous, Play/Pause, and Next buttons on the Audio Pulsing hero card to live `PlaybackSession` queue commands with seamless fallback to alphabetical library traversal.
- **Contextual Music Mini-Player Suppression on All Media:**
  - Integrated `NavigationBarState.isAllMediaTabActive` coordination via `DisposableEffect` on `MediaLibraryContent`: the bottom floating music mini-player bar automatically and smoothly hides whenever the user is browsing the **All Media** tab, preventing UI overlap and redundancy with the Audio Pulsing hero card.
- **Video Duration Cut-Out Timing Slider (0 to 2 Hours, 15-Minute Intervals):**
  - Added new configurable setting `minimumVideoHorizonDurationMinutes` (range: 0 to 120 minutes with 15-minute discrete steps: 0m, 15m, 30m, 45m, 60m, 75m, 90m, 105m, 120m).
  - Integrated a dedicated Material 3 Slider card in **Settings > Media library** (Folders & files) and indexed it under Settings Search for instant discovery.
  - Applied the duration cut-out threshold across both the **All Media Video/Movie rail** and the **Recently Played rail**, automatically filtering out short video clips while fully preserving audio tracks and songs.
- **Folder View in Videos Tab (Media Library):**
  - Added folder browsing support to the **Videos** tab in Media Library via interactive filter chips (`[ Folders ]` and `[ All Videos ]`), defaulted to Folders.
  - Tapping a video folder seamlessly navigates into the folder and displays all contained video files with thumbnail previews and playback info, matching the music tab's intuitive folder browsing experience.

### ☀️ Light Mode Audio Player Contrast & Readability
- **Dynamic Luminance-Aware Atmospheric Scrim:**
  - Resolved unreadable text and icons in Light Mode by dynamically adapting the full-screen atmospheric canvas overlay over blurred cover art based on theme luminance (`surface.luminance() < 0.5f`).
  - In Light Mode, replaced the hardcoded dark black scrim (`Color.Black` 0.42f–0.85f) with a luminous frosted light surface gradient (`surface` 0.55f–0.94f) ensuring dark charcoal typography (`#1C1B1F` and `#49454F`) has 100% crystal-clear contrast and WCAG AAA compliance.
- **Adaptive Ambient Color Mesh & Watercolor Glow:**
  - In Light Mode, ambient color extraction now selects light vibrant and light muted palette tones with softened translucency (0.25f / 0.15f), providing an airy watercolor glow over the light surface without darkening or muddying the canvas.
- **Theme-Aware Glass UI Pills & Drop Shadows:**
  - Glass capsules (`audioGlassSurface`) in Light Mode now utilize elevated surface containers with enhanced contrast specular outlines (`outlineVariant`), giving transport buttons, track pills, and speed controls clean floating tactile depth.
  - Adapted album art card shadows and borders for Light Mode, replacing aggressive 60% black drop shadows with natural, soft 22% ambient shadows and luminous white top specular rims.
- **Seekbar Track Legibility in Light Mode:**
  - Liquid seekbar and standard seekbar base tracks now automatically evaluate surface luminance, ensuring unplayed tracks retain subtle, distinct contrast in Light Mode rather than blending invisibly into white backgrounds.

### 🌌 Dual-Horizon Rails & Smart Audio Architecture
- **Glass UI Section Titles & Streamlined Rail Naming:**
  - Added frosted Glass UI capsule styling to all section titles (**Music**, **Video/Movie**, **Recently Played**, **AUDIO PULSING**) featuring translucent backdrops, glowing specular rims, and embedded count badges.
  - Simplified and streamlined rail titles: renamed `Sonic Soundstage Horizon` to **Music**, and `Cinematic Video Horizon` to **Video/Movie**.
- **Exclusive All Media Horizon Layout:**
  - Removed vertical redundant list of media under **Media Library > All Media** so the tab exclusively showcases the Bento Horizon Hub (Sonic Soundstage Horizon, Audio Pulsing, Cinematic Video Horizon, Recently Played).
- **Audio Pulsing Dedicated Controls & Clean Thumbnail:**
  - Integrated interactive Previous, Play/Pause, and Next transport controls directly into the Audio Pulsing hero card with live playback state sync and seamless playlist traversal.
  - Removed the play button overlay and codec badge overlay from the thumbnail in Audio Pulsing for an uncluttered cover art presentation.
- **Dynamic Time Format (`-MM:SS/MM:SS`):**
  - Updated live remaining time display in Audio Pulsing and Horizon cards from `-MM:SS remaining` to `-MM:SS/MM:SS` (remaining time / total duration).
- **Title-Wise Audio Sorting in Media Library:**
  - Audio files under **Media Library > All Media** and **Music** tabs are now sorted title-wise (by track title in natural alphabetical order), ensuring clean alphabetical track discovery.
- **Original Audio Codec & Real-Time Remaining Time:**
  - Audio Pulsing hero card detects and displays the file's original audio codec alongside live remaining and total duration (`-MM:SS/MM:SS`).
- **One UI 9 Continuous Fluid Waveform Visualizer:**
  - Upgraded Audio Pulsing visualizer to Samsung One UI 9 3-tier aurora wave visualizer with continuous vsync frame-clock accumulation eliminating loop stuttering.
- **Sonic Soundstage Horizon & Cinematic Video Horizon Position Sweep:**
  - Swept positions: Sonic Soundstage Horizon placed at the top of the screen (Rail 1), Audio Pulsing in the center (Hero 2), Cinematic Video Horizon below Audio Pulsing (Rail 3), and Recently Played at the bottom (Rail 4).
- **Sonic Soundstage Horizon (Top 10 Recently Played Files):**
  - Sonic Soundstage Horizon rail below Audio Pulsing now exclusively features recently played audio files, automatically displaying the top 10 most recently played tracks in chronological order with original codec badges and seek progress.
- **Cinematic Video Horizon Up & Audio Pulsing Down:**
  - Re-architected All Media / Horizon layout: **Cinematic Video Horizon rail** is placed at the top of the screen ("going up") featuring 16:9 widescreen previews, resolution badges (4K HDR, 1080p, 720p), duration, and playback progress indicators.
  - Placed the **Audio Pulsing hero card** directly below the video horizon rail ("going down") with real-time reactive animated equalizer waves, lossless audio tags, and quick playback actions.
- **Default Horizon View Mode & Bento Cleanup:**
  - Made **Dual Horizon ("Horizon")** the primary default view mode in `BrowserPreferences` for instant unified video and audio immersion on launch.
  - Streamlined View Mode options dialog: removed "Bento" in favor of the unified "Horizon" view mode alongside Folder, Tree, and Library.


### 🌊 Visualizer & Seekbar Innovations
- **Samsung One UI 9.5 Aurora Frosted Glass Veil Wavy Visualizer:**
  - Implemented a multi-depth 3-tier wave curtain visualizer flowing directly above the elapsed track of the seekbar in both video and audio players.
  - Three distinct harmonic wave curtains react dynamically in real-time to music frequencies:
    1. **Ambient Deep Bass Veil (First Wave):** Sweeping, deep backdrop wave with 23.5dp elevated base height and low-frequency resonance.
    2. **Harmonic Mid Veil (Mid Wave):** Fluid rolling wave with 20.0dp elevated base height reacting to vocal and mid-range energy.
    3. **Lead Luminous Wave (Last Wave):** Crisp foreground wave with 15.5dp base height, vibrant track-tinted glow, and white liquid specular highlight core.
  - Real-time beat pulse impact, smooth spring dampening when paused or scrubbed, and expanded 70dp container clearance preventing any clipping during bass drops.
  - Tapers gracefully to zero height directly at the seekbar thumb/playhead.
- **9 Premier Seekbar Styles with Live Preferences Preview:**
  - Integrated 9 distinct seekbar styles in **Settings → Player Controls & Layout → Seekbar Style** with live interactive previews and real-time scrubbing/tap support:
    1. **Cyber Neon Laser (`SeekbarStyle.CyberLaser`):** Photonic laser beam track with active neon bloom glow corridor, dual-ring cyan thumb, and pulsing halo.
    2. **Frosted Capsule (`SeekbarStyle.FrostedCapsule`):** Modern pill-capsule container with a frosted glass backdrop, vibrant inner gradient fill, ambient rim edge illumination, and dynamic thumb bounce physics.
    3. **Sonic Waveform (`SeekbarStyle.SonicWaveform`):** High-density 50-band acoustic equalizer waveform bars with an illuminated needle playhead indicator.
    4. **Liquid Mercury (`SeekbarStyle.LiquidMercury`):** Metallic silver-chrome liquid fluid track with high-specular reflective sheen and floating viscous mercury droplet thumb.
    5. **Liquid Glass (`SeekbarStyle.Liquid`):** Frosted glass trough track with top specular highlight rim, optical groove depth, glowing active playback gradient, and floating liquid glass thumb with 8dp ambient drop shadow.
    6. **Wavy (`SeekbarStyle.Wavy`):** Fluid undulating sine-wave rail.
    7. **Thick (`SeekbarStyle.Thick`):** Bold 16dp rounded capsule seekbar with internal thumb indicator.
    8. **Slim (`SeekbarStyle.Slim`):** Sleek 10dp compact seekbar with dynamic end-cap geometry.
    9. **Normal (`SeekbarStyle.Normal`):** Classic refined Material seekbar with clean round thumb.

### 🎵 Audio Player & Ambient Art Architecture
- **Dual-Layer Ambient Cover Art Fill & Smart Ambient Arbitration:**
  - Full-screen ambient canvas that magnifies and heavily blurs the current album artwork across the entire display from status bar to navigation bar.
  - Automatically disables ambient light mesh gradients when Immersive artwork is active to prevent visual clashing and eliminate redundant palette extraction.
  - Seamless mutual exclusivity in Settings: enabling Immersive artwork automatically turns off Ambient Mode, and enabling Ambient Mode turns off Immersive artwork.
  - Paired with high-contrast vertical darkening scrims to maintain 100% legibility for track titles, lyrics, seekbar, and playback controls.
  - Floating 3D Hero Card: expanded 92% width card with a 24dp 3D drop shadow, top specular gradient highlight rim, and 32dp continuous corners.
- **Pure Audio Mini Player:**
  - Video playback cleanly finishes and closes on back navigation without morphing into a docked mini player. Mini player docking is exclusively reserved for music and audio playback.
- **Ergonomic Playback Controls:**
  - Relocated playback speed toggle to the bottom action row pill (alongside Shuffle, Repeat, and Background Playback) for effortless one-handed thumb reach.
  - Relocated track and chapter counter (`Track X / Y`) to the top header bar directly below `NOW PLAYING`.
  - Guaranteed persistent playlist / queue button visibility in landscape playback mode.
- **Refined Music Library & "Jump Back In":**
  - **Dynamic Listening Prioritization:** "Jump Back In" hero carousel dynamically ranks and displays most listened songs based on listening frequency, with fallback to library sort order.
  - **Minimalist Cover Art Presentation:** Removed play count multiplier badges (`2x`, `3x`) and audio codec badges (`FLAC`, `WAV`, `ALAC`, `OPUS`) from Jump Back In cards for clean, uninterrupted artwork display.
  - **High-Res Audio Badges:** Track items in list and grid views display audio fidelity badges (`FLAC`, `WAV`, `ALAC`, `OPUS`, `AAC`, `320K`) and glowing `PLAYING` indicators.
  - Rounded thumbnail drop shadows cleanly matching album artwork corner radius.
- **17 Online Lyrics Providers & Translation:**
  - Integrated 17 online lyrics databases (`lrc.red`, `BiniLyrics`, `BetterLyrics`, `PaxSenix`, `KuGou`, `LRCLIB`, `Musixmatch`, `Genius`, etc.) with word-synced and line-synced timing capability badges.
  - Individual and bulk provider toggles, PaxSenix API configuration, and foreign lyrics translation with customizable target language and display style.

### 🎬 Video Engine & Core Playback
- **libmpv 1.1.8 Core:** Native FFmpeg/mpv decoding stack with universal demuxer and codec support.
- **Hardware Acceleration:** Automated GPU decoding via `mediacodec` and `mediacodec-copy` with automatic recovery fallback.
- **High-Throughput Vulkan Pipeline:** Native Vulkan video rasterizer with OpenGL ES fallback.
- **High-Precision Video Scalers:** Bilinear, Bicubic, Spline36, Lanczos, and EWA Lanczos custom shaders.
- **Dynamic Ambient Letterbox Glow:** Real-time edge illumination sampled dynamically from video frames.
- **Battery Saver Mode:** Automatically drops to energy-efficient bilinear passes, limits CPU wakeups, and cuts GPU load when battery &le; 20%.
- **Aspect-Preserving Picture-in-Picture (PiP):** True locked landscape aspect ratio across queue advancements and track changes.

### 🎚️ Sound, Speed & Acoustics
- **Graphic Equalizer:** Dual-mode tone controls (Bass / Treble) and multi-band graphical EQ.
- **Spatializer & Stereo Widening:** Low-latency algorithmic stereo expansion for immersive headphone audio.
- **Intelligent Playback-Aware Sleep Timer:**
  - Countdown timer ticks exclusively while music is actively running, pausing when playback is paused.
  - 5-second gentle audio fade-out before graceful, crash-free app auto-close.
  - Live countdown badge in player header with estimated stop time and quick extension chips (+5m, +15m).
- **Configurable Volume Step:** Discrete volume step multiplier (1 to 10 steps per hardware key press) with clean digital pre-amplification boost up to +30 dB.
- **Pitch Correction:** Maintains natural harmonic pitch across speed adjustments from 0.25x to 4.0x.

### 🎨 Flagship Branding & Launcher Suite
- **Official Brand Identity: "Aura Aperture Disc":**
  - Luxury vinyl and acoustic aperture silhouette featuring an outer dim aura ring, inner solid disc, and negative-space playhead cutout in electric Acid Lime (`#D4FF00`) on nocturnal Stealth Carbon (`#0E1017`).
  - Tuned to 87% optical scale for balanced breathing room on home-screen launchers.
  - Synchronized across adaptive launcher icons, Android 12+ animated splash screens, Android 13+ Material You monochrome dynamic silhouettes, and legacy density raster mipmaps (`mdpi` through `xxxhdpi`).
- **Curated 10-Icon Elite Launcher Suite:**
  - Dedicated icon switcher in **Settings → Appearance → App icon** featuring 10 futuristic designs:
    1. **Aura Aperture Disc (Acid Lime):** Official default luxury aperture disc.
    2. **Stealth Shutter 'V':** High-voltage Neon Lime & Cyan apex chevron.
    3. **Kilo-Hertz Monolith:** Stepped frequency pillars ascending into a play apex.
    4. **Quantum Aperture:** Particle accelerator ring with focused play diamond.
    5. **Liquid Acoustic Droplet:** Organic resonance droplet with concentric frosted ripples.
    6. **Horizon Aero Blades (Crimson):** Aerodynamic dual media blades.
    7. **Aura Aperture Disc (Cyan):** Flagship vinyl silhouette in Electric Cyan.
    8. **Eclipse Halo (Cyan):** Glowing architectural halo with floating playhead (75% scale).
    9. **Eclipse Halo (Lime):** Glowing architectural halo with floating playhead (75% scale).
    10. **Supersonic Split Arrow:** Delta arrow sliced into 'V' and 'M' ripples (75% scale).
  - Responsive 2-column grid picker with live 132dp animated previews and instant one-tap switching.

### 💎 Interface & Fluid Aesthetics
- **Bento Fusion Interface (Futuristic Unified Studio):**
  - **Flagship Hero Spotlight Bento Card:**
    - Ambient liquid-glass card highlighting the latest active or in-progress media item.
    - Pulsing status indicator (`✦ NOW PULSING` for audio / `🎬 RESUME PLAYING` for videos).
    - Media thumbnail preview with specular play button overlay, title, and metadata chips.
    - **Live Harmonic Equalizer Waveform:** 20-band fluid sine-wave animated equalizer bars reacting in real time across primary and tertiary neon accents.
    - Integrated seek ribbon showing exact resume position and playback progress.
  - **Dual-Horizon Rails Architecture:**
    - **🎬 Cinematic Video Horizon Rail:** Upper horizontal carousel featuring 16:9 thumbnail previews, dynamic 4K/1080p resolution chips, duration pills, watched % seek indicators, and one-tap playback.
    - **🎵 Sonic Soundstage Horizon Rail:** Lower horizontal carousel featuring lossless audio cards with album artwork, FLAC/WAV/ALAC/OPUS badges, track duration, artist metadata, and one-tap playback.
    - **Interactive "Swipe Deck →" Shortcuts:** One-tap navigation from each rail header directly scrolls the swipeable pager into the full dedicated Videos or Music catalog view.
  - **New View Mode Option & Default (`FolderViewMode.BentoFusion` & `FolderViewMode.DualHorizon`):**
    - Added "Bento" and "Horizon" as top-level view mode options in the View Options dialog.
    - Set Bento Fusion as the app's default landing view mode on fresh start, immediately welcoming users to the futuristic studio canvas.
    - **Fluid Swipeable Bento Tabs:** Integrated `NavigationPager` with velocity-aware horizontal swipe gestures across the category tabs (**All Media**, **Videos**, and **Music**). Swiping smoothly shifts the segmented tab indicator, clears selections safely, and preserves separate scroll positions for each media category. Tapping any Bento quick tile or segmented button smoothly animates the pager to that tab.
- **Unified Video & Music Canvas (All-in-One Studio Page):**
  - Seamlessly unified Video and Music on the same screen within Media Library with instant category filtering:
    - **✦ All Media (`MediaLibraryType.All`):** Unified timeline displaying both video cards (with 16:9 thumbnails, duration chips, resolution, FPS) and music cards (with album cover art, lossless tags, audio track indicators) side-by-side.
    - **🎬 Videos:** Instant filter dedicated exclusively to video content.
    - **🎵 Music:** Instant filter dedicated exclusively to audio and lossless songs.
  - **Dynamic Item Counters:** Category capsules display live item counts (`All (142)`, `Videos (38)`, `Music (104)`).
  - **Auto-Activating Audio Indexing:** Selecting `All Media` or `Music` automatically activates audio browser scanning on the fly so music files appear immediately without requiring manual preference toggling.
  - **Polymorphic Playback Queue:** Tapping a video launches full video playback, while tapping a track launches audio playback, preserving the unified queue across `PlayerActivity` without stripping either media type.
  - **Adaptive Search & Empty States:** Intelligent search input placeholders ("Search media...", "Search videos...", "Search audio...") and empty states tailored to the active filter mode.
- **Material 3 Expressive & System-Wide Liquid Glass:**
  - Full dynamic color integration, OLED pure black theme, and expressive typography.
  - Frosted translucent glass surfaces, top-lit specular rim highlights, and ambient drop shadows across app bars, player pods, dialogs, and bento cards.
  - Connected shape morphing (`rememberConnectedShape`) across button presses.
- **6 Futuristic Flagship App Themes:**
  - **Stealth Acid Lime (Default):** High-contrast sports aesthetic on deep nocturnal carbon with electric Acid Lime accents.
  - **Electric Cyan:** Cyber-glow aesthetic on midnight oceanic void with luminous cyan highlights.
  - **Hyper Crimson:** Supersonic track velocity look on obsidian ruby with pure Rosso Corsa crimson.
  - **Cyber Magenta:** Synthwave neon energy on velvet void with vibrant laser magenta.
  - **Quantum Violet:** Deep luxury cosmic void with soft radiant amethyst violet.
  - **Solar Gold:** Warm studio analog aesthetic with liquid pure gold accents.
- **5 Glass UI App Themes:** CyberPrism, RossoPrism, AuroraMint, HoloOpal, and SolarAmber.
- **Slimline HUD Sliders:** Sleek 36dp capsule brightness and volume indicators with 18dp iconography and tabular metrics.
- **Global Scrollbar Hide Toggle:** Added "Hide scrollbar" preference in **Settings → Appearance** to completely hide the fast scrollbar across Home, Music, and all media browser screens.
- **Floating Navigation Bar with Harmonized Companion Search Button:**
  - Integrated a circular liquid glass floating search button beside the floating navigation pill, inspired by modern media player aesthetics.
  - Automatically matches and animates to the exact height and vertical alignment of the floating navigation pill: expands seamlessly when navigation bar labels are shown, and shrinks smoothly to 48dp when labels are hidden.
  - Automatically triggers search with instantaneous input field focus and soft keyboard display across library tabs (Home & Music).
  - Uncluttered Top Bar in Floating Mode: relocates the primary search entry point to the bottom thumb zone, keeping the top app bar clean and spacious.
  - Classic Legacy Mode Preservation: when "Legacy navigation bar" is enabled, the search button remains at its classic top bar location.
  - **Single-Shot Search Event Lifecycle:** Fixed an issue where closing search and browsing folders or switching between tabs would cause the search bar to automatically re-open upon returning to the Home tab. Handled search requests are now immediately consumed, cleared upon closing, and neutralized on tab changes.
- **Hide Navigation Bar Labels Toggle:**
  - Added "Hide navigation bar labels" preference in **Settings → Appearance** to display only icons without text labels across both floating pill and legacy navigation bars for an ultra-clean, minimalist aesthetic.
- **Comprehensive Player Control Button Size Slider (Player Layout):**
  - Added a dedicated "Player control button size" slider exclusively in **Settings → Player Controls & Layout** to smoothly scale playback controls between 70% and 140% (with 100% default reset indicator).
  - Full-surface scaling across:
    - **Video Player:** Center playback controls (Play/Pause, Skip Next/Prev), top-right action buttons, bottom-left & bottom-right control clusters, back button, unlock controls button, and drawer action tiles.
    - **Audio Player:** Center playback controls, top bar (Back button and Info button), bottom action row (Equalizer, Speed pill, Background Playback, Playlist / Up Next), shuffle/repeat toggles, and metadata action buttons.
    - **Live Settings Preview:** Dynamic real-time preview scaling in the Player Controls & Layout editor.
- **Interactive Bento Watch & Listen Analytics:** Clickable 7-day activity chart with separate video vs audio breakdowns, atomic session tracking, and 100% offline privacy (zero telemetry).

### ⚡ Ultra-Lightweight Architecture & Zero Bloat
- **Zero Analytics & Tracking:** Completely free of telemetry, background trackers, and analytics daemons.
- **Zero Google Play Services:** No Cast SDK or Play Services dependencies.
- **Stripped Heavy Bundled Fonts:** Uses system native `SansSerif` typography, shrinking font resource overhead by >90%.
- **Page-Aligned Native Packaging:** Uncompressed page-aligned `.so` packaging eliminating duplicate library extraction and halving installed storage footprint.
- **Dynamic Streaming Buffer:** Low-RAM buffer scaling preventing memory pressure on budget devices.
- **Native Subtitle Engine:** Android native stream decompression replacing heavy third-party archive binaries.
