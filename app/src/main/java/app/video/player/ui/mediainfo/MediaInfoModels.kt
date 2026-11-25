package app.video.player.ui.mediainfo

data class MediaInfoUiState(
  val title: String,
  val fileName: String,
  val filePath: String,
  val fileSize: String,
  val duration: String,
  val overallBitrate: String,
  val badges: List<MediaBadge>,
  val videoTracks: List<TrackDetail>,
  val audioTracks: List<TrackDetail>,
  val subtitleTracks: List<TrackDetail>,
  val embeddedMetadata: Map<String, String> = emptyMap(),
)

data class MediaBadge(
  val label: String,
  val category: BadgeCategory,
)

enum class BadgeCategory {
  RESOLUTION,
  VIDEO_CODEC,
  HDR,
  AUDIO,
  SUBTITLE,
  SOURCE,
}

data class TrackDetail(
  val id: Int,
  val title: String,
  val language: String,
  val codecInfo: String,
  val isDefault: Boolean = false,
  val isSelected: Boolean = false,
)

