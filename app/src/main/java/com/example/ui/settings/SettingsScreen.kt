package com.example.ui.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.Settings
import com.example.utils.LocaleUtils
import java.util.Calendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    currentLanguageCode: String,
    onLanguageChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settingsState.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()

    // Temporary states during configuration before hitting Save
    var questionsCount by remember(settings) { mutableStateOf(settings.questionsPerSession) }
    var pauseSeconds by remember(settings) { mutableStateOf(settings.pauseDurationSeconds) }
    var notificationsEnabled by remember(settings) { mutableStateOf(settings.notificationsEnabled) }
    var notificationTime by remember(settings) { mutableStateOf(settings.dailyNotificationTime) }
    var githubAudioRepo by remember(settings) { mutableStateOf(settings.githubAudioRepo) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val languages = listOf(
        Pair("fa", "فارسی (Persian)"),
        Pair("en", "English"),
        Pair("id", "Indonesian"),
        Pair("th", "Thai"),
        Pair("vi", "Vietnamese"),
        Pair("ms", "Malay")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = LocaleUtils.getString(context, R.string.settings_title, currentLanguageCode),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Language Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = LocaleUtils.getString(context, R.string.language_section, currentLanguageCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        languages.forEach { (code, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onLanguageChanged(code)
                                        // Save language change immediately so strings translate in real time
                                        viewModel.saveSettings(settings.copy(appLanguage = code))
                                    }
                                    .padding(vertical = 12.dp)
                                    .testTag("lang_option_$code"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentLanguageCode == code,
                                    onClick = {
                                        onLanguageChanged(code)
                                        viewModel.saveSettings(settings.copy(appLanguage = code))
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (currentLanguageCode == code) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // 2. Practice Configuration Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Practice Volume & Pauses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Questions per Session
                        Text(
                            text = "${LocaleUtils.getString(context, R.string.questions_per_session_label, currentLanguageCode)}: $questionsCount",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = questionsCount.toFloat(),
                            onValueChange = { questionsCount = it.toInt() },
                            valueRange = 5f..20f,
                            steps = 2, // Steps: 5, 10, 15, 20
                            modifier = Modifier.testTag("questions_count_slider")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Pause duration
                        Text(
                            text = "${LocaleUtils.getString(context, R.string.pause_duration_label, currentLanguageCode)}: $pauseSeconds",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        // Use segment selectors
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            val pauses = listOf(20, 30, 60, 90)
                            pauses.forEach { duration ->
                                FilterChip(
                                    selected = pauseSeconds == duration,
                                    onClick = { pauseSeconds = duration },
                                    label = { Text("$duration s") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("pause_chip_$duration")
                                )
                            }
                        }
                    }
                }
            }

            // 3. WorkManager Daily Schedule Notifications
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = LocaleUtils.getString(context, R.string.notification_settings, currentLanguageCode),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { notificationsEnabled = it },
                                modifier = Modifier.testTag("notification_switch")
                            )
                        }

                        if (notificationsEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val parts = notificationTime.split(":")
                                        val currentHour = parts.getOrNull(0)?.toIntOrNull() ?: 9
                                        val currentMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                        
                                        TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                notificationTime = String.format("%02d:%02d", hour, minute)
                                            },
                                            currentHour,
                                            currentMinute,
                                            true
                                        ).show()
                                    }
                                    .padding(vertical = 12.dp)
                                    .testTag("reminder_time_picker"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = LocaleUtils.getString(context, R.string.reminder_time_label, currentLanguageCode),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = notificationTime,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 3.5 GitHub Audio Repository Config
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "GitHub Audio Source",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Format: username/repository",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = githubAudioRepo,
                            onValueChange = { githubAudioRepo = it },
                            label = { Text("GitHub Repo Path") },
                            modifier = Modifier.fillMaxWidth().testTag("github_repo_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.checkAndDownloadAll(githubAudioRepo)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("check_and_download_btn"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(
                                text = if (currentLanguageCode == "fa") "بررسی دسترسی و دریافت آفلاین فایل‌ها" else "Check Access & Download Files Offline",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 4. Save Button
            item {
                Button(
                    onClick = {
                        val newSettings = Settings(
                            dailyNotificationTime = notificationTime,
                            notificationsEnabled = notificationsEnabled,
                            appLanguage = currentLanguageCode,
                            questionsPerSession = questionsCount,
                            pauseDurationSeconds = pauseSeconds,
                            githubAudioRepo = githubAudioRepo
                        )
                        viewModel.saveSettings(newSettings)
                        
                        // Show snackbar confirmation
                        val successMessage = LocaleUtils.getString(context, R.string.settings_saved_success, currentLanguageCode)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(successMessage)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("save_settings_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Done, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LocaleUtils.getString(context, R.string.save_settings_btn, currentLanguageCode),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    when (val state = downloadState) {
        is DownloadProgressState.Idle -> { /* Do nothing */ }
        is DownloadProgressState.CheckingAccess -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = {
                    Text(
                        text = if (currentLanguageCode == "fa") "بررسی اتصال" else "Checking Connection",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = if (currentLanguageCode == "fa") "در حال بررسی دسترسی به مخزن..." else "Checking repository accessibility...")
                    }
                }
            )
        }
        is DownloadProgressState.AccessSuccess -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = {
                    Text(
                        text = if (currentLanguageCode == "fa") "اتصال موفق" else "Connected Successfully",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = state.message)
                    }
                }
            )
        }
        is DownloadProgressState.Downloading -> {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {},
                title = {
                    Text(
                        text = if (currentLanguageCode == "fa") "دریافت فایل‌های تمرینی" else "Downloading Practice Files",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (currentLanguageCode == "fa") "در حال دریافت: ${state.progress}%" else "Downloading: ${state.progress}%",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (currentLanguageCode == "fa") "فایل ${state.currentFileIndex} از ${state.totalFiles}" else "File ${state.currentFileIndex} of ${state.totalFiles}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
        is DownloadProgressState.Finished -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetDownloadState() },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetDownloadState() }) {
                        Text(text = if (currentLanguageCode == "fa") "تایید" else "OK")
                    }
                },
                title = {
                    Text(
                        text = if (currentLanguageCode == "fa") "عملیات موفقیت‌آمیز" else "Download Completed",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Text(text = if (currentLanguageCode == "fa") "تمامی فایل‌های صوتی با موفقیت دریافت و ذخیره شدند!" else "All practice audio files downloaded and saved offline successfully!")
                }
            )
        }
        is DownloadProgressState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.resetDownloadState() },
                confirmButton = {
                    TextButton(onClick = { viewModel.resetDownloadState() }) {
                        Text(text = if (currentLanguageCode == "fa") "تایید" else "OK")
                    }
                },
                title = {
                    Text(
                        text = if (currentLanguageCode == "fa") "خطا" else "Error",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                text = {
                    Text(text = state.message)
                }
            )
        }
    }
}
