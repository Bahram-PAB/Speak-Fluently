package com.example.domain.model

data class AudioFile(
    val id: String,
    val text: String,
    val audioUrl: String,
    val localPath: String? = null,
    val assetPath: String? = null,  // Direct path to file in APK assets (e.g., "audio/daily/speech-1.wav")
    val packageName: String
) {
    val isDownloaded: Boolean
        get() = localPath != null
}
