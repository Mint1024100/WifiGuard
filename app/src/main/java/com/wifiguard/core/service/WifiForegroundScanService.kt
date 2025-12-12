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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Foreground Service для выполнения полного сканирования Wi-Fi сетей
 * Используется для обхода ограничений Android 10+ на фоновое сканирование
 * 
 * КРИТИЧЕСКИЕ ИСПРАВЛЕНИЯ БЕЗОПАСНОСТИ И ПРОИЗВОДИТЕЛЬНОСТИ:
 * ✅ Использует SupervisorJob() для изоляции ошибок дочерних корутин
 * ✅ START_STICKY для автоматического перезапуска при убийстве системой
 * ✅ Корректная отмена корутин в onDestroy()
 * ✅ CoroutineExceptionHandler для обработки необработанных исключений
 * ✅ AtomicBoolean для предотвращения множественных запусков сканирования
 * ✅ Проверка isActive перед длительными операциями
 * 
 * @author WifiGuard Security Team
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
    
    // Обработчик исключений для корутин
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "❌ Необработанное исключение в serviceScope: ${throwable.message}", throwable)
        // Останавливаем сервис при критической ошибке
        stopSelf()
    }
    
    // ИСПРАВЛЕНО: SupervisorJob + CoroutineExceptionHandler для безопасного выполнения
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Main + exceptionHandler)
    
    // Флаг для предотвращения множественных сканирований
    private val isScanningInProgress = AtomicBoolean(false)
    
    // Текущая задача сканирования для отмены
    private var scanJob: Job? = null
    
    companion object {
        private const val TAG = "WifiForegroundScanService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "wifi_scan_channel"
        private const val CHANNEL_NAME = "WiFi Сканирование"
        
        /**
         * Запустить foreground сканирование
         */
        fun start(context: Context) {
            Log.d(TAG, "🚀 Запуск foreground сканирования")
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
            Log.d(TAG, "🛑 Остановка foreground сканирования")
            val intent = Intent(context, WifiForegroundScanService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "📦 Service created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "▶️ Service started (startId=$startId)")
        
        // Запускаем foreground notification СРАЗУ (требование Android 8+)
        val notification = createNotification("Подготовка к сканированию...")
        startForeground(NOTIFICATION_ID, notification)
        
        // Проверяем, не выполняется ли уже сканирование
        if (isScanningInProgress.compareAndSet(false, true)) {
            // Запускаем сканирование в корутине
            scanJob = serviceScope.launch {
                try {
                    performFullScan()
                } finally {
                    isScanningInProgress.set(false)
                }
            }
        } else {
            Log.w(TAG, "⚠️ Сканирование уже выполняется, пропускаем")
        }
        
        // ИСПРАВЛЕНО: START_STICKY - сервис будет перезапущен системой
        // если будет убит из-за нехватки памяти
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        Log.d(TAG, "🗑️ Service destroyed")
        
        // ИСПРАВЛЕНО: Отменяем текущую задачу сканирования
        scanJob?.cancel()
        scanJob = null
        
        // ИСПРАВЛЕНО: Отменяем все корутины в scope
        serviceScope.cancel()
        
        // Сбрасываем флаг сканирования
        isScanningInProgress.set(false)
        
        super.onDestroy()
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
     * 
     * ИСПРАВЛЕНО: Добавлены проверки isActive для корректной отмены
     */
    private suspend fun performFullScan() {
        Log.d(TAG, "🔍 Starting full scan")
        
        try {
            // ИСПРАВЛЕНО: Проверка isActive перед операциями
            if (!serviceScope.isActive) {
                Log.w(TAG, "⚠️ Scope неактивен, прерывание сканирования")
                return
            }
            
            // Проверяем, включен ли WiFi
            if (!wifiScannerService.isWifiEnabled()) {
                Log.w(TAG, "⚠️ WiFi is not enabled")
                updateNotification("WiFi отключен")
                if (serviceScope.isActive) delay(2000)
                stopSelf()
                return
            }
            
            updateNotification("Сканирование сетей...")
            
            // ИСПРАВЛЕНО: Проверка isActive перед длительной операцией
            if (!serviceScope.isActive) return
            
            // Запускаем сканирование
            val scanStatus = wifiScannerService.startScan()
            
            // ИСПРАВЛЕНО: Проверка isActive после операции
            if (!serviceScope.isActive) return
            
            when (scanStatus) {
                is WifiScanStatus.Success -> {
                    Log.d(TAG, "✅ Scan successful")
                    updateNotification("Обработка результатов...")
                    
                    // Небольшая задержка для получения результатов
                    if (serviceScope.isActive) delay(1000)
                    if (!serviceScope.isActive) return
                    
                    // Получаем результаты с метаданными
                    val (networks, metadata) = wifiScannerService.getScanResultsWithMetadata()
                    Log.d(TAG, "📊 Found ${networks.size} networks")
                    
                    if (networks.isNotEmpty() && serviceScope.isActive) {
                        updateNotification("Найдено ${networks.size} сетей. Анализ безопасности...")
                        
                        // Сохраняем результаты
                        networks.forEach { network ->
                            if (!serviceScope.isActive) return
                            wifiRepository.insertScanResult(network)
                        }
                        
                        if (!serviceScope.isActive) return
                        
                        // Анализируем безопасность
                        val securityReport = securityAnalyzer.analyzeNetworks(networks, metadata)
                        Log.d(TAG, "🛡️ Security analysis complete. Found ${securityReport.threats.size} threats")
                        
                        // Сохраняем угрозы
                        if (securityReport.threats.isNotEmpty() && serviceScope.isActive) {
                            threatRepository.insertThreats(securityReport.threats)
                            updateNotification("Обнаружено ${securityReport.threats.size} угроз")
                        } else {
                            updateNotification("Угроз не обнаружено")
                        }
                        
                        if (serviceScope.isActive) delay(2000)
                    } else {
                        Log.w(TAG, "⚠️ No networks found")
                        updateNotification("Сети не найдены")
                        if (serviceScope.isActive) delay(2000)
                    }
                }
                
                is WifiScanStatus.Throttled -> {
                    Log.w(TAG, "⏳ Scan throttled")
                    val minutesUntilNext = (scanStatus.nextAvailableTime - System.currentTimeMillis()) / 60000
                    updateNotification("Сканирование ограничено. Повторите через $minutesUntilNext мин.")
                    if (serviceScope.isActive) delay(3000)
                }
                
                is WifiScanStatus.Restricted -> {
                    Log.w(TAG, "🚫 Scan restricted: ${scanStatus.reason}")
                    updateNotification("Сканирование ограничено системой")
                    if (serviceScope.isActive) delay(3000)
                }
                
                is WifiScanStatus.Failed -> {
                    Log.e(TAG, "❌ Scan failed: ${scanStatus.error}")
                    updateNotification("Ошибка сканирования: ${scanStatus.error}")
                    if (serviceScope.isActive) delay(3000)
                }
            }
            
            Log.d(TAG, "✅ Full scan completed")
        } catch (e: kotlinx.coroutines.CancellationException) {
            // ИСПРАВЛЕНО: Корректная обработка отмены корутины
            Log.d(TAG, "🛑 Сканирование отменено")
            throw e // Пробрасываем для корректной отмены
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during full scan", e)
            if (serviceScope.isActive) {
                updateNotification("Ошибка: ${e.message}")
                delay(3000)
            }
        } finally {
            // Останавливаем сервис
            stopSelf()
        }
    }
}
