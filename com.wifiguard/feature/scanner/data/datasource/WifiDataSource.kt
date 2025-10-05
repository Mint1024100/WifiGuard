package com.wifiguard.feature.scanner.data.datasource

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.wifiguard.core.common.Constants
import com.wifiguard.core.security.SecurityManager
import com.wifiguard.feature.scanner.data.model.WifiInfoDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Локальный источник данных для сканирования Wi-Fi сетей
 * 
 * Основные обязанности:
 * - Взаимодействие с Android WifiManager API
 * - Проверка разрешений и состояния Wi-Fi
 * - Преобразование ScanResult в WifiInfoDto
 * - Обработка ошибок и исключительных ситуаций
 * - Оптимальное управление частотой сканирования
 * 
 * Особенности реализации:
 * - Адаптирован для API 26+ (таргет 35)
 * - Поддерживает все современные диапазоны: 2.4/5/6 ГГц
 * - Обрабатывает ограничения Android по частоте сканирования
 * - Оптимизирован для сохранения батареи
 * 
 * @param context Контекст приложения для доступа к системным сервисам
 * 
 * @author WifiGuard Data Team
 * @since 1.0.0
 */
@Singleton
class WifiDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = Constants.LogTags.WIFI_SCANNER
        
        // Ограничения Android по частоте сканирования
        private const val MIN_SCAN_INTERVAL_MS = 4000L // 4 секунды минимум
        private const val SCAN_THROTTLE_COUNT = 4 // Макс 4 скана в 2 минуты
        private const val SCAN_THROTTLE_WINDOW_MS = 120_000L // 2 минуты
    }
    
    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    
    // Учёт частоты сканирования для обхода Android throttling
    private var lastScanTime: Long = 0
    private val scanTimestamps = mutableListOf<Long>()
    
    /**
     * Выполняет сканирование доступных Wi-Fi сетей
     * 
     * Проверяет разрешения, состояние Wi-Fi и выполняет сканирование.
     * Автоматически обрабатывает Android throttling и ограничения API.
     * 
     * @param forceRefresh игнорировать throttling и выполнить новое сканирование
     * @return Список WifiInfoDto с результатами сканирования
     * @throws SecurityException если нет разрешения на местоположение
     * @throws IllegalStateException если Wi-Fi отключён или недоступен
     */
    suspend fun scanWifiNetworks(forceRefresh: Boolean = false): List<WifiInfoDto> = 
        withContext(Dispatchers.IO) {
            Log.d(TAG, "🔎 Начало сканирования Wi-Fi сетей (forceRefresh=$forceRefresh)")
            
            // 1. Проверка разрешений
            validatePermissions()
            
            // 2. Проверка состояния Wi-Fi
            validateWifiState()
            
            // 3. Проверка throttling (если не принудительное обновление)
            if (!forceRefresh && isThrottled()) {
                Log.d(TAG, "⏱️ Сканирование отложено из-за Android throttling")
                throw IllegalStateException(
                    "Слишком частые сканирования. Пожалуйста, подождите несколько секунд."
                )
            }
            
            // 4. Запуск сканирования
            val scanStarted = wifiManager.startScan()
            if (!scanStarted) {
                Log.e(TAG, "❌ Не удалось запустить сканирование")
                throw IllegalStateException(
                    "Не удалось запустить сканирование Wi-Fi. Проверьте состояние адаптера."
                )
            }
            
            // 5. Обновляем статистику throttling
            updateScanTimestamps()
            
            // 6. Ожидание завершения сканирования
            // Android может требовать 1-3 секунды для обновления результатов
            delay(2000L)
            
            // 7. Получение результатов сканирования
            val scanResults = try {
                wifiManager.scanResults
            } catch (e: SecurityException) {
                Log.e(TAG, "❌ Нет разрешения на получение результатов сканирования", e)
                throw SecurityException("Необходимо разрешение на доступ к местоположению")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка получения результатов сканирования", e)
                emptyList()
            }
            
            // 8. Преобразование в DTO с валидацией
            val wifiInfoList = scanResults.mapNotNull { scanResult ->
                try {
                    convertToWifiInfoDto(scanResult)
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Ошибка обработки сети ${scanResult.SSID}: ${e.message}")
                    null // Пропускаем некорректные результаты
                }
            }
            
            // 9. Логирование и возврат результата
            Log.i(TAG, "✅ Обнаружено ${wifiInfoList.size} Wi-Fi сетей")
            
            if (Constants.ENABLE_DEBUG_LOGGING) {
                logScanResults(wifiInfoList)
            }
            
            wifiInfoList
        }
    
    /**
     * Получает информацию о текущем Wi-Fi подключении
     */
    suspend fun getCurrentConnection(): WifiInfoDto? = withContext(Dispatchers.IO) {
        try {
            validatePermissions()
            
            val connectionInfo = wifiManager.connectionInfo ?: return@withContext null
            val currentSSID = connectionInfo.ssid?.removeSurrounding("\"") ?: return@withContext null
            val currentBSSID = connectionInfo.bssid ?: return@withContext null
            
            // Поищем текущую сеть в результатах сканирования
            val scanResults = wifiManager.scanResults
            val currentNetwork = scanResults.find { 
                it.SSID == currentSSID && it.BSSID == currentBSSID 
            }
            
            currentNetwork?.let { convertToWifiInfoDto(it) }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка получения текущего подключения", e)
            null
        }
    }
    
    /**
     * Проверяет состояние Wi-Fi адаптера
     */
    fun isWifiEnabled(): Boolean = wifiManager.isWifiEnabled
    
    /**
     * Проверяет наличие всех необходимых разрешений
     */
    fun hasRequiredPermissions(): Boolean {
        val locationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ требует FINE_LOCATION
            ActivityCompat.checkSelfPermission(
                context, 
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 9 и ниже - достаточно COARSE_LOCATION
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
        
        val wifiStatePermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED
        
        return locationPermission && wifiStatePermission
    }
    
    /**
     * Проверяет разрешения и выбрасывает исключение при отсутствии
     */
    private fun validatePermissions() {
        if (!hasRequiredPermissions()) {
            throw SecurityException(Constants.ErrorMessages.LOCATION_PERMISSION_REQUIRED)
        }
    }
    
    /**
     * Проверяет состояние Wi-Fi адаптера
     */
    private fun validateWifiState() {
        if (!wifiManager.isWifiEnabled) {
            throw IllegalStateException(Constants.ErrorMessages.WIFI_NOT_ENABLED)
        }
    }
    
    /**
     * Проверяет, не слишком ли часто выполняется сканирование
     */
    private fun isThrottled(): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // Проверка минимального интервала
        if (currentTime - lastScanTime < MIN_SCAN_INTERVAL_MS) {
            return true
        }
        
        // Очищаем устаревшие записи
        scanTimestamps.removeAll { currentTime - it > SCAN_THROTTLE_WINDOW_MS }
        
        // Проверка лимита сканирований в окне
        return scanTimestamps.size >= SCAN_THROTTLE_COUNT
    }
    
    /**
     * Обновляет статистику сканирования
     */
    private fun updateScanTimestamps() {
        val currentTime = System.currentTimeMillis()
        lastScanTime = currentTime
        scanTimestamps.add(currentTime)
    }
    
    /**
     * Преобразует Android ScanResult в WifiInfoDto
     */
    private fun convertToWifiInfoDto(scanResult: ScanResult): WifiInfoDto {
        return WifiInfoDto(
            ssid = scanResult.SSID ?: "",
            bssid = scanResult.BSSID ?: "",
            capabilities = scanResult.capabilities ?: "",
            frequency = scanResult.frequency,
            level = scanResult.level,
            timestamp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                scanResult.timestamp / 1000 // Конвертация из микросекунд в мс
            } else {
                @Suppress("DEPRECATION")
                scanResult.timestamp / 1000
            },
            channelWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mapChannelWidth(scanResult.channelWidth)
            } else {
                determineChannelWidthByFrequency(scanResult.frequency)
            },
            is5GHz = scanResult.frequency in Constants.FREQ_5_GHZ_MIN..Constants.FREQ_5_GHZ_MAX,
            is6GHz = scanResult.frequency >= 5925, // 6 ГГц диапазон
            metadata = extractMetadata(scanResult)
        )
    }
    
    /**
     * Маппит channelWidth из Android ScanResult
     */
    private fun mapChannelWidth(channelWidth: Int): Int {
        return when (channelWidth) {
            ScanResult.CHANNEL_WIDTH_20MHZ -> 20
            ScanResult.CHANNEL_WIDTH_40MHZ -> 40
            ScanResult.CHANNEL_WIDTH_80MHZ -> 80
            ScanResult.CHANNEL_WIDTH_160MHZ -> 160
            ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> 160 // 80+80 = 160 эквивалент
            else -> 20 // По умолчанию
        }
    }
    
    /**
     * Определяет ширину канала по частоте для старых версий Android
     */
    private fun determineChannelWidthByFrequency(frequency: Int): Int {
        return when (frequency) {
            in Constants.FREQ_2_4_GHZ_MIN..Constants.FREQ_2_4_GHZ_MAX -> 20 // 2.4 ГГц обычно 20 МГц
            in Constants.FREQ_5_GHZ_MIN..Constants.FREQ_5_GHZ_MAX -> 80 // 5 ГГц обычно 80 Мгц
            else -> 20
        }
    }
    
    /**
     * Извлекает дополнительные метаданные из ScanResult
     */
    private fun extractMetadata(scanResult: ScanResult): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        
        try {
            // Информация о производителе по OUI (Organizationally Unique Identifier)
            val oui = scanResult.BSSID?.take(8)?.replace(":", "")?.uppercase()
            if (!oui.isNullOrBlank()) {
                metadata["vendor"] = getVendorByOUI(oui)
            }
            
            // Информация о возможностях сети
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Можно добавить более детальную информацию для API 30+
                metadata["api_level"] = Build.VERSION.SDK_INT.toString()
            }
            
            // Определение стандарта Wi-Fi по capabilities
            val wifiStandard = determineWifiStandard(scanResult.capabilities)
            if (wifiStandard.isNotBlank()) {
                metadata["wifi_standard"] = wifiStandard
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Ошибка извлечения метаданных", e)
        }
        
        return metadata
    }
    
    /**
     * Определяет производителя по OUI (первые 3 байта MAC-адреса)
     */
    private fun getVendorByOUI(oui: String): String {
        return when {
            oui.startsWith("00248C") -> "Ubiquiti Networks"
            oui.startsWith("001DD8") -> "Mikrotik"
            oui.startsWith("F8C4F3") || oui.startsWith("2C5D93") -> "TP-Link"
            oui.startsWith("502B73") || oui.startsWith("7CE9D3") -> "Cisco Systems"
            oui.startsWith("9094E4") || oui.startsWith("2CF05D") -> "ASUS"
            oui.startsWith("107B44") || oui.startsWith("F0B479") -> "Apple Inc."
            oui.startsWith("20C9D0") || oui.startsWith("E46F13") -> "D-Link Corporation"
            oui.startsWith("000B6B") || oui.startsWith("001E2A") -> "Netgear"
            oui.startsWith("001999") -> "Belkin International"
            oui.startsWith("0050F2") -> "Microsoft Corporation"
            oui.startsWith("6805CA") -> "Xiaomi"
            oui.startsWith("8863DF") -> "Huawei Technologies"
            else -> "Неизвестный"
        }
    }
    
    /**
     * Определяет стандарт Wi-Fi по capabilities строке
     */
    private fun determineWifiStandard(capabilities: String): String {
        return when {
            capabilities.contains("802.11be", ignoreCase = true) -> "Wi-Fi 7 (802.11be)"
            capabilities.contains("802.11ax", ignoreCase = true) -> "Wi-Fi 6 (802.11ax)"
            capabilities.contains("802.11ac", ignoreCase = true) -> "Wi-Fi 5 (802.11ac)"
            capabilities.contains("802.11n", ignoreCase = true) -> "Wi-Fi 4 (802.11n)"
            capabilities.contains("802.11g", ignoreCase = true) -> "Wi-Fi 3 (802.11g)"
            capabilities.contains("802.11a", ignoreCase = true) -> "Wi-Fi 2 (802.11a)"
            capabilities.contains("802.11b", ignoreCase = true) -> "Wi-Fi 1 (802.11b)"
            else -> ""
        }
    }
    
    /**
     * Логирует результаты сканирования для отладки
     */
    private fun logScanResults(networks: List<WifiInfoDto>) {
        Log.d(TAG, "=== РЕЗУЛЬТАТЫ СКАНИРОВАНИЯ ===")
        
        networks.forEachIndexed { index, network ->
            Log.d(TAG, "${index + 1}. ${network.ssid.ifBlank { "<Скрытая>" }}")
            Log.d(TAG, "   BSSID: ${network.bssid}")
            Log.d(TAG, "   Сигнал: ${network.level} dBm")
            Log.d(TAG, "   Частота: ${network.frequency} Мгц (${network.getFrequencyBand()})")
            Log.d(TAG, "   Канал: ${network.getChannelNumber()}")
            Log.d(TAG, "   Безопасность: ${network.capabilities}")
            Log.d(TAG, "   Производитель: ${network.metadata["vendor"] ?: "Неизвестный"}")
            Log.d(TAG, "   ---")
        }
        
        // Статистика по диапазонам
        val band24Count = networks.count { it.frequency in Constants.FREQ_2_4_GHZ_MIN..Constants.FREQ_2_4_GHZ_MAX }
        val band5Count = networks.count { it.is5GHz }
        val band6Count = networks.count { it.is6GHz }
        
        Log.d(TAG, "📊 Статистика: 2.4ГГц=$band24Count, 5ГГц=$band5Count, 6ГГц=$band6Count")
        
        Log.d(TAG, "=== КОНЕЦ РЕЗУЛЬТАТОВ ===")
    }
    
    /**
     * Получает количество сканирований за последние 2 минуты
     */
    fun getRecentScanCount(): Int {
        val currentTime = System.currentTimeMillis()
        scanTimestamps.removeAll { currentTime - it > SCAN_THROTTLE_WINDOW_MS }
        return scanTimestamps.size
    }
    
    /**
     * Оценивает оставшееся время до следующего доступного сканирования
     */
    fun getTimeUntilNextScanAvailable(): Long {
        val timeSinceLastScan = System.currentTimeMillis() - lastScanTime
        return (MIN_SCAN_INTERVAL_MS - timeSinceLastScan).coerceAtLeast(0L)
    }
}