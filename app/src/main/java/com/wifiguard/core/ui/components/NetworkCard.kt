package com.wifiguard.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wifiguard.R
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiScanResult

/**
 * Карточка Wi-Fi сети
 */
@OptIn(ExperimentalMaterial3Api::class)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Text(
                    text = network.ssid.ifEmpty { stringResource(R.string.network_hidden) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                if (network.isConnected) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.network_connected),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Информация о сети
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Безопасность
                SecurityInfo(
                    securityType = network.securityType,
                    threatLevel = network.threatLevel
                )
                
                // Сигнал
                SignalInfo(
                    level = network.level,
                    frequency = network.frequency
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Дополнительная информация
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // BSSID
                Text(
                    text = network.bssid,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Канал
                if (network.channel > 0) {
                    Text(
                        text = stringResource(R.string.network_channel, network.channel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityInfo(
    securityType: SecurityType,
    threatLevel: ThreatLevel
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getSecurityIcon(securityType),
            contentDescription = null,
            tint = getSecurityColor(securityType),
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Column {
            Text(
                text = getSecurityTypeText(securityType),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = getThreatLevelText(threatLevel),
                style = MaterialTheme.typography.bodySmall,
                color = getThreatLevelColor(threatLevel)
            )
        }
    }
}

@Composable
private fun SignalInfo(
    level: Int,
    frequency: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getSignalIcon(level),
            contentDescription = null,
            tint = getSignalColor(level),
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Column {
            Text(
                text = stringResource(R.string.network_signal_strength, getSignalStrengthText(level)),
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = stringResource(R.string.network_frequency, frequency),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getSecurityIcon(securityType: SecurityType): ImageVector {
    return when (securityType) {
        SecurityType.OPEN -> Icons.Default.LockOpen
        else -> Icons.Default.Lock
    }
}

private fun getSecurityColor(securityType: SecurityType): Color {
    return when (securityType) {
        SecurityType.OPEN -> MaterialTheme.colorScheme.error
        SecurityType.WEP -> MaterialTheme.colorScheme.error
        SecurityType.WPA -> MaterialTheme.colorScheme.error
        SecurityType.WPA2 -> MaterialTheme.colorScheme.primary
        SecurityType.WPA3 -> MaterialTheme.colorScheme.primary
        SecurityType.WPA2_WPA3 -> MaterialTheme.colorScheme.primary
        SecurityType.EAP -> MaterialTheme.colorScheme.primary
        SecurityType.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun getSecurityTypeText(securityType: SecurityType): String {
    return when (securityType) {
        SecurityType.OPEN -> stringResource(R.string.security_open)
        SecurityType.WEP -> stringResource(R.string.security_wep)
        SecurityType.WPA -> stringResource(R.string.security_wpa)
        SecurityType.WPA2 -> stringResource(R.string.security_wpa2)
        SecurityType.WPA3 -> stringResource(R.string.security_wpa3)
        SecurityType.WPA2_WPA3 -> stringResource(R.string.security_wpa2_wpa3)
        SecurityType.EAP -> stringResource(R.string.security_eap)
        SecurityType.UNKNOWN -> stringResource(R.string.security_unknown)
    }
}

private fun getThreatLevelText(threatLevel: ThreatLevel): String {
    return when (threatLevel) {
        ThreatLevel.SAFE -> stringResource(R.string.threat_safe)
        ThreatLevel.LOW -> stringResource(R.string.threat_low)
        ThreatLevel.MEDIUM -> stringResource(R.string.threat_medium)
        ThreatLevel.HIGH -> stringResource(R.string.threat_high)
        ThreatLevel.CRITICAL -> stringResource(R.string.threat_critical)
        ThreatLevel.UNKNOWN -> stringResource(R.string.threat_unknown)
    }
}

private fun getThreatLevelColor(threatLevel: ThreatLevel): Color {
    return when (threatLevel) {
        ThreatLevel.SAFE -> MaterialTheme.colorScheme.primary
        ThreatLevel.LOW -> MaterialTheme.colorScheme.primary
        ThreatLevel.MEDIUM -> MaterialTheme.colorScheme.error
        ThreatLevel.HIGH -> MaterialTheme.colorScheme.error
        ThreatLevel.CRITICAL -> MaterialTheme.colorScheme.error
        ThreatLevel.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun getSignalIcon(level: Int): ImageVector {
    return when {
        level >= -50 -> Icons.Default.SignalWifi4Bar
        level >= -60 -> Icons.Default.SignalWifi4Bar
        level >= -70 -> Icons.Default.SignalWifi4Bar
        level >= -80 -> Icons.Default.SignalWifi4Bar
        else -> Icons.Default.SignalWifiOff
    }
}

private fun getSignalColor(level: Int): Color {
    return when {
        level >= -50 -> MaterialTheme.colorScheme.primary
        level >= -60 -> MaterialTheme.colorScheme.primary
        level >= -70 -> MaterialTheme.colorScheme.error
        level >= -80 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.error
    }
}

private fun getSignalStrengthText(level: Int): String {
    return when {
        level >= -30 -> stringResource(R.string.signal_excellent)
        level >= -50 -> stringResource(R.string.signal_good)
        level >= -60 -> stringResource(R.string.signal_fair)
        level >= -70 -> stringResource(R.string.signal_weak)
        level >= -80 -> stringResource(R.string.signal_very_weak)
        else -> stringResource(R.string.signal_critical)
    }
}