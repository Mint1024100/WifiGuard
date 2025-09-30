package com.wifiguard.feature.scanner.domain.usecase

import com.wifiguard.feature.scanner.domain.model.WifiInfo
import com.wifiguard.feature.scanner.domain.model.SecurityType
import com.wifiguard.feature.scanner.domain.repository.WifiScannerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.random.Random

/**
 * Use case для наблюдения за состоянием WiFi сетей
 * Предоставляет Flow с фиктивными данными для тестирования
 */
class ObserveWifiStateUseCase @Inject constructor(
    private val wifiRepository: WifiScannerRepository
) {
    
    /**
     * Наблюдает за списком доступных WiFi сетей
     * На данном этапе возвращает фиктивные данные через Flow
     */
    operator fun invoke(): Flow<List<WifiInfo>> = flow {
        while (true) {
            val networks = generateMockWifiNetworks()
            emit(networks)
            delay(5000) // Обновляем каждые 5 секунд
        }
    }
    
    /**
     * Наблюдает за состоянием WiFi (включен/выключен)
     */
    fun observeWifiEnabled(): Flow<Boolean> = flow {
        emit(true) // Всегда включен для тестирования
        while (true) {
            delay(10000) // Проверяем каждые 10 секунд
            emit(Random.nextBoolean() || true) // Преимущественно включен
        }
    }
    
    /**
     * Наблюдает за текущей подключенной сетью
     */
    fun observeCurrentWifi(): Flow<WifiInfo?> = flow {
        val connectedNetwork = WifiInfo(
            ssid = "Home_WiFi_5G",
            bssid = "00:1A:2B:3C:4D:5E",
            capabilities = "[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]",
            level = -45,
            frequency = 5180,
            timestamp = System.currentTimeMillis(),
            securityType = SecurityType.WPA2,
            signalStrength = -45,
            channel = 36,
            bandwidth = "80 MHz",
            isHidden = false
        )
        
        while (true) {
            // Имитируем смену уровня сигнала
            val updatedNetwork = connectedNetwork.copy(
                level = Random.nextInt(-60, -40),
                signalStrength = Random.nextInt(-60, -40),
                timestamp = System.currentTimeMillis()
            )
            emit(updatedNetwork)
            delay(3000) // Обновляем каждые 3 секунды
        }
    }
    
    /**
     * Генерирует фиктивный список WiFi сетей с небольшими вариациями
     */
    private fun generateMockWifiNetworks(): List<WifiInfo> {
        val currentTime = System.currentTimeMillis()
        val baseNetworks = listOf(
            "Home_WiFi_5G" to SecurityType.WPA2,
            "TP-Link_2.4G" to SecurityType.WPA2,
            "Office_Network" to SecurityType.WPA3,
            "Guest_Network" to SecurityType.OPEN,
            "Neighbor_WiFi" to SecurityType.WPA2,
            "Coffee_Shop_Free" to SecurityType.OPEN,
            "Legacy_WEP_Network" to SecurityType.WEP
        )
        
        return baseNetworks.mapIndexed { index, (ssid, security) ->
            val baseSignal = -40 - (index * 8) // От -40 до -88 dBm
            val signalVariation = Random.nextInt(-10, 10)
            val finalSignal = (baseSignal + signalVariation).coerceIn(-95, -30)
            
            val frequency = if (index % 2 == 0) {
                2400 + Random.nextInt(0, 84) * 5 // 2.4 GHz диапазон
            } else {
                5000 + Random.nextInt(0, 200) * 5 // 5 GHz диапазон
            }
            
            val capabilities = when (security) {
                SecurityType.WPA2 -> "[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]"
                SecurityType.WPA3 -> "[WPA3-SAE-CCMP][RSN-SAE-CCMP][MFPR][MFPC][ESS]"
                SecurityType.WPA -> "[WPA-PSK-CCMP+TKIP][ESS]"
                SecurityType.WEP -> "[WEP][ESS]"
                SecurityType.OPEN -> "[ESS]"
                SecurityType.UNKNOWN -> "[ESS]"
            }
            
            WifiInfo(
                ssid = ssid,
                bssid = generateRandomBSSID(),
                capabilities = capabilities,
                level = finalSignal,
                frequency = frequency,
                timestamp = currentTime - Random.nextLong(1000, 30000),
                securityType = security,
                signalStrength = finalSignal,
                channel = getChannelFromFrequency(frequency),
                bandwidth = if (frequency > 3000) "80 MHz" else "20 MHz",
                isHidden = ssid.isEmpty()
            )
        }.plus(
            // Добавляем скрытую сеть
            WifiInfo(
                ssid = "",
                bssid = generateRandomBSSID(),
                capabilities = "[WPA2-PSK-CCMP][ESS]",
                level = Random.nextInt(-95, -70),
                frequency = 5785,
                timestamp = currentTime - Random.nextLong(5000, 40000),
                securityType = SecurityType.WPA2,
                signalStrength = Random.nextInt(-95, -70),
                channel = 157,
                bandwidth = "80 MHz",
                isHidden = true
            )
        )
    }
    
    /**
     * Генерирует случайный BSSID (MAC-адрес)
     */
    private fun generateRandomBSSID(): String {
        return (1..6).map {
            Random.nextInt(0, 256).toString(16).padStart(2, '0').uppercase()
        }.joinToString(":")
    }
    
    /**
     * Получает номер канала по частоте
     */
    private fun getChannelFromFrequency(frequency: Int): Int {
        return when {
            frequency in 2400..2500 -> ((frequency - 2412) / 5) + 1
            frequency in 5000..6000 -> ((frequency - 5000) / 5)
            else -> 0
        }
    }
}