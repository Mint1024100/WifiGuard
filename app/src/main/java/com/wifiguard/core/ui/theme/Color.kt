package com.wifiguard.core.ui.theme

import androidx.compose.ui.graphics.Color

// ==============================================================================
// Modern "Cyber Security" Palette
// Designed for high contrast, professional trust, and modern tech aesthetics.
// ==============================================================================

// Primary Brand Colors (Blue/Indigo based)
// Light: Deep, trustworthy blue. Dark: Vibrant, electric blue.
val TechBluePrimaryLight = Color(0xFF0052CC)
val TechBluePrimaryDark = Color(0xFF4C9AFF)
val TechBlueContainerLight = Color(0xFFDEE9FC)
val TechBlueContainerDark = Color(0xFF003380)

// Secondary Brand Colors (Cyan/Teal based)
// Used for accents, actions, and "secure" indicators.
val CyberCyanSecondaryLight = Color(0xFF008DA6)
val CyberCyanSecondaryDark = Color(0xFF00D8FF)
val CyberCyanContainerLight = Color(0xFFE0F7FA)
val CyberCyanContainerDark = Color(0xFF004D5C)

// Tertiary Brand Colors (Purple/Violet based)
// Used for special highlights or "premium" features.
val DeepPurpleTertiaryLight = Color(0xFF651FFF)
val DeepPurpleTertiaryDark = Color(0xFFB388FF)
val DeepPurpleContainerLight = Color(0xFFEDE7F6)
val DeepPurpleContainerDark = Color(0xFF320B86)

// Neutral / Background Colors
// Light: Clean, professional gray-white. Dark: Deep, "Github Dimmed" style navy-gray.
val NeutralBackgroundLight = Color(0xFFF4F5F7)
val NeutralBackgroundDark = Color(0xFF0D1117)
val NeutralSurfaceLight = Color(0xFFFFFFFF)
val NeutralSurfaceDark = Color(0xFF161B22)

// Text Colors
val TextPrimaryLight = Color(0xFF172B4D)
val TextPrimaryDark = Color(0xFFF0F6FC)
val TextSecondaryLight = Color(0xFF5E6C84)
val TextSecondaryDark = Color(0xFF8B949E)

// Error Colors
val ErrorLight = Color(0xFFBF2600)
val ErrorDark = Color(0xFFFF7B72)
val ErrorContainerLight = Color(0xFFFFEBE6)
val ErrorContainerDark = Color(0xFF521816)

// ==============================================================================
// Status Indicators (Signals, Security)
// ==============================================================================

// Security Levels
val SecuritySafe = Color(0xFF008DA6)      // Cyan/Teal
val SecurityLow = Color(0xFF36B37E)       // Green
val SecurityMedium = Color(0xFFFFAB00)    // Amber
val SecurityHigh = Color(0xFFFF5630)      // Orange-Red
val SecurityCritical = Color(0xFFBF2600)  // Deep Red
val SecurityUnknown = Color(0xFF6B778C)   // Slate Gray

// Signal Strength (distinct from security to avoid confusion, but harmonious)
val SignalExcellent = Color(0xFF00A3BF)
val SignalGood = Color(0xFF36B37E)
val SignalFair = Color(0xFFFFAB00)
val SignalWeak = Color(0xFFFF8B00)
val SignalVeryWeak = Color(0xFFFF5630)
val SignalCritical = Color(0xFFBF2600)

// Outline & Dividers
val OutlineLight = Color(0xFFDFE1E6)
val OutlineDark = Color(0xFF30363D)

// Mapping to Generic Names for Theme.kt
val WifiGuardPrimary = TechBluePrimaryLight
val WifiGuardPrimaryContainer = TechBlueContainerLight
val WifiGuardOnPrimary = Color.White
val WifiGuardOnPrimaryContainer = Color(0xFF002561)

val WifiGuardSecondary = CyberCyanSecondaryLight
val WifiGuardSecondaryContainer = CyberCyanContainerLight
val WifiGuardOnSecondary = Color.White
val WifiGuardOnSecondaryContainer = Color(0xFF003642)

val WifiGuardTertiary = DeepPurpleTertiaryLight
val WifiGuardTertiaryContainer = DeepPurpleContainerLight
val WifiGuardOnTertiary = Color.White
val WifiGuardOnTertiaryContainer = Color(0xFF20005F)

val WifiGuardBackground = NeutralBackgroundLight
val WifiGuardOnBackground = TextPrimaryLight
val WifiGuardSurface = NeutralSurfaceLight
val WifiGuardOnSurface = TextPrimaryLight

val WifiGuardError = ErrorLight
val WifiGuardOnError = Color.White
val WifiGuardErrorContainer = ErrorContainerLight
val WifiGuardOnErrorContainer = Color(0xFF420D04)

val WifiGuardOutline = OutlineLight
val WifiGuardInverseOnSurface = TextPrimaryDark
val WifiGuardInverseSurface = NeutralSurfaceDark
val WifiGuardInversePrimary = TechBluePrimaryDark