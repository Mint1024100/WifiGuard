package com.wifiguard.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wifiguard.core.domain.model.Freshness
import java.util.concurrent.TimeUnit

/**
 * Индикатор свежести данных с цветовой кодировкой
 * 
 * РЕШЕНИЕ ПРОБЛЕМЫ 3.1: Визуальная индикация возраста кэшированных данных
 * для информирования пользователя о актуальности информации
 */
@Composable
fun DataFreshnessIndicator(
    freshness: Freshness,
    ageMillis: Long,
    modifier: Modifier = Modifier,
    showDetails: Boolean = true
) {
    val freshnessColor = when (freshness) {
        Freshness.FRESH -> Color(0xFF4CAF50)      // Зеленый
        Freshness.STALE -> Color(0xFFFF9800)      // Оранжевый
        Freshness.EXPIRED -> Color(0xFFF44336)    // Красный
        Freshness.UNKNOWN -> Color(0xFF9E9E9E)    // Серый
    }
    
    val freshnessIcon = when (freshness) {
        Freshness.FRESH -> Icons.Filled.CheckCircle
        Freshness.STALE -> Icons.Filled.Warning
        Freshness.EXPIRED -> Icons.Filled.Error
        Freshness.UNKNOWN -> Icons.Filled.Warning
    }
    
    val ageText = formatAge(ageMillis)
    
    Row(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Цветной индикатор
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(freshnessColor, CircleShape)
        )
        
        // Иконка статуса
        Icon(
            imageVector = freshnessIcon,
            contentDescription = freshness.getDescription(),
            tint = freshnessColor,
            modifier = Modifier.size(16.dp)
        )
        
        // Текстовое описание
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = freshness.getDescription(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = freshnessColor
            )
            
            if (showDetails) {
                Text(
                    text = "Обновлено: $ageText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Компактный вариант индикатора (только иконка и цвет)
 */
@Composable
fun CompactDataFreshnessIndicator(
    freshness: Freshness,
    modifier: Modifier = Modifier
) {
    val freshnessColor = when (freshness) {
        Freshness.FRESH -> Color(0xFF4CAF50)
        Freshness.STALE -> Color(0xFFFF9800)
        Freshness.EXPIRED -> Color(0xFFF44336)
        Freshness.UNKNOWN -> Color(0xFF9E9E9E)
    }
    
    val freshnessIcon = when (freshness) {
        Freshness.FRESH -> Icons.Filled.CheckCircle
        Freshness.STALE -> Icons.Filled.Warning
        Freshness.EXPIRED -> Icons.Filled.Error
        Freshness.UNKNOWN -> Icons.Filled.Warning
    }
    
    Icon(
        imageVector = freshnessIcon,
        contentDescription = freshness.getDescription(),
        tint = freshnessColor,
        modifier = modifier.size(20.dp)
    )
}

/**
 * Форматировать возраст данных в читаемый вид
 */
private fun formatAge(ageMillis: Long): String {
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ageMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ageMillis)
    val hours = TimeUnit.MILLISECONDS.toHours(ageMillis)
    
    return when {
        seconds < 60 -> "$seconds сек назад"
        minutes < 60 -> "$minutes мин назад"
        hours < 24 -> "$hours ч назад"
        else -> {
            val days = TimeUnit.MILLISECONDS.toDays(ageMillis)
            "$days дн назад"
        }
    }
}





















