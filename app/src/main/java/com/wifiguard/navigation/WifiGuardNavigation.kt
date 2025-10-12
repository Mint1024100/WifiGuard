package com.wifiguard.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wifiguard.feature.scanner.presentation.ScannerScreen
import com.wifiguard.feature.analysis.presentation.AnalysisScreen
import com.wifiguard.feature.settings.presentation.SettingsScreen
import com.wifiguard.feature.notifications.presentation.NotificationsScreen

@Composable
fun WifiGuardNavigation(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Scanner.route,
        modifier = modifier
    ) {
        composable(
            route = Screen.Scanner.route
        ) {
            ScannerScreen(
                onNavigateToAnalysis = { networkId ->
                    navController.navigate(Screen.Analysis.createRoute(networkId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                }
            )
        }
        
        composable(
            route = Screen.Analysis.route,
            arguments = Screen.Analysis.arguments
        ) { backStackEntry ->
            val networkId = backStackEntry.arguments?.getString("networkId") ?: ""
            AnalysisScreen(
                networkId = networkId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.Settings.route
        ) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.Notifications.route
        ) {
            NotificationsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAnalysis = { networkId ->
                    navController.navigate(Screen.Analysis.createRoute(networkId))
                }
            )
        }
    }
}