package com.wifiguard.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Результат сканирования Wi-Fi сетей
 * Используется для прозрачного отображения статуса сканирования
 */
sealed class WifiScanStatus {
    /**
     * Сканирование успешно выполнено
     * @param timestamp время выполнения сканирования
     */
    data class Success(val timestamp: Long) : WifiScanStatus()
    
    /**
     * Сканирование ограничено системой (throttling)
     * @param nextAvailableTime время, когда сканирование будет доступно
     */
    data class Throttled(val nextAvailableTime: Long) : WifiScanStatus()
    
    /**
     * Сканирование ограничено системой (Android 10+)
     * @param reason причина ограничения
     */
    data class Restricted(val reason: String) : WifiScanStatus()
    
    /**
     * Сканирование не удалось
     * @param error описание ошибки
     */
    data class Failed(val error: String) : WifiScanStatus()
}

/**
 * Метаданные сканирования
 * Содержит информацию о свежести и источнике данных
 */
@Parcelize
data class ScanMetadata(
    val timestamp: Long,
    val source: ScanSource,
    val freshness: Freshness
) : Parcelable {
    
    /**
     * Проверить, являются ли данные свежими
     */
    fun isFresh(): Boolean = freshness == Freshness.FRESH
    
    /**
     * Проверить, являются ли данные устаревшими
     */
    fun isStale(): Boolean = freshness == Freshness.STALE
    
    /**
     * Проверить, истек ли срок действия данных
     */
    fun isExpired(): Boolean = freshness == Freshness.EXPIRED
    
    /**
     * Получить возраст данных в миллисекундах
     */
    fun getAge(): Long = System.currentTimeMillis() - timestamp
    
    /**
     * Получить возраст данных в секундах
     */
    fun getAgeInSeconds(): Long = getAge() / 1000
    
    /**
     * Получить возраст данных в минутах
     */
    fun getAgeInMinutes(): Long = getAgeInSeconds() / 60
    
    companion object {
        /**
         * Создать метаданные с текущим временем
         */
        fun create(source: ScanSource, freshness: Freshness): ScanMetadata {
            return ScanMetadata(
                timestamp = System.currentTimeMillis(),
                source = source,
                freshness = freshness
            )
        }
        
        /**
         * Создать метаданные на основе времени последнего сканирования
         */
        fun fromLastScanTime(lastScanTime: Long, isActiveScan: Boolean): ScanMetadata {
            val currentTime = System.currentTimeMillis()
            val age = currentTime - lastScanTime
            
            val freshness = when {
                age < 300_000L -> Freshness.FRESH      // < 5 минут
                age < 1_800_000L -> Freshness.STALE   // 5-30 минут
                else -> Freshness.EXPIRED              // > 30 минут
            }
            
            val source = if (age < 60_000L && isActiveScan) {
                ScanSource.ACTIVE_SCAN
            } else {
                ScanSource.SYSTEM_CACHE
            }
            
            return ScanMetadata(lastScanTime, source, freshness)
        }
    }
}

/**
 * Источник данных сканирования
 */
@Parcelize
enum class ScanSource : Parcelable {
    /**
     * Активное сканирование (запущено приложением)
     */
    ACTIVE_SCAN,
    
    /**
     * Системный кэш (данные из кэша Android)
     */
    SYSTEM_CACHE,
    
    /**
     * Неизвестный источник
     */
    UNKNOWN;
    
    /**
     * Получить описание источника
     */
    fun getDescription(): String {
        return when (this) {
            ACTIVE_SCAN -> "Активное сканирование"
            SYSTEM_CACHE -> "Системный кэш"
            UNKNOWN -> "Неизвестный источник"
        }
    }
}

/**
 * Свежесть данных сканирования
 */
@Parcelize
enum class Freshness : Parcelable {
    /**
     * Свежие данные (< 5 минут)
     */
    FRESH,
    
    /**
     * Устаревшие данные (5-30 минут)
     */
    STALE,
    
    /**
     * Истекшие данные (> 30 минут)
     */
    EXPIRED,
    
    /**
     * Неизвестная свежесть (для обратной совместимости)
     */
    UNKNOWN;
    
    /**
     * Получить описание свежести
     */
    fun getDescription(): String {
        return when (this) {
            FRESH -> "Свежие данные"
            STALE -> "Устаревшие данные"
            EXPIRED -> "Истекшие данные"
            UNKNOWN -> "Неизвестно"
        }
    }
    
    /**
     * Получить цвет для отображения
     */
    fun getColorHex(): String {
        return when (this) {
            FRESH -> "#4CAF50"      // Зеленый
            STALE -> "#FF9800"      // Оранжевый
            EXPIRED -> "#F44336"    // Красный
            UNKNOWN -> "#9E9E9E"    // Серый
        }
    }
    
    companion object {
        /**
         * Определить свежесть на основе возраста данных
         * @param ageMillis возраст данных в миллисекундах
         */
        fun fromAge(ageMillis: Long): Freshness {
            return when {
                ageMillis < 300_000L -> FRESH      // < 5 минут
                ageMillis < 1_800_000L -> STALE   // 5-30 минут
                else -> EXPIRED                    // > 30 минут
            }
        }
    }
}
