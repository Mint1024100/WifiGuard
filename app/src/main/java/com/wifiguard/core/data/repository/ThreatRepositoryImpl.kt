package com.wifiguard.core.data.repository

import com.wifiguard.core.common.ThreatDomainToEntityMapper
import com.wifiguard.core.common.ThreatEntityToDomainMapper
import com.wifiguard.core.data.local.dao.ThreatDao
import com.wifiguard.core.domain.model.SecurityThreat
import com.wifiguard.core.domain.repository.ThreatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация репозитория для управления угрозами безопасности
 */
@Singleton
class ThreatRepositoryImpl @Inject constructor(
    private val threatDao: ThreatDao,
    private val threatEntityToDomainMapper: ThreatEntityToDomainMapper,
    private val threatDomainToEntityMapper: ThreatDomainToEntityMapper
) : ThreatRepository {

    override fun getAllThreats(): Flow<List<SecurityThreat>> {
        return threatDao.getAllThreats().map { entities ->
            entities.map { threatEntityToDomainMapper.map(it) }
        }
    }

    override fun getThreatsByScanId(scanId: Long): Flow<List<SecurityThreat>> {
        return threatDao.getThreatsByScanId(scanId).map { entities ->
            entities.map { threatEntityToDomainMapper.map(it) }
        }
    }

    override fun getThreatsByType(type: com.wifiguard.core.domain.model.ThreatType): Flow<List<SecurityThreat>> {
        return threatDao.getThreatsByType(type).map { entities ->
            entities.map { threatEntityToDomainMapper.map(it) }
        }
    }

    override fun getThreatsBySeverity(severity: com.wifiguard.core.domain.model.ThreatLevel): Flow<List<SecurityThreat>> {
        return threatDao.getThreatsBySeverity(severity).map { entities ->
            entities.map { threatEntityToDomainMapper.map(it) }
        }
    }

    override fun getThreatsByNetworkSsid(ssid: String): Flow<List<SecurityThreat>> {
        return threatDao.getThreatsByNetworkSsid(ssid).map { entities ->
            entities.map { threatEntityToDomainMapper.map(it) }
        }
    }

    override fun getThreatsByNetworkBssid(bssid: String): Flow<List<SecurityThreat>> {
        return threatDao.getThreatsByNetworkBssid(bssid).map { entities ->
            entities.map { threatEntityToDomainMapper.map(it) }
        }
    }

    override fun getUnresolvedThreatsByNetworkBssid(bssid: String): Flow<List<SecurityThreat>> {
        return threatDao.getUnresolvedThreatsByNetworkBssid(bssid).map { entities ->
            entities.map { threatEntityToDomainMapper.map(it) }
        }
    }

    override fun getUnresolvedThreats(): Flow<List<SecurityThreat>> {
        return threatDao.getUnresolvedThreats().map { entities ->
            entities.map { threatEntityToDomainMapper.map(it) }
        }
    }

    override fun getThreatsFromTimestamp(fromTimestamp: Long): Flow<List<SecurityThreat>> {
        return threatDao.getThreatsFromTimestamp(fromTimestamp).map { entities ->
            entities.map { threatEntityToDomainMapper.map(it) }
        }
    }

    override suspend fun getCriticalUnnotifiedThreats(): List<SecurityThreat> {
        return threatDao.getCriticalUnnotifiedThreats().map { threatEntityToDomainMapper.map(it) }
    }

    override suspend fun insertThreat(threat: SecurityThreat): Long {
        return withContext(Dispatchers.IO) {
            try {
                val entity = threatDomainToEntityMapper.map(threat)
                val id = threatDao.insertThreat(entity)
                Log.d("ThreatRepository", "Угроза успешно сохранена: id=$id, type=${threat.type}")
                id
            } catch (e: Exception) {
                Log.e("ThreatRepository", "Ошибка при сохранении угрозы: ${e.message}", e)
                throw e
            }
        }
    }

    override suspend fun insertThreats(threats: List<SecurityThreat>): List<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = threats.map { threatDomainToEntityMapper.map(it) }
                val ids = threatDao.insertThreats(entities)
                Log.d("ThreatRepository", "Успешно сохранено ${threats.size} угроз")
                ids
            } catch (e: Exception) {
                Log.e("ThreatRepository", "Ошибка при сохранении угроз: ${e.message}", e)
                throw e
            }
        }
    }

    override suspend fun updateThreat(threat: SecurityThreat) {
        withContext(Dispatchers.IO) {
            try {
                val entity = threatDomainToEntityMapper.map(threat)
                threatDao.updateThreat(entity)
                Log.d("ThreatRepository", "Угроза успешно обновлена: id=${threat.id}")
            } catch (e: Exception) {
                Log.e("ThreatRepository", "Ошибка при обновлении угрозы: ${e.message}", e)
                throw e
            }
        }
    }

    override suspend fun deleteThreat(threat: SecurityThreat) {
        val entity = threatDomainToEntityMapper.map(threat)
        threatDao.deleteThreat(entity)
    }

    override suspend fun markThreatAsNotified(threatId: Long) {
        threatDao.markThreatAsNotified(threatId)
    }

    override suspend fun markThreatsAsNotifiedForScan(scanId: Long) {
        threatDao.markThreatsAsNotifiedForScan(scanId)
    }

    override suspend fun resolveThreat(threatId: Long, resolutionNote: String?) {
        threatDao.resolveThreat(threatId, System.currentTimeMillis(), resolutionNote)
    }

    override suspend fun unresolveThreat(threatId: Long) {
        threatDao.unresolveThreat(threatId)
    }
}