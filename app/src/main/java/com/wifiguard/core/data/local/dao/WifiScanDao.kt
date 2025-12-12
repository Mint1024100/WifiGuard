package com.wifiguard.core.data.local.dao

import androidx.room.*
import com.wifiguard.core.data.local.entity.WifiScanEntity
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import kotlinx.coroutines.flow.Flow

/**
 * DAO для операций с историей сканирований Wi-Fi
 */
@Dao
interface WifiScanDao {
    
    @Query("SELECT * FROM wifi_scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<WifiScanEntity>>

    @Query("SELECT * FROM wifi_scans")
    suspend fun getAllWifiScansSuspend(): List<WifiScanEntity>
    
    @Query("SELECT * FROM wifi_scans WHERE scanSessionId = :sessionId ORDER BY timestamp DESC")
    fun getScansBySession(sessionId: String): Flow<List<WifiScanEntity>>
    
    @Query("SELECT * FROM wifi_scans WHERE ssid = :ssid ORDER BY timestamp DESC")
    fun getScansBySsid(ssid: String): Flow<List<WifiScanEntity>>
    
    @Query("SELECT * FROM wifi_scans WHERE bssid = :bssid ORDER BY timestamp DESC")
    fun getScansByBssid(bssid: String): Flow<List<WifiScanEntity>>
    
    @Query("SELECT * FROM wifi_scans WHERE threatLevel = :threatLevel ORDER BY timestamp DESC")
    fun getScansByThreatLevel(threatLevel: ThreatLevel): Flow<List<WifiScanEntity>>
    
    @Query("SELECT * FROM wifi_scans WHERE securityType = :securityType ORDER BY timestamp DESC")
    fun getScansBySecurityType(securityType: SecurityType): Flow<List<WifiScanEntity>>
    
    @Query("SELECT * FROM wifi_scans WHERE isConnected = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getCurrentConnectedNetwork(): WifiScanEntity?
    
    @Query("SELECT * FROM wifi_scans ORDER BY timestamp DESC LIMIT :limit")
    fun getLatestScans(limit: Int): Flow<List<WifiScanEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanResult(scanResult: WifiScanEntity): Long
    
    @Query("DELETE FROM wifi_scans WHERE timestamp < :timestamp")
    suspend fun clearOldScans(timestamp: Long)
    
    @Query("SELECT * FROM wifi_scans WHERE ssid = :ssid ORDER BY timestamp DESC")
    fun getNetworkStatistics(ssid: String): Flow<List<WifiScanEntity>>
    
    @Query("SELECT * FROM wifi_scans WHERE timestamp >= :fromTimestamp ORDER BY timestamp DESC")
    fun getScansFromTimestamp(fromTimestamp: Long): Flow<List<WifiScanEntity>>
    
    @Query("SELECT * FROM wifi_scans WHERE timestamp BETWEEN :fromTimestamp AND :toTimestamp ORDER BY timestamp DESC")
    fun getScansInTimeRange(fromTimestamp: Long, toTimestamp: Long): Flow<List<WifiScanEntity>>
    
    @Query("SELECT DISTINCT ssid FROM wifi_scans WHERE ssid IS NOT NULL AND ssid != '' ORDER BY ssid")
    fun getAllUniqueSsids(): Flow<List<String>>
    
    @Query("SELECT * FROM wifi_scans WHERE ssid = :ssid AND bssid = :bssid ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestScanForNetwork(ssid: String, bssid: String): WifiScanEntity?
    
    @Query("SELECT COUNT(*) FROM wifi_scans")
    suspend fun getTotalScansCount(): Int
    
    @Query("SELECT COUNT(*) FROM wifi_scans WHERE threatLevel = :threatLevel")
    suspend fun getScansCountByThreatLevel(threatLevel: ThreatLevel): Int
    
    @Query("SELECT COUNT(*) FROM wifi_scans WHERE securityType = :securityType")
    suspend fun getScansCountBySecurityType(securityType: SecurityType): Int
    
    @Query("SELECT * FROM wifi_scans WHERE isHidden = 1 ORDER BY timestamp DESC")
    fun getHiddenNetworks(): Flow<List<WifiScanEntity>>
    
    @Query("SELECT * FROM wifi_scans WHERE vendor = :vendor ORDER BY timestamp DESC")
    fun getScansByVendor(vendor: String): Flow<List<WifiScanEntity>>
    
    @Query("SELECT * FROM wifi_scans WHERE frequency BETWEEN :minFreq AND :maxFreq ORDER BY timestamp DESC")
    fun getScansByFrequencyRange(minFreq: Int, maxFreq: Int): Flow<List<WifiScanEntity>>
    
    @Query("SELECT * FROM wifi_scans WHERE level >= :minLevel ORDER BY timestamp DESC")
    fun getScansBySignalStrength(minLevel: Int): Flow<List<WifiScanEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: WifiScanEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScans(scans: List<WifiScanEntity>): List<Long>
    
    @Update
    suspend fun updateScan(scan: WifiScanEntity)
    
    @Delete
    suspend fun deleteScan(scan: WifiScanEntity)
    
    @Query("DELETE FROM wifi_scans WHERE id = :scanId")
    suspend fun deleteScanById(scanId: Long)
    
    @Query("DELETE FROM wifi_scans WHERE timestamp < :timestamp")
    suspend fun deleteScansOlderThan(timestamp: Long): Int
    
    @Query("DELETE FROM wifi_scans WHERE scanSessionId = :sessionId")
    suspend fun deleteScansBySession(sessionId: String)
    
    @Query("DELETE FROM wifi_scans")
    suspend fun deleteAllScans()
    
    // Статистические запросы
    @Query("""
        SELECT 
            securityType,
            COUNT(*) as count
        FROM wifi_scans 
        GROUP BY securityType 
        ORDER BY count DESC
    """)
    suspend fun getSecurityTypeStatistics(): List<SecurityTypeCount>
    
    @Query("""
        SELECT 
            threatLevel,
            COUNT(*) as count
        FROM wifi_scans 
        GROUP BY threatLevel 
        ORDER BY count DESC
    """)
    suspend fun getThreatLevelStatistics(): List<ThreatLevelCount>
    
    @Query("""
        SELECT 
            DATE(timestamp/1000, 'unixepoch') as date,
            COUNT(*) as count
        FROM wifi_scans 
        GROUP BY DATE(timestamp/1000, 'unixepoch')
        ORDER BY date DESC
        LIMIT 30
    """)
    suspend fun getDailyScanStatistics(): List<DailyScanCount>
    
    @Query("""
        SELECT 
            vendor,
            COUNT(*) as count
        FROM wifi_scans 
        WHERE vendor IS NOT NULL
        GROUP BY vendor 
        ORDER BY count DESC
        LIMIT 20
    """)
    suspend fun getVendorStatistics(): List<VendorCount>
}

/**
 * Результат статистики по типам безопасности
 */
data class SecurityTypeCount(
    val securityType: SecurityType,
    val count: Int
)

/**
 * Результат статистики по уровням угроз
 */
data class ThreatLevelCount(
    val threatLevel: ThreatLevel,
    val count: Int
)

/**
 * Результат статистики по дням
 */
data class DailyScanCount(
    val date: String,
    val count: Int
)

/**
 * Результат статистики по производителям
 */
data class VendorCount(
    val vendor: String,
    val count: Int
)