package com.wifiguard.feature.settings.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import com.wifiguard.core.ui.theme.*

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
    
    val scanIntervalDialogVisible by viewModel.scanIntervalDialogVisible.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Настройки")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp) // Increased spacing between sections
        ) {
            // Общие настройки
            item {
                SettingsSection(
                    title = "Общие",
                    icon = Icons.Filled.Settings
                ) {
                    SettingsItem(
                        title = "Автоматическое сканирование",
                        subtitle = "Периодическое сканирование в фоне",
                        trailing = {
                            Switch(
                                checked = uiState.autoScanEnabled,
                                onCheckedChange = { viewModel.setAutoScanEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.secondary,
                                    checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    )

                    SettingsItem(
                        title = "Интервал сканирования",
                        subtitle = "Частота автоматического сканирования: ${
                            when (uiState.scanIntervalMinutes) {
                                15 -> "15 минут"
                                30 -> "30 минут"
                                60 -> "60 минут"
                                120 -> "2 часа"
                                else -> "${uiState.scanIntervalMinutes} минут"
                            }
                        }",
                        onClick = { viewModel.showScanIntervalDialog() }
                    )
                }
            }
            
            // Настройки безопасности
            item {
                SettingsSection(
                    title = "Безопасность",
                    icon = Icons.Filled.Security
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
                    icon = Icons.Filled.Notifications
                ) {
                    SettingsItem(
                        title = "Уведомления об угрозах",
                        subtitle = "Получать уведомления о критических угрозах",
                        trailing = {
                            Switch(
                                checked = uiState.notificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.secondary,
                                    checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    )
                    
                    SettingsItem(
                        title = "Звук уведомлений",
                        subtitle = "Звук уведомлений",
                        trailing = {
                            Switch(
                                checked = uiState.notificationSoundEnabled,
                                onCheckedChange = { viewModel.setNotificationSoundEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.secondary,
                                    checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    )
                    
                    SettingsItem(
                        title = "Вибрация",
                        subtitle = "Вибрация уведомлений",
                        trailing = {
                            Switch(
                                checked = uiState.notificationVibrationEnabled,
                                onCheckedChange = { viewModel.setNotificationVibrationEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.secondary,
                                    checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    )
                }
            }
            
            // О приложении
            item {
                SettingsSection(
                    title = "О приложении",
                    icon = Icons.Filled.Info
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

    // Show scan interval dialog if requested
    if (scanIntervalDialogVisible) {
        ScanIntervalDialog(
            currentInterval = uiState.scanIntervalMinutes,
            onDismiss = { viewModel.hideScanIntervalDialog() },
            onConfirm = { interval ->
                viewModel.setScanInterval(interval)
                viewModel.hideScanIntervalDialog()
            }
        )
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
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = WifiGuardElevation.Level2)
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
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
    val cardColors = if (onClick != null) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    Card(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        elevation = CardDefaults.cardElevation(
            defaultElevation = WifiGuardElevation.Level1,
            pressedElevation = WifiGuardElevation.Level2,
            hoveredElevation = WifiGuardElevation.Level2
        ),
        colors = cardColors
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp), // Increased vertical padding
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Add spacing between text and trailing element
            Spacer(modifier = Modifier.width(12.dp))

            trailing?.invoke()
        }
    }
}

/**
 * Dialog for selecting scan interval
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanIntervalDialog(
    currentInterval: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val scanIntervals = mapOf(
        15 to "15 минут",
        30 to "30 минут",
        60 to "60 минут",
        120 to "2 часа"
    )

    var selectedInterval by remember { mutableIntStateOf(currentInterval) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Интервал сканирования") },
        text = {
            LazyColumn {
                scanIntervals.forEach { (minutes, label) ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedInterval = minutes }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedInterval == minutes,
                                onClick = { selectedInterval = minutes }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedInterval) }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Отмена")
            }
        }
    )
}