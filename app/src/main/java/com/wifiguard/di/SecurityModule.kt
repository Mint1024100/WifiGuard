package com.wifiguard.di

import com.wifiguard.core.security.EncryptionAnalyzer
import com.wifiguard.core.security.SecurityAnalyzer
import com.wifiguard.core.security.ThreatDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Модуль для предоставления зависимостей безопасности
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    
    @Provides
    @Singleton
    fun provideThreatDetector(): ThreatDetector {
        return ThreatDetector()
    }
    
    @Provides
    @Singleton
    fun provideEncryptionAnalyzer(): EncryptionAnalyzer {
        return EncryptionAnalyzer()
    }
    
    @Provides
    @Singleton
    fun provideSecurityAnalyzer(
        threatDetector: ThreatDetector,
        encryptionAnalyzer: EncryptionAnalyzer
    ): SecurityAnalyzer {
        return SecurityAnalyzer(threatDetector, encryptionAnalyzer)
    }
}
