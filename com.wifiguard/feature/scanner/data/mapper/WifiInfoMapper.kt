package com.wifiguard.feature.scanner.data.mapper

import android.net.wifi.ScanResult
import android.os.Build
import com.wifiguard.core.common.Mapper
import com.wifiguard.core.security.SecurityManager
import com.wifiguard.feature.scanner.data.model.WifiInfoDto
import com.wifiguard.feature.scanner.domain.model.WifiInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Маппер для преобразования данных Wi-Fi сетей между слоями архитектуры.
 * Обеспечивает комплексное преобразование данных Android ScanResult 
 * в структурированные доменные модели с обогащенной информацией о безопасности.
 * 
 * Особенности:
 * - Использует SecurityManager для детального анализа безопасности
 * - Обрабатывает различные версии Android API
 * - Обеспечивает отказоустойчивость при ошибках сканирования
 * - Нормализует данные для различных производителей устройств
 * 
 * @param securityManager Менеджер для анализа безопасности Wi-Fi сетей
 * 
 * @author WifiGuard Development Team
 * @since 1.0.0
 */
@Singleton
class WifiInfoMapper @Inject constructor(
    private val securityManager: SecurityManager
) {
    
    /**
     * Маппер для преобразования ScanResult в WifiInfoDto
     * Используется на data слое для преобразования сырых данных Android API
     */
    val scanResultToDtoMapper = object : Mapper<ScanResult, WifiInfoDto> {
        override fun map(from: ScanResult): WifiInfoDto {
            return try {
                val normalizedSsid = normalizeSsid(from.SSID)
                val capabilities = from.capabilities ?: ""
                val frequency = from.frequency
                val level = from.level
                val timestamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    from.timestamp
                } else {
                    @Suppress("DEPRECATION")
                    from.timestamp / 1000 // Конвертация из микросекунд в миллисекунды
                }
                
                WifiInfoDto(
                    ssid = normalizedSsid,
                    bssid = from.BSSID ?: "Unknown BSSID",
                    capabilities = capabilities,
                    frequency = frequency,
                    level = level,
                    timestamp = timestamp,
                    channelWidth = extractChannelWidth(frequency),
                    is5GHz = is5GHzFrequency(frequency),
                    is6GHz = is6GHzFrequency(frequency)
                )
            } catch (e: Exception) {
                // Создаем fallback объект при ошибке
                createFallbackDto(from)
            }
        }
    }
    
    /**
     * Маппер для преобразования WifiInfoDto в доменную модель WifiInfo
     * Обогащает данные анализом безопасности и вычисляемыми метриками
     */
    suspend fun mapToWifiInfo(dto: WifiInfoDto): WifiInfo {
        return try {
            // Создаем ScanResult из DTO для анализа безопасности
            val mockScanResult = createScanResultFromDto(dto)
            
            // Проводим анализ безопасности
            val securityAnalysis = securityManager.analyzeNetworkSecurity(mockScanResult)
            
            // Вычисляем дополнительные метрики
            val signalQuality = calculateSignalQuality(dto.level)
            val estimatedDistance = estimateDistance(dto.level, dto.frequency)
            val bandInfo = getBandInfo(dto.frequency)
            val channelNumber = getChannelNumber(dto.frequency)
            
            WifiInfo(
                ssid = dto.ssid,
                bssid = dto.bssid,
                capabilities = dto.capabilities,
                frequency = dto.frequency,
                level = dto.level,
                timestamp = dto.timestamp,
                channelWidth = dto.channelWidth,
                is5GHz = dto.is5GHz,
                is6GHz = dto.is6GHz,
                
                // Данные анализа безопасности
                securityType = securityAnalysis.securityType,
                encryptionLevel = securityAnalysis.encryptionLevel,
                threatLevel = securityAnalysis.threatLevel,
                securityScore = securityAnalysis.securityScore,
                isOpenNetwork = securityAnalysis.isOpenNetwork,
                isSuspicious = securityAnalysis.isSuspicious,
                isMalicious = securityAnalysis.isMalicious,
                risks = securityAnalysis.risks,
                recommendations = securityAnalysis.recommendations,
                
                // Вычисляемые метрики
                signalStrength = securityAnalysis.signalStrength,
                signalQuality = signalQuality,
                estimatedDistance = estimatedDistance,
                channelNumber = channelNumber,
                bandInfo = bandInfo,
                
                // Метаданные
                lastSeen = System.currentTimeMillis(),
                isHidden = dto.ssid.isBlank() || dto.ssid == "<unknown ssid>",
                vendor = extractVendorFromBssid(dto.bssid)
            )
        } catch (e: Exception) {
            // Создаем fallback объект при ошибке
            createFallbackWifiInfo(dto)
        }
    }
    
    /**
     * Нормализует SSID, удаляя лишние кавычки и обрабатывая null значения
     */
    private fun normalizeSsid(ssid: String?): String {
        return when {
            ssid.isNullOrBlank() -> "<Скрытая сеть>"
            ssid.startsWith("\"") && ssid.endsWith("\"") && ssid.length > 2 -> {
                ssid.substring(1, ssid.length - 1)
            }
            else -> ssid
        }
    }
    
    /**
     * Определяет ширину канала на основе частоты
     */
    private fun extractChannelWidth(frequency: Int): Int {
        return when {
            frequency in 5170..5825 -> {
                // 5 GHz сети обычно используют большую ширину канала
                80 // По умолчанию 80 MHz для 5 GHz
            }
            frequency >= 5925 -> {
                // 6 GHz сети (Wi-Fi 6E/7)
                160 // По умолчанию 160 MHz для 6 GHz
            }
            else -> {
                // 2.4 GHz сети
                20 // По умолчанию 20 MHz для 2.4 GHz
            }
        }
    }
    
    /**
     * Проверяет, является ли частота 5 GHz
     */
    private fun is5GHzFrequency(frequency: Int): Boolean {
        return frequency in 5170..5825
    }
    
    /**
     * Проверяет, является ли частота 6 GHz (Wi-Fi 6E/7)
     */
    private fun is6GHzFrequency(frequency: Int): Boolean {
        return frequency >= 5925 // 6 GHz диапазон начинается от 5925 MHz
    }
    
    /**
     * Вычисляет качество сигнала в процентах
     */
    private fun calculateSignalQuality(levelDbm: Int): Int {
        return when {
            levelDbm >= -30 -> 100 // Отлично
            levelDbm >= -50 -> 80  // Очень хорошо
            levelDbm >= -60 -> 70  // Хорошо
            levelDbm >= -70 -> 60  // Удовлетворительно
            levelDbm >= -80 -> 40  // Слабо
            levelDbm >= -90 -> 20  // Очень слабо
            else -> 10             // Критически слабо
        }.coerceIn(0, 100)
    }
    
    /**
     * Оценивает расстояние до точки доступа в метрах
     */
    private fun estimateDistance(levelDbm: Int, frequency: Int): Double {
        if (levelDbm == 0) return -1.0 // Нет данных
        
        // Простая модель path loss: RSSI = Потери на свободном пространстве - 20*log10(расстояние) - 20*log10(частота)
        val frequencyMHz = frequency.toDouble()
        val exp = (27.55 - (20 * Math.log10(frequencyMHz)) + Math.abs(levelDbm)) / 20.0
        return Math.pow(10.0, exp).coerceIn(0.1, 1000.0) // Ограничиваем от 10 см до 1 км
    }
    
    /**
     * Определяет информацию о диапазоне частот
     */
    private fun getBandInfo(frequency: Int): String {
        return when {
            frequency in 2400..2500 -> "2.4 GHz (b/g/n/ax)"
            frequency in 5170..5350 -> "5 GHz Низкий (a/n/ac/ax)"
            frequency in 5470..5725 -> "5 GHz Средний (a/n/ac/ax)"
            frequency in 5725..5825 -> "5 GHz Высокий (a/n/ac/ax)"
            frequency >= 5925 -> "6 GHz (ax/be)"
            else -> "Неизвестный ($frequency MHz)"
        }
    }
    
    /**
     * Определяет номер канала на основе частоты
     */
    private fun getChannelNumber(frequency: Int): Int {
        return when {
            // 2.4 GHz каналы
            frequency in 2412..2484 -> (frequency - 2412) / 5 + 1
            // 5 GHz каналы
            frequency == 5170 -> 34
            frequency == 5180 -> 36
            frequency == 5190 -> 38
            frequency == 5200 -> 40
            frequency == 5220 -> 44
            frequency == 5240 -> 48
            frequency in 5260..5320 -> (frequency - 5000) / 5
            frequency in 5500..5700 -> (frequency - 5000) / 5
            frequency in 5745..5825 -> (frequency - 5000) / 5
            // 6 GHz каналы (упрощенно)
            frequency >= 5925 -> ((frequency - 5925) / 5) + 1
            else -> 0 // Неизвестный канал
        }
    }
    
    /**
     * Извлекает информацию о производителе из BSSID (MAC-адреса)
     */
    private fun extractVendorFromBssid(bssid: String): String {
        if (bssid.length < 8) return "Неизвестный"
        
        val oui = bssid.substring(0, 8).replace(":", "").uppercase()
        
        return when {
            oui.startsWith("00248C") -> "Ubiquiti Networks"
            oui.startsWith("001DD8") -> "Mikrotik"
            oui.startsWith("F8C4F3") -> "TP-Link"
            oui.startsWith("2C5D93") -> "TP-Link"
            oui.startsWith("502B73") -> "Cisco"
            oui.startsWith("0050F2") -> "Microsoft"
            oui.startsWith("001999") -> "Belkin"
            oui.startsWith("9094E4") -> "ASUS"
            oui.startsWith("107B44") -> "Apple"
            oui.startsWith("20C9D0") -> "D-Link"
            oui.startsWith("000B6B") -> "Netgear"
            oui.startsWith("001E2A") -> "Netgear"
            else -> "Неизвестный"
        }
    }
    
    /**
     * Создает ScanResult объект из DTO для анализа безопасности
     */
    @Suppress("DEPRECATION")
    private fun createScanResultFromDto(dto: WifiInfoDto): ScanResult {
        val scanResult = ScanResult()
        scanResult.SSID = dto.ssid
        scanResult.BSSID = dto.bssid
        scanResult.capabilities = dto.capabilities
        scanResult.frequency = dto.frequency
        scanResult.level = dto.level
        scanResult.timestamp = dto.timestamp * 1000 // Конвертация в микросекунды
        return scanResult
    }
    
    /**
     * Создает fallback DTO при ошибке обработки ScanResult
     */
    private fun createFallbackDto(scanResult: ScanResult): WifiInfoDto {
        return WifiInfoDto(
            ssid = "<Ошибка обработки>",
            bssid = scanResult.BSSID ?: "Unknown",
            capabilities = "",
            frequency = 2412, // По умолчанию 2.4 GHz
            level = -100,
            timestamp = System.currentTimeMillis(),
            channelWidth = 20,
            is5GHz = false,
            is6GHz = false
        )
    }
    
    /**
     * Создает fallback WifiInfo при ошибке маппинга
     */
    private fun createFallbackWifiInfo(dto: WifiInfoDto): WifiInfo {
        return WifiInfo(
            ssid = dto.ssid,
            bssid = dto.bssid,
            capabilities = dto.capabilities,
            frequency = dto.frequency,
            level = dto.level,
            timestamp = dto.timestamp,
            channelWidth = dto.channelWidth,
            is5GHz = dto.is5GHz,
            is6GHz = dto.is6GHz,
            
            // По умолчанию значения для fallback
            securityType = "Неизвестный",
            encryptionLevel = SecurityManager.EncryptionLevel.UNKNOWN,
            threatLevel = SecurityManager.ThreatLevel.MEDIUM,
            securityScore = 0,
            isOpenNetwork = false,
            isSuspicious = true,
            isMalicious = false,
            risks = emptyList(),
            recommendations = listOf("Не удалось проанализировать безопасность сети"),
            
            signalStrength = 1,
            signalQuality = 10,
            estimatedDistance = -1.0,
            channelNumber = 0,
            bandInfo = "Неизвестный",
            
            lastSeen = System.currentTimeMillis(),
            isHidden = false,
            vendor = "Неизвестный"
        )
    }
}