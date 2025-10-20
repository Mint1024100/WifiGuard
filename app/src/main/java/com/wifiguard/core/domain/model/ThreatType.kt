package com.wifiguard.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Типы угроз безопасности Wi-Fi сетей
 */
@Parcelize
enum class ThreatType : Parcelable {
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
    CRITICAL_RISK,          // Критический риск
    
    // Угрозы из SecurityReport
    DUPLICATE_SSID,         // Дублирующийся SSID
    SUSPICIOUS_SSID,        // Подозрительное имя сети
    WPS_VULNERABILITY,      // Уязвимость WPS
    WEAK_SIGNAL,            // Слабый сигнал
    UNKNOWN_ENCRYPTION,     // Неизвестное шифрование
    MULTIPLE_DUPLICATES,    // Множественные дубликаты
    SUSPICIOUS_ACTIVITY,    // Подозрительная активность
    SUSPICIOUS_BSSID        // Подозрительный MAC-адрес
}