package com.example.data.remote

import com.example.SyncConfig
import com.example.domain.model.Exercise
import com.example.domain.model.ExerciseFile
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class GithubTreeApi(private val client: OkHttpClient) {

    @Throws(IOException::class)
    fun fetchExercises(
        repo: String = SyncConfig.GITHUB_REPO,
        branch: String = SyncConfig.GITHUB_BRANCH,
        pathPrefix: String = SyncConfig.AUDIO_PATH_PREFIX
    ): List<Exercise> {
        val treeUrl = "https://api.github.com/repos/$repo/git/trees/$branch?recursive=1"
        
        val request = Request.Builder()
            .url(treeUrl)
            .header("User-Agent", "SpeakFluently-App")
            .build()

        val response = client.newCall(request).execute()
        
        try {
            if (!response.isSuccessful) {
                throw IOException("GitHub API failed: ${response.code} ${response.message}")
            }

            val bodyString = response.body?.string() ?: return emptyList()
            if (bodyString.isEmpty()) return emptyList()

            val json = JSONObject(bodyString)
            val treeArray = json.optJSONArray("tree") ?: return emptyList()

            val folderMap = mutableMapOf<Int, MutableList<ExerciseFile>>()

            for (i in 0 until treeArray.length()) {
                val item = treeArray.getJSONObject(i)
                if (item.optString("type", "") != "blob") continue

                val path = item.optString("path", "")
                val lowerPath = path.lowercase()

                if (!lowerPath.endsWith(".wav") && !lowerPath.endsWith(".mp3") &&
                    !lowerPath.endsWith(".m4a") && !lowerPath.endsWith(".ogg")) continue

                if (!path.startsWith(pathPrefix)) continue

                val segments = path.split("/")
                
                // Find first numeric segment (folder number)
                val folderIndex = segments.indexOfFirst { segment -> segment.all { it in '0'..'9' } && segment.isNotEmpty() }
                if (folderIndex == -1) continue

                val folderName = segments[folderIndex]
                val fileName = segments.last()

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

            return folderMap.toSortedMap().map { (folderNum, files) ->
                Exercise(
                    id = folderNum,
                    name = "تمرین روز $folderNum",
                    files = files.sortedBy { it.id }
                )
            }

        } finally {
            response.close()
        }
    }

    /** Extract first number from filename: "speech-1.wav" -> 1, "01_hello.wav" -> 1 */
    private fun extractFileNumber(fileName: String): Int? {
        val nameWithoutExt = fileName.substringBeforeLast(".")
        var i = 0
        while (i < nameWithoutExt.length) {
            if (nameWithoutExt[i].isDigit()) {
                var end = i
                while (end < nameWithoutExt.length && nameWithoutExt[end].isDigit()) {
                    end++
                }
                return nameWithoutExt.substring(i, end).toIntOrNull()
            }
            i++
        }
        return null
    }

    /** Extract title: "speech-1.wav" -> "speech 1", "01_hello.wav" -> "hello" */
    private fun extractTitle(fileName: String): String {
        val nameWithoutExt = fileName.substringBeforeLast(".")
        val cleaned = nameWithoutExt
            .replace("_", " ")
            .replace("-", " ")
        return cleaned.trim()
    }
}
