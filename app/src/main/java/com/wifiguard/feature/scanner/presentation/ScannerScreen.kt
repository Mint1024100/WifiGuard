package com.wifiguard.feature.scanner.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wifiguard.core.common.BssidValidator
import com.wifiguard.core.common.PermissionHandler
import com.wifiguard.core.common.Result
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.ui.components.NetworkCard
import com.wifiguard.core.ui.components.NetworkCardDetails
import com.wifiguard.core.ui.components.NetworkDetailsModal
import com.wifiguard.core.ui.components.PermissionRationaleDialog
import com.wifiguard.core.ui.components.SignalAnalyticsUi
import com.wifiguard.core.ui.components.StatusIndicator

/**
 * Экран сканирования Wi-Fi сетей
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onNavigateToAnalysis: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionState by viewModel.permissionState.collectAsStateWithLifecycle()
    val scanResult by viewModel.scanResult.collectAsStateWithLifecycle()
    val filteredScanResult by viewModel.filteredScanResult.collectAsStateWithLifecycle()
    val currentNetwork by viewModel.currentNetwork.collectAsStateWithLifecycle()
    val detailsViewModel: NetworkDetailsViewModel = hiltViewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showPermissionDialog by remember { mutableStateOf(false) }
    var flippedBssid by rememberSaveable { mutableStateOf<String?>(null) }

    val detailsUiState by detailsViewModel.uiState.collectAsStateWithLifecycle()
    val detailsDbNetwork by detailsViewModel.currentNetwork.collectAsStateWithLifecycle()
    val detailsHistory by detailsViewModel.networkStatistics.collectAsStateWithLifecycle()
    val detailsSignalAnalytics by detailsViewModel.signalAnalytics.collectAsStateWithLifecycle()
    val detailsBssid by detailsViewModel.loadedBssid.collectAsStateWithLifecycle()

    LaunchedEffect(flippedBssid) {
        val bssid = flippedBssid ?: return@LaunchedEffect
        // Валидация BSSID перед загрузкой деталей для предотвращения потенциальных проблем
        if (BssidValidator.isValidForStorage(bssid)) {
            detailsViewModel.loadNetworkDetails(bssid)
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        // Check if any permission was permanently denied (no rationale)
        val shouldShowRationale = permissions.entries.any { 
            !it.value && androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                context as android.app.Activity, 
                it.key
            ) 
        }
        
        viewModel.onPermissionResult(allGranted, !shouldShowRationale)
    }
    
    // Показать диалог если нужно
    showPermissionDialog = when (permissionState) {
        ScannerViewModel.PermissionState.ShouldShowRationale,
        ScannerViewModel.PermissionState.NotGranted -> true
        else -> false
    }
    
    if (showPermissionDialog) {
        PermissionRationaleDialog(
            onDismiss = { /* Do nothing */ },
            onConfirm = {
                permissionLauncher.launch(
                    viewModel.permissionHandler.getRequiredWifiPermissions()
                )
            },
            onOpenSettings = {
                viewModel.permissionHandler.openAppSettings(context)
            },
            isPermanentlyDenied = permissionState is ScannerViewModel.PermissionState.PermanentlyDenied
        )
    }

    // Обновляем состояние геолокации при возврате на экран/в приложение.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLocationState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    // Вычисляем выбранную сеть для модального окна с защитой от race condition
    val selectedNetwork by remember {
        derivedStateOf {
            val bssid = flippedBssid
            if (bssid == null) {
                null
            } else {
                // Сначала проверяем подключенную сеть
                val connectedNet = currentNetwork
                if (connectedNet?.bssid == bssid) {
                    connectedNet
                } else {
                    // Затем ищем в результатах сканирования с null-safe проверкой
                    when (val result = scanResult) {
                        is Result.Success -> result.data.find { it.bssid == bssid }
                        else -> null
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.then(
                if (selectedNetwork != null) {
                    Modifier.blur(radius = 16.dp)
                } else {
                    Modifier
                }
            ),
            topBar = {
            TopAppBar(
                title = { 
                    Text("Wi-Fi Сканер")
                },
                actions = {
                    IconButton(onClick = { 
                        // При выключенной геолокации сканирование часто недоступно на OEM-устройствах.
                        if (viewModel.hasWifiPermissions() && !uiState.isLocationEnabled) {
                            viewModel.permissionHandler.openLocationSettings()
                        } else if (viewModel.hasWifiPermissions()) {
                            viewModel.startScan()
                        } else {
                            showPermissionDialog = true
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Обновить"
                        )
                    }
                    IconButton(onClick = onNavigateToAnalysis) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Анализ"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Настройки"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Статус индикатор
            val networksCount by remember(scanResult, uiState.networks) {
                derivedStateOf {
                    val currentScanResult = scanResult
                    if (currentScanResult is Result.Success) currentScanResult.data.size else uiState.networks.size
                }
            }
            
            StatusIndicator(
                isWifiEnabled = uiState.isWifiEnabled,
                isScanning = scanResult is Result.Loading,
                networksCount = networksCount,
                lastScanTime = uiState.lastScanTime,
                scanMetadata = uiState.scanMetadata,
                modifier = Modifier.padding(16.dp)
            )
            
            // Кнопка для foreground сканирования, если данные устарели
            val scanMetadata = uiState.scanMetadata
            if (scanMetadata != null && 
                (scanMetadata.freshness == com.wifiguard.core.domain.model.Freshness.STALE || 
                 scanMetadata.freshness == com.wifiguard.core.domain.model.Freshness.EXPIRED)) {
                
                Button(
                    onClick = { viewModel.startForegroundScan(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выполнить полное сканирование")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Отображение текущей подключенной сети
            currentNetwork?.let { network ->
                // Показывать только если есть подключенная сеть и WiFi включен
                if (network.isConnected && uiState.isWifiEnabled) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Подключенная сеть",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Отображение информации о текущей сети
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = network.ssid.ifEmpty { "Скрытая сеть" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Сигнал и безопасность
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Сигнал: ${network.getSignalStrengthDescription()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Text(
                                    text = getSecurityTypeText(network.securityType),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Частота и канал
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Частота: ${network.frequency} МГц",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                if (network.channel > 0) {
                                    Text(
                                        text = "Канал: ${network.channel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Основной контент
            val currentScanResult = scanResult
            
            // Подготовка данных для модального окна (вынесено выше для доступности)
            val detailsAnalyticsUi = remember(detailsSignalAnalytics) {
                detailsSignalAnalytics?.let {
                    SignalAnalyticsUi(
                        averageSignal = it.averageSignal,
                        minSignal = it.minSignal,
                        maxSignal = it.maxSignal,
                        scansCount = it.scansCount,
                        lastScanTime = it.lastScanTime,
                        signalVariation = it.signalVariation
                    )
                }
            }

            val activeDetails = remember(
                detailsUiState,
                detailsDbNetwork,
                detailsHistory,
                detailsAnalyticsUi
            ) {
                NetworkCardDetails(
                    dbNetwork = detailsDbNetwork,
                    scanHistory = detailsHistory.take(50),
                    signalAnalytics = detailsAnalyticsUi,
                    isLoading = detailsUiState.isLoading,
                    errorMessage = detailsUiState.errorMessage
                )
            }
            
            // Если WiFi выключен, показываем EmptyContent с сообщением об отключении
            if (!uiState.isWifiEnabled) {
                EmptyContent(
                    isWifiEnabled = false,
                    onStartScan = { 
                        if (viewModel.hasWifiPermissions()) {
                            viewModel.startScan()
                        } else {
                            showPermissionDialog = true
                        }
                    }
                )
            } else if (viewModel.hasWifiPermissions() && 
                       !uiState.isLocationEnabled && 
                       viewModel.permissionHandler.isLocationRequiredForWifiScan()) {
                // Отдельный сценарий: геолокация выключена, сканирование/scanResults могут быть недоступны.
                // На Android 13+ с NEARBY_WIFI_DEVICES это не требуется
                LocationDisabledContent(
                    onOpenLocationSettings = {
                        viewModel.permissionHandler.openLocationSettings()
                    }
                )
            } else {
                // Используем отфильтрованные результаты из ViewModel для оптимизации
                when (val result = filteredScanResult) {
                    is Result.Loading -> {
                        ScanningContent()
                    }
                    is Result.Success -> {
                        if (result.data.isEmpty()) {
                            EmptyContent(
                                isWifiEnabled = uiState.isWifiEnabled,
                                onStartScan = { 
                                    if (viewModel.hasWifiPermissions()) {
                                        viewModel.startScan()
                                    } else {
                                        showPermissionDialog = true
                                    }
                                }
                            )
                        } else {
                            NetworksList(
                                networks = result.data,
                                onNetworkClick = { network ->
                                    flippedBssid = network.bssid
                                },
                                currentConnectedBssid = currentNetwork?.bssid,
                                isRefreshing = uiState.isScanning,
                                onRefresh = {
                                    // #region agent log
                                    android.util.Log.d("PullToRefresh", "onRefresh вызван, isScanning=${uiState.isScanning}")
                                    try {
                                        val logFile = java.io.File("/Users/mint1024/Desktop/андроид/.cursor/debug.log")
                                        val logEntry = org.json.JSONObject().apply {
                                            put("sessionId", "debug-session")
                                            put("runId", "run1")
                                            put("hypothesisId", "A")
                                            put("location", "ScannerScreen.kt:onRefresh")
                                            put("message", "onRefresh вызван")
                                            put("timestamp", System.currentTimeMillis())
                                            put("data", org.json.JSONObject().apply {
                                                put("isScanning", uiState.isScanning)
                                                put("hasPermissions", viewModel.hasWifiPermissions())
                                            })
                                        }
                                        logFile.appendText(logEntry.toString() + "\n")
                                    } catch (e: Exception) { /* ignore */ }
                                    // #endregion
                                    if (viewModel.hasWifiPermissions()) {
                                        viewModel.startScan()
                                    }
                                }
                            )
                        }
                    }
                    is Result.Error -> {
                        ErrorContent(
                            error = result.message ?: result.exception.message ?: "Неизвестная ошибка",
                            onRetry = { 
                                if (viewModel.hasWifiPermissions()) {
                                    viewModel.retry()
                                } else {
                                    showPermissionDialog = true
                                }
                            }
                        )
                    }
                }
            }
        }
        }
        
        if (selectedNetwork != null) {
            val detailsAnalyticsUi = remember(detailsSignalAnalytics) {
                detailsSignalAnalytics?.let {
                    SignalAnalyticsUi(
                        averageSignal = it.averageSignal,
                        minSignal = it.minSignal,
                        maxSignal = it.maxSignal,
                        scansCount = it.scansCount,
                        lastScanTime = it.lastScanTime,
                        signalVariation = it.signalVariation
                    )
                }
            }

            val modalDetails = remember(
                detailsUiState,
                detailsDbNetwork,
                detailsHistory,
                detailsAnalyticsUi
            ) {
                NetworkCardDetails(
                    dbNetwork = detailsDbNetwork,
                    scanHistory = detailsHistory.take(50),
                    signalAnalytics = detailsAnalyticsUi,
                    isLoading = detailsUiState.isLoading,
                    errorMessage = detailsUiState.errorMessage
                )
            }
            
            NetworkDetailsModal(
                network = selectedNetwork,
                details = modalDetails,
                onDismiss = { flippedBssid = null }
            )
        }
    }
}

@Composable
private fun ScanningContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Сканирование...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Ошибка",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("Повторить")
            }
        }
    }
}

@Composable
private fun EmptyContent(
    isWifiEnabled: Boolean,
    onStartScan: () -> Unit
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = if (isWifiEnabled) Icons.Default.WifiOff else Icons.Default.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isWifiEnabled) {
                    "Сети не найдены"
                } else {
                    "Wi-Fi отключен"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            if (!isWifiEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Включите Wi-Fi для сканирования сетей",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (isWifiEnabled) {
                Button(onClick = onStartScan) {
                    Text("Сканировать")
                }
            } else {
                Button(
                    onClick = {
                        // Открываем настройки WiFi
                        val intent = android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    }
                ) {
                    Text("Включить Wi-Fi")
                }
            }
        }
    }
}

@Composable
private fun LocationDisabledContent(
    onOpenLocationSettings: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Включите геолокацию",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "На некоторых устройствах поиск Wi‑Fi сетей не работает при выключенной геолокации.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onOpenLocationSettings) {
                Text("Открыть настройки геолокации")
            }
        }
    }
}

@Composable
private fun NetworksList(
    networks: List<com.wifiguard.core.domain.model.WifiScanResult>,
    onNetworkClick: (com.wifiguard.core.domain.model.WifiScanResult) -> Unit,
    currentConnectedBssid: String? = null,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    // #region agent log
    val context = LocalContext.current
    LaunchedEffect(isRefreshing) {
        try {
            val logFile = java.io.File("/Users/mint1024/Desktop/андроид/.cursor/debug.log")
            val logEntry = org.json.JSONObject().apply {
                put("sessionId", "debug-session")
                put("runId", "run1")
                put("hypothesisId", "B")
                put("location", "ScannerScreen.kt:NetworksList")
                put("message", "isRefreshing изменилось")
                put("timestamp", System.currentTimeMillis())
                put("data", org.json.JSONObject().apply {
                    put("isRefreshing", isRefreshing)
                })
            }
            logFile.appendText(logEntry.toString() + "\n")
        } catch (e: Exception) { /* ignore */ }
    }
    // #endregion
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = networks,
                key = { index, network -> "${network.bssid}_${network.timestamp}_$index" },
                contentType = { _, network -> network.isConnected }
            ) { index, network ->
                NetworkCard(
                    network = network,
                    onClick = { onNetworkClick(network) },
                    isCurrentNetwork = network.bssid == currentConnectedBssid
                )
            }
        }
    }
}

private fun getSecurityTypeText(securityType: com.wifiguard.core.domain.model.SecurityType): String {
    return when (securityType) {
        com.wifiguard.core.domain.model.SecurityType.OPEN -> "Открытая"
        com.wifiguard.core.domain.model.SecurityType.WEP -> "WEP"
        com.wifiguard.core.domain.model.SecurityType.WPA -> "WPA"
        com.wifiguard.core.domain.model.SecurityType.WPA2 -> "WPA2"
        com.wifiguard.core.domain.model.SecurityType.WPA3 -> "WPA3"
        com.wifiguard.core.domain.model.SecurityType.WPA2_WPA3 -> "WPA2/WPA3"
        com.wifiguard.core.domain.model.SecurityType.EAP -> "EAP"
        com.wifiguard.core.domain.model.SecurityType.UNKNOWN -> "Неизвестно"
    }
}