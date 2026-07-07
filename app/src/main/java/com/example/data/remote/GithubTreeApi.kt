package com.example.data.remote

import com.example.SyncConfig
import com.example.domain.model.Exercise
import com.example.domain.model.ExerciseFile
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * دریافت ساختار فولدرها و فایلهای صوتی از GitHub Tree API
 */
class GithubTreeApi(private val client: OkHttpClient) {

    /**
     * دریافت لیست تمرینها از GitHub
     * @param repo مخزن (مثلاً "Bahram-PAB/Speak-Fluently")
     * @param branch برنچ (مثلاً "main")
     * @param pathPrefix پیشوند مسیر (مثلاً "audio/daily")
     * @return لیست تمرینها
     */
    @Throws(IOException::class)
    fun fetchExercises(
        repo: String = SyncConfig.GITHUB_REPO,
        branch: String = SyncConfig.GITHUB_BRANCH,
        pathPrefix: String = SyncConfig.AUDIO_PATH_PREFIX
    ): List<Exercise> {
        val treeUrl = "https://api.github.com/repos/$repo/git/trees/$branch?recursive=1"
        val request = Request.Builder()
            .url(treeUrl)
            .header("User-Agent", "SpeakFluently")
            .build()

        val response = client.newCall(request).execute()
        use(response) { resp ->
            if (!resp.isSuccessful) {
                throw IOException("GitHub API failed: ${resp.code} ${resp.message}")
            }
            val bodyString = resp.body?.string() ?: ""
            if (bodyString.isEmpty()) return emptyList()

            val json = JSONObject(bodyString)
            val treeArray = json.optJSONArray("tree") ?: return emptyList()

            // گروه‌بندی فایلها بر اساس شماره فولدر
            val folderMap = mutableMapOf<Int, MutableList<ExerciseFile>>()

            for (i in 0 until treeArray.length()) {
                val item = treeArray.getJSONObject(i)
                if (item.optString("type", "") != "blob") continue

                val path = item.optString("path", "")
                val lowerPath = path.lowercase()
                if (!lowerPath.endsWith(".wav") && !lowerPath.endsWith(".mp3") &&
                    !lowerPath.endsWith(".m4a") && !lowerPath.endsWith(".ogg")) continue

                // ساختار: audio/daily/01/001_title.wav
                val segments = path.split("/")
                if (segments.size != 3) continue
                if (segments[0] != pathPrefix) continue

                val folderName = segments[1]
                val fileName = segments[2]

                val folderNum = folderName.toIntOrNull() ?: continue
                val fileNum = extractFileNumber(fileName) ?: continue

                val title = extractTitle(fileName)
                val audioUrl = "https://raw.githubusercontent.com/$repo/$branch/$path"
                val exerciseFile = ExerciseFile(
                    id = fileNum,
                    title = title,
                    audioUrl = audioUrl
                )

                folderMap.getOrPut(folderNum) { mutableListOf() }.add(exerciseFile)
            }

            // تبدیل به لیست تمرینها، مرتب شده بر اساس شماره فولدر
            return folderMap.toSortedMap().map { (folderNum, files) ->
                Exercise(
                    id = folderNum,
                    name = "تمرین روز $folderNum",
                    files = files.sortedBy { it.id }
                )
            }
        }
    }

    private fun extractFileNumber(fileName: String): Int? {
        // فایلهای مثل: 001_title.wav, 002_question.mp3
        val nameWithoutExt = fileName.substringBeforeLast(".")
        val parts = nameWithoutExt.split("_")
        if (parts.isNotEmpty()) {
            return parts[0].toIntOrNull()
        }
        return null
    }

    private fun extractTitle(fileName: String): String {
        val nameWithoutExt = fileName.substringBeforeLast(".")
        val parts = nameWithoutExt.split("_", limit = 2)
        if (parts.size >= 2) {
            return parts[1].replace("_", " ").replace("-", " ")
        }
        return nameWithoutExt.replace("_", " ").replace("-", " ")
    }
}