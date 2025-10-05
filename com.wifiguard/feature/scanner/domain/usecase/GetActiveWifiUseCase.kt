package com.wifiguard.feature.scanner.domain.usecase

import android.util.Log
import com.wifiguard.core.common.Constants
import com.wifiguard.core.common.Resource
import com.wifiguard.feature.scanner.domain.model.WifiInfo
import com.wifiguard.feature.scanner.domain.repository.WifiScannerRepository
import com.wifiguard.feature.analyzer.domain.usecase.AnalyzeSecurityUseCase
import javax.inject.Inject

/**
 * Use Case для получения информации о текущем Wi-Fi подключении
 * 
 * Основные функции:
 * - Получает данные о текущей подключённой сети
 * - Анализирует уровень безопасности текущей сети
 * - Предоставляет детальную информацию о качестве соединения
 * - Обнаруживает потенциальные угрозы безопасности
 * 
 * Применение:
 * - Отображение статуса текущего подключения
 * - Мониторинг качества соединения
 * - Предупреждения о небезопасном подключении
 * - Диагностика проблем с сетью
 * 
 * @param repository репозиторий для работы с Wi-Fi данными
 * @param analyzeSecurityUseCase use case для анализа безопасности
 * 
 * @author WifiGuard Domain Team
 * @since 1.0.0
 */
class GetActiveWifiUseCase @Inject constructor(
    private val repository: WifiScannerRepository,
    private val analyzeSecurityUseCase: AnalyzeSecurityUseCase
) {
    
    companion object {
        private const val TAG = "${Constants.LogTags.WIFI_SCANNER}_GetActiveWifi"
    }
    
    /**
     * Получает полную информацию о текущем подключении
     * 
     * @return Resource с детальной информацией о текущей сети
     */
    suspend operator fun invoke(): Resource<ActiveConnectionInfo?> {
        return try {
            Log.d(TAG, "📱 Получение информации о текущем Wi-Fi подключении")
            
            // 1. Получаем текущее подключение
            when (val result = repository.getCurrentConnection()) {
                is Resource.Success -> {
                    val currentNetwork = result.data
                    
                    if (currentNetwork == null) {
                        Log.d(TAG, "📵 Нет активного Wi-Fi подключения")
                        return Resource.Success(null)
                    }
                    
                    // 2. Анализируем безопасность
                    val securityAnalysis = analyzeSecurityUseCase(currentNetwork)
                    
                    when (securityAnalysis) {
                        is Resource.Success -> {
                            val analysisResult = securityAnalysis.data
                            
                            // 3. Создаём полную информацию о подключении
                            val activeConnectionInfo = createActiveConnectionInfo(
                                network = currentNetwork,
                                securityAnalysis = analysisResult
                            )
                            
                            Log.i(TAG, "✅ Подключение к ${currentNetwork.displayName}: ${analysisResult.securityLevel}")
                            Resource.Success(activeConnectionInfo)
                        }
                        
                        is Resource.Error -> {
                            Log.e(TAG, "❌ Ошибка анализа безопасности", securityAnalysis.throwable)
                            
                            // Возвращаем сеть без анализа безопасности
                            val basicConnectionInfo = createActiveConnectionInfo(
                                network = currentNetwork,
                                securityAnalysis = null
                            )
                            
                            Resource.Success(basicConnectionInfo)
                        }
                        
                        is Resource.Loading -> Resource.Loading()
                    }
                }
                
                is Resource.Error -> {
                    Log.e(TAG, "❌ Ошибка получения текущего подключения", result.throwable)
                    Resource.Error(result.throwable)
                }
                
                is Resource.Loading -> Resource.Loading()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Неожиданная ошибка при получении активного подключения", e)
            Resource.Error(Exception("Не удалось получить сведения о текущем подключении", e))
        }
    }
    
    /**
     * Создаёт объект с полной информацией о текущем подключении
     */
    private fun createActiveConnectionInfo(
        network: WifiInfo,
        securityAnalysis: com.wifiguard.feature.analyzer.domain.model.ThreatAssessment?
    ): ActiveConnectionInfo {
        return ActiveConnectionInfo(
            network = network,
            connectionQuality = determineConnectionQuality(network),
            securityLevel = securityAnalysis?.securityLevel,
            threatAssessment = securityAnalysis,
            recommendations = generateRecommendations(network, securityAnalysis),
            diagnostics = generateDiagnostics(network),
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Определяет качество соединения на основе силы сигнала
     */
    private fun determineConnectionQuality(network: WifiInfo): ConnectionQuality {
        return when {
            network.signalStrength >= -50 -> ConnectionQuality.EXCELLENT
            network.signalStrength >= -60 -> ConnectionQuality.GOOD
            network.signalStrength >= -70 -> ConnectionQuality.FAIR
            network.signalStrength >= -80 -> ConnectionQuality.POOR
            else -> ConnectionQuality.VERY_POOR
        }
    }
    
    /**
     * Генерирует рекомендации на основе анализа сети
     */
    private fun generateRecommendations(
        network: WifiInfo,
        securityAnalysis: com.wifiguard.feature.analyzer.domain.model.ThreatAssessment?
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Рекомендации по безопасности
        when (network.encryptionType) {
            com.wifiguard.feature.scanner.domain.model.EncryptionType.OPEN -> {
                recommendations.add("⚠️ Открытая сеть! Используйте VPN")
                recommendations.add("🚫 Избегайте передачи конфиденциальных данных")
            }
            com.wifiguard.feature.scanner.domain.model.EncryptionType.WEP -> {
                recommendations.add("❌ Очень слабое шифрование! Обновите до WPA2/WPA3")
                recommendations.add("🚫 Не рекомендуется для важных данных")
            }
            com.wifiguard.feature.scanner.domain.model.EncryptionType.WPA -> {
                recommendations.add("⚠️ Устаревший протокол. Обновите до WPA2")
            }
            com.wifiguard.feature.scanner.domain.model.EncryptionType.WPA2 -> {
                recommendations.add("✅ Надёжное шифрование")
                recommendations.add("🔄 Рассмотрите обновление до WPA3")
            }
            com.wifiguard.feature.scanner.domain.model.EncryptionType.WPA3 -> {
                recommendations.add("🔒 Максимальная защита")
                recommendations.add("✨ Отличный выбор для безопасности")
            }
            com.wifiguard.feature.scanner.domain.model.EncryptionType.UNKNOWN -> {
                recommendations.add("❓ Неопределённый тип шифрования")
                recommendations.add("🔍 Проверьте настройки роутера")
            }
        }
        
        // Рекомендации по силе сигнала
        when (determineConnectionQuality(network)) {
            ConnectionQuality.VERY_POOR -> {
                recommendations.add("📶 Очень слабый сигнал! Подойдите ближе к роутеру")
            }
            ConnectionQuality.POOR -> {
                recommendations.add("📶 Слабый сигнал. Могут быть прерывания")
            }
            ConnectionQuality.FAIR -> {
                recommendations.add("📶 Удовлетворительный сигнал")
            }
            ConnectionQuality.GOOD -> {
                recommendations.add("✅ Хороший сигнал")
            }
            ConnectionQuality.EXCELLENT -> {
                recommendations.add("✨ Отличный сигнал")
            }
        }
        
        // Рекомендации по диапазону частот
        when (network.frequencyBand) {
            "2.4 ГГц" -> {
                recommendations.add("📶 2.4 ГГц: большая дальность, меньшая скорость")
            }
            "5 ГГц" -> {
                recommendations.add("⚡ 5 ГГц: высокая скорость, меньшая дальность")
            }
            "6 ГГц" -> {
                recommendations.add("🚀 6 ГГц: новейший стандарт Wi-Fi 6E/7")
            }
        }
        
        return recommendations
    }
    
    /**
     * Генерирует диагностическую информацию о сети
     */
    private fun generateDiagnostics(network: WifiInfo): NetworkDiagnostics {
        return NetworkDiagnostics(
            signalStrengthDbm = network.signalStrength,
            signalPercentage = network.signalPercentage,
            frequency = network.frequency,
            frequencyBand = network.frequencyBand,
            encryptionType = network.encryptionType,
            isHiddenNetwork = network.isHidden,
            estimatedRange = estimateRange(network.signalStrength, network.frequency),
            channelInfo = getChannelInfo(network.frequency),
            interferenceLevel = estimateInterference(network.frequency)
        )
    }
    
    /**
     * Оценивает приблизительное расстояние до роутера
     */
    private fun estimateRange(signalStrength: Int, frequency: Int): String {
        val baseRange = when {
            signalStrength >= -30 -> "1-2 м"
            signalStrength >= -50 -> "5-10 м"
            signalStrength >= -60 -> "10-20 м"
            signalStrength >= -70 -> "20-50 м"
            signalStrength >= -80 -> "50-100 м"
            else -> "100+ м"
        }
        
        // Корректировка по частоте
        val modifier = if (frequency > 5000) " (меньшая дальность на 5/6 ГГц)" else ""
        return baseRange + modifier
    }
    
    /**
     * Получает информацию о канале
     */
    private fun getChannelInfo(frequency: Int): String {
        val channel = when {
            frequency in 2412..2484 -> (frequency - 2412) / 5 + 1
            frequency in 5000..5900 -> (frequency - 5000) / 5
            else -> 0
        }
        
        return if (channel > 0) "Канал $channel" else "Неизвестный канал"
    }
    
    /**
     * Оценивает уровень помех на основе частоты
     */
    private fun estimateInterference(frequency: Int): InterferenceLevel {
        return when (frequency) {
            // 2.4 ГГц - много помех от Bluetooth, микроволновок
            in 2400..2500 -> InterferenceLevel.HIGH
            // 5 ГГц - меньше помех
            in 5000..5900 -> InterferenceLevel.MEDIUM
            // 6 ГГц - минимальные помехи (новый диапазон)
            in 5925..7125 -> InterferenceLevel.LOW
            else -> InterferenceLevel.UNKNOWN
        }
    }
    
    /**
     * Полная информация о текущем подключении
     */
    data class ActiveConnectionInfo(
        val network: WifiInfo,
        val connectionQuality: ConnectionQuality,
        val securityLevel: com.wifiguard.feature.analyzer.domain.model.SecurityLevel?,
        val threatAssessment: com.wifiguard.feature.analyzer.domain.model.ThreatAssessment?,
        val recommendations: List<String>,
        val diagnostics: NetworkDiagnostics,
        val timestamp: Long
    )
    
    /**
     * Качество соединения
     */
    enum class ConnectionQuality {
        EXCELLENT,  // -30 до -50 dBm
        GOOD,       // -50 до -60 dBm
        FAIR,       // -60 до -70 dBm
        POOR,       // -70 до -80 dBm
        VERY_POOR   // ниже -80 dBm
    }
    
    /**
     * Уровень помех
     */
    enum class InterferenceLevel {
        LOW,     // Минимальные помехи
        MEDIUM,  // Умеренные помехи
        HIGH,    // Высокий уровень помех
        UNKNOWN  // Неопределённые помехи
    }
    
    /**
     * Диагностика сети
     */
    data class NetworkDiagnostics(
        val signalStrengthDbm: Int,
        val signalPercentage: Int,
        val frequency: Int,
        val frequencyBand: String,
        val encryptionType: com.wifiguard.feature.scanner.domain.model.EncryptionType,
        val isHiddenNetwork: Boolean,
        val estimatedRange: String,
        val channelInfo: String,
        val interferenceLevel: InterferenceLevel
    )
}