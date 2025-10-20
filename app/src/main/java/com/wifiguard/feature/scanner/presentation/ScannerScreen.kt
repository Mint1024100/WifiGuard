package com.wifiguard.feature.scanner.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wifiguard.core.common.PermissionHandler
import com.wifiguard.core.common.Result
import com.wifiguard.core.ui.components.NetworkCard
import com.wifiguard.core.ui.components.PermissionRationaleDialog
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
    val uiState by viewModel.uiState.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val context = LocalContext.current

    var showPermissionDialog by remember { mutableStateOf(false) }
    
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Wi-Fi Сканер")
                },
                actions = {
                    IconButton(onClick = { 
                        if (viewModel.hasWifiPermissions()) {
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
            val currentScanResultForStatus = scanResult
            StatusIndicator(
                isWifiEnabled = uiState.isWifiEnabled,
                isScanning = currentScanResultForStatus is Result.Loading,
                networksCount = if (currentScanResultForStatus is Result.Success) currentScanResultForStatus.data.size else uiState.networks.size,
                lastScanTime = uiState.lastScanTime,
                modifier = Modifier.padding(16.dp)
            )
            
            // Основной контент
            val currentScanResult = scanResult
            when (currentScanResult) {
                is Result.Loading -> {
                    ScanningContent()
                }
                is Result.Success -> {
                    if (currentScanResult.data.isEmpty()) {
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
                            networks = currentScanResult.data,
                            onNetworkClick = { network ->
                                // TODO: Navigate to network details
                            }
                        )
                    }
                }
                is Result.Error -> {
                    ErrorContent(
                        error = currentScanResult.message ?: currentScanResult.exception.message ?: "Неизвестная ошибка",
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
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
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
            Spacer(modifier = Modifier.height(24.dp))
            if (isWifiEnabled) {
                Button(onClick = onStartScan) {
                    Text("Сканировать")
                }
            }
        }
    }
}

@Composable
private fun NetworksList(
    networks: List<com.wifiguard.core.domain.model.WifiScanResult>,
    onNetworkClick: (com.wifiguard.core.domain.model.WifiScanResult) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = networks,
            key = { it.bssid }
        ) { network ->
            NetworkCard(
                network = network,
                onClick = { onNetworkClick(network) }
            )
        }
    }
}