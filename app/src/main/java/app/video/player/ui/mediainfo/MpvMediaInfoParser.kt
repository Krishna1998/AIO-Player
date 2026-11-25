package app.video.player.ui.mediainfo

import app.video.player.ui.player.PlaybackSession
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

object MpvMediaInfoParser {

  fun extractFromActivePlayer(filePath: String): MediaInfoUiState {
    val file = File(filePath)
    val mediaTitle = PlaybackSession.getPropertyString("media-title")?.takeIf { it.isNotBlank() }
      ?: file.nameWithoutExtension
    val fileName = PlaybackSession.getPropertyString("filename")?.takeIf { it.isNotBlank() }
      ?: file.name

    val fileSizeBytes = PlaybackSession.getPropertyString("file-size")?.toLongOrNull() ?: 0L
    val durationSec = PlaybackSession.getPropertyDouble("duration") ?: 0.0
    val bitRateBps = PlaybackSession.getPropertyString("video-bitrate")?.toLongOrNull()
      ?: calculateAverageBitrate(fileSizeBytes, durationSec)

    val trackListJson = PlaybackSession.getPropertyNode("track-list")?.toJson() ?: "[]"
    val videoParamsJson = PlaybackSession.getPropertyNode("video-params")?.toJson() ?: "{}"
    val audioParamsJson = PlaybackSession.getPropertyNode("audio-params")?.toJson() ?: "{}"
    val metadataJson = PlaybackSession.getPropertyNode("metadata")?.toJson() ?: "{}"

    val videoParams = runCatching { JSONObject(videoParamsJson) }.getOrDefault(JSONObject())
    val audioParams = runCatching { JSONObject(audioParamsJson) }.getOrDefault(JSONObject())
    val trackArray = runCatching { JSONArray(trackListJson) }.getOrDefault(JSONArray())

    val videoTracks = mutableListOf<TrackDetail>()
    val audioTracks = mutableListOf<TrackDetail>()
    val subtitleTracks = mutableListOf<TrackDetail>()

    var activeVideoObj: JSONObject? = null
    var activeAudioObj: JSONObject? = null

    for (i in 0 until trackArray.length()) {
      val track = trackArray.optJSONObject(i) ?: continue
      val type = track.optString("type")
      val id = track.optInt("id", i + 1)
      val title = track.optString("title", "")
      val lang = formatLanguageCode(track.optString("lang", "und"))
      val codec = track.optString("codec", "unknown").uppercase()
      val isDefault = track.optBoolean("default", false)
      val isSelected = track.optBoolean("selected", false)

      when (type) {
        "video" -> {
          if (isSelected || activeVideoObj == null) activeVideoObj = track
          val w = track.optInt("demux-w", videoParams.optInt("w", 0))
          val h = track.optInt("demux-h", videoParams.optInt("h", 0))
          val fps = track.optDouble("demux-fps", 0.0)
          val fpsText = if (fps > 0) String.format(Locale.US, " \u2022 %.3f fps", fps) else ""
          val resText = if (w > 0 && h > 0) " \u2022 ${w}x$h" else ""

          videoTracks.add(
            TrackDetail(
              id = id,
              title = title.ifBlank { "Video Track $id" },
              language = lang,
              codecInfo = "$codec$resText$fpsText",
              isDefault = isDefault,
              isSelected = isSelected,
            ),
          )
        }

        "audio" -> {
          if (isSelected || activeAudioObj == null) activeAudioObj = track
          val channels = track.optString("audio-channels", audioParams.optString("channels", ""))
          val sampleRate = track.optInt("demux-samplerate", audioParams.optInt("samplerate", 0))
          val srText = if (sampleRate > 0) " \u2022 ${sampleRate / 1000} kHz" else ""
          val chText = if (channels.isNotBlank()) " \u2022 $channels" else ""

          audioTracks.add(
            TrackDetail(
              id = id,
              title = title.ifBlank { lang },
              language = lang,
              codecInfo = "$codec$chText$srText",
              isDefault = isDefault,
              isSelected = isSelected,
            ),
          )
        }

        "sub" -> {
          subtitleTracks.add(
            TrackDetail(
              id = id,
              title = title.ifBlank { lang },
              language = lang,
              codecInfo = mapSubtitleCodecName(codec),
              isDefault = isDefault,
              isSelected = isSelected,
            ),
          )
        }
      }
    }

    val badges = buildFormatBadges(
      fileName = fileName,
      videoParams = videoParams,
      audioParams = audioParams,
      activeVideoTrack = activeVideoObj,
      activeAudioTrack = activeAudioObj,
      subtitleTracks = subtitleTracks,
    )

    val embeddedMetadata = parseEmbeddedMetadata(metadataJson)

    return MediaInfoUiState(
      title = mediaTitle,
      fileName = fileName,
      filePath = filePath,
      fileSize = formatFileSize(fileSizeBytes),
      duration = formatDuration(durationSec),
      overallBitrate = formatBitrate(bitRateBps),
      badges = badges,
      videoTracks = videoTracks,
      audioTracks = audioTracks,
      subtitleTracks = subtitleTracks,
      embeddedMetadata = embeddedMetadata,
    )
  }

  private fun buildFormatBadges(
    fileName: String,
    videoParams: JSONObject,
    audioParams: JSONObject,
    activeVideoTrack: JSONObject?,
    activeAudioTrack: JSONObject?,
    subtitleTracks: List<TrackDetail>,
  ): List<MediaBadge> {
    val badges = mutableListOf<MediaBadge>()

    val ext = fileName.substringAfterLast('.', "").uppercase()
    if (ext.isNotBlank() && ext.length <= 5) {
      badges.add(MediaBadge(ext, BadgeCategory.SOURCE))
    }
    detectReleaseSource(fileName)?.let { sourceTag ->
      badges.add(MediaBadge(sourceTag, BadgeCategory.SOURCE))
    }

    val width = videoParams.optInt("w", activeVideoTrack?.optInt("demux-w", 0) ?: 0)
    val height = videoParams.optInt("h", activeVideoTrack?.optInt("demux-h", 0) ?: 0)
    detectResolutionLabel(width, height)?.let { res ->
      badges.add(MediaBadge(res, BadgeCategory.RESOLUTION))
    }

    val rawVideoCodec = activeVideoTrack?.optString("codec", "")?.lowercase() ?: ""
    mapVideoCodecLabel(rawVideoCodec)?.let { codecLabel ->
      badges.add(MediaBadge(codecLabel, BadgeCategory.VIDEO_CODEC))
    }

    val pixelFormat = videoParams.optString("pixelformat", "")
    if (pixelFormat.contains("10") || pixelFormat.contains("p010")) {
      badges.add(MediaBadge("10-BIT", BadgeCategory.VIDEO_CODEC))
    } else if (pixelFormat.contains("12")) {
      badges.add(MediaBadge("12-BIT", BadgeCategory.VIDEO_CODEC))
    }

    val primaries = videoParams.optString("primaries", "")
    val gamma = videoParams.optString("gamma", "")
    val dolbyVisionProfile = videoParams.optInt("dv-profile", -1)
    val hdrPeak = videoParams.optDouble("sig-peak", 0.0)

    when {
      dolbyVisionProfile >= 0 || fileName.contains("DV", ignoreCase = true) ||
        fileName.contains("Dolby.Vision", ignoreCase = true) -> {
        val dvLabel = if (dolbyVisionProfile > 0) "Dolby Vision P$dolbyVisionProfile" else "Dolby Vision"
        badges.add(MediaBadge(dvLabel, BadgeCategory.HDR))
      }
      fileName.contains("HDR10+", ignoreCase = true) -> {
        badges.add(MediaBadge("HDR10+", BadgeCategory.HDR))
      }
      gamma == "pq" || (primaries == "bt.2020" && hdrPeak > 1.0) -> {
        badges.add(MediaBadge("HDR10", BadgeCategory.HDR))
      }
      gamma == "hlg" -> {
        badges.add(MediaBadge("HLG", BadgeCategory.HDR))
      }
    }

    val rawAudioCodec = activeAudioTrack?.optString("codec", "")?.lowercase() ?: ""
    val audioTitle = activeAudioTrack?.optString("title", "") ?: ""
    val channels = activeAudioTrack?.optString("audio-channels", audioParams.optString("channels", "")) ?: ""

    detectAudioBadge(rawAudioCodec, audioTitle, fileName)?.let { audioLabel ->
      badges.add(MediaBadge(audioLabel, BadgeCategory.AUDIO))
    }
    formatChannelLayout(channels)?.let { chBadge ->
      badges.add(MediaBadge(chBadge, BadgeCategory.AUDIO))
    }

    subtitleTracks
      .map { it.codecInfo }
      .distinct()
      .take(3)
      .forEach { subBadge ->
        badges.add(MediaBadge(subBadge, BadgeCategory.SUBTITLE))
      }

    return badges
  }

  private fun detectResolutionLabel(w: Int, h: Int): String? = when {
    w >= 7600 || h >= 4300 -> "8K"
    w >= 3800 || h >= 2100 -> "4K"
    w >= 2500 || h >= 1400 -> "1440p"
    w >= 1900 || h >= 1000 -> "1080p"
    w >= 1200 || h >= 700 -> "720p"
    h > 0 -> "${h}p"
    else -> null
  }

  private fun mapVideoCodecLabel(codec: String): String? = when {
    codec.contains("hevc") || codec.contains("h265") -> "HEVC"
    codec.contains("h264") || codec.contains("avc") -> "AVC"
    codec.contains("av1") -> "AV1"
    codec.contains("vp9") -> "VP9"
    codec.contains("mpeg2") -> "MPEG-2"
    codec.isNotBlank() -> codec.uppercase()
    else -> null
  }

  private fun detectAudioBadge(codec: String, trackTitle: String, fileName: String): String? {
    val isAtmos = trackTitle.contains("atmos", ignoreCase = true) ||
      fileName.contains("atmos", ignoreCase = true)

    return when {
      codec.contains("truehd") -> if (isAtmos) "TrueHD Atmos" else "TrueHD"
      codec.contains("eac3") -> if (isAtmos) "DD+ Atmos" else "Dolby Digital+"
      codec.contains("ac3") -> "Dolby Digital"
      codec.contains("dts") -> when {
        trackTitle.contains("dts:x", true) || fileName.contains("dts-x", true) -> "DTS:X"
        trackTitle.contains("ma", true) || fileName.contains("dts-hd", true) -> "DTS-HD MA"
        else -> "DTS"
      }
      codec.contains("aac") -> "AAC"
      codec.contains("flac") -> "FLAC"
      codec.contains("opus") -> "OPUS"
      codec.isNotBlank() -> codec.uppercase()
      else -> null
    }
  }

  private fun formatChannelLayout(raw: String): String? = when {
    raw.contains("7.1") -> "7.1"
    raw.contains("5.1") -> "5.1"
    raw.contains("stereo") || raw == "2" -> "2.0"
    raw.contains("mono") || raw == "1" -> "1.0"
    raw.isNotBlank() -> raw.uppercase()
    else -> null
  }

  private fun mapSubtitleCodecName(codec: String): String = when (codec.lowercase()) {
    "subrip", "srt" -> "SRT"
    "ass", "ssa" -> "ASS"
    "hdmv_pgs_subtitle", "pgs" -> "PGS"
    "dvd_subtitle", "vobsub" -> "VobSub"
    "webvtt", "vtt" -> "WebVTT"
    "mov_text" -> "TX3G"
    else -> codec.uppercase()
  }

  private fun detectReleaseSource(fileName: String): String? = when {
    fileName.contains("remux", ignoreCase = true) -> "REMUX"
    fileName.contains("web-dl", ignoreCase = true) || fileName.contains("webdl", ignoreCase = true) -> "WEB-DL"
    fileName.contains("webrip", ignoreCase = true) -> "WEBRip"
    fileName.contains("bluray", ignoreCase = true) || fileName.contains("blu-ray", ignoreCase = true) -> "Blu-ray"
    fileName.contains("imax", ignoreCase = true) -> "IMAX"
    else -> null
  }

  private fun parseEmbeddedMetadata(metadataJson: String): Map<String, String> {
    val obj = runCatching { JSONObject(metadataJson) }.getOrDefault(JSONObject())
    val map = linkedMapOf<String, String>()
    val allowedKeys = listOf(
      "title" to "Title",
      "artist" to "Artist",
      "album" to "Album",
      "album_artist" to "Album Artist",
      "genre" to "Genre",
      "date" to "Date",
      "track" to "Track",
      "encoder" to "Encoder",
    )
    for ((jsonKey, displayLabel) in allowedKeys) {
      val value = obj.optString(jsonKey).ifBlank { obj.optString(jsonKey.uppercase()) }
      if (value.isNotBlank()) {
        map[displayLabel] = value
      }
    }
    return map
  }

  private fun formatLanguageCode(code: String): String {
    if (code.isBlank() || code == "und") return "Unknown"
    return runCatching {
      Locale.forLanguageTag(code).getDisplayLanguage(Locale.ENGLISH)
        .replaceFirstChar { it.uppercase() }
    }.getOrDefault(code.uppercase())
  }

  private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "\u2014"
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    if (gb >= 1.0) return String.format(Locale.US, "%.2f GB", gb)
    val mb = bytes / (1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f MB", mb)
  }

  private fun formatDuration(seconds: Double): String {
    if (seconds <= 0) return "\u2014"
    val totalSec = seconds.toLong()
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
  }

  private fun calculateAverageBitrate(bytes: Long, durationSec: Double): Long {
    if (bytes <= 0 || durationSec <= 0) return 0L
    return ((bytes * 8) / durationSec).toLong()
  }

  private fun formatBitrate(bps: Long): String {
    if (bps <= 0) return "\u2014"
    val mbps = bps / 1_000_000.0
    return if (mbps >= 1.0) String.format(Locale.US, "%.1f Mbps", mbps)
    else "${bps / 1000} kbps"
  }
}

