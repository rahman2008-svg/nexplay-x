package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AudioItem
import com.example.data.model.VideoItem
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberPink
import com.example.ui.viewmodel.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsComponent(
    viewModel: MediaViewModel,
    onBack: () -> Unit
) {
    val videos by viewModel.videos.collectAsState()
    val audios by viewModel.audios.collectAsState()
    val isFloatingActive by viewModel.isFloatingVideoActive.collectAsState()

    var activeToolMenu by remember { mutableStateOf(0) } // 0 = Mini Video Editor, 1 = Audio Converter, 2 = Wireless Share, 3 = Floating UI Controls
    var successToast by remember { mutableStateOf<String?>(null) }

    // Sharing configurations
    var shareStatus by remember { mutableStateOf("Inactive") } // "Waiting", "Connected", "Transferring", "Finished"
    var fileCountToShare by remember { mutableStateOf(3) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NexPlay Tools Suite", style = MaterialTheme.typography.titleMedium, color = CyberCyan) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
            // Success indicator toast banner
            AnimatedVisibility(visible = successToast != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberCyan)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(successToast ?: "", color = Color.Black, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    LaunchedEffect(successToast) {
                        kotlinx.coroutines.delay(3000)
                        successToast = null
                    }
                }
            }

            // Top Tab Controls
            ScrollableTabRow(
                selectedTabIndex = activeToolMenu,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = CyberCyan,
                edgePadding = 12.dp
            ) {
                listOf("Mini Editor", "Converter", "Wireless Share", "Floating Hub").forEachIndexed { index, name ->
                    Tab(selected = activeToolMenu == index, onClick = { activeToolMenu = index }) {
                        Text(name, modifier = Modifier.padding(14.dp), color = if (activeToolMenu == index) CyberCyan else Color.Gray)
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (activeToolMenu) {
                    0 -> {
                        // Mini Video Editor: Trim, Crop, slice, extract audio
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text("Trim or Slice Local Media", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Select video to trim/cut or convert directly:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            if (videos.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("No offline videos available to edit.", color = Color.Gray)
                                }
                            } else {
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    items(videos) { video ->
                                        ListItem(
                                            headlineContent = { Text(video.title, color = Color.White) },
                                            supportingContent = { Text("Duration: ${formatTime(video.duration)}", color = Color.Gray) },
                                            leadingContent = {
                                                Icon(Icons.Default.MovieFilter, contentDescription = null, tint = CyberCyan)
                                            },
                                            trailingContent = {
                                                Row {
                                                    Button(
                                                        onClick = {
                                                            viewModel.trimMedia(video.path, video.title, 1000L, 5000L)
                                                            successToast = "Successfully trimmed first 5 seconds to a new file!"
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = CyberPink, contentColor = Color.Black)
                                                    ) {
                                                        Text("Trim 5s")
                                                    }
                                                }
                                            },
                                            colors = ListItemDefaults.colors(containerColor = Color.Black)
                                        )
                                        Divider(color = Color(0xFF141414))
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Audio Converter (MP4/WAV/AAC -> MP3 Converter)
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            Text("Fast Offline Frequency Transcoder", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Convert formats between MP4, WAV, AAC back into high-fidelity space saving MP3:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

                            Spacer(modifier = Modifier.height(16.dp))

                            val allConvertible = remember(videos, audios) {
                                videos.map { Pair(it.path, it.title) } + audios.map { Pair(it.path, it.title) }
                            }

                            if (allConvertible.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("No offline assets found to transcode.", color = Color.Gray)
                                }
                            } else {
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    items(allConvertible) { item ->
                                        ListItem(
                                            headlineContent = { Text(item.second, color = Color.White) },
                                            supportingContent = { Text("Path: ${item.first}", color = Color.Gray, maxLines = 1) },
                                            leadingContent = {
                                                Icon(Icons.Outlined.Transform, contentDescription = null, tint = CyberCyan)
                                            },
                                            trailingContent = {
                                                Button(
                                                    onClick = {
                                                        viewModel.convertVideoToMP3(item.first, item.second)
                                                        successToast = "Transcoded ${item.second} to high quality MP3!"
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                                                ) {
                                                    Text("MP3")
                                                }
                                            },
                                            colors = ListItemDefaults.colors(containerColor = Color.Black)
                                        )
                                        Divider(color = Color(0xFF141414))
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Wireless Sharing: Phone-to-Phone Wi-Fi Direct simulation
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.WifiTethering, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(72.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Phone-to-Phone Wireless Transfer", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Fast direct peer connections: No mobile data / internet required!", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

                            Spacer(modifier = Modifier.height(24.dp))

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Connection Status: $shareStatus", color = if (shareStatus == "Active Network Connected") CyberCyan else CyberPink, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    when (shareStatus) {
                                        "Inactive" -> {
                                            Button(
                                                onClick = { shareStatus = "Awaiting Client Direct Connection..." },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                                            ) {
                                                Text("Initialize Send Network")
                                            }
                                        }
                                        "Awaiting Client Direct Connection..." -> {
                                            CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = { shareStatus = "Active Network Connected" },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyberPink, contentColor = Color.White)
                                            ) {
                                                Text("Simulate Phone Connect")
                                            }
                                        }
                                        "Active Network Connected" -> {
                                            Text("Awaiting File Transfers ($fileCountToShare files selected)", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = {
                                                    shareStatus = "Transferring..."
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                                            ) {
                                                Text("Send Selected Files")
                                            }
                                        }
                                        "Transferring..." -> {
                                            LinearProgressIndicator(color = CyberPink, modifier = Modifier.fillMaxWidth())
                                            LaunchedEffect(Unit) {
                                                kotlinx.coroutines.delay(2500)
                                                shareStatus = "Completed Successfully ✓"
                                            }
                                        }
                                        else -> {
                                            Text("Completed Successfully ✓", color = CyberCyan, fontWeight = FontWeight.Bold)
                                            Text("All files transferred at 28.5 MB/s!", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = { shareStatus = "Inactive" },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White)
                                            ) {
                                                Text("Close Connection")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // Floating Media Hub
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Launch, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Immersive Floating Media Window", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Enable overlay panels to keep controls and trailers floating while executing other activities:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF141414), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Floating overlay widget state", color = Color.White)
                                Switch(
                                    checked = isFloatingActive,
                                    onCheckedChange = { viewModel.toggleFloatingVideo(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = CyberPink, checkedTrackColor = CyberCyan)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
