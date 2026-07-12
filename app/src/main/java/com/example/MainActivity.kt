package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.example.data.local.SettingsStore
import com.example.ui.home.HomeScreen
import com.example.ui.player.PlayerScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.SpeakFluentlyTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val ds = SettingsStore(this)
        val langCode = runBlocking { ds.getLanguage().first() }
        applyLocale(langCode)

        setContent {
            var languageVersion by remember { mutableIntStateOf(0) }
            val lang = remember(languageVersion) { Lang.fromCode(langCode) }
            val context = LocalContext.current

            CompositionLocalProvider(LocalLayoutDirection provides if (lang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                var showSplash by remember { mutableStateOf(true) }

                SpeakFluentlyTheme(darkTheme = true) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        if (showSplash) {
                            SplashScreen(onFinished = { showSplash = false })
                        } else {
                            var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                            val settingsVm = remember { SettingsViewModel(context.applicationContext as android.app.Application) }

                            LaunchedEffect(Unit) {
                                settingsVm.language.collect { code ->
                                    Lang.current = Lang.Language.entries.first { it.code == code }
                                    applyLocale(code)
                                    languageVersion++
                                }
                            }

                            when (val s = screen) {
                                is Screen.Home -> HomeScreen(
                                    onExerciseClick = { id -> screen = Screen.Player(id) },
                                    onSettingsClick = { screen = Screen.Settings },
                                )
                                is Screen.Player -> PlayerScreen(exerciseId = s.id, onBackToHome = { screen = Screen.Home })
                                is Screen.Settings -> SettingsScreen(onBack = { screen = Screen.Home }, onLanguageChanged = { languageVersion++ })
                            }
                        }
                    }
                }
            }
        }
    }

    private fun applyLocale(code: String) {
        val locale = Locale(code)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}

private fun <T> runBlocking(block: suspend () -> T): T = kotlinx.coroutines.runBlocking { block() }

sealed class Screen {
    data object Home : Screen()
    data object Settings : Screen()
    data class Player(val id: Int) : Screen()
}