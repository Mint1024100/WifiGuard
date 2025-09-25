package com.wifiguard.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wifiguard.core.ui.theme.*

/**
 * Перечисление типов статусов для Wi-Fi анализатора
 */
enum class StatusType {
    SECURITY_HIGH,      // Высокий уровень безопасности  
    SECURITY_MEDIUM,    // Средний уровень безопасности
    SECURITY_LOW,       // Низкий уровень безопасности
    SECURITY_UNKNOWN,   // Неизвестная безопасность
    
    SIGNAL_EXCELLENT,   // Отличный сигнал
    SIGNAL_GOOD,        // Хороший сигнал
    SIGNAL_FAIR,        // Средний сигнал
    SIGNAL_POOR,        // Плохой сигнал
    SIGNAL_NONE,        // Нет сигнала
    
    CONNECTION_ACTIVE,      // Активное подключение
    CONNECTION_SCANNING,    // Сканирование
    CONNECTION_DISCONNECTED,// Отключено
    CONNECTION_ERROR,       // Ошибка подключения
    
    CUSTOM              // Кастомный статус
}

/**
 * Стиль отображения StatusIndicator
 */
enum class StatusStyle {
    DOT,        // Только точка (индикатор)
    CHIP,       // Chip с текстом и иконкой
    BADGE,      // Минимальный badge
    DETAILED    // Подробный статус с дополнительной информацией
}

/**
 * Универсальный компонент для отображения статусов
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusIndicator(
    type: StatusType,
    style: StatusStyle = StatusStyle.CHIP,
    text: String? = null,
    secondaryText: String? = null,
    customColor: Color? = null,
    customIcon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    animated: Boolean = true
) {
    // Определение цвета на основе типа
    val indicatorColor by animateColorAsState(
        targetValue = customColor ?: when (type) {
            StatusType.SECURITY_HIGH -> MaterialTheme.wifiColors.securityHigh
            StatusType.SECURITY_MEDIUM -> MaterialTheme.wifiColors.securityMedium
            StatusType.SECURITY_LOW -> MaterialTheme.wifiColors.securityLow
            StatusType.SECURITY_UNKNOWN -> MaterialTheme.wifiColors.securityUnknown
            
            StatusType.SIGNAL_EXCELLENT -> MaterialTheme.wifiColors.signalExcellent
            StatusType.SIGNAL_GOOD -> MaterialTheme.wifiColors.signalGood
            StatusType.SIGNAL_FAIR -> MaterialTheme.wifiColors.signalFair
            StatusType.SIGNAL_POOR -> MaterialTheme.wifiColors.signalPoor
            StatusType.SIGNAL_NONE -> MaterialTheme.wifiColors.signalNone
            
            StatusType.CONNECTION_ACTIVE -> MaterialTheme.wifiColors.statusActive
            StatusType.CONNECTION_SCANNING -> MaterialTheme.wifiColors.statusScanning
            StatusType.CONNECTION_DISCONNECTED -> MaterialTheme.wifiColors.statusDisconnected
            StatusType.CONNECTION_ERROR -> MaterialTheme.colorScheme.error
            
            StatusType.CUSTOM -> MaterialTheme.colorScheme.primary
        },
        animationSpec = if (animated) tween(300) else tween(0)
    )
    
    // Определение иконки
    val icon = customIcon ?: when (type) {
        StatusType.SECURITY_HIGH -> Icons.Filled.Security
        StatusType.SECURITY_MEDIUM -> Icons.Filled.Shield
        StatusType.SECURITY_LOW -> Icons.Filled.Warning
        StatusType.SECURITY_UNKNOWN -> Icons.Filled.Help
        
        StatusType.SIGNAL_EXCELLENT -> Icons.Filled.SignalWifi4Bar
        StatusType.SIGNAL_GOOD -> Icons.Filled.NetworkWifi
        StatusType.SIGNAL_FAIR -> Icons.Filled.NetworkWifi
        StatusType.SIGNAL_POOR -> Icons.Filled.SignalWifiOff
        StatusType.SIGNAL_NONE -> Icons.Filled.SignalWifiConnectedNoInternet4
        
        StatusType.CONNECTION_ACTIVE -> Icons.Filled.Wifi
        StatusType.CONNECTION_SCANNING -> Icons.Filled.WifiFind
        StatusType.CONNECTION_DISCONNECTED -> Icons.Filled.WifiOff
        StatusType.CONNECTION_ERROR -> Icons.Filled.Error
        
        StatusType.CUSTOM -> Icons.Filled.Info
    }
    
    // Отображение в зависимости от стиля
    when (style) {
        StatusStyle.DOT -> {
            StatusDot(
                color = indicatorColor,
                modifier = modifier,
                onClick = onClick
            )
        }
        
        StatusStyle.CHIP -> {
            StatusChip(
                type = type,
                text = text ?: getDefaultStatusText(type),
                icon = icon,
                color = indicatorColor,
                modifier = modifier,
                onClick = onClick,
                animated = animated
            )
        }
        
        StatusStyle.BADGE -> {
            StatusBadge(
                text = text ?: getShortStatusText(type),
                color = indicatorColor,
                modifier = modifier,
                onClick = onClick
            )
        }
        
        StatusStyle.DETAILED -> {
            StatusDetailed(
                type = type,
                text = text ?: getDefaultStatusText(type),
                secondaryText = secondaryText,
                icon = icon,
                color = indicatorColor,
                modifier = modifier,
                onClick = onClick,
                animated = animated
            )
        }
    }
}

/**
 * Простой цветовой индикатор (точка)
 */
@Composable
private fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val dotModifier = if (onClick != null) {
        modifier.size(8.dp)
    } else {
        modifier.size(8.dp)
    }
    
    Box(
        modifier = dotModifier
            .clip(CircleShape)
            .background(color)
            .run {
                if (onClick != null) clickable { onClick() } else this
            }
    )
}

/**
 * Chip-стиль статуса с иконкой и текстом
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusChip(
    type: StatusType,
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    animated: Boolean = true
) {
    val containerColor = color.copy(alpha = 0.12f)
    val contentColor = color
    
    if (onClick != null) {
        AssistChip(
            onClick = onClick,
            label = { 
                AnimatedContent(
                    targetState = text,
                    transitionSpec = if (animated) {
                        fadeIn() togetherWith fadeOut()
                    } else {
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    }
                ) { targetText ->
                    Text(
                        text = targetText,
                        style = MaterialTheme.wifiTypography.statusIndicator,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            leadingIcon = {
                AnimatedContent(
                    targetState = icon,
                    transitionSpec = if (animated) {
                        scaleIn() togetherWith scaleOut()
                    } else {
                        scaleIn(tween(0)) togetherWith scaleOut(tween(0))
                    }
                ) { targetIcon ->
                    Icon(
                        imageVector = targetIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = containerColor,
                labelColor = contentColor,
                leadingIconContentColor = contentColor
            ),
            modifier = modifier
        )
    } else {
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(8.dp),
            modifier = modifier
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = text,
                    style = MaterialTheme.wifiTypography.statusIndicator,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Минимальный badge со счетчиком или коротким текстом
 */
@Composable
private fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    BadgedBox(
        badge = {
            Badge(
                containerColor = color,
                contentColor = MaterialTheme.colorScheme.onError,
                modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.wifiTypography.captionEmphasized
                )
            }
        },
        modifier = modifier
    ) {
        // Пустое содержимое - badge отображается отдельно
    }
}

/**
 * Подробный статус с основным и дополнительным текстом
 */
@Composable
private fun StatusDetailed(
    type: StatusType,
    text: String,
    secondaryText: String?,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    animated: Boolean = true
) {
    val containerColor = color.copy(alpha = 0.08f)
    val contentColor = color
    
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier.run {
            if (onClick != null) clickable { onClick() } else this
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.wifiTypography.statusIndicator,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                secondaryText?.let { secondary ->
                    Text(
                        text = secondary,
                        style = MaterialTheme.wifiTypography.caption,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Вспомогательные функции для получения текста по умолчанию
 */
private fun getDefaultStatusText(type: StatusType): String {
    return when (type) {
        StatusType.SECURITY_HIGH -> "Безопасно"
        StatusType.SECURITY_MEDIUM -> "Умеренно"
        StatusType.SECURITY_LOW -> "Небезопасно"
        StatusType.SECURITY_UNKNOWN -> "Неизвестно"
        
        StatusType.SIGNAL_EXCELLENT -> "Отличный"
        StatusType.SIGNAL_GOOD -> "Хороший"
        StatusType.SIGNAL_FAIR -> "Средний"
        StatusType.SIGNAL_POOR -> "Слабый"
        StatusType.SIGNAL_NONE -> "Нет сигнала"
        
        StatusType.CONNECTION_ACTIVE -> "Подключено"
        StatusType.CONNECTION_SCANNING -> "Поиск..."
        StatusType.CONNECTION_DISCONNECTED -> "Отключено"
        StatusType.CONNECTION_ERROR -> "Ошибка"
        
        StatusType.CUSTOM -> "Статус"
    }
}

private fun getShortStatusText(type: StatusType): String {
    return when (type) {
        StatusType.SECURITY_HIGH -> "✓"
        StatusType.SECURITY_MEDIUM -> "~"
        StatusType.SECURITY_LOW -> "!"
        StatusType.SECURITY_UNKNOWN -> "?"
        
        StatusType.SIGNAL_EXCELLENT -> "4"
        StatusType.SIGNAL_GOOD -> "3"
        StatusType.SIGNAL_FAIR -> "2"
        StatusType.SIGNAL_POOR -> "1"
        StatusType.SIGNAL_NONE -> "0"
        
        StatusType.CONNECTION_ACTIVE -> "ON"
        StatusType.CONNECTION_SCANNING -> "..."
        StatusType.CONNECTION_DISCONNECTED -> "OFF"
        StatusType.CONNECTION_ERROR -> "ERR"
        
        StatusType.CUSTOM -> "•"
    }
}

// Preview компонентов
@Preview(showBackground = true)
@Composable
fun StatusIndicatorPreview() {
    WifiGuardTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Security Status Examples", style = MaterialTheme.typography.headlineSmall)
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusIndicator(StatusType.SECURITY_HIGH, StatusStyle.DOT)
                StatusIndicator(StatusType.SECURITY_MEDIUM, StatusStyle.DOT)
                StatusIndicator(StatusType.SECURITY_LOW, StatusStyle.DOT)
            }
            
            StatusIndicator(StatusType.SECURITY_HIGH, StatusStyle.CHIP)
            StatusIndicator(StatusType.CONNECTION_SCANNING, StatusStyle.CHIP)
            
            StatusIndicator(
                StatusType.SECURITY_LOW,
                StatusStyle.DETAILED,
                secondaryText = "WEP encryption detected"
            )
        }
    }
}
