package com.wifiguard.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
// РЕЗЕРВНАЯ КОПИЯ: Удаленные импорты для DataTransferManager
// import com.wifiguard.core.data.local.DataTransferManager
// import com.wifiguard.core.data.local.WifiGuardDatabase
import javax.inject.Singleton

/**
 * Основной Hilt модуль приложения для предоставления глобальных зависимостей.
 * Содержит только компоненты, не связанные с базой данных.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    /**
     * Предоставляет WorkManager для выполнения фоновых задач.
     * 
     * @param context Контекст приложения
     * @return WorkManager экземпляр
     */
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    // РЕЗЕРВНАЯ КОПИЯ: Удаленный провайдер DataTransferManager
    // @Provides
    // @Singleton
    // fun provideDataTransferManager(
    //     @ApplicationContext context: Context,
    //     wifiGuardDatabase: WifiGuardDatabase
    // ): DataTransferManager {
    //     return DataTransferManager(context, wifiGuardDatabase)
    // }
}