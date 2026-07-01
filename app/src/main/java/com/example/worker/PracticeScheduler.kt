package com.example.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object PracticeScheduler {
    const val WORK_NAME = "SpeakFluentlyDailyReminder"

    /**
     * Schedules a unique daily practice reminder at the requested time (HH:MM format).
     */
    fun scheduleDailyReminder(context: Context, timeString: String, enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val parts = timeString.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = Calendar.getInstance()
        if (calendar.before(now)) {
            // If the time has already passed today, schedule for tomorrow
            calendar.add(Calendar.DATE, 1)
        }

        val delayMs = calendar.timeInMillis - now.timeInMillis

        val workRequest = OneTimeWorkRequestBuilder<PracticeReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag("reminder_tag")
            .build()

        workManager.enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
