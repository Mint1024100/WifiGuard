package com.wifiguard.feature.analyzer.domain.model

/**
 * Результат оценки угроз для Wi-Fi сети.
 * Содержит детальную информацию о потенциальных рисках безопасности,
 * списке выявленных угроз и рекомендациях по использованию.
 * 
 * @param securityLevel Общий уровень безопасности сети
 * @param threats Список выявленных угроз и уязвимостей
 * @param recommendations Рекомендации по безопасности для пользователя
 * @param riskScore Числовая оценка риска (0-100, где 100 - максимальный риск)
 * @param analysisTimestamp Временная метка проведения анализа
 */
data class ThreatAssessment(
    val securityLevel: SecurityLevel,
    val threats: List<ThreatInfo>,
    val recommendations: List<String>,
    val riskScore: Int,
    val analysisTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Информация об отдельной угрозе безопасности.
     * 
     * @param type Тип угрозы
     * @param severity Серьезность угрозы
     * @param description Описание угрозы для пользователя
     * @param detectedValue Обнаруженное значение (например, сила сигнала или SSID)
     */
    data class ThreatInfo(
        val type: ThreatType,
        val severity: ThreatSeverity,
        val description: String,
        val detectedValue: String? = null
    ) {
        /**
         * Получает числовое значение серьёзности для калькуляций.
         */
        val severityScore: Int
            get() = when (severity) {
                ThreatSeverity.CRITICAL -> 40
                ThreatSeverity.HIGH -> 25
                ThreatSeverity.MEDIUM -> 15
                ThreatSeverity.LOW -> 5
            }
        
        /**
         * Проверяет, является ли угроза критической или высокой серьёзности.
         */
        val isHighPriority: Boolean
            get() = severity == ThreatSeverity.CRITICAL || severity == ThreatSeverity.HIGH
    }
    
    /**
     * Типы угроз безопасности Wi-Fi сетей.
     * Описывают различные виды уязвимостей и атак, которым может быть подвержена сеть.
     */
    enum class ThreatType(
        /**
         * Отображаемое название типа угрозы.
         */
        val displayName: String,
        
        /**
         * Краткое описание типа угрозы.
         */
        val shortDescription: String
    ) {
        /** Слабое или устаревшее шифрование */
        WEAK_ENCRYPTION(
            displayName = "Слабое шифрование",
            shortDescription = "Используемые методы шифрования устарели"
        ),
        
        /** Отсутствие шифрования (открытая сеть) */
        NO_ENCRYPTION(
            displayName = "Отсутствие шифрования",
            shortDescription = "Сеть не защищена паролем"
        ),
        
        /** Использование устаревших протоколов безопасности */
        OUTDATED_PROTOCOL(
            displayName = "Устаревший протокол",
            shortDescription = "Протокол безопасности устарел и уязвим"
        ),
        
        /** Подозрительное имя сети (SSID) */
        SUSPICIOUS_SSID(
            displayName = "Подозрительное имя",
            shortDescription = "Имя сети может быть поддельным"
        ),
        
        /** Слабый сигнал (возможность подмены Evil Twin) */
        WEAK_SIGNAL(
            displayName = "Слабый сигнал",
            shortDescription = "Может указывать на поддельную точку доступа"
        ),
        
        /** Дублирующая сеть (Evil Twin атака) */
        DUPLICATE_NETWORK(
            displayName = "Дублирующая сеть",
            shortDescription = "Обнаружена похожая сеть (возможно Evil Twin)"
        ),
        
        /** Неудачная конфигурация безопасности */
        POOR_CONFIGURATION(
            displayName = "Неудачная конфигурация",
            shortDescription = "Настройки безопасности не оптимальны"
        ),
        
        /** Потенциальная атака на сеть */
        POTENTIAL_ATTACK(
            displayName = "Потенциальная атака",
            shortDescription = "Обнаружены признаки активной атаки"
        )
    }
    
    /**
     * Уровни серьёзности угроз.
     * Определяют приоритет обработки и влияние на общий уровень риска.
     */
    enum class ThreatSeverity(
        /**
         * Отображаемое название уровня серьёзности.
         */
        val displayName: String,
        
        /**
         * Числовое значение серьёзности (1-4, где 4 - максимальная).
         */
        val severityLevel: Int
    ) {
        /** Низкая серьёзность - минимальное влияние на безопасность */
        LOW(
            displayName = "Низкая",
            severityLevel = 1
        ),
        
        /** Средняя серьёзность - умеренные риски */
        MEDIUM(
            displayName = "Средняя",
            severityLevel = 2
        ),
        
        /** Высокая серьёзность - существенные риски безопасности */
        HIGH(
            displayName = "Высокая",
            severityLevel = 3
        ),
        
        /** Критическая серьёзность - немедленное вмешательство */
        CRITICAL(
            displayName = "Критическая",
            severityLevel = 4
        )
    }
    
    /**
     * Получает количество угроз каждого уровня серьёзности.
     */
    val threatCountBySeverity: Map<ThreatSeverity, Int>
        get() = ThreatSeverity.values().associateWith { severity ->
            threats.count { it.severity == severity }
        }
    
    /**
     * Проверяет, есть ли критические угрозы.
     */
    val hasCriticalThreats: Boolean
        get() = threats.any { it.severity == ThreatSeverity.CRITICAL }
    
    /**
     * Получает количество угроз высокого приоритета (критические и высокие).
     */
    val highPriorityThreatsCount: Int
        get() = threats.count { it.isHighPriority }
    
    /**
     * Проверяет, рекомендуется ли подключение к сети.
     * Основывается на общем уровне безопасности и количестве критических угроз.
     */
    val isConnectionRecommended: Boolean
        get() = securityLevel == SecurityLevel.LOW && !hasCriticalThreats
    
    /**
     * Получает краткую сводку о результатах анализа.
     */
    val summary: String
        get() = "Обнаружено ${threats.size} угроз(${if (threats.size % 10 == 1 && threats.size % 100 != 11) "а" else ""}). " +
                "Уровень риска: $riskScore/100. " +
                "Статус: ${securityLevel.displayName}"
    
    companion object {
        /**
         * Создаёт пустой результат анализа для безопасных сетей.
         * Используется как значение по умолчанию.
         */
        fun createSafe(): ThreatAssessment {
            return ThreatAssessment(
                securityLevel = SecurityLevel.LOW,
                threats = emptyList(),
                recommendations = listOf(
                    "Сеть относительно безопасна",
                    "Используйте HTTPS-сайты",
                    "Следите за подозрительной активностью"
                ),
                riskScore = 10
            )
        }
        
        /**
         * Создаёт результат для критически небезопасных сетей.
         * Используется для открытых сетей и WEP.
         */
        fun createCritical(threats: List<ThreatInfo>): ThreatAssessment {
            return ThreatAssessment(
                securityLevel = SecurityLevel.HIGH,
                threats = threats,
                recommendations = listOf(
                    "Не подключайтесь к этой сети!",
                    "Используйте мобильные данные",
                    "При необходимости используйте VPN"
                ),
                riskScore = threats.sumOf { it.severityScore }.coerceIn(70, 100)
            )
        }
    }
}