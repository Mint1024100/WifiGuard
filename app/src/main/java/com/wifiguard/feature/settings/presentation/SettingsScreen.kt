package com.wifiguard.feature.settings.presentation

// РЕЗЕРВНАЯ КОПИЯ: Удаленные импорты для экспорта/импорта
// import androidx.activity.compose.rememberLauncherForActivityResult
// import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wifiguard.core.ui.theme.*
import android.widget.Toast
import com.wifiguard.core.ui.theme.calculateLuminance
import androidx.core.content.FileProvider
import com.wifiguard.core.common.DeviceDebugLogger
import java.io.File

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
    val context = LocalContext.current

    // РЕЗЕРВНАЯ КОПИЯ: Удаленный функционал экспорта/импорта данных
    // val exportLauncher = rememberLauncherForActivityResult(
    //     contract = ActivityResultContracts.CreateDocument("application/json"),
    //     onResult = { uri ->
    //         uri?.let { viewModel.exportData(it) }
    //     }
    // )
    //
    // val importLauncher = rememberLauncherForActivityResult(
    //     contract = ActivityResultContracts.GetContent(),
    //     onResult = { uri ->
    //         uri?.let { viewModel.importData(it) }
    //     }
    // )
    
    val scanIntervalDialogVisible by viewModel.scanIntervalDialogVisible.collectAsState()
    val clearDataDialogVisible by viewModel.clearDataDialogVisible.collectAsState()
    val clearDataResult by viewModel.clearDataResult.collectAsState()
    
    var themeDialogVisible by remember { mutableStateOf(false) }

    // Обработка результата очистки данных
    LaunchedEffect(clearDataResult) {
        clearDataResult?.let { result ->
            when (result) {
                is ClearDataResult.Success -> {
                    Toast.makeText(context, "Все данные удалены", Toast.LENGTH_SHORT).show()
                }
                is ClearDataResult.Error -> {
                    Toast.makeText(context, "Ошибка: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
            // Сбрасываем результат после показа
            viewModel.resetClearDataResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Настройки")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                        title = "Тема оформления",
                        subtitle = when (uiState.themeMode) {
                            "light" -> "Светлая"
                            "dark" -> "Темная"
                            else -> "Системная"
                        },
                        onClick = { themeDialogVisible = true },
                        trailing = {
                            Icon(
                                imageVector = Icons.Filled.BrightnessMedium,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    SettingsItem(
                        title = "Автоматическое сканирование",
                        subtitle = "Периодическое сканирование в фоне",
                        trailing = {
                            val isDarkTheme = MaterialTheme.colorScheme.surface.calculateLuminance() < 0.5f
                            Switch(
                                checked = uiState.autoScanEnabled,
                                onCheckedChange = { viewModel.setAutoScanEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = if (isDarkTheme) 
                                        MaterialTheme.colorScheme.secondary 
                                    else 
                                        Color(0xFF2E7D32), // Темно-зеленый для светлой темы
                                    checkedTrackColor = if (isDarkTheme) 
                                        MaterialTheme.colorScheme.secondaryContainer 
                                    else 
                                        Color(0xFF2E7D32).copy(alpha = 0.5f), // Средняя прозрачность зеленого
                                    uncheckedThumbColor = if (isDarkTheme) 
                                        MaterialTheme.colorScheme.outline 
                                    else 
                                        Color(0xFF555555), // Темно-серый для светлой темы
                                    uncheckedTrackColor = if (isDarkTheme) 
                                        MaterialTheme.colorScheme.surfaceVariant 
                                    else 
                                        Color(0xFFE0E0E0) // Светло-серый для светлой темы
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
                    
                    // РЕЗЕРВНАЯ КОПИЯ: Удаленный функционал экспорта данных
                    // SettingsItem(
                    //     title = "Экспорт данных",
                    //     subtitle = "Сохранить историю сканирований",
                    //     onClick = { exportLauncher.launch("wifiguard_backup.json") }
                    // )
                    
                    // РЕЗЕРВНАЯ КОПИЯ: Удаленный функционал импорта данных
                    // SettingsItem(
                    //     title = "Импорт данных",
                    //     subtitle = "Загрузить историю сканирований",
                    //     onClick = { importLauncher.launch("application/json") }
                    // )
                    
                    SettingsItem(
                        title = "Очистить данные",
                        subtitle = "Удалить всю историю сканирований",
                        onClick = { viewModel.showClearDataDialog() }
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
                            val isDarkTheme = MaterialTheme.colorScheme.surface.calculateLuminance() < 0.5f
                            Switch(
                                checked = uiState.notificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = if (isDarkTheme) 
                                        MaterialTheme.colorScheme.secondary 
                                    else 
                                        Color(0xFF2E7D32), // Темно-зеленый для светлой темы
                                    checkedTrackColor = if (isDarkTheme) 
                                        MaterialTheme.colorScheme.secondaryContainer 
                                    else 
                                        Color(0xFF2E7D32).copy(alpha = 0.5f), // Средняя прозрачность зеленого
                                    uncheckedThumbColor = if (isDarkTheme) 
                                        MaterialTheme.colorScheme.outline 
                                    else 
                                        Color(0xFF555555), // Темно-серый для светлой темы
                                    uncheckedTrackColor = if (isDarkTheme) 
                                        MaterialTheme.colorScheme.surfaceVariant 
                                    else 
                                        Color(0xFFE0E0E0) // Светло-серый для светлой темы
                                )
                            )
                        }
                    )
                    
                    SettingsItem(
                        title = "Звук уведомлений",
                        subtitle = "Звук уведомлений",
                        trailing = {
                            val isDarkTheme = MaterialTheme.colorScheme.surface.calculateLuminance() < 0.5f
                            Switch(
                                checked = uiState.notificationSoundEnabled,
                                onCheckedChange = { viewModel.setNotificationSoundEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = if (isDarkTheme) 
                                        MaterialTheme.colorScheme.secondary 
                                    else 
                                        Color(0xFF2E7D32), // Темно-зеленый для светлой темы
                                    checkedTrackColor = if (isDarkTheme) 
                                        MaterialTheme.colorScheme.secondaryContainer 
                                    else 
                                        Color(0xFF2E7D32).copy(alpha = 0.5f), // Средняя прозрачность зеленого
                                    uncheckedThumbColor = if (isDarkTheme) 
                                        MaterialTheme.colorScheme.outline 
                                    else 
                                        Color(0xFF555555), // Темно-серый для светлой темы
                                    uncheckedTrackColor = if (isDarkTheme) 
                                        MaterialTheme.colorScheme.surfaceVariant 
                                    else 
                                        Color(0xFFE0E0E0) // Светло-серый для светлой темы
                                )
                            )
                        }
                    )
                    
                    SettingsItem(
                        title = "Вибрация",
                        subtitle = "Вибрация уведомлений",
                        trailing = {
                            val isDarkTheme = MaterialTheme.colorScheme.surface.calculateLuminance() < 0.5f
                            Switch(
                                checked = uiState.notificationVibrationEnabled,
                                onCheckedChange = { viewModel.setNotificationVibrationEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = if (isDarkTheme) 
                                        MaterialTheme.colorScheme.secondary 
                                    else 
                                        Color(0xFF2E7D32), // Темно-зеленый для светлой темы
                                    checkedTrackColor = if (isDarkTheme) 
                                        MaterialTheme.colorScheme.secondaryContainer 
                                    else 
                                        Color(0xFF2E7D32).copy(alpha = 0.5f), // Средняя прозрачность зеленого
                                    uncheckedThumbColor = if (isDarkTheme) 
                                        MaterialTheme.colorScheme.outline 
                                    else 
                                        Color(0xFF555555), // Темно-серый для светлой темы
                                    uncheckedTrackColor = if (isDarkTheme) 
                                        MaterialTheme.colorScheme.surfaceVariant 
                                    else 
                                        Color(0xFFE0E0E0) // Светло-серый для светлой темы
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
                        title = "Экспортировать debug-лог",
                        subtitle = "Отправить файл для диагностики падений/сканирования",
                        onClick = {
                            val logFile: File = DeviceDebugLogger.getFile(context)
                            if (!logFile.exists() || logFile.length() == 0L) {
                                Toast.makeText(
                                    context,
                                    "Файл лога пуст. Сначала воспроизведите проблему.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@SettingsItem
                            }

                            runCatching {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    logFile
                                )

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/x-ndjson"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Отправить debug-лог"))
                            }.onFailure {
                                Toast.makeText(
                                    context,
                                    "Не удалось отправить debug-лог: ${it.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )

                    SettingsItem(
                        title = "Очистить debug-лог",
                        subtitle = "Сбросить файл перед новым тестом",
                        onClick = {
                            DeviceDebugLogger.clear(context)
                            Toast.makeText(
                                context,
                                "Debug-лог очищен: ${DeviceDebugLogger.filePathForUser(context)}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )

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

    // Show clear data confirmation dialog
    if (clearDataDialogVisible) {
        ClearDataConfirmationDialog(
            onDismiss = { viewModel.hideClearDataDialog() },
            onConfirm = {
                viewModel.clearAllData()
            }
        )
    }

    if (themeDialogVisible) {
        ThemeSelectionDialog(
            currentTheme = uiState.themeMode,
            onDismiss = { themeDialogVisible = false },
            onConfirm = { theme: String ->
                viewModel.setThemeMode(theme)
                themeDialogVisible = false
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            // Spacer removed as verticalArrangement handles it
            // Spacer(modifier = Modifier.height(16.dp))

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
    // Прозрачный фон - элементы сидят прямо на фоне секции
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick.invoke() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 0.dp, vertical = 12.dp),
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

        Spacer(modifier = Modifier.width(12.dp))

        trailing?.invoke()
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

/**
 * Dialog for confirming data deletion
 */
@Composable
fun ClearDataConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Подтверждение удаления",
                color = MaterialTheme.colorScheme.error
            ) 
        },
        text = {
            Column {
                Text(
                    text = "Вы уверены, что хотите удалить все данные?",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Это действие удалит:\n" +
                            "• Все угрозы безопасности\n" +
                            "• Все результаты сканирований\n" +
                            "• Все сохраненные сети\n" +
                            "• Все сессии сканирования",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Это действие нельзя отменить!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Удалить")
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

/**
 * Dialog for selecting theme
 */
@Composable
fun ThemeSelectionDialog(
    currentTheme: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val themes = mapOf(
        "system" to "Системная",
        "light" to "Светлая",
        "dark" to "Темная"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Тема оформления") },
        text = {
            Column {
                themes.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(mode) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == mode,
                            onClick = { onConfirm(mode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}