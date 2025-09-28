package com.wifiguard.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    
    data object Scanner : Screen("scanner")
    
    data object Analysis : Screen("analysis/{networkId}") {
        const val ARG_NETWORK_ID = "networkId"
        
        fun createRoute(networkId: String): String {
            return "analysis/$networkId"
        }
        
        val arguments: List<NamedNavArgument> = listOf(
            navArgument(ARG_NETWORK_ID) {
                type = NavType.StringType
                nullable = false
            }
        )
    }
    
    data object Settings : Screen("settings")
    
    data object Notifications : Screen("notifications")
}

// Navigation destinations for bottom navigation
val bottomNavigationScreens = listOf(
    Screen.Scanner,
    Screen.Notifications,
    Screen.Settings
)