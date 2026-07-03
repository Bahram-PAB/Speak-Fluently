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
                                            filesList.add(AudioFile(id = fid, text = name, audioUrl = audioUrl, assetPath = ap, packageName = "pkg_github_$pathPrefix"))
                                        }
                                    }
                                }
                            }
                            if (filesList.isNotEmpty()) {
                                return listOf(AudioPackage(id = "pkg_github_$pathPrefix", name = "\u067e\u06a9\u06cc\u062c \u062a\u0645\u0631\u06cc\u0646\u06cc $pathPrefix", description = "${filesList.size} \u0641\u0627\u06cc\u0644 \u0635\u0648\u062a\u06cc \u0627\u0632 \u067e\u0648\u0634\u0647 $pathPrefix", files = filesList, isPremiumOnly = false))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return defaultList
    }

    fun getHardcodedPackages(): List<AudioPackage> {
        return listOf(
            AudioPackage(id = "pkg_conversational_english", name = "Daily Conversational English", description = "Master everyday conversation.", files = listOf(
                AudioFile(id = "q_weekend_plans", text = "What are your plans for this upcoming weekend?", audioUrl = "$githubBaseUrl/daily/speech-1.wav", assetPath = "audio/daily/speech-1.wav", packageName = "pkg_conversational_english"),
                AudioFile(id = "q_favorite_hobby", text = "Tell me about your favorite hobby.", audioUrl = "$githubBaseUrl/daily/speech-2.wav", assetPath = "audio/daily/speech-2.wav", packageName = "pkg_conversational_english"),
                AudioFile(id = "q_perfect_day", text = "Describe your perfect day.", audioUrl = "$githubBaseUrl/daily/speech-3.wav", assetPath = "audio/daily/speech-3.wav", packageName = "pkg_conversational_english"),
                AudioFile(id = "q_weather_mood", text = "How does weather affect your mood?", audioUrl = "$githubBaseUrl/daily/speech-4.wav", assetPath = "audio/daily/speech-4.wav", packageName = "pkg_conversational_english"),
                AudioFile(id = "q_recommend_book", text = "Recommend a book or movie.", audioUrl = "$githubBaseUrl/daily/speech-5.wav", assetPath = "audio/daily/speech-5.wav", packageName = "pkg_conversational_english")
            ), isPremiumOnly = false),
            AudioPackage(id = "pkg_ielts_speaking", name = "IELTS Speaking Mastery", description = "Perfect IELTS Part 1 answers.", files = listOf(
                AudioFile(id = "q_ielts_hometown", text = "Tell us about your hometown.", audioUrl = "$githubBaseUrl/ielts/speech-1.wav", assetPath = "audio/ielts/speech-1.wav", packageName = "pkg_ielts_speaking"),
                AudioFile(id = "q_ielts_work_study", text = "Do you work or study?", audioUrl = "$githubBaseUrl/ielts/speech-2.wav", assetPath = "audio/ielts/speech-2.wav", packageName = "pkg_ielts_speaking"),
                AudioFile(id = "q_ielts_technology", text = "How often do you use technology?", audioUrl = "$githubBaseUrl/ielts/speech-3.wav", assetPath = "audio/ielts/speech-3.wav", packageName = "pkg_ielts_speaking"),
                AudioFile(id = "q_ielts_transport", text = "What is public transport like?", audioUrl = "$githubBaseUrl/ielts/speech-4.wav", assetPath = "audio/ielts/speech-4.wav", packageName = "pkg_ielts_speaking"),
                AudioFile(id = "q_ielts_plans", text = "What are your future plans?", audioUrl = "$githubBaseUrl/ielts/speech-5.wav", assetPath = "audio/ielts/speech-5.wav", packageName = "pkg_ielts_speaking")
            ), isPremiumOnly = false),
            AudioPackage(id = "pkg_job_interview", name = "Job Interview Confidence", description = "Practice behavioral questions.", files = listOf(
                AudioFile(id = "q_interview_introduce", text = "Introduce yourself.", audioUrl = "$githubBaseUrl/interview/speech-1.wav", assetPath = "audio/interview/speech-1.wav", packageName = "pkg_job_interview"),
                AudioFile(id = "q_interview_strength", text = "What is your greatest strength?", audioUrl = "$githubBaseUrl/interview/speech-2.wav", assetPath = "audio/interview/speech-2.wav", packageName = "pkg_job_interview"),
                AudioFile(id = "q_interview_conflict", text = "Describe a conflict and how you resolved it.", audioUrl = "$githubBaseUrl/interview/speech-3.wav", assetPath = "audio/interview/speech-3.wav", packageName = "pkg_job_interview"),
                AudioFile(id = "q_interview_pressure", text = "How do you handle pressure?", audioUrl = "$githubBaseUrl/interview/speech-4.wav", assetPath = "audio/interview/speech-4.wav", packageName = "pkg_job_interview"),
                AudioFile(id = "q_interview_failure", text = "Tell me about a failure.", audioUrl = "$githubBaseUrl/interview/speech-5.wav", assetPath = "audio/interview/speech-5.wav", packageName = "pkg_job_interview")
            ), isPremiumOnly = true)
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
