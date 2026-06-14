package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PhotoItem
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberPink
import com.example.ui.viewmodel.MediaViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GalleryComponent(
    viewModel: MediaViewModel,
    onBack: () -> Unit
) {
    val photos by viewModel.photos.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Photos, 1 = GIFs, 2 = Albums
    var activeViewerPhoto by remember { mutableStateOf<PhotoItem?>(null) }
    var slideShowActive by remember { mutableStateOf(false) }

    // Slideshow ticker
    LaunchedEffect(slideShowActive, activeViewerPhoto) {
        if (slideShowActive && activeViewerPhoto != null) {
            while (slideShowActive) {
                delay(3000)
                val currentIndex = photos.indexOfFirst { it.id == activeViewerPhoto?.id }
                if (currentIndex != -1 && currentIndex < photos.size - 1) {
                    activeViewerPhoto = photos[currentIndex + 1]
                } else {
                    activeViewerPhoto = photos.firstOrNull()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Smart Gallery", style = MaterialTheme.typography.titleMedium, color = CyberCyan) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (activeViewerPhoto != null) {
                        IconButton(onClick = { slideShowActive = !slideShowActive }) {
                            Icon(
                                imageVector = if (slideShowActive) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = "Slideshow",
                                tint = if (slideShowActive) CyberPink else Color.White
                            )
                        }
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
            // Main views controller (Top Tabs)
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = CyberCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = CyberCyan
                    )
                }
            ) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Photo, contentDescription = null, tint = if (activeTab == 0) CyberCyan else Color.Gray)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Photos", color = if (activeTab == 0) CyberCyan else Color.Gray)
                    }
                }
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Gif, contentDescription = null, tint = if (activeTab == 1) CyberCyan else Color.Gray)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GIFs", color = if (activeTab == 1) CyberCyan else Color.Gray)
                    }
                }
                Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Collections, contentDescription = null, tint = if (activeTab == 2) CyberCyan else Color.Gray)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Albums", color = if (activeTab == 2) CyberCyan else Color.Gray)
                    }
                }
            }

            // Grid layout display
            Box(modifier = Modifier.weight(1f)) {
                val filteredItems = remember(photos, activeTab) {
                    when (activeTab) {
                        0 -> photos.filter { !it.isGif }
                        1 -> photos.filter { it.isGif }
                        else -> photos // grouped in list below
                    }
                }

                if (activeTab == 2) {
                    // Group by folders/Albums list
                    val albumGroups = remember(photos) { photos.groupBy { it.folder } }
                    if (albumGroups.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No albums detected offline", color = Color.Gray)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(albumGroups.keys.toList()) { album ->
                                val list = albumGroups[album] ?: emptyList()
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activeTab = 0 // switch back, view items filtered?
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF2C2C2C)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Collections, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(36.dp))
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(album, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("${list.size} Items", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Photos / GIFs Grid List
                    if (filteredItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.Photo,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.DarkGray
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No offline media items", color = Color.Gray)
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredItems) { photo ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1C1C1F))
                                        .combinedClickable(
                                            onClick = { activeViewerPhoto = photo },
                                            onLongClick = {
                                                // Lock photo to Private Vault on hold!
                                                viewModel.hideMediaToVault(photo.path, "PHOTO")
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Simulated Photo visual preview (Custom dynamic card since images are synthesized)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (photo.isGif) Icons.Default.Gif else Icons.Default.Image,
                                            contentDescription = null,
                                            tint = if (photo.isGif) CyberPink else CyberCyan,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = photo.name,
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Fullscreen Photo Viewer and Slideshow Layer ---
    activeViewerPhoto?.let { photo ->
        val isFav = favorites.any { it.mediaPath == photo.path }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top control row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        activeViewerPhoto = null
                        slideShowActive = false
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White)
                    }

                    Text("Immersive Slide Viewer", color = CyberCyan)

                    IconButton(onClick = { viewModel.toggleFavorite(photo.path, "PHOTO", isFav) }) {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Fav",
                            tint = if (isFav) CyberPink else Color.White
                        )
                    }
                }

                // Mid viewer pane
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (photo.isGif) Icons.Default.Gif else Icons.Default.Photo,
                            contentDescription = null,
                            tint = if (photo.isGif) CyberPink else CyberCyan,
                            modifier = Modifier.size(100.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(photo.name, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("${(photo.size / 1024)} KB • ${photo.folder}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Bottom actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            viewModel.hideMediaToVault(photo.path, "PHOTO")
                            activeViewerPhoto = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPink, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send to Private Vault")
                    }
                }
            }
        }
    }
}
