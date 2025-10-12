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
 */
class ObserveWifiStateUseCase @Inject constructor(
    private val wifiRepository: WifiScannerRepository,
    private val wifiScannerService: com.wifiguard.core.data.wifi.WifiScannerService
) {
    
    /**
     * Наблюдает за списком доступных WiFi сетей
     */
    operator fun invoke(): Flow<List<WifiInfo>> {
        return wifiScannerService.observeScanResults()
    }
    
    /**
     * Наблюдает за состоянием WiFi (включен/выключен)
     */
    fun observeWifiEnabled(): Flow<Boolean> = flow {
        while (true) {
            emit(wifiScannerService.isWifiEnabled())
            delay(5000) // Проверяем каждые 5 секунд
        }
    }
    
    /**
     * Наблюдает за текущей подключенной сетью
     */
    fun observeCurrentWifi(): Flow<WifiInfo?> = flow {
        while (true) {
            val currentNetwork = wifiScannerService.getCurrentNetwork()
            emit(currentNetwork)
            delay(3000) // Обновляем каждые 3 секунды
        }
    }
}