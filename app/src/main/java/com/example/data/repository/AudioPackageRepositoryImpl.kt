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

    private val downloadsDir = File(context.filesDir, "audio_downloads").apply { if (!exists()) mkdirs() }
    private var cachedPackages: List<AudioPackage>? = null

    /**
     * Copies a file from APK assets to local storage using the file's assetPath.
     * Returns true if copy was successful, false otherwise.
     */
    private fun copyAssetToLocalIfPresent(file: AudioFile, targetFile: File): Boolean {
        if (targetFile.exists() && targetFile.length() > 0L) return true
        val assetPath = file.assetPath ?: return false
        return try {
            context.assets.open(assetPath).use { input ->
                targetFile.parentFile?.apply { if (!exists()) mkdirs() }
                java.io.FileOutputStream(targetFile).use { output -> input.copyTo(output) }
            }
            true
        } catch (e: Exception) { false }
    }

    override fun getPackages(): Flow<List<AudioPackage>> = flow {
        cachedPackages?.let { emit(it) }
        try {
            try { cleanUpCompletedPackages() } catch (ignored: Exception) {}
            val s = localSettings.settingsFlow.first()
            val repo = extractGithubRepo(s.githubAudioRepo)
            val branch = s.githubBranch
            val prefix = s.githubPathPrefix
            val remoteMetadata = remotePackages.getRemotePackagesMetadata(repo, branch, prefix)
            val mappedList = remoteMetadata.map { pkg ->
                pkg.copy(files = pkg.files.map { file ->
                    val ext = file.audioUrl.substringAfterLast(".", "wav")
                    val localFile = File(downloadsDir, "${file.id}.$ext")
                    copyAssetToLocalIfPresent(file, localFile)
                    file.copy(localPath = if (localFile.exists() && localFile.length() > 0L) localFile.absolutePath else null)
                })
            }
            cachedPackages = mappedList
            emit(mappedList)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(cachedPackages ?: remotePackages.getHardcodedPackages())
        }
    }.flowOn(Dispatchers.IO)

    override fun getPackageById(id: String): Flow<AudioPackage?> = getPackages().map { list -> list.find { it.id == id } }

    override fun downloadFile(file: AudioFile, force: Boolean): Flow<DownloadStatus> = flow {
        val ext = file.audioUrl.substringAfterLast(".", "wav")
        val localFile = File(downloadsDir, "${file.id}.$ext")
        emit(DownloadStatus.Progress(10))
        if (!force && localFile.exists() && localFile.length() > 0L) {
            emit(DownloadStatus.Success(localFile.absolutePath))
            return@flow
        }
        if (copyAssetToLocalIfPresent(file, localFile)) {
            emit(DownloadStatus.Progress(100))
            emit(DownloadStatus.Success(localFile.absolutePath))
            return@flow
        }
        emit(DownloadStatus.Progress(40))
        try {
            val downloadedFile = remotePackages.downloadUrlToFile(file.audioUrl, localFile)
            emit(DownloadStatus.Progress(100))
            emit(DownloadStatus.Success(downloadedFile.absolutePath))
        } catch (e: Exception) {
            try { if (localFile.exists()) localFile.delete() } catch (ignored: Exception) {}
            emit(DownloadStatus.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun getSettings(): Flow<Settings> = localSettings.settingsFlow
    override suspend fun saveSettings(settings: Settings) = localSettings.saveSettings(settings)
    override fun getPremiumStatus(): Flow<PremiumStatus> = localSettings.premiumStatusFlow

    override suspend fun activatePremium(code: String): Boolean {
        val sanitized = code.trim().uppercase()
        val isValid = sanitized == "SPEAK2026" || sanitized == "FLUENT" || sanitized == "FREEPASS"
        if (isValid) localSettings.savePremiumStatus(PremiumStatus(isPremium = true, activationCode = sanitized))
        return isValid
    }

    override suspend fun checkGithubAccess(repo: String): String? = withContext(Dispatchers.IO) {
        val repoClean = extractGithubRepo(repo)
        if (repoClean.isEmpty()) return@withContext "\u0646\u0627\u0645 \u06a9\u0627\u0631\u0628\u0631\u06cc \u06cc\u0627 \u0646\u0627\u0645 \u0645\u062e\u0632\u0646 \u0646\u0627\u0645\u0639\u062a\u0628\u0631 \u0627\u0633\u062a. \u0641\u0631\u0645\u062a \u0635\u062d\u06cc\u062d: username/repository"
        val client = okhttp3.OkHttpClient()
        try {
            client.newCall(okhttp3.Request.Builder().url("https://github.com/$repoClean").header("User-Agent", "Mozilla/5.0").build()).execute().use { r ->
                if (r.code == 404) return@withContext "\u0645\u062e\u0632\u0646 '$repoClean' \u06cc\u0627\u0641\u062a \u0646\u0634\u062f. \u0644\u0637\u0641\u0627\u064b \u0627\u0637\u0645\u06cc\u0646\u0627\u0646 \u062d\u0627\u0635\u0644 \u06a9\u0646\u06cc\u062f \u0645\u062e\u0632\u0646 \u0639\u0645\u0648\u0645\u06cc (Public) \u0627\u0633\u062a.\"\n                if (!r.isSuccessful) return@withContext "\u062e\u0637\u0627 \u062f\u0631 \u0628\u0631\u0642\u0631\u0627\u0631\u06cc \u0628\u0627 \u06af\u06cc\u062a\u0647\u0627\u0628 (\u06a9\u062f \u062e\u0637\u0627: ${r.code})"
            }
        } catch (e: Exception) { return@withContext "\u062e\u0637\u0627\u06cc \u0634\u0628\u06a9\u0647: ${e.localizedMessage}" }

        for (branch in listOf("main", "master")) {
            try {
                client.newCall(okhttp3.Request.Builder().url("https://api.github.com/repos/$repoClean/git/trees/$branch?recursive=1").header("User-Agent", "Mozilla/5.0").build()).execute().use { r ->
                    if (r.isSuccessful) {
                        val body = r.body?.string() ?: ""
                        if (body.isNotEmpty()) {
                            val tree = org.json.JSONObject(body).optJSONArray("tree")
                            if (tree != null && tree.length() > 0) {
                                var detectedPrefix = ""
                                for (i in 0 until tree.length()) {
                                    val item = tree.getJSONObject(i)
                                    val path = item.optString("path", "")
                                    val segs = path.split("/")
                                    val lp = path.lowercase()
                                    if (segs.size == 2 && (lp.endsWith(".wav") || lp.endsWith(".mp3") || lp.endsWith(".m4a"))) {
                                        detectedPrefix = segs[0]
                                        break
                                    }
                                }
                                val cs = localSettings.settingsFlow.first()
                                localSettings.saveSettings(cs.copy(githubAudioRepo = repoClean, githubBranch = branch, githubPathPrefix = detectedPrefix))
                                return@withContext null
                            }
                        }
                    }
                }
            } catch (ignored: Exception) {}
        }
        try { val cs = localSettings.settingsFlow.first(); localSettings.saveSettings(cs.copy(githubAudioRepo = repoClean, githubBranch = "main", githubPathPrefix = "")) } catch (ignored: Exception) {}
        null
    }

    override suspend fun checkFileExistsOnGithub(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            okhttp3.OkHttpClient.Builder().connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS).readTimeout(5, java.util.concurrent.TimeUnit.SECONDS).build()
                .newCall(okhttp3.Request.Builder().url(url).head().header("User-Agent", "Mozilla/5.0").build()).execute().use { it.isSuccessful }
        } catch (e: Exception) { false }
    }

    override suspend fun markPackageCompleted(packageId: String) {
        val key = androidx.datastore.preferences.core.longPreferencesKey("completed_time_$packageId")
        context.dataStore.edit { it[key] = System.currentTimeMillis() }
    }

    override suspend fun triggerCompletedPackagesCleanUp() = cleanUpCompletedPackages()

    private suspend fun cleanUpCompletedPackages() {
        val s = localSettings.settingsFlow.first()
        val metadata = remotePackages.getRemotePackagesMetadata(extractGithubRepo(s.githubAudioRepo), s.githubBranch, s.githubPathPrefix)
        for (pkg in metadata) {
            val key = androidx.datastore.preferences.core.longPreferencesKey("completed_time_${pkg.id}")
            val ct = context.dataStore.data.map { it[key] }.first()
            if (ct != null && ct > 0L && System.currentTimeMillis() - ct > 7L * 24 * 60 * 60 * 1000L) {
                for (file in pkg.files) { val f = File(downloadsDir, "${file.id}.${file.audioUrl.substringAfterLast(".", "wav")}"); if (f.exists()) f.delete() }
                context.dataStore.edit { it[key] = 0L }
            }
        }
    }

    private fun extractGithubRepo(input: String): String {
        val clean = input.trim()
        if (clean.isEmpty()) return ""
        var path = clean
        if (path.startsWith("https://")) path = path.substringAfter("https://")
        else if (path.startsWith("http://")) path = path.substringAfter("http://")
        if (path.startsWith("github.com/")) path = path.substringAfter("github.com/")
        val parts = path.split("/").filter { it.isNotEmpty() }
        return if (parts.size >= 2) "${parts[0]}/${parts[1]}" else clean
    }
}
