package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.di.AppContainer
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.player.PlayerScreen
import com.example.ui.player.PlayerViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.SpeakFluentlyTheme
import com.example.utils.LocaleUtils

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContainer = AppContainer(applicationContext)

        // Request notification permission if not granted (for Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            SpeakFluentlyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Hardcode language to Persian
                    val languageCode = "fa"
                    LocaleUtils.setLocale(this, languageCode)

                    val homeViewModel: HomeViewModel by viewModels { HomeViewModel.provideFactory(appContainer.audioPackageRepository, appContainer.localSettingsDataSource) }
                    val playerViewModel: PlayerViewModel by viewModels { PlayerViewModel.provideFactory(appContainer.audioPackageRepository, appContainer.localSettingsDataSource) }
                    val settingsViewModel: SettingsViewModel by viewModels { SettingsViewModel.provideFactory(appContainer.audioPackageRepository, appContainer.localSettingsDataSource) }

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                viewModel = homeViewModel,
                                languageCode = languageCode,
                                onStartPractice = { packageId -> navController.navigate("player/$packageId") },
                                onNavigateToSettings = { navController.navigate("settings") } // Add settings navigation
                            )
                        }
                        composable("player/{packageId}") {
                            val packageId = it.arguments?.getString("packageId")
                            if (packageId != null) {
                                PlayerScreen(
                                    viewModel = playerViewModel,
                                    languageCode = languageCode,
                                    packageId = packageId,
                                    onBack = { navController.popBackStack() },
                                    onCompletePackage = { // Handle package completion
                                        homeViewModel.markPackageAsCompleted(packageId)
                                        navController.popBackStack() // Go back to home after completion
                                    }
                                )
                            } else {
                                // Handle error or navigate back
                                navController.popBackStack()
                            }
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                currentLanguageCode = languageCode,
                                onLanguageChanged = { /* Language is hardcoded, so no action needed */ }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Notification permission granted
            } else {
                // Notification permission denied
            }
        }
    }
}
