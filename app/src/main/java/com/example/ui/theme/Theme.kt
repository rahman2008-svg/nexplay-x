package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val AmoledDarkColorScheme =
  darkColorScheme(
    primary = CyberCyan,
    secondary = CyberPink,
    tertiary = ActiveBlue,
    background = AmoledBlack,
    surface = DarkSurface,
    surfaceVariant = DarkCard,
    outline = SlateGray,
    onPrimary = Color(0xFF21005D),
    onSecondary = Color(0xFF1D192B),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFCCC2DC)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ActiveBlue,
    secondary = CyberPink,
    tertiary = CyberCyan,
    background = Color(0xFFF9F9FF),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F1F6),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to true for premium AMOLED visual experience
  dynamicColor: Boolean = false, // Disable default dynamic color to maintain stunning custom branding by default!
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) AmoledDarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
