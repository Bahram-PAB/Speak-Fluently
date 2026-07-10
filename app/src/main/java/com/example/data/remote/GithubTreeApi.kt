package com.example.data.remote

import com.example.SyncConfig
import com.example.domain.model.Exercise
import com.example.domain.model.ExerciseFile
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class GithubTreeApi(private val client: OkHttpClient) {

    /**
     * Fetch exercises from index.json on the configured host.
     * Any audio file format is accepted (wav, mp3, m4a, ogg, flac, etc.)
     * File naming is completely flexible - no naming convention required.
     */
    @Throws(IOException::class)
    fun fetchExercises(): List<Exercise> {
        val request = Request.Builder()
            .url(SyncConfig.INDEX_JSON_URL)
            .header("User-Agent", "SpeakFluently-App")
            .build()

        val response = client.newCall(request).execute()
        try {
            if (!response.isSuccessful) {
                throw IOException("Host API failed: ${response.code} ${response.message}")
            }

            val bodyString = response.body?.string() ?: return emptyList()
            if (bodyString.isEmpty()) return emptyList()

            return parseIndexJson(bodyString)
        } finally {
            response.close()
        }
    }

    private fun parseIndexJson(jsonString: String): List<Exercise> {
        val json = JSONObject(jsonString)
        val exercisesArray = json.optJSONArray("exercises") ?: return emptyList()
        val result = mutableListOf<Exercise>()

        for (i in 0 until exercisesArray.length()) {
            val exerciseObj = exercisesArray.getJSONObject(i)
            val id = exerciseObj.optInt("id", i + 1)
            val name = exerciseObj.optString("name", "تمرین روز $id")
            val filesArray = exerciseObj.optJSONArray("files") ?: continue

            val files = mutableListOf<ExerciseFile>()
            for (j in 0 until filesArray.length()) {
                val fileObj = filesArray.getJSONObject(j)
                val fileId = fileObj.optInt("id", j + 1)
                val title = fileObj.optString("title", "فایل $fileId")
                val url = fileObj.optString("url", "")

                if (url.isNotEmpty()) {
                    files.add(ExerciseFile(
                        id = fileId,
                        title = title,
                        audioUrl = url
                    ))
                }
            }

            if (files.isNotEmpty()) {
                result.add(Exercise(
                    id = id,
                    name = name,
                    files = files.sortedBy { it.id }
                ))
            }
        }

        return result.sortedBy { it.id }
    }
}