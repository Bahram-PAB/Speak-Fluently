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
    private val githubBaseUrl = "https://raw.githubusercontent.com/Bahram-PAB/Speak-Fluently/main/app/src/main/assets/audio/daily"

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
                            val packageMap = mutableMapOf<String, MutableList<AudioFile>>()

                            for (i in 0 until treeArray.length()) {
                                val item = treeArray.getJSONObject(i)
                                if (item.optString("type", "") == "blob") {
                                    val path = item.optString("path", "")
                                    val lp = path.lowercase()
                                    if (lp.endsWith(".wav") || lp.endsWith(".mp3") || lp.endsWith(".m4a") || lp.endsWith(".ogg")) {
                                        val segs = path.split("/")
                                        if (segs.size == 3 && segs[0] == pathPrefix) {
                                            val folderName = segs[1]
                                            val fileName = segs[2]
                                            val name = fileName.substringBeforeLast(".").replace("_", " ").replace("-", " ").trim()
                                            val audioUrl = "https://raw.githubusercontent.com/$repo/$branch/$path"
                                            val fid = path.replace("/", "_").replace(".", "_")
                                            val assetPath = "audio/$pathPrefix/$folderName/$fileName"
                                            val audioFile = AudioFile(id = fid, text = name, audioUrl = audioUrl, assetPath = assetPath, packageName = "pkg_$folderName")
                                            packageMap.getOrPut(folderName) { mutableListOf() }.add(audioFile)
                                        }
                                    }
                                }
                            }

                            if (packageMap.isNotEmpty()) {
                                return packageMap.toSortedMap(compareBy { it.toIntOrNull() ?: 0 })
                                    .map { (folderName, files) ->
                                        AudioPackage(
                                            id = "pkg_$folderName",
                                            name = "تمرین $folderName",
                                            description = "${files.size} فایل صوتی",
                                            files = files,
                                            isPremiumOnly = false
                                        )
                                    }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return defaultList
    }

    fun getHardcodedPackages(): List<AudioPackage> {
        val packages = mutableListOf<AudioPackage>()

        try {
            val dailyPath = "audio/daily"
            val items = context.assets.list(dailyPath) ?: emptyArray()

            val subfolders = items.filter { item ->
                val subItems = context.assets.list("$dailyPath/$item")
                subItems != null && subItems.isNotEmpty()
            }.sortedBy { it.toIntOrNull() ?: 0 }

            for (folderName in subfolders) {
                val folderPath = "$dailyPath/$folderName"
                val files = context.assets.list(folderPath) ?: emptyArray()
                val audioFiles = files.filter { f ->
                    val lp = f.lowercase()
                    lp.endsWith(".wav") || lp.endsWith(".mp3") || lp.endsWith(".m4a") || lp.endsWith(".ogg")
                }.sorted()

                if (audioFiles.isNotEmpty()) {
                    val packageFiles = audioFiles.map { fileName ->
                        val name = fileName.substringBeforeLast(".").replace("_", " ").replace("-", " ").trim()
                        AudioFile(
                            id = "pkg_${folderName}_${fileName.replace(".", "_")}",
                            text = name,
                            audioUrl = "$githubBaseUrl/$folderName/$fileName",
                            assetPath = "$folderPath/$fileName",
                            packageName = "pkg_$folderName"
                        )
                    }
                    packages.add(AudioPackage(
                        id = "pkg_$folderName",
                        name = "تمرین $folderName",
                        description = "${packageFiles.size} فایل صوتی",
                        files = packageFiles,
                        isPremiumOnly = false
                    ))
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        if (packages.isEmpty()) {
            val totalFiles = 20
            val allFiles = (1..totalFiles).map { i ->
                AudioFile(
                    id = "q_daily_$i",
                    text = "",
                    audioUrl = "$githubBaseUrl/speech-$i.wav",
                    assetPath = "audio/daily/speech-$i.wav",
                    packageName = "pkg_daily_1"
                )
            }
            packages.add(AudioPackage(
                id = "pkg_daily_1",
                name = "تمرین ۱",
                description = "فایل‌های پیش‌فرض",
                files = allFiles,
                isPremiumOnly = false
            ))
        }

        return packages
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