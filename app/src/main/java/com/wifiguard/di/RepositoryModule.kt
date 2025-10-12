package com.wifiguard.di

import com.wifiguard.core.data.repository.SettingsRepositoryImpl
import com.wifiguard.core.data.repository.WifiRepositoryImpl
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.feature.settings.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
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
     * Биндит интерфейс SettingsRepository к его реализации SettingsRepositoryImpl.
     * Обеспечивает связь между интерфейсом настроек и DataStore реализацией.
     * 
     * @param settingsRepositoryImpl Реализация репозитория настроек
     * @return Интерфейс SettingsRepository
     */
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}