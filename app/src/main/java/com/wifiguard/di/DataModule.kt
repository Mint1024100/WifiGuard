package com.wifiguard.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.wifiguard.core.common.Constants
import com.wifiguard.feature.scanner.data.datasource.WifiDataSource
import com.wifiguard.feature.scanner.data.datasource.WifiDataSourceImpl
import com.wifiguard.feature.scanner.data.repository.WifiScannerRepositoryImpl
import com.wifiguard.feature.scanner.domain.repository.WifiScannerRepository
import com.wifiguard.feature.settings.data.datasource.SettingsDataSource
import com.wifiguard.feature.settings.data.datasource.SettingsDataSourceImpl
import com.wifiguard.feature.settings.data.repository.SettingsRepositoryImpl
import com.wifiguard.feature.settings.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.PREFERENCES_NAME
)

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    
    @Binds
    abstract fun bindWifiDataSource(
        wifiDataSourceImpl: WifiDataSourceImpl
    ): WifiDataSource
    
    @Binds
    abstract fun bindWifiScannerRepository(
        wifiScannerRepositoryImpl: WifiScannerRepositoryImpl
    ): WifiScannerRepository
    
    @Binds
    abstract fun bindSettingsDataSource(
        settingsDataSourceImpl: SettingsDataSourceImpl
    ): SettingsDataSource
    
    @Binds
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
    
    companion object {
        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
            return context.dataStore
        }
    }
}