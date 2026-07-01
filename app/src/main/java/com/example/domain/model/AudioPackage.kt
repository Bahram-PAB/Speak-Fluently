package com.example.domain.model

data class AudioPackage(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String? = null,
    val files: List<AudioFile> = emptyList(),
    val isPremiumOnly: Boolean = false
)
