package com.parspodcast.studio.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat

private val ParsPodcastDarkColorScheme = darkColorScheme(
    primary = Turquoise,
    onPrimary = DeepNavy,
    primaryContainer = SecondaryNavy,
    onPrimaryContainer = Cyan,
    secondary = Cyan,
    onSecondary = DeepNavy,
    tertiary = PurpleAccent,
    onTertiary = PrimaryText,
    background = DeepNavy,
    onBackground = PrimaryText,
    surface = SecondaryNavy,
    onSurface = PrimaryText,
    surfaceVariant = DarkNavyCard,
    onSurfaceVariant = SecondaryText,
    outline = DarkNavyBorder,
    error = ErrorRed,
    onError = PrimaryText
)

private val ParsPodcastLightColorScheme = lightColorScheme(
    primary = Color(0xFF00838F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = Color(0xFF004D40),
    secondary = Color(0xFF0288D1),
    onSecondary = Color.White,
    tertiary = Color(0xFF5E35B1),
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEDF2F7),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun ParsPodcastTheme(
    darkTheme: Boolean = true, // Dark theme as default
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ParsPodcastDarkColorScheme else ParsPodcastLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // Enforce full Persian RTL layout direction for the entire application
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
