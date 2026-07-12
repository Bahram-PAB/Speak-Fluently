package com.example

import java.util.Locale

object Lang {
    enum class Language(val code: String, val displayName: String) {
        FA("fa", "فارسی"),
        EN("en", "English")
    }

    var current: Language = Language.FA

    private val strings = mapOf(
        Language.FA to mapOf(
            "app_name" to "Speak Fluently",
            "daily_exercises" to "تمرین‌های روزانه",
            "settings" to "تنظیمات",
            "loading" to "در حال بارگذاری...",
            "sync" to "همگام‌سازی",
            "syncing" to "در حال همگام‌سازی...",
            "sync_done" to "همگام‌سازی انجام شد",
            "sync_error" to "خطا در همگام‌سازی",
            "no_exercises" to "هنوز تمرینی یافت نشد.\nبرای شروع دکمه همگام‌سازی را بزنید.",
            "interval_title" to "فاصله بین فایل‌ها",
            "interval_desc" to "زمان انتظار بین پخش هر فایل صوتی",
            "interval_current" to "زمان فعلی",
            "seconds" to "ثانیه",
            "language" to "زبان",
            "language_desc" to "انتخاب زبان برنامه",
            "back" to "بازگشت",
            "play" to "پخش",
            "pause" to "توقف",
            "skip" to "رد شدن",
            "timer" to "تایمر",
            "seconds_to_next" to "ثانیه تا فایل بعدی",
            "listening" to "🎧 گوش دهید، تکرار کنید",
            "auto_next" to "⏳ فایل بعدی به صورت خودکار پخش می‌شود...",
            "downloading" to "در حال دانلود فایل‌ها...",
            "training_complete" to "تمرین تکمیل شد!",
            "click_to_return" to "برای بازگشت کلیک کنید",
            "files_count" to "فایل صوتی",
            "completed" to "تکمیل شده",
            "locked" to "قفل شده",
            "unlocked" to "باز"
        ),
        Language.EN to mapOf(
            "app_name" to "Speak Fluently",
            "daily_exercises" to "Daily Exercises",
            "settings" to "Settings",
            "loading" to "Loading...",
            "sync" to "Sync",
            "syncing" to "Syncing...",
            "sync_done" to "Sync completed",
            "sync_error" to "Sync error",
            "no_exercises" to "No exercises found yet.\nTap Sync to get started.",
            "interval_title" to "Interval between files",
            "interval_desc" to "Waiting time between each audio file",
            "interval_current" to "Current interval",
            "seconds" to "seconds",
            "language" to "Language",
            "language_desc" to "Change app language",
            "back" to "Back",
            "play" to "Play",
            "pause" to "Pause",
            "skip" to "Skip",
            "timer" to "Timer",
            "seconds_to_next" to "seconds to next file",
            "listening" to "🎧 Listen, repeat",
            "auto_next" to "⏳ Next file will play automatically...",
            "downloading" to "Downloading files...",
            "training_complete" to "Training completed!",
            "click_to_return" to "Tap to return home",
            "files_count" to "audio files",
            "completed" to "Completed",
            "locked" to "Locked",
            "unlocked" to "Unlocked"
        )
    )

    fun t(key: String): String = strings[current]?.get(key) ?: strings[Language.FA]?.get(key) ?: key

    fun getLocale(): Locale = when (current) {
        Language.FA -> Locale("fa")
        Language.EN -> Locale.ENGLISH
    }

    fun isRtl(): Boolean = current == Language.FA
}