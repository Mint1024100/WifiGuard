package com.wifiguard.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wifiguard.core.domain.model.ThreatLevel

/**
 * Индикатор статуса безопасности
 */
@Composable
fun SecurityStatusIndicator(
    threatLevel: ThreatLevel,
    modifier: Modifier = Modifier
) {
    val (color, icon, text) = when (threatLevel) {
        ThreatLevel.SAFE -> Triple(
            Color(0xFF4CAF50),
            Icons.Default.Security,
            "Безопасно"
        )
        ThreatLevel.LOW -> Triple(
            Color(0xFF8BC34A),
            Icons.Default.Security,
            "Низкий риск"
        )
        ThreatLevel.MEDIUM -> Triple(
            Color(0xFFFF9800),
            Icons.Default.Warning,
            "Средний риск"
        )
        ThreatLevel.HIGH -> Triple(
            Color(0xFFFF5722),
            Icons.Default.Warning,
            "Высокий риск"
        )
        ThreatLevel.CRITICAL -> Triple(
            MaterialTheme.colorScheme.error,
            Icons.Default.Warning,
            "Критический риск"
        )
        ThreatLevel.UNKNOWN -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.Security,
            "Неизвестно"
        )
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}