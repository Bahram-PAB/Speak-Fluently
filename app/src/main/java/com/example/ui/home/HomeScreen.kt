package com.example.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToExercise: (Int) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val exercises by viewModel.exercises.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تمرین‌های روزانه") },
                actions = {
                    IconButton(onClick = { viewModel.sync() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "همگام‌سازی")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToSettings,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Default.Settings, contentDescription = "تنظیمات")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (syncState) {
                is HomeViewModel.SyncState.Syncing -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is HomeViewModel.SyncState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(text = (syncState as HomeViewModel.SyncState.Success).message, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is HomeViewModel.SyncState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(text = (syncState as HomeViewModel.SyncState.Error).message, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                else -> {}
            }

            if (exercises.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.GetApp, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "هنوز تمرینی یافت نشد.\nبرای شروع دکمه همگام‌سازی را بزنید.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.sync() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("همگام‌سازی")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(bottom = 16.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(exercises) { exercise ->
                        ExerciseCard(exercise = exercise, onClick = { onNavigateToExercise(exercise.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseCard(exercise: Exercise, onClick: () -> Unit) {
    // Green for completed, surface for unlocked, muted for locked
    val containerColor = when {
        exercise.isCompleted -> Color(0xFFE8F5E9) // Light green
        exercise.isLocked -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !exercise.isLocked) { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (exercise.isCompleted) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4CAF50))
        } else {
            null
        }
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when {
                    exercise.isCompleted -> Icons.Default.CheckCircle
                    exercise.isLocked -> Icons.Default.Lock
                    else -> Icons.Default.LockOpen
                },
                contentDescription = when {
                    exercise.isCompleted -> "تکمیل شده"
                    exercise.isLocked -> "قفل شده"
                    else -> "باز"
                },
                tint = when {
                    exercise.isCompleted -> Color(0xFF4CAF50) // Green
                    exercise.isLocked -> MaterialTheme.colorScheme.outline
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${exercise.files.size} فایل صوتی",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (exercise.isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                )
            }
            // Status indicator: arrow for unlocked, lock icon for locked (visual cue already on left)
            if (exercise.isCompleted) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            } else if (!exercise.isLocked) {
                Text(
                    text = "→",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}