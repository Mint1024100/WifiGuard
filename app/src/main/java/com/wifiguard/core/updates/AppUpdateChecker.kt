package com.wifiguard.core.updates

import android.app.Activity

/**
 * Проверка и запуск сценариев обновления приложения.
 */
interface AppUpdateChecker {
    fun onResume(activity: Activity)

    /**
     * Инициализация ActivityResultLauncher для нового API обновлений.
     * Должна быть вызвана из Activity (например, в onCreate).
     */
    fun initializeLauncher(activity: Activity)
}






