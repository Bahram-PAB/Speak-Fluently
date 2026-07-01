package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AudioFile
import com.example.domain.model.AudioPackage
import com.example.domain.model.PremiumStatus
import com.example.domain.repository.AudioPackageRepository
import com.example.domain.repository.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val packages: List<AudioPackage> = emptyList(),
    val premiumStatus: PremiumStatus = PremiumStatus(),
    val isLoading: Boolean = true,
    val downloadStatuses: Map<String, DownloadStatus> = emptyMap()
)

class HomeViewModel(private val repository: AudioPackageRepository) : ViewModel() {

    private val _downloadStatuses = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getPackages(),
        repository.getPremiumStatus(),
        _downloadStatuses
    ) { packages, premiumStatus, downloadStatuses ->
        HomeUiState(
            packages = packages,
            premiumStatus = premiumStatus,
            isLoading = false,
            downloadStatuses = downloadStatuses
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun downloadFile(file: AudioFile) {
        viewModelScope.launch {
            repository.downloadFile(file).collect { status ->
                _downloadStatuses.value = _downloadStatuses.value.toMutableMap().apply {
                    put(file.id, status)
                }
            }
        }
    }

    companion object {
        fun provideFactory(repository: AudioPackageRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(repository) as T
                }
            }
    }
}
