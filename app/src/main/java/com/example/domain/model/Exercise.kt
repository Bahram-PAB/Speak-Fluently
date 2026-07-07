package com.example.domain.model

import java.io.File

/**
 * یک تمرین روزانه (مثلاً "تمرین روز ۱")
 * @property id شناسه عددی تمرین (مثلاً ۱، ۲، ...)
 * @property name نام فارسی تمرین (مثلاً "تمرین روز ۱")
 * @property files لیست فایلهای صوتی این تمرین
 * @property isCompleted آیا تمرین کامل شده؟
 * @property lastCompletedAt آخرین زمان تکمیل (برای پاکسازی خودکار)
 */
data class Exercise(
    val id: Int,
    val name: String,
    val files: List<ExerciseFile>,
    val isCompleted: Boolean = false,
    val lastCompletedAt: Long? = null
) {
    val isLocked: Boolean get() = !isCompleted && id > 1
}

/**
 * یک فایل صوتی داخل تمرین
 * @property id شناسه فایل (مثلاً ۱، ۲، ...)
 * @property title عنوان فارسی فایل (مثلاً "سوال ۱")
 * @property audioUrl آدرس دانلود فایل از گیتهاب
 * @property localFile فایل محلی ذخیره شده (null اگر دانلود نشده)
 */
data class ExerciseFile(
    val id: Int,
    val title: String,
    val audioUrl: String,
    val localFile: File? = null
) {
    val isDownloaded: Boolean get() = localFile != null && localFile.exists()
}