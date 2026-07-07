package com.example.ui.player
import kotlinx.coroutines.flow.first

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AudioExerciseRepository
import com.example.domain.model.Exercise
import com.example.domain.model.ExerciseFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AudioExerciseRepository.getInstance(application)

    private val _exercise = MutableStateFlow<Exercise?>(null)
    val exercise: StateFlow<Exercise?> = _exercise

    private val _currentFileIndex = MutableStateFlow(0)
    val currentFileIndex: StateFlow<Int> = _currentFileIndex

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun loadExercise(exerciseId: Int) {
        viewModelScope.launch {
            val exercises = repository.getExercises().first()
            val exercise = exercises.find { it.id == exerciseId }
            _exercise.value = exercise
            _currentFileIndex.value = 0
            if (exercise != null && !exercise.files.all { it.isDownloaded }) {
                downloadFiles(exercise)
            }
        }
    }

    private fun downloadFiles(exercise: Exercise) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading
            repository.downloadExercise(exercise)
                .onSuccess { files ->
                    _downloadState.value = DownloadState.Success
                    _exercise.value = exercise.copy(files = files)
                }
                .onFailure { error ->
                    _downloadState.value = DownloadState.Error(error.message ?: "خطا در دانلود")
                }
        }
    }

    fun onFileComplete() {
        val exercise = _exercise.value ?: return
        val nextIndex = _currentFileIndex.value + 1
        if (nextIndex < exercise.files.size) {
            _currentFileIndex.value = nextIndex
        } else {
            _playbackState.value = PlaybackState.Completed
            viewModelScope.launch {
                repository.markCompleted(exercise.id)
                _exercise.value = _exercise.value?.copy(isCompleted = true)
            }
        }
    }

    fun setPlaying() { _playbackState.value = PlaybackState.Playing }
    fun setPaused() { _playbackState.value = PlaybackState.Paused }
    fun setIdle() { _playbackState.value = PlaybackState.Idle }

    enum class PlaybackState { Idle, Playing, Paused, Completed }
    sealed class DownloadState {
        data object Idle : DownloadState()
        data object Downloading : DownloadState()
        data object Success : DownloadState()
        data class Error(val message: String) : DownloadState()
    }
}