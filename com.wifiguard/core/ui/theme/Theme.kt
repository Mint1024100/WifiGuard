package com.wifiguard.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Extended colors for WiFi-specific functionality
data class WifiColors(
    val securityHigh: Color,
    val securityMedium: Color,
    val securityLow: Color,
    val securityUnknown: Color,
    val signalExcellent: Color,
    val signalGood: Color,
    val signalFair: Color,
    val signalPoor: Color,
    val signalNone: Color,
    val statusActive: Color,
    val statusScanning: Color,
    val statusDisconnected: Color
)

private val LightWifiColors = WifiColors(
    securityHigh = WifiSecurityHigh,
    securityMedium = WifiSecurityMedium,
    securityLow = WifiSecurityLow,
    securityUnknown = WifiSecurityUnknown,
    signalExcellent = SignalStrengthExcellent,
    signalGood = SignalStrengthGood,
    signalFair = SignalStrengthFair,
    signalPoor = SignalStrengthPoor,
    signalNone = SignalStrengthNone,
    statusActive = StatusActive,
    statusScanning = StatusScanning,
    statusDisconnected = StatusDisconnected
)

private val DarkWifiColors = WifiColors(
    securityHigh = WifiSecurityHighDark,
    securityMedium = WifiSecurityMediumDark,
    securityLow = WifiSecurityLowDark,
    securityUnknown = WifiSecurityUnknownDark,
    signalExcellent = SignalStrengthExcellent,
    signalGood = SignalStrengthGood,
    signalFair = SignalStrengthFair,
    signalPoor = SignalStrengthPoor,
    signalNone = SignalStrengthNone,
    statusActive = StatusActiveDark,
    statusScanning = StatusScanningDark,
    statusDisconnected = StatusDisconnectedDark
)

// Material 3 Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inverseSurface = md_theme_light_inverseSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
)

// Material 3 Dark Color Scheme  
private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inverseSurface = md_theme_dark_inverseSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
)

// CompositionLocal для кастомных Wi-Fi цветов
val LocalWifiColors = staticCompositionLocalOf { LightWifiColors }

@Composable
fun WifiGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Поддержка динамических цветов Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // Определение цветовой схемы
    val colorScheme = when {
        // Динамические цвета доступны с Android 12 (API 31)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    // Кастомные Wi-Fi цвета (не зависят от динамических цветов)
    val wifiColors = if (darkTheme) DarkWifiColors else LightWifiColors
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalWifiColors provides wifiColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Extension для удобного доступа к кастомным цветам
val MaterialTheme.wifiColors: WifiColors
    @Composable get() = LocalWifiColors.current

// Вспомогательные функции для получения цветов по уровню безопасности
@Composable
fun getSecurityColor(securityLevel: String): Color {
    return when (securityLevel.uppercase()) {
        "WPA3", "WPA2" -> MaterialTheme.wifiColors.securityHigh
        "WPA", "WEP" -> MaterialTheme.wifiColors.securityMedium
        "OPEN", "NONE" -> MaterialTheme.wifiColors.securityLow
        else -> MaterialTheme.wifiColors.securityUnknown
    }
}

@Composable  
fun getSignalColor(signalStrength: Int): Color {
    return when {
        signalStrength >= -50 -> MaterialTheme.wifiColors.signalExcellent
        signalStrength >= -60 -> MaterialTheme.wifiColors.signalGood  
        signalStrength >= -70 -> MaterialTheme.wifiColors.signalFair
        signalStrength >= -80 -> MaterialTheme.wifiColors.signalPoor
        else -> MaterialTheme.wifiColors.signalNone
    }
}
