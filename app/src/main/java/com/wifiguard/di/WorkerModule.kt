package com.wifiguard.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt модуль для предоставления WorkManager компонентов.
 * Обеспечивает фоновую обработку для мониторинга Wi-Fi сетей.
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {
    
    /**
     * Предоставляет WorkManager экземпляр для управления фоновыми задачами.
     * 
     * @param context Контекст приложения
     * @return WorkManager экземпляр
     */
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }
}