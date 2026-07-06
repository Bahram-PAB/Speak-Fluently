package com.example.data.repository

import android.content.Context
import com.example.data.local.LocalSettingsDataSource
import com.example.data.remote.RemoteAudioPackageDataSource
import com.example.domain.repository.DownloadStatus
import com.example.domain.repository.AudioPackageRepository
import com.example.domain.model.AudioFile
import com.example.domain.model.AudioPackage
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

    override fun getPlayedFileIds(): Flow<Set<String>> = localSettings.playedFilesFlow
    override suspend fun markFileAsPlayed(fileId: String) = localSettings.markFileAsPlayed(fileId)
    override suspend fun clearPlayedFiles() = localSettings.clearPlayedFiles()

    override fun getPackages(): Flow<List<AudioPackage>> = flow {
        cachedPackages?.let { emit(it) }
        try {
            try { cleanUpCompletedPackages() } catch (ignored: Exception) {}
            val s = localSettings.settingsFlow.first()
            val repo = extractGithubRepo(s.githubAudioRepo)
            val branch = s.githubBranch
            val prefix = s.githubPathPrefix
            val remoteMetadata = remotePackages.getRemotePackagesMetadata(repo, branch, prefix)

            // Check which files are already downloaded locally
            val mappedList = remoteMetadata.map { pkg ->
                pkg.copy(files = pkg.files.map { file ->
                    val ext = file.audioUrl.substringAfterLast(".", "wav")
                    val localFile = File(downloadsDir, "${file.id}.$ext")
                    file.copy(localPath = if (localFile.exists() && localFile.length() > 0L) localFile.absolutePath else null)
                })
            }
            cachedPackages = mappedList
            emit(mappedList)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(cachedPackages ?: emptyList())
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

    override suspend fun checkGithubAccess(repo: String): String? = withContext(Dispatchers.IO) {
        val repoClean = extractGithubRepo(repo)
        if (repoClean.isEmpty()) return@withContext "Invalid format. Use: username/repository"
        val client = okhttp3.OkHttpClient()
        try {
            val request = okhttp3.Request.Builder().url("https://github.com/$repoClean").header("User-Agent", "Mozilla/5.0").build()
            val response = client.newCall(request).execute()
            if (response.code == 404) return@withContext "Repository '$repoClean' not found."
            if (!response.isSuccessful) return@withContext "GitHub API error (code: ${response.code})"
            response.close()
        } catch (e: Exception) { return@withContext "Network error: ${e.localizedMessage}" }

        for (branch in listOf("main", "master")) {
            try {
                val request = okhttp3.Request.Builder().url("https://api.github.com/repos/$repoClean/git/trees/$branch?recursive=1").header("User-Agent", "Mozilla/5.0").build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
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
                response.close()
            } catch (ignored: Exception) {}
        }
        try {
            val cs = localSettings.settingsFlow.first()
            localSettings.saveSettings(cs.copy(githubAudioRepo = repoClean, githubBranch = "main", githubPathPrefix = ""))
        } catch (ignored: Exception) {}
        null
    }

    override suspend fun checkFileExistsOnGithub(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient.Builder().connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS).readTimeout(5, java.util.concurrent.TimeUnit.SECONDS).build()
            val request = okhttp3.Request.Builder().url(url).head().header("User-Agent", "Mozilla/5.0").build()
            val response = client.newCall(request).execute()
            val result = response.isSuccessful
            response.close()
            result
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
                for (file in pkg.files) {
                    val f = File(downloadsDir, "${file.id}.${file.audioUrl.substringAfterLast(".", "wav")}")
                    if (f.exists()) f.delete()
                }
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
