package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.AudioItem
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberPink
import com.example.ui.viewmodel.MediaViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerComponent(
    viewModel: MediaViewModel,
    onBack: () -> Unit
) {
    val currentAudio by viewModel.currentAudio.collectAsState()
    val isPlaying by viewModel.isAudioPlaying.collectAsState()
    val speed by viewModel.audioPlaybackSpeed.collectAsState()
    val progress by viewModel.audioProgress.collectAsState()
    val audios by viewModel.audios.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val selectedEq by viewModel.selectedEqualizer.collectAsState()
    val sleepMinutes by viewModel.sleepTimerMinutes.collectAsState()
    val lyricsText by viewModel.lyrics.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var showEqDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showPlaylistDialogByTrack by remember { mutableStateOf<AudioItem?>(null) }
    var tabSelected by remember { mutableStateOf(0) } // 0 = Tracks, 1 = Folders, 2 = Playlists

    // Local ticker for progress simulation while playing!
    LaunchedEffect(isPlaying, currentAudio, speed) {
        if (isPlaying && currentAudio != null) {
            while (isPlaying) {
                delay((1000 / speed).toLong())
                val currentTrack = currentAudio
                if (currentTrack != null) {
                    val nextProgress = progress + 1000L
                    if (nextProgress >= currentTrack.duration) {
                        viewModel.updateAudioProgress(0L)
                        // Trigger next track if available
                        val currentIndex = audios.indexOfFirst { it.id == currentTrack.id }
                        if (currentIndex != -1 && currentIndex < audios.size - 1) {
                            viewModel.playAudio(audios[currentIndex + 1])
                        } else {
                            viewModel.toggleAudioPlayback()
                        }
                    } else {
                        viewModel.updateAudioProgress(nextProgress)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NexPlay Audio Engine", style = MaterialTheme.typography.titleMedium, color = CyberCyan) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showSleepTimerDialog = true }) {
                        Icon(
                            Icons.Default.Timer, 
                            contentDescription = "Sleep Timer", 
                            tint = if (sleepMinutes > 0) CyberPink else Color.White
                        )
                    }
                    IconButton(onClick = { showEqDialog = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Equalizer", tint = CyberCyan)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Player Visual Section (Active Song)
            AnimatedVisibility(
                visible = currentAudio != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                currentAudio?.let { audio ->
                    val isFav = favorites.any { it.mediaPath == audio.path }

                    // Album rotation effect
                    val infiniteTransition = rememberInfiniteTransition()
                    val rotationAngle by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(12000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF11001C), Color.Black)
                                )
                            )
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = audio.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = audio.artist + " • " + audio.album,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = {
                                viewModel.toggleFavorite(audio.path, "AUDIO", isFav)
                            }) {
                                Icon(
                                    imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFav) CyberPink else Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Visual Disc Vinyl
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF141414))
                                .rotate(if (isPlaying) rotationAngle else 0f),
                            contentAlignment = Alignment.Center
                        ) {
                            // Track grooves
                            Box(modifier = Modifier.size(145.dp).clip(CircleShape).background(Color.Black))
                            Box(modifier = Modifier.size(110.dp).clip(CircleShape).background(Color(0xFF1A1A1A)))
                            
                            // Center sticker
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            colors = listOf(CyberCyan, CyberPink, CyberCyan)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Black, modifier = Modifier.size(28.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Lyrics Tumble preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .background(Color(0xFF0F0014), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val activeLyricLine = remember(progress, lyricsText) {
                                val lines = lyricsText.split("\n")
                                val currentSec = progress / 1000
                                var pickedLine = "NexPlay Offline Audio Stream..."
                                for (line in lines) {
                                    if (line.startsWith("[")) {
                                        val minStr = line.substring(1, 3)
                                        val secStr = line.substring(4, 6)
                                        val totalSeconds = minStr.toInt() * 60 + secStr.toInt()
                                        if (currentSec >= totalSeconds) {
                                            pickedLine = line.substring(8)
                                        }
                                    }
                                }
                                pickedLine
                            }
                            Text(activeLyricLine, color = CyberCyan, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Seek Bar Details
                        Slider(
                            value = progress.toFloat(),
                            onValueChange = { viewModel.updateAudioProgress(it.toLong()) },
                            valueRange = 0f..audio.duration.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                activeTrackColor = CyberCyan,
                                inactiveTrackColor = Color.DarkGray,
                                thumbColor = CyberPink
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatTime(progress), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            Text(formatTime(audio.duration), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                val newSpeed = when (speed) {
                                    1.0f -> 1.5f
                                    1.5f -> 2.0f
                                    2.0f -> 0.5f
                                    0.5f -> 1.0f
                                    else -> 1.0f
                                }
                                viewModel.setAudioSpeed(newSpeed)
                            }) {
                                Text("${speed}x", color = CyberCyan, style = MaterialTheme.typography.bodyMedium)
                            }

                            IconButton(onClick = {
                                val currentIndex = audios.indexOfFirst { it.id == audio.id }
                                if (currentIndex > 0) {
                                    viewModel.playAudio(audios[currentIndex - 1])
                                }
                            }) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(36.dp))
                            }

                            FloatingActionButton(
                                onClick = { viewModel.toggleAudioPlayback() },
                                containerColor = CyberPink,
                                contentColor = Color.Black,
                                shape = CircleShape,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            IconButton(onClick = {
                                val currentIndex = audios.indexOfFirst { it.id == audio.id }
                                if (currentIndex != -1 && currentIndex < audios.size - 1) {
                                    viewModel.playAudio(audios[currentIndex + 1])
                                }
                            }) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                            }

                            IconButton(onClick = { showPlaylistDialogByTrack = audio }) {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to Playlist", tint = CyberCyan)
                            }
                        }
                    }
                }
            }

            // Tab Selector (Tracks, Folders, Playlists)
            TabRow(
                selectedTabIndex = tabSelected,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = CyberCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[tabSelected]),
                        color = CyberCyan
                    )
                }
            ) {
                Tab(selected = tabSelected == 0, onClick = { tabSelected = 0 }) {
                    Text("Tracks", modifier = Modifier.padding(14.dp), color = if (tabSelected == 0) CyberCyan else Color.Gray)
                }
                Tab(selected = tabSelected == 1, onClick = { tabSelected = 1 }) {
                    Text("Folders", modifier = Modifier.padding(14.dp), color = if (tabSelected == 1) CyberCyan else Color.Gray)
                }
                Tab(selected = tabSelected == 2, onClick = { tabSelected = 2 }) {
                    Text("Playlists", modifier = Modifier.padding(14.dp), color = if (tabSelected == 2) CyberCyan else Color.Gray)
                }
            }

            // Dynamic Content
            Box(modifier = Modifier.weight(1f)) {
                when (tabSelected) {
                    0 -> {
                        // Tracks sorted/favorites
                        if (audios.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.MusicOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color.DarkGray
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("No local voice tracks found", color = Color.Gray)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(audios) { audio ->
                                    val isCurrent = currentAudio?.id == audio.id
                                    ListItem(
                                        headlineContent = { Text(audio.title, color = if (isCurrent) CyberCyan else Color.White) },
                                        supportingContent = { Text("${audio.artist} • ${audio.album}", color = Color.Gray) },
                                        leadingContent = {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isCurrent) CyberPink else Color(0xFF1E1E1E)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    if (isCurrent && isPlaying) Icons.Default.VolumeUp else Icons.Default.MusicNote,
                                                    contentDescription = null,
                                                    tint = if (isCurrent) Color.Black else Color.Gray
                                                )
                                            }
                                        },
                                        trailingContent = {
                                            Row {
                                                IconButton(onClick = { showPlaylistDialogByTrack = audio }) {
                                                    Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = Color.Gray)
                                                }
                                                IconButton(onClick = { viewModel.toggleFavorite(audio.path, "AUDIO", favorites.any { it.mediaPath == audio.path }) }) {
                                                    Icon(
                                                        imageVector = if (favorites.any { it.mediaPath == audio.path }) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                        contentDescription = null,
                                                        tint = if (favorites.any { it.mediaPath == audio.path }) CyberPink else Color.Gray
                                                    )
                                                }
                                            }
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Black),
                                        modifier = Modifier
                                            .combinedClickable(
                                                onClick = { viewModel.playAudio(audio) },
                                                onLongClick = {
                                                    // Trigger move file to Private Vault on hold!
                                                    viewModel.hideMediaToVault(audio.path, "AUDIO")
                                                }
                                            )
                                    )
                                    Divider(color = Color(0xFF121212))
                                }
                            }
                        }
                    }
                    1 -> {
                        val folders = remember(audios) {
                            audios.groupBy { it.folder }
                        }
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(folders.keys.toList()) { folder ->
                                val tracks = folders[folder] ?: emptyList()
                                ListItem(
                                    headlineContent = { Text(folder, color = Color.White) },
                                    supportingContent = { Text("${tracks.size} tracks", color = Color.Gray) },
                                    leadingContent = {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = CyberCyan)
                                    },
                                    trailingContent = {
                                        IconButton(onClick = {
                                            if (tracks.isNotEmpty()) {
                                                viewModel.playAudio(tracks[0])
                                            }
                                        }) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                        }
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Black),
                                    modifier = Modifier.clickable {
                                        // Filter UI or show directory list
                                        if (tracks.isNotEmpty()) {
                                            viewModel.playAudio(tracks[0])
                                        }
                                    }
                                )
                                Divider(color = Color(0xFF121212))
                            }
                        }
                    }
                    2 -> {
                        // Playlists List
                        Column(modifier = Modifier.fillMaxSize()) {
                            var newPlaylistName by remember { mutableStateOf("") }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newPlaylistName,
                                    onValueChange = { newPlaylistName = it },
                                    label = { Text("New Playlist", color = Color.Gray) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = Color.DarkGray
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (newPlaylistName.isNotBlank()) {
                                            viewModel.createAndAddPlaylist(newPlaylistName, null)
                                            newPlaylistName = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black)
                                ) {
                                    Text("Create")
                                }
                            }

                            if (playlists.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No custom playlists created yet", color = Color.Gray)
                                }
                            } else {
                                LazyColumn {
                                    items(playlists) { playlist ->
                                        ListItem(
                                            headlineContent = { Text(playlist.name, color = Color.White) },
                                            supportingContent = { Text("Custom Playlist Collection", color = Color.Gray) },
                                            leadingContent = {
                                                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = CyberPink)
                                            },
                                            trailingContent = {
                                                IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Gray)
                                                }
                                            },
                                            colors = ListItemDefaults.colors(containerColor = Color.Black),
                                            modifier = Modifier.clickable {
                                                // play dynamic tracks in playlist when clicked!
                                            }
                                        )
                                        Divider(color = Color(0xFF121212))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Equalizer Presets Modal Dialog ---
    if (showEqDialog) {
        AlertDialog(
            onDismissRequest = { showEqDialog = false },
            title = { Text("Audio Equalizer Presets", color = CyberCyan) },
            text = {
                Column {
                    Text("Select a sound frequency profile to boost hardware drivers:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    viewModel.equalizerPresets.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.changeEqualizer(preset)
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(preset, color = if (selectedEq == preset) CyberPink else Color.White)
                            if (selectedEq == preset) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = CyberPink)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEqDialog = false }) {
                    Text("Close", color = CyberCyan)
                }
            },
            containerColor = Color(0xFF121212)
        )
    }

    // --- Sleep Timer Modal Dialog ---
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Configure Sleep Timer", color = CyberCyan) },
            text = {
                Column {
                    Text("Automatically quiet playbacks when time is reached:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    listOf(0, 5, 15, 30, 45, 60).forEach { mins ->
                        val label = if (mins == 0) "Turn Off" else "$mins Minutes"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSleepTimer(mins)
                                    showSleepTimerDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, color = if (sleepMinutes == mins) CyberPink else Color.White)
                            if (sleepMinutes == mins) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = CyberPink)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Cancel", color = CyberCyan)
                }
            },
            containerColor = Color(0xFF121212)
        )
    }

    // --- Choose Playlist Dialog ---
    showPlaylistDialogByTrack?.let { track ->
        AlertDialog(
            onDismissRequest = { showPlaylistDialogByTrack = null },
            title = { Text("Add Track to Playlist", color = CyberCyan) },
            text = {
                Column {
                    if (playlists.isEmpty()) {
                        Text("No playlists created yet. Create one above first!", color = Color.LightGray)
                    } else {
                        Text("Select targets:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(playlists) { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addTrackToPlaylist(playlist.id, track)
                                            showPlaylistDialogByTrack = null
                                        }
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(playlist.name, color = Color.White)
                                    Icon(Icons.Default.Add, contentDescription = null, tint = CyberCyan)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistDialogByTrack = null }) {
                    Text("Close", color = CyberPink)
                }
            },
            containerColor = Color(0xFF121212)
        )
    }
}

fun formatTime(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / (1000 * 60)) % 60
    return String.format("%02d:%02d", min, sec)
}
