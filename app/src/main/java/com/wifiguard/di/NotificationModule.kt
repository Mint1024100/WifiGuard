package com.wifiguard.di

import com.wifiguard.core.notification.INotificationHelper
import com.wifiguard.core.notification.NotificationHelper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt модуль для биндинга NotificationHelper
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    
    /**
     * Биндит интерфейс INotificationHelper к его реализации NotificationHelper
     * 
     * @param notificationHelper Реализация NotificationHelper
     * @return Интерфейс INotificationHelper
     */
    @Binds
    @Singleton
    abstract fun bindNotificationHelper(
        notificationHelper: NotificationHelper
    ): INotificationHelper
}












