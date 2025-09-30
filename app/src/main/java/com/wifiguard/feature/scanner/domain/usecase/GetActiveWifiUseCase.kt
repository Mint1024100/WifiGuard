package com.wifiguard.feature.scanner.domain.usecase

import com.wifiguard.feature.scanner.domain.model.WifiInfo
import com.wifiguard.feature.scanner.domain.model.SecurityType
import com.wifiguard.feature.scanner.domain.repository.WifiScannerRepository
import javax.inject.Inject
import kotlin.random.Random

/**
 * Use case для получения списка активных WiFi сетей
 * Предоставляет фиктивный список сетей для тестирования
 */
class GetActiveWifiUseCase @Inject constructor(
    private val wifiRepository: WifiScannerRepository
) {
    
    /**
     * Получает список доступных WiFi сетей
     * На данном этапе возвращает фиктивные данные
     */
    suspend operator fun invoke(): List<WifiInfo> {
        // Возвращаем фиктивный список сетей для тестирования
        return generateMockWifiNetworks()
    }
    
    /**
     * Генерирует фиктивный список WiFi сетей для демонстрации
     */
    private fun generateMockWifiNetworks(): List<WifiInfo> {
        val currentTime = System.currentTimeMillis()
        
        return listOf(
            WifiInfo(
                ssid = "Home_WiFi_5G",
                bssid = "00:1A:2B:3C:4D:5E",
                capabilities = "[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]",
                level = -45,
                frequency = 5180,
                timestamp = currentTime - Random.nextLong(1000, 10000),
                securityType = SecurityType.WPA2,
                signalStrength = -45,
                channel = 36,
                bandwidth = "80 MHz",
                isHidden = false
            ),
            WifiInfo(
                ssid = "TP-Link_2.4G",
                bssid = "A4:2B:B0:D5:8F:12",
                capabilities = "[WPA2-PSK-CCMP+TKIP][WPS][ESS]",
                level = -67,
                frequency = 2442,
                timestamp = currentTime - Random.nextLong(2000, 15000),
                securityType = SecurityType.WPA2,
                signalStrength = -67,
                channel = 7,
                bandwidth = "20 MHz",
                isHidden = false
            ),
            WifiInfo(
                ssid = "Office_Network",
                bssid = "B8:27:EB:12:34:56",
                capabilities = "[WPA3-SAE-CCMP][RSN-SAE-CCMP][MFPR][MFPC][ESS]",
                level = -52,
                frequency = 5240,
                timestamp = currentTime - Random.nextLong(500, 5000),
                securityType = SecurityType.WPA3,
                signalStrength = -52,
                channel = 48,
                bandwidth = "160 MHz",
                isHidden = false
            ),
            WifiInfo(
                ssid = "Guest_Network",
                bssid = "C0:25:E9:AB:CD:EF",
                capabilities = "[ESS]",
                level = -72,
                frequency = 2462,
                timestamp = currentTime - Random.nextLong(3000, 20000),
                securityType = SecurityType.OPEN,
                signalStrength = -72,
                channel = 11,
                bandwidth = "20 MHz",
                isHidden = false
            ),
            WifiInfo(
                ssid = "Neighbor_WiFi",
                bssid = "D4:6E:0E:98:76:54",
                capabilities = "[WPA-PSK-CCMP+TKIP][WPA2-PSK-CCMP+TKIP][ESS]",
                level = -78,
                frequency = 2437,
                timestamp = currentTime - Random.nextLong(1500, 12000),
                securityType = SecurityType.WPA2,
                signalStrength = -78,
                channel = 6,
                bandwidth = "40 MHz",
                isHidden = false
            ),
            WifiInfo(
                ssid = "", // Скрытая сеть
                bssid = "E8:DE:27:11:22:33",
                capabilities = "[WPA2-PSK-CCMP][ESS]",
                level = -81,
                frequency = 5785,
                timestamp = currentTime - Random.nextLong(4000, 25000),
                securityType = SecurityType.WPA2,
                signalStrength = -81,
                channel = 157,
                bandwidth = "80 MHz",
                isHidden = true
            ),
            WifiInfo(
                ssid = "Coffee_Shop_Free",
                bssid = "F2:9A:36:44:55:66",
                capabilities = "[ESS]",
                level = -85,
                frequency = 2412,
                timestamp = currentTime - Random.nextLong(6000, 30000),
                securityType = SecurityType.OPEN,
                signalStrength = -85,
                channel = 1,
                bandwidth = "20 MHz",
                isHidden = false
            ),
            WifiInfo(
                ssid = "Legacy_WEP_Network",
                bssid = "AA:BB:CC:DD:EE:FF",
                capabilities = "[WEP][ESS]",
                level = -89,
                frequency = 2472,
                timestamp = currentTime - Random.nextLong(8000, 35000),
                securityType = SecurityType.WEP,
                signalStrength = -89,
                channel = 13,
                bandwidth = "20 MHz",
                isHidden = false
            )
        ).shuffled() // Перемешиваем для имитации реального сканирования
    }
}