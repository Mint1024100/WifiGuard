package com.wifiguard.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape

// Modern shape system for rounded corners and consistent design language
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),      // For small elements like checkboxes
    small = RoundedCornerShape(8.dp),           // For buttons and small cards
    medium = RoundedCornerShape(16.dp),         // For standard cards and dialogs (Increased from 12dp)
    large = RoundedCornerShape(24.dp),          // For larger containers and sheets (Increased from 16dp)
    extraLarge = RoundedCornerShape(32.dp)      // For bottom sheets and special elements (Increased from 28dp)
)

// Modern elevation system with consistent shadow effects
object WifiGuardElevation {
    val Level0 = 0.dp
    val Level1 = 1.dp
    val Level2 = 2.dp
    val Level3 = 4.dp
    val Level4 = 8.dp
    val Level5 = 12.dp
    val Level6 = 16.dp
    val Level7 = 24.dp
}

// Modern light color scheme
private val LightColorScheme = lightColorScheme(
    primary = TechBluePrimaryLight,
    onPrimary = Color.White,
    primaryContainer = TechBlueContainerLight,
    onPrimaryContainer = Color(0xFF002561),
    secondary = CyberCyanSecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = CyberCyanContainerLight,
    onSecondaryContainer = Color(0xFF003642),
    tertiary = DeepPurpleTertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = DeepPurpleContainerLight,
    onTertiaryContainer = Color(0xFF20005F),
    background = NeutralBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = NeutralSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE1E3E8), // Slightly darker surface for cards/inputs
    onSurfaceVariant = TextSecondaryLight,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = Color(0xFF420D04),
    outline = OutlineLight,
    inverseOnSurface = TextPrimaryDark,
    inverseSurface = NeutralSurfaceDark,
    inversePrimary = TechBluePrimaryDark,
)

// Modern dark color scheme
private val DarkColorScheme = darkColorScheme(
    primary = TechBluePrimaryDark,
    onPrimary = Color(0xFF002561), // Dark text on light primary in dark mode? No, usually dark mode primary is light.
    primaryContainer = TechBlueContainerDark,
    onPrimaryContainer = Color(0xFFDEE9FC),
    secondary = CyberCyanSecondaryDark,
    onSecondary = Color(0xFF003642),
    secondaryContainer = CyberCyanContainerDark,
    onSecondaryContainer = Color(0xFFE0F7FA),
    tertiary = DeepPurpleTertiaryDark,
    onTertiary = Color(0xFF20005F),
    tertiaryContainer = DeepPurpleContainerDark,
    onTertiaryContainer = Color(0xFFEDE7F6),
    background = NeutralBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = NeutralSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF21262D), // Slightly lighter surface for cards/inputs
    onSurfaceVariant = TextSecondaryDark,
    error = ErrorDark,
    onError = Color(0xFF420D04),
    errorContainer = ErrorContainerDark,
    onErrorContainer = Color(0xFFFFEBE6),
    outline = OutlineDark,
    inverseOnSurface = TextPrimaryLight,
    inverseSurface = NeutralSurfaceLight,
    inversePrimary = TechBluePrimaryLight,
)

@Composable
fun WifiGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb() // Match background for seamless look
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}