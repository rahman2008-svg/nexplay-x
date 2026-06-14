package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.CustomPresetEntity
import com.example.data.database.ProjectEntity
import com.example.ui.theme.*
import com.example.utils.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LensoraApp(viewModel: LensoraViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Navigation Screen track
    var currentScreen by remember { mutableStateOf("splash") }

    val errorMsg by viewModel.errorAlert.collectAsState()
    val activePhotoState by viewModel.photoState.collectAsState()
    val isCanvaMode by viewModel.isCanvaMode.collectAsState()

    // Pick image contract
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadBitmapFromUri(uri, context)
            currentScreen = "editor"
        }
    }

    // Capture lifecycle times
    DisposableEffect(currentScreen) {
        viewModel.flushTimeStats()
        onDispose {
            viewModel.flushTimeStats()
        }
    }

    // Simple error alert banner
    errorMsg?.let {
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("Performance Notice", color = Color.White) },
            text = { Text(it, color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text("OK", color = NeonCyan)
                }
            },
            containerColor = CyberSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentScreen != "splash" && currentScreen != "editor") {
                NavigationBar(
                    containerColor = CyberSurface,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = currentScreen == "home",
                        onClick = { currentScreen = "home" },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonPink,
                            selectedTextColor = NeonPink,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = CyberSurfaceElevated
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen == "templates",
                        onClick = { currentScreen = "templates" },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Templates") },
                        label = { Text("Canva Lite") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonPink,
                            selectedTextColor = NeonPink,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = CyberSurfaceElevated
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen == "presets",
                        onClick = { currentScreen = "presets" },
                        icon = { Icon(Icons.Default.FilterList, contentDescription = "Presets") },
                        label = { Text("Presets") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonPink,
                            selectedTextColor = NeonPink,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = CyberSurfaceElevated
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen == "projects",
                        onClick = { currentScreen = "projects" },
                        icon = { Icon(Icons.Default.Folder, contentDescription = "Projects") },
                        label = { Text("Drafts") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonPink,
                            selectedTextColor = NeonPink,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = CyberSurfaceElevated
                        )
                    )
                    NavigationBarItem(
                        selected = currentScreen == "settings",
                        onClick = { currentScreen = "settings" },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Info & Stats") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonPink,
                            selectedTextColor = NeonPink,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = CyberSurfaceElevated
                        )
                    )
                }
            }
        },
        containerColor = CyberDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = spring()) with fadeOut(animationSpec = spring())
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    "splash" -> SplashScreen(
                        onGetStarted = { currentScreen = "home" }
                    )
                    "home" -> HomeScreen(
                        viewModel = viewModel,
                        onOpenSelector = { imagePicker.launch("image/*") },
                        onNavigateToEditor = { currentScreen = "editor" },
                        onSelectTemplate = { currentScreen = "templates" }
                    )
                    "editor" -> EditorScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = "home" }
                    )
                    "templates" -> TemplatesScreen(
                        viewModel = viewModel,
                        onSelect = { currentScreen = "editor" }
                    )
                    "presets" -> PresetsScreen(
                        viewModel = viewModel,
                        onSelectPreset = { currentScreen = "editor" }
                    )
                    "projects" -> ProjectsScreen(
                        viewModel = viewModel,
                        onRestored = { currentScreen = "editor" }
                    )
                    "settings" -> SettingsScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

// 1. SPLASH SCREEN
@Composable
fun SplashScreen(onGetStarted: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CyberDark, CyberSurface)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Lensora branding logo box
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NeonPink, Color.Transparent)
                        )
                    )
                    .border(2.dp, CardBorder, RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = "Logo",
                    tint = NeonCyan,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Lensora Studio X",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Edit Photos, Create Designs, Enhance Images — All Offline.",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Premium Glassmorphism Developer Bio card
            Card(
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NexVora Lab's Ofc",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Developed by Prince AR Abdur Rahman",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "© 2026 • Privacy-First, Zero Cloud.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onGetStarted,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(50.dp)
                    .testTag("get_started_button"),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text(
                    text = "START DESIGNING",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// 2. DASHBOARD / HOME SCREEN
@Composable
fun HomeScreen(
    viewModel: LensoraViewModel,
    onOpenSelector: () -> Unit,
    onNavigateToEditor: () -> Unit,
    onSelectTemplate: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Lensora Studio X",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "OFFLINE PRO ENGINE",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CyberSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.OfflineBolt,
                        contentDescription = "Offline indicator",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CyberSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Brush.sweepGradient(listOf(Color(0xFF60A5FA), Color(0xFF4F46E5), Color(0xFF60A5FA))))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Clean Minimalism Promo Aspect Grid Layout matching theme HTML
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Photo Editor - Full Width Broad Promo Card (16:7 Aspect Ratio)
            Card(
                onClick = onOpenSelector,
                colors = CardDefaults.cardColors(containerColor = NeonPink),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 7f)
                    .testTag("launch_photo_editor_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "Photo Editor",
                            color = BrightGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.2).sp
                        )
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = BrightGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "Curves, HSL, and Pro Adjustments",
                        color = BrightGold.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // Second Row: Side-by-Side Square Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Canva Lite Card
                Card(
                    onClick = {
                        viewModel.initEmptyCanva()
                        onNavigateToEditor()
                    },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF334963)),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .testTag("launch_canva_designer_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(NeonPink),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = null,
                                tint = BrightGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Canva Lite",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Sandbox Studio Card
                Card(
                    onClick = onNavigateToEditor,
                    colors = CardDefaults.cardColors(containerColor = CyberSurfaceElevated),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .border(1.dp, CardBorder, RoundedCornerShape(28.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Sandbox Studio",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Analytics metrics (100% offline tracking, zero firebase)
        Text(
            text = "Offline Stats & Usage",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(GlassCardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text("Designs", color = TextSecondary, fontSize = 11.sp)
                    Text("${stats.totalProjects}", color = NeonPink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(GlassCardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text("Exports", color = TextSecondary, fontSize = 11.sp)
                    Text("${stats.totalExports}", color = NeonCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .background(GlassCardBg, RoundedCornerShape(14.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text("Popular Choice", color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                    Text(stats.mostUsedFilter, color = BrightGold, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// 3. EDITOR SCREEN (The powerful core)
@Composable
fun EditorScreen(
    viewModel: LensoraViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isCanvaMode by viewModel.isCanvaMode.collectAsState()
    val editedBitmap by viewModel.editedBitmap.collectAsState()
    val photoState by viewModel.photoState.collectAsState()

    val canvaElements by viewModel.canvaElements.collectAsState()
    val selectedElementId by viewModel.selectedElementId.collectAsState()
    val canvaBackgroundHex by viewModel.canvaBackgroundHex.collectAsState()

    var activeTab by remember { mutableStateOf("adjust") } // adjust, filters, face, export, canva_layers

    // Export formats state
    var exportFormat by remember { mutableStateOf("PNG") }
    var exportQuality by remember { mutableStateOf("HIGH") }

    // Custom draft save prompts
    var showDraftDialog by remember { mutableStateOf(false) }
    var draftNameInput by remember { mutableStateOf("Draft ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}") }

    // Preset save prompt
    var showPresetDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("My Preset ${System.currentTimeMillis() % 1000}") }

    // Text editor model details
    var showTextAddDialog by remember { mutableStateOf(false) }
    var canvaTextInput by remember { mutableStateOf("Type something") }

    if (showDraftDialog) {
        AlertDialog(
            onDismissRequest = { showDraftDialog = false },
            title = { Text("Save Project Draft", color = Color.White) },
            text = {
                Column {
                    Text("Save this design matrix inside Room storage to restore anytime offline.", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = draftNameInput,
                        onValueChange = { draftNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan
                        ),
                        singleLine = true,
                        label = { Text("Draft Name", color = NeonCyan) }
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                    onClick = {
                        viewModel.saveProjectDraft(draftNameInput)
                        showDraftDialog = false
                        Toast.makeText(context, "Draft Saved Offline!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDraftDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberSurfaceElevated
        )
    }

    if (showPresetDialog) {
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = { Text("Save Custom Preset", color = Color.White) },
            text = {
                Column {
                    Text("Compile these active color parameters as a custom Lightroom preset.", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = presetNameInput,
                        onValueChange = { presetNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan
                        ),
                        singleLine = true,
                        label = { Text("Preset Name", color = NeonCyan) }
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                    onClick = {
                        viewModel.saveCustomPreset(presetNameInput)
                        showPresetDialog = false
                        Toast.makeText(context, "Preset Template Saved!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPresetDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberSurfaceElevated
        )
    }

    if (showTextAddDialog) {
        AlertDialog(
            onDismissRequest = { showTextAddDialog = false },
            title = { Text("Add Canva Text", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = canvaTextInput,
                    onValueChange = { canvaTextInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonCyan
                    )
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                    onClick = {
                        viewModel.addTextCanvaElement(canvaTextInput)
                        showTextAddDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextAddDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberSurfaceElevated
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // App bar top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberSurface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = if (isCanvaMode) "Canva Designer" else "Lensora Editor",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isCanvaMode) {
                    IconButton(onClick = { viewModel.triggerAutoEnhance() }) {
                        Icon(Icons.Default.Star, contentDescription = "Auto Enhance", tint = BrightGold)
                    }
                }
                Button(
                    onClick = { showDraftDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save", fontSize = 12.sp, color = Color.White)
                }
            }
        }

        // Main preview container viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(CyberDark)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isCanvaMode) {
                // CANVA LITE WORKSPACE (fully draggable overlay container)
                var currentBgColor = Color(android.graphics.Color.parseColor(canvaBackgroundHex))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(currentBgColor, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                viewModel.updateSelectedElementPosition(dragAmount.x, dragAmount.y)
                            }
                        }
                        .testTag("canva_lite_workspace")
                ) {
                    canvaElements.forEach { element ->
                        val isSelected = element.id == selectedElementId
                        Box(
                            modifier = Modifier
                                .offset(x = element.x.dp, y = element.y.dp)
                                .clickable { viewModel.selectCanvaElement(element.id) }
                                .padding(8.dp)
                                .border(
                                    if (isSelected) 1.5.dp else 0.dp,
                                    if (isSelected) NeonCyan else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                        ) {
                            when (element) {
                                is CanvaElement.Text -> {
                                    Text(
                                        text = element.text,
                                        color = Color(android.graphics.Color.parseColor(element.colorHex)),
                                        fontSize = (element.fontSizeSp * element.scale).sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = when (element.fontFamily) {
                                            "Space Grotesk" -> FontFamily.SansSerif
                                            "JetBrains Mono" -> FontFamily.Monospace
                                            else -> FontFamily.Default
                                        }
                                    )
                                }
                                is CanvaElement.Sticker -> {
                                    Text(
                                        text = element.emoji,
                                        fontSize = (38 * element.scale).sp
                                    )
                                }
                                is CanvaElement.Shape -> {
                                    val shapeColor = Color(android.graphics.Color.parseColor(element.colorHex))
                                    Box(
                                        modifier = Modifier
                                            .size((element.width * element.scale).dp, (element.height * element.scale).dp)
                                            .background(shapeColor, RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // PHOTO PREVIEW (Lightroom / Snapseed Render outcome)
                editedBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Editing preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // Contextual bottom configuration dock
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberSurface)
                .padding(14.dp)
        ) {
            Column {
                if (isCanvaMode) {
                    // Canva control modifiers
                    Text(
                        text = "Canva Control Desk",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showTextAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Text", fontSize = 11.sp, color = Color.White)
                        }
                        Button(
                            onClick = { viewModel.addStickerCanvaElement(CanvaTemplates.predefinedEmojis.random()) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.EmojiEmotions, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Sticker", fontSize = 11.sp, color = Color.White)
                        }
                        Button(
                            onClick = { viewModel.addShapeCanvaElement("Card", CanvaTemplates.predefinedColors.random()) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Shape", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    if (selectedElementId != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Active Layer Select", color = NeonCyan, fontSize = 11.sp)
                            IconButton(onClick = { viewModel.deleteSelectedElement() }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete layer", tint = Color.Red, modifier = Modifier.size(18.dp))
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Scale", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(45.dp))
                            Slider(
                                value = 1f,
                                onValueChange = { viewModel.updateSelectedElementScale(it) },
                                valueRange = 0.8f..1.2f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = NeonPink, activeTrackColor = NeonPink)
                            )
                        }
                    }
                } else {
                    // Snapseed/Lightroom dynamic slider block matching active tab
                    when (activeTab) {
                        "adjust" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                MatrixSlider(
                                    label = "Brightness",
                                    value = photoState.brightness,
                                    valueRange = -100f..100f,
                                    onValueChange = { value -> viewModel.updatePhotoState { it.copy(brightness = value) } }
                                )
                                MatrixSlider(
                                    label = "Contrast",
                                    value = photoState.contrast,
                                    valueRange = -100f..100f,
                                    onValueChange = { value -> viewModel.updatePhotoState { it.copy(contrast = value) } }
                                )
                                MatrixSlider(
                                    label = "Exposure",
                                    value = photoState.exposure,
                                    valueRange = -100f..100f,
                                    onValueChange = { value -> viewModel.updatePhotoState { it.copy(exposure = value) } }
                                )
                                MatrixSlider(
                                    label = "Saturation",
                                    value = photoState.saturation,
                                    valueRange = -100f..100f,
                                    onValueChange = { value -> viewModel.updatePhotoState { it.copy(saturation = value) } }
                                )
                            }
                        }
                        "filters" -> {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Pre-compiled Presets", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    TextButton(onClick = { showPresetDialog = true }) {
                                        Text("+ Create Custom", color = NeonCyan, fontSize = 11.sp)
                                    }
                                }
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val presetChoices = listOf("Normal", "Cinematic", "Moody", "Vintage", "B&W", "Portrait", "Summer", "Night")
                                    items(presetChoices) { choice ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (photoState.chosenFilter == choice) NeonPink.copy(0.3f) else CyberSurfaceElevated)
                                                .clickable { viewModel.selectPreset(choice) }
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Text(choice, color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                        "face" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Offline Face Beautify (Heuristics)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                MatrixSlider(
                                    label = "Skin Smooth",
                                    value = photoState.faceBeautifySmooth,
                                    valueRange = 0f..100f,
                                    onValueChange = { value -> viewModel.updatePhotoState { it.copy(faceBeautifySmooth = value) } }
                                )
                                MatrixSlider(
                                    label = "Teeth Whitening",
                                    value = photoState.faceTeethWhite,
                                    valueRange = 0f..100f,
                                    onValueChange = { value -> viewModel.updatePhotoState { it.copy(faceTeethWhite = value) } }
                                )
                                MatrixSlider(
                                    label = "Eye Brightening",
                                    value = photoState.faceEyeBright,
                                    valueRange = 0f..100f,
                                    onValueChange = { value -> viewModel.updatePhotoState { it.copy(faceEyeBright = value) } }
                                )
                                MatrixSlider(
                                    label = "Portrait Radial Blur",
                                    value = photoState.portraitBlur,
                                    valueRange = 0f..100f,
                                    onValueChange = { value -> viewModel.updatePhotoState { it.copy(portraitBlur = value) } }
                                )
                            }
                        }
                        "export" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("High Definition Export Engine", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Format select
                                    listOf("PNG", "JPG", "WEBP", "PDF").forEach { fmt ->
                                        Button(
                                            onClick = { exportFormat = fmt },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (exportFormat == fmt) NeonCyan else CyberSurfaceElevated
                                            )
                                        ) {
                                            Text(fmt, color = if (exportFormat == fmt) Color.Black else Color.White, fontSize = 10.sp)
                                        }
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Quality select
                                    listOf("MEDIUM", "HIGH", "ULTRA HD").forEach { qlt ->
                                        Button(
                                            onClick = { exportQuality = qlt },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (exportQuality == qlt) NeonPink else CyberSurfaceElevated
                                            )
                                        ) {
                                            Text(qlt, color = Color.White, fontSize = 9.sp)
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val out = viewModel.triggerExportAction(exportFormat, exportQuality)
                                            if (out != null) {
                                                Toast.makeText(context, "Exported successfully offline! Size: ${out.size / 1024} KB", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "An error occurred during build conversion.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("RENDER & SAVE FILE", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Tab triggers (Adjust, Filters, Face details, HD Export)
                if (!isCanvaMode) {
                    TabRow(
                        selectedTabIndex = when (activeTab) {
                            "adjust" -> 0
                            "filters" -> 1
                            "face" -> 2
                            else -> 3
                        },
                        containerColor = CyberSurface,
                        contentColor = NeonPink
                    ) {
                        Tab(selected = activeTab == "adjust", onClick = { activeTab = "adjust" }) {
                            Text("Tune", modifier = Modifier.padding(12.dp), color = if (activeTab == "adjust") NeonPink else Color.White, fontSize = 13.sp)
                        }
                        Tab(selected = activeTab == "filters", onClick = { activeTab = "filters" }) {
                            Text("Presets", modifier = Modifier.padding(12.dp), color = if (activeTab == "filters") NeonPink else Color.White, fontSize = 13.sp)
                        }
                        Tab(selected = activeTab == "face", onClick = { activeTab = "face" }) {
                            Text("Face", modifier = Modifier.padding(12.dp), color = if (activeTab == "face") NeonPink else Color.White, fontSize = 13.sp)
                        }
                        Tab(selected = activeTab == "export", onClick = { activeTab = "export" }) {
                            Text("Export", modifier = Modifier.padding(12.dp), color = if (activeTab == "export") NeonPink else Color.White, fontSize = 13.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = { activeTab = "export"; viewModel.selectCanvaElement(null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Canva Layout")
                    }
                    if (activeTab == "export") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Custom canvas encoding format ready", color = TextSecondary, fontSize = 11.sp)
                            TextButton(onClick = { activeTab = "adjust" }) {
                                Text("Back to editing", color = NeonCyan, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatrixSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.width(90.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = CyberSurfaceElevated
            )
        )
        Text(
            text = "${value.toInt()}",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.width(35.dp),
            textAlign = TextAlign.End
        )
    }
}

// 4. CANVA TEMPLATES SCREEN
@Composable
fun TemplatesScreen(
    viewModel: LensoraViewModel,
    onSelect: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Canva Lite Templates", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Instant multi-layer templates, 100% editable offline", color = TextSecondary, fontSize = 13.sp)
        }

        items(CanvaTemplates.templatesList) { template ->
            Card(
                onClick = {
                    viewModel.selectTemplate(template)
                    onSelect()
                },
                colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(Color(android.graphics.Color.parseColor(template.backgroundHex)), RoundedCornerShape(10.dp))
                            .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(template.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(template.category, color = NeonPink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("${template.elements.size} editable vector components", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// 5. CUSTOM PRESETS SCREEN
@Composable
fun PresetsScreen(
    viewModel: LensoraViewModel,
    onSelectPreset: () -> Unit
) {
    val presets by viewModel.presetsList.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Presets Manager", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Compile and apply rapid color tuning filters custom made", color = TextSecondary, fontSize = 12.sp)
        }

        if (presets.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FilterFrames, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No custom presets created yet", color = TextSecondary, fontSize = 13.sp)
                        Text("Fine tune a picture in editor and tap '+ Create Custom' to compile.", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(presets) { preset ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).clickable {
                            viewModel.applyCustomPreset(preset)
                            onSelectPreset()
                        }) {
                            Text(preset.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Brt: ${preset.brightness.toInt()} | Cnt: ${preset.contrast.toInt()} | Sat: ${preset.saturation.toInt()}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = { viewModel.deletePreset(preset.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete preset", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

// 6. DRAFT PROJECTS SCREEN
@Composable
fun ProjectsScreen(
    viewModel: LensoraViewModel,
    onRestored: () -> Unit
) {
    val context = LocalContext.current
    val projects by viewModel.projectsList.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Draft Room Backups", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Never lose progress. Restore offline draft states completely.", color = TextSecondary, fontSize = 12.sp)
        }

        if (projects.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Any designs or presets saved appear here", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(projects) { project ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = GlassCardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.restoreDraft(project, context)
                                    onRestored()
                                }
                        ) {
                            Text(project.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            val type = if (project.isCanvaDesign) "Canva Template Overlay" else "Photo Tuning Stack"
                            Text(type, color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "Created: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(project.createdAt))}",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        IconButton(onClick = { viewModel.deleteProject(project.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete design draft", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

// 7. INFO AND HELP SETTINGS
@Composable
fun SettingsScreen(viewModel: LensoraViewModel) {
    val stats by viewModel.stats.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Info & Offline Stats", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // Offline Analytics visualizer
        Card(
            colors = CardDefaults.cardColors(containerColor = GlassCardBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Total System Analytics", color = NeonPink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Drafts Saved", color = TextSecondary, fontSize = 12.sp)
                    Text("${stats.totalProjects}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Divider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Image Renderings", color = TextSecondary, fontSize = 12.sp)
                    Text("${stats.totalExports}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Divider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Time Editing", color = TextSecondary, fontSize = 12.sp)
                    val hrs = stats.totalEditingTimeSeconds / 3600
                    val mins = (stats.totalEditingTimeSeconds % 3600) / 60
                    val textVal = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m ${stats.totalEditingTimeSeconds % 60}s"
                    Text(textVal, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Divider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Offline Storage Usage", color = TextSecondary, fontSize = 12.sp)
                    val mb = stats.storageUsageBytes / (1024.0 * 1024.0)
                    Text(String.format("%.2f MB", mb), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Developer bio card
        Text("Developer & Company Credentials", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Card(
            colors = CardDefaults.cardColors(containerColor = GlassCardBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Author & Chief Architect", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Prince AR Abdur Rahman", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Independent app developer passionate about building next-generation secure Android apps and design utilities.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text("Design Studio Organization", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("NexVora Lab's Ofc", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Focuses on creating privacy-first, zero-telemetry, beautifully responsive utilities accessible to everyone globally.",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("WhatsApp: +8801707424006 / +8801796951709", fontSize = 11.sp, color = BrightGold)
                Text("Version: 1.0.0 (Release Build Stable)", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}
