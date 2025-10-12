package com.wifiguard.navigation

/**
 * Определение экранов приложения
 */
sealed class Screen(val route: String) {
    object Scanner : Screen("scanner")
    object Analysis : Screen("analysis")
    object Settings : Screen("settings")
    object Notifications : Screen("notifications")
    
    // Дополнительные экраны
    object NetworkDetails : Screen("network_details/{networkId}") {
        fun createRoute(networkId: String) = "network_details/$networkId"
    }
    
    object ThreatDetails : Screen("threat_details/{threatId}") {
        fun createRoute(threatId: String) = "threat_details/$threatId"
    }
    
    object SecurityReport : Screen("security_report")
    object About : Screen("about")
    object PrivacyPolicy : Screen("privacy_policy")
    object TermsOfService : Screen("terms_of_service")
}