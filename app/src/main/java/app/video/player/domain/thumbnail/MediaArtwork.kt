/* SPDX-License-Identifier: AGPL-3.0-or-later */
package app.video.player.domain.thumbnail

import android.graphics.Bitmap
import android.net.Uri
import app.video.player.domain.media.model.Video
import app.video.player.ui.player.resolveLocalPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal fun isLocalArtworkSource(source: String?): Boolean {
  if (source.isNullOrBlank()) return false
  return Uri.parse(source).scheme.let { it == null || it == "file" || it == "content" }
}

/** Playback, widgets and local music use the same generator and cache as browser thumbnails. */
internal suspend fun ThumbnailRepository.getArtwork(
  source: String?,
  artworkUri: String? = null,
  isAudio: Boolean = true,
  widthPx: Int = 960,
  heightPx: Int = widthPx,
  title: String? = null,
  artist: String? = null,
): Bitmap? = withContext(Dispatchers.IO) {
  // If an explicit artworkUri is given (file://, http://, or cache URL), try decoding it directly first
  if (!artworkUri.isNullOrBlank()) {
    val nativeArtworkUri = artworkUri.takeUnless {
      source != null && isLocalArtworkSource(source) &&
        runCatching { Uri.parse(it).authority == android.provider.MediaStore.AUTHORITY }.getOrDefault(false)
    }
    if (!nativeArtworkUri.isNullOrBlank()) {
      EmbeddedArtworkResolver.decodeArtworkUri(artworkContext, nativeArtworkUri)?.let { return@withContext it }
    }
  }

  if (source.isNullOrBlank()) return@withContext null

  if (!isLocalArtworkSource(source)) {
    return@withContext EmbeddedArtworkResolver.decodeArtworkUri(artworkContext, artworkUri)
  }

  val raw = checkNotNull(source)
  val parsed = Uri.parse(raw)
  val localPath = if (parsed.scheme == "file") {
    parsed.path ?: raw
  } else if (parsed.scheme == "content") {
    runCatching { parsed.resolveLocalPath(artworkContext) }.getOrNull() ?: raw
  } else raw

  val uri = if (parsed.scheme == null) Uri.fromFile(File(localPath)) else parsed
  val fallbackName = uri.lastPathSegment.orEmpty()
  val effectiveTitle = title?.takeIf(String::isNotBlank) ?: fallbackName
  val effectiveArtist = artist?.takeIf(String::isNotBlank).orEmpty()

  val video = Video(
    id = 0L, title = effectiveTitle, displayName = effectiveTitle, path = localPath, uri = uri,
    duration = 0L, durationFormatted = "", size = 0L, sizeFormatted = "",
    dateModified = 0L, dateAdded = 0L, mimeType = if (isAudio) "audio/*" else "video/*",
    bucketId = "", bucketDisplayName = "", width = 0, height = 0,
    fps = 0f, resolution = "", isAudio = isAudio,
    artist = effectiveArtist,
  )

  if (!isAudio) {
    runCatching {
      val browserPrefs = org.koin.java.KoinJavaComponent.get<app.video.player.preferences.BrowserPreferences>(
        app.video.player.preferences.BrowserPreferences::class.java,
      )
      if (browserPrefs.autoFetchOnlinePosters.get()) {
        onlinePosterRepository.getOrFetchPoster(video, widthPx, heightPx, timeoutMs = 4000L)?.let { poster ->
          return@withContext poster
        }
      }
    }
  } else {
    runCatching {
      val browserPrefs = org.koin.java.KoinJavaComponent.get<app.video.player.preferences.BrowserPreferences>(
        app.video.player.preferences.BrowserPreferences::class.java,
      )
      if (browserPrefs.replaceEmbeddedWithOnlineMusicArtwork.get() && browserPrefs.autoFetchOnlineMusicArtwork.get()) {
        // 1. Check memory / disk cache first
        onlineMusicArtworkRepository.getCachedArtworkBitmap(source, effectiveTitle, effectiveArtist, widthPx, heightPx)?.let {
          return@withContext it
        }
        onlineMusicArtworkRepository.getCachedArtworkBitmap(video, widthPx, heightPx)?.let {
          return@withContext it
        }
        // 2. Fetch online poster with timeout
        onlineMusicArtworkRepository.getOrFetchArtwork(video, widthPx, heightPx, timeoutMs = 4000L)?.let { artwork ->
          return@withContext artwork
        }
      }
    }
  }

  val localThumbnail = getThumbnail(
    video,
    widthPx,
    heightPx,
  )
  if (localThumbnail != null) return@withContext localThumbnail

  if (isAudio) {
    runCatching {
      val browserPrefs = org.koin.java.KoinJavaComponent.get<app.video.player.preferences.BrowserPreferences>(
        app.video.player.preferences.BrowserPreferences::class.java,
      )
      if (browserPrefs.autoFetchOnlineMusicArtwork.get()) {
        onlineMusicArtworkRepository.getCachedArtworkBitmap(source, effectiveTitle, effectiveArtist, widthPx, heightPx)?.let {
          return@withContext it
        }
        onlineMusicArtworkRepository.getCachedArtworkBitmap(video, widthPx, heightPx)?.let {
          return@withContext it
        }
        onlineMusicArtworkRepository.getOrFetchArtwork(video, widthPx, heightPx, timeoutMs = 4000L)?.let { artwork ->
          return@withContext artwork
        }
      }
    }
  }

  null
}
