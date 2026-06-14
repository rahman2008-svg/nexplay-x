package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberPink
import com.example.ui.viewmodel.MediaViewModel
import java.io.File

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FileManagerComponent(
    viewModel: MediaViewModel,
    onBack: () -> Unit
) {
    val files by viewModel.files.collectAsState()
    val currentDir by viewModel.currentDirectory.collectAsState()
    val stats by viewModel.storageStats.collectAsState()
    val largeFiles by viewModel.largeFiles.collectAsState()
    val duplicates by viewModel.duplicateFiles.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Analyzer, 1 = Browser, 2 = Large Files, 3 = Duplicates
    var cleanupNotice by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NexPlay File Manager", style = MaterialTheme.typography.titleMedium, color = CyberCyan) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.cleanStorageCache()
                        cleanupNotice = "Cleared temporary files and cached media layers successfully."
                    }) {
                        Icon(Icons.Outlined.CleaningServices, contentDescription = "Clean Cache", tint = CyberPink)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Notification toast banner
            AnimatedVisibility(visible = cleanupNotice != null, enter = expandVertically(), exit = shrinkVertically()) {
                cleanupNotice?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberPink)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(msg, color = Color.Black, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                "Dismiss", 
                                modifier = Modifier.clickable { cleanupNotice = null }, 
                                color = Color.White, 
                                style = MaterialTheme.typography.bodySmall, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Top Tab Controls
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = CyberCyan,
                edgePadding = 12.dp
            ) {
                listOf("Analyzer", "Browser", "Large Files", "Duplicates").forEachIndexed { index, name ->
                    Tab(selected = activeTab == index, onClick = { activeTab = index }) {
                        Text(name, modifier = Modifier.padding(14.dp), color = if (activeTab == index) CyberCyan else Color.Gray)
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    0 -> {
                        // Storage Analyzer (Visual stats, sizes, bars)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val totalBytes = stats.videoSize + stats.audioSize + stats.imageSize + stats.docSize + stats.otherSize
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141414))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Total Media Chunk Used", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                        Text("${(totalBytes / (1024 * 1024))} MB", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        // Visual Gauge Bar
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(Color.DarkGray)
                                        ) {
                                            val max = totalBytes.coerceAtLeast(1L).toFloat()
                                            Box(modifier = Modifier.fillMaxHeight().weight((stats.videoSize / max).coerceAtLeast(0.01f)).background(CyberCyan))
                                            Box(modifier = Modifier.fillMaxHeight().weight((stats.audioSize / max).coerceAtLeast(0.01f)).background(CyberPink))
                                            Box(modifier = Modifier.fillMaxHeight().weight((stats.imageSize / max).coerceAtLeast(0.01f)).background(Color.Yellow))
                                            Box(modifier = Modifier.fillMaxHeight().weight(((stats.docSize + stats.otherSize) / max).coerceAtLeast(0.01f)).background(Color.Gray))
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Legends
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            LegendItem("Video", CyberCyan, stats.videoSize)
                                            LegendItem("Audio", CyberPink, stats.audioSize)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            LegendItem("Images", Color.Yellow, stats.imageSize)
                                            LegendItem("Docs/Misc", Color.Gray, stats.docSize + stats.otherSize)
                                        }
                                    }
                                }
                            }

                            item {
                                Button(
                                    onClick = {
                                        viewModel.cleanStorageCache()
                                        cleanupNotice = "Media cache cleared - Freed up device space!"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CleaningServices, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("One-Tap Smart Media Cleaner")
                                }
                            }
                        }
                    }
                    1 -> {
                        // Folder / Directory Explorer
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Current Path Breadcrumb
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF141414))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = CyberCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    currentDir ?: "Root / Private Storage Directory",
                                    color = Color.LightGray,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (currentDir != null) {
                                    IconButton(onClick = { viewModel.browseDirectory(null) }) {
                                        Icon(Icons.Default.Home, contentDescription = "Home Dir", tint = Color.White)
                                    }
                                }
                            }

                            if (files.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("This directory is empty", color = Color.Gray)
                                }
                            } else {
                                LazyColumn {
                                    items(files) { file ->
                                        val extColor = when {
                                            file.isVideo -> CyberCyan
                                            file.isAudio -> CyberPink
                                            file.isImage -> Color.Yellow
                                            file.isDirectory -> Color.White
                                            else -> Color.Gray
                                        }

                                        val icon = when {
                                            file.isDirectory -> Icons.Default.Folder
                                            file.isVideo -> Icons.Default.Movie
                                            file.isAudio -> Icons.Default.MusicNote
                                            file.isImage -> Icons.Default.Image
                                            else -> Icons.Default.InsertDriveFile
                                        }

                                        ListItem(
                                            headlineContent = { Text(file.name, color = extColor) },
                                            supportingContent = {
                                                if (file.isDirectory) {
                                                    Text("Folder Directory", color = Color.DarkGray)
                                                } else {
                                                    Text("${file.size / 1024} KB", color = Color.Gray)
                                                }
                                            },
                                            leadingContent = {
                                                Icon(icon, contentDescription = null, tint = extColor)
                                            },
                                            trailingContent = {
                                                IconButton(onClick = {
                                                    viewModel.deleteFileFromExplorer(file.path)
                                                    cleanupNotice = "Safely deleted file: ${file.name}"
                                                }) {
                                                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color.Gray)
                                                }
                                            },
                                            colors = ListItemDefaults.colors(containerColor = Color.Black),
                                            modifier = Modifier.combinedClickable(
                                                onClick = {
                                                    if (file.isDirectory) {
                                                        viewModel.browseDirectory(file.path)
                                                    }
                                                },
                                                onLongClick = {
                                                    // Move files to private vault on long click!
                                                    viewModel.hideMediaToVault(file.path, if (file.isVideo) "VIDEO" else if (file.isImage) "PHOTO" else "AUDIO")
                                                    cleanupNotice = "Moved ${file.name} securely into local vault!"
                                                }
                                            )
                                        )
                                        Divider(color = Color(0xFF141414))
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Large File Finder
                        if (largeFiles.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No heavy files over 5MB detected", color = Color.Gray)
                            }
                        } else {
                            LazyColumn {
                                items(largeFiles) { item ->
                                    ListItem(
                                        headlineContent = { Text(item.name, color = Color.White) },
                                        supportingContent = { Text("${item.size / (1024 * 1024)} MB • ${(item.path)}", color = Color.Gray) },
                                        leadingContent = {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = CyberPink)
                                        },
                                        trailingContent = {
                                            IconButton(onClick = {
                                                viewModel.deleteFileFromExplorer(item.path)
                                                cleanupNotice = "Removed heavy file!"
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray)
                                            }
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Black)
                                    )
                                    Divider(color = Color(0xFF141414))
                                }
                            }
                        }
                    }
                    3 -> {
                        // Duplicate Finder
                        if (duplicates.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Zero duplicate logs found. Space is fully optimal!", color = Color.Gray)
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            duplicates.forEach { pair ->
                                                viewModel.deleteFileFromExplorer(pair.second.path)
                                            }
                                            cleanupNotice = "Cleaned all redundant duplicate clones!"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberPink, contentColor = Color.Black)
                                    ) {
                                        Text("Delete All Duplicates")
                                    }
                                }

                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    items(duplicates) { pair ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF0C0C0C))
                                                .padding(8.dp)
                                        ) {
                                            Text("Original: ${pair.first.name}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                            Text("Duplicate copy: ${pair.second.name} (${pair.second.size / 1024} KB)", color = CyberPink, style = MaterialTheme.typography.bodySmall)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            IconButton(onClick = {
                                                viewModel.deleteFileFromExplorer(pair.second.path)
                                                cleanupNotice = "Removed copy successfully."
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray)
                                            }
                                            Divider(color = Color(0xFF222222))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color, size: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text("${size / (1024 * 1024)} MB", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
    }
}
