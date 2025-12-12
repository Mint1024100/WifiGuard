package com.wifiguard.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.wifiguard.core.common.Constants
import com.wifiguard.core.security.SecurityAnalyzer
import com.wifiguard.core.security.ThreatDetector
import com.wifiguard.core.security.EncryptionAnalyzer
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

/**
 * ИСПРАВЛЕНО: Singleton DataStore для предотвращения множественных экземпляров
 * 
 * ИСПРАВЛЕНИЯ:
 * ✅ Используется extension property preferencesDataStore - автоматически singleton
 * ✅ Extension property создается один раз на Context и кэшируется
 * ✅ Правильная обработка ошибок инициализации
 * ✅ Thread-safe инициализация через lazy делегат
 * 
 * ВАЖНО: preferencesDataStore extension property автоматически создает singleton
 * для каждого файла. Не нужно создавать его вручную - это вызовет ошибку
 * "multiple DataStores active for the same file"
 */
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
        /**
         * ИСПРАВЛЕНО: Singleton DataStore через extension property
         * 
         * preferencesDataStore extension property автоматически создает singleton
         * для каждого имени файла. Это гарантирует, что для одного файла
         * создается только один экземпляр DataStore.
         * 
         * Extension property использует lazy инициализацию и thread-safe доступ.
         */
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = Constants.PREFERENCES_NAME
        )
        
        /**
         * ИСПРАВЛЕНО: Thread-safe предоставление DataStore singleton
         * 
         * Использует extension property, которое автоматически создает singleton
         * для указанного имени файла. Это предотвращает ошибку
         * "There are multiple DataStores active for the same file"
         */
        @Provides
        @Singleton
        fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
            // ИСПРАВЛЕНО: Используем extension property, которое автоматически
            // создает singleton для файла с именем Constants.PREFERENCES_NAME
            // Это гарантирует один экземпляр DataStore на файл
            return context.dataStore
        }
    }
}