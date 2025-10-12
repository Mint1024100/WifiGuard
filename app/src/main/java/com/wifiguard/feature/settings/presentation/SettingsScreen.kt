package com.wifiguard.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wifiguard.R

/**
 * Экран настроек
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToTermsOfService: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(stringResource(R.string.settings_title))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Общие настройки
            item {
                SettingsSection(
                    title = stringResource(R.string.settings_general),
                    icon = Icons.Default.Settings
                ) {
                    SettingsItem(
                        title = stringResource(R.string.settings_auto_scan),
                        subtitle = stringResource(R.string.settings_auto_scan_summary),
                        trailing = {
                            Switch(
                                checked = uiState.autoScanEnabled,
                                onCheckedChange = { viewModel.setAutoScanEnabled(it) }
                            )
                        }
                    )
                    
                    SettingsItem(
                        title = stringResource(R.string.settings_scan_interval),
                        subtitle = stringResource(R.string.settings_scan_interval_summary),
                        onClick = { /* TODO: Show interval picker */ }
                    )
                }
            }
            
            // Настройки безопасности
            item {
                SettingsSection(
                    title = stringResource(R.string.settings_security),
                    icon = Icons.Default.Security
                ) {
                    SettingsItem(
                        title = stringResource(R.string.settings_data_retention),
                        subtitle = stringResource(R.string.settings_data_retention_summary),
                        onClick = { /* TODO: Show retention picker */ }
                    )
                    
                    SettingsItem(
                        title = stringResource(R.string.settings_export_data),
                        subtitle = stringResource(R.string.settings_export_data_summary),
                        onClick = { /* TODO: Export data */ }
                    )
                    
                    SettingsItem(
                        title = stringResource(R.string.settings_import_data),
                        subtitle = stringResource(R.string.settings_import_data_summary),
                        onClick = { /* TODO: Import data */ }
                    )
                    
                    SettingsItem(
                        title = stringResource(R.string.settings_clear_data),
                        subtitle = stringResource(R.string.settings_clear_data_summary),
                        onClick = { /* TODO: Clear data */ }
                    )
                }
            }
            
            // Настройки уведомлений
            item {
                SettingsSection(
                    title = stringResource(R.string.settings_notifications),
                    icon = Icons.Default.Notifications
                ) {
                    SettingsItem(
                        title = stringResource(R.string.settings_notifications_enabled),
                        subtitle = stringResource(R.string.settings_notifications_enabled_summary),
                        trailing = {
                            Switch(
                                checked = uiState.notificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                            )
                        }
                    )
                    
                    SettingsItem(
                        title = stringResource(R.string.settings_notifications_sound),
                        subtitle = "Звук уведомлений",
                        trailing = {
                            Switch(
                                checked = uiState.notificationSoundEnabled,
                                onCheckedChange = { viewModel.setNotificationSoundEnabled(it) }
                            )
                        }
                    )
                    
                    SettingsItem(
                        title = stringResource(R.string.settings_notifications_vibration),
                        subtitle = "Вибрация уведомлений",
                        trailing = {
                            Switch(
                                checked = uiState.notificationVibrationEnabled,
                                onCheckedChange = { viewModel.setNotificationVibrationEnabled(it) }
                            )
                        }
                    )
                }
            }
            
            // О приложении
            item {
                SettingsSection(
                    title = stringResource(R.string.settings_about),
                    icon = Icons.Default.Info
                ) {
                    SettingsItem(
                        title = "Версия",
                        subtitle = "1.0.0",
                        onClick = onNavigateToAbout
                    )
                    
                    SettingsItem(
                        title = stringResource(R.string.settings_about_privacy),
                        subtitle = "Политика конфиденциальности",
                        onClick = onNavigateToPrivacyPolicy
                    )
                    
                    SettingsItem(
                        title = stringResource(R.string.settings_about_terms),
                        subtitle = "Условия использования",
                        onClick = onNavigateToTermsOfService
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                trailing?.invoke()
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                trailing?.invoke()
            }
        }
    }
}