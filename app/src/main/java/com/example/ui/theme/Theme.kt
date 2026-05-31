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

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8), // Indigo 400
    onPrimary = Color(0xFF0A0B0D), // Dark slate/black
    primaryContainer = Color(0xFF312E81), // Dark Indigo
    onPrimaryContainer = Color(0xFFE0E7FF), // Slate light
    secondary = Color(0xFF6366F1), // Indigo 500
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFF0A0B0D), // Sophisticated Dark Background
    onBackground = Color(0xFFF1F5F9), // Slate 100
    surface = Color(0xFF1A1C1E), // Surface Dark
    onSurface = Color(0xFFE2E8F0), // Slate 200
    surfaceVariant = Color(0xFF272A30), // Surface Light
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6366F1),
    secondary = Color(0xFF4F46E5),
    background = Color(0xFF0A0B0D),
    surface = Color(0xFF1A1C1E)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme by default matching Sophisticated Dark
  dynamicColor: Boolean = false, // Disable dynamic colors to enforce the theme brand palette
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

