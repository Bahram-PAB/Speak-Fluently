package com.example.data.remote

import com.example.SyncConfig
import com.example.Lang
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class Banner(
    val textFa: String,
    val textEn: String,
    val url: String?,
    val enabled: Boolean,
    val days: List<Int>
) {
    val text: String get() = if (Lang.current == Lang.Language.EN) textEn else textFa
    val hasUrl: Boolean get() = !url.isNullOrEmpty()
}

class BannerFetcher(private val client: OkHttpClient) {

    /**
     * Fetch ALL matching banners for the given exercise ID.
     * Returns every enabled banner whose "day" list contains exerciseId.
     */
    fun fetchAll(exerciseId: Int): List<Banner> {
        return try {
            val request = Request.Builder()
                .url("${SyncConfig.HOST_BASE_URL}/banner.json")
                .header("User-Agent", "SpeakFluently-App")
                .build()

            val response = client.newCall(request).execute()
            try {
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                if (body.isEmpty()) return emptyList()

                parseBanners(body).filter { it.enabled && exerciseId in it.days }
            } finally {
                response.close()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseBanners(jsonString: String): List<Banner> {
        val result = mutableListOf<Banner>()
        try {
            val json = JSONObject(jsonString)
            val bannersArray = json.optJSONArray("banners") ?: return emptyList()

            for (i in 0 until bannersArray.length()) {
                val obj = bannersArray.getJSONObject(i)
                val enabled = obj.optBoolean("enabled", true)
                val textFa = obj.optString("text_fa", "")
                val textEn = obj.optString("text_en", "")
                val url = obj.optString("url", "").ifEmpty { null }
                val days = parseDays(obj)

                if (textFa.isNotEmpty() || textEn.isNotEmpty()) {
                    result.add(Banner(textFa = textFa, textEn = textEn, url = url, enabled = enabled, days = days))
                }
            }
        } catch (_: Exception) {}
        return result
    }

    private fun parseDays(obj: JSONObject): List<Int> {
        val days = mutableListOf<Int>()
        val dayRaw = obj.opt("day") ?: return emptyList()
        when (dayRaw) {
            is org.json.JSONArray -> {
                for (i in 0 until dayRaw.length()) {
                    dayRaw.optInt(i, -1).takeIf { it > 0 }?.let { days.add(it) }
                }
            }
            is String -> {
                dayRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { s ->
                    s.toIntOrNull()?.takeIf { it > 0 }?.let { days.add(it) }
                }
            }
            is Int -> days.add(dayRaw)
        }
        return days
    }
}