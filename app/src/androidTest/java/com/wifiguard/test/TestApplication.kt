package com.wifiguard.test

import dagger.hilt.android.testing.HiltTestApplication

/**
 * Тестовое приложение для инструментальных тестов с Hilt
 *
 * Используем HiltTestApplication напрямую, так как он правильно
 * инициализирует Hilt компоненты для всех Activity, включая MainActivity.
 *
 * @see com.wifiguard.test.CustomTestRunner
 */
typealias TestApplication = HiltTestApplication



