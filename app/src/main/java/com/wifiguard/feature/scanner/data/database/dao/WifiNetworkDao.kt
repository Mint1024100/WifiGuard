package com.wifiguard.feature.scanner.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.wifiguard.feature.scanner.data.database.entity.WifiNetworkEntity

@Dao
interface WifiNetworkDao {
    
    @Query("SELECT * FROM wifi_networks ORDER BY lastSeenTimestamp DESC")
    fun getAllNetworks(): Flow<List<WifiNetworkEntity>>
    
    @Query("SELECT * FROM wifi_networks WHERE bssid = :bssid")
    suspend fun getNetworkByBssid(bssid: String): WifiNetworkEntity?
    
    @Query("SELECT * FROM wifi_networks WHERE ssid LIKE :ssid")
    fun getNetworksBySsid(ssid: String): Flow<List<WifiNetworkEntity>>
    
    @Query("SELECT * FROM wifi_networks WHERE securityType = :securityType")
    fun getNetworksBySecurityType(securityType: String): Flow<List<WifiNetworkEntity>>
    
    @Query("SELECT * FROM wifi_networks WHERE lastSeenTimestamp >= :timestamp")
    fun getRecentNetworks(timestamp: Long): Flow<List<WifiNetworkEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetwork(network: WifiNetworkEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetworks(networks: List<WifiNetworkEntity>)
    
    @Update
    suspend fun updateNetwork(network: WifiNetworkEntity)
    
    @Delete
    suspend fun deleteNetwork(network: WifiNetworkEntity)
    
    @Query("DELETE FROM wifi_networks WHERE lastSeenTimestamp < :timestamp")
    suspend fun deleteOldNetworks(timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM wifi_networks")
    suspend fun getNetworkCount(): Int
}