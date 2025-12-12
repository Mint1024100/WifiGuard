package com.wifiguard.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wifiguard.feature.about.presentation.AboutScreen
import com.wifiguard.feature.analysis.presentation.AnalysisScreen
import com.wifiguard.feature.notifications.presentation.NotificationsScreen
import com.wifiguard.feature.privacypolicy.presentation.PrivacyPolicyScreen
import com.wifiguard.feature.scanner.presentation.ScannerScreen
import com.wifiguard.feature.securityreport.presentation.SecurityReportScreen
import com.wifiguard.feature.settings.presentation.SettingsScreen
import com.wifiguard.feature.termsofservice.presentation.TermsOfServiceScreen

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
                onNavigateToSecurityReport = {
                    navController.navigate(Screen.SecurityReport.route)
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
            AboutScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.TermsOfService.route) {
            TermsOfServiceScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.SecurityReport.route) {
            SecurityReportScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}