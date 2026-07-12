package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.local.SettingsStore
import com.example.ui.home.HomeScreen
import com.example.ui.player.PlayerScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.SpeakFluentlyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply saved language before setContent
        val store = SettingsStore(applicationContext)
        kotlinx.coroutines.runBlocking { store.applySavedLanguage() }

        setContent {
            var languageVersion by remember { mutableIntStateOf(0) }
            val scope = rememberCoroutineScope()

            // Force recomposition when language changes
            key(languageVersion) {
                SpeakFluentlyTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        NavHost(
                            navController = navController,
                            startDestination = "home"
                        ) {
                            composable("home") {
                                HomeScreen(
                                    onNavigateToExercise = { exerciseId ->
                                        navController.navigate("player/$exerciseId")
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate("settings")
                                    }
                                )
                            }
                            composable("player/{exerciseId}") { backStackEntry ->
                                val exerciseId = backStackEntry.arguments?.getString("exerciseId")?.toIntOrNull() ?: 1
                                PlayerScreen(
                                    exerciseId = exerciseId,
                                    onBackToHome = { navController.popBackStack() }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    onBack = { navController.popBackStack() },
                                    onLanguageChanged = {
                                        languageVersion++
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}