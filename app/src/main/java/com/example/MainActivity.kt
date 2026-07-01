package com.example

import android.Manifest
import android.content.Intent
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
import androidx.compose.material.icons.filled.Star
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
import com.example.ui.premium.PremiumScreen
import com.example.ui.premium.PremiumViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.LocaleUtils
import com.example.worker.PracticeReminderWorker

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled gracefully
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as SpeakFluentlyApplication).container
        
        // Request post notifications permission on Android 13+ (SDK 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                
                // Track current active language code inside Compose state
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.provideFactory(
                        appContainer.audioPackageRepository,
                        this
                    )
                )
                val settingsState by settingsViewModel.settingsState.collectAsState()
                var currentLanguageCode by remember { mutableStateOf("fa") }
                
                LaunchedEffect(settingsState.appLanguage) {
                    currentLanguageCode = settingsState.appLanguage
                }

                // Check if activity was launched from scheduled WorkManager practice reminders
                LaunchedEffect(intent) {
                    if (intent?.action == PracticeReminderWorker.ACTION_START_PRACTICE) {
                        // Navigate directly to the conversational English practice package
                        navController.navigate("player/pkg_conversational_english") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Show bottom nav bar only for root screens (don't show inside active audio practice)
                val showBottomBar = currentRoute in listOf("home", "premium", "settings")

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                val context = LocalContext.current
                                
                                NavigationBarItem(
                                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = null) },
                                    label = { Text(LocaleUtils.getString(context, R.string.nav_home, currentLanguageCode)) },
                                    selected = currentRoute == "home",
                                    onClick = {
                                        navController.navigate("home") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    modifier = Modifier.testTag("nav_item_home")
                                )

                                NavigationBarItem(
                                    icon = { Icon(imageVector = Icons.Default.Star, contentDescription = null) },
                                    label = { Text(LocaleUtils.getString(context, R.string.nav_premium, currentLanguageCode)) },
                                    selected = currentRoute == "premium",
                                    onClick = {
                                        navController.navigate("premium") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    modifier = Modifier.testTag("nav_item_premium")
                                )

                                NavigationBarItem(
                                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                                    label = { Text(LocaleUtils.getString(context, R.string.nav_settings, currentLanguageCode)) },
                                    selected = currentRoute == "settings",
                                    onClick = {
                                        navController.navigate("settings") {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
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
                                languageCode = currentLanguageCode,
                                onStartPractice = { packageId ->
                                    navController.navigate("player/$packageId")
                                },
                                onNavigateToPremium = {
                                    navController.navigate("premium") {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(
                            route = "player/{packageId}",
                            arguments = listOf(navArgument("packageId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val packageId = backStackEntry.arguments?.getString("packageId") ?: "pkg_conversational_english"
                            val playerViewModel: PlayerViewModel = viewModel(
                                factory = PlayerViewModel.provideFactory(
                                    appContainer.audioPackageRepository,
                                    this@MainActivity
                                )
                            )
                            PlayerScreen(
                                viewModel = playerViewModel,
                                packageId = packageId,
                                languageCode = currentLanguageCode,
                                onBackToHome = {
                                    navController.navigateUp()
                                }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                currentLanguageCode = currentLanguageCode,
                                onLanguageChanged = { newCode ->
                                    currentLanguageCode = newCode
                                }
                            )
                        }

                        composable("premium") {
                            val premiumViewModel: PremiumViewModel = viewModel(
                                factory = PremiumViewModel.provideFactory(appContainer.audioPackageRepository)
                            )
                            PremiumScreen(
                                viewModel = premiumViewModel,
                                languageCode = currentLanguageCode
                            )
                        }
                    }
                }
            }
        }
    }
}
