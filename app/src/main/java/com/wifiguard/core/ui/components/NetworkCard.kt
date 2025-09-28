package com.wifiguard.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wifiguard.core.ui.theme.*
import com.wifiguard.feature.scanner.domain.model.SecurityType
import com.wifiguard.feature.scanner.domain.model.SignalQuality
import com.wifiguard.feature.scanner.domain.model.WifiInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkCard(
    wifiInfo: WifiInfo,
    onCardClick: (WifiInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick(wifiInfo) },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Network Name
                Text(
                    text = if (wifiInfo.isHidden) "<Hidden Network>" else wifiInfo.ssid,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // Security Icon
                SecurityIcon(
                    securityType = wifiInfo.securityType,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Signal Strength
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SignalStrengthIcon(
                        signalQuality = wifiInfo.signalQuality,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${wifiInfo.signalStrength} dBm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                // Channel
                Text(
                    text = "Ch ${wifiInfo.channel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                // Security Badge
                SecurityBadge(
                    securityType = wifiInfo.securityType
                )
            }
        }
    }
}

@Composable
fun SecurityIcon(
    securityType: SecurityType,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = when (securityType) {
        SecurityType.OPEN -> Icons.Default.Lock to DangerRed
        SecurityType.WEP -> Icons.Default.LockOpen to WarningOrange
        SecurityType.WPA -> Icons.Default.Lock to WarningOrange
        SecurityType.WPA2 -> Icons.Default.Lock to SecureGreen
        SecurityType.WPA3 -> Icons.Default.Security to SecureGreen
        SecurityType.UNKNOWN -> Icons.Default.Help to UnknownGray
    }
    
    Icon(
        imageVector = icon,
        contentDescription = "Security: ${securityType.name}",
        tint = tint,
        modifier = modifier.size(24.dp)
    )
}

@Composable
fun SignalStrengthIcon(
    signalQuality: SignalQuality,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = when (signalQuality) {
        SignalQuality.EXCELLENT -> Icons.Default.SignalWifi4Bar to SignalExcellent
        SignalQuality.GOOD -> Icons.Default.SignalWifi3Bar to SignalGood
        SignalQuality.FAIR -> Icons.Default.SignalWifi2Bar to SignalFair
        SignalQuality.POOR -> Icons.Default.SignalWifi1Bar to SignalPoor
    }
    
    Icon(
        imageVector = icon,
        contentDescription = "Signal: ${signalQuality.name}",
        tint = tint,
        modifier = modifier
    )
}

@Composable
fun SecurityBadge(
    securityType: SecurityType,
    modifier: Modifier = Modifier
) {
    val (text, backgroundColor, textColor) = when (securityType) {
        SecurityType.OPEN -> Triple("OPEN", DangerRed, Color.White)
        SecurityType.WEP -> Triple("WEP", WarningOrange, Color.White)
        SecurityType.WPA -> Triple("WPA", WarningOrange, Color.White)
        SecurityType.WPA2 -> Triple("WPA2", SecureGreen, Color.White)
        SecurityType.WPA3 -> Triple("WPA3", SecureGreen, Color.White)
        SecurityType.UNKNOWN -> Triple("?", UnknownGray, Color.White)
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}