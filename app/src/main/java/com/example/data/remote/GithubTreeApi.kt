package com.example.data.remote

import com.example.SyncConfig
import com.example.Lang
import com.example.domain.model.Exercise
import com.example.domain.model.ExerciseFile
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class GithubTreeApi(private val client: OkHttpClient) {

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
            val rawName = exerciseObj.optString("name", "")
            val name = translateExerciseName(rawName, id)
            val filesArray = exerciseObj.optJSONArray("files") ?: continue

            val files = mutableListOf<ExerciseFile>()
            for (j in 0 until filesArray.length()) {
                val fileObj = filesArray.getJSONObject(j)
                val fileId = fileObj.optInt("id", j + 1)
                val rawTitle = fileObj.optString("title", "")
                val title = translateFileTitle(rawTitle, fileId)
                val url = fileObj.optString("url", "")

                if (url.isNotEmpty()) {
                    files.add(ExerciseFile(id = fileId, title = title, audioUrl = url))
                }
            }

            if (files.isNotEmpty()) {
                result.add(Exercise(id = id, name = name, files = files.sortedBy { it.id }))
            }
        }
        return result.sortedBy { it.id }
    }

    /**
     * Translate "تمرین روز 5" → "Day 5" (when English), keep as-is for Persian.
     * Pattern: "تمرین روز {number}" ↔ "Day {number}"
     */
    private fun translateExerciseName(raw: String, id: Int): String {
        if (Lang.current == Lang.Language.FA) return raw.ifEmpty { "تمرین روز $id" }
        // English: extract number from Persian pattern or use id
        val num = extractNumber(raw) ?: id
        return "Day $num"
    }

    /**
     * Translate "سوال 3" → "Question 3" (when English), keep as-is for Persian.
     * Pattern: "سوال {number}" ↔ "Question {number}"
     */
    private fun translateFileTitle(raw: String, id: Int): String {
        if (Lang.current == Lang.Language.FA) return raw.ifEmpty { "فایل $id" }
        val num = extractNumber(raw) ?: id
        return "Question $num"
    }

    /** Extract the last number from a string like "تمرین روز 5" → 5 */
    private fun extractNumber(s: String): Int? {
        val match = Regex("\\d+").findAll(s).lastOrNull()
        return match?.value?.toIntOrNull()
    }
}