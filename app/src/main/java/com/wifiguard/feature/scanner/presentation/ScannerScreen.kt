package com.wifiguard.feature.scanner.presentation

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wifiguard.R
import com.wifiguard.core.ui.components.NetworkCard
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(stringResource(R.string.scanner_title))
                },
                actions = {
                    IconButton(onClick = { viewModel.startScan() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.common_refresh)
                        )
                    }
                    IconButton(onClick = onNavigateToAnalysis) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = stringResource(R.string.nav_analysis)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_settings)
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
            StatusIndicator(
                isWifiEnabled = uiState.isWifiEnabled,
                isScanning = uiState.isScanning,
                networksCount = uiState.networks.size,
                lastScanTime = uiState.lastScanTime,
                modifier = Modifier.padding(16.dp)
            )
            
            // Основной контент
            when {
                uiState.isScanning -> {
                    ScanningContent()
                }
                uiState.error != null -> {
                    ErrorContent(
                        error = uiState.error,
                        onRetry = { viewModel.startScan() }
                    )
                }
                uiState.networks.isEmpty() -> {
                    EmptyContent(
                        isWifiEnabled = uiState.isWifiEnabled,
                        onStartScan = { viewModel.startScan() }
                    )
                }
                else -> {
                    NetworksList(
                        networks = uiState.networks,
                        onNetworkClick = { network ->
                            // TODO: Navigate to network details
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
                text = stringResource(R.string.scanner_scanning),
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
                text = stringResource(R.string.common_error),
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
                Text(stringResource(R.string.common_retry))
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
                    stringResource(R.string.scanner_no_networks)
                } else {
                    stringResource(R.string.scanner_wifi_disabled)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (isWifiEnabled) {
                Button(onClick = onStartScan) {
                    Text(stringResource(R.string.scanner_scan_button))
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