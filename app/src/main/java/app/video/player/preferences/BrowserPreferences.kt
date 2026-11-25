/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.preferences

import app.video.player.preferences.preference.Preference
import app.video.player.preferences.preference.PreferenceStore
import app.video.player.preferences.preference.getEnum
import app.video.player.ui.browser.music.MusicSortField
import app.video.player.ui.browser.music.MusicSortOrder
import app.video.player.ui.browser.music.MusicViewMode
import app.video.player.utils.device.DeviceFormFactor

/**
 * Preferences for the video browser (folder and video lists)
 */
enum class VideoSwipeAction {
  None,
  ToggleWatched,
  AddToPlaylist,
  PlayNext,
  AddToQueue,
  Delete,
  MarkNew,
  LastPlayed,
  Finished,
  ClearHistory,
}

class BrowserPreferences(
  preferenceStore: PreferenceStore,
  context: android.content.Context,
) {
  companion object {
    internal const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"
    internal const val DEFAULT_VIDEO_SWIPE_ZONE_PERCENT = 25
    internal val VIDEO_SWIPE_ZONE_RANGE = 1..50
  }

  // TV boxes decode thumbnails slowly enough to stall D-pad navigation, so TV starts without them.
  private val television = DeviceFormFactor.isTelevision(context)

  // Folder sorting preferences
  val folderSortType = preferenceStore.getEnum("folder_sort_type", FolderSortType.Title)
  val folderSortOrder = preferenceStore.getEnum("folder_sort_order", SortOrder.Ascending)

  // Video sorting preferences
  val videoSortType = preferenceStore.getEnum("video_sort_type", VideoSortType.Title)
  val videoSortOrder = preferenceStore.getEnum("video_sort_order", SortOrder.Ascending)
  val videoSwipeRight = preferenceStore.getEnum("video_swipe_right", VideoSwipeAction.None)
  val videoSwipeLeft = preferenceStore.getEnum("video_swipe_left", VideoSwipeAction.None)
  val videoSwipeRightZonePercent = preferenceStore.getInt("video_swipe_right_zone_percent", DEFAULT_VIDEO_SWIPE_ZONE_PERCENT)
  val videoSwipeLeftZonePercent = preferenceStore.getInt("video_swipe_left_zone_percent", DEFAULT_VIDEO_SWIPE_ZONE_PERCENT)

  val playlistSortType = preferenceStore.getEnum("playlist_sort_type", PlaylistSortType.Original)
  val playlistSortOrder = preferenceStore.getEnum("playlist_sort_order", SortOrder.Ascending)
  val playlistItemSortType = preferenceStore.getEnum("playlist_item_sort_type", PlaylistSortType.Original)
  val playlistItemSortOrder = preferenceStore.getEnum("playlist_item_sort_order", SortOrder.Ascending)
  val recentSortType = preferenceStore.getEnum("recent_sort_type", RecentSortType.LastPlayed)
  val recentSortOrder = preferenceStore.getEnum("recent_sort_order", SortOrder.Descending)
  // FR-09: the grid defaults to newest capture first.
  val snapshotSortType = preferenceStore.getEnum("snapshot_sort_type", SnapshotSortType.CapturedAt)
  val snapshotSortOrder = preferenceStore.getEnum("snapshot_sort_order", SortOrder.Descending)
  val snapshotFolderSortType = preferenceStore.getEnum("snapshot_folder_sort_type", SnapshotFolderSortType.Name)
  val snapshotFolderSortOrder = preferenceStore.getEnum("snapshot_folder_sort_order", SortOrder.Ascending)
  val snapshotLibraryView =
    BrowserPageViewPreferences(
      preferenceStore,
      "snapshot_library",
      folderDefaults = true,
      defaultLayoutMode = MediaLayoutMode.GRID,
      inheritGlobalLayout = false,
    )
  val snapshotItemView =
    BrowserPageViewPreferences(
      preferenceStore,
      "snapshot_item",
      defaultLayoutMode = MediaLayoutMode.GRID,
      inheritGlobalLayout = false,
    )
  val recentView = BrowserPageViewPreferences(preferenceStore, "recent", thumbnailsByDefault = !television)
  val playlistView = BrowserPageViewPreferences(preferenceStore, "playlist_library", folderDefaults = true)
  val playlistItemView =
    BrowserPageViewPreferences(preferenceStore, "playlist_item", thumbnailsByDefault = !television)

  // Music view mode and sorting preferences
  val musicViewMode = preferenceStore.getEnum("music_view_mode", MusicViewMode.GRID)
  val musicSortField = preferenceStore.getEnum("music_sort_field", MusicSortField.TITLE)
  val musicSortOrder = preferenceStore.getEnum("music_sort_order", MusicSortOrder.ASCENDING)

  // Audiobook view mode and sorting preferences
  val audiobookSortType = preferenceStore.getEnum("audiobook_sort_type", AudiobookSortType.Title)
  val audiobookSortOrder = preferenceStore.getEnum("audiobook_sort_order", SortOrder.Ascending)
  val audiobookLayoutMode = preferenceStore.getEnum("audiobook_layout_mode", MediaLayoutMode.LIST)

  // Network sorting preferences
  val networkSortType = preferenceStore.getEnum("network_sort_type", NetworkSortType.Title)
  val networkSortOrder = preferenceStore.getEnum("network_sort_order", SortOrder.Ascending)
  val networkLayoutMode = preferenceStore.getEnum("network_layout_mode", MediaLayoutMode.LIST)
  val jellyfinLayoutMode = preferenceStore.getEnum("jellyfin_layout_mode", MediaLayoutMode.GRID)
  val jellyfinMusicViewMode = preferenceStore.getEnum("jellyfin_music_view_mode", MusicViewMode.GRID)
  val jellyfinMusicSortField = preferenceStore.getEnum("jellyfin_music_sort_field", MusicSortField.TITLE)
  val jellyfinMusicSortOrder = preferenceStore.getEnum("jellyfin_music_sort_order", MusicSortOrder.ASCENDING)
  val navidromeViewMode = preferenceStore.getEnum("navidrome_view_mode", MusicViewMode.GRID)
  val navidromeSortField = preferenceStore.getEnum("navidrome_sort_field", MusicSortField.TITLE)
  val navidromeSortOrder = preferenceStore.getEnum("navidrome_sort_order", MusicSortOrder.ASCENDING)

  val folderViewMode = preferenceStore.getEnum("folder_view_mode", FolderViewMode.DualHorizon)
  val dualPaneForTablet = preferenceStore.getBoolean("dual_pane_for_tablet", false)

  val folderGridColumnsPortrait = preferenceStore.getInt("folder_grid_columns_portrait", 2)
  val folderGridColumnsLandscape = preferenceStore.getInt("folder_grid_columns_landscape", 6)

  val videoGridColumnsPortrait = preferenceStore.getInt("video_grid_columns_portrait", 2)
  val videoGridColumnsLandscape = preferenceStore.getInt("video_grid_columns_landscape", 6)

  val musicGridColumnsPortrait = preferenceStore.getInt("music_grid_columns_portrait", 2)
  val musicGridColumnsLandscape = preferenceStore.getInt("music_grid_columns_landscape", 6)

  val folderGridColumnsDualPanePortrait = preferenceStore.getInt("folder_grid_columns_dual_pane_portrait", 0)
  val folderGridColumnsDualPaneLandscape = preferenceStore.getInt("folder_grid_columns_dual_pane_landscape", 0)
  val videoGridColumnsDualPanePortrait = preferenceStore.getInt("video_grid_columns_dual_pane_portrait", 0)
  val videoGridColumnsDualPaneLandscape = preferenceStore.getInt("video_grid_columns_dual_pane_landscape", 0)
  val musicGridColumnsDualPanePortrait = preferenceStore.getInt("music_grid_columns_dual_pane_portrait", 0)
  val musicGridColumnsDualPaneLandscape = preferenceStore.getInt("music_grid_columns_dual_pane_landscape", 0)

  val showExtensionField = preferenceStore.getBoolean("show_extension_field", false)
  // Off by default: files without a MediaStore duration are probed one by one to fill it.
  val showDurationField = preferenceStore.getBoolean("show_duration_field", false)

  // Visibility preferences for video card chips
  val showVideoThumbnails = preferenceStore.getBoolean("show_video_thumbnails", !television)
  val thumbnailGenerator = preferenceStore.getEnum("thumbnail_generator", ThumbnailGenerator.Auto)
  val thumbnailMode = preferenceStore.getEnum("thumbnail_mode", ThumbnailMode.Smart)
  val thumbnailQuality = preferenceStore.getEnum("thumbnail_quality", ThumbnailQuality.High)
  val thumbnailFramePosition = preferenceStore.getFloat("thumbnail_frame_position", 33f)

  /** Thumbnails generated at the same time; 0 means Auto (a safe value for this device). */
  val thumbnailParallelism = preferenceStore.getInt("thumbnail_parallelism", 0)
  val autoFetchOnlinePosters = preferenceStore.getBoolean("auto_fetch_online_posters", true)
  val replaceEmbeddedWithOnlinePosters = preferenceStore.getBoolean("replace_embedded_with_online_posters", true)
  val autoFetchOnlineMusicArtwork = preferenceStore.getBoolean("auto_fetch_online_music_artwork", true)
  val replaceEmbeddedWithOnlineMusicArtwork = preferenceStore.getBoolean("replace_embedded_with_online_music_artwork", false)
  val showSizeChip = preferenceStore.getBoolean("show_size_chip", false)

  // Metadata-dependent chips (disabled by default for better performance)
  val showResolutionChip = preferenceStore.getBoolean("show_resolution_chip", false)
  val showFramerateInResolution = preferenceStore.getBoolean("show_framerate_in_resolution", false)
  val showSubtitleIndicator = preferenceStore.getBoolean("show_subtitle_indicator", false)
  val showCodecSupportIndicator = preferenceStore.getBoolean("show_codec_support_indicator", false)
  val showProgressBar = preferenceStore.getBoolean("show_progress_bar", true)
  val grayscaleWatchedThumbnails = preferenceStore.getBoolean("grayscale_watched_thumbnails", true)
  val centerGridTitles = preferenceStore.getBoolean("center_grid_titles", true)
  val mediaLayoutMode = preferenceStore.getEnum("media_layout_mode", MediaLayoutMode.GRID)
  val folderViewFolderLayoutMode = preferenceStore.getEnum("folder_view_folder_layout_mode", MediaLayoutMode.GRID)
  val folderViewVideoLayoutMode = preferenceStore.getEnum("folder_view_video_layout_mode", MediaLayoutMode.GRID)
  val separateFolderVideoLayout = preferenceStore.getBoolean("separate_folder_video_layout", false)
  val manualGridColumnsEnabled = preferenceStore.getBoolean("manual_grid_columns_enabled", true)
  val musicCoverArtSize = preferenceStore.getInt("music_cover_art_size", 48)
  val musicGridCoverArtSize = preferenceStore.getInt("music_grid_cover_art_size", 145)

  // Visibility preferences for folder card chips
  val showTotalVideosChip = preferenceStore.getBoolean("show_total_videos_chip", true)

  // Metadata-dependent chips (disabled by default for better performance)
  val showTotalDurationChip = preferenceStore.getBoolean("show_total_duration_chip", false)
  val showTotalSizeChip = preferenceStore.getBoolean("show_total_size_chip", false)
  val showDateChip = preferenceStore.getBoolean("show_date_chip", false)
  val showFolderPath = preferenceStore.getBoolean("show_folder_path", false)
  val showFolderThumbnails = preferenceStore.getBoolean("show_folder_thumbnails", true)

  // Auto-scroll to last played media preference (like MX Player)
  val autoScrollToLastPlayed = preferenceStore.getBoolean("auto_scroll_to_last_played", true)

  // Maximum single-child folder levels skipped in one Tree View navigation step.
  val treeFlattenDepth = preferenceStore.getEnum("tree_flatten_depth", TreeFlattenDepth.Unlimited)
  val includeAudioBrowser = preferenceStore.getBoolean("include_audio_browser", true)
  val includeImagesInBrowser = preferenceStore.getBoolean("include_images_in_browser", false)
  val minimumAudioDurationSeconds = preferenceStore.getInt("minimum_audio_duration_seconds", 120)
  val minimumVideoHorizonDurationMinutes = preferenceStore.getInt("minimum_video_horizon_duration_minutes", 0)
  val mediaLibraryType = preferenceStore.getEnum("media_library_type", MediaLibraryType.All)
  val lastPlayedAudioPath = preferenceStore.getString("last_played_audio_path", "")

  // True once the user finished (or skipped through) the first-run permission flow.
  val onboardingCompleted = preferenceStore.getBoolean(ONBOARDING_COMPLETED_KEY, false)

  // DualHorizon & Media Library section visibility toggles
  val homeScreenLayout = preferenceStore.getEnum("home_screen_layout", HomeScreenLayout.CinematicHorizon)
  val dualHorizonShowVideoRail = preferenceStore.getBoolean("bentofusion_show_video_rail", true)
  val dualHorizonShowAudioPulsing = preferenceStore.getBoolean("bentofusion_show_audio_pulsing", true)
  val cinematicShowHeroSpotlight = preferenceStore.getBoolean("cinematic_show_hero_spotlight", true)
  val cinematicSpotlightScale = preferenceStore.getFloat("cinematic_spotlight_scale", 1.0f)
  val heroSpotlightSeekbarStyle =
    preferenceStore.getEnum("hero_spotlight_seekbar_style", HeroSpotlightSeekbarStyle.TimestampPill)
  val dualHorizonAudioPulsingScale = preferenceStore.getFloat("bentofusion_audio_pulsing_scale", 1.0f)
  val dualHorizonVideoRailScale = preferenceStore.getFloat("bentofusion_video_rail_scale", 1.0f)
  val dualHorizonMusicRailScale = preferenceStore.getFloat("bentofusion_music_rail_scale", 1.0f)
  val audioPulsingProgressStyle = preferenceStore.getEnum("audio_pulsing_progress_style", AudioPulsingProgressStyle.WavyVisualizer)
  val dualHorizonHeaderStyle = preferenceStore.getEnum("bentofusion_header_style", DualHorizonHeaderStyle.LiquidCapsule)
  val dualHorizonShowMusicRail = preferenceStore.getBoolean("bentofusion_show_music_rail", true)
  val dualHorizonSectionOrder = preferenceStore.getObject(
    key = "bentofusion_section_order",
    defaultValue = listOf("VIDEO", "PULSING", "MUSIC"),
    serializer = { order -> order.joinToString(",") },
    deserializer = { stored ->
      val defaults = listOf("VIDEO", "PULSING", "MUSIC")
      val parsed = stored.split(",").map(String::trim).filter { it in defaults }.distinct()
      parsed + defaults.filterNot { it in parsed }
    },
  )
  val libraryShowMusicTab = preferenceStore.getBoolean("library_show_music_tab", true)
  val libraryShowMoviesTab = preferenceStore.getBoolean("library_show_movies_tab", true)
  val openMovieDetailsOnTap = preferenceStore.getBoolean("open_movie_details_on_tap", true)
  val hideMoviesInVideoFolders = preferenceStore.getBoolean("hide_movies_in_video_folders", true)
  val movieExcludedFolders = preferenceStore.getStringSet("movie_excluded_folders", emptySet())
  val mediaLibrarySubTabsOrder = preferenceStore.getObject(
    key = "media_library_sub_tabs_order",
    defaultValue = listOf("ALL", "MOVIES", "VIDEO", "AUDIO"),
    serializer = { order -> order.joinToString(",") },
    deserializer = { stored ->
      val defaults = listOf("ALL", "MOVIES", "VIDEO", "AUDIO")
      val parsed = stored.split(",").map(String::trim).filter { it in defaults }.distinct()
      parsed + defaults.filterNot { it in parsed }
    },
  )

  // Feature discovery hint for DualHorizon (Audio Pulsing gestures & interactive highlights)
  val dualHorizonHintDismissed = preferenceStore.getBoolean("bentofusion_hint_dismissed", false)

  // Spotlight Gestures Guide hint seen state (shows automatically one time)
  val spotlightGesturesHintSeen = preferenceStore.getBoolean("spotlight_gestures_hint_seen", false)

  // Show gestures guide every time on launch (don't remember dismissal)
  val spotlightGesturesShowEveryTime = preferenceStore.getBoolean("spotlight_gestures_show_every_time", false)

  // How to play / stream hint dismissal state for Network Streaming screen
  val networkStreamHintDismissed = preferenceStore.getBoolean("network_stream_hint_dismissed", false)

  // Watched threshold preference (percentage 1-100)
  val watchedThreshold = preferenceStore.getInt("watched_threshold", 95)

  // When deleting a folder, delete all files instead of only media files
  val deleteFolderAllContents = preferenceStore.getBoolean("delete_folder_all_contents", false)
}

class BrowserPageViewPreferences internal constructor(
  private val preferenceStore: PreferenceStore,
  private val prefix: String,
  folderDefaults: Boolean = false,
  /**
   * The layout a page starts in. Snapshots pass GRID: a photo album that opens as a list gets the
   * first impression wrong, and the browser's own LIST default is about file names, not pictures.
   */
  defaultLayoutMode: MediaLayoutMode = MediaLayoutMode.LIST,
  /**
   * Whether an unset page layout is seeded from the browser's global `media_layout_mode`. Snapshots
   * opt out so their grid default is not dragged to the list by an unrelated setting.
   */
  inheritGlobalLayout: Boolean = true,
  thumbnailsByDefault: Boolean = !folderDefaults,
) {
  val layoutMode =
    if (inheritGlobalLayout) {
      preferenceStore
        .getEnum("${prefix}_layout_mode", defaultLayoutMode)
        .initializeFrom(preferenceStore.getEnum("media_layout_mode", defaultLayoutMode))
    } else {
      preferenceStore.getEnum("${prefix}_layout_mode", defaultLayoutMode)
    }
  val manualGridColumnsEnabled = boolean("manual_grid_columns_enabled", false)
  val gridColumnsPortrait = integer(
    "grid_columns_portrait",
    0,
    if (folderDefaults) "folder_grid_columns_portrait" else "video_grid_columns_portrait",
  )
  val gridColumnsLandscape = integer(
    "grid_columns_landscape",
    0,
    if (folderDefaults) "folder_grid_columns_landscape" else "video_grid_columns_landscape",
  )
  val showThumbnails = boolean(
    "show_thumbnails",
    thumbnailsByDefault,
    if (folderDefaults) "show_folder_thumbnails" else "show_video_thumbnails",
  )
  val unlimitedNameLines = boolean("unlimited_name_lines", false)
  val centerGridTitles = boolean("center_grid_titles", true)
  val showExtensionField = boolean("show_extension_field", false)
  val showDurationField = boolean("show_duration_field", false)
  val showSizeChip = boolean("show_size_chip", false)
  val showResolutionChip = boolean("show_resolution_chip", false)
  val showFramerateInResolution = boolean("show_framerate_in_resolution", false)
  val showSubtitleIndicator = boolean("show_subtitle_indicator", false)
  val showCodecSupportIndicator = boolean("show_codec_support_indicator", false)
  val showProgressBar = boolean("show_progress_bar", true)
  val grayscaleWatchedThumbnails = boolean("grayscale_watched_thumbnails", true)
  val showDateChip = boolean("show_date_chip", false)
  val showItemCount = boolean("show_item_count", true, "show_total_videos_chip")
  val showLocation = boolean("show_location", true, "show_playlist_location")
  val showCategory = boolean("show_category", true, "show_playlist_category")
  val showStreamDetails = boolean("show_stream_details", true, "show_playlist_stream_details")
  val audioCoverArtSize = integer("audio_cover_art_size", 48, "music_cover_art_size")

  private fun boolean(key: String, defaultValue: Boolean, legacyKey: String = key): Preference<Boolean> =
    preferenceStore.getBoolean("${prefix}_$key", defaultValue)
      .initializeFrom(preferenceStore.getBoolean(legacyKey, defaultValue))

  private fun integer(key: String, defaultValue: Int, legacyKey: String): Preference<Int> =
    preferenceStore.getInt("${prefix}_$key", defaultValue)
      .initializeFrom(preferenceStore.getInt(legacyKey, defaultValue))

  private fun <T> Preference<T>.initializeFrom(legacy: Preference<T>): Preference<T> =
    apply {
      if (!isSet()) set(legacy.get())
    }
}

enum class RecentSortType {
  LastPlayed,
  Name,
}

/** Sort fields offered on the Snapshots tab. */
enum class SnapshotSortType {
  CapturedAt,
  VideoTitle,
  Position,
}

/** Sort fields offered on the snapshot library's folder list. */
enum class SnapshotFolderSortType {
  Name,
  CreatedAt,
  ItemCount,
}

/**
 * Sort order options
 */
enum class SortOrder {
  Ascending,
  Descending,
  ;

  val isAscending: Boolean
    get() = this == Ascending
}

enum class PlaylistSortType {
  Original,
  Name,
  Location,
  DateAdded,
  LastPlayed,
  ItemCount,
  Category,
}

/**
 * Folder sorting options
 */
enum class FolderSortType {
  Title,
  Date,
  Size,
  VideoCount,
  ;

  @Suppress("DEPRECATION")
  val displayName: String
    get() =
      when (this) {
        Title -> "Title"
        Date -> "Date"
        Size -> "Size"
        VideoCount -> "Count"
      }
}

/**
 * Video sorting options
 */
enum class VideoSortType {
  Title,
  Duration,
  Date,
  Size,
  ;

  @Suppress("DEPRECATION")
  val displayName: String
    get() =
      when (this) {
        Title -> "Title"
        Duration -> "Duration"
        Date -> "Date"
        Size -> "Size"
      }
}

/**
 * Network sorting options
 */
enum class NetworkSortType {
  Title,
  Date,
  Size,
  ;

  @Suppress("DEPRECATION")
  val displayName: String
    get() =
      when (this) {
        Title -> "Title"
        Date -> "Date"
        Size -> "Size"
      }
}

/**
 * Audiobook sorting options
 */
enum class AudiobookSortType {
  Title,
  Author,
  Duration,
  Progress,
  LastPlayed,
  DateAdded,
  ;

  @Suppress("DEPRECATION")
  val displayName: String
    get() =
      when (this) {
        Title -> "Title"
        Author -> "Author"
        Duration -> "Duration"
        Progress -> "Progress"
        LastPlayed -> "Recent"
        DateAdded -> "Date Added"
      }
}

/**
 * Folder view mode options
 */
enum class FolderViewMode {
  DualHorizon,
  AlbumView,
  FileManager,
  MediaLibrary,
  ;

  val displayName: String
    get() =
      when (this) {
        DualHorizon -> "Library"
        AlbumView -> "Folder View"
        FileManager -> "Tree View"
        MediaLibrary -> "Library"
      }
}

enum class TreeFlattenDepth(
  val maxLevels: Int,
  @Suppress("DEPRECATION")
  val displayName: String,
) {
  Off(0, "Off (show every folder)"),
  One(1, "1 level"),
  Two(2, "2 levels"),
  Three(3, "3 levels"),
  Four(4, "4 levels"),
  Five(5, "5 levels"),
  Unlimited(-1, "Unlimited"),
}

enum class MediaLayoutMode {
  LIST,
  GRID,

  /**
   * Photo-wall layout: every image keeps its own aspect ratio and a wide frame spans two columns.
   * Only the snapshots' item view offers it — the enum is shared, the choices are not.
   */
  MOSAIC,
  ;

  @Suppress("DEPRECATION")
  val displayName: String
    get() =
      when (this) {
        LIST -> "List"
        GRID -> "Grid"
        MOSAIC -> "Mosaic"
      }
}

enum class MediaLibraryType {
  All,
  Video,
  Audio,
}

enum class HeroSpotlightSeekbarStyle(
  val titleRes: Int,
) {
  TimestampPill(app.video.player.R.string.pref_hero_spotlight_seekbar_style_timestamp_pill),
  Disabled(app.video.player.R.string.pref_hero_spotlight_seekbar_style_disabled),
}

enum class AudioPulsingProgressStyle(
  val titleRes: Int,
) {
  WavyVisualizer(app.video.player.R.string.pref_audio_pulsing_style_wavy),
  Seekbar(app.video.player.R.string.pref_audio_pulsing_style_seekbar),
}

enum class HomeScreenLayout(
  val titleRes: Int,
) {
  DualHorizon(app.video.player.R.string.pref_home_screen_layout_dual_horizon),
  CinematicHorizon(app.video.player.R.string.pref_home_screen_layout_cinematic),
}

enum class DualHorizonHeaderStyle(
  val titleRes: Int,
) {
  LiquidCapsule(app.video.player.R.string.pref_dual_horizon_header_style_capsule),
  CyberPillar(app.video.player.R.string.pref_dual_horizon_header_style_pillar),
  BentoStrip(app.video.player.R.string.pref_dual_horizon_header_style_card),
  FluidPulse(app.video.player.R.string.pref_dual_horizon_header_style_pulse),
  StudioLuxury(app.video.player.R.string.pref_dual_horizon_header_style_luxury),
}

enum class ThumbnailGenerator {
  Auto,
  Native,
  MediaStore,
}

enum class ThumbnailMode {
  Smart,
  FirstFrame,
  FrameAtPosition,
  EmbeddedThumbnail,
  ;

  @Suppress("DEPRECATION")
  val displayName: String
    get() =
      when (this) {
        Smart -> "Smart (embedded + 33%)"
        FirstFrame -> "First frame"
        FrameAtPosition -> "Frame position"
        EmbeddedThumbnail -> "Embedded thumbnail"
      }
}

enum class ThumbnailQuality(
  val maxSizePx: Int,
  @Suppress("DEPRECATION")
  val displayName: String,
) {
  Balanced(720, "Balanced (720p)"),
  High(1080, "High (1080p)"),
  Ultra(1440, "Ultra (1440p)"),
}
