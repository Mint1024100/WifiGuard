package com.wifiguard.core.data.wifi

import com.wifiguard.core.domain.model.ScanMetadata
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
     * Наблюдать за изменениями состояния WiFi (включен/выключен) в реальном времени
     */
    fun observeWifiEnabled(): Flow<Boolean>

    /**
     * Наблюдать за фактом Wi‑Fi подключения (TRANSPORT_WIFI) в реальном времени.
     * Нужен для обновления "текущей сети" при реальном коннекте, а не только при включении тумблера Wi‑Fi.
     */
    fun observeWifiTransportConnected(): Flow<Boolean>
    
    /**
     * Получить текущую подключенную сеть
     */
    suspend fun getCurrentNetwork(): WifiScanResult?
    
    /**
     * Запустить непрерывное сканирование
     */
    fun startContinuousScan(intervalMs: Long = 30000): Flow<List<WifiScanResult>>
    
    /**
     * Получить метаданные последнего сканирования (свежесть, источник данных)
     * Возвращает null если сканирование еще не выполнялось
     */
    fun getLastScanMetadata(): ScanMetadata?
}