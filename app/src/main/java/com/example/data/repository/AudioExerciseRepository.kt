package com.example.data.repository

import android.app.Application
import com.example.SyncConfig
import com.example.SpeakFluentlyApplication
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

    fun getExercises(): Flow<List<Exercise>> {
    fun getExercises(): Flow<List<Exercise>> {
        return combine<List<Int>, List<Exercise>>(
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

    suspend fun downloadExercise(exercise: Exercise): Result<List<ExerciseFile>> = withContext(Dispatchers.IO) {
        try {
            val downloaded = downloader.downloadExerciseFiles(exercise.files)
            // Update cache
            cachedExercises = cachedExercises.map {
                if (it.id == exercise.id) it.copy(files = downloaded) else it
            }
            Result.success(downloaded)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markCompleted(exerciseId: Int) {
        completedStore.markAsCompleted(exerciseId)
    }

    suspend fun cleanupOldFiles() = withContext(Dispatchers.IO) {
        downloader.cleanupOldFiles()
    }

    fun getLastSyncTime(): Flow<Long> = syncPrefs.getLastSyncTime()

    suspend fun initialize() {
        val lastSync = syncPrefs.getLastSyncTime().first()
        if (lastSync > 0) {
            sync()
        }
    }
}