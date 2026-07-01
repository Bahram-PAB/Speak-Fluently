package com.example.domain.model

data class AudioFile(
    val id: String,
    val text: String,
    val audioUrl: String,
    val localPath: String? = null,
    val packageName: String
) {
    val isDownloaded: Boolean
        get() = localPath != null
}
