package com.wifiguard.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wifiguard.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Индикатор статуса сканирования
 */
@Composable
fun StatusIndicator(
    isWifiEnabled: Boolean,
    isScanning: Boolean,
    networksCount: Int,
    lastScanTime: Long?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isWifiEnabled -> MaterialTheme.colorScheme.errorContainer
                isScanning -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка статуса
            Icon(
                imageVector = getStatusIcon(isWifiEnabled, isScanning),
                contentDescription = null,
                tint = getStatusColor(isWifiEnabled, isScanning),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Информация о статусе
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = getStatusText(isWifiEnabled, isScanning),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = getStatusColor(isWifiEnabled, isScanning)
                )
                
                if (isWifiEnabled && !isScanning) {
                    Text(
                        text = stringResource(R.string.scanner_networks_found, networksCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    lastScanTime?.let { time ->
                        Text(
                            text = stringResource(
                                R.string.scanner_last_scan,
                                formatTime(time)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun getStatusIcon(isWifiEnabled: Boolean, isScanning: Boolean): ImageVector {
    return when {
        !isWifiEnabled -> Icons.Default.WifiOff
        isScanning -> Icons.Default.Wifi
        else -> Icons.Default.CheckCircle
    }
}

private fun getStatusColor(isWifiEnabled: Boolean, isScanning: Boolean): Color {
    return when {
        !isWifiEnabled -> MaterialTheme.colorScheme.error
        isScanning -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun getStatusText(isWifiEnabled: Boolean, isScanning: Boolean): String {
    return when {
        !isWifiEnabled -> stringResource(R.string.scanner_wifi_disabled)
        isScanning -> stringResource(R.string.scanner_scanning)
        else -> "Wi-Fi активен"
    }
}

private fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(timestamp))
}