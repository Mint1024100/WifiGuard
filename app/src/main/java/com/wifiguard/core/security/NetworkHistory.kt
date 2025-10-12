package com.wifiguard.core.security

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Класс для отслеживания истории сети
 */
class NetworkHistory {
    private val signalReadings = mutableListOf<Int>()
    private val maxReadings = 50 // Максимальное количество записей
    
    fun addSignalReading(signalStrength: Int) {
        signalReadings.add(signalStrength)
        
        // Ограничиваем размер истории
        if (signalReadings.size > maxReadings) {
            signalReadings.removeAt(0)
        }
    }
    
    fun getAverageSignalStrength(): Int {
        return if (signalReadings.isEmpty()) {
            -70 // Значение по умолчанию
        } else {
            signalReadings.average().toInt()
        }
    }
    
    fun getSignalReadings(): List<Int> {
        return signalReadings.toList()
    }
    
    fun getReadingCount(): Int {
        return signalReadings.size
    }
}
