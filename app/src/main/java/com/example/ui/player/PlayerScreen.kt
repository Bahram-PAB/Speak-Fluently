package com.example.ui.player

import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    exerciseId: Int,
    onBackToHome: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val exercise by viewModel.exercise.collectAsState()
    val currentIndex by viewModel.currentFileIndex.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val intervalRemaining by viewModel.intervalRemaining.collectAsState()
    val isIntervalActive by viewModel.isIntervalActive.collectAsState()
    val autoPlaySignal by viewModel.autoPlaySignal.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(exerciseId) {
        viewModel.loadExercise(exerciseId)
    }

    // MediaPlayer state
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Get current file
    val ex = exercise
    val currentFile = ex?.files?.getOrNull(currentIndex)

    // Auto-play: when autoPlaySignal changes and interval is done, start playing
    LaunchedEffect(autoPlaySignal) {
        if (autoPlaySignal > 0 && currentFile != null) {
            val localFile = currentFile.localFile
            if (localFile != null && localFile.exists()) {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(localFile.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        isPlaying = false
                        viewModel.onFileComplete()
                    }
                }
                isPlaying = true
                viewModel.setPlaying()
            }
        }
    }

    // Cleanup on file change or dispose
    DisposableEffect(currentFile) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "تمرین") },
                navigationIcon = {
                    IconButton(onClick = {
                        mediaPlayer?.release()
                        mediaPlayer = null
                        onBackToHome()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Download state
            when (downloadState) {
                is PlayerViewModel.DownloadState.Downloading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("در حال دانلود فایل‌ها...")
                    return@Column
                }
                is PlayerViewModel.DownloadState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = (downloadState as PlayerViewModel.DownloadState.Error).message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    return@Column
                }
                else -> {}
            }

            if (ex == null || currentFile == null) return@Column

            // Interval timer countdown card
            if (isIntervalActive) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "تایمر",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$intervalRemaining ثانیه تا فایل بعدی...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // File counter
            Text(
                text = "${currentIndex + 1} / ${ex.files.size}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(32.dp))

            // File title
            Text(
                text = currentFile.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip button
                FilledTonalButton(
                    onClick = {
                        mediaPlayer?.release()
                        mediaPlayer = null
                        isPlaying = false
                        viewModel.onFileComplete()
                    },
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    enabled = !isIntervalActive
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "رد شدن")
                }

                // Play/Pause button
                LargeFloatingActionButton(
                    onClick = {
                        if (isIntervalActive) return@LargeFloatingActionButton

                        if (isPlaying) {
                            mediaPlayer?.pause()
                            isPlaying = false
                            viewModel.setPaused()
                        } else {
                            val localFile = currentFile.localFile
                            if (localFile != null && localFile.exists()) {
                                mediaPlayer?.release()
                                mediaPlayer = MediaPlayer().apply {
                                    setDataSource(localFile.absolutePath)
                                    prepare()
                                    start()
                                    setOnCompletionListener {
                                        isPlaying = false
                                        viewModel.onFileComplete()
                                    }
                                }
                                isPlaying = true
                                viewModel.setPlaying()
                            }
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "توقف" else "پخش",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Status
            when (playbackState) {
                PlayerViewModel.PlaybackState.Completed -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "✅ تمرین تکمیل شد!",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                else -> {
                    Text(
                        text = if (isIntervalActive) "فایل بعدی به صورت خودکار پخش می‌شود..."
                               else "گوش دهید، تکرار کنید",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}