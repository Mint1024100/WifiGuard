package com.wifiguard.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.wifiguard.core.common.Constants
import com.wifiguard.feature.scanner.data.database.WifiGuardDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideWifiGuardDatabase(@ApplicationContext context: Context): WifiGuardDatabase {
        return Room.databaseBuilder(
            context,
            WifiGuardDatabase::class.java,
            Constants.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }
    
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}