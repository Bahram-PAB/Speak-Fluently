package com.example.ui.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AudioFile
import com.example.domain.model.AudioPackage
import com.example.domain.model.Settings
import com.example.domain.repository.AudioPackageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

sealed interface SessionState {
    object Idle : SessionState
    object Loading : SessionState
    data class PlayingAudio(val currentQuestionIndex: Int) : SessionState
    data class PracticingPause(val currentQuestionIndex: Int, val secondsRemaining: Int) : SessionState
    object Completed : SessionState
}

data class PlayerUiState(
    val sessionState: SessionState = SessionState.Idle,
    val currentPackage: AudioPackage? = null,
    val questions: List<AudioFile> = emptyList(),
    val settings: Settings = Settings(),
    val isPaused: Boolean = false
)

class PlayerViewModel(
    private val repository: AudioPackageRepository,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var timerJob: Job? = null
    private var currentQuestionIndex = 0

    val settingsState = repository.getSettings().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
    )

    fun startSession(packageId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(sessionState = SessionState.Loading) }
            
            val settings = repository.getSettings().first()
            val audioPackage = repository.getPackageById(packageId).first()
            
            if (audioPackage == null) {
                _uiState.update { it.copy(sessionState = SessionState.Idle) }
                return@launch
            }

            // Shuffle and take questions based on settings count
            val shuffledQuestions = audioPackage.files.shuffled()
                .take(settings.questionsPerSession)

            _uiState.update {
                it.copy(
                    currentPackage = audioPackage,
                    questions = shuffledQuestions,
                    settings = settings,
                    sessionState = SessionState.PlayingAudio(0),
                    isPaused = false
                )
            }
            
            currentQuestionIndex = 0
            playQuestion(0)
        }
    }

    private fun playQuestion(index: Int) {
        timerJob?.cancel()
        releasePlayer()

        val state = _uiState.value
        if (index >= state.questions.size) {
            finishSession()
            return
        }

        currentQuestionIndex = index
        _uiState.update {
            it.copy(
                sessionState = SessionState.PlayingAudio(index),
                isPaused = false
            )
        }

        val question = state.questions[index]

        try {
            mediaPlayer = MediaPlayer().apply {
                val localPath = question.localPath
                val mediaUri = if (localPath != null && File(localPath).exists()) {
                    Uri.fromFile(File(localPath))
                } else {
                    Uri.parse(question.audioUrl)
                }
                setDataSource(appContext, mediaUri)

                prepareAsync()
                setOnPreparedListener { mp ->
                    if (!_uiState.value.isPaused) {
                        mp.start()
                    }
                }
                setOnCompletionListener {
                    // Audio finished -> trigger dynamic pause for practice speaking
                    startPauseCountdown()
                }
                setOnErrorListener { _, _, _ ->
                    // Fallback in case audio playback crashes on unsupported file format or offline
                    startPauseCountdown()
                    true
                }
            }
        } catch (e: Exception) {
            // Safe fallback to countdown if initialization crashes
            startPauseCountdown()
        }
    }

    private fun startPauseCountdown() {
        releasePlayer()
        timerJob?.cancel()
        
        val duration = _uiState.value.settings.pauseDurationSeconds
        
        _uiState.update {
            it.copy(
                sessionState = SessionState.PracticingPause(currentQuestionIndex, duration),
                isPaused = false
            )
        }

        timerJob = viewModelScope.launch {
            var remaining = duration
            while (remaining > 0) {
                delay(1000)
                if (!_uiState.value.isPaused) {
                    remaining--
                    _uiState.update { state ->
                        if (state.sessionState is SessionState.PracticingPause) {
                            state.copy(sessionState = SessionState.PracticingPause(currentQuestionIndex, remaining))
                        } else {
                            state
                        }
                    }
                }
            }
            // Transition to next question
            playQuestion(currentQuestionIndex + 1)
        }
    }

    fun pauseSession() {
        if (_uiState.value.isPaused) return
        
        _uiState.update { it.copy(isPaused = true) }
        
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (ignored: Exception) {}
    }

    fun resumeSession() {
        if (!_uiState.value.isPaused) return
        
        _uiState.update { it.copy(isPaused = false) }
        
        try {
            mediaPlayer?.start()
        } catch (ignored: Exception) {}
    }

    fun skipCurrent() {
        timerJob?.cancel()
        releasePlayer()
        
        val state = _uiState.value
        val currentState = state.sessionState
        
        if (currentState is SessionState.PlayingAudio) {
            // Skip directly to practice pause
            startPauseCountdown()
        } else if (currentState is SessionState.PracticingPause) {
            // Skip directly to next question
            playQuestion(currentState.currentQuestionIndex + 1)
        }
    }

    fun finishSession() {
        timerJob?.cancel()
        releasePlayer()
        val currentPkgId = _uiState.value.currentPackage?.id
        if (currentPkgId != null) {
            viewModelScope.launch {
                try {
                    repository.markPackageCompleted(currentPkgId)
                } catch (ignored: Exception) {}
            }
        }
        _uiState.update {
            it.copy(sessionState = SessionState.Completed)
        }
    }

    fun resetSession() {
        timerJob?.cancel()
        releasePlayer()
        _uiState.update {
            it.copy(
                sessionState = SessionState.Idle,
                currentPackage = null,
                questions = emptyList()
            )
        }
        currentQuestionIndex = 0
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.stop()
        } catch (ignored: Exception) {}
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        releasePlayer()
    }

    companion object {
        fun provideFactory(
            repository: AudioPackageRepository,
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlayerViewModel(repository, context.applicationContext) as T
            }
        }
    }
}
