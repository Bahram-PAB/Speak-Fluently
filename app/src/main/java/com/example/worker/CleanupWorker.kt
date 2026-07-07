package com.example.worker

import android.app.Application
import android.content.Context
import androidx.work.WorkerParameters
import androidx.work.CoroutineWorker
import androidx.work.WorkerFactory
import androidx.work.ListenableWorker
import com.example.data.local.CompletedPackagesStore
import com.example.data.download.AudioDownloader
import kotlinx.coroutines.flow.first

class CleanupWorkerFactory(
    private val application: Application,
    private val downloader: AudioDownloader,
    private val completedStore: CompletedPackagesStore
) : WorkerFactory() {
    override fun createWorker(context: Context, workerClassName: String, params: WorkerParameters): ListenableWorker? {
        return when (workerClassName) {
            CleanupWorker::class.java.name -> CleanupWorker(application, params, downloader, completedStore)
            else -> null
        }
    }
}

class CleanupWorker(
    context: Context,
    params: WorkerParameters,
    private val downloader: AudioDownloader,
    private val completedStore: CompletedPackagesStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // حذف فایلهای قدیمی (بیش از ۷ روز)
            val maxAge = 7 * 24 * 60 * 60 * 1000L // 7 days in millis
            downloader.cleanupOldFiles(emptyMap(), maxAge)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}