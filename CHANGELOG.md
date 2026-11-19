# Changelog

These notes are written in plain English and focus on what changed for real use.

## 1.0.0

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
