package app.video.player.audio.autoeq

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

data class AutoEqHeadphone(
  val name: String,
  val source: String,
)

object AutoEqRepository {
  private const val INDEX_URL =
    "https://raw.githubusercontent.com/jaakkopasanen/AutoEq/master/results/INDEX.md"

  @Volatile
  private var cachedModels: List<AutoEqHeadphone>? = null

  private val linkRegex = Regex("""-\s+\[([^\]]+)\]\(([^)]+)\)""")

  suspend fun fetchModels(): Result<List<AutoEqHeadphone>> = withContext(Dispatchers.IO) {
    cachedModels?.let { return@withContext Result.success(it) }
    runCatching {
      val text = URL(INDEX_URL).readText()
      val list = text.lineSequence().mapNotNull { line ->
        val match = linkRegex.find(line) ?: return@mapNotNull null
        val name = match.groupValues[1].trim()
        val path = match.groupValues[2].trim()
        val source = path.removePrefix("./").substringBefore('/')
        AutoEqHeadphone(name = name, source = source)
      }.toList()
      cachedModels = list
      list
    }
  }
}
