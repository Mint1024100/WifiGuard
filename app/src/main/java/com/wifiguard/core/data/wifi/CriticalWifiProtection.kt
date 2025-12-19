package com.wifiguard.core.data.wifi

import com.wifiguard.core.domain.model.ThreatLevel

/**
 * Утилита для принятия решений по авто-защите при подключении к опасной сети.
 *
 * ВАЖНО:
 * - Логика вынесена в отдельный класс, чтобы её было легко тестировать.
 * - Реальные действия (disconnect/disable) выполняются отдельно (в receiver/service).
 */
object CriticalWifiProtection {

    /**
     * Нужно ли пытаться выполнить авто-отключение/разрыв соединения.
     */
    fun shouldAttemptAutoDisable(threatLevel: ThreatLevel, userConsentEnabled: Boolean): Boolean {
        return userConsentEnabled && threatLevel == ThreatLevel.CRITICAL
    }

    /**
     * Можно ли пытаться выключить Wi‑Fi программно по версии SDK.
     *
     * На Android 10+ (API 29+) управление Wi‑Fi для обычных приложений часто ограничено.
     */
    fun canDisableWifiBySdk(sdkInt: Int): Boolean {
        return sdkInt < 29
    }
}


