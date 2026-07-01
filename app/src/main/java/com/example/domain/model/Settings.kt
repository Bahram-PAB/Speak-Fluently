package com.example.domain.model

data class Settings(
    val dailyNotificationTime: String = "09:00",
    val notificationsEnabled: Boolean = true,
    val appLanguage: String = "fa", // Default language is Persian (Farsi)
    val questionsPerSession: Int = 5,
    val pauseDurationSeconds: Int = 20
)
