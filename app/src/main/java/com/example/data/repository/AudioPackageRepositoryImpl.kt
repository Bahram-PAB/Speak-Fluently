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

    private var cachedPackages: List<AudioPackage>? = null

    private fun copyAssetToLocalIfPresent(fileName: String, targetFile: File) {
        if (targetFile.exists() && targetFile.length() > 0L) return
        
        val categories = listOf("daily", "ielts", "interview")
        val extensions = listOf("wav", "mp3")
        
        for (category in categories) {
            for (ext in extensions) {
                val assetPath = "audio/$category/$fileName.$ext"
                try {
                    context.assets.open(assetPath).use { inputStream ->
                        // Found in assets! Copy to local storage
                        targetFile.parentFile?.apply { if (!exists()) mkdirs() }
                        java.io.FileOutputStream(targetFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        return
                    }
                } catch (ignored: Exception) {
                    // Try next combination
                }
            }
        }
    }

    override fun getPackages(): Flow<List<AudioPackage>> = flow {
        // Emit cached packages immediately to prevent UI blocking or loading screen hangs
        cachedPackages?.let { emit(it) }

        try {
            // Trigger automated clean up of packages older than 1 week
            try {
                cleanUpCompletedPackages()
            } catch (ignored: Exception) {}

            val currentSettings = localSettings.settingsFlow.first()
            val repo = extractGithubRepo(currentSettings.githubAudioRepo)
            val branch = currentSettings.githubBranch
            val pathPrefix = currentSettings.githubPathPrefix

            val baseUrl = if (pathPrefix.isNotEmpty()) {
                "https://raw.githubusercontent.com/$repo/$branch/$pathPrefix"
            } else {
                "https://raw.githubusercontent.com/$repo/$branch"
            }

            val remoteMetadata = remotePackages.getRemotePackagesMetadata(repo, branch, pathPrefix)
            
            val mappedList = remoteMetadata.map { pkg ->
                val updatedFiles = pkg.files.map { file ->
                    val relativePath = if (file.audioUrl.contains("main/packages/")) {
                        file.audioUrl.substringAfter("main/packages/")
                    } else {
                        file.audioUrl.substringAfterLast("/")
                    }
                    
                    val updatedUrl = if (file.audioUrl.contains("main/packages/")) {
                        "$baseUrl/$relativePath"
                    } else {
                        file.audioUrl
                    }
                    
                    val ext = relativePath.substringAfterLast(".", "wav")
                    val localFile = File(downloadsDir, "${file.id}.$ext")
                    
                    // Proactively copy from assets if available offline in APK
                    try {
                        copyAssetToLocalIfPresent(file.id, localFile)
                    } catch (ignored: Exception) {}
                    
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
            
            cachedPackages = mappedList
            emit(mappedList)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to previous cache or hardcoded list so flow never fails abruptly
            val fallback = cachedPackages ?: remotePackages.getHardcodedPackages()
            emit(fallback)
        }
    }.flowOn(Dispatchers.IO)

    override fun getPackageById(id: String): Flow<AudioPackage?> = getPackages().map { list ->
        list.find { it.id == id }
    }

    override fun downloadFile(file: AudioFile, force: Boolean): Flow<DownloadStatus> = flow {
        val relativePath = if (file.audioUrl.contains("main/packages/")) {
            file.audioUrl.substringAfter("main/packages/")
        } else {
            file.audioUrl.substringAfterLast("/")
        }
        val ext = relativePath.substringAfterLast(".", "wav")
        val localFile = File(downloadsDir, "${file.id}.$ext")

        emit(DownloadStatus.Progress(10))
        if (!force && localFile.exists() && localFile.length() > 0L) {
            // Check if it's a dummy beep file (exactly 48044 bytes). If yes, let's force re-download it!
            if (localFile.length() != 48044L) {
                emit(DownloadStatus.Success(localFile.absolutePath))
                return@flow
            }
        }

        emit(DownloadStatus.Progress(40))
        try {
            val downloadedFile = remotePackages.downloadUrlToFile(file.audioUrl, localFile, file.text, throwOnError = true)
            emit(DownloadStatus.Progress(100))
            emit(DownloadStatus.Success(downloadedFile.absolutePath))
        } catch (e: Exception) {
            // Delete target file if it was created partially, so it can be retried cleanly next time
            try {
                if (localFile.exists()) {
                    localFile.delete()
                }
            } catch (ignored: Exception) {}
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

    override fun getPlayedFileIds(): Flow<Set<String>> {
        return localSettings.playedFilesFlow
    }

    override suspend fun markFileAsPlayed(fileId: String) {
        localSettings.markFileAsPlayed(fileId)
    }

    override suspend fun clearPlayedFiles() {
        localSettings.clearPlayedFiles()
    }

    override suspend fun checkGithubAccess(repo: String): String? = withContext(Dispatchers.IO) {
        val repoClean = extractGithubRepo(repo)
        if (repoClean.isEmpty()) {
            return@withContext "نام کاربری یا نام مخزن نامعتبر است. فرمت صحیح: username/repository"
        }
        
        val client = okhttp3.OkHttpClient()
        
        // 1. First, check if the repository exists on GitHub and is public
        val repoUrl = "https://github.com/$repoClean"
        val repoRequest = okhttp3.Request.Builder()
            .url(repoUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()
            
        try {
            client.newCall(repoRequest).execute().use { response ->
                if (response.code == 404) {
                    return@withContext "مخزن '$repoClean' یافت نشد. لطفاً از صحت نام کاربری/مخزن مطمئن شوید و اطمینان حاصل کنید که مخزن شما عمومی (Public) است، نه خصوصی (Private)."
                } else if (!response.isSuccessful) {
                    return@withContext "خطا در برقراری ارتباط با گیت‌هاب (کد خطا: ${response.code})"
                }
            }
        } catch (e: Exception) {
            return@withContext "خطای شبکه در برقراری ارتباط با گیت‌هاب: ${e.localizedMessage}. لطفاً اتصال اینترنت خود را بررسی کنید."
        }

        // 2. The repo exists and is public! Now let's try to auto-detect branch and prefix
        val branches = listOf("main", "master")
        val prefixes = listOf("packages", "")
        
        for (branch in branches) {
            for (prefix in prefixes) {
                // We will test both conversational speech and IELTS speech file to find a match
                val testFiles = listOf("daily/speech-1.wav", "ielts/speech-1.wav")
                for (testFile in testFiles) {
                    val testUrl = if (prefix.isNotEmpty()) {
                        "https://raw.githubusercontent.com/$repoClean/$branch/$prefix/$testFile"
                    } else {
                        "https://raw.githubusercontent.com/$repoClean/$branch/$testFile"
                    }
                    
                    val request = okhttp3.Request.Builder()
                        .url(testUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .build()
                    try {
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                // Successfully matched! Save these configuration settings
                                val currentSettings = localSettings.settingsFlow.first()
                                localSettings.saveSettings(
                                    currentSettings.copy(
                                        githubAudioRepo = repoClean,
                                        githubBranch = branch,
                                        githubPathPrefix = prefix
                                    )
                                )
                                return@withContext null // Success!
                            }
                        }
                    } catch (e: Exception) {
                        // ignore and try next combination
                    }
                }
            }
        }
        
        // 3. If repo exists but we couldn't fetch specific files, do not block the user!
        // Save the cleaned repository name with default main branch and packages prefix so they can try downloading anyway.
        try {
            val currentSettings = localSettings.settingsFlow.first()
            localSettings.saveSettings(
                currentSettings.copy(
                    githubAudioRepo = repoClean,
                    githubBranch = "main",
                    githubPathPrefix = "packages"
                )
            )
        } catch (ignored: Exception) {}
        
        null // We return null (success) because the repository is public and accessible!
    }

    override suspend fun checkFileExistsOnGithub(url: String): Boolean = withContext(Dispatchers.IO) {
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val request = okhttp3.Request.Builder()
            .url(url)
            .head()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()
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
        val repo = extractGithubRepo(currentSettings.githubAudioRepo)
        val branch = currentSettings.githubBranch
        val pathPrefix = currentSettings.githubPathPrefix

        val baseUrl = if (pathPrefix.isNotEmpty()) {
            "https://raw.githubusercontent.com/$repo/$branch/$pathPrefix"
        } else {
            "https://raw.githubusercontent.com/$repo/$branch"
        }

        val remoteMetadata = remotePackages.getRemotePackagesMetadata(repo, branch, pathPrefix)
        for (pkg in remoteMetadata) {
            val key = androidx.datastore.preferences.core.longPreferencesKey("completed_time_${pkg.id}")
            val completedTime = context.dataStore.data.map { it[key] }.first()
            if (completedTime != null && completedTime > 0L) {
                val oneWeekMs = 7L * 24 * 60 * 60 * 1000L
                if (System.currentTimeMillis() - completedTime > oneWeekMs) {
                    for (file in pkg.files) {
                        val relativePath = if (file.audioUrl.contains("main/packages/")) {
                            file.audioUrl.substringAfter("main/packages/")
                        } else {
                            file.audioUrl.substringAfterLast("/")
                        }
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

    private fun extractGithubRepo(input: String): String {
        val clean = input.trim()
        if (clean.isEmpty()) return ""
        
        var path = clean
        if (path.startsWith("https://")) {
            path = path.substringAfter("https://")
        } else if (path.startsWith("http://")) {
            path = path.substringAfter("http://")
        }
        
        if (path.startsWith("github.com/")) {
            path = path.substringAfter("github.com/")
        }
        
        val parts = path.split("/").filter { it.isNotEmpty() }
        return if (parts.size >= 2) {
            "${parts[0]}/${parts[1]}"
        } else {
            clean
        }
    }
}
