package com.example.ui.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.BuildConfig
import com.example.Lang
import com.example.data.local.SettingsStore
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsStore = SettingsStore(application)
    val interval = settingsStore.getInterval()
    val language = settingsStore.getLanguage()

    fun setInterval(interval: Int) {
        viewModelScope.launch { settingsStore.setInterval(interval) }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch { settingsStore.setLanguage(code) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLanguageChanged: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val interval by viewModel.interval.collectAsState(initial = SettingsStore.DEFAULT_INTERVAL)
    val language by viewModel.language.collectAsState(initial = Lang.Language.FA.code)
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Lang.t("settings")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = Lang.t("back"))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Language selector
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(Lang.t("language"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(Lang.t("language_desc"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Lang.Language.entries.forEach { lang ->
                                FilterChip(
                                    selected = language == lang.code,
                                    onClick = { viewModel.setLanguage(lang.code); onLanguageChanged() },
                                    label = { Text(lang.displayName) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interval selector
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(Lang.t("interval_title"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(Lang.t("interval_desc"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SettingsStore.INTERVAL_OPTIONS.forEach { option ->
                                FilterChip(
                                    selected = interval == option,
                                    onClick = { viewModel.setInterval(option) },
                                    label = { Text("${option}s") }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${Lang.t("interval_current")}: ${interval} ${Lang.t("seconds")}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Footer — simple: name + version + YouTube link
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                Text(
                    text = "Speak Fluently",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@SpeakFluently_ir")))
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}