package com.wifiguard.di

import android.content.Context
import android.net.wifi.WifiManager
import com.wifiguard.core.data.wifi.WifiCapabilitiesAnalyzer
import com.wifiguard.core.data.wifi.WifiScanner
import com.wifiguard.core.data.wifi.WifiScannerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Модуль для предоставления сетевых зависимостей
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideWifiManager(
        @ApplicationContext context: Context
    ): WifiManager {
        return context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    
    @Provides
    @Singleton
    fun provideWifiCapabilitiesAnalyzer(): WifiCapabilitiesAnalyzer {
        return WifiCapabilitiesAnalyzer()
    }
    
    @Provides
    @Singleton
    fun provideWifiScanner(
        @ApplicationContext context: Context
    ): WifiScanner {
        return WifiScannerImpl(context)
    }
}