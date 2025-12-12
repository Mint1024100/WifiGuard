package com.wifiguard.test

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.test.runner.AndroidJUnitRunner

/**
 * Кастомный тестовый раннер для инструментальных тестов с Hilt
 *
 * Используется для кастомизации тестового окружения.
 * При использовании @CustomTestApplication, Hilt автоматически
 * генерирует подходящее тестовое приложение (TestApplication_Application).
 *
 * AndroidManifest.xml для тестов должен указывать на сгенерированный класс,
 * чтобы Android использовал правильное тестовое приложение вместо WifiGuardApp.
 *
 * Использование:
 * В build.gradle.kts установить:
 * testInstrumentationRunner = "com.wifiguard.test.CustomTestRunner"
 */
class CustomTestRunner : AndroidJUnitRunner() {

    companion object {
        private const val TAG = "CustomTestRunner"
        private const val TEST_APPLICATION_CLASS = "dagger.hilt.android.testing.HiltTestApplication"
    }

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        Log.d(TAG, "CustomTestRunner.newApplication called")
        Log.d(TAG, "className from manifest: $className")
        Log.d(TAG, "Using test application: $TEST_APPLICATION_CLASS")
        
        try {
            // ВАЖНО: Всегда используем HiltTestApplication
            // независимо от className из манифеста. Это стандартный подход для Hilt тестов.
            // CustomTestRunner переопределяет приложение, указанное в манифесте,
            // чтобы гарантировать использование правильного Hilt тестового приложения.
            val app = super.newApplication(
                cl,
                TEST_APPLICATION_CLASS,
                context
            )
            Log.d(TAG, "Successfully created application: ${app.javaClass.name}")
            return app
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "ClassNotFoundException: ${e.message}", e)
            Log.e(TAG, "Falling back to className from manifest: $className")
            // Если класс не найден, попробуем использовать className из манифеста
            return super.newApplication(cl, className, context)
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating application: ${e.message}", e)
            throw e
        }
    }
}



