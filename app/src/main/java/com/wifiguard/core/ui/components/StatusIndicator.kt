package com.wifiguard.core.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import com.wifiguard.core.ui.theme.*

/**
 * Modern cyber-security themed dashboard widget for Wi-Fi scanning status
 * Features a prominent dashboard-style design with enhanced visual hierarchy
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
        shape = MaterialTheme.shapes.large, // More rounded corners for dashboard widget
        elevation = CardDefaults.cardElevation(
            defaultElevation = WifiGuardElevation.Level3 // More prominent for dashboard
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer // Use primary container for dashboard look
        )
    ) {
        // Dashboard layout with prominent metrics and status indicators
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp) // Increased padding for better dashboard feel
        ) {
            // Dashboard Header - Prominent metric display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (isWifiEnabled && networksCount > 0) SecuritySafe else MaterialTheme.colorScheme.error,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = if (isWifiEnabled) "Wi-Fi Активен" else "Wi-Fi Выключен",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Networks count as key metric
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.secondary,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = networksCount.toString(),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold // Prominent number display
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status details - horizontal arrangement for better dashboard feel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scanning status
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Сканирование...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else if (isWifiEnabled) {
                        Icon(
                            imageVector = if (networksCount > 0) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (networksCount > 0) SecuritySafe else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Нет активных сканирований",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Last scan time
                if (lastScanTime != null && !isScanning) {
                    Text(
                        text = "Обновлено: ${formatTime(lastScanTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(timestamp))
}