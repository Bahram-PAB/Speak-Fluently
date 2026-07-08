package com.example.domain.model

import java.io.File

data class Exercise(
    val id: Int,
    val name: String,
    val files: List<ExerciseFile>,
    val isCompleted: Boolean = false,
    val isLocked: Boolean = true,
    val lastCompletedAt: Long? = null
)

data class ExerciseFile(
    val id: Int,
    val title: String,
    val audioUrl: String,
    val localFile: File? = null
) {
    val isDownloaded: Boolean get() = localFile != null && localFile.exists()
}