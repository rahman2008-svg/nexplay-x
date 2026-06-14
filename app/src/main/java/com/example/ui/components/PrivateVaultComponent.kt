package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.database.VaultItem
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberPink
import com.example.ui.viewmodel.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateVaultComponent(
    viewModel: MediaViewModel,
    onBack: () -> Unit
) {
    val vaultItems by viewModel.vaultItems.collectAsState()
    val isUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val savedPIN by viewModel.savedPIN.collectAsState()

    var activeVaultTab by remember { mutableStateOf(0) } // 0 = Images, 1 = Videos, 2 = Audios
    var enteredKeys by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var secureNotice by remember { mutableStateOf<String?>(null) }

    // State reset when back is pressed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.lockVault()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("🔒 NexPlay Security Vault", style = MaterialTheme.typography.titleMedium, color = CyberCyan) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (isUnlocked) {
                        IconButton(onClick = { viewModel.lockVault() }) {
                            Icon(Icons.Default.LockReset, contentDescription = "Lock", tint = CyberPink)
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
            // Toast / Notice Bar
            AnimatedVisibility(visible = secureNotice != null, enter = expandVertically(), exit = shrinkVertically()) {
                secureNotice?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberCyan)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(msg, color = Color.Black, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        LaunchedEffect(msg) {
                            kotlinx.coroutines.delay(3000)
                            secureNotice = null
                        }
                    }
                }
            }

            if (savedPIN == null) {
                // Pin Setup flow
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Setup Secure PIN Lock", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text("Choose a 4-digit numeric code to hide device media folders locally:", color = Color.Gray, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        enteredKeys.padEnd(4, '—'),
                        color = CyberPink,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    NumericKeypad(
                        onDigitClick = { digit ->
                            if (enteredKeys.length < 4) {
                                enteredKeys += digit
                                if (enteredKeys.length == 4) {
                                    viewModel.setupPIN(enteredKeys)
                                    enteredKeys = ""
                                    secureNotice = "PIN Lock configured! Vault is ready."
                                }
                            }
                        },
                        onBackspaceClick = {
                            if (enteredKeys.isNotEmpty()) enteredKeys = enteredKeys.dropLast(1)
                        }
                    )
                }
            } else if (!isUnlocked) {
                // Enter PIN code screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = CyberPink, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Private Storage Encrypted", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text("Enter secure PIN to view hidden assets:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMessage!!, color = Color.Red, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        enteredKeys.replace(Regex("."), "●").padEnd(4, '—'),
                        color = CyberCyan,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    NumericKeypad(
                        onDigitClick = { digit ->
                            if (enteredKeys.length < 4) {
                                enteredKeys += digit
                                errorMessage = null
                                if (enteredKeys.length == 4) {
                                    val checked = viewModel.unlockVault(enteredKeys)
                                    enteredKeys = ""
                                    if (!checked) {
                                        errorMessage = "🚫 Access Denied! Invalid credentials."
                                    } else {
                                        secureNotice = "Vault decrypted. File structures unlocked."
                                    }
                                }
                            }
                        },
                        onBackspaceClick = {
                            if (enteredKeys.isNotEmpty()) enteredKeys = enteredKeys.dropLast(1)
                        }
                    )
                }
            } else {
                // Vault Unlocked Panel
                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(
                        selectedTabIndex = activeVaultTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = CyberCyan,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[activeVaultTab]),
                                color = CyberCyan
                            )
                        }
                    ) {
                        listOf("Photos", "Videos", "Audios").forEachIndexed { index, title ->
                            Tab(selected = activeVaultTab == index, onClick = { activeVaultTab = index }) {
                                Text(title, modifier = Modifier.padding(14.dp), color = if (activeVaultTab == index) CyberCyan else Color.Gray)
                            }
                        }
                    }

                    val filteredVault = remember(vaultItems, activeVaultTab) {
                        when (activeVaultTab) {
                            0 -> vaultItems.filter { it.mediaType == "PHOTO" }
                            1 -> vaultItems.filter { it.mediaType == "VIDEO" }
                            else -> vaultItems.filter { it.mediaType == "AUDIO" }
                        }
                    }

                    if (filteredVault.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.DarkGray
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("This folder is completely empty.", color = Color.Gray)
                                Text("Long-click files inside file manager or gallery to lock them in here!", color = Color.DarkGray, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(filteredVault) { item ->
                                ListItem(
                                    headlineContent = { Text(item.fileName, color = Color.White) },
                                    supportingContent = { Text("Size: ${item.originalSize / 1024} KB", color = Color.Gray) },
                                    leadingContent = {
                                        Icon(
                                            imageVector = when (item.mediaType) {
                                                "PHOTO" -> Icons.Default.Image
                                                "VIDEO" -> Icons.Default.Movie
                                                else -> Icons.Default.MusicNote
                                            },
                                            contentDescription = null,
                                            tint = CyberCyan
                                        )
                                    },
                                    trailingContent = {
                                        Button(
                                            onClick = {
                                                viewModel.unhideMediaFromVault(item)
                                                secureNotice = "Restored ${item.fileName} into general storage!"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberPink, contentColor = Color.Black)
                                        ) {
                                            Text("Restore")
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
        }
    }
}

@Composable
fun NumericKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit
) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "◀")
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (i in 0..3) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                for (j in 0..2) {
                    val key = keys[i * 3 + j]
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F0F14))
                            .clickable {
                                when (key) {
                                    "C" -> { /* do nothing / can act as clean if needed */ }
                                    "◀" -> onBackspaceClick()
                                    else -> onDigitClick(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            color = if (key == "◀" || key == "C") CyberPink else Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
