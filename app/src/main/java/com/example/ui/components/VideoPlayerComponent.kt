package com.example.ui.components

import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.VideoItem
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberPink
import com.example.ui.viewmodel.MediaViewModel
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerComponent(
    viewModel: MediaViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentVideo by viewModel.currentVideo.collectAsState()
    val isPlaying by viewModel.isVideoPlaying.collectAsState()
    val speed by viewModel.videoPlaybackSpeed.collectAsState()
    val progress by viewModel.videoProgress.collectAsState()
    val subtitleText by viewModel.videoSubtitle.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val audioBoostVal by viewModel.audioBoostPower.collectAsState()
    val sleepMinutes by viewModel.sleepTimerMinutes.collectAsState()

    var aspectState by remember { mutableStateOf(0) } // 0 = Fit, 1 = Stretch, 2 = 16:9
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var gesturePrompt by remember { mutableStateOf("Swipe UP/DOWN on left for Audio Boost") }

    // Dimming overlay for controls
    var showControls by remember { mutableStateOf(true) }

    // Toggle controls visibility with a timer
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(5000)
            showControls = false
        }
    }

    // Video playback timer ticking simulation for local paths
    LaunchedEffect(isPlaying, currentVideo, speed) {
        if (isPlaying && currentVideo != null) {
            while (isPlaying) {
                delay((1000 / speed).toLong())
                val currentTrack = currentVideo
                if (currentTrack != null) {
                    val nextProgress = progress + 1000L
                    if (nextProgress >= currentTrack.duration) {
                        viewModel.updateVideoProgress(0L)
                        viewModel.toggleVideoPlayback()
                    } else {
                        viewModel.updateVideoProgress(nextProgress)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NexPlay Video Engine", style = MaterialTheme.typography.titleMedium, color = CyberCyan) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showSleepTimer = true }) {
                        Icon(Icons.Default.Timer, contentDescription = "Sleep Timer", tint = if (sleepMinutes > 0) CyberPink else Color.White)
                    }
                    IconButton(onClick = {
                        aspectState = (aspectState + 1) % 3
                        gesturePrompt = "Aspect Fit Layer: " + when (aspectState) {
                            0 -> "Fit"
                            1 -> "Stretch full"
                            else -> "16:9 Cinema"
                        }
                    }) {
                        Icon(Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = CyberCyan)
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
            // Main Playing Screen
            currentVideo?.let { video ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(Color.DarkGray)
                        .pointerInput(Unit) {
                            // Touch gesture detectors
                            detectTapGestures(
                                onTap = { showControls = !showControls },
                                onDoubleTap = { offset ->
                                    // Swipe seek skip 10s back or forward based on side of tap
                                    if (offset.x < size.width / 2) {
                                        val target = (progress - 10000L).coerceAtLeast(0L)
                                        viewModel.updateVideoProgress(target)
                                        gesturePrompt = "Skipped 10s BACK"
                                    } else {
                                        val target = (progress + 10000L).coerceAtMost(video.duration)
                                        viewModel.updateVideoProgress(target)
                                        gesturePrompt = "Skipped 10s FORWARD"
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount < -10f) {
                                    val nextBoost = (audioBoostVal + 0.25f).coerceAtMost(4.0f)
                                    viewModel.setAudioBoost(nextBoost)
                                    gesturePrompt = "Audio Boost: ${String.format("%.2f", nextBoost)}x"
                                } else if (dragAmount > 10f) {
                                    val nextBoost = (audioBoostVal - 0.25f).coerceAtLeast(1.0f)
                                    viewModel.setAudioBoost(nextBoost)
                                    gesturePrompt = "Audio Boost: ${String.format("%.2f", nextBoost)}x"
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val file = File(video.path)
                    if (file.exists() && file.length() > 50000) {
                        // Native player for files possessing real encoding
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setVideoPath(video.path)
                                    setOnPreparedListener { mp ->
                                        mp.isLooping = true
                                        if (isPlaying) start()
                                    }
                                }
                            },
                            update = { view ->
                                if (isPlaying) {
                                    view.start()
                                } else {
                                    view.pause()
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Immersive Cinematic waveform player fallback when standard files not indexed code
                        val infiniteTransition = rememberInfiniteTransition()
                        val pulse by infiniteTransition.animateFloat(
                            initialValue = 0.5f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = EaseInOutSine),
                                repeatMode = RepeatMode.Reverse
                            )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF20003B), Color.Black)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.MovieFilter,
                                    contentDescription = null,
                                    tint = CyberCyan.copy(alpha = pulse),
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Offline Cinema Streaming", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                Text("Playing: ${video.title}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Floating Controls Overlay
                    if (showControls) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            // Bottom seek bar & titles
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                // Subtitle String
                                if (subtitleText.isNotEmpty()) {
                                    Text(
                                        text = subtitleText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = CyberCyan,
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(formatTime(progress), color = Color.White, style = MaterialTheme.typography.bodySmall)
                                    Slider(
                                        value = progress.toFloat(),
                                        onValueChange = { viewModel.updateVideoProgress(it.toLong()) },
                                        valueRange = 0f..video.duration.toFloat().coerceAtLeast(1f),
                                        colors = SliderDefaults.colors(
                                            activeTrackColor = CyberCyan,
                                            thumbColor = CyberPink
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp)
                                    )
                                    Text(formatTime(video.duration), color = Color.White, style = MaterialTheme.typography.bodySmall)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { showSpeedDialog = true }) {
                                        Text("Speed: ${speed}x", color = CyberCyan)
                                    }

                                    Row {
                                        IconButton(onClick = {
                                            val currentIdx = videos.indexOfFirst { it.id == video.id }
                                            if (currentIdx > 0) {
                                                viewModel.playVideo(videos[currentIdx - 1])
                                            }
                                        }) {
                                            Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Color.White)
                                        }

                                        IconButton(onClick = { viewModel.toggleVideoPlayback() }) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                                contentDescription = null,
                                                tint = CyberPink,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }

                                        IconButton(onClick = {
                                            val currentIdx = videos.indexOfFirst { it.id == video.id }
                                            if (currentIdx != -1 && currentIdx < videos.size - 1) {
                                                viewModel.playVideo(videos[currentIdx + 1])
                                            }
                                        }) {
                                            Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White)
                                        }
                                    }

                                    IconButton(onClick = {
                                        // Auto-add dynamic video to vault
                                        viewModel.hideMediaToVault(video.path, "VIDEO")
                                    }) {
                                        Icon(Icons.Default.Lock, contentDescription = "Lock in Vault", tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }

                    // Prompt notifications
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(gesturePrompt, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Local Directory List of Videos
            Text(
                "Local Video Library",
                color = CyberCyan,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            if (videos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VideoCall,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No compatible offline movies found", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(videos) { video ->
                        val isCurrent = currentVideo?.id == video.id
                        ListItem(
                            headlineContent = { Text(video.title, color = if (isCurrent) CyberCyan else Color.White) },
                            supportingContent = { Text("${formatTime(video.duration)} • ${(video.size / (1024 * 1024))} MB", color = Color.Gray) },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp, 36.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isCurrent) CyberPink else Color(0x3300F0FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = if (isCurrent) Color.Black else CyberCyan
                                    )
                                }
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    // Extract sound to MP3 converter call
                                    viewModel.convertVideoToMP3(video.path, video.title)
                                }) {
                                    Icon(Icons.Default.MusicVideo, contentDescription = "Convert video to audio", tint = Color.Gray)
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Black),
                            modifier = Modifier.clickable {
                                viewModel.playVideo(video)
                            }
                        )
                        Divider(color = Color(0xFF141414))
                    }
                }
            }
        }
    }

    // Speed setting dialog
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Velocity Controls", color = CyberCyan) },
            text = {
                Column {
                    listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f, 3.0f, 4.0f).forEach { spd ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setVideoSpeed(spd)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${spd}x Velocity", color = if (speed == spd) CyberPink else Color.White)
                            if (speed == spd) Icon(Icons.Default.Check, contentDescription = null, tint = CyberPink)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("Dismiss", color = CyberCyan) }
            },
            containerColor = Color(0xFF111111)
        )
    }

    // Sleep dialog
    if (showSleepTimer) {
        AlertDialog(
            onDismissRequest = { showSleepTimer = false },
            title = { Text("Sleep Timer Control", color = CyberCyan) },
            text = {
                Column {
                    Text("Select time range", color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))
                    listOf(0, 15, 30, 60).forEach { mins ->
                        val label = if (mins == 0) "Dismiss Timer" else "$mins minutes"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSleepTimer(mins)
                                    showSleepTimer = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, color = if (sleepMinutes == mins) CyberPink else Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimer = false }) { Text("Cancel", color = CyberCyan) }
            },
            containerColor = Color(0xFF111111)
        )
    }
}
