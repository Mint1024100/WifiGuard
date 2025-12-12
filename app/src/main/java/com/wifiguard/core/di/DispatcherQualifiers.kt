package com.wifiguard.core.di

import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

@Retention(BINARY)
@Qualifier
annotation class DefaultDispatcher

@Retention(BINARY)
@Qualifier
annotation class IoDispatcher

@Retention(BINARY)
@Qualifier  
annotation class MainDispatcher

@Retention(BINARY)
@Qualifier
annotation class MainImmediateDispatcher