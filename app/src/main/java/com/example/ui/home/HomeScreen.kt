package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.Lang
import com.example.R
import com.example.domain.model.Exercise
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToExercise: (Int) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val exercises by viewModel.exercises.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    var showSyncMessage by remember { mutableStateOf(false) }

    // Auto-dismiss sync message after 10 seconds
    LaunchedEffect(syncState) {
        if (syncState is HomeViewModel.SyncState.Success) {
            showSyncMessage = true
            delay(10_000)
            showSyncMessage = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Lang.t("daily_exercises")) },
                actions = {
                    IconButton(onClick = { viewModel.sync() }) {
                        Icon(Icons.Default.Refresh, contentDescription = Lang.t("sync"))
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
                Icon(Icons.Default.Settings, contentDescription = Lang.t("settings"))
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Sync progress — animated logo
            if (syncState is HomeViewModel.SyncState.Syncing) {
                SyncLoadingIndicator()
            }

            // Sync completion/error message (auto-dismiss after 10s)
            AnimatedVisibility(
                visible = showSyncMessage && syncState is HomeViewModel.SyncState.Success,
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = (syncState as? HomeViewModel.SyncState.Success)?.message ?: "",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (syncState is HomeViewModel.SyncState.Error && !showSyncMessage) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = (syncState as HomeViewModel.SyncState.Error).message,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (exercises.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.GetApp, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = Lang.t("no_exercises"), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.sync() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Lang.t("sync"))
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
    val containerColor = when {
        exercise.isCompleted -> Color(0xFFE8F5E9)
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
        } else null
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when {
                    exercise.isCompleted -> Icons.Default.CheckCircle
                    exercise.isLocked -> Icons.Default.Lock
                    else -> Icons.Default.LockOpen
                },
                contentDescription = when {
                    exercise.isCompleted -> Lang.t("completed")
                    exercise.isLocked -> Lang.t("locked")
                    else -> Lang.t("unlocked")
                },
                tint = when {
                    exercise.isCompleted -> Color(0xFF4CAF50)
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
                    text = "${exercise.files.size} ${Lang.t("files_count")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (exercise.isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                )
            }
            if (exercise.isCompleted) {
                Text("✓", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            } else if (!exercise.isLocked) {
                Text("→", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun SyncLoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "sync")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoAlpha"
    )
    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = com.example.R.drawable.splash_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
                    .alpha(alpha)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                Lang.t("syncing"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}