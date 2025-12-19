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
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wifiguard.core.ui.theme.*
import android.widget.Toast
import android.util.Log
import com.wifiguard.core.ui.theme.calculateLuminance
import androidx.core.content.FileProvider
import com.wifiguard.core.common.DeviceDebugLogger
import com.wifiguard.core.common.Constants
import com.wifiguard.BuildConfig
import java.io.File
import com.wifiguard.core.ui.testing.UiTestTags
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
    // ИСПРАВЛЕНО: Защита от множественных кликов на экспорт логов
    var isExporting by remember { mutableStateOf(false) }
    var lastExportClickTime by remember { mutableStateOf(0L) }
    val EXPORT_DEBOUNCE_MS = 2000L // 2 секунды между кликами
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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
    val dataRetentionDialogVisible by viewModel.dataRetentionDialogVisible.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var themeDialogVisible by remember { mutableStateOf(false) }

    // ИСПРАВЛЕНО: Получаем строковые ресурсы ДО LaunchedEffect, чтобы избежать предупреждения о LocalContext.current
    val successMessage = stringResource(R.string.settings_all_data_deleted)
    val errorPrefixFormat = stringResource(R.string.settings_error_prefix)

    // ИСПРАВЛЕНО: Безопасная обработка результата очистки данных
    // Проверяем жизненный цикл перед показом Toast
    LaunchedEffect(clearDataResult) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            clearDataResult?.let { result ->
                when (result) {
                    is ClearDataResult.Success -> {
                        Toast.makeText(
                            context,
                            successMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    is ClearDataResult.Error -> {
                        // Используем String.format() для форматирования строки с параметром
                        // Это избегает предупреждения о LocalContext.current внутри LaunchedEffect
                        // Строка ресурса уже получена через stringResource() выше
                        val errorMessage = errorPrefixFormat.replace("%s", result.message ?: "")
                        Toast.makeText(
                            context,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                // Сбрасываем результат после показа
                viewModel.resetClearDataResult()
            }
        }
    }

    // ИСПРАВЛЕНО: Безопасная обработка ошибок сохранения настроек
    LaunchedEffect(errorMessage) {
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            errorMessage?.let { message ->
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    Scaffold(
        modifier = Modifier.testTag(UiTestTags.SETTINGS_SCREEN),
        topBar = {
            TopAppBar(
                modifier = Modifier.testTag(UiTestTags.SETTINGS_TOP_APP_BAR),
                title = {
                    Text(stringResource(R.string.settings_title))
                },
                navigationIcon = {
                    IconButton(
                        modifier = Modifier.testTag(UiTestTags.SETTINGS_NAV_BACK),
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp) // Increased spacing between sections
        ) {
            // Общие настройки
            item {
                SettingsSection(
                    title = stringResource(R.string.settings_general),
                    icon = Icons.Filled.Settings
                ) {
                    SettingsItem(
                        title = stringResource(R.string.settings_theme),
                        subtitle = when (uiState.themeMode) {
                            "light" -> stringResource(R.string.theme_light)
                            "dark" -> stringResource(R.string.theme_dark)
                            else -> stringResource(R.string.theme_system)
                        },
                        onClick = { themeDialogVisible = true },
                        modifier = Modifier.testTag(UiTestTags.SETTINGS_ITEM_THEME),
                        trailing = {
                            Icon(
                                imageVector = Icons.Filled.BrightnessMedium,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )

                    SettingsItem(
                        title = stringResource(R.string.settings_auto_scan),
                        subtitle = stringResource(R.string.settings_auto_scan_summary),
                        trailing = {
                            SettingsSwitch(
                                checked = uiState.autoScanEnabled,
                                onCheckedChange = { viewModel.setAutoScanEnabled(it) }
                            )
                        }
                    )

                    SettingsItem(
                        title = stringResource(R.string.settings_scan_interval),
                        subtitle = "Частота автоматического сканирования: ${
                            when (uiState.scanIntervalMinutes) {
                                15 -> stringResource(R.string.settings_scan_interval_15min)
                                30 -> stringResource(R.string.settings_scan_interval_30min)
                                60 -> stringResource(R.string.settings_scan_interval_1hour)
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
                    title = stringResource(R.string.settings_security),
                    icon = Icons.Filled.Security
                ) {
                    SettingsItem(
                        title = stringResource(R.string.settings_data_retention),
                        subtitle = "Период хранения истории сканирований: ${
                            when (uiState.dataRetentionDays) {
                                1 -> stringResource(R.string.settings_data_retention_1day)
                                7 -> stringResource(R.string.settings_data_retention_1week)
                                30 -> stringResource(R.string.settings_data_retention_1month)
                                90 -> stringResource(R.string.settings_data_retention_3months)
                                -1 -> stringResource(R.string.settings_data_retention_forever)
                                else -> "${uiState.dataRetentionDays} дней"
                            }
                        }",
                        onClick = { 
                            viewModel.showDataRetentionDialog() 
                        }
                    )

                    SettingsItem(
                        title = stringResource(R.string.settings_auto_disable_wifi_on_critical_title),
                        subtitle = stringResource(R.string.settings_auto_disable_wifi_on_critical_summary),
                        trailing = {
                            SettingsSwitch(
                                checked = uiState.autoDisableWifiOnCritical,
                                onCheckedChange = { viewModel.setAutoDisableWifiOnCritical(it) }
                            )
                        }
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
                        title = stringResource(R.string.settings_clear_data),
                        subtitle = stringResource(R.string.settings_clear_data_summary),
                        onClick = { viewModel.showClearDataDialog() }
                    )
                }
            }
            
            // Настройки уведомлений
            item {
                SettingsSection(
                    title = stringResource(R.string.settings_notifications),
                    icon = Icons.Filled.Notifications
                ) {
                    SettingsItem(
                        title = stringResource(R.string.settings_notifications_enabled),
                        subtitle = stringResource(R.string.settings_notifications_enabled_summary),
                        trailing = {
                            SettingsSwitch(
                                checked = uiState.notificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                            )
                        }
                    )
                    
                    SettingsItem(
                        title = stringResource(R.string.settings_notifications_sound),
                        subtitle = stringResource(R.string.settings_notifications_sound),
                        trailing = {
                            SettingsSwitch(
                                checked = uiState.notificationSoundEnabled,
                                onCheckedChange = { viewModel.setNotificationSoundEnabled(it) }
                            )
                        }
                    )
                    
                    SettingsItem(
                        title = stringResource(R.string.settings_notifications_vibration),
                        subtitle = stringResource(R.string.settings_notifications_vibration),
                        trailing = {
                            SettingsSwitch(
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
                    icon = Icons.Filled.Info
                ) {
                    SettingsItem(
                        title = "Экспортировать debug-лог",
                        subtitle = "Отправить файл для диагностики падений/сканирования",
                        onClick = {
                            // ИСПРАВЛЕНО: Защита от множественных кликов
                            val currentTime = System.currentTimeMillis()
                            if (isExporting || (currentTime - lastExportClickTime) < EXPORT_DEBOUNCE_MS) {
                                // #region agent log
                                Log.d("SettingsScreen", "Экспорт пропущен: isExporting=$isExporting, timeSinceLastClick=${currentTime - lastExportClickTime}ms")
                                // #endregion
                                return@SettingsItem
                            }
                            lastExportClickTime = currentTime
                            isExporting = true
                            
                            // ИСПРАВЛЕНО: Безопасная проверка контекста
                            val activity = context as? android.app.Activity
                            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                                isExporting = false
                                return@SettingsItem
                            }
                            
                            val logFile: File = DeviceDebugLogger.getFile(context)
                            
                            // #region agent log
                            Log.d("SettingsScreen", "Попытка экспорта лог-файла: path=${logFile.absolutePath}, exists=${logFile.exists()}, size=${logFile.length()}, canRead=${logFile.canRead()}")
                            // #endregion
                            
                            // ИСПРАВЛЕНО: Проверка существования и доступности файла
                            if (!logFile.exists()) {
                                // #region agent log
                                Log.w("SettingsScreen", "Файл лога не существует: ${logFile.absolutePath}, создаем тестовую запись")
                                // #endregion
                                
                                // ИСПРАВЛЕНО: Создаем тестовую запись, если файл не существует
                                try {
                                    val runId = DeviceDebugLogger.currentRunId()
                                    DeviceDebugLogger.log(
                                        context = context,
                                        runId = runId,
                                        hypothesisId = "MANUAL",
                                        location = "SettingsScreen.kt:onClick",
                                        message = "Тестовая запись для создания файла лога",
                                        data = org.json.JSONObject().apply {
                                            put("action", "manual_export_trigger")
                                            put("timestamp", System.currentTimeMillis())
                                        }
                                    )
                                    // #region agent log
                                    Log.d("SettingsScreen", "Тестовая запись создана, проверяем файл снова")
                                    // #endregion
                                    
                                    // Проверяем еще раз после создания
                                    if (!logFile.exists()) {
                                        isExporting = false
                                        Toast.makeText(
                                            context,
                                            "Не удалось создать файл лога. Проверьте разрешения на запись.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@SettingsItem
                                    }
                                } catch (e: Exception) {
                                    // #region agent log
                                    Log.e("SettingsScreen", "Ошибка при создании тестовой записи: ${e.message}", e)
                                    // #endregion
                                    isExporting = false
                                    Toast.makeText(
                                        context,
                                        "Не удалось создать файл лога: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@SettingsItem
                                }
                            }
                            
                            if (logFile.length() == 0L) {
                                // #region agent log
                                Log.w("SettingsScreen", "Файл лога пуст: ${logFile.absolutePath}")
                                // #endregion
                                isExporting = false
                                Toast.makeText(
                                    context,
                                    "Файл лога пуст. Сначала воспроизведите проблему.",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@SettingsItem
                            }
                            
                            // Проверка доступности файла для чтения
                            if (!logFile.canRead()) {
                                // #region agent log
                                Log.e("SettingsScreen", "Нет доступа для чтения файла: ${logFile.absolutePath}")
                                // #endregion
                                isExporting = false
                                Toast.makeText(
                                    context,
                                    "Нет доступа для чтения файла лога",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@SettingsItem
                            }

                            runCatching {
                                val authority = "${context.packageName}.fileprovider"
                                // #region agent log
                                Log.d("SettingsScreen", "Создание URI через FileProvider: authority=$authority, file=${logFile.absolutePath}, parent=${logFile.parent}, externalFilesDir=${context.getExternalFilesDir(null)?.absolutePath}, filesDir=${context.filesDir.absolutePath}")
                                // #endregion
                                
                                // ИСПРАВЛЕНО: Проверяем, что файл находится в доступной директории
                                val externalFilesDir = context.getExternalFilesDir(null)
                                val filesDir = context.filesDir
                                
                                // Нормализуем пути для корректного сравнения
                                val filePath = logFile.absolutePath
                                val externalPath = externalFilesDir?.absolutePath?.let { 
                                    if (!it.endsWith("/")) "$it/" else it 
                                } ?: ""
                                val internalPath = filesDir.absolutePath.let { 
                                    if (!it.endsWith("/")) "$it/" else it 
                                }
                                
                                val isInExternalFiles = externalFilesDir != null && filePath.startsWith(externalPath)
                                val isInInternalFiles = filePath.startsWith(internalPath)
                                
                                // #region agent log
                                Log.d("SettingsScreen", "Проверка пути файла: isInExternalFiles=$isInExternalFiles, isInInternalFiles=$isInInternalFiles")
                                Log.d("SettingsScreen", "Детали путей: filePath=$filePath, externalPath=$externalPath, internalPath=$internalPath")
                                // #endregion
                                
                                if (!isInExternalFiles && !isInInternalFiles) {
                                    val errorMsg = "Файл находится вне доступных директорий для FileProvider. Файл: $filePath, External: $externalPath, Internal: $internalPath"
                                    // #region agent log
                                    Log.e("SettingsScreen", errorMsg)
                                    // #endregion
                                    throw IllegalStateException(errorMsg)
                                }
                                
                                // minSdk = 26, поэтому используем FileProvider всегда (без ветвлений по версии).
                                // #region agent log
                                Log.d("SettingsScreen", "Вызов FileProvider.getUriForFile с authority=$authority")
                                // #endregion
                                
                                val uri = try {
                                    FileProvider.getUriForFile(
                                        context,
                                        authority,
                                        logFile
                                    )
                                } catch (e: IllegalArgumentException) {
                                    // #region agent log
                                    Log.e("SettingsScreen", "IllegalArgumentException при создании URI: ${e.message}", e)
                                    Log.e("SettingsScreen", "Детали: authority=$authority, file=${logFile.absolutePath}, fileExists=${logFile.exists()}, fileCanRead=${logFile.canRead()}")
                                    // #endregion
                                    throw e
                                } catch (e: Exception) {
                                    // #region agent log
                                    Log.e("SettingsScreen", "Неожиданная ошибка при создании URI: ${e.javaClass.simpleName}: ${e.message}", e)
                                    // #endregion
                                    throw e
                                }
                                
                                // #region agent log
                                Log.d("SettingsScreen", "URI создан успешно: $uri")
                                // #endregion

                                // ИСПРАВЛЕНО: Создаем Intent с правильными флагами
                                // Используем ClipData для автоматической выдачи разрешений на URI
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/x-ndjson"
                                    // ИСПРАВЛЕНО: Используем ClipData для правильной передачи URI с разрешениями
                                    // Это предотвращает SecurityException при попытке системы предпросмотреть файл
                                    val clipData = android.content.ClipData.newUri(
                                        context.contentResolver,
                                        "Debug Log",
                                        uri
                                    )
                                    setClipData(clipData)
                                    // Оставляем EXTRA_STREAM для обратной совместимости
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                
                                val packageManager = context.packageManager
                                
                                // ИСПРАВЛЕНО: Используем правильные флаги для queryIntentActivities
                                // На Android 11+ нужно использовать MATCH_DEFAULT_ONLY или MATCH_ALL
                                val queryFlags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                    android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                                } else {
                                    0
                                }
                                
                                val resolveInfos = try {
                                    packageManager.queryIntentActivities(shareIntent, queryFlags)
                                } catch (e: Exception) {
                                    // #region agent log
                                    Log.e("SettingsScreen", "Ошибка при queryIntentActivities: ${e.message}", e)
                                    // #endregion
                                    emptyList()
                                }
                                
                                // #region agent log
                                Log.d("SettingsScreen", "Найдено приложений для отправки: ${resolveInfos.size}, queryFlags=$queryFlags")
                                // #endregion
                                
                                // ИСПРАВЛЕНО: Безопасная проверка перед запуском Activity
                                val chooser = Intent.createChooser(shareIntent, "Отправить debug-лог")
                                
                                @Suppress("QueryPermissionsNeeded")
                                val resolveInfo = chooser.resolveActivity(packageManager)
                                
                                // #region agent log
                                Log.d("SettingsScreen", "ResolveInfo для chooser: $resolveInfo, activity.isFinishing=${activity.isFinishing}, activity.isDestroyed=${activity.isDestroyed}")
                                // #endregion
                                
                                if (resolveInfo != null && !activity.isFinishing && !activity.isDestroyed) {
                                    // #region agent log
                                    Log.d("SettingsScreen", "Запуск Activity для отправки файла, URI=$uri")
                                    // #endregion
                                    try {
                                        // ИСПРАВЛЕНО: Используем activity.startActivity вместо context.startActivity
                                        // для правильной работы с lifecycle
                                        activity.startActivity(chooser)
                                        // #region agent log
                                        Log.d("SettingsScreen", "Activity запущена успешно")
                                        // #endregion
                                        // ИСПРАВЛЕНО: Сбрасываем флаг после успешного запуска
                                        isExporting = false
                                    } catch (e: android.content.ActivityNotFoundException) {
                                        // #region agent log
                                        Log.e("SettingsScreen", "ActivityNotFoundException: ${e.message}", e)
                                        // #endregion
                                        isExporting = false
                                        if (!isExporting) {
                                            Toast.makeText(
                                                context,
                                                "Не найдено приложение для отправки файла",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } catch (e: SecurityException) {
                                        // #region agent log
                                        Log.e("SettingsScreen", "SecurityException при запуске Activity: ${e.message}", e)
                                        // #endregion
                                        isExporting = false
                                        if (!isExporting) {
                                            Toast.makeText(
                                                context,
                                                "Нет разрешения на отправку файла: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        // #region agent log
                                        Log.e("SettingsScreen", "Ошибка при запуске Activity: ${e.javaClass.simpleName}: ${e.message}", e)
                                        // #endregion
                                        isExporting = false
                                        if (!isExporting) {
                                            Toast.makeText(
                                                context,
                                                "Не удалось открыть диалог отправки: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                } else {
                                    // #region agent log
                                    Log.w("SettingsScreen", "Не удалось запустить Activity: resolveInfo=$resolveInfo, isFinishing=${activity.isFinishing}, isDestroyed=${activity.isDestroyed}, resolveInfos.size=${resolveInfos.size}")
                                    // #endregion
                                    isExporting = false
                                    if (!isExporting) {
                                        Toast.makeText(
                                            context,
                                            "Нет приложения для отправки файла. Установите приложение для отправки файлов (например, Gmail, Telegram).",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }.onFailure { e ->
                                // #region agent log
                                Log.e("SettingsScreen", "Ошибка при отправке debug-лога: ${e.javaClass.simpleName}: ${e.message}", e)
                                Log.e("SettingsScreen", "StackTrace: ${e.stackTraceToString()}")
                                // #endregion
                                
                                val errorMessage = when (e) {
                                    is IllegalArgumentException -> "Ошибка конфигурации FileProvider. Проверьте file_paths.xml"
                                    is IllegalStateException -> e.message ?: "Файл находится в недоступной директории"
                                    is SecurityException -> "Нет разрешения на доступ к файлу"
                                    else -> "Не удалось отправить debug-лог: ${e.message}"
                                }
                                
                                // ИСПРАВЛЕНО: Показываем Toast только если не показывается другой
                                if (!isExporting) {
                                    Toast.makeText(
                                        context,
                                        errorMessage,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }.also {
                                // ИСПРАВЛЕНО: Сбрасываем флаг экспорта после завершения операции
                                isExporting = false
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
                        subtitle = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
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
            onDismiss = { 
                themeDialogVisible = false 
            },
            onConfirm = { theme ->
                viewModel.setThemeMode(theme)
                themeDialogVisible = false
            }
        )
    }

    // Show data retention dialog if requested
    if (dataRetentionDialogVisible) {
        DataRetentionDialog(
            currentRetentionDays = uiState.dataRetentionDays,
            onDismiss = { viewModel.hideDataRetentionDialog() },
            onConfirm = { days ->
                viewModel.setDataRetentionDays(days)
                viewModel.hideDataRetentionDialog()
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

/**
 * ИСПРАВЛЕНО: Переиспользуемый Switch компонент для настроек
 * Устраняет дублирование кода и обеспечивает единообразный стиль
 */
@Composable
private fun SettingsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.calculateLuminance() < 0.5f
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
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

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    // Прозрачный фон - элементы сидят прямо на фоне секции
    Row(
        modifier = modifier
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

    // ИСПРАВЛЕНО: Обновляем selectedInterval при изменении currentInterval
    var selectedInterval by remember(currentInterval) { mutableIntStateOf(currentInterval) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_scan_interval)) },
        text = {
            Column {
                // ИСПРАВЛЕНО: Добавлено предупреждение о минимальном интервале
                Text(
                    text = "Минимальный интервал: ${Constants.MIN_SCAN_INTERVAL_MINUTES} минут (требование Android WorkManager)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
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
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    // Валидация: убеждаемся, что интервал не меньше минимального
                    val validInterval = maxOf(Constants.MIN_SCAN_INTERVAL_MINUTES, selectedInterval)
                    onConfirm(validInterval)
                }
            ) {
                Text(stringResource(R.string.common_ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.common_cancel))
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
        "system" to stringResource(R.string.theme_system),
        "light" to stringResource(R.string.theme_light),
        "dark" to stringResource(R.string.theme_dark)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(UiTestTags.SETTINGS_THEME_DIALOG),
        title = { Text(stringResource(R.string.settings_theme)) },
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
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

/**
 * Dialog for selecting data retention period
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataRetentionDialog(
    currentRetentionDays: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val retentionOptions = mapOf(
        1 to "1 день",
        7 to "1 неделя",
        30 to "1 месяц",
        90 to "3 месяца",
        -1 to "Навсегда"
    )

    // ИСПРАВЛЕНО: Обновляем selectedDays при изменении currentRetentionDays
    var selectedDays by remember(currentRetentionDays) { mutableIntStateOf(currentRetentionDays) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_data_retention)) },
        text = {
            LazyColumn {
                retentionOptions.forEach { (days, label) ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDays = days }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedDays == days,
                                onClick = { selectedDays = days }
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
                onClick = { onConfirm(selectedDays) }
            ) {
                Text(stringResource(R.string.common_ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}