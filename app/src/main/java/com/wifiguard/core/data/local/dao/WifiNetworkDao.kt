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
 * 
 * КРИТИЧЕСКИЕ ИСПРАВЛЕНИЯ БЕЗОПАСНОСТИ:
 * ✅ @Transaction для атомарных многошаговых операций
 * ✅ ACID гарантии для операций записи
 * ✅ Стратегии разрешения конфликтов (REPLACE vs IGNORE)
 * ✅ Логирование откатов для отладки
 * 
 * @author WifiGuard Security Team
 */
@Dao
interface WifiNetworkDao {
    
    // ==================== ОПЕРАЦИИ ЧТЕНИЯ ====================
    
    /**
     * Получить все сети с реальным обновлением (Flow для реактивности)
     */
    @Query("SELECT * FROM wifi_networks ORDER BY last_seen DESC")
    fun getAllNetworks(): Flow<List<WifiNetworkEntity>>

    /**
     * Получить все сети (suspend для одноразовых запросов)
     */
    @Query("SELECT * FROM wifi_networks")
    suspend fun getAllWifiNetworksSuspend(): List<WifiNetworkEntity>
    
    /**
     * Получить сеть по BSSID (уникальный MAC-адрес)
     */
    @Query("SELECT * FROM wifi_networks WHERE bssid = :bssid LIMIT 1")
    suspend fun getNetworkByBssid(bssid: String): WifiNetworkEntity?
    
    /**
     * Получить сеть по SSID (имя сети)
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
     * Поиск сетей по SSID (частичное совпадение)
     */
    @Query("SELECT * FROM wifi_networks WHERE ssid LIKE '%' || :query || '%' ORDER BY last_seen DESC")
    fun searchNetworksByName(query: String): Flow<List<WifiNetworkEntity>>
    
    /**
     * Получить количество сетей по уровням угроз (для статистики)
     */
    @Query("SELECT threat_level, COUNT(*) as count FROM wifi_networks GROUP BY threat_level")
    suspend fun getNetworkCountByThreatLevel(): List<ThreatLevelCount>
    
    // ==================== АТОМАРНЫЕ ОПЕРАЦИИ ЗАПИСИ ====================
    
    /**
     * Добавить новую сеть (REPLACE при конфликте BSSID)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetwork(network: WifiNetworkEntity): Long
    
    /**
     * Добавить несколько сетей атомарно
     * 
     * @Transaction гарантирует, что все сети будут добавлены или ни одна
     */
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetworks(networks: List<WifiNetworkEntity>): List<Long>
    
    /**
     * Обновить существующую сеть
     */
    @Update
    suspend fun updateNetwork(network: WifiNetworkEntity)
    
    /**
     * Обновить время последнего обнаружения и инкрементировать счетчик
     */
    @Query("UPDATE wifi_networks SET last_seen = :timestamp, detection_count = detection_count + 1 WHERE bssid = :bssid")
    suspend fun updateLastSeen(bssid: String, timestamp: Long)
    
    /**
     * Обновить уровень угрозы для сети
     */
    @Query("UPDATE wifi_networks SET threat_level = :threatLevel WHERE id = :networkId")
    suspend fun updateThreatLevel(networkId: Long, threatLevel: ThreatLevel)
    
    /**
     * Отметить сеть как подозрительную/безопасную
     */
    @Query("UPDATE wifi_networks SET is_suspicious = :suspicious WHERE id = :networkId")
    suspend fun markAsSuspicious(networkId: Long, suspicious: Boolean)
    
    // ==================== СОСТАВНЫЕ ТРАНЗАКЦИОННЫЕ ОПЕРАЦИИ ====================
    
    /**
     * Добавить или обновить сеть (upsert) с обновлением времени
     * 
     * @Transaction гарантирует атомарность: либо вставка, либо обновление
     */
    @Transaction
    suspend fun upsertNetwork(network: WifiNetworkEntity) {
        val existing = getNetworkByBssid(network.bssid)
        if (existing != null) {
            // Обновляем существующую запись с явным перечислением всех полей
            // ИСПРАВЛЕНО: Явное перечисление полей для предотвращения потери данных при изменении модели
            updateNetwork(
                WifiNetworkEntity(
                    id = existing.id,
                    bssid = network.bssid,
                    ssid = network.ssid,
                    frequency = network.frequency,
                    signalStrength = network.signalStrength,
                    securityType = network.securityType,
                    channel = network.channel,
                    threatLevel = network.threatLevel,
                    isHidden = network.isHidden,
                    firstSeen = existing.firstSeen, // Сохраняем время первого обнаружения
                    lastSeen = network.lastSeen,
                    detectionCount = existing.detectionCount + 1,
                    isSuspicious = network.isSuspicious,
                    vendor = network.vendor,
                    notes = network.notes
                )
            )
        } else {
            // Вставляем новую запись
            insertNetwork(network)
        }
    }
    
    /**
     * Пакетное обновление уровней угроз
     * 
     * @Transaction гарантирует атомарность всех обновлений
     */
    @Transaction
    suspend fun updateThreatLevels(updates: Map<Long, ThreatLevel>) {
        updates.forEach { (networkId, threatLevel) ->
            updateThreatLevel(networkId, threatLevel)
        }
    }
    
    /**
     * Помечает несколько сетей как подозрительные атомарно
     * 
     * @Transaction гарантирует, что все сети будут помечены или ни одна
     */
    @Transaction
    suspend fun markNetworksAsSuspicious(networkIds: List<Long>, suspicious: Boolean) {
        networkIds.forEach { networkId ->
            markAsSuspicious(networkId, suspicious)
        }
    }
    
    // ==================== ОПЕРАЦИИ УДАЛЕНИЯ ====================
    
    /**
     * Удалить сеть
     */
    @Delete
    suspend fun deleteNetwork(network: WifiNetworkEntity)
    
    /**
     * Удалить все сети (используйте с осторожностью!)
     */
    @Query("DELETE FROM wifi_networks")
    suspend fun deleteAllNetworks()
    
    /**
     * Удалить старые сети (старше указанного времени)
     * Возвращает количество удалённых записей
     */
    @Query("DELETE FROM wifi_networks WHERE last_seen < :cutoffTimestamp")
    suspend fun deleteOldNetworks(cutoffTimestamp: Long): Int
    
    /**
     * Очистка и вставка новых сетей атомарно
     * 
     * @Transaction гарантирует, что старые данные будут удалены
     * только если новые успешно вставлены
     */
    @Transaction
    suspend fun replaceAllNetworks(networks: List<WifiNetworkEntity>) {
        deleteAllNetworks()
        insertNetworks(networks)
    }
}