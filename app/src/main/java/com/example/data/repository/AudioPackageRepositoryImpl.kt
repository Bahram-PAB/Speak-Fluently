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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
        val remoteMetadata = remotePackages.getRemotePackagesMetadata()
        // Map files to check if they exist locally
        val mappedList = remoteMetadata.map { pkg ->
            val updatedFiles = pkg.files.map { file ->
                val relativePath = file.audioUrl.substringAfter("main/packages/") // e.g., "daily/q_weekend_plans.wav"
                val baseRelativePath = relativePath.substringBeforeLast(".") // e.g., "daily/q_weekend_plans"
                
                var foundAssetPath: String? = null
                for (ext in listOf("wav", "mp3")) {
                    val assetPath = "audio/$baseRelativePath.$ext"
                    try {
                        context.assets.open(assetPath).use {
                            foundAssetPath = assetPath
                        }
                        break
                    } catch (e: Exception) {
                        // Not found
                    }
                }

                if (foundAssetPath != null) {
                    file.copy(localPath = "asset:///$foundAssetPath")
                } else {
                    val localFile = File(downloadsDir, "${file.id}.wav")
                    if (localFile.exists() && localFile.length() > 0L) {
                        file.copy(localPath = localFile.absolutePath)
                    } else {
                        file
                    }
                }
            }
            pkg.copy(files = updatedFiles)
        }
        emit(mappedList)
    }.flowOn(Dispatchers.IO)

    override fun getPackageById(id: String): Flow<AudioPackage?> = getPackages().map { list ->
        list.find { it.id == id }
    }

    override fun downloadFile(file: AudioFile): Flow<DownloadStatus> = flow {
        emit(DownloadStatus.Progress(10))
        val localFile = File(downloadsDir, "${file.id}.wav")
        
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
}
