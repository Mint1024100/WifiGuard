package com.wifiguard.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Типы сканирования Wi-Fi сетей
 */
@Parcelize
@Serializable
enum class ScanType : Parcelable {
    MANUAL,         // Ручное сканирование
    AUTOMATIC,      // Автоматическое сканирование
    BACKGROUND,     // Фоновое сканирование
    SCHEDULED,      // Планируемое сканирование
    TRIGGERED       // Сканирование по триггеру
}