package com.wifiguard.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Уровень серьёзности угрозы
 */
@Parcelize
enum class ThreatSeverity(val displayName: String, val color: String) : Parcelable {
    HIGH("Высокий", "#FF5722"),    // Высокий
    MEDIUM("Средний", "#FF9800"),  // Средний
    LOW("Низкий", "#FFC107")       // Низкий
}