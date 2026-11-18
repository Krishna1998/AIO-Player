/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package app.video.player.ui.browser.music

import android.content.Context
import androidx.compose.runtime.Immutable
import app.video.player.database.dao.RecentlyPlayedDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

@Immutable
data class SongPlayStat(
  val playCount: Int = 0,
  val lastPlayedTimestamp: Long = 0L,
)

/**
 * Tracks listening statistics (play count and last played timestamp) for songs.
 * Powers the "Jump Back In" hero carousel to dynamically surface most listened songs,
 * while seamlessly falling back to default library order if no songs have been listened to yet.
 */
object SongPlayCountTracker {
  private const val PREFS_NAME = "audio_song_play_counts"
  private const val KEY_PREFIX_COUNT = "count_"
  private const val KEY_PREFIX_TIME = "time_"

  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  @Volatile private var isInitialized = false

  private val _stats = MutableStateFlow<Map<String, SongPlayStat>>(emptyMap())
  val stats: StateFlow<Map<String, SongPlayStat>> = _stats.asStateFlow()

  private var lastRecordedPath: String? = null
  private var lastRecordedTimeMs: Long = 0L

  fun init(context: Context) {
    if (isInitialized) return
    isInitialized = true
    scope.launch {
      loadStats(context.applicationContext)
    }
  }

  private fun loadStats(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val map = mutableMapOf<String, SongPlayStat>()

    prefs.all.forEach { (key, value) ->
      if (key.startsWith(KEY_PREFIX_COUNT) && value is Int) {
        val path = key.removePrefix(KEY_PREFIX_COUNT)
        val time = prefs.getLong(KEY_PREFIX_TIME + path, 0L)
        map[path] = SongPlayStat(playCount = value, lastPlayedTimestamp = time)
      }
    }

    // Seed from RecentlyPlayedDao history for existing user listens
    runCatching {
      val recentlyPlayedDao: RecentlyPlayedDao by inject(RecentlyPlayedDao::class.java)
      val recentList = kotlinx.coroutines.runBlocking { recentlyPlayedDao.getRecentlyPlayed(limit = 200) }
      for (entity in recentList) {
        val existing = map[entity.filePath]
        if (existing == null) {
          map[entity.filePath] = SongPlayStat(playCount = 1, lastPlayedTimestamp = entity.timestamp)
        } else if (existing.lastPlayedTimestamp < entity.timestamp) {
          map[entity.filePath] = existing.copy(lastPlayedTimestamp = entity.timestamp)
        }
      }
    }

    _stats.value = map
  }

  fun recordPlay(context: Context, filePath: String?) {
    if (filePath.isNullOrBlank()) return
    val now = System.currentTimeMillis()
    if (filePath == lastRecordedPath && now - lastRecordedTimeMs < 3000L) {
      return
    }
    lastRecordedPath = filePath
    lastRecordedTimeMs = now

    val appContext = context.applicationContext
    scope.launch {
      val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val currentCount = prefs.getInt(KEY_PREFIX_COUNT + filePath, 0)
      val newCount = currentCount + 1

      prefs.edit()
        .putInt(KEY_PREFIX_COUNT + filePath, newCount)
        .putLong(KEY_PREFIX_TIME + filePath, now)
        .apply()

      val currentMap = _stats.value.toMutableMap()
      currentMap[filePath] = SongPlayStat(playCount = newCount, lastPlayedTimestamp = now)
      _stats.value = currentMap
    }
  }

  /**
   * Returns songs for the "Jump Back In" hero carousel:
   * - When user has listened to songs: displays most listened songs (highest play count, then latest timestamp).
   *   If fewer than [limit] have been listened to, backfills remaining slots with unplayed library songs.
   * - If the user has NOT listened to any songs yet: preserves the exact default library order (`allSongs.take(limit)`).
   */
  fun getHeroSongs(allSongs: List<MusicSong>, limit: Int = 8): List<MusicSong> {
    if (allSongs.isEmpty()) return emptyList()

    val currentStats = _stats.value
    val listenedSongs = allSongs.filter { song ->
      val stat = currentStats[song.path]
      stat != null && stat.playCount > 0
    }.sortedWith(
      compareByDescending<MusicSong> { currentStats[it.path]?.playCount ?: 0 }
        .thenByDescending { currentStats[it.path]?.lastPlayedTimestamp ?: 0L }
    )

    return if (listenedSongs.isNotEmpty()) {
      if (listenedSongs.size >= limit) {
        listenedSongs.take(limit)
      } else {
        val listenedPaths = listenedSongs.mapTo(HashSet()) { it.path }
        val remaining = allSongs.filter { it.path !in listenedPaths }
        listenedSongs + remaining.take(limit - listenedSongs.size)
      }
    } else {
      allSongs.take(limit)
    }
  }

  fun hasListenedSongs(allSongs: List<MusicSong>): Boolean {
    val currentStats = _stats.value
    return allSongs.any { song ->
      val stat = currentStats[song.path]
      stat != null && stat.playCount > 0
    }
  }

  fun getPlayCount(filePath: String): Int {
    return _stats.value[filePath]?.playCount ?: 0
  }
}

