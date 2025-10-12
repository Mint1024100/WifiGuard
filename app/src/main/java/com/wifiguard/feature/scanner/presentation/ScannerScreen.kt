package com.wifiguard.feature.scanner.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wifiguard.core.ui.components.NetworkCard
import com.wifiguard.core.ui.components.ScanningStatusIndicator

/**
 * Главный экран сканирования Wi-Fi сетей
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onNavigateToAnalysis: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    viewModel: WifiScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val discoveredNetworks by viewModel.discoveredNetworks.collectAsStateWithLifecycle()
    val suspiciousNetworks by viewModel.suspiciousNetworks.collectAsStateWithLifecycle()
    
    var showFilterMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "WifiGuard",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Notifications icon with badge
                    IconButton(onClick = onNavigateToNotifications) {
                        BadgedBox(
                            badge = {
                                if (suspiciousNetworks.isNotEmpty()) {
                                    Badge {
                                        Text("${suspiciousNetworks.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Уведомления"
                            )
                        }
                    }
                    
                    // Settings icon
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Настройки"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.startWifiScan() },
                icon = {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(Icons.Default.Refresh, "Сканировать")
                    }
                },
                text = { 
                    Text(if (uiState.isScanning) "Сканирование..." else "Сканировать") 
                },
                expanded = !uiState.isScanning
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Scanning status indicator
            ScanningStatusIndicator(
                isScanning = uiState.isScanning,
                networksFound = discoveredNetworks.size,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.networkFilter == NetworkFilter.ALL,
                    onClick = { viewModel.updateNetworkFilter(NetworkFilter.ALL) },
                    label = { Text("Все") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                
                FilterChip(
                    selected = uiState.networkFilter == NetworkFilter.SUSPICIOUS,
                    onClick = { viewModel.updateNetworkFilter(NetworkFilter.SUSPICIOUS) },
                    label = { Text("Подозрительные") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                
                FilterChip(
                    selected = uiState.networkFilter == NetworkFilter.KNOWN,
                    onClick = { viewModel.updateNetworkFilter(NetworkFilter.KNOWN) },
                    label = { Text("Известные") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
            
            // Error message
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            
            // Networks list
            if (discoveredNetworks.isEmpty() && !uiState.isScanning) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Нет доступных сетей",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Нажмите кнопку сканирования для поиска",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = discoveredNetworks,
                        key = { it.bssid }
                    ) { network ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            NetworkCard(
                                wifiInfo = convertToWifiInfo(network),
                                onCardClick = { 
                                    onNavigateToAnalysis(network.ssid)
                                },
                                modifier = Modifier.animateItemPlacement()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Преобразует WifiNetwork в WifiInfo для отображения
 */
private fun convertToWifiInfo(network: com.wifiguard.core.domain.model.WifiNetwork): com.wifiguard.feature.scanner.domain.model.WifiInfo {
    return com.wifiguard.feature.scanner.domain.model.WifiInfo(
        ssid = network.ssid,
        bssid = network.bssid,
        capabilities = "", // Не сохраняем capabilities в WifiNetwork
        level = network.signalStrength,
        frequency = network.frequency,
        timestamp = network.lastSeen,
        encryptionType = network.securityType,
        signalStrength = network.signalStrength,
        channel = network.channel,
        bandwidth = null,
        isHidden = network.ssid.isEmpty(),
        isConnected = false,
        isSaved = network.isKnown
    )
}

