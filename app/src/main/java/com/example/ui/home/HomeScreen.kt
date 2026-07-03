package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.AudioPackage
import com.example.domain.repository.DownloadStatus
import com.example.utils.LocaleUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    languageCode: String,
    onStartPractice: (String) -> Unit,
    onNavigateToPremium: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var selectedPackageForDetails by remember { mutableStateOf<AudioPackage?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = LocaleUtils.getString(context, R.string.app_name, languageCode), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                            Text(text = LocaleUtils.getString(context, R.string.app_tagline, languageCode), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.onBackground),
                actions = {
                    IconButton(onClick = onNavigateToPremium, modifier = Modifier.testTag("premium_shortcut").clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.secondaryContainer)) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = "Premium", tint = if (uiState.premiumStatus.isPremium) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Text(text = LocaleUtils.getString(context, R.string.home_title, languageCode), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = LocaleUtils.getString(context, R.string.home_subtitle, languageCode), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                }

                items(uiState.packages) { pkg ->
                    val isLocked = pkg.isPremiumOnly && !uiState.premiumStatus.isPremium
                    PackageCard(
                        audioPackage = pkg,
                        isLocked = isLocked,
                        languageCode = languageCode,
                        downloadStatuses = uiState.downloadStatuses,
                        onDownloadClick = { file -> viewModel.downloadFile(file) },
                        onCardClick = { selectedPackageForDetails = if (selectedPackageForDetails?.id == pkg.id) null else pkg },
                        onStartPracticeClick = { onStartPractice(pkg.id) },
                        isExpanded = selectedPackageForDetails?.id == pkg.id
                    )
                }

                if (!uiState.premiumStatus.isPremium) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onNavigateToPremium() }.testTag("premium_promo_card")
                        ) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = LocaleUtils.getString(context, R.string.premium_title, languageCode), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Text(text = LocaleUtils.getString(context, R.string.premium_subtitle, languageCode), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                                }
                                Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PackageCard(
    audioPackage: AudioPackage,
    isLocked: Boolean,
    languageCode: String,
    downloadStatuses: Map<String, DownloadStatus>,
    onDownloadClick: (com.example.domain.model.AudioFile) -> Unit,
    onCardClick: () -> Unit,
    onStartPracticeClick: () -> Unit,
    isExpanded: Boolean
) {
    val context = LocalContext.current
    val totalFiles = audioPackage.files.size
    val downloadedFilesCount = audioPackage.files.count { file -> file.isDownloaded || downloadStatuses[file.id] is DownloadStatus.Success }
    val allDownloaded = downloadedFilesCount == totalFiles
    val hasDownloadError = audioPackage.files.any { file -> downloadStatuses[file.id] is DownloadStatus.Error }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isLocked) { onCardClick() }.testTag("package_card_${audioPackage.id}"),
        colors = CardDefaults.cardColors(containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = when {
                        isLocked -> MaterialTheme.colorScheme.errorContainer
                        allDownloaded -> MaterialTheme.colorScheme.primaryContainer
                        hasDownloadError -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                isLocked -> Icons.Default.Lock
                                allDownloaded -> Icons.Default.CheckCircle
                                hasDownloadError -> Icons.Default.Warning
                                else -> Icons.Default.Refresh
                            },
                            contentDescription = null,
                            tint = when {
                                isLocked -> MaterialTheme.colorScheme.onErrorContainer
                                allDownloaded -> MaterialTheme.colorScheme.onPrimaryContainer
                                hasDownloadError -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = audioPackage.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = when {
                            isLocked -> LocaleUtils.getString(context, R.string.premium_locked, languageCode)
                            allDownloaded -> LocaleUtils.getString(context, R.string.download_ready, languageCode)
                            hasDownloadError -> LocaleUtils.getString(context, R.string.download_error_label, languageCode)
                            else -> "${LocaleUtils.getString(context, R.string.download_required, languageCode)} ($downloadedFilesCount/$totalFiles)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isLocked -> MaterialTheme.colorScheme.error
                            allDownloaded -> MaterialTheme.colorScheme.primary
                            hasDownloadError -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outline
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = "Expand/Collapse")
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp).fillMaxWidth()) {
                    Text(text = audioPackage.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))

                    if (isLocked) {
                        Text(text = LocaleUtils.getString(context, R.string.premium_locked_desc, languageCode), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
                    } else {
                        Text(text = LocaleUtils.getString(context, R.string.package_contents_label, languageCode), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        
                        audioPackage.files.forEach { file ->
                            val fileDownloadStatus = downloadStatuses[file.id]
                            val isFileDownloaded = file.isDownloaded || fileDownloadStatus is DownloadStatus.Success
                            val isFileDownloading = fileDownloadStatus is DownloadStatus.Progress
                            val isFileError = fileDownloadStatus is DownloadStatus.Error

                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when {
                                        isFileDownloaded -> Icons.Default.CheckCircle
                                        isFileError -> Icons.Default.Warning
                                        else -> Icons.Default.Info
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        isFileDownloaded -> MaterialTheme.colorScheme.primary
                                        isFileError -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.outline
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = file.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                
                                if (isFileDownloading) {
                                    CircularProgressIndicator(progress = { (fileDownloadStatus as DownloadStatus.Progress).percentage / 100f }, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else if (isFileError) {
                                    IconButton(onClick = { onDownloadClick(file) }, modifier = Modifier.size(24.dp)) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = LocaleUtils.getString(context, R.string.retry_download_button, languageCode), modifier = Modifier.size(16.dp))
                                    }
                                } else if (!isFileDownloaded) {
                                    IconButton(onClick = { onDownloadClick(file) }, modifier = Modifier.size(24.dp)) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = LocaleUtils.getString(context, R.string.download_button, languageCode), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onStartPracticeClick,
                            enabled = allDownloaded && !hasDownloadError,
                            modifier = Modifier.fillMaxWidth().testTag("start_practice_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = LocaleUtils.getString(context, R.string.start_practice, languageCode))
                        }
                    }
                }
            }
        }
    }
}
