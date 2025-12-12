package com.wifiguard.core.ui.theme

import androidx.compose.ui.graphics.Color

// ==============================================================================
// Modern "Cyber Security" Palette - Deep Navy & Emerald Theme
// Designed for high contrast, professional trust, and modern tech aesthetics.
// ==============================================================================

// Primary Brand Colors (Deep Navy based)
// Dark: Deep Navy for background and surfaces.
val TechBluePrimaryLight = Color(0xFF3757D4)        // Original blue for light mode compatibility
val TechBluePrimaryDark = Color(0xFF1E293B)         // Deep Navy surface
val TechBlueContainerLight = Color(0xFFDEE9FC)      // Original container for light mode
val TechBlueContainerDark = Color(0xFF0F172A)       // Deep Navy background

// Secondary Brand Colors (Emerald based)
// Used for accents, actions, and "secure" indicators.
val CyberCyanSecondaryLight = Color(0xFF008DA6)     // Original cyan for light mode
val CyberCyanSecondaryDark = Color(0xFF10B981)      // Emerald primary accent
val CyberCyanContainerLight = Color(0xFFE0F7FA)     // Original container for light mode
val CyberCyanContainerDark = Color(0xFF047857)      // Dark emerald container

// Tertiary Brand Colors (Amber/Red for warnings)
// Used for critical warnings and alerts.
val DeepPurpleTertiaryLight = Color(0xFF651FFF)     // Original purple for light mode
val DeepPurpleTertiaryDark = Color(0xFFF59E0B)     // Amber for warnings
val DeepPurpleContainerLight = Color(0xFFEDE7F6)    // Original container for light mode
val DeepPurpleContainerDark = Color(0xFFDC2626)     // Red for critical warnings

// Neutral / Background Colors - Dark Theme Focus
// Cyber Security themed: Deep Navy background, Slate surfaces.
val NeutralBackgroundLight = Color(0xFFF4F5F7)      // Original light background
val NeutralBackgroundDark = Color(0xFF0F172A)       // Deep Navy background (#0F172A)
val NeutralSurfaceLight = Color(0xFFFFFFFF)         // Original light surface
val NeutralSurfaceDark = Color(0xFF1E293B)          // Slate 800 surface (#1E293B)

// Text Colors - High contrast for security tools
val TextPrimaryLight = Color(0xFF172B4D)            // Original light primary text
val TextPrimaryDark = Color(0xFFF8FAFC)             // High contrast primary text (#F8FAFC)
val TextSecondaryLight = Color(0xFF5E6C84)          // Original light secondary text
val TextSecondaryDark = Color(0xFF94A3B8)           // Slate 400 secondary text (#94A3B8)

// Error Colors - Cyber Security themed
val ErrorLight = Color(0xFFBF2600)                  // Original light error
val ErrorDark = Color(0xFFEF4444)                   // Red 500 for errors (#EF4444)
val ErrorContainerLight = Color(0xFFFFEBE6)         // Original light error container
val ErrorContainerDark = Color(0xFFB91C1C)          // Dark red error container

// ==============================================================================
// Status Indicators (Signals, Security) - Cyber Security Themed
// ==============================================================================

// Security Levels - Updated for Emerald/Red theme
val SecuritySafe = Color(0xFF10B981)                // Emerald 500 for safe networks
val SecurityLow = Color(0xFF34D399)                 // Light emerald for low risk
val SecurityMedium = Color(0xFFF59E0B)               // Amber 500 for medium risk
val SecurityHigh = Color(0xFFEF4444)                 // Red 500 for high risk
val SecurityCritical = Color(0xFFDC2626)             // Red 600 for critical risk
val SecurityUnknown = Color(0xFF94A3B8)              // Slate 400 for unknown

// Signal Strength - Harmonious with security colors
val SignalExcellent = Color(0xFF10B981)             // Emerald (same as safe)
val SignalGood = Color(0xFF34D399)                  // Light emerald
val SignalFair = Color(0xFFF59E0B)                  // Amber (same as medium risk)
val SignalWeak = Color(0xFFFBBF24)                  // Yellow for weak signals
val SignalVeryWeak = Color(0xFFEF4444)              // Red (same as high risk)
val SignalCritical = Color(0xFFB91C1C)              // Dark red

// Outline & Dividers - Updated for dark theme
val OutlineLight = Color(0xFFDFE1E6)                // Original light outline
val OutlineDark = Color(0xFF94A3B8)                 // Light outline for dark theme

// Mapping to Generic Names for Theme.kt - Updated for Cyber Security Theme
val WifiGuardPrimary = TechBluePrimaryDark           // Deep Navy (dark theme primary)
val WifiGuardPrimaryContainer = TechBlueContainerDark // Deep Navy container
val WifiGuardOnPrimary = Color(0xFFF8FAFC)           // Light text on primary
val WifiGuardOnPrimaryContainer = Color(0xFF94A3B8)  // Secondary light text

val WifiGuardSecondary = CyberCyanSecondaryDark      // Emerald accent
val WifiGuardSecondaryContainer = CyberCyanContainerDark // Dark emerald container
val WifiGuardOnSecondary = Color(0xFFFFFFFF)         // White text on secondary
val WifiGuardOnSecondaryContainer = Color(0xFFECFDF5) // Light emerald container

val WifiGuardTertiary = DeepPurpleTertiaryDark        // Amber for warnings
val WifiGuardTertiaryContainer = DeepPurpleContainerDark // Red for critical warnings
val WifiGuardOnTertiary = Color(0xFFFFFFFF)          // White text on tertiary
val WifiGuardOnTertiaryContainer = Color(0xFFFEF2F2) // Light red container

val WifiGuardBackground = NeutralBackgroundDark      // Deep Navy background
val WifiGuardOnBackground = TextPrimaryDark          // Light text on background
val WifiGuardSurface = NeutralSurfaceDark            // Slate 800 surface
val WifiGuardOnSurface = TextPrimaryDark             // Light text on surface

val WifiGuardError = ErrorDark                       // Red error
val WifiGuardOnError = Color(0xFFFFFFFF)             // White text on error
val WifiGuardErrorContainer = ErrorContainerDark     // Dark red error container
val WifiGuardOnErrorContainer = Color(0xFFFEE2E2)    // Light red error container

val WifiGuardOutline = OutlineDark                   // Light outline for dark theme
val WifiGuardInverseOnSurface = TextPrimaryLight     // Dark text for inverse
val WifiGuardInverseSurface = TextPrimaryDark        // Light surface for inverse
val WifiGuardInversePrimary = TechBluePrimaryDark    // Deep Navy for inverse primary

/**
 * Вычисляет относительную яркость цвета (0.0 - 1.0)
 * Используется для определения темной/светлой темы
 */
fun Color.calculateLuminance(): Float {
    val red = this.red
    val green = this.green
    val blue = this.blue
    
    // Формула относительной яркости (ITU-R BT.709)
    return 0.299f * red + 0.587f * green + 0.114f * blue
}