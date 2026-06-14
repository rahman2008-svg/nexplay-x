package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberPink
import com.example.ui.viewmodel.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardComponent(
    viewModel: MediaViewModel,
    onNavigate: (String) -> Unit
) {
    val videos by viewModel.videos.collectAsState()
    val audios by viewModel.audios.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val isFloatingActive by viewModel.isFloatingVideoActive.collectAsState()

    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "NexPlay X", 
                            fontWeight = FontWeight.Bold, 
                            style = MaterialTheme.typography.titleLarge,
                            color = CyberCyan
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "PLAY EVERYTHING. OFFLINE.",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 2.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAboutDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "About Developer & Company", tint = CyberCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Tagline Banner: Sophisticated Dark Hero Section
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Play icon action circle
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .offset(x = 1.dp)
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CONTINUE PLAYING",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Oppenheimer.2023.MKV",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Storage progress stream segment
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.75f)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }

            // Quick Analytics Summary Card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MiniStatCard(
                        title = "Videos Available",
                        count = "${videos.size} items",
                        icon = Icons.Default.Movie,
                        color = CyberCyan,
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatCard(
                        title = "Voice Tracks",
                        count = "${audios.size} items",
                        icon = Icons.Default.MusicNote,
                        color = CyberPink,
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatCard(
                        title = "Photos & GIFs",
                        count = "${photos.size} cards",
                        icon = Icons.Default.Image,
                        color = Color.Yellow,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Floating Media Controls hub check
            if (isFloatingActive) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0C0C)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CyberPink),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Waves, contentDescription = null, tint = Color.Black)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Floating Media Player Mini Hub", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("Overlay panel actively monitoring queue streams", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            IconButton(onClick = { viewModel.toggleFloatingVideo(false) }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Primary Navigation Grid
            item {
                Text(
                    text = "Core Suite",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardNavButton(
                            title = "Advanced Video Player",
                            descr = "Hardware speed, custom overlays & gesture bounds",
                            icon = Icons.Default.Movie,
                            color = CyberCyan,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("video") }
                        )
                        DashboardNavButton(
                            title = "Advanced Music Player",
                            descr = "Lyrics, Equalizer presets & custom offline playlists",
                            icon = Icons.Default.MusicNote,
                            color = CyberPink,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("music") }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardNavButton(
                            title = "Smart Photo Gallery",
                            descr = "Photo viewer collections, slideshow loops & GIFs viewer",
                            icon = Icons.Default.Image,
                            color = Color.Yellow,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("gallery") }
                        )
                        DashboardNavButton(
                            title = "Secure Private Vault",
                            descr = "Encrypted local PIN system to shield voice or image logs",
                            icon = Icons.Default.Lock,
                            color = Color(0xFFA100FF),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("vault") }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DashboardNavButton(
                            title = "Advanced File Manager",
                            descr = "Analyze disk spaces, discard duplicates & empty cache",
                            icon = Icons.Default.FolderZip,
                            color = Color.LightGray,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("files") }
                        )
                        DashboardNavButton(
                            title = "Premium Tools Suite",
                            descr = "Mini video editor slicer, MP3 converters, Wi-Fi Share",
                            icon = Icons.Default.SettingsSuggest,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate("tools") }
                        )
                    }
                }
            }

            // Developer / Credits Panel
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0C0C)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAboutDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Engineering, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Independent Developer", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Prince AR Abdur Rahman • NexVora Lab's", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }

    // Comprehensive Dialog explaining Developer, Company, Products, and Technical CI/CD details
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Badge, contentDescription = null, tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Developer & Studio Info", color = Color.White)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("About Developer", color = CyberCyan, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Prince AR Abdur Rahman\nIndependent App Developer passionate about building modern Android applications, productivity tools, AI-powered experiences, media players, educational apps, and next-generation digital products.",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Contacts:\n• WhatsApp: 01707424006 / 01796951709\n• Facebook: facebook.com/share/1BNn32qoJo/\n• Instagram: instagram.com/ur___abdur____rahman__2008", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }

                    item {
                        Divider(color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("About Company", color = CyberPink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "NexVora Lab's Ofc\nFocuses on creating innovative Android applications designed to improve productivity, entertainment, learning, and digital experiences.",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("Mission: Build fast, beautiful, privacy-friendly, and user-focused applications accessible to everyone.", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }

                    item {
                        Divider(color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Company Products Portfolio", color = Color.Yellow, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        val products = listOf(
                            "NexPlay X (Active)", "LifeSphere OS", "Smart Day Planner X",
                            "Study AI", "Lensora Studio", "Offline AI", "NexVora Love Space",
                            "CalcVerse", "NexVoice OS"
                        )
                        Text(products.joinToString("  •  "), color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    }

                    item {
                        Divider(color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Technical Information", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Version: 1.0.0\n• CI/CD: GitHub Actions, Codemagic\n• Automatic APK builds & Release validation\n• License: © 2026 NexVora Lab's Ofc. All Rights Reserved.", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close Panel", color = CyberCyan)
                }
            },
            containerColor = Color(0xFF121212)
        )
    }
}

@Composable
fun MiniStatCard(
    title: String,
    count: String,
    icon: Any, // ImageVector
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF381E72)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon as androidx.compose.ui.graphics.vector.ImageVector, 
                    contentDescription = null, 
                    tint = CyberCyan, 
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Text(count, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun DashboardNavButton(
    title: String,
    descr: String,
    icon: Any,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF381E72)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(descr, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun CyberCyberGradientBrush() = listOf(CyberCyan, CyberPink)
