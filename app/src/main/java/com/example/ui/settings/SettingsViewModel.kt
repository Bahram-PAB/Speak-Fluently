package com.example.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Settings
import com.example.domain.repository.AudioPackageRepository
import com.example.worker.PracticeScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
