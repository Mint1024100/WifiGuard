package com.wifiguard.testing

import android.content.Context
import android.net.wifi.WifiManager
import com.wifiguard.core.data.wifi.WifiScanner
import com.wifiguard.di.NetworkModule
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Тестовый модуль для подмены сетевых зависимостей.
 *
 * ВАЖНО: заменяем реальный [WifiScanner] на [FakeWifiScanner], чтобы UI тесты
 * не зависели от Wi‑Fi окружения, разрешений OEM и состояния геолокации.
 *
 * Дополнительно: предоставляем [WifiManager], так как он требуется
 * `WifiScannerService`, `WifiScannerViewModel` и другим компонентам.
 */
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class]
)
@Module
object TestNetworkModule {

    @Provides
    @Singleton
    fun provideWifiManager(
        @ApplicationContext context: Context
    ): WifiManager {
        return context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    @Provides
    @Singleton
    fun provideWifiScanner(fakeWifiScanner: FakeWifiScanner): WifiScanner = fakeWifiScanner
}






