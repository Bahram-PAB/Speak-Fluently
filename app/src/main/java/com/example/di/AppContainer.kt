package com.example.di

import android.content.Context
import com.example.data.local.LocalSettingsDataSource
import com.example.data.remote.RemoteAudioPackageDataSource
import com.example.data.repository.AudioPackageRepositoryImpl
import com.example.domain.repository.AudioPackageRepository

class AppContainer(private val context: Context) {

    val localSettingsDataSource: LocalSettingsDataSource by lazy {
        LocalSettingsDataSource(context.applicationContext)
    }

    val remoteAudioPackageDataSource: RemoteAudioPackageDataSource by lazy {
        RemoteAudioPackageDataSource(context.applicationContext)
    }

    val audioPackageRepository: AudioPackageRepository by lazy {
        AudioPackageRepositoryImpl(
            context.applicationContext,
            localSettingsDataSource,
            remoteAudioPackageDataSource
        )
    }
}
