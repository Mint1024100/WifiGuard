package com.wifiguard.core.data.wifi

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.model.WifiStandard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

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

