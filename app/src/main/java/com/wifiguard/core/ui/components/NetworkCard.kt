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
 * Modern cyber-security themed card component for displaying Wi-Fi network information.
 * Features a visual threat indicator and clean hierarchy with enhanced visual hierarchy.
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
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isCurrentNetwork) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp), // Increased vertical spacing between cards
        shape = MaterialTheme.shapes.medium.copy(
            bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd,
            bottomStart = MaterialTheme.shapes.extraSmall.bottomStart
        ), // Slightly more modern rounded corners
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = WifiGuardElevation.Level2, // Consistent elevation system
            pressedElevation = WifiGuardElevation.Level3,
            hoveredElevation = WifiGuardElevation.Level3
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            borderColor.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left Status Strip - More prominent visual threat indicator
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .background(statusColor)
            )

            // Main Content - More internal padding for better visual hierarchy
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .weight(1f)
            ) {
                // Header: SSID + Signal - Enhanced visual hierarchy
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false) // Allow space for badge
                    ) {
                        Icon(
                            imageVector = if (network.isConnected) Icons.Default.Wifi else Icons.Default.SignalWifi4Bar,
                            contentDescription = null,
                            tint = if (isCurrentNetwork) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = network.ssid.ifEmpty { "Скрытая сеть" },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold, // Bolder for better hierarchy
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface // Higher contrast
                            )
                            if (isCurrentNetwork) {
                                Text(
                                    text = "Подключено",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    // Security Badge - More prominent and better positioned
                    ThreatBadge(threatLevel = network.threatLevel)
                }

                Spacer(modifier = Modifier.height(14.dp)) // Increased spacing

                // Details Section - Improved layout for technical information
                // Using a grid-like layout for better information density
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Frequency and Channel Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Frequency and Channel in a single row
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (network.frequency > 0) {
                                Text(
                                    text = ".freq: ${network.frequency} МГц",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (network.channel > 0) {
                                Text(
                                    text = "· channel: ${network.channel}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Security Protocol
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (network.securityType == SecurityType.OPEN) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = getSecurityTypeText(network.securityType),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Vendor Info (if available) - Smaller, less prominent
                    if (!network.vendor.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Vendor: $network.vendor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
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
        ThreatLevel.LOW -> SecurityLow to "Низкий"
        ThreatLevel.MEDIUM -> SecurityMedium to "Средний"
        ThreatLevel.HIGH -> SecurityHigh to "Высокий"
        ThreatLevel.CRITICAL -> SecurityCritical to "Критический"
        ThreatLevel.UNKNOWN -> SecurityUnknown to "Неизвестно"
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), // More pill-like shape
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), // More generous padding
            style = MaterialTheme.typography.labelMedium,
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