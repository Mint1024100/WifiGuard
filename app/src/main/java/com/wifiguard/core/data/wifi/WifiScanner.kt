package com.wifiguard.core.data.wifi

import com.wifiguard.core.domain.model.WifiScanResult
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс для сканирования Wi-Fi сетей
 */
interface WifiScanner {
    /**
     * Запустить сканирование Wi-Fi сетей
     */
    suspend fun startScan(): Result<List<WifiScanResult>>
    
    /**
     * Получить поток результатов сканирования
     */
    fun getScanResultsFlow(): Flow<List<WifiScanResult>>
    
    /**
     * Проверить, включен ли Wi-Fi
     */
    fun isWifiEnabled(): Boolean
    
    /**
     * Получить текущую подключенную сеть
     */
    suspend fun getCurrentNetwork(): WifiScanResult?
    
    /**
     * Запустить непрерывное сканирование
     */
    fun startContinuousScan(intervalMs: Long = 30000): Flow<List<WifiScanResult>>
}