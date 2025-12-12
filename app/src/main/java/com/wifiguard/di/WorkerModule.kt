package com.wifiguard.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Модуль для Worker'ов.
 * Workers создаются автоматически Hilt через @HiltWorker аннотацию.
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {
    // Workers не требуют ручного предоставления через @Provides
    // Они создаются автоматически Hilt при использовании @HiltWorker
}