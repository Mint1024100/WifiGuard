package com.wifiguard.core.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.wifiguard.core.data.local.converter.DatabaseConverters
import com.wifiguard.core.data.local.dao.ScanSessionDao
import com.wifiguard.core.data.local.dao.ThreatDao
import com.wifiguard.core.data.local.dao.WifiNetworkDao
import com.wifiguard.core.data.local.dao.WifiScanDao
import com.wifiguard.core.data.local.entity.ScanSessionEntity
import com.wifiguard.core.data.local.entity.ThreatEntity
import com.wifiguard.core.data.local.entity.WifiNetworkEntity
import com.wifiguard.core.data.local.entity.WifiScanEntity

/**
 * Основная база данных WifiGuard
 */
@Database(
    entities = [
        WifiScanEntity::class,
        WifiNetworkEntity::class,
        ThreatEntity::class,
        ScanSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class WifiGuardDatabase : RoomDatabase() {
    
    abstract fun wifiScanDao(): WifiScanDao
    abstract fun wifiNetworkDao(): WifiNetworkDao
    abstract fun threatDao(): ThreatDao
    abstract fun scanSessionDao(): ScanSessionDao
    
    companion object {
        const val DATABASE_NAME = "wifiguard_database"
        
        @Volatile
        private var INSTANCE: WifiGuardDatabase? = null
        
        fun getDatabase(context: Context): WifiGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WifiGuardDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}