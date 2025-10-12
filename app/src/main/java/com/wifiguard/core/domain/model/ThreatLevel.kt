package com.wifiguard.core.domain.model

/**
 * Уровень угрозы для Wi-Fi сети
 */
enum class ThreatLevel {
    LOW,        // Низкий уровень угрозы
    MEDIUM,     // Средний уровень угрозы
    HIGH,       // Высокий уровень угрозы
    CRITICAL,   // Критический уровень угрозы
    UNKNOWN     // Неизвестный уровень угрозы
}
