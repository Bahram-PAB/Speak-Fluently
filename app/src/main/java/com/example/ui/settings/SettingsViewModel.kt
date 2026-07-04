package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalSettingsDataSource
import com.example.domain.model.Settings
import com.example.domain.repository.AudioPackageRepository
import com.example.domain.repository.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    fun saveSettings(settings: Settings) {
        viewModelScope.launch {
            localSettings.saveSettings(settings)
        }
    }

    fun checkAndDownloadAll(githubRepo: String) {
        viewModelScope.launch {
            _downloadState.value = DownloadProgressState.CheckingAccess
            try {
                val result = repository.checkGithubAccess(githubRepo)
                if (result != null) {
                    _downloadState.value = DownloadProgressState.Error(result)
                    return@launch
                }
                _downloadState.value = DownloadProgressState.AccessSuccess("دسترسی به مخزن تایید شد. شروع دریافت فایل‌ها...")
                downloadAllFiles()
            } catch (e: Exception) {
                _downloadState.value = DownloadProgressState.Error("خطای شبکه: ${e.message}")
            }
        }
    }

    private suspend fun downloadAllFiles() {
        val packages = repository.getPackages().first()
        val allFiles = packages.flatMap { it.files }
        val totalFiles = allFiles.size
        if (totalFiles == 0) {
            _downloadState.value = DownloadProgressState.Finished(0, 0, "فایلی برای دریافت یافت نشد")
            return
        }
        var successCount = 0
        var failedCount = 0

        allFiles.forEachIndexed { index, file ->
            repository.downloadFile(file, force = false).collect { status ->
                when (status) {
                    is DownloadStatus.Success -> successCount++
                    is DownloadStatus.Error -> failedCount++
                    else -> {}
                }
            }
            _downloadState.value = DownloadProgressState.Downloading(
                progress = ((index + 1) * 100 / totalFiles).coerceAtMost(100),
                currentFileIndex = index + 1,
                totalFiles = totalFiles
            )
        }

        val message = if (failedCount == 0) "تمام فایل‌ها با موفقیت دریافت شدند" else "$successCount فایل دریافت شد، $failedCount فایل با خطا مواجه شد"
        _downloadState.value = DownloadProgressState.Finished(successCount, failedCount, message)
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadProgressState.Idle
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
