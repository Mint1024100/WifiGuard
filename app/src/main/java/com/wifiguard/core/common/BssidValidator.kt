package com.wifiguard.core.common

/**
 * Утилиты для валидации BSSID (MAC-адреса точки доступа) в контексте бизнес-логики.
 *
 * ВАЖНО:
 * - Для хранения/обновления сущностей `wifi_networks` нам нужен стабильный уникальный идентификатор.
 * - Значения вроде "unknown" или "02:00:00:00:00:00" не подходят и приводят к коллизиям.
 */
internal object BssidValidator {
    // Строгая проверка MAC-адреса вида "AA:BB:CC:DD:EE:FF"
    private val MAC_REGEX = Regex("^[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){5}$")

    /**
     * Проверяет, можно ли использовать BSSID как ключ для хранения/обновления данных о сети.
     */
    fun isValidForStorage(bssid: String): Boolean {
        if (bssid.isBlank()) return false
        if (bssid.equals("unknown", ignoreCase = true)) return false
        if (!MAC_REGEX.matches(bssid)) return false

        val normalized = bssid.lowercase()

        // Плейсхолдер/рандомизированный MAC, который встречается на некоторых версиях Android/девайсах.
        if (normalized == "02:00:00:00:00:00") return false
        // Нулевой MAC адрес не должен использоваться как ключ.
        if (normalized == "00:00:00:00:00:00") return false

        // BSSID должен быть unicast: multicast/групповые адреса не подходят как идентификатор точки доступа.
        val firstByte = normalized.substring(0, 2).toIntOrNull(16) ?: return false
        val isMulticast = (firstByte and 0x01) == 0x01
        if (isMulticast) return false

        return true
    }
}












