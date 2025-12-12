package com.wifiguard.di

import android.content.Context
import android.util.Log
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
 * 
 * КРИТИЧЕСКИЕ ИСПРАВЛЕНИЯ БЕЗОПАСНОСТИ:
 * ✅ Используется единый источник истины для создания БД (WifiGuardDatabase.getDatabase)
 * ✅ НЕ используется fallbackToDestructiveMigration() - предотвращает потерю данных
 * ✅ Все миграции централизованы в WifiGuardDatabase
 * ✅ Добавлена обработка ошибок при создании экземпляра БД
 * 
 * @author WifiGuard Security Team
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    private const val TAG = "DatabaseModule"
    
    /**
     * Предоставляет singleton экземпляр базы данных WifiGuard
     * 
     * ВАЖНО: Использует централизованный метод WifiGuardDatabase.getDatabase()
     * для обеспечения единообразной конфигурации миграций
     * 
     * @param context Контекст приложения
     * @return WifiGuardDatabase экземпляр базы данных
     * @throws IllegalStateException если миграция не найдена
     */
    @Provides
    @Singleton
    fun provideWifiGuardDatabase(
        @ApplicationContext context: Context
    ): WifiGuardDatabase {
        Log.d(TAG, "📦 Создание экземпляра базы данных через Hilt")
        return try {
            // Используем централизованный метод создания БД
            // который содержит все миграции и НЕ использует fallbackToDestructiveMigration
            WifiGuardDatabase.getDatabase(context)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Критическая ошибка создания БД: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Предоставляет DAO для работы с историей Wi-Fi сканирований
     */
    @Provides
    fun provideWifiScanDao(database: WifiGuardDatabase): WifiScanDao {
        return database.wifiScanDao()
    }
    
    /**
     * Предоставляет DAO для работы с Wi-Fi сетями
     */
    @Provides
    fun provideWifiNetworkDao(database: WifiGuardDatabase): WifiNetworkDao {
        return database.wifiNetworkDao()
    }
    
    /**
     * Предоставляет DAO для работы с угрозами безопасности
     */
    @Provides
    fun provideThreatDao(database: WifiGuardDatabase): ThreatDao {
        return database.threatDao()
    }
    
    /**
     * Предоставляет DAO для работы с сессиями сканирования
     */
    @Provides
    fun provideScanSessionDao(database: WifiGuardDatabase): ScanSessionDao {
        return database.scanSessionDao()
    }
}