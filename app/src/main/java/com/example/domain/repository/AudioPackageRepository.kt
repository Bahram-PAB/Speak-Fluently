package com.example.domain.repository

import com.example.domain.model.AudioFile
import com.example.domain.model.AudioPackage
import com.example.domain.model.PremiumStatus
import com.example.domain.model.Settings
import kotlinx.coroutines.flow.Flow

sealed interface DownloadStatus {
    object Idle : DownloadStatus
    data class Progress(val percentage: Int) : DownloadStatus
    data class Success(val localPath: String) : DownloadStatus
    data class Error(val exception: Throwable) : DownloadStatus
}

interface AudioPackageRepository {
    fun getPackages(): Flow<List<AudioPackage>>
    fun getPackageById(id: String): Flow<AudioPackage?>
    fun downloadFile(file: AudioFile, force: Boolean = false): Flow<DownloadStatus>
    fun getSettings(): Flow<Settings>
    suspend fun saveSettings(settings: Settings)
    fun getPremiumStatus(): Flow<PremiumStatus>
    suspend fun activatePremium(code: String): Boolean
    suspend fun checkGithubAccess(repo: String): String?
    suspend fun checkFileExistsOnGithub(url: String): Boolean
    suspend fun markPackageCompleted(packageId: String)
    suspend fun triggerCompletedPackagesCleanUp()
    fun getPlayedFileIds(): Flow<Set<String>>
    suspend fun markFileAsPlayed(fileId: String)
    suspend fun clearPlayedFiles()
}
