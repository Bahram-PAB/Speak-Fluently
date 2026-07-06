package com.example.ui.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.domain.model.Settings
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

    var questionsCount by remember(settings) { mutableStateOf(settings.questionsPerSession) }
    var pauseSeconds by remember(settings) { mutableStateOf(settings.pauseDurationSeconds) }
    var notificationsEnabled by remember(settings) { mutableStateOf(settings.notificationsEnabled) }
    var notificationTime by remember(settings) { mutableStateOf(settings.dailyNotificationTime) }
    var githubAudioRepo by remember(settings) { mutableStateOf(settings.githubAudioRepo) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "تنظیمات تمرین", fontWeight = FontWeight.Bold)
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
            // Practice Configuration
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "تنظیمات تمرین",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "تعداد سوالات در هر جلسه: $questionsCount",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = questionsCount.toFloat(),
                            onValueChange = { questionsCount = it.toInt() },
                            valueRange = 5f..20f,
                            steps = 2,
                            modifier = Modifier.testTag("questions_count_slider")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "زمان مکث بین سوالات: $pauseSeconds ثانیه",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            listOf(20, 30, 60, 90).forEach { duration ->
                                FilterChip(
                                    selected = pauseSeconds == duration,
                                    onClick = { pauseSeconds = duration },
                                    label = { Text("$duration ثانیه") },
                                    modifier = Modifier.weight(1f).testTag("pause_chip_$duration")
                                )
                            }
                        }
                    }
                }
            }

            // Notifications
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
                                Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "یادآوری روزانه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it }, modifier = Modifier.testTag("notification_switch"))
                        }
                        if (notificationsEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val parts = notificationTime.split(":")
                                    val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
                                    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                    TimePickerDialog(context, { _, hour, minute -> notificationTime = String.format("%02d:%02d", hour, minute) }, h, m, true).show()
                                }.padding(vertical = 12.dp).testTag("reminder_time_picker"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "زمان یادآوری", style = MaterialTheme.typography.bodyLarge)
                                Text(text = notificationTime, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // GitHub Audio Source
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "منبع فایل‌های صوتی",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "فرمت: username/repository",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = githubAudioRepo,
                            onValueChange = { githubAudioRepo = it },
                            label = { Text("آدرس مخزن گیت‌هاب") },
                            modifier = Modifier.fillMaxWidth().testTag("github_repo_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.checkGithubAccess(githubAudioRepo) },
                            modifier = Modifier.fillMaxWidth().testTag("check_access_btn"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(text = "بررسی دسترسی و ذخیره", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Save
            item {
                Button(
                    onClick = {
                        viewModel.saveSettings(
                            Settings(
                                dailyNotificationTime = notificationTime,
                                notificationsEnabled = notificationsEnabled,
                                appLanguage = "fa",
                                questionsPerSession = questionsCount,
                                pauseDurationSeconds = pauseSeconds,
                                githubAudioRepo = githubAudioRepo
                            )
                        )
                        coroutineScope.launch { snackbarHostState.showSnackbar("تنظیمات با موفقیت ذخیره شد!") }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("save_settings_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Done, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "ذخیره تنظیمات", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    // Download progress dialogs
    when (val state = downloadState) {
        is DownloadProgressState.Idle -> {}
        is DownloadProgressState.CheckingAccess -> {
            AlertDialog(onDismissRequest = {}, confirmButton = {}, title = { Text("بررسی اتصال", fontWeight = FontWeight.Bold) }, text = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("در حال بررسی دسترسی به مخزن...")
                }
            })
        }
        is DownloadProgressState.AccessSuccess -> {
            AlertDialog(onDismissRequest = {}, confirmButton = {}, title = { Text("اتصال موفق", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }, text = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(state.message)
                }
            })
        }
        is DownloadProgressState.Downloading -> {
            AlertDialog(onDismissRequest = {}, confirmButton = {}, title = { Text("دریافت فایل‌های تمرینی", fontWeight = FontWeight.Bold) }, text = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(progress = { state.progress / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp), color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("در حال دریافت: ${state.progress}%", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("فایل ${state.currentFileIndex} از ${state.totalFiles}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            })
        }
        is DownloadProgressState.Finished -> {
            AlertDialog(onDismissRequest = { viewModel.resetDownloadState() }, confirmButton = {
                TextButton(onClick = { viewModel.resetDownloadState() }) { Text("تایید") }
            }, title = {
                Text(
                    text = if (state.failedCount == 0) "عملیات موفق" else if (state.successCount > 0) "دریافت ناقص" else "خطا در دریافت",
                    fontWeight = FontWeight.Bold,
                    color = if (state.failedCount > 0 && state.successCount == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }, text = {
                val msg = if (state.failedCount == 0) "تمامی فایل‌ها (${state.successCount}) با موفقیت دریافت شدند!"
                else if (state.successCount > 0) "${state.successCount} فایل دریافت شد، ${state.failedCount} فایل با خطا مواجه شد."
                else "خطا در دریافت فایل‌ها! لطفاً اتصال اینترنت و آدرس مخزن را بررسی کنید."
                Text(text = msg)
            })
        }
        is DownloadProgressState.Error -> {
            AlertDialog(onDismissRequest = { viewModel.resetDownloadState() }, confirmButton = {
                TextButton(onClick = { viewModel.resetDownloadState() }) { Text("تایید") }
            }, title = { Text("خطا", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) }, text = { Text(state.message) })
        }
    }
}
