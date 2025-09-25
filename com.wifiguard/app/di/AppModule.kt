package com.wifiguard.app.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Главный модуль Hilt для предоставления зависимостей уровня приложения.
 * Содержит базовые зависимости, которые должны жить в течение всего жизненного цикла приложения.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Предоставляет контекст приложения для внедрения зависимостей.
     * 
     * @param application экземпляр Application
     * @return Context контекст приложения
     */
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Cont ext {
        TODO("Implement context provision")
    }

    /**
     * Предоставляет SharedPreferences для хранения настроек приложения.
     * 
     * @param context контекст приложения
     * @return SharedPreferences экземпляр для работы с настройками
     */
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        TODO("Implement SharedPreferences provision")
    }

    /**
     * Предоставляет экземпляр Application для компонентов, которым требуется
     * доступ к Application-уровню функциональности.
     * 
     * @param application экземпляр Application
     * @return Application для внедрения
     */
    @Provides
    @Singleton
    fun provideApplication(application: Application): Application {
        TODO("Implement Application provision")
    }
}
