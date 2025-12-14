package com.wifiguard.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wifiguard.MainActivity
import com.wifiguard.R
import com.wifiguard.core.data.wifi.ScanStatusBus
import com.wifiguard.core.data.wifi.ScanStatusState
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
import kotlinx.coroutines.CancellationException
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

    @Inject
    lateinit var scanStatusBus: ScanStatusBus
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isScanInProgress = false
    
    companion object {
        private const val TAG = "WifiForegroundScanService"
        private const val NOTIFICATION_ID = 1001
        private const val PROMPT_NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "wifi_scan_channel"
        private const val CHANNEL_NAME = "Wi-Fi Сканирование"
        
        /**
         * Запустить foreground сканирование
         * Android 14+ требует разрешение FOREGROUND_SERVICE_LOCATION
         */
        fun start(context: Context) {
            Log.d(TAG, "Requesting service start")
            
            // Android 14+: старт foreground service может быть запрещён, если приложение в фоне
            // (ForegroundServiceStartNotAllowedException). Также для типа "location" необходимо
            // иметь разрешение на геолокацию.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val hasFineLocation = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (!hasFineLocation) {
                    Log.w(TAG, "ACCESS_FINE_LOCATION не предоставлено. Нельзя запускать location foreground service.")
                    showOpenAppPrompt(
                        context = context,
                        message = "Чтобы запустить сканирование, откройте приложение и предоставьте разрешение " +
                            "на геолокацию."
                    )
                    return
                }
            }
            
            try {
                val intent = Intent(context, WifiForegroundScanService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (t: Throwable) {
                // На Android 14+ при попытке запуска из фона возможен ForegroundServiceStartNotAllowedException.
                Log.e(TAG, "Не удалось запустить foreground service: ${t.message}", t)
                showOpenAppPrompt(
                    context = context,
                    message = "Сканирование нельзя запустить из фона. Откройте приложение и повторите."
                )
            }
        }

        private fun showOpenAppPrompt(context: Context, message: String) {
            try {
                ensurePromptChannel(context)

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_wifi_scan)
                    .setContentTitle("Требуется действие")
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()

                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(PROMPT_NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                Log.e(TAG, "Не удалось показать уведомление-подсказку: ${e.message}", e)
            }
        }

        private fun ensurePromptChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existing != null) return

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Уведомления о запуске сканирования Wi‑Fi"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(channel)
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
        Log.d(TAG, "Service started, isScanInProgress: $isScanInProgress")
        
        // Если сканирование уже выполняется, игнорируем повторный запуск
        if (isScanInProgress) {
            Log.d(TAG, "Scan already in progress, ignoring duplicate start request")
            return START_NOT_STICKY
        }
        
        // Запускаем foreground notification
        val notification = createNotification("Подготовка к сканированию...")
        startForeground(NOTIFICATION_ID, notification)
        
        // Запускаем сканирование в корутине
        serviceScope.launch {
            try {
                isScanInProgress = true
                performFullScan()
            } finally {
                isScanInProgress = false
            }
        }
        
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        isScanInProgress = false
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
            .setContentTitle("Wi-Fi Guard сканирование")
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
        Log.d(TAG, "Starting full scan, isScanInProgress: $isScanInProgress")
        
        try {
            scanStatusBus.update(ScanStatusState.Scanning())

            // Проверяем, включен ли WiFi
            if (!wifiScannerService.isWifiEnabled()) {
                Log.w(TAG, "WiFi is not enabled")
                updateNotification("Wi-Fi отключен")
                scanStatusBus.update(ScanStatusState.Result(WifiScanStatus.Failed("Wi‑Fi отключен")))
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
                    scanStatusBus.update(ScanStatusState.Processing())
                    
                    // Небольшая задержка для получения результатов
                    delay(1000)
                    
                    // Получаем результаты с метаданными
                    val (networks, metadata) = wifiScannerService.getScanResultsWithMetadata()
                    Log.d(TAG, "Found ${networks.size} networks")
                    
                    if (networks.isNotEmpty()) {
                        updateNotification("Найдено ${networks.size} сетей. Анализ безопасности...")
                        scanStatusBus.update(ScanStatusState.Processing(networksCount = networks.size))
                        
                        // Сохраняем результаты батчем, чтобы избежать «шторма» обновлений Room/Flow
                        wifiRepository.insertScanResults(networks)
                        
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

                        scanStatusBus.update(
                            ScanStatusState.Completed(
                                networksCount = networks.size,
                                threatsCount = securityReport.threats.size
                            )
                        )
                        
                        delay(2000)
                    } else {
                        Log.w(TAG, "No networks found")
                        updateNotification("Сети не найдены")
                        scanStatusBus.update(ScanStatusState.Result(WifiScanStatus.Failed("Сети не найдены")))
                        delay(2000)
                    }
                }
                
                is WifiScanStatus.Throttled -> {
                    Log.w(TAG, "Scan throttled")
                    val minutesUntilNext = (scanStatus.nextAvailableTime - System.currentTimeMillis()) / 60000
                    updateNotification("Сканирование ограничено. Повторите через $minutesUntilNext мин.")
                    scanStatusBus.update(ScanStatusState.Result(scanStatus))
                    delay(3000)
                }
                
                is WifiScanStatus.Restricted -> {
                    Log.w(TAG, "Scan restricted: ${scanStatus.reason}")
                    updateNotification("Сканирование ограничено системой")
                    scanStatusBus.update(ScanStatusState.Result(scanStatus))
                    delay(3000)
                }
                
                is WifiScanStatus.Failed -> {
                    Log.e(TAG, "Scan failed: ${scanStatus.error}")
                    updateNotification("Ошибка сканирования: ${scanStatus.error}")
                    scanStatusBus.update(ScanStatusState.Result(scanStatus))
                    delay(3000)
                }
            }
            
            Log.d(TAG, "Full scan completed")
        } catch (e: CancellationException) {
            // ВАЖНО: CancellationException нельзя подавлять — пробрасываем дальше
            // Это нормальное поведение при остановке сервиса или отмене корутины
            Log.d(TAG, "Scan cancelled (normal behavior) - this is expected when service stops", e)
            // Не обновляем статус как ошибку - это нормальная отмена
            // scanStatusBus обновляется в onDestroy или при следующем запуске
            throw e // Пробрасываем дальше
        } catch (e: Exception) {
            Log.e(TAG, "Error during full scan", e)
            updateNotification("Ошибка: ${e.message}")
            scanStatusBus.update(
                ScanStatusState.Result(
                    WifiScanStatus.Failed(e.message ?: "Неизвестная ошибка")
                )
            )
            delay(3000)
        } finally {
            // Останавливаем сервис только если не было CancellationException
            // (при CancellationException сервис уже будет остановлен)
            try {
                stopSelf()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping service", e)
            }
        }
    }
}
