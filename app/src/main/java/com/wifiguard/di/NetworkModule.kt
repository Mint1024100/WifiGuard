package com.wifiguard.di

import android.content.Context
import android.net.wifi.WifiManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt модуль для предоставления сетевых компонентов и Wi-Fi сервисов.
 * Обеспечивает доступ к WifiManager и корутинным диспетчерам.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    /**
     * Предоставляет системный WifiManager для сканирования Wi-Fi сетей.
     * 
     * @param context Контекст приложения
     * @return WifiManager экземпляр
     */
    @Provides
    @Singleton
    fun provideWifiManager(
        @ApplicationContext context: Context
    ): WifiManager {
        return context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    
    /**
     * Предоставляет IO диспетчер для выполнения I/O операций.
     * 
     * @return CoroutineDispatcher для IO операций
     */
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    
    /**
     * Предоставляет Main диспетчер для UI операций.
     * 
     * @return CoroutineDispatcher для Main потока
     */
    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
    
    /**
     * Предоставляет Default диспетчер для CPU-интенсивных операций.
     * 
     * @return CoroutineDispatcher для вычислений
     */
    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

/**
 * Квалифайеры для различных типов корутинных диспетчеров
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher