package com.wifiguard.core.domain.repository

import com.wifiguard.core.domain.model.WifiNetwork
import com.wifiguard.core.domain.model.WifiScanResult
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для управления Wi-Fi данными.
 * Определяет контракт для всех операций с Wi-Fi сетями и результатами сканирования.
 */
interface WifiRepository {
    
    // Операции с Wi-Fi сетями
    
    /**
     * Получить все сохранённые Wi-Fi сети
     * @return Flow со списком сетей
     */
    fun getAllNetworks(): Flow<List<WifiNetwork>>
    
    /**
     * Получить сеть по SSID
     * @param ssid идентификатор сети
     * @return Wi-Fi сеть или null
     */
    suspend fun getNetworkBySSID(ssid: String): WifiNetwork?
    
    /**
     * Сохранить новую Wi-Fi сеть
     * @param network сеть для сохранения
     */
    suspend fun insertNetwork(network: WifiNetwork)
    
    /**
     * Обновить информацию о Wi-Fi сети
     * @param network обновлённая сеть
     */
    suspend fun updateNetwork(network: WifiNetwork)
    
    /**
     * Удалить Wi-Fi сеть
     * @param network сеть для удаления
     */
    suspend fun deleteNetwork(network: WifiNetwork)
    
    // Операции с результатами сканирования
    
    /**
     * Получить последние результаты сканирования
     * @param limit максимальное количество записей
     * @return Flow со списком результатов
     */
    fun getLatestScans(limit: Int = 100): Flow<List<WifiScanResult>>
    
    /**
     * Сохранить результат сканирования
     * @param scanResult результат для сохранения
     */
    suspend fun insertScanResult(scanResult: WifiScanResult)
    
    /**
     * Очистить старые результаты сканирования
     * @param olderThanMillis время в миллисекундах, старше которого данные будут удалены
     */
    suspend fun clearOldScans(olderThanMillis: Long)
    
    // Аналитические операции
    
    /**
     * Получить статистику по конкретной сети
     * @param ssid идентификатор сети
     * @return Flow с историей сканирования сети
     */
    fun getNetworkStatistics(ssid: String): Flow<List<WifiScanResult>>
    
    /**
     * Пометить сеть как подозрительную
     * @param ssid идентификатор сети
     * @param reason причина подозрения
     */
    suspend fun markNetworkAsSuspicious(ssid: String, reason: String)
    
    /**
     * Получить все подозрительные сети
     * @return Flow со списком подозрительных сетей
     */
    fun getSuspiciousNetworks(): Flow<List<WifiNetwork>>
}