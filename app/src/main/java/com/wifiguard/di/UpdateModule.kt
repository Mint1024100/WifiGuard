package com.wifiguard.di

import com.wifiguard.core.updates.AppUpdateChecker
import com.wifiguard.core.updates.PlayInAppUpdateChecker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI-модуль обновлений.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UpdateModule {

    @Binds
    @Singleton
    abstract fun bindAppUpdateChecker(
        impl: PlayInAppUpdateChecker
    ): AppUpdateChecker
}






