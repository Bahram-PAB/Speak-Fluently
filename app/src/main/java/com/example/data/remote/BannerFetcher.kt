package com.example.data.remote

import com.example.SyncConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class Banner(val text: String, val url: String?, val enabled: Boolean)

class BannerFetcher(private val client: OkHttpClient) {

    fun fetch(): Banner? {
        return try {
            val request = Request.Builder()
                .url("${SyncConfig.HOST_BASE_URL}/banner.json")
                .header("User-Agent", "SpeakFluently-App")
                .build()

            val response = client.newCall(request).execute()
            try {
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                if (body.isEmpty()) return null

                val json = JSONObject(body)
                val enabled = json.optBoolean("enabled", true)
                if (!enabled) return null

                val text = json.optString("text", "")
                if (text.isEmpty()) return null

                val url = json.optString("url", "").ifEmpty { null }
                Banner(text = text, url = url, enabled = true)
            } finally {
                response.close()
            }
        } catch (_: Exception) {
            null
        }
    }
}