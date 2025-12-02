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
    medium = RoundedCornerShape(12.dp),         // For standard cards and dialogs
    large = RoundedCornerShape(16.dp),          // For larger containers and sheets
    extraLarge = RoundedCornerShape(28.dp)      // For bottom sheets and special elements
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
    primary = WifiGuardPrimary,
    onPrimary = WifiGuardOnPrimary,
    primaryContainer = WifiGuardPrimaryContainer,
    onPrimaryContainer = WifiGuardOnPrimaryContainer,
    secondary = WifiGuardSecondary,
    onSecondary = WifiGuardOnSecondary,
    secondaryContainer = WifiGuardSecondaryContainer,
    onSecondaryContainer = WifiGuardOnSecondaryContainer,
    tertiary = WifiGuardTertiary,
    onTertiary = WifiGuardOnTertiary,
    tertiaryContainer = WifiGuardTertiaryContainer,
    onTertiaryContainer = WifiGuardOnTertiaryContainer,
    background = WifiGuardBackground,
    onBackground = WifiGuardOnBackground,
    surface = WifiGuardSurface,
    onSurface = WifiGuardOnSurface,
    error = WifiGuardError,
    onError = WifiGuardOnError,
    errorContainer = WifiGuardErrorContainer,
    onErrorContainer = WifiGuardOnErrorContainer,
    outline = WifiGuardOutline,
    inverseOnSurface = WifiGuardInverseOnSurface,
    inverseSurface = WifiGuardInverseSurface,
    inversePrimary = WifiGuardInversePrimary,
)

// Modern dark color scheme
private val DarkColorScheme = darkColorScheme(
    primary = WifiGuardPrimary,
    onPrimary = WifiGuardOnPrimary,
    primaryContainer = WifiGuardPrimaryContainer,
    onPrimaryContainer = WifiGuardOnPrimaryContainer,
    secondary = WifiGuardSecondary,
    onSecondary = WifiGuardOnSecondary,
    secondaryContainer = WifiGuardSecondaryContainer,
    onSecondaryContainer = WifiGuardOnSecondaryContainer,
    tertiary = WifiGuardTertiary,
    onTertiary = WifiGuardOnTertiary,
    tertiaryContainer = WifiGuardTertiaryContainer,
    onTertiaryContainer = WifiGuardOnTertiaryContainer,
    background = WifiGuardBackground,
    onBackground = WifiGuardOnBackground,
    surface = WifiGuardSurface,
    onSurface = WifiGuardOnSurface,
    error = WifiGuardError,
    onError = WifiGuardOnError,
    errorContainer = WifiGuardErrorContainer,
    onErrorContainer = WifiGuardOnErrorContainer,
    outline = WifiGuardOutline,
    inverseOnSurface = WifiGuardInverseOnSurface,
    inverseSurface = WifiGuardInverseSurface,
    inversePrimary = WifiGuardInversePrimary,
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
            window.statusBarColor = colorScheme.primary.toArgb()
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