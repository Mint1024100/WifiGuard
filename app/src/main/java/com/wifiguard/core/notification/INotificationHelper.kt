package com.wifiguard.core.notification

import com.wifiguard.core.domain.model.ThreatLevel

/**
 * Интерфейс для работы с уведомлениями
 * Создан для поддержки тестирования и dependency injection
 */
interface INotificationHelper {
    
    /**
     * Создать канал уведомлений
     */
    fun createNotificationChannel()
    
    /**
     * Отправить уведомление об угрозе с throttling и автоматическими настройками
     * 
     * @param networkBssid BSSID сети для идентификации
     * @param threatLevel уровень угрозы для определения приоритета
     * @param title заголовок уведомления
     * @param content текст уведомления
     * @return true если уведомление отправлено успешно
     */
    suspend fun showThreatNotification(
        networkBssid: String,
        threatLevel: ThreatLevel,
        title: String,
        content: String
    ): Boolean
    
    /**
     * УСТАРЕВШИЙ МЕТОД: Оставлен для обратной совместимости
     */
    @Deprecated(
        message = "Используйте версию с параметрами networkBssid и threatLevel",
        replaceWith = ReplaceWith("showThreatNotification(networkBssid, threatLevel, title, content)")
    )
    fun showThreatNotification(
        title: String,
        content: String,
        vibrationEnabled: Boolean = true,
        soundEnabled: Boolean = true
    ): Boolean
    
    /**
     * Отменить уведомление
     */
    fun cancelNotification()
    
    /**
     * Проверить разрешение POST_NOTIFICATIONS для Android 13+
     */
    fun checkNotificationPermission(): Boolean
    
    /**
     * Проверить, включены ли уведомления в системе
     */
    fun areNotificationsEnabled(): Boolean
    
    /**
     * Отправить тестовое уведомление для проверки работоспособности
     */
    fun testNotification(): Boolean
    
    /**
     * Получить статус уведомлений для диагностики
     */
    fun getNotificationStatus(): String
}











