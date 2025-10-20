package com.wifiguard.core.data.local.dao

import androidx.room.*
import com.wifiguard.core.data.local.entity.WifiNetworkEntity
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.ThreatLevelCount
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object для работы с таблицей wifi_networks.
 * 
 * Предоставляет методы для CRUD операций с Wi-Fi сетями,
 * а также специализированные запросы для анализа безопасности.
 */
@Dao
interface WifiNetworkDao {
    
    /**
     * Получить все сети с реальным обновлением
     */
    @Query("SELECT * FROM wifi_networks ORDER BY last_seen DESC")
    fun getAllNetworks(): Flow<List<WifiNetworkEntity>>
    
    /**
     * Получить сеть по BSSID
     */
    @Query("SELECT * FROM wifi_networks WHERE bssid = :bssid LIMIT 1")
    suspend fun getNetworkByBssid(bssid: String): WifiNetworkEntity?
    
    /**
     * Получить сеть по SSID
     */
    @Query("SELECT * FROM wifi_networks WHERE ssid = :ssid LIMIT 1")
    suspend fun getNetworkBySSID(ssid: String): WifiNetworkEntity?
    
    /**
     * Получить все сети с указанным уровнем угрозы
     */
    @Query("SELECT * FROM wifi_networks WHERE threat_level = :threatLevel ORDER BY last_seen DESC")
    fun getNetworksByThreatLevel(threatLevel: ThreatLevel): Flow<List<WifiNetworkEntity>>
    
    /**
     * Получить подозрительные сети
     */
    @Query("SELECT * FROM wifi_networks WHERE is_suspicious = 1 ORDER BY last_seen DESC")
    fun getSuspiciousNetworks(): Flow<List<WifiNetworkEntity>>
    
    /**
     * Получить сети, обнаруженные за последние N миллисекунд
     */
    @Query("SELECT * FROM wifi_networks WHERE last_seen > :timestamp ORDER BY last_seen DESC")
    fun getRecentNetworks(timestamp: Long): Flow<List<WifiNetworkEntity>>
    
    /**
     * Поиск сетей по SSID
     */
    @Query("SELECT * FROM wifi_networks WHERE ssid LIKE '%' || :query || '%' ORDER BY last_seen DESC")
    fun searchNetworksByName(query: String): Flow<List<WifiNetworkEntity>>
    
    /**
     * Получить количество сетей по уровням угроз
     */
    @Query("SELECT threat_level, COUNT(*) as count FROM wifi_networks GROUP BY threat_level")
    suspend fun getNetworkCountByThreatLevel(): List<ThreatLevelCount>
    
    /**
     * Добавить новую сеть
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetwork(network: WifiNetworkEntity): Long
    
    /**
     * Добавить несколько сетей
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetworks(networks: List<WifiNetworkEntity>): List<Long>
    
    /**
     * Обновить существующую сеть
     */
    @Update
    suspend fun updateNetwork(network: WifiNetworkEntity)
    
    /**
     * Обновить время последнего обнаружения и счетчик
     */
    @Query("UPDATE wifi_networks SET last_seen = :timestamp, detection_count = detection_count + 1 WHERE bssid = :bssid")
    suspend fun updateLastSeen(bssid: String, timestamp: Long)
    
    /**
     * Обновить уровень угрозы
     */
    @Query("UPDATE wifi_networks SET threat_level = :threatLevel WHERE id = :networkId")
    suspend fun updateThreatLevel(networkId: Long, threatLevel: ThreatLevel)
    
    /**
     * Отметить сеть как подозрительную
     */
    @Query("UPDATE wifi_networks SET is_suspicious = :suspicious WHERE id = :networkId")
    suspend fun markAsSuspicious(networkId: Long, suspicious: Boolean)
    
    /**
     * Удалить сеть
     */
    @Delete
    suspend fun deleteNetwork(network: WifiNetworkEntity)
    
    /**
     * Удалить все сети
     */
    @Query("DELETE FROM wifi_networks")
    suspend fun deleteAllNetworks()
    
    /**
     * Удалить старые сети (ольдер 30 дней)
     */
    @Query("DELETE FROM wifi_networks WHERE last_seen < :cutoffTimestamp")
    suspend fun deleteOldNetworks(cutoffTimestamp: Long)
}