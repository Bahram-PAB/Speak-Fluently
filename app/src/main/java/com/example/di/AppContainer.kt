package com.example.di

import android.app.Application
import com.example.data.download.AudioDownloader
import com.example.data.local.CompletedPackagesStore
import com.example.data.local.SyncPreferences
import com.example.data.remote.GithubTreeApi
import com.example.data.repository.AudioExerciseRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class AppContainer(val application: Application) {
    // Network
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        .build()

    val githubApi = GithubTreeApi(httpClient)
    val audioDownloader = AudioDownloader(application, httpClient)
    val completedStore = CompletedPackagesStore(application)
    val syncPrefs = SyncPreferences(application)

    // Repository (Singleton)
    val exerciseRepository = AudioExerciseRepository(
        githubApi = githubApi,
        downloader = audioDownloader,
        completedStore = completedStore,
        syncPrefs = syncPrefs
    )
}