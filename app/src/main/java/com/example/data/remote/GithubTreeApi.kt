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
            .header("User-Agent", "SpeakFluently")
            .build()

        val response = client.newCall(request).execute()
        try {
            if (!response.isSuccessful) {
                throw IOException("GitHub API failed: ${response.code} ${response.message}")
            }
            val bodyString = response.body?.string() ?: ""
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

                val segments = path.split("/")
                if (segments.size < 4) continue
                if (!path.startsWith(pathPrefix)) continue

                val folderName = segments[2]
                val fileName = segments[3]

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

    private fun extractFileNumber(fileName: String): Int? {
        val nameWithoutExt = fileName.substringBeforeLast(".")
        val parts = nameWithoutExt.split("_")
        return parts.firstOrNull()?.toIntOrNull()
    }

    private fun extractTitle(fileName: String): String {
        val nameWithoutExt = fileName.substringBeforeLast(".")
        val parts = nameWithoutExt.split("_", limit = 2)
        return if (parts.size >= 2) {
            parts[1].replace("_", " ").replace("-", " ")
        } else {
            nameWithoutExt.replace("_", " ").replace("-", " ")
        }
    }
}