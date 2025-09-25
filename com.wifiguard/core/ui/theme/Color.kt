package com.wifiguard.core.ui.theme

import androidx.compose.ui.graphics.Color

// Primary colors - основные цвета приложения (сине-зеленая гамма)
val md_theme_light_primary = Color(0xFF006C4C)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFF89F8C7)
val md_theme_light_onPrimaryContainer = Color(0xFF002114)

val md_theme_dark_primary = Color(0xFF6ADBAA)
val md_theme_dark_onPrimary = Color(0xFF003828)
val md_theme_dark_primaryContainer = Color(0xFF00513A)
val md_theme_dark_onPrimaryContainer = Color(0xFF89F8C7)

// Secondary colors - вторичные цвета (серо-синие тона)
val md_theme_light_secondary = Color(0xFF4A6267)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFCDE7EC)
val md_theme_light_onSecondaryContainer = Color(0xFF051F23)

val md_theme_dark_secondary = Color(0xFFB1CBD0)
val md_theme_dark_onSecondary = Color(0xFF1C3438)
val md_theme_dark_secondaryContainer = Color(0xFF334B4F)
val md_theme_dark_onSecondaryContainer = Color(0xFFCDE7EC)

// Tertiary colors - третичные цвета (фиолетовые акценты)
val md_theme_light_tertiary = Color(0xFF446277)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFCAE6FF)
val md_theme_light_onTertiaryContainer = Color(0xFF001E2E)

val md_theme_dark_tertiary = Color(0xFF9ECAE3)
val md_theme_dark_onTertiary = Color(0xFF003347)
val md_theme_dark_tertiaryContainer = Color(0xFF28495E)
val md_theme_dark_onTertiaryContainer = Color(0xFFCAE6FF)

// Error colors - цвета ошибок и угроз безопасности
val md_theme_light_error = Color(0xFFB3261E)
val md_theme_light_errorContainer = Color(0xFFF9DEDC)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = Color(0xFF410E0B)

val md_theme_dark_error = Color(0xFFF2B8B5)
val md_theme_dark_errorContainer = Color(0xFF8C1D18)
val md_theme_dark_onError = Color(0xFF601410)
val md_theme_dark_onErrorContainer = Color(0xFFF9DEDC)

// Background colors - цвета фона
val md_theme_light_background = Color(0xFFFAFDFB)
val md_theme_light_onBackground = Color(0xFF191C1B)
val md_theme_light_surface = Color(0xFFFAFDFB)
val md_theme_light_onSurface = Color(0xFF191C1B)

val md_theme_dark_background = Color(0xFF0F1513)
val md_theme_dark_onBackground = Color(0xFFDFE4E2)
val md_theme_dark_surface = Color(0xFF0F1513)
val md_theme_dark_onSurface = Color(0xFFDFE4E2)

// Surface variant colors - варианты поверхностей
val md_theme_light_surfaceVariant = Color(0xFFDEE5E2)
val md_theme_light_onSurfaceVariant = Color(0xFF414944)
val md_theme_light_outline = Color(0xFF717970)
val md_theme_light_inverseOnSurface = Color(0xFFF0F1EE)
val md_theme_light_inverseSurface = Color(0xFF2E312F)
val md_theme_light_inversePrimary = Color(0xFF6ADBAA)
val md_theme_light_shadow = Color(0xFF000000)
val md_theme_light_surfaceTint = Color(0xFF006C4C)
val md_theme_light_outlineVariant = Color(0xFFC2C9C6)
val md_theme_light_scrim = Color(0xFF000000)

val md_theme_dark_surfaceVariant = Color(0xFF414944)
val md_theme_dark_onSurfaceVariant = Color(0xFFC2C9C6)
val md_theme_dark_outline = Color(0xFF8B9389)
val md_theme_dark_inverseOnSurface = Color(0xFF191C1B)
val md_theme_dark_inverseSurface = Color(0xFFDFE4E2)
val md_theme_dark_inversePrimary = Color(0xFF006C4C)
val md_theme_dark_shadow = Color(0xFF000000)
val md_theme_dark_surfaceTint = Color(0xFF6ADBAA)
val md_theme_dark_outlineVariant = Color(0xFF414944)
val md_theme_dark_scrim = Color(0xFF000000)

// Custom colors for WiFi security levels - кастомные цвета для уровней безопасности
val WifiSecurityHigh = Color(0xFF4CAF50)     // Зеленый - высокая безопасность
val WifiSecurityMedium = Color(0xFFFF9800)   // Оранжевый - средняя безопасность  
val WifiSecurityLow = Color(0xFFF44336)      // Красный - низкая безопасность
val WifiSecurityUnknown = Color(0xFF9E9E9E)  // Серый - неизвестно

val WifiSecurityHighDark = Color(0xFF81C784)
val WifiSecurityMediumDark = Color(0xFFFFB74D)
val WifiSecurityLowDark = Color(0xFFE57373)
val WifiSecurityUnknownDark = Color(0xFFBDBDBD)

// Signal strength colors - цвета для уровня сигнала
val SignalStrengthExcellent = Color(0xFF2E7D32)   // Темно-зеленый
val SignalStrengthGood = Color(0xFF388E3C)        // Зеленый
val SignalStrengthFair = Color(0xFFF57C00)        // Оранжевый
val SignalStrengthPoor = Color(0xFFD32F2F)        // Красный
val SignalStrengthNone = Color(0xFF616161)        // Серый

// Status colors - цвета статусов
val StatusActive = Color(0xFF1976D2)        // Синий - активное соединение
val StatusScanning = Color(0xFF7B1FA2)      // Фиолетовый - сканирование
val StatusDisconnected = Color(0xFF757575)  // Серый - отключено

val StatusActiveDark = Color(0xFF42A5F5)
val StatusScanningDark = Color(0xFFBA68C8)
val StatusDisconnectedDark = Color(0xFFBDBDBD)
