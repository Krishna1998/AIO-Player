/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package app.video.player.utils.sort

import app.video.player.domain.browser.FileSystemItem
import app.video.player.domain.media.model.Video
import app.video.player.domain.media.model.VideoFolder
import app.video.player.preferences.FolderSortType
import app.video.player.preferences.SortOrder
import app.video.player.preferences.VideoSortType
import app.video.player.utils.storage.FileTypeUtils
import java.util.Locale

object SortUtils {
  /**
   * Sort videos by the specified type and order
   */
  fun sortVideos(
    videos: List<Video>,
    sortType: VideoSortType,
    sortOrder: SortOrder,
  ): List<Video> {
    val sorted =
      when (sortType) {
        VideoSortType.Title ->
          videos.sortedWith { t1, t2 ->
            val key1 = if (t1.isAudio) {
              val (artist, title) = getAudioArtistAndTitle(t1)
              if (artist.isNotBlank() && !artist.equals("Unknown Artist", ignoreCase = true)) {
                "$artist - $title"
              } else {
                title
              }
            } else {
              t1.displayName
            }
            val key2 = if (t2.isAudio) {
              val (artist, title) = getAudioArtistAndTitle(t2)
              if (artist.isNotBlank() && !artist.equals("Unknown Artist", ignoreCase = true)) {
                "$artist - $title"
              } else {
                title
              }
            } else {
              t2.displayName
            }
            NaturalOrderComparator.DEFAULT.compare(key1, key2)
          }
        VideoSortType.Duration -> videos.sortedBy { it.duration }
        VideoSortType.Date -> videos.sortedBy { it.dateModified }
        VideoSortType.Size -> videos.sortedBy { it.size }
      }
    return if (sortOrder.isAscending) sorted else sorted.reversed()
  }

  /**
   * Resolves artist and title pair for audio media.
   * Priority:
   * 1. Video model's explicit artist (from MediaStore audio tags / MusicSong)
   * 2. Parsed "Artist - Title" string from displayName or title
   * 3. Album / folder bucket name or "Unknown Artist"
   */
  fun getAudioArtistAndTitle(video: Video): Pair<String, String> {
    val rawArtist = video.artist.trim()
    val hasValidArtist = rawArtist.isNotBlank() &&
      !rawArtist.equals("<unknown>", ignoreCase = true) &&
      !rawArtist.equals("Unknown Artist", ignoreCase = true)

    if (hasValidArtist) {
      val title = if (video.title.isNotBlank()) video.title.trim() else FileTypeUtils.stripExtension(video.displayName)
      return Pair(rawArtist, title)
    }

    val cleanName = FileTypeUtils.stripExtension(video.displayName)
    if (cleanName.contains(" - ")) {
      val parts = cleanName.split(" - ", limit = 2)
      val parsedArtist = parts[0].trim()
      val parsedTitle = parts[1].trim()
      if (parsedArtist.isNotBlank() && parsedTitle.isNotBlank()) {
        return Pair(parsedArtist, parsedTitle)
      }
    }

    val title = if (video.title.isNotBlank()) video.title.trim() else cleanName
    val fallbackArtist = if (video.bucketDisplayName.isNotBlank() && !video.bucketDisplayName.equals("Audio", ignoreCase = true)) {
      video.bucketDisplayName.trim()
    } else {
      "Unknown Artist"
    }
    return Pair(fallbackArtist, title)
  }

  /**
   * Sort folders by the specified type and order
   */
  fun sortFolders(
    folders: List<VideoFolder>,
    sortType: FolderSortType,
    sortOrder: SortOrder,
  ): List<VideoFolder> {
    val sorted =
      when (sortType) {
        FolderSortType.Title ->
          folders.sortedWith(::compareVideoFoldersByTitle)
        FolderSortType.Date -> folders.sortedBy { it.lastModified }
        FolderSortType.Size -> folders.sortedBy { it.totalSize }
        FolderSortType.VideoCount -> folders.sortedBy { it.videoCount }
      }
    return if (sortOrder.isAscending) sorted else sorted.reversed()
  }

  /**
   * Sort filesystem items (folders and videos) by the specified type and order
   * Folders are always shown first, then videos
   */
  fun sortFileSystemItems(
    items: List<FileSystemItem>,
    sortType: FolderSortType,
    sortOrder: SortOrder,
  ): List<FileSystemItem> {
    // Separate folders and videos
    val folders = items.filterIsInstance<FileSystemItem.Folder>()
    val videos = items.filterIsInstance<FileSystemItem.VideoFile>()

    // Sort folders
    val sortedFolders =
      when (sortType) {
        FolderSortType.Title ->
          folders.sortedWith(::compareFileSystemFoldersByTitle)
        FolderSortType.Date -> folders.sortedBy { it.lastModified }
        FolderSortType.Size -> folders.sortedBy { it.totalSize }
        FolderSortType.VideoCount -> folders.sortedBy { it.videoCount }
      }

    // Sort videos (by corresponding properties)
    val sortedVideos =
      when (sortType) {
        FolderSortType.Title -> videos.sortedWith { t1, t2 -> NaturalOrderComparator.DEFAULT.compare(t1.name, t2.name) }
        FolderSortType.Date -> videos.sortedBy { it.lastModified }
        FolderSortType.Size -> videos.sortedBy { it.video.size }
        FolderSortType.VideoCount -> videos.sortedBy { it.video.duration } // Use duration for videos
      }

    // Apply sort order
    val orderedFolders = if (sortOrder.isAscending) sortedFolders else sortedFolders.reversed()
    val orderedVideos = if (sortOrder.isAscending) sortedVideos else sortedVideos.reversed()

    // Return folders first, then videos
    return orderedFolders + orderedVideos
  }

  class NaturalOrderComparator(
    private val ignoreCase: Boolean,
    private val shouldSkip: (Char) -> Boolean,
  ) : Comparator<String> {
    companion object {
      val DEFAULT =
        NaturalOrderComparator(
          ignoreCase = true,
          shouldSkip = { it.isWhitespace() },
        )
    }

    override fun compare(
      a: String,
      b: String,
    ): Int {
      var ia = 0
      var ib = 0

      while (true) {
        // Skip ignored characters
        while (ia < a.length && shouldSkip(a[ia])) ia++
        while (ib < b.length && shouldSkip(b[ib])) ib++

        // One or both strings ended => shorter string is smaller
        if (ia >= a.length || ib >= b.length) {
          return when {
            ia >= a.length && ib >= b.length -> 0
            ia >= a.length -> -1
            else -> 1
          }
        }

        val numA = parseNumber(a, ia)
        val numB = parseNumber(b, ib)

        when {
          numA != null && numB != null -> {
            // Both numeric
            val lengthComparison = (numA.exclusiveEndIndex - numA.significantStart)
              .compareTo(numB.exclusiveEndIndex - numB.significantStart)
            if (lengthComparison != 0) return lengthComparison
            var cmp = 0
            for (offset in 0 until numA.exclusiveEndIndex - numA.significantStart) {
              cmp = a[numA.significantStart + offset].digitToInt().compareTo(b[numB.significantStart + offset].digitToInt())
              if (cmp != 0) break
            }
            if (cmp != 0) return cmp
            // Numbers equal => advance past them and continue
            ia = numA.exclusiveEndIndex
            ib = numB.exclusiveEndIndex
          }
          else -> {
            // Compare single character
            val ca = if (ignoreCase) a[ia].lowercaseChar() else a[ia]
            val cb = if (ignoreCase) b[ib].lowercaseChar() else b[ib]
            val cmp = ca.compareTo(cb)
            if (cmp != 0) return cmp
            ia++
            ib++
          }
        }
      }
    }

    private data class ParsedNumber(
      val significantStart: Int,
      val exclusiveEndIndex: Int,
    )

    private fun parseNumber(
      s: String,
      start: Int,
    ): ParsedNumber? {
      var i = start

      var hasDigit = false

      while (i < s.length) {
        val c = s[i]
        if (c.isDigit()) {
          hasDigit = true
          i++
        } else {
          break
        }
      }

      if (!hasDigit) return null

      var significantStart = start
      while (significantStart < i && s[significantStart].digitToInt() == 0) significantStart++
      return ParsedNumber(significantStart, i)
    }
  }

  private fun folderSortGroupKey(
    name: String,
    path: String,
  ): String {
    val normalized = "$name $path".lowercase(Locale.ROOT)
    return when {
      "whatsapp" in normalized -> "whatsapp"
      else -> name.lowercase(Locale.ROOT)
    }
  }

  private fun <T> compareFoldersByTitle(
    first: T,
    second: T,
    getName: (T) -> String,
    getPath: (T) -> String,
  ): Int {
    val groupCompare =
      folderSortGroupKey(getName(first), getPath(first)).compareTo(folderSortGroupKey(getName(second), getPath(second)))
    if (groupCompare != 0) {
      return groupCompare
    }

    val nameCompare = NaturalOrderComparator.DEFAULT.compare(getName(first), getName(second))
    if (nameCompare != 0) {
      return nameCompare
    }

    return getPath(first).lowercase(Locale.ROOT).compareTo(getPath(second).lowercase(Locale.ROOT))
  }

  private fun compareVideoFoldersByTitle(
    first: VideoFolder,
    second: VideoFolder,
  ): Int = compareFoldersByTitle(first, second, VideoFolder::name, VideoFolder::path)

  private fun compareFileSystemFoldersByTitle(
    first: FileSystemItem.Folder,
    second: FileSystemItem.Folder,
  ): Int = compareFoldersByTitle(first, second, FileSystemItem.Folder::name, FileSystemItem.Folder::path)
}
