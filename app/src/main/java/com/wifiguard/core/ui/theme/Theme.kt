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
    extraSmall = RoundedCornerShape(8.dp),      // For small elements like checkboxes
    small = RoundedCornerShape(12.dp),          // For buttons and small cards
    medium = RoundedCornerShape(20.dp),         // For standard cards and dialogs (Increased from 16dp)
    large = RoundedCornerShape(28.dp),          // For larger containers and sheets (Increased from 24dp)
    extraLarge = RoundedCornerShape(36.dp)      // For bottom sheets and special elements (Increased from 32dp)
)

// Modern elevation system with consistent shadow effects
object WifiGuardElevation {
    val Level0 = 0.dp
    val Level1 = 1.dp
    val Level2 = 3.dp      // Slightly increased for better visibility
    val Level3 = 6.dp      // Slightly increased for better visibility
    val Level4 = 10.dp     // Slightly increased for better visibility
    val Level5 = 16.dp
    val Level6 = 20.dp     // Increased for important elements
    val Level7 = 28.dp     // Increased for floating elements
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

// Modern dark color scheme - Cyber Security Theme
private val DarkColorScheme = darkColorScheme(
    primary = TechBluePrimaryDark,          // Deep Navy surface
    onPrimary = Color(0xFFF8FAFC),          // Light text on primary (F8FAFC)
    primaryContainer = TechBlueContainerDark, // Deep Navy background
    onPrimaryContainer = Color(0xFF94A3B8),  // Light secondary text on primary container (94A3B8)
    secondary = CyberCyanSecondaryDark,     // Emerald accent
    onSecondary = Color.White,              // White text on secondary
    secondaryContainer = CyberCyanContainerDark, // Dark emerald container
    onSecondaryContainer = Color(0xFFECFDF5), // Light emerald container text
    tertiary = DeepPurpleTertiaryDark,      // Amber for warnings
    onTertiary = Color.White,               // White text on tertiary
    tertiaryContainer = DeepPurpleContainerDark, // Red for critical warnings
    onTertiaryContainer = Color(0xFFFEF2F2), // Light red container text
    background = NeutralBackgroundDark,     // Deep Navy background (#0F172A)
    onBackground = TextPrimaryDark,         // Light text on background (#F8FAFC)
    surface = NeutralSurfaceDark,           // Slate 800 surface (#1E293B)
    onSurface = TextPrimaryDark,            // Light text on surface (#F8FAFC)
    surfaceVariant = Color(0xFF334155),     // Darker surface for cards/inputs (Slate 700)
    onSurfaceVariant = TextSecondaryDark,   // Light secondary text (#94A3B8)
    error = ErrorDark,                      // Red error
    onError = Color.White,                  // White text on error
    errorContainer = ErrorContainerDark,    // Dark red error container
    onErrorContainer = Color(0xFFFEE2E2),   // Light red error container
    outline = OutlineDark,                  // Light outline for dark theme (#94A3B8)
    inverseOnSurface = TextPrimaryLight,    // Dark text for inverse
    inverseSurface = TextPrimaryDark,       // Light surface for inverse
    inversePrimary = TechBluePrimaryDark,   // Deep Navy for inverse primary
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