package com.wifiguard.core.data.local.dao

import androidx.room.*
import com.wifiguard.core.data.local.entity.WifiNetworkEntity
import com.wifiguard.core.data.local.entity.toDomainModel
import com.wifiguard.core.domain.model.WifiNetwork
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DAO (Data Access Object) для управления Wi-Fi сетями в Room базе данных.
 * Определяет SQL операции для CRUD операций с Wi-Fi сетями.
 */
@Dao
abstract class WifiNetworkDao {
    
    /**
     * Получить все Wi-Fi сети, отсортированные по времени последнего обновления
     */
    @Query("SELECT * FROM wifi_networks ORDER BY last_updated DESC")
    abstract fun getAllNetworksFlow(): Flow<List<WifiNetworkEntity>>
    
    fun getAllNetworks(): Flow<List<WifiNetwork>> {
        return getAllNetworksFlow().map { entities -> 
            entities.map { it.toDomainModel() }
        }
    }
    
    /**
     * Получить сеть по SSID
     */
    @Query("SELECT * FROM wifi_networks WHERE ssid = :ssid LIMIT 1")
    abstract suspend fun getNetworkEntityBySSID(ssid: String): WifiNetworkEntity?
    
    suspend fun getNetworkBySSID(ssid: String): WifiNetwork? {
        return getNetworkEntityBySSID(ssid)?.toDomainModel()
    }
    
    /**
     * Вставить или обновить Wi-Fi сеть
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertNetworkEntity(network: WifiNetworkEntity)
    
    suspend fun insertNetwork(network: WifiNetwork) {
        insertNetworkEntity(network.toEntity())
    }
    
    /**
     * Обновить Wi-Fi сеть
     */
    @Update
    abstract suspend fun updateNetworkEntity(network: WifiNetworkEntity)
    
    suspend fun updateNetwork(network: WifiNetwork) {
        updateNetworkEntity(network.toEntity())
    }
    
    /**
     * Удалить Wi-Fi сеть
     */
    @Delete
    abstract suspend fun deleteNetworkEntity(network: WifiNetworkEntity)
    
    suspend fun deleteNetwork(network: WifiNetwork) {
        deleteNetworkEntity(network.toEntity())
    }
    
    /**
     * Получить подозрительные сети
     */
    @Query("SELECT * FROM wifi_networks WHERE is_suspicious = 1 ORDER BY last_updated DESC")
    abstract fun getSuspiciousNetworksFlow(): Flow<List<WifiNetworkEntity>>
    
    fun getSuspiciousNetworks(): Flow<List<WifiNetwork>> {
        return getSuspiciousNetworksFlow().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    /**
     * Получить сети по частоте
     */
    @Query("SELECT * FROM wifi_networks WHERE frequency = :frequency ORDER BY signal_strength DESC")
    abstract fun getNetworksByFrequency(frequency: Int): Flow<List<WifiNetworkEntity>>
    
    /**
     * Получить сети по каналу
     */
    @Query("SELECT * FROM wifi_networks WHERE channel = :channel ORDER BY signal_strength DESC")
    abstract fun getNetworksByChannel(channel: Int): Flow<List<WifiNetworkEntity>>
    
    /**
     * Получить статистику - количество сетей
     */
    @Query("SELECT COUNT(*) FROM wifi_networks")
    abstract suspend fun getNetworksCount(): Int
    
    /**
     * Получить количество подозрительных сетей
     */
    @Query("SELECT COUNT(*) FROM wifi_networks WHERE is_suspicious = 1")
    abstract suspend fun getSuspiciousNetworksCount(): Int
    
    /**
     * Очистить старые сети (не видели больше 30 дней)
     */
    @Query("DELETE FROM wifi_networks WHERE last_seen < :olderThanMillis AND is_known = 0")
    abstract suspend fun cleanupOldNetworks(olderThanMillis: Long)
    
    /**
     * Очистить все сети
     */
    @Query("DELETE FROM wifi_networks")
    abstract suspend fun clearAllNetworks()
}