package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalSettingsDataSource
import com.example.domain.model.AudioFile
import com.example.domain.model.AudioPackage
import com.example.domain.repository.AudioPackageRepository
import com.example.domain.repository.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val packages: List<AudioPackage> = emptyList(),
    val isLoading: Boolean = true,
    val downloadStatuses: Map<String, DownloadStatus> = emptyMap(),
    val completedPackageIds: Set<String> = emptySet()
)

class HomeViewModel(
    private val repository: AudioPackageRepository,
    private val localSettings: LocalSettingsDataSource
) : ViewModel() {

    private val _downloadStatuses = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    private val _completedPackageIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getPackages(),
        _downloadStatuses,
        _completedPackageIds
    ) { packages, downloadStatuses, completedIds ->
        HomeUiState(
            packages = packages,
            isLoading = false,
            downloadStatuses = downloadStatuses,
            completedPackageIds = completedIds
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

    fun markPackageAsCompleted(packageId: String) {
        viewModelScope.launch {
            repository.markPackageCompleted(packageId)
            _completedPackageIds.value = _completedPackageIds.value + packageId
        }
    }

    companion object {
        fun provideFactory(
            repository: AudioPackageRepository,
            localSettings: LocalSettingsDataSource
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(repository, localSettings) as T
            }
        }
    }
}
