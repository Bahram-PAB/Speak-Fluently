package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AudioPackage
import com.example.utils.LocaleUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    languageCode: String,
    onStartPractice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocaleUtils.getString(context, R.string.app_name, languageCode)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(uiState.packages) { index, pkg ->
                    val isUnlocked = index == 0 || uiState.packages.take(index).all { prevPkg ->
                        uiState.completedPackageIds.contains(prevPkg.id)
                    }
                    PackageCard(
                        audioPackage = pkg,
                        packageNumber = index + 1,
                        isUnlocked = isUnlocked,
                        onStartPracticeClick = { if (isUnlocked) onStartPractice(pkg.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun PackageCard(
    audioPackage: AudioPackage,
    packageNumber: Int,
    isUnlocked: Boolean,
    onStartPracticeClick: () -> Unit
) {
    val alpha = if (isUnlocked) 1f else 0.5f

    Card(
        modifier = Modifier.fillMaxWidth().alpha(alpha),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$packageNumber",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = audioPackage.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${audioPackage.files.size} فایل صوتی",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            if (isUnlocked) {
                Button(onClick = onStartPracticeClick) {
                    Text("شروع")
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "قفل",
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
