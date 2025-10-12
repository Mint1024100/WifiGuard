package com.wifiguard.core.security

/**
 * Типы угроз безопасности Wi-Fi сетей
 */
enum class ThreatType {
    // Основные типы угроз
    OPEN_NETWORK,           // Открытая сеть без шифрования
    WEAK_ENCRYPTION,        // Слабое шифрование (WEP)
    SUSPICIOUS_BEHAVIOR,    // Подозрительное поведение
    FAKE_ACCESS_POINT,      // Поддельная точка доступа
    MAN_IN_THE_MIDDLE,      // Атака "человек посередине"
    DATA_INTERCEPTION,      // Перехват данных
    MALWARE_DISTRIBUTION,   // Распространение вредоносного ПО
    PHISHING_ATTEMPT,       // Попытка фишинга
    DNS_HIJACKING,          // Подмена DNS
    ROGUE_AP,               // Несанкционированная точка доступа
    
    // Дополнительные угрозы
    WEAK_PASSWORD,          // Слабый пароль
    OUTDATED_PROTOCOL,      // Устаревший протокол
    UNENCRYPTED_TRAFFIC,    // Незашифрованный трафик
    LOCATION_TRACKING,      // Отслеживание местоположения
    DEVICE_FINGERPRINTING,  // Создание отпечатка устройства
    NETWORK_SCANNING,       // Сканирование сети
    BRUTE_FORCE_ATTACK,     // Атака перебора
    SESSION_HIJACKING,      // Перехват сессии
    PACKET_SNIFFING,        // Перехват пакетов
    WARDRILLING,            // Wardriving (поиск уязвимых сетей)
    
    // Специфичные угрозы Wi-Fi
    EVIL_TWIN,              // Злой двойник (Evil Twin)
    KARMA_ATTACK,           // Karma атака
    HONEYPOT,               // Ловушка (Honeypot)
    DEAUTH_ATTACK,          // Атака деаутентификации
    WPS_ATTACK,             // Атака на WPS
    KRACK_ATTACK,           // KRACK атака
    DRAGONBLOOD,            // Dragonblood уязвимость
    
    // Неопределенные угрозы
    UNKNOWN_THREAT,         // Неизвестная угроза
    POTENTIAL_RISK,         // Потенциальный риск
    LOW_RISK,               // Низкий риск
    MEDIUM_RISK,            // Средний риск
    HIGH_RISK,              // Высокий риск
    CRITICAL_RISK           // Критический риск
}

/**
 * Получить описание угрозы
 */
fun ThreatType.getDescription(): String {
    return when (this) {
        ThreatType.OPEN_NETWORK -> "Открытая сеть без шифрования - все данные передаются в открытом виде"
        ThreatType.WEAK_ENCRYPTION -> "Слабое шифрование WEP - легко взламывается"
        ThreatType.SUSPICIOUS_BEHAVIOR -> "Подозрительное поведение сети"
        ThreatType.FAKE_ACCESS_POINT -> "Поддельная точка доступа"
        ThreatType.MAN_IN_THE_MIDDLE -> "Атака 'человек посередине'"
        ThreatType.DATA_INTERCEPTION -> "Перехват передаваемых данных"
        ThreatType.MALWARE_DISTRIBUTION -> "Распространение вредоносного ПО"
        ThreatType.PHISHING_ATTEMPT -> "Попытка фишинга"
        ThreatType.DNS_HIJACKING -> "Подмена DNS серверов"
        ThreatType.ROGUE_AP -> "Несанкционированная точка доступа"
        ThreatType.WEAK_PASSWORD -> "Слабый пароль сети"
        ThreatType.OUTDATED_PROTOCOL -> "Устаревший протокол безопасности"
        ThreatType.UNENCRYPTED_TRAFFIC -> "Незашифрованный трафик"
        ThreatType.LOCATION_TRACKING -> "Отслеживание местоположения"
        ThreatType.DEVICE_FINGERPRINTING -> "Создание отпечатка устройства"
        ThreatType.NETWORK_SCANNING -> "Сканирование сети"
        ThreatType.BRUTE_FORCE_ATTACK -> "Атака перебора паролей"
        ThreatType.SESSION_HIJACKING -> "Перехват сессии"
        ThreatType.PACKET_SNIFFING -> "Перехват сетевых пакетов"
        ThreatType.WARDRILLING -> "Поиск уязвимых сетей"
        ThreatType.EVIL_TWIN -> "Злой двойник - поддельная сеть с тем же именем"
        ThreatType.KARMA_ATTACK -> "Karma атака - подключение к известным сетям"
        ThreatType.HONEYPOT -> "Ловушка для сбора данных"
        ThreatType.DEAUTH_ATTACK -> "Атака деаутентификации"
        ThreatType.WPS_ATTACK -> "Атака на WPS"
        ThreatType.KRACK_ATTACK -> "KRACK атака на WPA2"
        ThreatType.DRAGONBLOOD -> "Dragonblood уязвимость WPA3"
        ThreatType.UNKNOWN_THREAT -> "Неизвестная угроза"
        ThreatType.POTENTIAL_RISK -> "Потенциальный риск"
        ThreatType.LOW_RISK -> "Низкий риск"
        ThreatType.MEDIUM_RISK -> "Средний риск"
        ThreatType.HIGH_RISK -> "Высокий риск"
        ThreatType.CRITICAL_RISK -> "Критический риск"
    }
}

/**
 * Получить уровень серьезности угрозы
 */
fun ThreatType.getSeverityLevel(): Int {
    return when (this) {
        ThreatType.CRITICAL_RISK -> 5
        ThreatType.HIGH_RISK -> 4
        ThreatType.MAN_IN_THE_MIDDLE,
        ThreatType.EVIL_TWIN,
        ThreatType.KRACK_ATTACK,
        ThreatType.DRAGONBLOOD -> 4
        ThreatType.MEDIUM_RISK -> 3
        ThreatType.FAKE_ACCESS_POINT,
        ThreatType.ROGUE_AP,
        ThreatType.DEAUTH_ATTACK,
        ThreatType.WPS_ATTACK -> 3
        ThreatType.LOW_RISK -> 2
        ThreatType.OPEN_NETWORK,
        ThreatType.WEAK_ENCRYPTION,
        ThreatType.WEAK_PASSWORD -> 2
        ThreatType.POTENTIAL_RISK,
        ThreatType.UNKNOWN_THREAT -> 1
        else -> 1
    }
}

/**
 * Проверить, является ли угроза критической
 */
fun ThreatType.isCritical(): Boolean {
    return getSeverityLevel() >= 4
}

/**
 * Проверить, является ли угроза высокой
 */
fun ThreatType.isHigh(): Boolean {
    return getSeverityLevel() >= 3
}

/**
 * Получить рекомендации по защите
 */
fun ThreatType.getProtectionRecommendations(): List<String> {
    return when (this) {
        ThreatType.OPEN_NETWORK -> listOf(
            "Не подключайтесь к открытым сетям",
            "Используйте VPN при необходимости",
            "Не передавайте конфиденциальные данные"
        )
        ThreatType.WEAK_ENCRYPTION -> listOf(
            "Избегайте сетей с WEP шифрованием",
            "Используйте WPA2 или WPA3",
            "Обновите роутер"
        )
        ThreatType.EVIL_TWIN -> listOf(
            "Проверьте MAC-адрес точки доступа",
            "Используйте VPN",
            "Не вводите пароли в подозрительных сетях"
        )
        ThreatType.MAN_IN_THE_MIDDLE -> listOf(
            "Используйте HTTPS",
            "Проверяйте SSL сертификаты",
            "Используйте VPN"
        )
        ThreatType.KRACK_ATTACK -> listOf(
            "Обновите устройства",
            "Используйте WPA3",
            "Отключите WPS"
        )
        else -> listOf(
            "Будьте осторожны при подключении",
            "Используйте VPN",
            "Обновляйте устройства"
        )
    }
}
