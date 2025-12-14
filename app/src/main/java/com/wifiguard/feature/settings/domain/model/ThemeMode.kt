package com.wifiguard.feature.settings.domain.model

/**
 * Режим темы оформления приложения
 */
sealed class ThemeMode(val value: String) {
    object Light : ThemeMode("light")
    object Dark : ThemeMode("dark")
    object System : ThemeMode("system")
    
    companion object {
        /**
         * Получить ThemeMode по строковому значению
         * @param value строковое значение ("light", "dark", "system")
         * @return ThemeMode или System по умолчанию
         */
        fun fromString(value: String?): ThemeMode {
            return when (value) {
                "light" -> Light
                "dark" -> Dark
                "system" -> System
                else -> System // По умолчанию системная тема
            }
        }
        
        /**
         * Проверить, является ли значение валидным режимом темы
         */
        fun isValid(value: String?): Boolean {
            return value in setOf("light", "dark", "system")
        }
    }
}

