package com.example.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Settings
import com.example.domain.repository.AudioPackageRepository
import com.example.worker.PracticeScheduler
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
    object Finished : DownloadProgressState
    data class Error(val message: String) : DownloadProgressState
}

class SettingsViewModel(
    private val repository: AudioPackageRepository,
    private val appContext: Context
) : ViewModel() {

    val settingsState: StateFlow<Settings> = repository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Settings()
        )

    private val _downloadState = MutableStateFlow<DownloadProgressState>(DownloadProgressState.Idle)
    val downloadState: StateFlow<DownloadProgressState> = _downloadState.asStateFlow()

    fun saveSettings(settings: Settings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
            
            // Re-schedule background practice reminder with WorkManager
            PracticeScheduler.scheduleDailyReminder(
                context = appContext,
                timeString = settings.dailyNotificationTime,
                enabled = settings.notificationsEnabled
            )
        }
    }

    fun checkAndDownloadAll(repo: String) {
        viewModelScope.launch {
            _downloadState.value = DownloadProgressState.CheckingAccess
            val hasAccess = repository.checkGithubAccess(repo)
            if (!hasAccess) {
                _downloadState.value = DownloadProgressState.Error("عدم دسترسی به ریپازیتوری یا نامعتبر بودن مسیر. لطفاً از عمومی بودن مخزن و صحت نام کاربری/نام مخزن مطمئن شوید.")
                return@launch
            }

            _downloadState.value = DownloadProgressState.AccessSuccess("اتصال موفقیت‌آمیز بود. در حال آماده‌سازی دریافت...")
            kotlinx.coroutines.delay(1200)

            try {
                val packages = repository.getPackages().first()
                val allFiles = packages.flatMap { it.files }
                val totalCount = allFiles.size
                
                if (totalCount == 0) {
                    _downloadState.value = DownloadProgressState.Finished
                    return@launch
                }

                _downloadState.value = DownloadProgressState.Downloading(0, 0, totalCount)

                var completedCount = 0
                for (file in allFiles) {
                    repository.downloadFile(file).collect { status ->
                        when (status) {
                            is com.example.domain.repository.DownloadStatus.Success -> {
                                completedCount++
                                val percentage = (completedCount * 100) / totalCount
                                _downloadState.value = DownloadProgressState.Downloading(percentage, completedCount, totalCount)
                            }
                            is com.example.domain.repository.DownloadStatus.Error -> {
                                // Keep downloading other files even if one fails
                                completedCount++
                                val percentage = (completedCount * 100) / totalCount
                                _downloadState.value = DownloadProgressState.Downloading(percentage, completedCount, totalCount)
                            }
                            else -> {}
                        }
                    }
                }
                
                _downloadState.value = DownloadProgressState.Finished
            } catch (e: Exception) {
                _downloadState.value = DownloadProgressState.Error("خطا در جریان دانلود: ${e.localizedMessage}")
            }
        }
    }

    fun resetDownloadState() {
        _downloadState.value = DownloadProgressState.Idle
    }

    companion object {
        fun provideFactory(
            repository: AudioPackageRepository,
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(repository, context.applicationContext) as T
            }
        }
    }
}
