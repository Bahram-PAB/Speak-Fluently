package com.example.data.repository

import com.example.SyncConfig
import com.example.data.download.AudioDownloader
import com.example.data.local.CompletedPackagesStore
import com.example.data.local.SyncPreferences
import com.example.data.remote.GithubTreeApi
import com.example.domain.model.Exercise
import com.example.domain.model.ExerciseFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AudioExerciseRepository(
    private val githubApi: GithubTreeApi,
    private val downloader: AudioDownloader,
    private val completedStore: CompletedPackagesStore,
    private val syncPrefs: SyncPreferences
) {

    private var cachedExercises: List<Exercise> = emptyList()

    /**
     * Singleton instance for easy access
     */
    companion object {
        @Volatile private var INSTANCE: AudioExerciseRepository? = null

        fun getInstance(application: Application): AudioExerciseRepository {
            return INSTANCE ?: synchronized(this) {
                val container = (application as SpeakFluentlyApplication).container
                INSTANCE ?: AudioExerciseRepository(
                    container.githubApi,
                    container.audioDownloader,
                    container.completedStore,
                    container.syncPrefs
                ).also { INSTANCE = it }
            }
        }
    }

    /**
     * دریافت لیست تمرینها با وضعیت تکمیل
     */
    fun getExercises(): Flow<List<Exercise>> {
        return combine(
            completedStore.getCompletedExercises()
        ) { completedIds ->
            if (cachedExercises.isEmpty()) {
                emptyList()
            } else {
                val completed = completedIds.toSet()
                cachedExercises.map { exercise ->
                    exercise.copy(isCompleted = completed.contains(exercise.id))
                }
            }
        }
    }

    /**
     * همگامسازی: خواندن لیست فایلها از GitHub
     */
    suspend fun sync(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val exercises = githubApi.fetchExercises()
            cachedExercises = exercises
            syncPrefs.setLastSyncTime(System.currentTimeMillis())
            Result.success(exercises.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * دانلود فایلهای یک تمرین
     */
    suspend fun downloadExercise(exercise: Exercise): Result<List<ExerciseFile>> = withContext(Dispatchers.IO) {
        try {
            val downloaded = downloader.downloadExerciseFiles(exercise.files)
            // آپدیت کش
            cachedExercises = cachedExercises.map {
                if (it.id == exercise.id) it.copy(files = downloaded) else it
            }
            Result.success(downloaded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * علامت گذاری تمرین به عنوان تکمیل شده
     */
    suspend fun markCompleted(exerciseId: Int) {
        completedStore.markAsCompleted(exerciseId)
    }

    /**
     * پاکسازی فایلهای قدیمی (بیش از ۷ روز)
     */
    suspend fun cleanupOldFiles() = withContext(Dispatchers.IO) {
        // اینجا باید زمان تکمیل رو بگیریم
        // ساده‌سازی: حذف فایلهای قدیمی بر اساس سن فایل
        val maxAge = 7 * 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()
        downloader.cleanupOldFiles(emptyMap(), maxAge)
    }

    /**
     * دریافت زمان آخرین همگامسازی
     */
    fun getLastSyncTime(): Flow<Long> = syncPrefs.getLastSyncTime()

    /**
     * مقداردهی اولیه لیست تمرینها (اگر قبلاً sync شده)
     */
    suspend fun initialize() {
        val lastSync = syncPrefs.getLastSyncTime().first()
        if (lastSync > 0) {
            sync() // تلاش برای دریافت مجدد
        }
    }
}