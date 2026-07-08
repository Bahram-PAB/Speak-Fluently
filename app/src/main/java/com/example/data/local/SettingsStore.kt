package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.settingsDataStore by preferencesDataStore(name = "speak_fluently_settings")

class SettingsStore(private val context: Context) {

    companion object {
        private val INTERVAL_KEY = intPreferencesKey("interval_seconds")
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

    suspend fun setIntervalOnce(interval: Int) {
        setInterval(interval)
    }
}
