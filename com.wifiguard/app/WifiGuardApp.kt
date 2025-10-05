package com.wifiguard.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.wifiguard.core.common.Constants
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Главный класс приложения WifiGuard
 * 
 * Инициализирует все ключевые компоненты:
 * - Dependency Injection (Hilt)
 * - Каналы уведомлений для системы безопасности
 * - WorkManager для фонового мониторинга Wi-Fi
 * - Глобальный обработчик исключений
 * 
 * Архитектурные принципы:
 * - Single Activity с Jetpack Compose
 * - Clean Architecture с разделением на модули
 * - Безопасная инициализация всех сервисов
 */
@HiltAndroidApp
class WifiGuardApp : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    companion object {
        private const val TAG = Constants.LOG_TAG
        
        @Volatile
        private var INSTANCE: WifiGuardApp? = null
        
        fun getInstance(): WifiGuardApp {
            return INSTANCE ?: throw IllegalStateException(
                "WifiGuardApp не был инициализирован. Проверьте AndroidManifest.xml"
            )
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        INSTANCE = this
        
        Log.i(TAG, "🚀 Запуск WifiGuard приложения...")
        
        try {
            initializeNotificationChannels()
            setupGlobalExceptionHandler()
            
            Log.i(TAG, "✅ WifiGuard успешно инициализирован")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Критическая ошибка инициализации", e)
            // В production можно отправить crash report
        }
    }
    
    /**
     * Создаёт каналы уведомлений для Android 8.0+
     */
    private fun initializeNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Канал для критических угроз безопасности
            val securityChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о критических угрозах безопасности Wi-Fi сетей"
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setShowBadge(true)
            }
            
            // Канал для фонового мониторинга
            val monitoringChannel = NotificationChannel(
                "${Constants.NOTIFICATION_CHANNEL_ID}_monitoring",
                "Фоновый мониторинг Wi-Fi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Статус фонового мониторинга Wi-Fi сетей"
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
            
            // Канал для системных уведомлений
            val systemChannel = NotificationChannel(
                "${Constants.NOTIFICATION_CHANNEL_ID}_system",
                "Системные уведомления",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Системные уведомления и обновления приложения"
                enableLights(false)
                enableVibration(true)
                setShowBadge(true)
            }
            
            notificationManager.apply {
                createNotificationChannel(securityChannel)
                createNotificationChannel(monitoringChannel)
                createNotificationChannel(systemChannel)
            }
            
            Log.d(TAG, "📱 Каналы уведомлений созданы")
        }
    }
    
    /**
     * Настраивает глобальный обработчик исключений
     */
    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                Log.e(TAG, "💥 Критическое исключение в потоке: ${thread.name}", exception)
                
                if (Constants.ENABLE_DEBUG_LOGGING) {
                    Log.e(TAG, "=== ДЕТАЛЬНАЯ ИНФОРМАЦИЯ ===")
                    Log.e(TAG, "Поток: ${thread.name}")
                    Log.e(TAG, "Исключение: ${exception.javaClass.simpleName}")
                    Log.e(TAG, "Сообщение: ${exception.message}")
                    exception.printStackTrace()
                }
                
                // В production отправляем в аналитику
                // crashlytics.recordException(exception)
                
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка в обработчике исключений", e)
            } finally {
                defaultHandler?.uncaughtException(thread, exception)
            }
        }
    }
    
    /**
     * Конфигурация WorkManager с поддержкой Hilt DI
     */
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(
                if (Constants.ENABLE_DEBUG_LOGGING) Log.DEBUG else Log.INFO
            )
            .setMaxSchedulerLimit(20) // Максимум 20 задач одновременно
            .build()
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "⚠️ Система сигнализирует о нехватке памяти")
        
        try {
            // Очищаем кэши при нехватке памяти
            // Можно добавить очистку image cache, network cache и т.д.
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при освобождении памяти", e)
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        try {
            INSTANCE = null
            WorkManager.getInstance(this).cancelAllWork()
            Log.i(TAG, "🔚 WifiGuard корректно завершён")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при завершении приложения", e)
        }
    }
}