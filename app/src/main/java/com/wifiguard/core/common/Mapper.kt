package com.wifiguard.core.common

import com.wifiguard.core.data.local.entity.WifiNetworkEntity
import com.wifiguard.core.data.local.entity.WifiScanEntity
import com.wifiguard.core.domain.model.WifiNetwork
import com.wifiguard.core.domain.model.WifiScanResult

interface Mapper<in From, out To> {
    fun map(from: From): To
}

interface ListMapper<in From, out To> : Mapper<List<From>, List<To>> {
    fun mapItem(from: From): To
    
    override fun map(from: List<From>): List<To> {
        return from.map { mapItem(it) }
    }
}

abstract class BaseMapper<in From, out To> : Mapper<From, To>
abstract class BaseListMapper<in From, out To> : ListMapper<From, To>

/**
 * Маппер для преобразования WifiNetworkEntity в WifiNetwork
 */
class WifiNetworkEntityToDomainMapper : BaseMapper<WifiNetworkEntity, WifiNetwork>() {
    override fun map(from: WifiNetworkEntity): WifiNetwork {
        return WifiNetwork(
            ssid = from.ssid ?: "Unknown",
            bssid = from.bssid,
            securityType = from.securityType,
            signalStrength = from.signalStrength,
            frequency = from.frequency,
            channel = from.channel,
            vendor = from.vendor,
            firstSeen = from.firstSeen,
            lastSeen = from.lastSeen,
            lastUpdated = from.lastSeen, // Используем lastSeen как lastUpdated
            isSuspicious = from.isSuspicious,
            suspiciousReason = from.notes,
            connectionCount = from.detectionCount,
            isKnown = false, // По умолчанию сеть не известна
            trustLevel = when {
                from.isSuspicious -> com.wifiguard.core.domain.model.TrustLevel.SUSPICIOUS
                from.detectionCount > 5 -> com.wifiguard.core.domain.model.TrustLevel.KNOWN
                else -> com.wifiguard.core.domain.model.TrustLevel.UNKNOWN
            }
        )
    }
}

/**
 * Маппер для преобразования WifiNetwork в WifiNetworkEntity
 */
class WifiNetworkDomainToEntityMapper : BaseMapper<WifiNetwork, WifiNetworkEntity>() {
    override fun map(from: WifiNetwork): WifiNetworkEntity {
        return WifiNetworkEntity(
            bssid = from.bssid,
            ssid = from.ssid,
            frequency = from.frequency,
            signalStrength = from.signalStrength,
            securityType = from.securityType,
            channel = from.channel,
            threatLevel = when (from.trustLevel) {
                com.wifiguard.core.domain.model.TrustLevel.SUSPICIOUS -> com.wifiguard.core.domain.model.ThreatLevel.HIGH
                com.wifiguard.core.domain.model.TrustLevel.UNKNOWN -> com.wifiguard.core.domain.model.ThreatLevel.MEDIUM
                else -> com.wifiguard.core.domain.model.ThreatLevel.LOW
            },
            isHidden = false, // По умолчанию сеть не скрыта
            firstSeen = from.firstSeen,
            lastSeen = from.lastSeen,
            detectionCount = from.connectionCount,
            isSuspicious = from.isSuspicious,
            vendor = from.vendor,
            notes = from.suspiciousReason
        )
    }
}

/**
 * Маппер для преобразования WifiScanEntity в WifiScanResult
 */
class WifiScanEntityToDomainMapper : BaseMapper<WifiScanEntity, WifiScanResult>() {
    override fun map(from: WifiScanEntity): WifiScanResult {
        return WifiScanResult(
            ssid = from.ssid,
            bssid = from.bssid,
            capabilities = from.capabilities,
            frequency = from.frequency,
            level = from.level,
            timestamp = from.timestamp,
            securityType = from.securityType,
            threatLevel = from.threatLevel,
            isConnected = from.isConnected,
            isHidden = from.isHidden,
            vendor = from.vendor,
            channel = from.channel,
            standard = from.standard,
            scanType = from.scanType
        )
    }
}

/**
 * Маппер для преобразования WifiScanResult в WifiScanEntity
 */
class WifiScanDomainToEntityMapper : BaseMapper<WifiScanResult, WifiScanEntity>() {
    override fun map(from: WifiScanResult): WifiScanEntity {
        return WifiScanEntity(
            ssid = from.ssid,
            bssid = from.bssid,
            capabilities = from.capabilities,
            frequency = from.frequency,
            level = from.level,
            timestamp = from.timestamp,
            securityType = from.securityType,
            threatLevel = from.threatLevel,
            isConnected = from.isConnected,
            isHidden = from.isHidden,
            vendor = from.vendor,
            channel = from.channel,
            standard = from.standard,
            scanType = from.scanType,
            securityScore = 0, // По умолчанию
            scanSessionId = null // По умолчанию
        )
    }
}