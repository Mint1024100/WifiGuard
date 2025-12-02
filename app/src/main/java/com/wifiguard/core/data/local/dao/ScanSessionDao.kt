package com.wifiguard.core.data.local.dao

import androidx.room.*
import com.wifiguard.core.data.local.entity.ScanSessionEntity
import com.wifiguard.core.domain.model.ThreatLevel
import kotlinx.coroutines.flow.Flow

/**
 * DAO для операций с сессиями сканирования
 */
@Dao
interface ScanSessionDao {
    
    @Query("SELECT * FROM scan_sessions ORDER BY startTimestamp DESC")
    fun getAllSessions(): Flow<List<ScanSessionEntity>>

    @Query("SELECT * FROM scan_sessions")
    suspend fun getAllScanSessionsSuspend(): List<ScanSessionEntity>
    
    @Query("SELECT * FROM scan_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): ScanSessionEntity?
    
    @Query("SELECT * FROM scan_sessions WHERE startTimestamp >= :fromTimestamp ORDER BY startTimestamp DESC")
    fun getSessionsFromTimestamp(fromTimestamp: Long): Flow<List<ScanSessionEntity>>
    
    @Query("SELECT * FROM scan_sessions WHERE startTimestamp BETWEEN :fromTimestamp AND :toTimestamp ORDER BY startTimestamp DESC")
    fun getSessionsInTimeRange(fromTimestamp: Long, toTimestamp: Long): Flow<List<ScanSessionEntity>>
    
    @Query("SELECT * FROM scan_sessions WHERE isBackgroundScan = 1 ORDER BY startTimestamp DESC")
    fun getBackgroundSessions(): Flow<List<ScanSessionEntity>>
    
    @Query("SELECT * FROM scan_sessions WHERE isBackgroundScan = 0 ORDER BY startTimestamp DESC")
    fun getManualSessions(): Flow<List<ScanSessionEntity>>
    
    @Query("SELECT * FROM scan_sessions WHERE overallRiskLevel = :riskLevel ORDER BY startTimestamp DESC")
    fun getSessionsByRiskLevel(riskLevel: ThreatLevel): Flow<List<ScanSessionEntity>>
    
    @Query("SELECT * FROM scan_sessions WHERE endTimestamp IS NULL ORDER BY startTimestamp DESC")
    fun getActiveSessions(): Flow<List<ScanSessionEntity>>
    
    @Query("SELECT * FROM scan_sessions WHERE endTimestamp IS NOT NULL ORDER BY startTimestamp DESC")
    fun getCompletedSessions(): Flow<List<ScanSessionEntity>>
    
    @Query("SELECT * FROM scan_sessions ORDER BY startTimestamp DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<ScanSessionEntity>>
    
    @Query("SELECT COUNT(*) FROM scan_sessions")
    suspend fun getTotalSessionsCount(): Int
    
    @Query("SELECT COUNT(*) FROM scan_sessions WHERE isBackgroundScan = 1")
    suspend fun getBackgroundSessionsCount(): Int
    
    @Query("SELECT COUNT(*) FROM scan_sessions WHERE isBackgroundScan = 0")
    suspend fun getManualSessionsCount(): Int
    
    @Query("SELECT COUNT(*) FROM scan_sessions WHERE overallRiskLevel = :riskLevel")
    suspend fun getSessionsCountByRiskLevel(riskLevel: ThreatLevel): Int
    
    @Query("SELECT COUNT(*) FROM scan_sessions WHERE startTimestamp >= :fromTimestamp")
    suspend fun getSessionsCountFromTimestamp(fromTimestamp: Long): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ScanSessionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<ScanSessionEntity>)
    
    @Update
    suspend fun updateSession(session: ScanSessionEntity)
    
    @Delete
    suspend fun deleteSession(session: ScanSessionEntity)
    
    @Query("DELETE FROM scan_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSessionById(sessionId: String)
    
    @Query("DELETE FROM scan_sessions WHERE startTimestamp < :timestamp")
    suspend fun deleteSessionsOlderThan(timestamp: Long)
    
    @Query("DELETE FROM scan_sessions")
    suspend fun deleteAllSessions()
    
    @Query("UPDATE scan_sessions SET endTimestamp = :endTimestamp WHERE sessionId = :sessionId")
    suspend fun endSession(sessionId: String, endTimestamp: Long)
    
    @Query("UPDATE scan_sessions SET totalNetworks = :totalNetworks, safeNetworks = :safeNetworks, lowRiskNetworks = :lowRiskNetworks, mediumRiskNetworks = :mediumRiskNetworks, highRiskNetworks = :highRiskNetworks, criticalRiskNetworks = :criticalRiskNetworks, overallRiskLevel = :overallRiskLevel, totalThreats = :totalThreats WHERE sessionId = :sessionId")
    suspend fun updateSessionStatistics(
        sessionId: String,
        totalNetworks: Int,
        safeNetworks: Int,
        lowRiskNetworks: Int,
        mediumRiskNetworks: Int,
        highRiskNetworks: Int,
        criticalRiskNetworks: Int,
        overallRiskLevel: ThreatLevel,
        totalThreats: Int
    )
    
    // Статистические запросы
    @Query("""
        SELECT 
            DATE(startTimestamp/1000, 'unixepoch') as date,
            COUNT(*) as count,
            AVG(totalNetworks) as avgNetworks,
            AVG(totalThreats) as avgThreats
        FROM scan_sessions 
        WHERE endTimestamp IS NOT NULL
        GROUP BY DATE(startTimestamp/1000, 'unixepoch')
        ORDER BY date DESC
        LIMIT 30
    """)
    suspend fun getDailySessionStatistics(): List<DailySessionStatistics>
    
    @Query("""
        SELECT 
            overallRiskLevel,
            COUNT(*) as count
        FROM scan_sessions 
        WHERE endTimestamp IS NOT NULL
        GROUP BY overallRiskLevel 
        ORDER BY count DESC
    """)
    suspend fun getSessionRiskLevelStatistics(): List<SessionRiskLevelCount>
    
    @Query("""
        SELECT 
            isBackgroundScan,
            COUNT(*) as count,
            AVG(totalNetworks) as avgNetworks,
            AVG(totalThreats) as avgThreats
        FROM scan_sessions 
        WHERE endTimestamp IS NOT NULL
        GROUP BY isBackgroundScan
    """)
    suspend fun getSessionTypeStatistics(): List<SessionTypeStatistics>
    
    @Query("""
        SELECT 
            AVG(totalNetworks) as avgNetworks,
            AVG(totalThreats) as avgThreats,
            AVG(safeNetworks) as avgSafeNetworks,
            AVG(criticalRiskNetworks) as avgCriticalNetworks
        FROM scan_sessions 
        WHERE endTimestamp IS NOT NULL
    """)
    suspend fun getOverallSessionStatistics(): OverallSessionStatistics?
}

/**
 * Результат статистики сессий по дням
 */
data class DailySessionStatistics(
    val date: String,
    val count: Int,
    val avgNetworks: Double,
    val avgThreats: Double
)

/**
 * Результат статистики по уровням риска сессий
 */
data class SessionRiskLevelCount(
    val overallRiskLevel: ThreatLevel,
    val count: Int
)

/**
 * Результат статистики по типам сессий
 */
data class SessionTypeStatistics(
    val isBackgroundScan: Boolean,
    val count: Int,
    val avgNetworks: Double,
    val avgThreats: Double
)

/**
 * Общая статистика сессий
 */
data class OverallSessionStatistics(
    val avgNetworks: Double,
    val avgThreats: Double,
    val avgSafeNetworks: Double,
    val avgCriticalNetworks: Double
)
