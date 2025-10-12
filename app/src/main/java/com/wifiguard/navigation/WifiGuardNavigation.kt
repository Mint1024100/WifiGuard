package com.wifiguard.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wifiguard.feature.analysis.presentation.AnalysisScreen
import com.wifiguard.feature.notifications.presentation.NotificationsScreen
import com.wifiguard.feature.scanner.presentation.ScannerScreen
import com.wifiguard.feature.settings.presentation.SettingsScreen

/**
 * Навигационный граф приложения
 */
@Composable
fun WifiGuardNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Scanner.route
    ) {
        composable(Screen.Scanner.route) {
            ScannerScreen(
                onNavigateToAnalysis = {
                    navController.navigate(Screen.Analysis.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(Screen.Analysis.route) {
            AnalysisScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAbout = {
                    navController.navigate(Screen.About.route)
                },
                onNavigateToPrivacyPolicy = {
                    navController.navigate(Screen.PrivacyPolicy.route)
                },
                onNavigateToTermsOfService = {
                    navController.navigate(Screen.TermsOfService.route)
                }
            )
        }
        
        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.About.route) {
            // TODO: Implement AboutScreen
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAbout = { },
                onNavigateToPrivacyPolicy = { },
                onNavigateToTermsOfService = { }
            )
        }
        
        composable(Screen.PrivacyPolicy.route) {
            // TODO: Implement PrivacyPolicyScreen
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAbout = { },
                onNavigateToPrivacyPolicy = { },
                onNavigateToTermsOfService = { }
            )
        }
        
        composable(Screen.TermsOfService.route) {
            // TODO: Implement TermsOfServiceScreen
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAbout = { },
                onNavigateToPrivacyPolicy = { },
                onNavigateToTermsOfService = { }
            )
        }
    }
}