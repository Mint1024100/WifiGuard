package com.wifiguard.feature.settings.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let { viewModel.exportData(it) }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { viewModel.importData(it) }
        }
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Настройки")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
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
                    title = "Общие",
                    icon = Icons.Default.Settings
                ) {
                    SettingsItem(
                        title = "Автоматическое сканирование",
                        subtitle = "Периодическое сканирование в фоне",
                        trailing = {
                            Switch(
                                checked = uiState.autoScanEnabled,
                                onCheckedChange = { viewModel.setAutoScanEnabled(it) }
                            )
                        }
                    )
                    
                    SettingsItem(
                        title = "Интервал сканирования",
                        subtitle = "Частота автоматического сканирования",
                        onClick = { /* TODO: Show interval picker */ }
                    )
                }
            }
            
            // Настройки безопасности
            item {
                SettingsSection(
                    title = "Безопасность",
                    icon = Icons.Default.Security
                ) {
                    SettingsItem(
                        title = "Хранение данных",
                        subtitle = "Период хранения истории сканирований",
                        onClick = { /* TODO: Show retention picker */ }
                    )
                    
                    SettingsItem(
                        title = "Экспорт данных",
                        subtitle = "Сохранить историю сканирований",
                        onClick = { exportLauncher.launch("wifiguard_backup.json") }
                    )
                    
                    SettingsItem(
                        title = "Импорт данных",
                        subtitle = "Загрузить историю сканирований",
                        onClick = { importLauncher.launch("application/json") }
                    )
                    
                    SettingsItem(
                        title = "Очистить данные",
                        subtitle = "Удалить всю историю сканирований",
                        onClick = { viewModel.clearAllData() }
                    )
                }
            }
            
            // Настройки уведомлений
            item {
                SettingsSection(
                    title = "Уведомления",
                    icon = Icons.Default.Notifications
                ) {
                    SettingsItem(
                        title = "Уведомления об угрозах",
                        subtitle = "Получать уведомления о критических угрозах",
                        trailing = {
                            Switch(
                                checked = uiState.notificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                            )
                        }
                    )
                    
                    SettingsItem(
                        title = "Звук уведомлений",
                        subtitle = "Звук уведомлений",
                        trailing = {
                            Switch(
                                checked = uiState.notificationSoundEnabled,
                                onCheckedChange = { viewModel.setNotificationSoundEnabled(it) }
                            )
                        }
                    )
                    
                    SettingsItem(
                        title = "Вибрация",
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
                    title = "О приложении",
                    icon = Icons.Default.Info
                ) {
                    SettingsItem(
                        title = "Версия",
                        subtitle = "1.0.0",
                        onClick = onNavigateToAbout
                    )
                    
                    SettingsItem(
                        title = "Политика конфиденциальности",
                        subtitle = "Политика конфиденциальности",
                        onClick = onNavigateToPrivacyPolicy
                    )
                    
                    SettingsItem(
                        title = "Условия использования",
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