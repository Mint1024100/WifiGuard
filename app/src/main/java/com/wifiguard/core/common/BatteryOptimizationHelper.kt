package com.wifiguard.core.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Вспомогательный класс для работы с настройками оптимизации батареи
 * 
 * ВАЖНО: Использование ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS нарушает
 * политику Google Play Store и может привести к отклонению приложения.
 * Вместо этого рекомендуется открывать общие настройки батареи.
 */
@Singleton
class BatteryOptimizationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Открывает общие настройки оптимизации батареи
     * 
     * Используется вместо ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
     * чтобы соответствовать политике Google Play Store
     */
    fun openGeneralBatterySettings() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        try {
            context.startActivity(intent)
        } catch (@Suppress("UNUSED_PARAMETER") unused: Exception) {
            // Fallback - открываем настройки приложения
            openAppSettings()
        }
    }
    
    /**
     * Открывает настройки приложения
     */
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            // Используем Uri.fromParts для создания правильного Uri для настроек приложения
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    /**
     * Получает рекомендации для конкретного производителя устройства
     * 
     * @return Текст с инструкциями для отключения оптимизации батареи
     */
    fun getManufacturerSpecificInstructions(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        return when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                """
                Для Xiaomi/Redmi устройств:
                1. Настройки → Приложения → WifiGuard
                2. Раздел "Батарея"
                3. Выберите "Без ограничений"
                4. Включите "Автозапуск"
                5. В MIUI Security → Разрешения → Автозапуск → Разрешить WifiGuard
                """.trimIndent()
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                """
                Для Huawei/Honor устройств:
                1. Настройки → Приложения → WifiGuard
                2. Батарея → Запуск приложения
                3. Отключите "Управлять автоматически"
                4. Включите все три опции (Автозапуск, Вторичный запуск, Работа в фоне)
                5. Настройки → Батарея → Запуск приложений → WifiGuard
                """.trimIndent()
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                """
                Для OPPO/Realme устройств:
                1. Настройки → Батарея → Оптимизация батареи
                2. Выберите WifiGuard → Не оптимизировать
                3. Настройки → Приложения → WifiGuard → Батарея
                4. Разрешите работу в фоне
                """.trimIndent()
            }
            manufacturer.contains("samsung") -> {
                """
                Для Samsung устройств:
                1. Настройки → Приложения → WifiGuard
                2. Батарея → Не оптимизировать
                3. Отключите "Переводить приложение в спящий режим"
                4. Добавьте в "Неотслеживаемые приложения"
                """.trimIndent()
            }
            manufacturer.contains("oneplus") -> {
                """
                Для OnePlus устройств:
                1. Настройки → Батарея → Оптимизация батареи
                2. WifiGuard → Не оптимизировать
                3. Настройки → Приложения → WifiGuard
                4. Оптимизация батареи → Выключить
                """.trimIndent()
            }
            else -> {
                """
                Общие рекомендации:
                1. Откройте настройки Android
                2. Перейдите в раздел "Батарея"
                3. Найдите "Оптимизация батареи"
                4. Выберите WifiGuard
                5. Установите "Не оптимизировать"
                """.trimIndent()
            }
        }
    }
}