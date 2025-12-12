package com.wifiguard.core.data.wifi

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Анализатор возможностей Wi-Fi сетей
 * 
 * ОПТИМИЗИРОВАНО: Singleton через Hilt DI + OUI lookup через HashMap (O(1) вместо O(n))
 * 
 * РЕШАЕМАЯ ПРОБЛЕМА: Ранее создавался новый экземпляр для каждой сети (память + CPU),
 * и использовался огромный when statement (~200 строк) для поиска производителя.
 * Теперь один экземпляр на все приложение и быстрый O(1) поиск через HashMap.
 */
@Singleton
class WifiCapabilitiesAnalyzer @Inject constructor() {

    companion object {
        /**
         * OUI (Organizationally Unique Identifier) база данных
         * Ключ: первые 8 символов MAC-адреса (формат "XX:XX:XX")
         * Значение: название производителя
         * 
         * HashMap обеспечивает O(1) поиск вместо O(n) через when statement
         */
        private val ouiMap: Map<String, String> = buildMap {
            // Виртуализация
            put("00:50:56", "VMware")
            put("00:0C:29", "VMware")
            put("00:1C:42", "Parallels")
            put("08:00:27", "VirtualBox")
            put("00:15:5D", "Microsoft")
            put("00:16:3E", "Xen")
            
            // Сетевое оборудование
            put("00:1B:21", "Intel")
            
            // Apple (диапазоны OUI)
            // Вместо 200+ строк with statement используем цикл для генерации всех адресов
            for (i in 0x00..0xFF) {
                val suffix = i.toString(16).uppercase().padStart(2, '0')
                put("00:1F:5B", "Apple")  // Фиксированные префиксы
                put("00:23:12", "Apple")
                put("00:25:00", "Apple")
                put("00:26:BB", "Apple")
                put("00:26:B0", "Apple")
                put("00:26:4A", "Apple")
                put("00:26:08", "Apple")
                
                // Диапазон 00:25:4B - 00:25:FF (всё Apple)
                put("00:25:$suffix", "Apple")
            }
            
            // Другие популярные производители роутеров
            put("00:1D:7E", "TP-Link")
            put("00:27:22", "TP-Link")
            put("A4:2B:B0", "TP-Link")
            put("C0:4A:00", "TP-Link")
            put("00:18:E7", "D-Link")
            put("14:D6:4D", "D-Link")
            put("28:10:7B", "D-Link")
            put("00:1B:2F", "ASUS")
            put("04:D9:F5", "ASUS")
            put("30:5A:3A", "ASUS")
            put("00:1E:8C", "Netgear")
            put("00:26:F2", "Netgear")
            put("A0:63:91", "Netgear")
            put("00:1A:70", "Linksys")
            put("00:21:29", "Linksys")
            put("00:25:9C", "Linksys")
            put("00:14:BF", "Belkin")
            put("08:86:3B", "Belkin")
            put("94:10:3E", "Belkin")
            put("00:14:6C", "Netgear")
            put("00:18:4D", "Netgear")
            put("00:26:F3", "Netgear")
            put("00:22:3F", "Huawei")
            put("00:25:9E", "Huawei")
            put("E0:19:1D", "Huawei")
            put("00:1C:10", "Xiaomi")
            put("34:CE:00", "Xiaomi")
            put("64:09:80", "Xiaomi")
        }
    }

    /**
     * Получить производителя устройства по MAC-адресу (O(1) поиск)
     * 
     * @param bssid MAC-адрес устройства в формате "XX:XX:XX:XX:XX:XX"
     * @return название производителя или null если не найдено
     */
    fun getVendorFromBssid(bssid: String?): String? {
        if (bssid.isNullOrEmpty() || bssid.length < 8) return null
        
        // Берем первые 8 символов (XX:XX:XX) и нормализуем
        val oui = bssid.substring(0, 8).uppercase()
        
        // O(1) поиск в HashMap
        return ouiMap[oui]
    }

    /**
     * Получить номер канала по частоте
     */
    fun getChannelFromFrequency(frequency: Int): Int {
        return when {
            frequency in 2412..2484 -> {
                // 2.4 GHz band
                (frequency - 2412) / 5 + 1
            }
            frequency in 5170..5825 -> {
                // 5 GHz band
                (frequency - 5000) / 5
            }
            frequency in 5925..7125 -> {
                // 6 GHz band
                (frequency - 5925) / 5
            }
            else -> 0
        }
    }
}