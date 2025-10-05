package com.wifiguard.core.data.local.dao

import androidx.room.*
import com.wifiguard.core.data.local.entity.WifiScanResultEntity
import com.wifiguard.core.data.local.entity.toDomainModel
import com.wifiguard.core.domain.model.WifiScanResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DAO (Data Access Object) для управления результатами сканирования Wi-Fi.
 * Обеспечивает операции для хранения и анализа истории сканирования.
 */
@Dao
abstract class WifiScanDao {
    
    /**
     * Получить последние результаты сканирования
     */
    @Query("SELECT * FROM wifi_scan_results ORDER BY timestamp DESC LIMIT :limit")
    abstract fun getLatestScansFlow(limit: Int): Flow<List<WifiScanResultEntity>>
    
    fun getLatestScans(limit: Int = 100): Flow<List<WifiScanResult>> {
        return getLatestScansFlow(limit).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    /**
     * Вставить результат сканирования
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertScanResultEntity(scanResult: WifiScanResultEntity)
    
    suspend fun insertScanResult(scanResult: WifiScanResult) {
        insertScanResultEntity(scanResult.toEntity())
    }
    
    /**
     * Вставить множество результатов сканирования
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertScanResultEntities(scanResults: List<WifiScanResultEntity>)
    
    suspend fun insertScanResults(scanResults: List<WifiScanResult>) {
        insertScanResultEntities(scanResults.map { it.toEntity() })
    }
    
    /**
     * Получить статистику по конкретной сети
     */
    @Query("SELECT * FROM wifi_scan_results WHERE ssid = :ssid ORDER BY timestamp DESC")
    abstract fun getNetworkStatisticsFlow(ssid: String): Flow<List<WifiScanResultEntity>>
    
    fun getNetworkStatistics(ssid: String): Flow<List<WifiScanResult>> {
        return getNetworkStatisticsFlow(ssid).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    /**
     * Получить сканы за определённый период
     */
    @Query("""
        SELECT * FROM wifi_scan_results 
        WHERE timestamp BETWEEN :startTime AND :endTime 
        ORDER BY timestamp DESC
    """)
    abstract fun getScansByTimeRange(
        startTime: Long, 
        endTime: Long
    ): Flow<List<WifiScanResultEntity>>
    
    /**
     * Получить сканы по типу
     */
    @Query("SELECT * FROM wifi_scan_results WHERE scan_type = :scanType ORDER BY timestamp DESC")
    abstract fun getScansByType(scanType: String): Flow<List<WifiScanResultEntity>>
    
    /**
     * Очистить старые результаты сканирования
     */
    @Query("DELETE FROM wifi_scan_results WHERE timestamp < :olderThanMillis")
    abstract suspend fun clearOldScans(olderThanMillis: Long)
    
    /**
     * Получить количество сканов за последние 24 часа
     */
    @Query("""
        SELECT COUNT(*) FROM wifi_scan_results 
        WHERE timestamp > :timestampMillis
    """)
    abstract suspend fun getScansCountSince(timestampMillis: Long): Int
    
    /**
     * Получить среднюю силу сигнала для конкретной сети
     */
    @Query("""
        SELECT AVG(signal_strength) FROM wifi_scan_results 
        WHERE ssid = :ssid AND timestamp > :sinceTimestamp
    """)
    abstract suspend fun getAverageSignalStrength(ssid: String, sinceTimestamp: Long): Double?
    
    /**
     * Получить уникальные сети за последний час
     */
    @Query("""
        SELECT DISTINCT ssid FROM wifi_scan_results 
        WHERE timestamp > :sinceTimestamp
        ORDER BY timestamp DESC
    """)
    abstract suspend fun getUniqueSSIDsSince(sinceTimestamp: Long): List<String>
    
    /**
     * Очистить все сканы
     */
    @Query("DELETE FROM wifi_scan_results")
    abstract suspend fun clearAllScans()
    
    /**
     * Получить общее количество сканов
     */
    @Query("SELECT COUNT(*) FROM wifi_scan_results")
    abstract suspend fun getTotalScansCount(): Int
    
    /**
     * Получить самый последний скан
     */
    @Query("SELECT * FROM wifi_scan_results ORDER BY timestamp DESC LIMIT 1")
    abstract suspend fun getLatestScan(): WifiScanResultEntity?
}