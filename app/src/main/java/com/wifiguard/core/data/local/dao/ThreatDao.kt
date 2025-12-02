package com.wifiguard.core.data.local.dao

import androidx.room.*
import com.wifiguard.core.data.local.entity.ThreatEntity
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.security.ThreatType
import kotlinx.coroutines.flow.Flow

/**
 * DAO для операций с угрозами безопасности
 */
@Dao
interface ThreatDao {
    
    @Query("SELECT * FROM threats ORDER BY timestamp DESC")
    fun getAllThreats(): Flow<List<ThreatEntity>>

    @Query("SELECT * FROM threats")
    suspend fun getAllThreatsSuspend(): List<ThreatEntity>
    
    @Query("SELECT * FROM threats WHERE scanId = :scanId ORDER BY timestamp DESC")
    fun getThreatsByScanId(scanId: Long): Flow<List<ThreatEntity>>
    
    @Query("SELECT * FROM threats WHERE threatType = :threatType ORDER BY timestamp DESC")
    fun getThreatsByType(threatType: ThreatType): Flow<List<ThreatEntity>>
    
    @Query("SELECT * FROM threats WHERE severity = :severity ORDER BY timestamp DESC")
    fun getThreatsBySeverity(severity: ThreatLevel): Flow<List<ThreatEntity>>
    
    @Query("SELECT * FROM threats WHERE networkSsid = :ssid ORDER BY timestamp DESC")
    fun getThreatsByNetworkSsid(ssid: String): Flow<List<ThreatEntity>>
    
    @Query("SELECT * FROM threats WHERE networkBssid = :bssid ORDER BY timestamp DESC")
    fun getThreatsByNetworkBssid(bssid: String): Flow<List<ThreatEntity>>
    
    @Query("SELECT * FROM threats WHERE isResolved = 0 ORDER BY timestamp DESC")
    fun getUnresolvedThreats(): Flow<List<ThreatEntity>>
    
    @Query("SELECT * FROM threats WHERE isResolved = 1 ORDER BY timestamp DESC")
    fun getResolvedThreats(): Flow<List<ThreatEntity>>
    
    @Query("SELECT * FROM threats WHERE timestamp >= :fromTimestamp ORDER BY timestamp DESC")
    fun getThreatsFromTimestamp(fromTimestamp: Long): Flow<List<ThreatEntity>>
    
    @Query("SELECT * FROM threats WHERE timestamp BETWEEN :fromTimestamp AND :toTimestamp ORDER BY timestamp DESC")
    fun getThreatsInTimeRange(fromTimestamp: Long, toTimestamp: Long): Flow<List<ThreatEntity>>
    
    @Query("SELECT * FROM threats WHERE severity IN (:severities) ORDER BY timestamp DESC")
    fun getThreatsBySeverities(severities: List<ThreatLevel>): Flow<List<ThreatEntity>>
    
    @Query("SELECT * FROM threats WHERE threatType IN (:types) ORDER BY timestamp DESC")
    fun getThreatsByTypes(types: List<ThreatType>): Flow<List<ThreatEntity>>
    
    @Query("SELECT * FROM threats WHERE id = :threatId")
    suspend fun getThreatById(threatId: Long): ThreatEntity?
    
    @Query("SELECT COUNT(*) FROM threats")
    suspend fun getTotalThreatsCount(): Int
    
    @Query("SELECT COUNT(*) FROM threats WHERE isResolved = 0")
    suspend fun getUnresolvedThreatsCount(): Int
    
    @Query("SELECT COUNT(*) FROM threats WHERE severity = :severity")
    suspend fun getThreatsCountBySeverity(severity: ThreatLevel): Int
    
    @Query("SELECT COUNT(*) FROM threats WHERE threatType = :threatType")
    suspend fun getThreatsCountByType(threatType: ThreatType): Int
    
    @Query("SELECT COUNT(*) FROM threats WHERE timestamp >= :fromTimestamp")
    suspend fun getThreatsCountFromTimestamp(fromTimestamp: Long): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreat(threat: ThreatEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreats(threats: List<ThreatEntity>): List<Long>
    
    @Update
    suspend fun updateThreat(threat: ThreatEntity)
    
    @Delete
    suspend fun deleteThreat(threat: ThreatEntity)
    
    @Query("DELETE FROM threats WHERE id = :threatId")
    suspend fun deleteThreatById(threatId: Long)
    
    @Query("DELETE FROM threats WHERE scanId = :scanId")
    suspend fun deleteThreatsByScanId(scanId: Long)
    
    @Query("DELETE FROM threats WHERE timestamp < :timestamp")
    suspend fun deleteThreatsOlderThan(timestamp: Long)
    
    @Query("DELETE FROM threats WHERE isResolved = 1 AND resolutionTimestamp < :timestamp")
    suspend fun deleteResolvedThreatsOlderThan(timestamp: Long)
    
    @Query("DELETE FROM threats")
    suspend fun deleteAllThreats()
    
    @Query("UPDATE threats SET isResolved = 1, resolutionTimestamp = :timestamp, resolutionNote = :note WHERE id = :threatId")
    suspend fun resolveThreat(threatId: Long, timestamp: Long, note: String? = null)
    
    @Query("UPDATE threats SET isResolved = 0, resolutionTimestamp = NULL, resolutionNote = NULL WHERE id = :threatId")
    suspend fun unresolveThreat(threatId: Long)
    
    @Query("SELECT * FROM threats WHERE isNotified = 0 AND severity = 'CRITICAL' ORDER BY timestamp DESC")
    suspend fun getCriticalUnnotifiedThreats(): List<ThreatEntity>
    
    @Query("UPDATE threats SET isNotified = 1 WHERE id = :threatId")
    suspend fun markThreatAsNotified(threatId: Long)
    
    @Query("UPDATE threats SET isNotified = 1 WHERE scanId = :scanId")
    suspend fun markThreatsAsNotifiedForScan(scanId: Long)
    
    @Query("SELECT * FROM threats WHERE isNotified = 0 ORDER BY timestamp DESC")
    suspend fun getUnnotifiedThreats(): List<ThreatEntity>
    
    // Статистические запросы
    @Query("""
        SELECT 
            threatType,
            COUNT(*) as count
        FROM threats 
        GROUP BY threatType 
        ORDER BY count DESC
    """)
    suspend fun getThreatTypeStatistics(): List<ThreatTypeCount>
    
    @Query("""
        SELECT 
            severity,
            COUNT(*) as count
        FROM threats 
        GROUP BY severity 
        ORDER BY count DESC
    """)
    suspend fun getThreatSeverityStatistics(): List<ThreatSeverityCount>
    
    @Query("""
        SELECT 
            DATE(timestamp/1000, 'unixepoch') as date,
            COUNT(*) as count
        FROM threats 
        GROUP BY DATE(timestamp/1000, 'unixepoch')
        ORDER BY date DESC
        LIMIT 30
    """)
    suspend fun getDailyThreatStatistics(): List<DailyThreatCount>
    
    @Query("""
        SELECT 
            networkSsid,
            COUNT(*) as count
        FROM threats 
        WHERE networkSsid IS NOT NULL AND networkSsid != ''
        GROUP BY networkSsid 
        ORDER BY count DESC
        LIMIT 20
    """)
    suspend fun getNetworkThreatStatistics(): List<NetworkThreatCount>
    
    @Query("""
        SELECT 
            threatType,
            severity,
            COUNT(*) as count
        FROM threats 
        GROUP BY threatType, severity 
        ORDER BY count DESC
    """)
    suspend fun getThreatTypeSeverityStatistics(): List<ThreatTypeSeverityCount>
}

/**
 * Результат статистики по типам угроз
 */
data class ThreatTypeCount(
    val threatType: ThreatType,
    val count: Int
)

/**
 * Результат статистики по уровням серьезности угроз
 */
data class ThreatSeverityCount(
    val severity: ThreatLevel,
    val count: Int
)

/**
 * Результат статистики угроз по дням
 */
data class DailyThreatCount(
    val date: String,
    val count: Int
)

/**
 * Результат статистики угроз по сетям
 */
data class NetworkThreatCount(
    val networkSsid: String,
    val count: Int
)

/**
 * Результат статистики по типам и уровням угроз
 */
data class ThreatTypeSeverityCount(
    val threatType: ThreatType,
    val severity: ThreatLevel,
    val count: Int
)
