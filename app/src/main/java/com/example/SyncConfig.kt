package com.example

object SyncConfig {
    // ====== فقط این خط رو تغییر بده ======
    const val HOST_BASE_URL = "https://binev.ir/audio"
    // =====================================

    // index.json از همین آدرس دانلود میشه
    const val INDEX_JSON_URL = "$HOST_BASE_URL/index.json"

    const val FILES_PER_EXERCISE = 5
    const val CLEANUP_DAYS_AFTER_COMPLETION = 7

    // قدیمی - دیگر استفاده نمیشه ولی برای compatibility نگه داشته شد
    const val GITHUB_REPO = "Bahram-PAB/Speak-Fluently"
    const val GITHUB_BRANCH = "main"
    const val AUDIO_PATH_PREFIX = "audio/daily"
}