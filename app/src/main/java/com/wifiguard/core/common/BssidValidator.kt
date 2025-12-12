package com.wifiguard.core.common

/**
 * Утилиты для валидации BSSID (MAC-адреса точки доступа) в контексте бизнес-логики.
 *
 * ВАЖНО:
 * - Для хранения/обновления сущностей `wifi_networks` нам нужен стабильный уникальный идентификатор.
 * - Значения вроде "unknown" или "02:00:00:00:00:00" не подходят и приводят к коллизиям.
 */
internal object BssidValidator {
    /**
     * Проверяет, можно ли использовать BSSID как ключ для хранения/обновления данных о сети.
     */
    fun isValidForStorage(bssid: String): Boolean {
        if (bssid.isBlank()) return false
        if (bssid.equals("unknown", ignoreCase = true)) return false
        // Плейсхолдер/рандомизированный MAC, который встречается на некоторых версиях Android/девайсах.
        if (bssid == "02:00:00:00:00:00") return false
        return true
    }
}



