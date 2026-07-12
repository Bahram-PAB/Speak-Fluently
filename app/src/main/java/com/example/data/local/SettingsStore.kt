package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.Lang
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.settingsDataStore by preferencesDataStore(name = "speak_fluently_settings")

class SettingsStore(private val context: Context) {

    companion object {
        private val INTERVAL_KEY = intPreferencesKey("interval_seconds")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
        const val DEFAULT_INTERVAL = 30
        val INTERVAL_OPTIONS = listOf(20, 30, 45, 60)
    }

    fun getInterval(): Flow<Int> {
        return context.settingsDataStore.data.map { preferences ->
            preferences[INTERVAL_KEY] ?: DEFAULT_INTERVAL
        }
    }

    suspend fun setInterval(interval: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[INTERVAL_KEY] = interval
        }
    }

    fun getLanguage(): Flow<String> {
        return context.settingsDataStore.data.map { preferences ->
            preferences[LANGUAGE_KEY] ?: Lang.Language.FA.code
        }
    }

    suspend fun setLanguage(code: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = code
        }
        Lang.current = Lang.Language.entries.find { it.code == code } ?: Lang.Language.FA
    }

    suspend fun setIntervalOnce(interval: Int) {
        setInterval(interval)
    }

    suspend fun applySavedLanguage() {
        val code = context.settingsDataStore.data.first()[LANGUAGE_KEY] ?: Lang.Language.FA.code
        Lang.current = Lang.Language.entries.find { it.code == code } ?: Lang.Language.FA
    }
}