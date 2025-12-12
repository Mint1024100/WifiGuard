package com.wifiguard.feature.scanner.domain.usecase

import com.wifiguard.feature.scanner.domain.model.WifiInfo
import com.wifiguard.feature.scanner.domain.model.EncryptionType
import com.wifiguard.feature.scanner.domain.repository.WifiScannerRepository
import javax.inject.Inject
import kotlin.random.Random

/**
 * Use case для получения списка активных WiFi сетей
 */
class GetActiveWifiUseCase @Inject constructor(
    private val wifiRepository: WifiScannerRepository,
    private val wifiScannerService: com.wifiguard.core.data.wifi.WifiScannerService
) {
    
    /**
     * Получает список доступных WiFi сетей
     */
    suspend operator fun invoke(): List<WifiInfo> {
        // Запускаем сканирование
        val scanStatus = wifiScannerService.startScan()
        
        // Проверяем, успешно ли запущено сканирование
        if (scanStatus !is com.wifiguard.core.domain.model.WifiScanStatus.Success) {
            return emptyList()
        }
        
        // Ждем завершения сканирования
        kotlinx.coroutines.delay(1500)
        
        // Получаем результаты
        return wifiScannerService.getScanResults()
    }
}