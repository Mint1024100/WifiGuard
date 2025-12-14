package com.wifiguard.core.data.wifi

import com.wifiguard.core.data.local.entity.ThreatEntity
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel

/**
 * Утилита для вычисления уровня угрозы по набору известных угроз.
 *
 * Принцип:
 * - Если есть нерешённые угрозы в БД — берём максимальную серьёзность (severity).
 * - Если угроз нет — используем fallback по типу безопасности сети.
 * - Если fallback неинформативен (UNKNOWN) — возвращаем HIGH как безопасное предупреждение.
 */
internal object ThreatLevelSelector {
    fun calculateMaxThreatLevel(
        unresolvedThreats: List<ThreatEntity>,
        securityType: SecurityType
    ): ThreatLevel {
        val maxFromThreats = unresolvedThreats
            .maxByOrNull { it.severity.getNumericValue() }
            ?.severity

        if (maxFromThreats != null) {
            return maxFromThreats
        }

        val fallback = ThreatLevel.fromSecurityType(securityType)
        return if (fallback == ThreatLevel.UNKNOWN) ThreatLevel.HIGH else fallback
    }
}





