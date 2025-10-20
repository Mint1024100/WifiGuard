package com.wifiguard.core.domain.model

/**
 * Данные для результатов подсчета сетей по уровню угроз
 */
data class ThreatLevelCount(
    val threat_level: ThreatLevel,
    val count: Int
)