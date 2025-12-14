package com.wifiguard.core.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiNetwork
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.ui.theme.*
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

/**
 * Дополнительные данные для обратной стороны карточки (БД + история).
 *
 * ВАЖНО: типы только из core domain, чтобы не зависеть от feature-модулей.
 */
data class NetworkCardDetails(
    val dbNetwork: WifiNetwork? = null,
    val scanHistory: List<WifiScanResult> = emptyList(),
    val signalAnalytics: SignalAnalyticsUi? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * UI-модель аналитики сигнала (без зависимости от feature).
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
 * Карточка Wi‑Fi сети с 3D‑переворотом: front → back.
 *
 * Front: текущий компактный вид.\n
 * Back: расширенные данные (скан + БД + история/аналитика).
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

    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "networkCardFlip"
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
                // Чем больше, тем “дальше камера” и тем меньше искажение.
                cameraDistance = 12f * density
            }
        ) {
            if (!showBack) {
                NetworkCardFrontContent(
                    network = network,
                    statusColor = statusColor,
                    isCurrentNetwork = isCurrentNetwork
                )
            } else {
                // Компенсация зеркальности на обратной стороне.
                Box(modifier = Modifier.graphicsLayer {
                    this.rotationY = 180f
                }) {
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
                        tint = if (isCurrentNetwork) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = network.ssid.ifEmpty { "Скрытая сеть" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
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
        // Верхняя “полоса статуса”
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
                        text = network.ssid.ifEmpty { "Скрытая сеть" },
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
                    text = "Данные сканирования",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                DetailItem("BSSID", network.bssid)
                DetailItem("RSSI", "${network.level} dBm (${network.getSignalStrengthDescription()})")
                DetailItem("Частота", "${network.frequency} МГц")
                if (network.channel > 0) DetailItem("Канал", network.channel.toString())
                DetailItem("Безопасность", getSecurityTypeText(network.securityType))
                DetailItem("Capabilities", network.capabilities.ifEmpty { "—" })
                DetailItem("Стандарт", network.standard.name)
                DetailItem("Скрытая", if (network.isHidden) "Да" else "Нет")
                DetailItem("Подключена", if (network.isConnected) "Да" else "Нет")
                if (!network.vendor.isNullOrEmpty()) DetailItem("Vendor", network.vendor ?: "—")
                DetailItem("Время скана", dateTimeFormatter.format(Date(network.timestamp)))
                DetailItem("Тип скана", network.scanType.name)

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Данные из базы",
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
                                text = "Загрузка деталей…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // Показываем ошибку только если это реальная ошибка (не просто отсутствие сети в БД)
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
                            text = "Сеть ещё не сохранена в базе данных. Данные появятся после нескольких сканирований.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        val db = details.dbNetwork
                        DetailItem("First seen", dateTimeFormatter.format(Date(db.firstSeen)))
                        DetailItem("Last seen", dateTimeFormatter.format(Date(db.lastSeen)))
                        DetailItem("Known", if (db.isKnown) "Да" else "Нет")
                        DetailItem("Suspicious", if (db.isSuspicious) "Да" else "Нет")
                        if (!db.suspiciousReason.isNullOrEmpty()) {
                            DetailItem("Причина", db.suspiciousReason ?: "—")
                        }
                        DetailItem("Trust", db.trustLevel.name)
                        DetailItem("Detections", db.connectionCount.toString())
                    }

                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "История и аналитика",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val history = details?.scanHistory.orEmpty()
                if (history.isEmpty()) {
                    Text(
                        text = "История сканов пока отсутствует.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    DetailItem("Сканов", history.size.toString())
                    details?.signalAnalytics?.let { a ->
                        DetailItem("Средний RSSI", "${a.averageSignal} dBm")
                        DetailItem("Мин/Макс", "${a.minSignal} / ${a.maxSignal} dBm")
                        DetailItem("Вариация", String.format(Locale.getDefault(), "%.2f", a.signalVariation))
                        DetailItem("Последний", dateTimeFormatter.format(Date(a.lastScanTime)))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Нажмите ещё раз, чтобы закрыть детали.",
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

/**
 * Модальная карточка с деталями сети Wi‑Fi.
 * Появляется поверх списка с размытием фона и 3D-анимацией переворота.
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

    // Анимация появления (alpha)
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 400),
        label = "modalAlpha"
    )
    
    // 3D вращение при появлении
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
            .clickable(onClick = onDismiss)
            .background(Color.Black.copy(alpha = 0.6f * alpha)) // Затемнение фона
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .align(Alignment.Center)
                .graphicsLayer {
                    this.rotationY = rotationY.value
                    this.alpha = alpha
                    this.cameraDistance = 16f * density // Эффект 3D перспективы
                    
                    // Небольшой наклон для усиления 3D эффекта
                    if (rotationY.value > 0) {
                        this.rotationZ = rotationY.value / 10f
                    }
                }
                .clickable(enabled = false) { /* Предотвращаем закрытие при клике на карточку */ },
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
                // Заголовок с кнопкой закрытия
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
                            text = network.ssid.ifEmpty { "Скрытая сеть" },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ThreatBadge(threatLevel = network.threatLevel)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Контент с прокруткой
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