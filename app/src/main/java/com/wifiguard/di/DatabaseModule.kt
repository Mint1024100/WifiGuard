package com.wifiguard.di

import android.content.Context
import androidx.room.Room
import com.wifiguard.core.data.local.WifiGuardDatabase
import com.wifiguard.core.data.local.dao.WifiNetworkDao
import com.wifiguard.core.data.local.dao.WifiScanDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt модуль для предоставления компонентов Room базы данных.
 * Конфигурирует базу данных и DAO объекты для dependency injection.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Предоставляет единственный экземпляр Room базы данных для всего приложения.
     * 
     * @param context Контекст приложения
     * @return Экземпляр WifiGuardDatabase
     */
    @Provides
    @Singleton
    fun provideWifiGuardDatabase(
        @ApplicationContext context: Context
    ): WifiGuardDatabase {
        return Room.databaseBuilder(
            context = context,
            klass = WifiGuardDatabase::class.java,
            name = WifiGuardDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // В продакшене заменить на правильные миграции
            .enableMultiInstanceInvalidation() // Поддержка множественных экземпляров
            .build()
    }
    
    /**
     * Предоставляет DAO для операций с Wi-Fi сетями.
     * 
     * @param database Экземпляр базы данных
     * @return WifiNetworkDao
     */
    @Provides
    fun provideWifiNetworkDao(
        database: WifiGuardDatabase
    ): WifiNetworkDao {
        return database.wifiNetworkDao()
    }
    
    /**
     * Предоставляет DAO для операций с результатами сканирования.
     * 
     * @param database Экземпляр базы данных
     * @return WifiScanDao
     */
    @Provides
    fun provideWifiScanDao(
        database: WifiGuardDatabase
    ): WifiScanDao {
        return database.wifiScanDao()
    }
}