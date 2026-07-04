package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.player.PlayerScreen
import com.example.ui.player.PlayerViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.LocaleUtils
import com.example.worker.PracticeReminderWorker

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as SpeakFluentlyApplication).container
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.provideFactory(appContainer.audioPackageRepository, this)
                )
                val settingsState by settingsViewModel.settingsState.collectAsState()
                val languageCode = "fa"

                LaunchedEffect(intent) {
                    if (intent?.action == PracticeReminderWorker.ACTION_START_PRACTICE) {
                        navController.navigate("player/pkg_daily_1") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showBottomBar = currentRoute in listOf("home", "settings")

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                val context = LocalContext.current
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    label = { Text(LocaleUtils.getString(context, R.string.nav_home, languageCode)) },
                                    selected = currentRoute == "home",
                                    onClick = {
                                        navController.navigate("home") {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    modifier = Modifier.testTag("nav_item_home")
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    label = { Text(LocaleUtils.getString(context, R.string.nav_settings, languageCode)) },
                                    selected = currentRoute == "settings",
                                    onClick = {
                                        navController.navigate("settings") {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    modifier = Modifier.testTag("nav_item_settings")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            val homeViewModel: HomeViewModel = viewModel(
                                factory = HomeViewModel.provideFactory(appContainer.audioPackageRepository)
                            )
                            HomeScreen(
                                viewModel = homeViewModel,
                                languageCode = languageCode,
                                onStartPractice = { packageId -> navController.navigate("player/$packageId") }
                            )
                        }

                        composable(
                            route = "player/{packageId}",
                            arguments = listOf(navArgument("packageId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val packageId = backStackEntry.arguments?.getString("packageId") ?: "pkg_daily_1"
                            val playerViewModel: PlayerViewModel = viewModel(
                                factory = PlayerViewModel.provideFactory(appContainer.audioPackageRepository, this@MainActivity)
                            )
                            PlayerScreen(
                                viewModel = playerViewModel,
                                packageId = packageId,
                                languageCode = languageCode,
                                onBackToHome = { navController.navigateUp() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                currentLanguageCode = languageCode,
                                onLanguageChanged = { }
                            )
                        }
                    }
                }
            }
        }
    }
}
