package com.wifiguard.feature.scanner.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifi2Bar
import androidx.compose.material.icons.filled.SignalWifi1Bar
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wifiguard.core.common.Resource
import com.wifiguard.feature.analyzer.domain.model.SecurityLevel
import com.wifiguard.feature.scanner.domain.model.WifiInfo

/**
 * Экран сканирования Wi-Fi сетей.
 * Отображает список доступных сетей с их характеристиками безопасности,
 * обрабатывает различные состояния загрузки и ошибок через Resource wrapper.
 * 
 * @param viewModel ViewModel для управления состоянием сканирования
 * @param onNetworkClick Callback для клика по сети (переход к анализу)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = hiltViewModel(),
    onNetworkClick: (WifiInfo) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Обработка сообщений об ошибках
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            // TODO: Показать Snackbar с ошибкой
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WiFi Сканер") },
                actions = {
                    IconButton(
                        onClick = { viewModel.startScan() },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Обновить сканирование"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val resource = uiState.networksResource) {
                is Resource.Loading -> {
                    LoadingContent()
                }
                
                is Resource.Success -> {
                    if (resource.data.isEmpty()) {
                        EmptyContent(
                            onRetryClick = { viewModel.startScan() }
                        )
                    } else {
                        NetworksList(
                            networks = resource.data,
                            onNetworkClick = onNetworkClick
                        )
                    }
                }
                
                is Resource.Error -> {
                    ErrorContent(
                        error = resource.throwable,
                        onRetryClick = { viewModel.startScan() }
                    )
                }
            }
            
            // Индикатор активного сканирования
            if (uiState.isScanning) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Сканирование сетей...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Компонент отображения состояния загрузки.
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Поиск WiFi сетей...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Компонент отображения пустого результата.
 */
@Composable
private fun EmptyContent(
    onRetryClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SignalWifiOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Сети не найдены",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Убедитесь, что WiFi включен и разрешения предоставлены",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetryClick) {
                Text("Повторить поиск")
            }
        }
    }
}

/**
 * Компонент отображения ошибки.
 */
@Composable
private fun ErrorContent(
    error: Throwable,
    onRetryClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SignalWifiOff,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Ошибка сканирования",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = error.message ?: "Неизвестная ошибка",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetryClick) {
                Text("Попробовать снова")
            }
        }
    }
}

/**
 * Список сетей WiFi.
 */
@Composable
private fun NetworksList(
    networks: List<WifiInfo>,
    onNetworkClick: (WifiInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = networks,
            key = { it.bssid } // Используем BSSID как уникальный ключ
        ) { network ->
            WifiNetworkCard(
                wifiInfo = network,
                onClick = { onNetworkClick(network) }
            )
        }
    }
}

/**
 * Карточка отображения информации о WiFi сети.
 * Показывает SSID, уровень безопасности, силу сигнала и тип шифрования.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WifiNetworkCard(
    wifiInfo: WifiInfo,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Заголовок с именем сети и бейджем безопасности
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = wifiInfo.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                wifiInfo.securityLevel?.let { level ->
                    SecurityLevelBadge(securityLevel = level)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Информация о сигнале и шифровании
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Сила сигнала с иконкой
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = getSignalIcon(wifiInfo.signalPercentage),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${wifiInfo.signalStrength} dBm",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Тип шифрования
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = wifiInfo.encryptionType.displayName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Дополнительная информация
            Text(
                text = "Канал: ${wifiInfo.frequency} МГц • ${wifiInfo.frequencyBand}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Бейдж уровня безопасности сети.
 */
@Composable
private fun SecurityLevelBadge(securityLevel: SecurityLevel) {
    val (color, text) = when (securityLevel) {
        SecurityLevel.HIGH -> MaterialTheme.colorScheme.error to "РИСК"
        SecurityLevel.MEDIUM -> Color(0xFFFF9800) to "СРЕДНИЙ"
        SecurityLevel.LOW -> Color(0xFF4CAF50) to "БЕЗОПАСНО"
    }
    
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Получает иконку силы сигнала в зависимости от процентного значения.
 */
private fun getSignalIcon(signalPercentage: Int) = when {
    signalPercentage >= 75 -> Icons.Default.SignalWifi4Bar
    signalPercentage >= 50 -> Icons.Default.SignalWifi2Bar  
    signalPercentage >= 25 -> Icons.Default.SignalWifi1Bar
    else -> Icons.Default.SignalWifiOff
}