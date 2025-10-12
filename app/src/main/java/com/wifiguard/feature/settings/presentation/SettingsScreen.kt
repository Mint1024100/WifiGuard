package com.wifiguard.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Экран настроек приложения
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    var autoScanEnabled by remember { mutableStateOf(true) }
    var backgroundMonitoring by remember { mutableStateOf(true) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var highPriorityNotifications by remember { mutableStateOf(false) }
    var scanInterval by remember { mutableStateOf(15f) }
    var threatSensitivity by remember { mutableStateOf(1f) } // 0=Low, 1=Medium, 2=High
    
    var showClearDataDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
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
            // Scanning Settings
            item {
                SettingsSectionHeader("Сканирование")
            }
            
            item {
                SettingsSwitch(
                    title = "Автоматическое сканирование",
                    description = "Периодически сканировать Wi-Fi сети",
                    checked = autoScanEnabled,
                    onCheckedChange = { autoScanEnabled = it },
                    icon = Icons.Default.Autorenew
                )
            }
            
            item {
                SettingsSwitch(
                    title = "Фоновый мониторинг",
                    description = "Сканировать сети в фоновом режиме",
                    checked = backgroundMonitoring,
                    onCheckedChange = { backgroundMonitoring = it },
                    icon = Icons.Default.CloudQueue
                )
            }
            
            item {
                SettingsSlider(
                    title = "Интервал сканирования",
                    description = "${scanInterval.toInt()} минут",
                    value = scanInterval,
                    onValueChange = { scanInterval = it },
                    valueRange = 5f..60f,
                    steps = 10,
                    icon = Icons.Default.Timer
                )
            }
            
            // Notifications Settings
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader("Уведомления")
            }
            
            item {
                SettingsSwitch(
                    title = "Включить уведомления",
                    description = "Получать уведомления об угрозах",
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                    icon = Icons.Default.Notifications
                )
            }
            
            item {
                SettingsSwitch(
                    title = "Высокий приоритет",
                    description = "Показывать уведомления поверх других",
                    checked = highPriorityNotifications,
                    onCheckedChange = { highPriorityNotifications = it },
                    enabled = notificationsEnabled,
                    icon = Icons.Default.NotificationImportant
                )
            }
            
            // Security Settings
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader("Безопасность")
            }
            
            item {
                SettingsSlider(
                    title = "Чувствительность обнаружения",
                    description = when (threatSensitivity.toInt()) {
                        0 -> "Низкая - меньше ложных срабатываний"
                        1 -> "Средняя - баланс между точностью и охватом"
                        else -> "Высокая - максимальная защита"
                    },
                    value = threatSensitivity,
                    onValueChange = { threatSensitivity = it },
                    valueRange = 0f..2f,
                    steps = 1,
                    icon = Icons.Default.Security
                )
            }
            
            // Data Management
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader("Управление данными")
            }
            
            item {
                SettingsItem(
                    title = "Очистить историю",
                    description = "Удалить все сохраненные результаты сканирования",
                    icon = Icons.Default.DeleteSweep,
                    onClick = { showClearDataDialog = true }
                )
            }
            
            item {
                SettingsItem(
                    title = "Экспорт данных",
                    description = "Экспортировать данные в файл",
                    icon = Icons.Default.Download,
                    onClick = { /* TODO: Implement export */ }
                )
            }
            
            // About
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader("О приложении")
            }
            
            item {
                SettingsItem(
                    title = "Версия",
                    description = "1.0.0",
                    icon = Icons.Default.Info,
                    onClick = {}
                )
            }
            
            item {
                SettingsItem(
                    title = "Лицензии",
                    description = "Открытые лицензии и авторские права",
                    icon = Icons.Default.Description,
                    onClick = { /* TODO: Show licenses */ }
                )
            }
        }
    }
    
    // Clear data confirmation dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("Очистить историю?") },
            text = { Text("Это действие удалит все сохраненные результаты сканирования. Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // TODO: Clear data
                        showClearDataDialog = false
                    }
                ) {
                    Text("Очистить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun SettingsSlider(
    title: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps
            )
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


