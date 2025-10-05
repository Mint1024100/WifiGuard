package com.wifiguard.feature.scanner.data.repository

import android.util.Log
import com.wifiguard.core.common.Constants
import com.wifiguard.core.common.Resource
import com.wifiguard.feature.scanner.data.datasource.WifiDataSource
import com.wifiguard.feature.scanner.data.mapper.WifiInfoMapper
import com.wifiguard.feature.scanner.domain.model.WifiInfo
import com.wifiguard.feature.scanner.domain.repository.WifiScannerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация репозитория для сканирования Wi-Fi сетей
 * 
 * Координирует работу между:
 * - WifiDataSource (получение данных от Android API)
 * - WifiInfoMapper (преобразование DTO в доменные модели)
 * - Кэширование и обработка ошибок
 * 
 * Архитектурные принципы:
 * - Single Source of Truth для Wi-Fi данных
 * - Автоматическое кэширование результатов
 * - Graceful обработка ошибок сети и разрешений
 * - Оптимизация для сохранения батареи
 * - Поддержка реактивного программирования через Flow
 * 
 * @param wifiDataSource источник данных Wi-Fi сканирования
 * @param wifiInfoMapper маппер для преобразования DTO -> Domain
 * 
 * @author WifiGuard Data Team
 * @since 1.0.0
 */
@Singleton
class WifiScannerRepositoryImpl @Inject constructor(
    private val wifiDataSource: WifiDataSource,
    private val wifiInfoMapper: WifiInfoMapper
) : WifiScannerRepository {
    
    companion object {
        private const val TAG = Constants.LogTags.WIFI_SCANNER
        
        // Настройки кэширования
        private const val CACHE_VALIDITY_MS = 30_000L // 30 секунд
        private const val MAX_CACHE_SIZE = 100 // Максимум 100 сетей в кэше
    }
    
    // Локальный кэш для оптимизации производительности
    private var cachedNetworks: List<WifiInfo> = emptyList()
    private var lastScanTime: Long = 0L
    private var lastScanError: Exception? = null
    
    /**
     * Получает список доступных Wi-Fi сетей
     * 
     * Логика работы:
     * 1. Проверяет валидность кэша
     * 2. Если кэш устарел или принудительное обновление - выполняет новое сканирование
     * 3. Преобразует DTO в доменные модели
     * 4. Обновляет кэш и возвращает результат
     * 
     * @param forceRefresh игнорировать кэш и выполнить новое сканирование
     * @return Flow с ресурсом списка Wi-Fi сетей
     */
    override fun getAvailableNetworks(forceRefresh: Boolean): Flow<Resource<List<WifiInfo>>> = flow {
        try {
            Log.d(TAG, "📡 Запрос сканирования Wi-Fi сетей (forceRefresh=$forceRefresh)")
            
            // Эмитим состояние загрузки
            emit(Resource.Loading())
            
            // Проверяем кэш
            val currentTime = System.currentTimeMillis()
            val cacheIsValid = !forceRefresh && 
                             cachedNetworks.isNotEmpty() && 
                             (currentTime - lastScanTime) < CACHE_VALIDITY_MS
            
            if (cacheIsValid) {
                Log.d(TAG, "💾 Возвращаем данные из кэша (${cachedNetworks.size} сетей)")
                emit(Resource.Success(cachedNetworks))
                return@flow
            }
            
            // Проверяем, не превысили ли лимит сканирований (Android throttling)
            if (!forceRefresh && wifiDataSource.getRecentScanCount() >= 4) {
                val waitTime = wifiDataSource.getTimeUntilNextScanAvailable()
                if (waitTime > 0) {
                    Log.w(TAG, "⏱️ Android throttling активен, ожидание ${waitTime}мс")
                    
                    // Если есть кэш, возвращаем его со старыми данными
                    if (cachedNetworks.isNotEmpty()) {
                        emit(Resource.Success(cachedNetworks))
                        return@flow
                    }
                    
                    // Иначе возвращаем ошибку
                    emit(Resource.Error(
                        Exception("Слишком частые сканирования. Подождите ${waitTime/1000} секунд.")
                    ))
                    return@flow
                }
            }
            
            // Выполняем сканирование
            Log.d(TAG, "🔄 Выполняем новое сканирование Wi-Fi сетей")
            val wifiDtoList = wifiDataSource.scanWifiNetworks(forceRefresh)
            
            // Преобразуем DTO в доменные модели
            val wifiNetworks = wifiDtoList.map { dto ->
                wifiInfoMapper.map(dto)
            }
            
            // Фильтруем и сортируем результат
            val processedNetworks = processNetworks(wifiNetworks)
            
            // Обновляем кэш
            updateCache(processedNetworks, currentTime)
            
            Log.i(TAG, "✅ Сканирование завершено успешно: ${processedNetworks.size} сетей")
            emit(Resource.Success(processedNetworks))
            
        } catch (e: SecurityException) {
            Log.e(TAG, "🔒 Нет разрешения на сканирование Wi-Fi", e)
            lastScanError = e
            emit(Resource.Error(Exception(Constants.ErrorMessages.LOCATION_PERMISSION_REQUIRED, e)))
            
        } catch (e: IllegalStateException) {
            Log.e(TAG, "📶 Wi-Fi адаптер недоступен", e)
            lastScanError = e
            emit(Resource.Error(Exception(e.message ?: Constants.ErrorMessages.WIFI_NOT_ENABLED, e)))
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Неожиданная ошибка сканирования", e)
            lastScanError = e
            emit(Resource.Error(Exception(e.message ?: Constants.ErrorMessages.SCAN_FAILED, e)))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Получает информацию о текущем Wi-Fi подключении
     */
    override suspend fun getCurrentConnection(): Resource<WifiInfo?> {
        return try {
            Log.d(TAG, "📱 Получение информации о текущем подключении")
            
            val currentDto = wifiDataSource.getCurrentConnection()
            
            if (currentDto != null) {
                val currentNetwork = wifiInfoMapper.map(currentDto)
                Log.d(TAG, "✅ Текущее подключение: ${currentNetwork.displayName}")
                Resource.Success(currentNetwork)
            } else {
                Log.d(TAG, "📵 Нет активного Wi-Fi подключения")
                Resource.Success(null)
            }
            
        } catch (e: SecurityException) {
            Log.e(TAG, "🔒 Нет разрешения на получение информации о подключении", e)
            Resource.Error(Exception(Constants.ErrorMessages.LOCATION_PERMISSION_REQUIRED, e))
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка получения текущего подключения", e)
            Resource.Error(Exception("Не удалось получить информацию о текущем подключении", e))
        }
    }
    
    /**
     * Проверяет, включён ли Wi-Fi адаптер
     */
    override fun isWifiEnabled(): Boolean {
        return try {
            wifiDataSource.isWifiEnabled()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка проверки состояния Wi-Fi", e)
            false
        }
    }
    
    /**
     * Проверяет наличие необходимых разрешений
     */
    override fun hasRequiredPermissions(): Boolean {
        return try {
            wifiDataSource.hasRequiredPermissions()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка проверки разрешений", e)
            false
        }
    }
    
    /**
     * Получает количество недавних сканирований (для отладки throttling)
     */
    override fun getRecentScanCount(): Int {
        return try {
            wifiDataSource.getRecentScanCount()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка получения счётчика сканирований", e)
            0
        }
    }
    
    /**
     * Получает время до следующего доступного сканирования в миллисекундах
     */
    override fun getTimeUntilNextScan(): Long {
        return try {
            wifiDataSource.getTimeUntilNextScanAvailable()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка получения времени ожидания", e)
            0L
        }
    }
    
    /**
     * Возвращает последнюю ошибку сканирования (если есть)
     */
    override fun getLastScanError(): Exception? {
        return lastScanError
    }
    
    /**
     * Очищает кэш сканированных сетей
     */
    override fun clearCache() {
        Log.d(TAG, "🗑️ Очистка кэша сканирования")
        cachedNetworks = emptyList()
        lastScanTime = 0L
        lastScanError = null
    }
    
    /**
     * Обрабатывает список сетей: фильтрует, сортирует, очищает дубликаты
     */
    private fun processNetworks(networks: List<WifiInfo>): List<WifiInfo> {
        return networks
            .asSequence()
            // Фильтруем некорректные данные
            .filter { network ->
                network.bssid.isNotBlank() && 
                network.frequency > 0 && 
                network.signalStrength < 0
            }
            // Убираем дубликаты по BSSID (один BSSID = одна точка доступа)
            .distinctBy { it.bssid }
            // Сортируем по силе сигнала (сильные сверху)
            .sortedByDescending { it.signalStrength }
            // Ограничиваем количество для производительности
            .take(MAX_CACHE_SIZE)
            .toList()
    }
    
    /**
     * Обновляет локальный кэш
     */
    private fun updateCache(networks: List<WifiInfo>, scanTime: Long) {
        cachedNetworks = networks
        lastScanTime = scanTime
        lastScanError = null // Очищаем предыдущие ошибки при успешном сканировании
        
        Log.d(TAG, "💾 Кэш обновлён: ${networks.size} сетей, время: ${scanTime}")
        
        // Логируем топ-5 сетей для отладки
        if (Constants.ENABLE_DEBUG_LOGGING && networks.isNotEmpty()) {
            Log.d(TAG, "📊 Топ-5 сетей по силе сигнала:")
            networks.take(5).forEachIndexed { index, network ->
                Log.d(TAG, "  ${index + 1}. ${network.displayName} (${network.signalStrength} dBm, ${network.encryptionType.displayName})")
            }
        }
    }
    
    /**
     * Возвращает статистику кэша для мониторинга производительности
     */
    fun getCacheStatistics(): CacheStats {
        return CacheStats(
            cachedNetworksCount = cachedNetworks.size,
            lastScanTimestamp = lastScanTime,
            cacheAgeMs = System.currentTimeMillis() - lastScanTime,
            isValidCache = (System.currentTimeMillis() - lastScanTime) < CACHE_VALIDITY_MS,
            hasError = lastScanError != null
        )
    }
    
    /**
     * Статистика кэша для мониторинга
     */
    data class CacheStats(
        val cachedNetworksCount: Int,
        val lastScanTimestamp: Long,
        val cacheAgeMs: Long,
        val isValidCache: Boolean,
        val hasError: Boolean
    )
}