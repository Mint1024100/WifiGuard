package com.wifiguard.core.security

import android.util.Log
import com.wifiguard.core.domain.model.WifiScanResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Валидатор входных данных для WiFi сканирования
 * 
 * КРИТИЧЕСКИЕ МЕРЫ БЕЗОПАСНОСТИ:
 * ✅ Валидация формата BSSID (MAC-адрес)
 * ✅ Валидация SSID (длина, символы, XSS)
 * ✅ Проверка диапазона уровня сигнала
 * ✅ Проверка частоты WiFi
 * ✅ Санитизация пользовательского ввода в поиске/фильтрации
 * ✅ Раннее отклонение некорректных данных
 * 
 * @author WifiGuard Security Team
 */
@Singleton
class InputValidator @Inject constructor() {
    
    companion object {
        private const val TAG = "InputValidator"
        
        // Регулярные выражения для валидации
        private val BSSID_PATTERN = Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")
        private val SSID_DANGEROUS_CHARS = Regex("[<>\"'&;\\\\]")
        private val SAFE_SEARCH_PATTERN = Regex("^[a-zA-Z0-9а-яА-ЯёЁ\\s_\\-\\.]+$")
        
        // Константы для валидации
        private const val MAX_SSID_LENGTH = 32
        private const val MIN_SIGNAL_STRENGTH = -127 // dBm
        private const val MAX_SIGNAL_STRENGTH = 0 // dBm
        private const val MIN_FREQUENCY_2_4_GHZ = 2400
        private const val MAX_FREQUENCY_2_4_GHZ = 2500
        private const val MIN_FREQUENCY_5_GHZ = 5100
        private const val MAX_FREQUENCY_5_GHZ = 5900
        private const val MIN_FREQUENCY_6_GHZ = 5925
        private const val MAX_FREQUENCY_6_GHZ = 7125
    }
    
    /**
     * Результат валидации
     */
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
    
    /**
     * Валидирует результат сканирования WiFi
     * 
     * @param scanResult Результат сканирования для проверки
     * @return ValidationResult.Valid если данные корректны, иначе Invalid с причиной
     */
    fun validateWifiScanResult(scanResult: WifiScanResult): ValidationResult {
        // Валидация BSSID (обязательное поле)
        val bssidValidation = validateBssid(scanResult.bssid)
        if (bssidValidation is ValidationResult.Invalid) {
            Log.w(TAG, "❌ Невалидный BSSID: ${bssidValidation.reason}")
            return bssidValidation
        }
        
        // Валидация SSID
        val ssidValidation = validateSsid(scanResult.ssid)
        if (ssidValidation is ValidationResult.Invalid) {
            Log.w(TAG, "❌ Невалидный SSID: ${ssidValidation.reason}")
            return ssidValidation
        }
        
        // Валидация уровня сигнала
        val signalValidation = validateSignalStrength(scanResult.level)
        if (signalValidation is ValidationResult.Invalid) {
            Log.w(TAG, "❌ Невалидный уровень сигнала: ${signalValidation.reason}")
            return signalValidation
        }
        
        // Валидация частоты
        val frequencyValidation = validateFrequency(scanResult.frequency)
        if (frequencyValidation is ValidationResult.Invalid) {
            Log.w(TAG, "❌ Невалидная частота: ${frequencyValidation.reason}")
            return frequencyValidation
        }
        
        return ValidationResult.Valid
    }
    
    /**
     * Валидирует и фильтрует список результатов сканирования
     * 
     * @param scanResults Список результатов для проверки
     * @return Отфильтрованный список только валидных результатов
     */
    fun filterValidResults(scanResults: List<WifiScanResult>): List<WifiScanResult> {
        val (valid, invalid) = scanResults.partition { 
            validateWifiScanResult(it) is ValidationResult.Valid 
        }
        
        if (invalid.isNotEmpty()) {
            Log.w(TAG, "⚠️ Отфильтровано ${invalid.size} невалидных результатов из ${scanResults.size}")
        }
        
        return valid
    }
    
    /**
     * Валидирует BSSID (MAC-адрес точки доступа)
     */
    fun validateBssid(bssid: String): ValidationResult {
        if (bssid.isBlank()) {
            return ValidationResult.Invalid("BSSID не может быть пустым")
        }
        
        if (bssid == "unknown") {
            return ValidationResult.Invalid("BSSID неизвестен")
        }
        
        if (!BSSID_PATTERN.matches(bssid)) {
            return ValidationResult.Invalid("Некорректный формат MAC-адреса: $bssid")
        }
        
        // Проверка на broadcast/multicast MAC
        val firstByte = bssid.substring(0, 2).toIntOrNull(16) ?: return ValidationResult.Invalid("Не удалось распарсить MAC")
        if (firstByte and 0x01 == 1) {
            return ValidationResult.Invalid("Multicast MAC-адрес не допускается")
        }
        
        return ValidationResult.Valid
    }
    
    /**
     * Валидирует SSID (имя сети)
     */
    fun validateSsid(ssid: String): ValidationResult {
        // Пустой SSID допустим для скрытых сетей
        if (ssid.isBlank() || ssid == "<unknown ssid>" || ssid == "Hidden Network") {
            return ValidationResult.Valid // Скрытая сеть
        }
        
        // Проверка длины
        if (ssid.length > MAX_SSID_LENGTH) {
            return ValidationResult.Invalid("SSID слишком длинный (макс $MAX_SSID_LENGTH символов)")
        }
        
        // Проверка на потенциально опасные символы (XSS prevention)
        if (SSID_DANGEROUS_CHARS.containsMatchIn(ssid)) {
            Log.w(TAG, "⚠️ SSID содержит потенциально опасные символы: $ssid")
            // Не отклоняем, но логируем - SSID может содержать разные символы
        }
        
        return ValidationResult.Valid
    }
    
    /**
     * Валидирует уровень сигнала
     */
    fun validateSignalStrength(level: Int): ValidationResult {
        if (level < MIN_SIGNAL_STRENGTH || level > MAX_SIGNAL_STRENGTH) {
            return ValidationResult.Invalid("Уровень сигнала вне допустимого диапазона: $level dBm")
        }
        return ValidationResult.Valid
    }
    
    /**
     * Валидирует частоту WiFi
     */
    fun validateFrequency(frequency: Int): ValidationResult {
        if (frequency == 0) {
            // 0 допустим как "неизвестно"
            return ValidationResult.Valid
        }
        
        val isValid2_4GHz = frequency in MIN_FREQUENCY_2_4_GHZ..MAX_FREQUENCY_2_4_GHZ
        val isValid5GHz = frequency in MIN_FREQUENCY_5_GHZ..MAX_FREQUENCY_5_GHZ
        val isValid6GHz = frequency in MIN_FREQUENCY_6_GHZ..MAX_FREQUENCY_6_GHZ
        
        if (!isValid2_4GHz && !isValid5GHz && !isValid6GHz) {
            return ValidationResult.Invalid("Частота вне допустимых диапазонов WiFi: $frequency MHz")
        }
        
        return ValidationResult.Valid
    }
    
    /**
     * Санитизирует строку поиска для безопасного использования в запросах
     * 
     * @param query Пользовательский ввод
     * @return Очищенная строка или null если ввод некорректен
     */
    fun sanitizeSearchQuery(query: String): String? {
        if (query.isBlank()) {
            return null
        }
        
        // Удаляем потенциально опасные символы
        val sanitized = query
            .trim()
            .take(100) // Ограничение длины
            .replace(SSID_DANGEROUS_CHARS, "")
        
        if (sanitized.isBlank()) {
            Log.w(TAG, "⚠️ Запрос поиска отклонён после санитизации")
            return null
        }
        
        // Проверяем на допустимые символы
        if (!SAFE_SEARCH_PATTERN.matches(sanitized)) {
            Log.w(TAG, "⚠️ Запрос содержит недопустимые символы: $query")
            // Возвращаем только буквенно-цифровые символы
            return sanitized.filter { it.isLetterOrDigit() || it.isWhitespace() || it in "_-." }
                .takeIf { it.isNotBlank() }
        }
        
        return sanitized
    }
    
    /**
     * Проверяет, является ли SSID потенциально подозрительным
     * 
     * @param ssid Имя сети
     * @return true если SSID требует внимания
     */
    fun isSuspiciousSsid(ssid: String): Boolean {
        val lowerSsid = ssid.lowercase()
        
        // Проверка на известные подозрительные паттерны
        val suspiciousPatterns = listOf(
            "free wifi", "free-wifi", "free_wifi",
            "public wifi", "open wifi",
            "guest", "hotspot",
            "no password", "no-password",
            "admin", "root", "test", "default",
            "linksys", "netgear", "dlink", "tp-link", "asus", "belkin",
            "router", "modem", "setup"
        )
        
        return suspiciousPatterns.any { pattern ->
            lowerSsid.contains(pattern) || lowerSsid == pattern
        }
    }
    
    /**
     * Валидирует канал WiFi
     */
    fun validateChannel(channel: Int): ValidationResult {
        if (channel == 0) return ValidationResult.Valid // Неизвестно
        
        // Каналы 2.4 GHz: 1-14
        // Каналы 5 GHz: 36-165 (с пропусками)
        // Каналы 6 GHz: 1-233 (Wi-Fi 6E)
        
        val valid2_4GHz = channel in 1..14
        val valid5GHz = channel in 36..165
        val valid6GHz = channel in 1..233
        
        return if (valid2_4GHz || valid5GHz || valid6GHz) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("Некорректный номер канала: $channel")
        }
    }
}





















