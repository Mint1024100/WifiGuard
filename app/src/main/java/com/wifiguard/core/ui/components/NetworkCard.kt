package com.wifiguard.core.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.SecurityThreat
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiNetwork
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.ui.testing.UiTestTags
import com.wifiguard.core.ui.theme.*
import com.wifiguard.R
import java.text.DateFormat
import java.util.Date
import java.util.Locale

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

    val isCurrentCritical = isCurrentNetwork && network.threatLevel == ThreatLevel.CRITICAL

    val containerColor = when {
        isCurrentCritical -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
        isCurrentNetwork -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        isCurrentCritical -> MaterialTheme.colorScheme.error
        isCurrentNetwork -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
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
                            tint = when {
                                isCurrentNetwork && network.threatLevel == ThreatLevel.CRITICAL -> MaterialTheme.colorScheme.error
                                isCurrentNetwork -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = network.ssid.ifEmpty { stringResource(R.string.network_hidden) },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold, // Bolder for better hierarchy
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface // Higher contrast
                            )
                            if (isCurrentNetwork) {
                                Text(
                                    text = stringResource(R.string.network_connected),
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
                                    text = "freq: ${network.frequency} MHz",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (network.channel > 0) {
                                Text(
                                    text = "channel: ${network.channel}",
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

/**
 * ?????????????? ?????? ??? ???????? ??????? ???????? (?? + ???????).
 *
 * ?????: ???? ?????? ?? core domain, ????? ?? ???????? ?? feature-???????.
 */
data class NetworkCardDetails(
    val dbNetwork: WifiNetwork? = null,
    val scanHistory: List<WifiScanResult> = emptyList(),
    val signalAnalytics: SignalAnalyticsUi? = null,
    val activeThreats: List<SecurityThreat> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * UI-?????? ????????? ??????? (??? ??????????? ?? feature).
 */
data class SignalAnalyticsUi(
    val averageSignal: Int,
    val minSignal: Int,
    val maxSignal: Int,
    val scansCount: Int,
    val lastScanTime: Long,
    val signalVariation: Double
)

/**
 * ???????? Wi?Fi ???? ? 3D????????????: front ? back.
 *
 * Front: ??????? ?????????? ???.\n
 * Back: ??????????? ?????? (???? + ?? + ???????/?????????).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlippableNetworkCard(
    network: WifiScanResult,
    onClick: () -> Unit,
    isFlipped: Boolean,
    modifier: Modifier = Modifier,
    isCurrentNetwork: Boolean = false,
    details: NetworkCardDetails? = null
) {
    val statusColor = when (network.threatLevel) {
        ThreatLevel.SAFE -> SecuritySafe
        ThreatLevel.LOW -> SecurityLow
        ThreatLevel.MEDIUM -> SecurityMedium
        ThreatLevel.HIGH -> SecurityHigh
        ThreatLevel.CRITICAL -> SecurityCritical
        ThreatLevel.UNKNOWN -> SecurityUnknown
    }

    val isCurrentCritical = isCurrentNetwork && network.threatLevel == ThreatLevel.CRITICAL

    val containerColor = when {
        isCurrentCritical -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
        isCurrentNetwork -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        isCurrentCritical -> MaterialTheme.colorScheme.error
        isCurrentNetwork -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    // Плавная анимация поворота с использованием spring для естественного движения
    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.75f, // Более мягкое затухание для плавности
            stiffness = 300f // Умеренная жесткость для естественного движения
        ),
        label = "networkCardFlip"
    )
    
    // Плавное изменение прозрачности для более естественного перехода
    val frontAlpha by animateFloatAsState(
        targetValue = if (isFlipped) 0f else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "frontAlpha"
    )
    
    val backAlpha by animateFloatAsState(
        targetValue = if (isFlipped) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "backAlpha"
    )
    
    // Легкое изменение масштаба для эффекта глубины
    val scale by animateFloatAsState(
        targetValue = if (rotationY in 80f..100f) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 500f
        ),
        label = "cardScale"
    )
    
    val showBack = rotationY > 90f

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .animateContentSize(),
        shape = MaterialTheme.shapes.medium.copy(
            bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd,
            bottomStart = MaterialTheme.shapes.extraSmall.bottomStart
        ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = WifiGuardElevation.Level2,
            pressedElevation = WifiGuardElevation.Level3,
            hoveredElevation = WifiGuardElevation.Level3
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            borderColor.copy(alpha = 0.7f)
        )
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                this.rotationY = rotationY
                this.scaleX = scale
                this.scaleY = scale
                // Увеличенное расстояние камеры для более реалистичного 3D эффекта
                cameraDistance = 14f * density
            }
        ) {
            if (!showBack) {
                Box(
                    modifier = Modifier.alpha(frontAlpha)
                ) {
                    NetworkCardFrontContent(
                        network = network,
                        statusColor = statusColor,
                        isCurrentNetwork = isCurrentNetwork
                    )
                }
            } else {
                // Обратная сторона с компенсацией поворота для правильного отображения
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            this.rotationY = 180f
                        }
                        .alpha(backAlpha)
                ) {
                    NetworkCardBackContent(
                        network = network,
                        statusColor = statusColor,
                        details = details
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkCardFrontContent(
    network: WifiScanResult,
    statusColor: Color,
    isCurrentNetwork: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(8.dp)
                .background(statusColor)
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = if (network.isConnected) Icons.Default.Wifi else Icons.Default.SignalWifi4Bar,
                        contentDescription = null,
                        tint = when {
                            isCurrentNetwork && network.threatLevel == ThreatLevel.CRITICAL -> MaterialTheme.colorScheme.error
                            isCurrentNetwork -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = network.ssid.ifEmpty { stringResource(R.string.network_hidden) },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isCurrentNetwork) {
                            Text(
                                text = stringResource(R.string.network_connected),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                ThreatBadge(threatLevel = network.threatLevel)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (network.frequency > 0) {
                            Text(
                                text = "freq: ${network.frequency} MHz",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (network.channel > 0) {
                            Text(
                                text = "channel: ${network.channel}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

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

                if (!network.vendor.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Vendor: ${network.vendor}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkCardBackContent(
    network: WifiScanResult,
    statusColor: Color,
    details: NetworkCardDetails?,
    isModal: Boolean = false
) {
    val scrollState = rememberScrollState()
    val dateTimeFormatter = remember {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, Locale.getDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isModal) {
                    Modifier.heightIn(max = 420.dp).verticalScroll(scrollState)
                } else {
                    Modifier
                }
            )
    ) {
        // ??????? �?????? ???????�
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = network.ssid.ifEmpty { stringResource(R.string.network_hidden) },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    ThreatBadge(threatLevel = network.threatLevel)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.network_details_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                DetailItem("BSSID", network.bssid)
                DetailItem("RSSI", "${network.level} dBm (${network.getSignalStrengthDescription()})")
                DetailItem(stringResource(R.string.network_details_frequency), stringResource(R.string.network_details_frequency_mhz, network.frequency))
                if (network.channel > 0) DetailItem(stringResource(R.string.network_details_channel), network.channel.toString())
                DetailItem(stringResource(R.string.network_details_security_type), getSecurityTypeText(network.securityType))
                DetailItem("Capabilities", network.capabilities.ifEmpty { "-" })
                DetailItem(stringResource(R.string.network_details_standard), network.standard.name)
                DetailItem(stringResource(R.string.network_details_hidden), if (network.isHidden) stringResource(R.string.network_details_yes) else stringResource(R.string.network_details_no))
                DetailItem(stringResource(R.string.network_connected), if (network.isConnected) stringResource(R.string.network_details_yes) else stringResource(R.string.network_details_no))
                if (!network.vendor.isNullOrEmpty()) DetailItem("Vendor", network.vendor ?: "-")
                DetailItem(stringResource(R.string.network_details_last_scan), dateTimeFormatter.format(Date(network.timestamp)))
                DetailItem(stringResource(R.string.network_details_scan_type), network.scanType.name)

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Причины подозрений",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val reasons = buildList {
                    if (network.securityType == SecurityType.OPEN) {
                        add("Открытая сеть без шифрования (данные могут быть перехвачены)")
                    }

                    val threats = details?.activeThreats.orEmpty()
                    threats.forEach { t ->
                        val extra = t.additionalInfo?.takeIf { it.isNotBlank() }
                        if (extra != null) {
                            add("${t.description} (${extra})")
                        } else {
                            add(t.description)
                        }
                    }
                }

                if (reasons.isEmpty()) {
                    Text(
                        text = "Явные причины не найдены. Уровень угрозы рассчитан по общим факторам.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    reasons.take(10).forEach { reason ->
                        Text(
                            text = "• $reason",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.network_details_statistics),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                when {
                    details?.isLoading == true -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.network_details_loading_statistics),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // Показываем ошибку только если она не пустая и не связана с отсутствием сети в БД
                    !details?.errorMessage.isNullOrEmpty() && 
                    !details?.errorMessage?.contains("не найдена", ignoreCase = true)!! -> {
                        Text(
                            text = details?.errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    details?.dbNetwork == null -> {
                        Text(
                            text = stringResource(R.string.network_details_no_network_in_db),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        val db = details.dbNetwork
                        DetailItem(stringResource(R.string.network_details_first_seen), dateTimeFormatter.format(Date(db.firstSeen)))
                        DetailItem(stringResource(R.string.network_details_last_seen), dateTimeFormatter.format(Date(db.lastSeen)))
                        DetailItem(stringResource(R.string.network_details_known), if (db.isKnown) stringResource(R.string.network_details_yes) else stringResource(R.string.network_details_no))
                        DetailItem(stringResource(R.string.network_details_suspicious), if (db.isSuspicious) stringResource(R.string.network_details_yes) else stringResource(R.string.network_details_no))
                        if (!db.suspiciousReason.isNullOrEmpty()) {
                            DetailItem(stringResource(R.string.network_details_reason), db.suspiciousReason ?: "-")
                        }
                        DetailItem(stringResource(R.string.network_details_trust), db.trustLevel.name)
                        DetailItem(stringResource(R.string.network_details_detections), db.connectionCount.toString())
                    }

                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.network_details_history_statistics),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val history = details?.scanHistory.orEmpty()
                if (history.isEmpty()) {
                    Text(
                        text = stringResource(R.string.network_details_no_history),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    DetailItem(stringResource(R.string.network_details_scans_count), history.size.toString())
                    details?.signalAnalytics?.let { a ->
                        DetailItem(stringResource(R.string.network_details_avg_rssi), "${a.averageSignal} dBm")
                        DetailItem(stringResource(R.string.network_details_min_max), "${a.minSignal} / ${a.maxSignal} dBm")
                        DetailItem(stringResource(R.string.network_details_variation), String.format(Locale.getDefault(), "%.2f", a.signalVariation))
                        DetailItem(stringResource(R.string.network_details_last_scan_time), dateTimeFormatter.format(Date(a.lastScanTime)))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.network_details_history_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    val (color, textRes) = when (threatLevel) {
        ThreatLevel.SAFE -> SecuritySafe to R.string.threat_safe
        ThreatLevel.LOW -> SecurityLow to R.string.threat_low
        ThreatLevel.MEDIUM -> SecurityMedium to R.string.threat_medium
        ThreatLevel.HIGH -> SecurityHigh to R.string.threat_high
        ThreatLevel.CRITICAL -> SecurityCritical to R.string.threat_critical
        ThreatLevel.UNKNOWN -> SecurityUnknown to R.string.threat_unknown
    }
    val text = stringResource(textRes)

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

@Composable
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

/**
 * ????????? ???????? ? ???????? ???? Wi?Fi.
 * ?????????? ?????? ?????? ? ????????? ???? ? 3D-????????? ??????????.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDetailsModal(
    network: WifiScanResult?,
    details: NetworkCardDetails?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (network == null) return

    val statusColor = when (network.threatLevel) {
        ThreatLevel.SAFE -> SecuritySafe
        ThreatLevel.LOW -> SecurityLow
        ThreatLevel.MEDIUM -> SecurityMedium
        ThreatLevel.HIGH -> SecurityHigh
        ThreatLevel.CRITICAL -> SecurityCritical
        ThreatLevel.UNKNOWN -> SecurityUnknown
    }

    // ???????? ????????? (alpha)
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 400),
        label = "modalAlpha"
    )
    
    // 3D ???????? ??? ?????????
    val rotationY = remember { androidx.compose.animation.core.Animatable(90f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        rotationY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = 400f
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(1000f)
            .testTag(UiTestTags.NETWORK_DETAILS_MODAL)
            .clickable(onClick = onDismiss)
            .background(Color.Black.copy(alpha = 0.6f * alpha)) // ?????????? ????
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .align(Alignment.Center)
                .graphicsLayer {
                    this.rotationY = rotationY.value
                    this.alpha = alpha
                    this.cameraDistance = 16f * density // ?????? 3D ???????????
                    
                    // ????????? ?????? ??? ???????? 3D ???????
                    if (rotationY.value > 0) {
                        this.rotationZ = rotationY.value / 10f
                    }
                }
                .clickable(enabled = false) { /* ????????????? ???????? ??? ????? ?? ???????? */ },
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = WifiGuardElevation.Level7
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
            ) {
                // ????????? ? ??????? ????????
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(statusColor.copy(alpha = 0.1f))
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = network.ssid.ifEmpty { stringResource(R.string.network_hidden) },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ThreatBadge(threatLevel = network.threatLevel)
                    }
                    IconButton(
                        modifier = Modifier.testTag(UiTestTags.NETWORK_DETAILS_MODAL_CLOSE),
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.common_close),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // ??????? ? ??????????
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(20.dp)
                ) {
                    NetworkCardBackContent(
                        network = network,
                        statusColor = statusColor,
                        details = details,
                        isModal = true
                    )
                }
            }
        }
    }
}