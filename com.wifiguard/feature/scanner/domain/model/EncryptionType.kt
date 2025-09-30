package com.wifiguard.feature.scanner.domain.model

/**
 * Перечисление типов шифрования WiFi сетей.
 * Определяет методы защиты беспроводных сетей от открытых до современных протоколов.
 */
enum class EncryptionType(
    /**
     * Отображаемое название типа шифрования.
     */
    val displayName: String,
    
    /**
     * Описание уровня безопасности.
     */
    val description: String,
    
    /**
     * Относительный уровень безопасности (1-5, где 1 - наименьшая, 5 - наивысшая).
     */
    val securityRating: Int
) {
    /**
     * Открытая сеть без какой-либо защиты.
     * Любой может подключиться без пароля. Крайне небезопасно.
     */
    OPEN(
        displayName = "Открытая",
        description = "Нет защиты",
        securityRating = 1
    ),
    
    /**
     * Wired Equivalent Privacy - устаревший протокол с множеством уязвимостей.
     * Легко взламывается современными средствами.
     */
    WEP(
        displayName = "WEP",
        description = "Устаревший протокол",
        securityRating = 2
    ),
    
    /**
     * Wi-Fi Protected Access - первая версия протокола WPA.
     * Лучше WEP, но имеет известные уязвимости.
     */
    WPA(
        displayName = "WPA",
        description = "Умеренная защита",
        securityRating = 3
    ),
    
    /**
     * Wi-Fi Protected Access 2 - улучшенная версия WPA.
     * Надежный протокол, широко используемый сегодня.
     */
    WPA2(
        displayName = "WPA2",
        description = "Надежная защита",
        securityRating = 4
    ),
    
    /**
     * Wi-Fi Protected Access 3 - новейший стандарт безопасности.
     * Обеспечивает наивысший уровень защиты.
     */
    WPA3(
        displayName = "WPA3",
        description = "Максимальная защита",
        securityRating = 5
    ),
    
    /**
     * Неизвестный или неопределимый тип шифрования.
     * Используется когда система не может определить тип защиты.
     */
    UNKNOWN(
        displayName = "Неизвестно",
        description = "Неопределённый тип",
        securityRating = 1
    );
    
    /**
     * Проверяет, является ли тип шифрования безопасным.
     * Безопасными считаются WPA2 и WPA3.
     */
    val isSecure: Boolean
        get() = securityRating >= 4
    
    /**
     * Проверяет, является ли тип шифрования устаревшим.
     * Устаревшими считаются WEP и WPA.
     */
    val isDeprecated: Boolean
        get() = this == WEP || this == WPA
    
    companion object {
        /**
         * Определяет тип шифрования по строковому представлению.
         * Используется для парсинга данных от Android WiFi API.
         */
        fun fromCapabilities(capabilities: String): EncryptionType {
            return when {
                capabilities.contains("WPA3", ignoreCase = true) -> WPA3
                capabilities.contains("WPA2", ignoreCase = true) -> WPA2
                capabilities.contains("WPA", ignoreCase = true) -> WPA
                capabilities.contains("WEP", ignoreCase = true) -> WEP
                capabilities.contains("OPEN", ignoreCase = true) || 
                capabilities.isBlank() -> OPEN
                else -> UNKNOWN
            }
        }
    }
}