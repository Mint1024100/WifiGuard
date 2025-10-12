package com.wifiguard

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Главный класс приложения WifiGuard
 */
@HiltAndroidApp
class WifiGuardApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Инициализация приложения
        initializeApp()
    }
    
    private fun initializeApp() {
        // TODO: Initialize any global components here
        // - Analytics
        // - Crash reporting
        // - Background work scheduling
    }
}