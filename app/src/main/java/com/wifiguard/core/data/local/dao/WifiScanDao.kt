package com.wifiguard.core.data.local.dao

import androidx.room.*
import com.wifiguard.core.data.local.entity.WifiScanResultEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object для работы с результатами сканирования Wi-Fi.
 * 
 * Предоставляет методы для сохранения и анализа исторических данных
 * сканирования, мониторинга сигналов и геолокации.
 */
@Dao
interface WifiScanDao {
    
    /**
     * Получить все результаты сканирования
     */
    @Query("SELECT * FROM wifi_scan_results ORDER BY scan_timestamp DESC")
    fun getAllScanResults(): Flow<List<WifiScanResultEntity>>
    
    /**
     * Получить результаты для конкретной сети
     */
    @Query("SELECT * FROM wifi_scan_results WHERE network_id = :networkId ORDER BY scan_timestamp DESC")
    fun getScanResultsForNetwork(networkId: Long): Flow<List<WifiScanResultEntity>>
    
    /**
     * Получить результаты за период
     */
    @Query("SELECT * FROM wifi_scan_results WHERE scan_timestamp BETWEEN :startTime AND :endTime ORDER BY scan_timestamp DESC")
    fun getScanResultsInPeriod(startTime: Long, endTime: Long): Flow<List<WifiScanResultEntity>>
    
    /**
     * Получить результаты по сессии сканирования
     */
    @Query("SELECT * FROM wifi_scan_results WHERE scan_session_id = :sessionId ORDER BY scan_timestamp ASC")
    fun getScanResultsBySession(sessionId: String): Flow<List<WifiScanResultEntity>>
    
    /**
     * Получить последние результаты для каждой сети
     */
    @Query("""
        SELECT sr.* FROM wifi_scan_results sr
        INNER JOIN (
            SELECT network_id, MAX(scan_timestamp) as max_timestamp 
            FROM wifi_scan_results 
            GROUP BY network_id
        ) latest ON sr.network_id = latest.network_id 
        AND sr.scan_timestamp = latest.max_timestamp
        ORDER BY sr.scan_timestamp DESC
    """)
    fun getLatestScanForEachNetwork(): Flow<List<WifiScanResultEntity>>
    
    /**
     * Получить статистику мощности сигнала для сети
     */
    @Query("""
        SELECT 
            MIN(signal_strength) as min_signal,
            MAX(signal_strength) as max_signal,
            AVG(signal_strength) as avg_signal,
            COUNT(*) as scan_count
        FROM wifi_scan_results 
        WHERE network_id = :networkId
    """)
    suspend fun getSignalStatistics(networkId: Long): SignalStatistics?
    
    /**
     * Получить количество сканирований за период
     */
    @Query("SELECT COUNT(*) FROM wifi_scan_results WHERE scan_timestamp BETWEEN :startTime AND :endTime")
    suspend fun getScanCountInPeriod(startTime: Long, endTime: Long): Int
    
    /**
     * Получить результаты в радиусе от указанной точки
     */
    @Query("""
        SELECT * FROM wifi_scan_results 
        WHERE location_latitude IS NOT NULL 
        AND location_longitude IS NOT NULL
        AND (
            (location_latitude BETWEEN :lat - :radiusDegrees AND :lat + :radiusDegrees)
            AND (location_longitude BETWEEN :lng - :radiusDegrees AND :lng + :radiusDegrees)
        )
        ORDER BY scan_timestamp DESC
    """)
    fun getScanResultsInRadius(
        lat: Double, 
        lng: Double, 
        radiusDegrees: Double = 0.001 // ~100m
    ): Flow<List<WifiScanResultEntity>>
    
    /**
     * Добавить результат сканирования
     */
    @Insert
    suspend fun insertScanResult(scanResult: WifiScanResultEntity): Long
    
    /**
     * Добавить несколько результатов сканирования
     */
    @Insert
    suspend fun insertScanResults(scanResults: List<WifiScanResultEntity>): List<Long>
    
    /**
     * Обновить результат сканирования
     */
    @Update
    suspend fun updateScanResult(scanResult: WifiScanResultEntity)
    
    /**
     * Удалить результат сканирования
     */
    @Delete
    suspend fun deleteScanResult(scanResult: WifiScanResultEntity)
    
    /**
     * Удалить все результаты сканирования
     */
    @Query("DELETE FROM wifi_scan_results")
    suspend fun deleteAllScanResults()
    
    /**
     * Удалить старые результаты (старше 30 дней)
     */
    @Query("DELETE FROM wifi_scan_results WHERE scan_timestamp < :cutoffTimestamp")
    suspend fun deleteOldScanResults(cutoffTimestamp: Long)
    
    /**
     * Удалить результаты для конкретной сети
     */
    @Query("DELETE FROM wifi_scan_results WHERE network_id = :networkId")
    suspend fun deleteScanResultsForNetwork(networkId: Long)
}

/**
 * Data class для статистики сигналов
 */
data class SignalStatistics(
    val min_signal: Int,
    val max_signal: Int,
    val avg_signal: Double,
    val scan_count: Int
)