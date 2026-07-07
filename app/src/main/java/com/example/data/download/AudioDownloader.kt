package com.example.data.download

import android.content.Context
import com.example.domain.model.ExerciseFile
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * دانلود فایلهای صوتی به صورت on-demand
 */
class AudioDownloader(
    private val context: Context,
    private val client: OkHttpClient
) {

    private val downloadsDir: File
        get() = File(context.filesDir, "audio_downloads").apply { mkdirs() }

    /**
     * دانلود یک فایل صوتی
     * @param file فایل صوتی برای دانلود
     * @return فایل محلی دانلود شده
     */
    @Throws(IOException::class)
    suspend fun downloadFile(file: ExerciseFile): ExerciseFile {
        val localFile = File(downloadsDir, "exercise_${file.id}_${file.title}.wav")

        // اگر قبلاً دانلود شده، برگردان
        if (localFile.exists() && localFile.length() > 0) {
            return file.copy(localFile = localFile)
        }

        val request = Request.Builder().url(file.audioUrl).build()
        val response = client.newCall(request).execute()
        use(response) { resp ->
            if (!resp.isSuccessful) {
                throw IOException("Download failed: ${resp.code} ${resp.message}")
            }
            val body = resp.body ?: throw IOException("Empty response body")
            FileOutputStream(localFile).use { fos ->
                body.byteStream().use { bis -> bis.copyTo(fos) }
            }
        }

        return file.copy(localFile = localFile)
    }

    /**
     * دانلود تمام فایلهای یک تمرین
     */
    suspend fun downloadExerciseFiles(files: List<ExerciseFile>): List<ExerciseFile> = files.map { downloadFile(it) }

    /**
     * حذف فایلهای یک تمرین (برای پاکسازی خودکار)
     */
    fun deleteExerciseFiles(exerciseId: Int) {
        downloadsDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("exercise_$exerciseId")) {
                file.delete()
            }
        }
    }

    /**
     * حذف فایلهای تمام تمرینهای تکمیل شده که ۷ روز گذشته
     */
    fun cleanupOldFiles(completedExercises: Map<Int, Long>, maxAgeMillis: Long = 7 * 24 * 60 * 60 * 1000L) {
        val now = System.currentTimeMillis()
        downloadsDir.listFiles()?.forEach { file ->
            // استخراج exerciseId از نام فایل
            val match = file.name.match(Regex("exercise_(\\d+)_"))
            if (match) {
                val exerciseId = match.groupValues[1].toIntOrNull() ?: return@forEach
                val completedAt = completedExercises[exerciseId] ?: return@forEach
                if (now - completedAt > maxAgeMillis) {
                    file.delete()
                }
            }
        }
    }
}