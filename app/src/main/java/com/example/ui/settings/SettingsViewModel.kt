package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalSettingsDataSource
import com.example.domain.model.Settings
import com.example.domain.repository.AudioPackageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DownloadProgressState {
    object Idle : DownloadProgressState
    object CheckingAccess : DownloadProgressState
    data class AccessSuccess(val message: String) : DownloadProgressState
    data class Downloading(val progress: Int, val currentFileIndex: Int, val totalFiles: Int) : DownloadProgressState
    data class Finished(val successCount: Int, val failedCount: Int, val message: String) : DownloadProgressState
    data class Error(val message: String) : DownloadProgressState
}

class SettingsViewModel(
    private val repository: AudioPackageRepository,
    private val localSettings: LocalSettingsDataSource
) : ViewModel() {

    val settingsState: StateFlow<Settings> = localSettings.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
    )

    private val _downloadState = MutableStateFlow<DownloadProgressState>(DownloadProgressState.Idle)
    val downloadState: StateFlow<DownloadProgressState> = _downloadState

    fun checkAndDownloadAll(githubRepo: String) {
        viewModelScope.launch {
            _downloadState.value = DownloadProgressState.CheckingAccess
            try {
                val result = repository.checkGithubAccess(githubRepo)
                if (result != null) {
                    _downloadState.value = DownloadProgressState.Error(result)
                    return@launch
                }
                _downloadState.value = DownloadProgressState.AccessSuccess("Repository access confirmed. Starting download...")
                downloadAllFiles()
            } catch (e: Exception) {
                _downloadState.value = DownloadProgressState.Error("Network error: ${e.message}")
            }
        }
    }

    private fun downloadAllFiles() {
        viewModelScope.launch {
            val packages = repository.getPackages().first()
            val allFiles = packages.flatMap { it.files }
            val totalFiles = allFiles.size
            var successCount = 0
            var failedCount = 0

            allFiles.forEachIndexed { index, file ->
                repository.downloadFile(file, force = false).collect { status ->
                    when (status) {
                        is DownloadStatus.Success -> {
                            successCount++
                            _downloadState.value = DownloadProgressState.Downloading(
                                progress = ((index + 1) * 100 / totalFiles).coerceAtMost(100),
                                currentFileIndex = index + 1,
                                totalFiles = totalFiles
                            )
                        }
                        is DownloadStatus.Error -> {
                            failedCount++
                        }
                        else -> {
                            _downloadState.value = DownloadProgressState.Downloading(
                                progress = ((index + 1) * 100 / totalFiles).coerceAtMost(100),
                                currentFileIndex = index + 1,
                                totalFiles = totalFiles
                            )
                        }
                    }
                }
            }

            val message = if (failedCount == 0) "All files downloaded successfully" else "$successCount downloaded, $failedCount failed"
            _downloadState.value = DownloadProgressState.Finished(successCount, failedCount, message)
        }
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadProgressState.Idle
    }

    suspend fun saveSettings(settings: Settings) {
        localSettings.saveSettings(settings)
    }

    companion object {
        fun provideFactory(
            repository: AudioPackageRepository,
            localSettings: LocalSettingsDataSource
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(repository, localSettings) as T
            }
        }
    }
}