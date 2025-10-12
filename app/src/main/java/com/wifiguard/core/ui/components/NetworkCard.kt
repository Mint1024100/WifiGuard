package com.wifiguard.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wifiguard.R
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiScanResult

/**
 * Карточка для отображения информации о Wi-Fi сети
 */
@Composable
fun NetworkCard(
    network: WifiScanResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Заголовок с SSID и статусом подключения
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (network.isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = if (network.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = network.ssid.ifEmpty { "Hidden Network" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Индикатор уровня угрозы
                ThreatLevelIndicator(threatLevel = network.threatLevel)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Детали сети
            NetworkDetails(network = network)
        }
    }
}

@Composable
private fun ThreatLevelIndicator(
    threatLevel: ThreatLevel,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (threatLevel) {
        ThreatLevel.SAFE -> MaterialTheme.colorScheme.primary to "Безопасно"
        ThreatLevel.LOW -> Color(0xFF8BC34A) to "Низкий риск"
        ThreatLevel.MEDIUM -> Color(0xFFFF9800) to "Средний риск"
        ThreatLevel.HIGH -> Color(0xFFFF5722) to "Высокий риск"
        ThreatLevel.CRITICAL -> MaterialTheme.colorScheme.error to "Критический"
        ThreatLevel.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant to "Неизвестно"
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun NetworkDetails(
    network: WifiScanResult,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
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
        
        // Производитель
        if (!network.vendor.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Производитель: ${network.vendor}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getSecurityTypeText(securityType: SecurityType): String {
    return when (securityType) {
        SecurityType.OPEN -> "Открытая"
        SecurityType.WEP -> "WEP"
        SecurityType.WPA -> "WPA"
        SecurityType.WPA2 -> "WPA2"
        SecurityType.WPA3 -> "WPA3"
        SecurityType.WPA2_WPA3 -> "WPA2/WPA3"
        SecurityType.EAP -> "EAP"
        SecurityType.UNKNOWN -> "Неизвестно"
    }
}