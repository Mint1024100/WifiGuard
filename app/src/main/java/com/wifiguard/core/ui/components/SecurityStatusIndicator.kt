package com.wifiguard.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.ui.theme.*

/**
 * Индикатор статуса безопасности (Modern Style)
 */
@Composable
fun SecurityStatusIndicator(
    threatLevel: ThreatLevel,
    modifier: Modifier = Modifier
) {
    val (color, icon, text) = when (threatLevel) {
        ThreatLevel.SAFE -> Triple(
            SecuritySafe,
            Icons.Default.GppGood,
            "Безопасно"
        )
        ThreatLevel.LOW -> Triple(
            SecurityLow,
            Icons.Default.Security,
            "Низкий риск"
        )
        ThreatLevel.MEDIUM -> Triple(
            SecurityMedium,
            Icons.Default.Warning,
            "Средний риск"
        )
        ThreatLevel.HIGH -> Triple(
            SecurityHigh,
            Icons.Default.ReportProblem,
            "Высокий риск"
        )
        ThreatLevel.CRITICAL -> Triple(
            SecurityCritical,
            Icons.Default.GppBad,
            "Критический риск"
        )
        ThreatLevel.UNKNOWN -> Triple(
            SecurityUnknown,
            Icons.Default.Security,
            "Неизвестно"
        )
    }
    
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(percent = 50), // Pill shape
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}