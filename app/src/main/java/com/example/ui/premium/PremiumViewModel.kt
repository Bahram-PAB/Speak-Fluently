package com.example.ui.premium

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.PremiumStatus
import com.example.domain.repository.AudioPackageRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ActivationResult {
    object Idle : ActivationResult
    object Success : ActivationResult
    object Failure : ActivationResult
}

class PremiumViewModel(private val repository: AudioPackageRepository) : ViewModel() {

    val premiumStatusState: StateFlow<PremiumStatus> = repository.getPremiumStatus()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PremiumStatus()
        )

    private val _activationResult = MutableSharedFlow<ActivationResult>()
    val activationResult: SharedFlow<ActivationResult> = _activationResult.asSharedFlow()

    fun activatePremium(code: String) {
        viewModelScope.launch {
            val isSuccess = repository.activatePremium(code)
            if (isSuccess) {
                _activationResult.emit(ActivationResult.Success)
            } else {
                _activationResult.emit(ActivationResult.Failure)
            }
        }
    }

    companion object {
        fun provideFactory(repository: AudioPackageRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PremiumViewModel(repository) as T
                }
            }
    }
}
