package com.example.ui.player

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.Lang

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    exerciseId: Int,
    onBackToHome: () -> Unit,
    BackHandler { onBackToHome() }
    viewModel: PlayerViewModel = viewModel()
) {
    val exercise by viewModel.exercise.collectAsState()
    val currentIndex by viewModel.currentFileIndex.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val intervalRemaining by viewModel.intervalRemaining.collectAsState()
    val isIntervalActive by viewModel.isIntervalActive.collectAsState()
    val autoPlaySignal by viewModel.autoPlaySignal.collectAsState()
    val banners by viewModel.banners.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(exerciseId) { viewModel.loadExercise(exerciseId) }

    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val ex = exercise
    val currentFile = ex?.files?.getOrNull(currentIndex)

    LaunchedEffect(autoPlaySignal) {
        if (autoPlaySignal > 0 && currentFile != null) {
            val localFile = currentFile.localFile
            if (localFile != null && localFile.exists()) {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(localFile.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener { isPlaying = false; viewModel.onFileComplete() }
                }
                isPlaying = true
                viewModel.setPlaying()
            }
        }
    }

    DisposableEffect(currentFile) {
        onDispose { mediaPlayer?.release(); mediaPlayer = null; isPlaying = false }
    }

    val progress = if (ex != null && ex.files.isNotEmpty()) (currentIndex + 1).toFloat() / ex.files.size.toFloat() else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(600, easing = FastOutSlowInEasing), label = "progress")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = { mediaPlayer?.release(); mediaPlayer = null; onBackToHome() }) {
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
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (downloadState) {
                is PlayerViewModel.DownloadState.Downloading -> {
                    Spacer(modifier = Modifier.height(60.dp))
                    CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 4.dp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(Lang.t("downloading"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    return@Column
                }
                is PlayerViewModel.DownloadState.Error -> {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(16.dp)) {
                        Text((downloadState as PlayerViewModel.DownloadState.Error).message, modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    return@Column
                }
                else -> {}
            }

            if (ex == null || currentFile == null) return@Column

            LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text("${currentIndex + 1} / ${ex.files.size}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(32.dp))

            if (isIntervalActive) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("$intervalRemaining", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text(Lang.t("seconds_to_next"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                    }
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(currentFile.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // ===== BANNERS SECTION (all matching) =====
            if (banners.isNotEmpty() && !isIntervalActive) {
                banners.forEach { banner ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = banner.hasUrl) {
                                banner.url?.let { url ->
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = banner.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            if (banner.hasUrl) {
                                Text(
                                    "→",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            // ===========================================

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = { mediaPlayer?.release(); mediaPlayer = null; isPlaying = false; viewModel.onFileComplete() }, modifier = Modifier.size(56.dp), shape = CircleShape, enabled = !isIntervalActive) {
                    Icon(Icons.Default.SkipNext, Lang.t("skip"), modifier = Modifier.size(28.dp))
                }
                Box(
                    modifier = Modifier.size(88.dp).shadow(12.dp, CircleShape).clip(CircleShape)
                        .background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))))
                        .clickable(enabled = !isIntervalActive) {
                            if (isPlaying) { mediaPlayer?.pause(); isPlaying = false; viewModel.setPaused() }
                            else {
                                val localFile = currentFile.localFile
                                if (localFile != null && localFile.exists()) {
                                    mediaPlayer?.release()
                                    mediaPlayer = MediaPlayer().apply { setDataSource(localFile.absolutePath); prepare(); start(); setOnCompletionListener { isPlaying = false; viewModel.onFileComplete() } }
                                    isPlaying = true; viewModel.setPlaying()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow, if (isPlaying) Lang.t("pause") else Lang.t("play"), modifier = Modifier.size(44.dp), tint = Color.White)
                }
                Spacer(modifier = Modifier.size(56.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            when (playbackState) {
                PlayerViewModel.PlaybackState.Completed -> {
                    Card(modifier = Modifier.fillMaxWidth().clickable { mediaPlayer?.release(); mediaPlayer = null; onBackToHome() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)), elevation = CardDefaults.cardElevation(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(Lang.t("training_complete"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(Lang.t("click_to_return"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                else -> {
                    Text(if (isIntervalActive) Lang.t("auto_next") else Lang.t("listening"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}