package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MediaViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MediaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Gracefully request standard storage permissions on Android (API <= 32 or 33+)
        checkAndRequestStoragePermissions()

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf("dashboard") }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    // State based screen switcher
                    when (currentScreen) {
                        "dashboard" -> {
                            DashboardComponent(
                                viewModel = viewModel,
                                onNavigate = { screen -> currentScreen = screen }
                            )
                        }
                        "music" -> {
                            MusicPlayerComponent(
                                viewModel = viewModel,
                                onBack = { currentScreen = "dashboard" }
                            )
                        }
                        "video" -> {
                            VideoPlayerComponent(
                                viewModel = viewModel,
                                onBack = { currentScreen = "dashboard" }
                            )
                        }
                        "gallery" -> {
                            GalleryComponent(
                                viewModel = viewModel,
                                onBack = { currentScreen = "dashboard" }
                            )
                        }
                        "files" -> {
                            FileManagerComponent(
                                viewModel = viewModel,
                                onBack = { currentScreen = "dashboard" }
                            )
                        }
                        "vault" -> {
                            PrivateVaultComponent(
                                viewModel = viewModel,
                                onBack = { currentScreen = "dashboard" }
                            )
                        }
                        "tools" -> {
                            ToolsComponent(
                                viewModel = viewModel,
                                onBack = { currentScreen = "dashboard" }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestStoragePermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), 101)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            // Refresh media list if permissions were granted
            viewModel.refreshAllMedia()
        }
    }
}
