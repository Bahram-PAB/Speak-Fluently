package com.example.data.remote

import android.content.Context
import com.example.domain.model.AudioFile
import com.example.domain.model.AudioPackage
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class RemoteAudioPackageDataSource(private val context: Context) {

    private val client = OkHttpClient()
    private val githubBaseUrl = "https://raw.githubusercontent.com/username/speakfluently-audio/main/packages"

    fun getRemotePackagesMetadata(repo: String = "", branch: String = "", pathPrefix: String = ""): List<AudioPackage> {
        val defaultList = getHardcodedPackages()
        if (repo.isEmpty() || branch.isEmpty()) return defaultList

        try {
            val treeUrl = "https://api.github.com/repos/$repo/git/trees/$branch?recursive=1"
            val request = Request.Builder().url(treeUrl).header("User-Agent", "Mozilla/5.0").build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString.isNotEmpty()) {
                        val json = org.json.JSONObject(bodyString)
                        val treeArray = json.optJSONArray("tree")
                        if (treeArray != null) {
                            val filesList = mutableListOf<AudioFile>()
                            for (i in 0 until treeArray.length()) {
                                val item = treeArray.getJSONObject(i)
                                if (item.optString("type", "") == "blob") {
                                    val path = item.optString("path", "")
                                    val lp = path.lowercase()
                                    if (lp.endsWith(".wav") || lp.endsWith(".mp3") || lp.endsWith(".m4a") || lp.endsWith(".ogg")) {
                                        val segs = path.split("/")
                                        val matches = if (pathPrefix.isEmpty()) true else (segs.size == 2 && segs[0] == pathPrefix)
                                        if (matches) {
                                            val fn = segs.last()
                                            val name = fn.substringBeforeLast(".").replace("_", " ").replace("-", " ").trim()
                                            val audioUrl = "https://raw.githubusercontent.com/$repo/$branch/$path"
                                            val fid = path.replace("/", "_").replace(".", "_")
                                            val ap = if (pathPrefix.isNotEmpty()) "audio/$pathPrefix/$fn" else null
                                            filesList.add(AudioFile(id = fid, text = name, audioUrl = audioUrl, assetPath = ap, packageName = "pkg_github"))
                                        }
                                    }
                                }
                            }
                            if (filesList.isNotEmpty()) {
                                return listOf(AudioPackage(
                                    id = "pkg_github",
                                    name = "تمارین روزانه",
                                    description = "${filesList.size} فایل صوتی",
                                    files = filesList,
                                    isPremiumOnly = false
                                ))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return defaultList
    }

    fun getHardcodedPackages(): List<AudioPackage> {
        val dailyFiles = (1..10).map { i ->
            AudioFile(
                id = "q_daily_$i",
                text = "",
                audioUrl = "$githubBaseUrl/daily/speech-$i.wav",
                assetPath = "audio/daily/speech-$i.wav",
                packageName = "pkg_daily"
            )
        }
        return listOf(
            AudioPackage(
                id = "pkg_daily",
                name = "تمارین روزانه",
                description = "${dailyFiles.size} فایل صوتی برای تمرین روزانه",
                files = dailyFiles,
                isPremiumOnly = false
            )
        )
    }

    @Throws(IOException::class)
    fun downloadUrlToFile(url: String, targetFile: File): File {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Download failed: $response")
            val body = response.body ?: throw IOException("Empty response body")
            FileOutputStream(targetFile).use { fos -> body.byteStream().use { bis -> bis.copyTo(fos) } }
        }
        return targetFile
    }
}
