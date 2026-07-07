package com.example.ui.player

import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

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
    val context = LocalContext.current

    LaunchedEffect(exerciseId) {
        viewModel.loadExercise(exerciseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "تمرین") },
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
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
            // وضعیت دانلود
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

            val ex = exercise ?: return@Column
            if (currentIndex >= ex.files.size) return@Column
            val currentFile = ex.files[currentIndex]

            // شمارنده پیشرفت
            Text(
                text = "${currentIndex + 1} / ${ex.files.size}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(32.dp))

            // نام فایل فعلی
            Text(
                text = currentFile.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // دکمه‌های کنترل
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // دکمه تکرار
                FilledTonalButton(
                    onClick = { viewModel.onFileComplete() },
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "رد شدن")
                }

                // دکمه پخش/توقف
                var isPlaying by remember { mutableStateOf(false) }
                var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

                DisposableEffect(currentFile) {
                    onDispose { mediaPlayer?.release(); mediaPlayer = null }
                }

                LargeFloatingActionButton(
                    onClick = {
                        if (isPlaying) {
                            mediaPlayer?.pause()
                            isPlaying = false
                            viewModel.setPaused()
                        } else {
                            val file = currentFile
                            val localFile = file.localFile
                            if (localFile != null && localFile.exists()) {
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
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "توقف" else "پخش",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // پیام راهنما
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
                        text = "گوش دهید، تکرار کنید",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}