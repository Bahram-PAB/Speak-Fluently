package com.example.data.repository

import android.content.Context
import com.example.data.local.LocalSettingsDataSource
import com.example.data.remote.RemoteAudioPackageDataSource
import com.example.domain.repository.DownloadStatus
import com.example.domain.repository.AudioPackageRepository
import com.example.domain.model.AudioFile
import com.example.domain.model.AudioPackage
import com.example.domain.model.PremiumStatus
import com.example.domain.model.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.edit
import com.example.data.local.dataStore
import java.io.File

class AudioPackageRepositoryImpl(
    private val context: Context,
    private val localSettings: LocalSettingsDataSource,
    private val remotePackages: RemoteAudioPackageDataSource
) : AudioPackageRepository {

    private val downloadsDir = File(context.filesDir, "audio_downloads").apply {
        if (!exists()) mkdirs()
    }

    override fun getPackages(): Flow<List<AudioPackage>> = flow {
        // Trigger automated clean up of packages older than 1 week
        try {
            cleanUpCompletedPackages()
        } catch (ignored: Exception) {}

        val currentSettings = localSettings.settingsFlow.first()
        val repo = currentSettings.githubAudioRepo
        val baseUrl = "https://raw.githubusercontent.com/$repo/main/packages"

        val remoteMetadata = remotePackages.getRemotePackagesMetadata()
        
        val mappedList = remoteMetadata.map { pkg ->
            val updatedFiles = pkg.files.map { file ->
                val relativePath = file.audioUrl.substringAfter("main/packages/")
                val updatedUrl = "$baseUrl/$relativePath"
                val ext = relativePath.substringAfterLast(".", "wav")
                val localFile = File(downloadsDir, "${file.id}.$ext")
                
                val resolvedLocalPath = if (localFile.exists() && localFile.length() > 0L) {
                    localFile.absolutePath
                } else {
                    null
                }
                file.copy(
                    audioUrl = updatedUrl,
                    localPath = resolvedLocalPath
                )
            }
            pkg.copy(files = updatedFiles)
        }
        emit(mappedList)
    }.flowOn(Dispatchers.IO)

    override fun getPackageById(id: String): Flow<AudioPackage?> = getPackages().map { list ->
        list.find { it.id == id }
    }

    override fun downloadFile(file: AudioFile): Flow<DownloadStatus> = flow {
        val relativePath = file.audioUrl.substringAfter("main/packages/")
        val ext = relativePath.substringAfterLast(".", "wav")
        val localFile = File(downloadsDir, "${file.id}.$ext")

        emit(DownloadStatus.Progress(10))
        if (localFile.exists() && localFile.length() > 0L) {
            emit(DownloadStatus.Success(localFile.absolutePath))
            return@flow
        }

        emit(DownloadStatus.Progress(40))
        try {
            val downloadedFile = remotePackages.downloadUrlToFile(file.audioUrl, localFile, file.text)
            emit(DownloadStatus.Progress(100))
            emit(DownloadStatus.Success(downloadedFile.absolutePath))
        } catch (e: Exception) {
            emit(DownloadStatus.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun getSettings(): Flow<Settings> {
        return localSettings.settingsFlow
    }

    override suspend fun saveSettings(settings: Settings) {
        localSettings.saveSettings(settings)
    }

    override fun getPremiumStatus(): Flow<PremiumStatus> {
        return localSettings.premiumStatusFlow
    }

    override suspend fun activatePremium(code: String): Boolean {
        // Simple valid activation code for our demo and future readiness
        val sanitized = code.trim().uppercase()
        val isValid = sanitized == "SPEAK2026" || sanitized == "FLUENT" || sanitized == "FREEPASS"
        if (isValid) {
            localSettings.savePremiumStatus(PremiumStatus(isPremium = true, activationCode = sanitized))
        }
        return isValid
    }

    override suspend fun checkGithubAccess(repo: String): Boolean = withContext(Dispatchers.IO) {
        val testUrl = "https://raw.githubusercontent.com/$repo/main/packages/daily/speech-1.wav"
        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder().url(testUrl).head().build()
        try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun markPackageCompleted(packageId: String) {
        val key = androidx.datastore.preferences.core.longPreferencesKey("completed_time_$packageId")
        context.dataStore.edit { preferences ->
            preferences[key] = System.currentTimeMillis()
        }
    }

    override suspend fun triggerCompletedPackagesCleanUp() {
        cleanUpCompletedPackages()
    }

    private suspend fun cleanUpCompletedPackages() {
        val currentSettings = localSettings.settingsFlow.first()
        val repo = currentSettings.githubAudioRepo
        val baseUrl = "https://raw.githubusercontent.com/$repo/main/packages"

        val remoteMetadata = remotePackages.getRemotePackagesMetadata()
        for (pkg in remoteMetadata) {
            val key = androidx.datastore.preferences.core.longPreferencesKey("completed_time_${pkg.id}")
            val completedTime = context.dataStore.data.map { it[key] }.first()
            if (completedTime != null && completedTime > 0L) {
                val oneWeekMs = 7L * 24 * 60 * 60 * 1000L
                if (System.currentTimeMillis() - completedTime > oneWeekMs) {
                    for (file in pkg.files) {
                        val relativePath = file.audioUrl.substringAfter("main/packages/")
                        val ext = relativePath.substringAfterLast(".", "wav")
                        val localFile = File(downloadsDir, "${file.id}.$ext")
                        if (localFile.exists()) {
                            localFile.delete()
                        }
                    }
                    context.dataStore.edit { preferences ->
                        preferences[key] = 0L
                    }
                }
            }
        }
    }
}
