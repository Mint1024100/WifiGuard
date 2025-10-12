package com.wifiguard.di

import android.content.Context
import androidx.room.Room
import com.wifiguard.core.data.local.WifiGuardDatabase
import com.wifiguard.core.data.local.dao.ScanSessionDao
import com.wifiguard.core.data.local.dao.ThreatDao
import com.wifiguard.core.data.local.dao.WifiNetworkDao
import com.wifiguard.core.data.local.dao.WifiScanDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Модуль для предоставления зависимостей базы данных
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideWifiGuardDatabase(
        @ApplicationContext context: Context
    ): WifiGuardDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            WifiGuardDatabase::class.java,
            WifiGuardDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    fun provideWifiScanDao(database: WifiGuardDatabase): WifiScanDao {
        return database.wifiScanDao()
    }
    
    @Provides
    fun provideWifiNetworkDao(database: WifiGuardDatabase): WifiNetworkDao {
        return database.wifiNetworkDao()
    }
    
    @Provides
    fun provideThreatDao(database: WifiGuardDatabase): ThreatDao {
        return database.threatDao()
    }
    
    @Provides
    fun provideScanSessionDao(database: WifiGuardDatabase): ScanSessionDao {
        return database.scanSessionDao()
    }
}