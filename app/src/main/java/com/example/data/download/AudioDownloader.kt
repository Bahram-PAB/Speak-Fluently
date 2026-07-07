package com.example.data.download

import android.content.Context
import com.example.domain.model.ExerciseFile
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AudioDownloader(
    private val context: Context,
    private val client: OkHttpClient
) {

    private val downloadsDir: File
        get() = File(context.filesDir, "audio_downloads").apply { mkdirs() }

    @Throws(IOException::class)
    fun downloadFile(file: ExerciseFile): ExerciseFile {
        val safeName = file.title.replace(Regex("[^a-zA-Z0-9\\-]"), "_")
        val localFile = File(downloadsDir, "exercise_${file.id}_$safeName.wav")

        if (localFile.exists() && localFile.length() > 0) {
            return file.copy(localFile = localFile)
        }

        val request = Request.Builder().url(file.audioUrl).build()
        val response = client.newCall(request).execute()
        try {
            if (!response.isSuccessful) {
                throw IOException("Download failed: ${response.code} ${response.message}")
            }
            val body = response.body ?: throw IOException("Empty response body")
            FileOutputStream(localFile).use { fos ->
                body.byteStream().use { bis -> bis.copyTo(fos) }
            }
        } finally {
            response.close()
        }

        return file.copy(localFile = localFile)
    }

    fun downloadExerciseFiles(files: List<ExerciseFile>): List<ExerciseFile> =
        files.map { downloadFile(it) }

    fun deleteExerciseFiles(exerciseId: Int) {
        downloadsDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("exercise_$exerciseId")) {
                file.delete()
            }
        }
    }

    fun cleanupOldFiles(maxAgeMillis: Long = 7 * 24 * 60 * 60 * 1000L) {
        val now = System.currentTimeMillis()
        downloadsDir.listFiles()?.forEach { file ->
            val match = Regex("exercise_(\\d+)_").find(file.name)
            if (match != null) {
                val exerciseId = match.groupValues[1].toIntOrNull() ?: return@forEach
                if (now - file.lastModified() > maxAgeMillis) {
                    file.delete()
                }
            }
        }
    }
}