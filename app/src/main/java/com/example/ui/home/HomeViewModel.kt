package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.Lang
import com.example.data.repository.AudioExerciseRepository
import com.example.domain.model.Exercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AudioExerciseRepository.getInstance(application)

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getExercises().collect { exercises ->
                _exercises.value = exercises
            }
        }
        viewModelScope.launch { repository.initialize() }
    }

    fun sync() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            repository.sync()
                .onSuccess { count ->
                    val msg = if (Lang.current == Lang.Language.EN)
                        "Sync completed. $count exercises found."
                    else
                        "همگام‌سازی انجام شد. $count تمرین یافت شد."
                    _syncState.value = SyncState.Success(msg)
                }
                .onFailure { error ->
                    val msg = if (Lang.current == Lang.Language.EN)
                        "Sync error: ${error.message}"
                    else
                        "خطا در همگام‌سازی: ${error.message}"
                    _syncState.value = SyncState.Error(msg)
                }
        }
    }

    sealed class SyncState {
        data object Idle : SyncState()
        data object Syncing : SyncState()
        data class Success(val message: String) : SyncState()
        data class Error(val message: String) : SyncState()
    }
}