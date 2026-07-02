package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.domain.model.PremiumStatus
import com.example.domain.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "speak_fluently_preferences")

class LocalSettingsDataSource(private val context: Context) {

    companion object {
        private val KEY_NOTIFICATION_TIME = stringPreferencesKey("notification_time")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_QUESTIONS_PER_SESSION = intPreferencesKey("questions_per_session")
        private val KEY_PAUSE_DURATION = intPreferencesKey("pause_duration")
        private val KEY_GITHUB_AUDIO_REPO = stringPreferencesKey("github_audio_repo")
        private val KEY_GITHUB_BRANCH = stringPreferencesKey("github_branch")
        private val KEY_GITHUB_PATH_PREFIX = stringPreferencesKey("github_path_prefix")
        
        private val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
        private val KEY_ACTIVATION_CODE = stringPreferencesKey("activation_code")
        
        private val KEY_PLAYED_FILES = stringSetPreferencesKey("played_files")
    }

    val playedFilesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_PLAYED_FILES] ?: emptySet()
    }

    suspend fun markFileAsPlayed(fileId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_PLAYED_FILES] ?: emptySet()
            preferences[KEY_PLAYED_FILES] = current + fileId
        }
    }

    suspend fun clearPlayedFiles() {
        context.dataStore.edit { preferences ->
            preferences[KEY_PLAYED_FILES] = emptySet()
        }
    }

    val settingsFlow: Flow<Settings> = context.dataStore.data.map { preferences ->
        Settings(
            dailyNotificationTime = preferences[KEY_NOTIFICATION_TIME] ?: "09:00",
            notificationsEnabled = preferences[KEY_NOTIFICATIONS_ENABLED] ?: true,
            appLanguage = preferences[KEY_APP_LANGUAGE] ?: "fa", // Persian is default
            questionsPerSession = preferences[KEY_QUESTIONS_PER_SESSION] ?: 5,
            pauseDurationSeconds = preferences[KEY_PAUSE_DURATION] ?: 20,
            githubAudioRepo = preferences[KEY_GITHUB_AUDIO_REPO] ?: "username/speakfluently-audio",
            githubBranch = preferences[KEY_GITHUB_BRANCH] ?: "main",
            githubPathPrefix = preferences[KEY_GITHUB_PATH_PREFIX] ?: "packages"
        )
    }

    suspend fun saveSettings(settings: Settings) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_TIME] = settings.dailyNotificationTime
            preferences[KEY_NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
            preferences[KEY_APP_LANGUAGE] = settings.appLanguage
            preferences[KEY_QUESTIONS_PER_SESSION] = settings.questionsPerSession
            preferences[KEY_PAUSE_DURATION] = settings.pauseDurationSeconds
            preferences[KEY_GITHUB_AUDIO_REPO] = settings.githubAudioRepo
            preferences[KEY_GITHUB_BRANCH] = settings.githubBranch
            preferences[KEY_GITHUB_PATH_PREFIX] = settings.githubPathPrefix
        }
    }

    val premiumStatusFlow: Flow<PremiumStatus> = context.dataStore.data.map { preferences ->
        PremiumStatus(
            isPremium = preferences[KEY_IS_PREMIUM] ?: false,
            activationCode = preferences[KEY_ACTIVATION_CODE]
        )
    }

    suspend fun savePremiumStatus(premiumStatus: PremiumStatus) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_PREMIUM] = premiumStatus.isPremium
            if (premiumStatus.activationCode != null) {
                preferences[KEY_ACTIVATION_CODE] = premiumStatus.activationCode
            } else {
                preferences.remove(KEY_ACTIVATION_CODE)
            }
        }
    }
}
