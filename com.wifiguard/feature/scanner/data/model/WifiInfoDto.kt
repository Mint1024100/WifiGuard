package com.wifiguard.feature.scanner.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object для Wi-Fi сети на Data слое
 * 
 * Представляет "сырые" данные, полученные от Android Wi-Fi API,
 * до обработки и анализа безопасности на Domain слое.
 * 
 * Используется для:
 * - Передачи данных между Data Source и Repository
 * - Сериализации/десериализации при работе с внешними API
 * - Кэширования результатов сканирования
 * - Сохранения в локальную базу данных
 * 
 * @author WifiGuard Data Team
 * @since 1.0.0
 */
data class WifiInfoDto(
    /**
     * SSID (Service Set Identifier) - имя Wi-Fi сети
     * Может быть пустым для скрытых сетей
     */
    @SerializedName("ssid")
    val ssid: String,
    
    /**
     * BSSID (Basic Service Set Identifier) - MAC-адрес точки доступа
     * Уникальный идентификатор каждой точки доступа
     */
    @SerializedName("bssid")
    val bssid: String,
    
    /**
     * Строка capabilities из Android ScanResult
     * Содержит информацию о поддерживаемых протоколах безопасности
     * Пример: "[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS][WPS]"
     */
    @SerializedName("capabilities")
    val capabilities: String,
    
    /**
     * Частота сети в MHz
     * 2400-2500 МГц - диапазон 2.4 ГГц
     * 5000-6000 МГц - диапазон 5 ГГц
     * 5925-7125 МГц - диапазон 6 ГГц (Wi-Fi 6E/7)
     */
    @SerializedName("frequency")
    val frequency: Int,
    
    /**
     * Уровень сигнала в dBm (децибел-милливатт)
     * Обычно от -100 (очень слабый) до -30 (очень сильный)
     * 
     * Интерпретация:
     * -30 до -50 dBm: Отличный сигнал
     * -50 до -60 dBm: Хороший сигнал
     * -60 до -70 dBm: Удовлетворительный
     * -70 до -80 dBm: Слабый
     * -80 до -90 dBm: Очень слабый
     * Ниже -90 dBm: Критически слабый
     */
    @SerializedName("level")
    val level: Int,
    
    /**
     * Временная метка последнего обнаружения сети (в миллисекундах)
     * Используется для определения "свежести" данных
     */
    @SerializedName("timestamp")
    val timestamp: Long,
    
    /**
     * Ширина канала в MHz (20, 40, 80, 160)
     * Влияет на пропускную способность сети
     */
    @SerializedName("channelWidth")
    val channelWidth: Int = 20,
    
    /**
     * Флаг, указывающий что сеть работает в диапазоне 5 ГГц
     * True для частот 5000-6000 МГц
     */
    @SerializedName("is5GHz")
    val is5GHz: Boolean = false,
    
    /**
     * Флаг, указывающий что сеть работает в диапазоне 6 ГГц
     * True для частот 5925-7125 МГц (Wi-Fi 6E/7)
     */
    @SerializedName("is6GHz")
    val is6GHz: Boolean = false,
    
    /**
     * Дополнительные метаданные сети
     * Может содержать информацию о производителе точки доступа,
     * версии Wi-Fi протокола и другие технические данные
     */
    @SerializedName("metadata")
    val metadata: Map<String, String> = emptyMap()
) {
    
    /**
     * Проверяет, является ли сеть скрытой (без имени)
     */
    fun isHiddenNetwork(): Boolean {
        return ssid.isBlank() || ssid == "<unknown ssid>"
    }
    
    /**
     * Определяет диапазон частот сети
     */
    fun getFrequencyBand(): String {
        return when (frequency) {
            in 2400..2500 -> "2.4 ГГц"
            in 5000..5900 -> "5 ГГц"
            in 5925..7125 -> "6 ГГц"
            else -> "Неизвестный ($frequency МГц)"
        }
    }
    
    /**
     * Вычисляет приблизительный номер канала
     */
    fun getChannelNumber(): Int {
        return when (frequency) {
            // 2.4 ГГц каналы
            in 2412..2484 -> (frequency - 2412) / 5 + 1
            // 5 ГГц каналы (упрощённый расчёт)
            in 5000..5900 -> (frequency - 5000) / 5
            // 6 ГГц каналы (упрощённый расчёт)
            in 5925..7125 -> (frequency - 5925) / 5 + 1
            else -> 0
        }
    }
    
    /**
     * Возвращает информацию о ширине канала в читаемом виде
     */
    fun getChannelWidthDescription(): String {
        return when (channelWidth) {
            20 -> "20 МГц (стандарт)"
            40 -> "40 МГц (широкий)"
            80 -> "80 МГц (очень широкий)"
            160 -> "160 МГц (сверхширокий)"
            else -> "$channelWidth МГц"
        }
    }
    
    /**
     * Проверяет, содержит ли capabilities определённый протокол
     */
    fun hasSecurityProtocol(protocol: String): Boolean {
        return capabilities.contains(protocol, ignoreCase = true)
    }
    
    /**
     * Определяет, поддерживает ли сеть WPS (Wi-Fi Protected Setup)
     */
    fun hasWPS(): Boolean {
        return hasSecurityProtocol("WPS")
    }
    
    /**
     * Проверяет поддержку Enterprise-режима (корпоративная аутентификация)
     */
    fun hasEnterpriseMode(): Boolean {
        return hasSecurityProtocol("EAP") || hasSecurityProtocol("Enterprise")
    }
    
    /**
     * Возвращает краткое описание технических характеристик сети
     */
    fun getTechnicalSummary(): String {
        val channelInfo = if (getChannelNumber() > 0) {
            "канал ${getChannelNumber()}"
        } else {
            "частота ${frequency} МГц"
        }
        
        return "${getFrequencyBand()}, $channelInfo, ${getChannelWidthDescription()}"
    }
    
    /**
     * Проверяет валидность данных DTO
     */
    fun isValid(): Boolean {
        return bssid.isNotBlank() && 
               frequency > 0 && 
               level < 0 && // dBm всегда отрицательный
               timestamp > 0
    }
    
    companion object {
        /**
         * Создаёт пустой/default объект для fallback ситуаций
         */
        fun createEmpty(bssid: String = "00:00:00:00:00:00"): WifiInfoDto {
            return WifiInfoDto(
                ssid = "<Неизвестная сеть>",
                bssid = bssid,
                capabilities = "",
                frequency = 2412, // Канал 1, 2.4 ГГц по умолчанию
                level = -100, // Минимальный уровень сигнала
                timestamp = System.currentTimeMillis()
            )
        }
    }
}