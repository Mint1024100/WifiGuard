package com.wifiguard.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель угрозы безопасности Wi-Fi сети
 */
@Parcelize
data class SecurityThreat(
    val id: Long = 0,
    val type: ThreatType,
    val severity: ThreatLevel,
    val description: String,
    val networkSsid: String,
    val networkBssid: String,
    val additionalInfo: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    
    /**
     * Получить краткое описание угрозы
     */
    fun getShortDescription(): String {
        return when (type) {
            ThreatType.OPEN_NETWORK -> "Открытая сеть"
            ThreatType.WEAK_ENCRYPTION -> "Слабое шифрование"
            ThreatType.SUSPICIOUS_BEHAVIOR -> "Подозрительное поведение"
            ThreatType.FAKE_ACCESS_POINT -> "Поддельная точка доступа"
            ThreatType.MAN_IN_THE_MIDDLE -> "Атака человек-посередине"
            ThreatType.DATA_INTERCEPTION -> "Перехват данных"
            ThreatType.MALWARE_DISTRIBUTION -> "Распространение вредоносного ПО"
            ThreatType.PHISHING_ATTEMPT -> "Попытка фишинга"
            ThreatType.DNS_HIJACKING -> "Подмена DNS"
            ThreatType.ROGUE_AP -> "Несанкционированная точка доступа"
            ThreatType.WEAK_PASSWORD -> "Слабый пароль"
            ThreatType.OUTDATED_PROTOCOL -> "Устаревший протокол"
            ThreatType.UNENCRYPTED_TRAFFIC -> "Незашифрованный трафик"
            ThreatType.LOCATION_TRACKING -> "Отслеживание местоположения"
            ThreatType.DEVICE_FINGERPRINTING -> "Создание отпечатка устройства"
            ThreatType.NETWORK_SCANNING -> "Сканирование сети"
            ThreatType.BRUTE_FORCE_ATTACK -> "Атака методом перебора"
            ThreatType.SESSION_HIJACKING -> "Перехват сессии"
            ThreatType.PACKET_SNIFFING -> "Перехват пакетов"
            ThreatType.WARDRILLING -> "Wardriving"
            ThreatType.EVIL_TWIN -> "Злой двойник"
            ThreatType.KARMA_ATTACK -> "Karma атака"
            ThreatType.HONEYPOT -> "Ловушка"
            ThreatType.DEAUTH_ATTACK -> "Атака деаутентификации"
            ThreatType.WPS_ATTACK -> "Атака на WPS"
            ThreatType.KRACK_ATTACK -> "KRACK атака"
            ThreatType.DRAGONBLOOD -> "Dragonblood уязвимость"
            ThreatType.UNKNOWN_THREAT -> "Неизвестная угроза"
            ThreatType.POTENTIAL_RISK -> "Потенциальный риск"
            ThreatType.LOW_RISK -> "Низкий риск"
            ThreatType.MEDIUM_RISK -> "Средний риск"
            ThreatType.HIGH_RISK -> "Высокий риск"
            ThreatType.CRITICAL_RISK -> "Критический риск"
            ThreatType.DUPLICATE_SSID -> "Дублирующийся SSID"
            ThreatType.SUSPICIOUS_SSID -> "Подозрительное имя сети"
            ThreatType.WPS_VULNERABILITY -> "Уязвимость WPS"
            ThreatType.WEAK_SIGNAL -> "Слабый сигнал"
            ThreatType.UNKNOWN_ENCRYPTION -> "Неизвестное шифрование"
            ThreatType.MULTIPLE_DUPLICATES -> "Множественные дубликаты"
            ThreatType.SUSPICIOUS_ACTIVITY -> "Подозрительная активность"
            ThreatType.SUSPICIOUS_BSSID -> "Подозрительный MAC-адрес"
        }
    }
}