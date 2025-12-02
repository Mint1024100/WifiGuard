package com.wifiguard.di

import com.wifiguard.core.common.*
import com.wifiguard.core.data.repository.WifiRepositoryImpl
import com.wifiguard.core.data.repository.ThreatRepositoryImpl
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.core.domain.repository.ThreatRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt модуль для биндинга репозиториев.
 * Обеспечивает связь между интерфейсами доменного слоя и их реализациями в data слое.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    /**
     * Биндит интерфейс WifiRepository к его реализации WifiRepositoryImpl.
     * Это позволяет Hilt автоматически предоставлять конкретную реализацию,
     * когда запрашивается интерфейс.
     * 
     * @param wifiRepositoryImpl Реализация репозитория
     * @return Интерфейс WifiRepository
     */
    @Binds
    @Singleton
    abstract fun bindWifiRepository(
        wifiRepositoryImpl: WifiRepositoryImpl
    ): WifiRepository
    
    /**
     * Биндит интерфейс ThreatRepository к его реализации ThreatRepositoryImpl.
     * Это позволяет Hilt автоматически предоставлять конкретную реализацию,
     * когда запрашивается интерфейс.
     * 
     * @param threatRepositoryImpl Реализация репозитория угроз
     * @return Интерфейс ThreatRepository
     */
    @Binds
    @Singleton
    abstract fun bindThreatRepository(
        threatRepositoryImpl: ThreatRepositoryImpl
    ): ThreatRepository
    
    companion object {
        @Provides
        @Singleton
        fun provideWifiNetworkEntityToDomainMapper(): WifiNetworkEntityToDomainMapper {
            return WifiNetworkEntityToDomainMapper()
        }
        
        @Provides
        @Singleton
        fun provideWifiNetworkDomainToEntityMapper(): WifiNetworkDomainToEntityMapper {
            return WifiNetworkDomainToEntityMapper()
        }
        
        @Provides
        @Singleton
        fun provideWifiScanEntityToDomainMapper(): WifiScanEntityToDomainMapper {
            return WifiScanEntityToDomainMapper()
        }
        
        @Provides
        @Singleton
        fun provideWifiScanDomainToEntityMapper(): WifiScanDomainToEntityMapper {
            return WifiScanDomainToEntityMapper()
        }
        
        @Provides
        @Singleton
        fun provideThreatEntityToDomainMapper(): ThreatEntityToDomainMapper {
            return ThreatEntityToDomainMapper()
        }
        
        @Provides
        @Singleton
        fun provideThreatDomainToEntityMapper(): ThreatDomainToEntityMapper {
            return ThreatDomainToEntityMapper()
        }
    }
    
    // SettingsRepository биндится в DataModule.kt
}