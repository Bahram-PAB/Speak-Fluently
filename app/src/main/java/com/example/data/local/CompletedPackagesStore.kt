package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * ذخیره وضعیت تکمیل تمرینها
 */
class CompletedPackagesStore(private val context: Context) {
    private val Context.dataStore by preferencesDataStore(name = "completed_packages")
    private val dataStore = context.dataStore

    companion object {
        private fun completedKey(exerciseId: Int) = booleanPreferencesKey("completed_$exerciseId")
        private fun completedAtKey(exerciseId: Int) = longPreferencesKey("completed_at_$exerciseId")
    }

    /** دریافت لیست تمرینهای تکمیل شده */
    fun getCompletedExercises(): Flow<Set<Int>> = dataStore.data
        .map { prefs ->
            prefs.asMap().keys
                .filter { it.name.startsWith("completed_") }
                .mapNotNull { it.name.substringAfter("completed_").toIntOrNull() }
                .toSet()
        }

    /** دریافت زمان تکمیل یک تمرین */
    fun getCompletionTime(exerciseId: Int): Flow<Long?> = dataStore.data
        .map { prefs -> prefs[completedAtKey(exerciseId)] }

    /** علامت گذاری تمرین به عنوان تکمیل شده */
    suspend fun markAsCompleted(exerciseId: Int) {
        val now = System.currentTimeMillis()
        dataStore.edit { prefs ->
            prefs[completedKey(exerciseId)] = true
            prefs[completedAtKey(exerciseId)] = now
        }
    }

    /** پاک کردن وضعیت تکمیل تمرین (برای پاکسازی خودکار) */
    suspend fun clearCompletion(exerciseId: Int) {
        dataStore.edit { prefs ->
            prefs.remove(completedKey(exerciseId))
            prefs.remove(completedAtKey(exerciseId))
        }
    }
}

/**
 * ذخیره تنظیمات همگامسازی
 */
class SyncPreferences(private val context: Context) {
    private val Context.dataStore by preferencesDataStore(name = "sync_prefs")
    private val dataStore = context.dataStore

    companion object {
        private val LAST_SYNC_KEY = longPreferencesKey("last_sync_timestamp")
    }

    /** دریافت آخرین زمان همگامسازی */
    fun getLastSyncTime(): Flow<Long> = dataStore.data
        .map { prefs -> prefs[LAST_SYNC_KEY] ?: 0L }

    /** ذخیره آخرین زمان همگامسازی */
    suspend fun setLastSyncTime(timestamp: Long) {
        dataStore.edit { prefs -> prefs[LAST_SYNC_KEY] = timestamp }
    }
}