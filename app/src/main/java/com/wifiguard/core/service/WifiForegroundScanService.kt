package com.wifiguard.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.wifiguard.MainActivity
import com.wifiguard.R
import com.wifiguard.core.data.wifi.WifiScannerService
import com.wifiguard.core.domain.model.WifiScanStatus
import com.wifiguard.core.domain.repository.ThreatRepository
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.core.security.SecurityAnalyzer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground Service для выполнения полного сканирования Wi-Fi сетей
 * Используется для обхода ограничений Android 10+ на фоновое сканирование
 */
@AndroidEntryPoint
class WifiForegroundScanService : Service() {
    
    @Inject
    lateinit var wifiScannerService: WifiScannerService
    
    @Inject
    lateinit var securityAnalyzer: SecurityAnalyzer
    
    @Inject
    lateinit var wifiRepository: WifiRepository
    
    @Inject
    lateinit var threatRepository: ThreatRepository
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    companion object {
        private const val TAG = "WifiForegroundScanService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "wifi_scan_channel"
        private const val CHANNEL_NAME = "WiFi Сканирование"
        
        /**
         * Запустить foreground сканирование
         */
        fun start(context: Context) {
            val intent = Intent(context, WifiForegroundScanService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * Остановить foreground сканирование
         */
        fun stop(context: Context) {
            val intent = Intent(context, WifiForegroundScanService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        
        // Запускаем foreground notification
        val notification = createNotification("Подготовка к сканированию...")
        startForeground(NOTIFICATION_ID, notification)
        
        // Запускаем сканирование в корутине
        serviceScope.launch {
            performFullScan()
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        serviceScope.cancel()
    }
    
    /**
     * Создать notification channel для Android 8+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомления о сканировании Wi-Fi сетей"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }
    
    /**
     * Создать notification
     */
    private fun createNotification(contentText: String): Notification {
        // Intent для открытия приложения при нажатии на уведомление
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WiFi Guard сканирование")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_wifi_scan) // Убедитесь, что этот ресурс существует
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
    
    /**
     * Обновить notification
     */
    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Выполнить полное сканирование
     */
    private suspend fun performFullScan() {
        Log.d(TAG, "Starting full scan")
        
        try {
            // Проверяем, включен ли WiFi
            if (!wifiScannerService.isWifiEnabled()) {
                Log.w(TAG, "WiFi is not enabled")
                updateNotification("WiFi отключен")
                delay(2000)
                stopSelf()
                return
            }
            
            updateNotification("Сканирование сетей...")
            
            // Запускаем сканирование
            val scanStatus = wifiScannerService.startScan()
            
            when (scanStatus) {
                is WifiScanStatus.Success -> {
                    Log.d(TAG, "Scan successful")
                    updateNotification("Обработка результатов...")
                    
                    // Небольшая задержка для получения результатов
                    delay(1000)
                    
                    // Получаем результаты с метаданными
                    val (networks, metadata) = wifiScannerService.getScanResultsWithMetadata()
                    Log.d(TAG, "Found ${networks.size} networks")
                    
                    if (networks.isNotEmpty()) {
                        updateNotification("Найдено ${networks.size} сетей. Анализ безопасности...")
                        
                        // Сохраняем результаты
                        networks.forEach { network ->
                            wifiRepository.insertScanResult(network)
                        }
                        
                        // Анализируем безопасность
                        val securityReport = securityAnalyzer.analyzeNetworks(networks, metadata)
                        Log.d(TAG, "Security analysis complete. Found ${securityReport.threats.size} threats")
                        
                        // Сохраняем угрозы
                        if (securityReport.threats.isNotEmpty()) {
                            threatRepository.insertThreats(securityReport.threats)
                            updateNotification("Обнаружено ${securityReport.threats.size} угроз")
                        } else {
                            updateNotification("Угроз не обнаружено")
                        }
                        
                        delay(2000)
                    } else {
                        Log.w(TAG, "No networks found")
                        updateNotification("Сети не найдены")
                        delay(2000)
                    }
                }
                
                is WifiScanStatus.Throttled -> {
                    Log.w(TAG, "Scan throttled")
                    val minutesUntilNext = (scanStatus.nextAvailableTime - System.currentTimeMillis()) / 60000
                    updateNotification("Сканирование ограничено. Повторите через $minutesUntilNext мин.")
                    delay(3000)
                }
                
                is WifiScanStatus.Restricted -> {
                    Log.w(TAG, "Scan restricted: ${scanStatus.reason}")
                    updateNotification("Сканирование ограничено системой")
                    delay(3000)
                }
                
                is WifiScanStatus.Failed -> {
                    Log.e(TAG, "Scan failed: ${scanStatus.error}")
                    updateNotification("Ошибка сканирования: ${scanStatus.error}")
                    delay(3000)
                }
            }
            
            Log.d(TAG, "Full scan completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during full scan", e)
            updateNotification("Ошибка: ${e.message}")
            delay(3000)
        } finally {
            // Останавливаем сервис
            stopSelf()
        }
    }
}
