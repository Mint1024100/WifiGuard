package com.wifiguard.feature.scanner.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.wifiguard.feature.scanner.data.database.dao.WifiNetworkDao
import com.wifiguard.feature.scanner.data.database.dao.ScanHistoryDao
import com.wifiguard.feature.scanner.data.database.entity.WifiNetworkEntity
import com.wifiguard.feature.scanner.data.database.entity.ScanHistoryEntity
import com.wifiguard.feature.scanner.data.database.converter.Converters

@Database(
    entities = [
        WifiNetworkEntity::class,
        ScanHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WifiGuardDatabase : RoomDatabase() {
    
    abstract fun wifiNetworkDao(): WifiNetworkDao
    abstract fun scanHistoryDao(): ScanHistoryDao
}