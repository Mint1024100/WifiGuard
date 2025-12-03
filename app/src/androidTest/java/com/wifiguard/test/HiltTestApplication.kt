package com.wifiguard.test

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wifiguard.WifiGuardApp
import dagger.hilt.android.testing.CustomTestApplication

/**
 * Test Application for Hilt testing
 */
@CustomTestApplication(WifiGuardHiltTestApplication::class)
interface HiltTestApplication

class WifiGuardHiltTestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize the actual application
        val originalApp = WifiGuardApp()
        originalApp.attachBaseContext(ApplicationProvider.getApplicationContext<Context>())
        originalApp.onCreate()
    }
}