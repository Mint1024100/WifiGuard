package com.wifiguard.feature.scanner.domain.repository

import com.wifiguard.feature.scanner.domain.model.WifiInfo
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с WiFi сканированием
 * Определяет контракт для работы с данными WiFi сетей
 */
interface WifiScannerRepository {
    
    /**
     * Получение списка доступных WiFi сетей однократно
     * @return список найденных WiFi сетей
     */
    suspend fun scanWifiNetworks(): List<WifiInfo>
    
    /**
     * Наблюдение за списком доступных WiFi сетей в реальном времени
     * Автоматически обновляет список при обнаружении изменений
     * @return Flow с обновляемым списком сетей
     */
    fun observeWifiNetworks(): Flow<List<WifiInfo>>
    
    /**
     * Получение информации о текущей подключенной WiFi сети
     * @return информация о текущей сети или null, если нет подключения
     */
    suspend fun getCurrentWifiInfo(): WifiInfo?
    
    /**
     * Наблюдение за состоянием текущего WiFi подключения
     * @return Flow с информацией о текущей сети
     */
    fun observeCurrentWifiInfo(): Flow<WifiInfo?>
    
    /**
     * Проверка, включен ли WiFi на устройстве
     * @return true, если WiFi включен
     */
    suspend fun isWifiEnabled(): Boolean
    
    /**
     * Наблюдение за состоянием WiFi (включен/выключен)
     * @return Flow с текущим состоянием WiFi
     */
    fun observeWifiState(): Flow<Boolean>
    
    /**
     * Запуск процесса сканирования WiFi сетей
     * @return статус сканирования (Success, Throttled, Restricted, Failed)
     */
    suspend fun startScan(): com.wifiguard.core.domain.model.WifiScanStatus
    
    /**
     * Остановка процесса наблюдения за сетями
     */
    suspend fun stopObserving()
}