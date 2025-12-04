package com.wifiguard.core.security

/**
 * Type alias for ThreatType from domain layer.
 * 
 * This allows the security package to reference ThreatType without
 * creating circular dependencies with the domain layer.
 * 
 * All threat type definitions are maintained in:
 * @see com.wifiguard.core.domain.model.ThreatType
 */
typealias ThreatType = com.wifiguard.core.domain.model.ThreatType
