package com.wifiguard.core.domain.repository

import com.wifiguard.core.domain.model.SecurityThreat
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для управления угрозами безопасности
 */
interface ThreatRepository {
    
    /**
     * Получить все угрозы
     */
    fun getAllThreats(): Flow<List<SecurityThreat>>
    
    /**
     * Получить угрозы по ID сканирования
     */
    fun getThreatsByScanId(scanId: Long): Flow<List<SecurityThreat>>
    
    /**
     * Получить угрозы по типу
     */
    fun getThreatsByType(type: com.wifiguard.core.security.ThreatType): Flow<List<SecurityThreat>>
    
    /**
     * Получить угрозы по уровню серьезности
     */
    fun getThreatsBySeverity(severity: com.wifiguard.core.domain.model.ThreatLevel): Flow<List<SecurityThreat>>
    
    /**
     * Получить угрозы по SSID сети
     */
    fun getThreatsByNetworkSsid(ssid: String): Flow<List<SecurityThreat>>
    
    /**
     * Получить неразрешенные угрозы
     */
    fun getUnresolvedThreats(): Flow<List<SecurityThreat>>
    
    /**
     * Получить угрозы за определенный период
     */
    fun getThreatsFromTimestamp(fromTimestamp: Long): Flow<List<SecurityThreat>>
    
    /**
     * Получить критические угрозы, которые не были уведомлены
     */
    suspend fun getCriticalUnnotifiedThreats(): List<SecurityThreat>
    
    /**
     * Сохранить угрозу
     */
    suspend fun insertThreat(threat: SecurityThreat): Long
    
    /**
     * Сохранить список угроз
     */
    suspend fun insertThreats(threats: List<SecurityThreat>): List<Long>
    
    /**
     * Обновить угрозу
     */
    suspend fun updateThreat(threat: SecurityThreat)
    
    /**
     * Удалить угрозу
     */
    suspend fun deleteThreat(threat: SecurityThreat)
    
    /**
     * Отметить угрозу как уведомленную
     */
    suspend fun markThreatAsNotified(threatId: Long)
    
    /**
     * Отметить угрозы как уведомленные для сканирования
     */
    suspend fun markThreatsAsNotifiedForScan(scanId: Long)
    
    /**
     * Разрешить угрозу
     */
    suspend fun resolveThreat(threatId: Long, resolutionNote: String? = null)
    
    /**
     * Отменить разрешение угрозы
     */
    suspend fun unresolveThreat(threatId: Long)
}