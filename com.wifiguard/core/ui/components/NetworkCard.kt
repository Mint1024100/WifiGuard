package com.wifiguard.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wifiguard.core.ui.theme.*

/**
 * Модель данных для Wi-Fi сети
 */
data class NetworkInfo(
    val ssid: String,
    val bssid: String,
    val security: String,
    val signalStrength: Int,
    val frequency: Int,
    val channel: Int,
    val isConnected: Boolean = false,
    val isScanning: Boolean = false,
    val capabilities: String = "",
    val distance: String? = null,
    val vendor: String? = null,
    val lastSeen: Long? = null
)

/**
 * Стиль отображения NetworkCard
 */
enum class NetworkCardStyle {
    COMPACT,    // Компактная версия для списков
    DETAILED,   // Подробная версия с дополнительной информацией
    MINIMAL     // Минимальная версия для обзора
}

/**
 * Основной компонент для отображения информации о Wi-Fi сети
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkCard(
    networkInfo: NetworkInfo,
    style: NetworkCardStyle = NetworkCardStyle.COMPACT,
    modifier: Modifier = Modifier,
    onNetworkClick: ((NetworkInfo) -> Unit)? = null,
    onNetworkLongClick: ((NetworkInfo) -> Unit)? = null,
    showTechnicalDetails: Boolean = false,
    animated: Boolean = true
) {
    val density = LocalDensity.current
    
    // Анимация появления карточки
    val animationSpec = if (animated) {
        spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    } else {
        snap()
    }
    
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(networkInfo.ssid) {
        isVisible = true
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = if (animated) {
            slideInVertically(
                initialOffsetY = { with(density) { 40.dp.roundToPx() } },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        } else {
            EnterTransition.None
        }
    ) {
        when (style) {
            NetworkCardStyle.COMPACT -> {
                CompactNetworkCard(
                    networkInfo = networkInfo,
                    onNetworkClick = onNetworkClick,
                    onNetworkLongClick = onNetworkLongClick,
                    modifier = modifier,
                    animated = animated
                )
            }
            NetworkCardStyle.DETAILED -> {
                DetailedNetworkCard(
                    networkInfo = networkInfo,
                    onNetworkClick = onNetworkClick,
                    onNetworkLongClick = onNetworkLongClick,
                    showTechnicalDetails = showTechnicalDetails,
                    modifier = modifier,
                    animated = animated
                )
            }
            NetworkCardStyle.MINIMAL -> {
                MinimalNetworkCard(
                    networkInfo = networkInfo,
                    onNetworkClick = onNetworkClick,
                    modifier = modifier,
                    animated = animated
                )
            }
        }
    }
}

/**
 * Компактная версия карточки сети для списков
 */
@Composable
private fun CompactNetworkCard(
    networkInfo: NetworkInfo,
    onNetworkClick: ((NetworkInfo) -> Unit)?,
    onNetworkLongClick: ((NetworkInfo) -> Unit)?,
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val securityType = getSecurityType(networkInfo.security)
    val signalType = getSignalType(networkInfo.signalStrength)
    val connectionStatus = getConnectionStatus(networkInfo)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Wi-Fi network ${networkInfo.ssid}, " +
                    "Security: ${networkInfo.security}, " +
                    "Signal: ${networkInfo.signalStrength} dBm"
            }
            .run {
                when {
                    onNetworkClick != null && onNetworkLongClick != null -> {
                        clickable { onNetworkClick(networkInfo) }
                    }
                    onNetworkClick != null -> clickable { onNetworkClick(networkInfo) }
                    else -> this
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (networkInfo.isConnected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Signal strength indicator
            SignalStrengthIndicator(
                signalStrength = networkInfo.signalStrength,
                animated = animated
            )
            
            // Network information
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = networkInfo.ssid.ifEmpty { "Hidden Network" },
                        style = MaterialTheme.wifiTypography.networkName,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (networkInfo.isConnected) {
                        StatusIndicator(
                            type = StatusType.CONNECTION_ACTIVE,
                            style = StatusStyle.DOT
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusIndicator(
                        type = securityType,
                        style = StatusStyle.CHIP,
                        animated = animated
                    )
                    
                    Text(
                        text = "${networkInfo.signalStrength} dBm",
                        style = MaterialTheme.wifiTypography.technicalDataSmall,
                        color = getSignalColor(networkInfo.signalStrength),
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (networkInfo.frequency > 0) {
                        Text(
                            text = "${networkInfo.frequency / 1000.0}GHz",
                            style = MaterialTheme.wifiTypography.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Connection status and actions
            if (networkInfo.isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

/**
 * Подробная версия карточки с дополнительной технической информацией
 */
@Composable
private fun DetailedNetworkCard(
    networkInfo: NetworkInfo,
    onNetworkClick: ((NetworkInfo) -> Unit)?,
    onNetworkLongClick: ((NetworkInfo) -> Unit)?,
    showTechnicalDetails: Boolean,
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val securityType = getSecurityType(networkInfo.security)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .run {
                if (onNetworkClick != null) clickable { onNetworkClick(networkInfo) } else this
            },
        colors = CardDefaults.cardColors(
            containerColor = if (networkInfo.isConnected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header с названием сети и статусом подключения
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = networkInfo.ssid.ifEmpty { "Hidden Network" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    networkInfo.vendor?.let { vendor ->
                        Text(
                            text = vendor,
                            style = MaterialTheme.wifiTypography.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (networkInfo.isConnected) {
                    StatusIndicator(
                        type = StatusType.CONNECTION_ACTIVE,
                        style = StatusStyle.CHIP,
                        text = "Подключено"
                    )
                }
            }
            
            // Основная информация о сети
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Signal strength с большим индикатором
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SignalStrengthIndicator(
                        signalStrength = networkInfo.signalStrength,
                        size = 32.dp,
                        animated = animated
                    )
                    Text(
                        text = "${networkInfo.signalStrength} dBm",
                        style = MaterialTheme.wifiTypography.numericData,
                        color = getSignalColor(networkInfo.signalStrength)
                    )
                    Text(
                        text = getSignalQualityText(networkInfo.signalStrength),
                        style = MaterialTheme.wifiTypography.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Безопасность и дополнительная информация
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusIndicator(
                        type = securityType,
                        style = StatusStyle.DETAILED,
                        text = networkInfo.security,
                        secondaryText = getSecurityDescription(networkInfo.security)
                    )
                    
                    if (networkInfo.frequency > 0) {
                        NetworkInfoRow(
                            label = "Частота",
                            value = "${networkInfo.frequency} MHz (${networkInfo.frequency / 1000.0}GHz)",
                            icon = Icons.Filled.Radio
                        )
                    }
                    
                    if (networkInfo.channel > 0) {
                        NetworkInfoRow(
                            label = "Канал",
                            value = networkInfo.channel.toString(),
                            icon = Icons.Filled.Tune
                        )
                    }
                    
                    networkInfo.distance?.let { distance ->
                        NetworkInfoRow(
                            label = "Расстояние",
                            value = distance,
                            icon = Icons.Filled.LocationOn
                        )
                    }
                }
            }
            
            // Техническая информация (расширяемая)
            if (showTechnicalDetails) {
                AnimatedVisibility(
                    visible = showTechnicalDetails,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    TechnicalDetailsSection(networkInfo)
                }
            }
        }
    }
}

/**
 * Минимальная версия карточки для обзора
 */
@Composable
private fun MinimalNetworkCard(
    networkInfo: NetworkInfo,
    onNetworkClick: ((NetworkInfo) -> Unit)?,
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val securityColor = getSecurityColor(networkInfo.security)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .run {
                if (onNetworkClick != null) clickable { onNetworkClick(networkInfo) } else this
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = networkInfo.ssid.ifEmpty { "Hidden" },
                style = MaterialTheme.wifiTypography.networkNameSecondary,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusIndicator(
                    type = getSecurityType(networkInfo.security),
                    style = StatusStyle.DOT
                )
                
                Text(
                    text = "${networkInfo.signalStrength}",
                    style = MaterialTheme.wifiTypography.caption,
                    color = getSignalColor(networkInfo.signalStrength)
                )
            }
        }
    }
}

/**
 * Индикатор уровня сигнала
 */
@Composable
private fun SignalStrengthIndicator(
    signalStrength: Int,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    animated: Boolean = true
) {
    val signalLevel = getSignalLevel(signalStrength)
    val signalColor = getSignalColor(signalStrength)
    
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        repeat(4) { index ->
            val barHeight = (index + 1) * 0.25f
            val isActive = index < signalLevel
            
            val animatedAlpha by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.3f,
                animationSpec = if (animated) tween(300) else snap()
            )
            
            Box(
                modifier = Modifier
                    .fillMaxHeight(barHeight)
                    .width(3.dp)
                    .offset(x = (index * 4).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(signalColor.copy(alpha = animatedAlpha))
            )
        }
    }
}

/**
 * Строка с информацией о сети
 */
@Composable
private fun NetworkInfoRow(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.wifiTypography.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.wifiTypography.technicalDataSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Секция с технической информацией
 */
@Composable
private fun TechnicalDetailsSection(networkInfo: NetworkInfo) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Техническая информация",
                style = MaterialTheme.wifiTypography.tableHeader,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            NetworkInfoRow(
                label = "BSSID",
                value = networkInfo.bssid,
                icon = Icons.Filled.Fingerprint
            )
            
            if (networkInfo.capabilities.isNotEmpty()) {
                NetworkInfoRow(
                    label = "Возможности",
                    value = networkInfo.capabilities,
                    icon = Icons.Filled.Settings
                )
            }
            
            networkInfo.lastSeen?.let { lastSeen ->
                NetworkInfoRow(
                    label = "Последнее обнаружение",
                    value = formatLastSeen(lastSeen),
                    icon = Icons.Filled.AccessTime
                )
            }
        }
    }
}

// Вспомогательные функции
private fun getSecurityType(security: String): StatusType {
    return when {
        security.contains("WPA3", ignoreCase = true) -> StatusType.SECURITY_HIGH
        security.contains("WPA2", ignoreCase = true) -> StatusType.SECURITY_HIGH
        security.contains("WPA", ignoreCase = true) -> StatusType.SECURITY_MEDIUM
        security.contains("WEP", ignoreCase = true) -> StatusType.SECURITY_LOW
        security.contains("OPEN", ignoreCase = true) || security.isEmpty() -> StatusType.SECURITY_LOW
        else -> StatusType.SECURITY_UNKNOWN
    }
}

private fun getConnectionStatus(networkInfo: NetworkInfo): StatusType {
    return when {
        networkInfo.isScanning -> StatusType.CONNECTION_SCANNING
        networkInfo.isConnected -> StatusType.CONNECTION_ACTIVE
        else -> StatusType.CONNECTION_DISCONNECTED
    }
}

private fun getSignalLevel(signalStrength: Int): Int {
    return when {
        signalStrength >= -50 -> 4
        signalStrength >= -60 -> 3
        signalStrength >= -70 -> 2
        signalStrength >= -80 -> 1
        else -> 0
    }
}

private fun getSignalQualityText(signalStrength: Int): String {
    return when {
        signalStrength >= -50 -> "Отлично"
        signalStrength >= -60 -> "Хорошо"
        signalStrength >= -70 -> "Средне"
        signalStrength >= -80 -> "Слабо"
        else -> "Очень слабо"
    }
}

private fun getSecurityDescription(security: String): String {
    return when {
        security.contains("WPA3") -> "Высокий уровень защиты"
        security.contains("WPA2") -> "Хорошая защита"
        security.contains("WPA") -> "Базовая защита"
        security.contains("WEP") -> "Устаревшая защита"
        else -> "Нет защиты"
    }
}

private fun formatLastSeen(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60000 -> "Только что"
        diff < 3600000 -> "${diff / 60000} мин назад"
        diff < 86400000 -> "${diff / 3600000} ч назад"
        else -> "${diff / 86400000} д назад"
    }
}

// Preview компонентов
@Preview(showBackground = true)
@Composable
fun NetworkCardPreview() {
    WifiGuardTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val sampleNetwork = NetworkInfo(
                ssid = "MyHomeWiFi",
                bssid = "00:11:22:33:44:55",
                security = "WPA2-Personal",
                signalStrength = -45,
                frequency = 2437,
                channel = 6,
                isConnected = true,
                vendor = "TP-Link Technologies"
            )
            
            val weakNetwork = NetworkInfo(
                ssid = "WeakSignal",
                bssid = "AA:BB:CC:DD:EE:FF",
                security = "Open",
                signalStrength = -85,
                frequency = 5180,
                channel = 36,
                isConnected = false
            )
            
            Text("Compact Style", style = MaterialTheme.typography.headlineSmall)
            NetworkCard(sampleNetwork, NetworkCardStyle.COMPACT)
            NetworkCard(weakNetwork, NetworkCardStyle.COMPACT)
            
            Text("Detailed Style", style = MaterialTheme.typography.headlineSmall)
            NetworkCard(
                sampleNetwork, 
                NetworkCardStyle.DETAILED,
                showTechnicalDetails = true
            )
            
            Text("Minimal Style", style = MaterialTheme.typography.headlineSmall)
            NetworkCard(sampleNetwork, NetworkCardStyle.MINIMAL)
        }
    }
}
