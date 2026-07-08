package com.example.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsStore
import com.example.data.repository.AudioExerciseRepository
import com.example.domain.model.Exercise
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AudioExerciseRepository.getInstance(application)
    private val settingsStore = SettingsStore(application)

    private val _exercise = MutableStateFlow<Exercise?>(null)
    val exercise: StateFlow<Exercise?> = _exercise

    private val _currentFileIndex = MutableStateFlow(0)
    val currentFileIndex: StateFlow<Int> = _currentFileIndex

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // Interval timer state
    private val _intervalRemaining = MutableStateFlow(0)
    val intervalRemaining: StateFlow<Int> = _intervalRemaining.asStateFlow()

    private val _isIntervalActive = MutableStateFlow(false)
    val isIntervalActive: StateFlow<Boolean> = _isIntervalActive.asStateFlow()

    // Signal for PlayerScreen to auto-play the next file
    private val _autoPlaySignal = MutableStateFlow(0)
    val autoPlaySignal: StateFlow<Int> = _autoPlaySignal.asStateFlow()

    fun loadExercise(exerciseId: Int) {
        viewModelScope.launch {
            val exercises = repository.getExercises().first()
            val ex = exercises.find { it.id == exerciseId }
            _exercise.value = ex
            _currentFileIndex.value = 0
            if (ex != null && !ex.files.all { it.isDownloaded }) {
                downloadFiles(ex)
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
        val ex = _exercise.value ?: return
        val nextIndex = _currentFileIndex.value + 1

        if (nextIndex < ex.files.size) {
            // Start interval countdown, then auto-play next file
            startIntervalThenPlay(nextIndex)
        } else {
            // Last file completed
            _playbackState.value = PlaybackState.Completed
            viewModelScope.launch {
                repository.markCompleted(ex.id)
                _exercise.value = ex.copy(isCompleted = true)
            }
        }
    }

    private fun startIntervalThenPlay(nextIndex: Int) {
        viewModelScope.launch {
            val interval = settingsStore.getInterval().first()
            _isIntervalActive.value = true

            // Countdown
            for (i in interval downTo 1) {
                _intervalRemaining.value = i
                delay(1000)
            }
            _intervalRemaining.value = 0
            _isIntervalActive.value = false

            // Move to next file and signal auto-play
            _currentFileIndex.value = nextIndex
            _autoPlaySignal.value = _autoPlaySignal.value + 1
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