package com.wifiguard.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.ui.theme.*

/**
 * Modern card component for displaying Wi-Fi network information.
 * Features a visual threat indicator and clean hierarchy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkCard(
    network: WifiScanResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrentNetwork: Boolean = false
) {
    // Determine status color based on threat level
    val statusColor = when (network.threatLevel) {
        ThreatLevel.SAFE -> SecuritySafe
        ThreatLevel.LOW -> SecurityLow
        ThreatLevel.MEDIUM -> SecurityMedium
        ThreatLevel.HIGH -> SecurityHigh
        ThreatLevel.CRITICAL -> SecurityCritical
        ThreatLevel.UNKNOWN -> SecurityUnknown
    }

    val containerColor = if (isCurrentNetwork) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isCurrentNetwork) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Add slight vertical spacing between cards
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrentNetwork) 4.dp else 1.dp
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min) // Match height to content
        ) {
            // Left Status Strip
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(statusColor)
            )

            // Main Content
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
            ) {
                // Header: SSID + Signal
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (network.isConnected) Icons.Default.Wifi else Icons.Default.SignalWifi4Bar,
                            contentDescription = null,
                            tint = if (isCurrentNetwork) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = network.ssid.ifEmpty { "Скрытая сеть" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isCurrentNetwork) {
                                Text(
                                    text = "Подключено",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Security Badge
                    ThreatBadge(threatLevel = network.threatLevel)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Details Row: Frequency, Channel, Security Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Frequency
                    DetailItem(
                        label = "Част",
                        value = "${network.frequency} МГц"
                    )

                    // Channel
                    if (network.channel > 0) {
                        DetailItem(
                            label = "Кан",
                            value = "${network.channel}"
                        )
                    }

                    // Security Protocol
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (network.securityType == SecurityType.OPEN) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = getSecurityTypeText(network.securityType),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Vendor Info (if available)
                if (!network.vendor.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = network.vendor,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ThreatBadge(threatLevel: ThreatLevel) {
    val (color, text) = when (threatLevel) {
        ThreatLevel.SAFE -> SecuritySafe to "Безопасно"
        ThreatLevel.LOW -> SecurityLow to "Низкий риск"
        ThreatLevel.MEDIUM -> SecurityMedium to "Средний риск"
        ThreatLevel.HIGH -> SecurityHigh to "Высокий риск"
        ThreatLevel.CRITICAL -> SecurityCritical to "Критический"
        ThreatLevel.UNKNOWN -> SecurityUnknown to "Неизвестно"
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun getSecurityTypeText(securityType: SecurityType): String {
    return when (securityType) {
        SecurityType.OPEN -> "Открытая"
        SecurityType.WEP -> "WEP"
        SecurityType.WPA -> "WPA"
        SecurityType.WPA2 -> "WPA2"
        SecurityType.WPA3 -> "WPA3"
        SecurityType.WPA2_WPA3 -> "WPA2/3"
        SecurityType.EAP -> "EAP"
        SecurityType.UNKNOWN -> "Неизвестно"
    }
}