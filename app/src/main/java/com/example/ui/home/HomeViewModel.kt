package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AudioFile
import com.example.domain.model.AudioPackage
import com.example.domain.repository.AudioPackageRepository
import com.example.domain.repository.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val packages: List<AudioPackage> = emptyList(),
    val isLoading: Boolean = true,
    val downloadStatuses: Map<String, DownloadStatus> = emptyMap(),
    val completedPackageIds: Set<String> = emptySet()
)

class HomeViewModel(private val repository: AudioPackageRepository) : ViewModel() {

    private val _downloadStatuses = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getPackages(),
        _downloadStatuses
    ) { packages, downloadStatuses ->
        HomeUiState(
            packages = packages,
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

    fun isPackageUnlocked(packageIndex: Int, completedIds: Set<String>): Boolean {
        if (packageIndex == 0) return true
        val previousPackageId = "pkg_daily_$packageIndex"
        return completedIds.contains(previousPackageId)
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
