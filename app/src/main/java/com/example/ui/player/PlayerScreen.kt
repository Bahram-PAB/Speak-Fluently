package com.example.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.utils.LocaleUtils

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    packageId: String,
    languageCode: String,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Trigger start of session when screen mounts
    LaunchedEffect(packageId) {
        viewModel.startSession(packageId)
    }

    // Intercept back button to clean up resources properly
    BackHandler {
        viewModel.resetSession()
        onBackToHome()
    }

    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val sessionState = uiState.sessionState) {
                is SessionState.Idle, is SessionState.Loading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading Practice Session...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                is SessionState.PlayingAudio -> {
                    val currentIndex = sessionState.currentQuestionIndex
                    
                    PracticeActiveContent(
                        title = uiState.currentPackage?.name ?: "",
                        currentIndex = currentIndex,
                        totalQuestions = uiState.questions.size,
                        isPlayingMode = true,
                        isPaused = uiState.isPaused,
                        pauseRemainingSeconds = 0,
                        maxPauseSeconds = uiState.settings.pauseDurationSeconds,
                        languageCode = languageCode,
                        onPauseClick = { viewModel.pauseSession() },
                        onResumeClick = { viewModel.resumeSession() },
                        onSkipClick = { viewModel.skipCurrent() },
                        onFinishClick = { viewModel.finishSession() }
                    )
                }

                is SessionState.PracticingPause -> {
                    val currentIndex = sessionState.currentQuestionIndex
                    
                    PracticeActiveContent(
                        title = uiState.currentPackage?.name ?: "",
                        currentIndex = currentIndex,
                        totalQuestions = uiState.questions.size,
                        isPlayingMode = false,
                        isPaused = uiState.isPaused,
                        pauseRemainingSeconds = sessionState.secondsRemaining,
                        maxPauseSeconds = uiState.settings.pauseDurationSeconds,
                        languageCode = languageCode,
                        onPauseClick = { viewModel.pauseSession() },
                        onResumeClick = { viewModel.resumeSession() },
                        onSkipClick = { viewModel.skipCurrent() },
                        onFinishClick = { viewModel.finishSession() }
                    )
                }

                is SessionState.Completed -> {
                    SessionCompletedCard(
                        packageName = uiState.currentPackage?.name ?: "",
                        languageCode = languageCode,
                        onBackToHome = {
                            viewModel.resetSession()
                            onBackToHome()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedWaveform(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val heights = listOf(16.dp, 24.dp, 32.dp, 20.dp, 12.dp, 24.dp, 28.dp, 16.dp)
    
    Row(
        modifier = modifier.height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEachIndexed { index, baseHeight ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 350 + (index * 80), easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(baseHeight * scale)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun QuestionCard(
    isPlayingMode: Boolean,
    pauseRemainingSeconds: Int,
    maxPauseSeconds: Int,
    languageCode: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Label Badge
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Text(
                    text = if (isPlayingMode) "LISTEN AND REPEAT" else "SPEAKING TIME",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            
            // Waveform (for visual excitement)
            AnimatedWaveform(
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Pause Countdown / Speaking progress
            if (!isPlayingMode) {
                val timerPercentage = if (maxPauseSeconds > 0) {
                    (pauseRemainingSeconds.toFloat() / maxPauseSeconds.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(72.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { timerPercentage },
                            strokeWidth = 4.dp,
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                        Text(
                            text = "${pauseRemainingSeconds}s",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SPEAKING TIME",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.sp
                    )
                }
            } else {
                // Audio is playing state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Playing Audio",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PLAYING QUESTION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PracticeActiveContent(
    title: String,
    currentIndex: Int,
    totalQuestions: Int,
    isPlayingMode: Boolean,
    isPaused: Boolean,
    pauseRemainingSeconds: Int,
    maxPauseSeconds: Int,
    languageCode: String,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onSkipClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper Progress bar
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { (currentIndex + 1) / totalQuestions.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = LocaleUtils.getString(context, R.string.question_count, languageCode, currentIndex + 1, totalQuestions),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // Central visual interactive focus point
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            QuestionCard(
                isPlayingMode = isPlayingMode,
                pauseRemainingSeconds = pauseRemainingSeconds,
                maxPauseSeconds = maxPauseSeconds,
                languageCode = languageCode,
                modifier = Modifier.fillMaxHeight()
            )
        }

        // Lower Controls Row (Professional player buttons matching Design HTML)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Exit / Finish Button (Left small circular outlined button)
                OutlinedIconButton(
                    onClick = onFinishClick,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("finish_session_btn"),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Session",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Play / Pause Toggle (Large rounded square primary button)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { if (isPaused) onResumeClick() else onPauseClick() }
                        .testTag(if (isPaused) "resume_session_btn" else "pause_session_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Refresh,
                        contentDescription = if (isPaused) "Play" else "Pause",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Skip Question (Right small circular outlined button)
                OutlinedIconButton(
                    onClick = onSkipClick,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("skip_question_btn"),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Skip Question",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SessionCompletedCard(
    packageName: String,
    languageCode: String,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = LocaleUtils.getString(context, R.string.session_completed, languageCode),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = packageName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onBackToHome,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(LocaleUtils.getString(context, R.string.back_to_home, languageCode))
            }
        }
    }
}
