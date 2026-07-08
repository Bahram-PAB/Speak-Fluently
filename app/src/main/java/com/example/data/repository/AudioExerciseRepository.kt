package com.example.data.repository

import android.app.Application
import com.example.SpeakFluentlyApplication
import com.example.data.download.AudioDownloader
import com.example.data.local.CompletedPackagesStore
import com.example.data.local.SyncPreferences
import com.example.data.remote.GithubTreeApi
import com.example.domain.model.Exercise
import com.example.domain.model.ExerciseFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioExerciseRepository(
    private val githubApi: GithubTreeApi,
    private val downloader: AudioDownloader,
    private val completedStore: CompletedPackagesStore,
    private val syncPrefs: SyncPreferences
) {
    // Use MutableStateFlow to hold cached exercises and emit changes
    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises = _exercises.asStateFlow()

    companion object {
        @Volatile private var INSTANCE: AudioExerciseRepository? = null
        fun getInstance(application: Application): AudioExerciseRepository {
            return INSTANCE ?: synchronized(this) {
                val container = (application as SpeakFluentlyApplication).container
                INSTANCE ?: AudioExerciseRepository(
                    container.githubApi, container.audioDownloader,
                    container.completedStore, container.syncPrefs
                ).also { INSTANCE = it }
            }
        }
    }

    fun getExercises(): kotlinx.coroutines.flow.StateFlow<List<Exercise>> {
        // Combine cached exercises with completed status
        val completedFlow = completedStore.getCompletedExercises()
            .map { it.toSet() }
            .distinctUntilChanged()
        
        return combine(_exercises, completedFlow) { exercises, completed ->
            exercises.map { it.copy(isCompleted = completed.contains(it.id)) }
        }
            .distinctUntilChanged()
            .stateIn(
                scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Main),
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(),
                initialValue = emptyList()
            )
    }

    suspend fun sync(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val exercises = githubApi.fetchExercises()
            _exercises.value = exercises  // This triggers the flow update
            syncPrefs.setLastSyncTime(System.currentTimeMillis())
            Result.success(exercises.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadExercise(exercise: Exercise): Result<List<ExerciseFile>> = withContext(Dispatchers.IO) {
        try {
            val downloaded = downloader.downloadExerciseFiles(exercise.files)
            _exercises.value = _exercises.value.map {
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

    fun getLastSyncTime(): kotlinx.coroutines.flow.Flow<Long> = syncPrefs.getLastSyncTime()

    suspend fun initialize() {
        val lastSync = syncPrefs.getLastSyncTime().first()
        if (lastSync > 0) { sync() }
    }
}
