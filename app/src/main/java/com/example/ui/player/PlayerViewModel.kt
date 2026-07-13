package com.example.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsStore
import com.example.data.remote.Banner
import com.example.data.remote.BannerFetcher
import com.example.data.repository.AudioExerciseRepository
import com.example.domain.model.Exercise
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AudioExerciseRepository.getInstance(application)
    private val settingsStore = SettingsStore(application)
    private val bannerFetcher = BannerFetcher(OkHttpClient())

    private val _exercise = MutableStateFlow<Exercise?>(null)
    val exercise: StateFlow<Exercise?> = _exercise

    private val _currentFileIndex = MutableStateFlow(0)
    val currentFileIndex: StateFlow<Int> = _currentFileIndex

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // Banners — all matching
    private val _banners = MutableStateFlow<List<Banner>>(emptyList())
    val banners: StateFlow<List<Banner>> = _banners.asStateFlow()

    // Interval timer
    private val _intervalRemaining = MutableStateFlow(0)
    val intervalRemaining: StateFlow<Int> = _intervalRemaining.asStateFlow()
    private val _isIntervalActive = MutableStateFlow(false)
    val isIntervalActive: StateFlow<Boolean> = _isIntervalActive.asStateFlow()

    private val _autoPlaySignal = MutableStateFlow(0)
    val autoPlaySignal: StateFlow<Int> = _autoPlaySignal.asStateFlow()

    fun loadExercise(exerciseId: Int) {
        viewModelScope.launch {
            val exercises = repository.getExercises().first()
            val ex = exercises.find { it.id == exerciseId }
            _exercise.value = ex
            val savedIndex = settingsStore.getPlaybackPosition(exerciseId)
            _currentFileIndex.value = savedIndex
            if (ex != null) {
                if (!ex.files.all { it.isDownloaded }) {
                    downloadFiles(ex)
                }
                // Fetch ALL matching banners for this exercise
                withContext(Dispatchers.IO) { _banners.value = bannerFetcher.fetchAll(ex.id) }
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
            startIntervalThenPlay(nextIndex)
        } else {
            _playbackState.value = PlaybackState.Completed
            viewModelScope.launch {
                repository.markCompleted(ex.id)
                settingsStore.savePlaybackPosition(ex.id, 0)
                _exercise.value = ex.copy(isCompleted = true)
            }
        }
    }

    private fun startIntervalThenPlay(nextIndex: Int) {
        viewModelScope.launch {
            val interval = settingsStore.getInterval().first()
            _isIntervalActive.value = true
            for (i in interval downTo 1) {
                _intervalRemaining.value = i
                delay(1000)
            }
            _intervalRemaining.value = 0
            _isIntervalActive.value = false
            _currentFileIndex.value = nextIndex
            _exercise.value?.let { settingsStore.savePlaybackPosition(it.id, nextIndex) }
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