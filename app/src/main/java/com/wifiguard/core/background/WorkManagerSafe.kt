package com.wifiguard.core.background

import android.content.Context
import android.util.Log
import androidx.work.WorkManager
import com.wifiguard.core.common.Constants

/**
 * Безопасный доступ к WorkManager.
 *
 * На части кастомных прошивок/сборок (и при проблемах с инициализацией) WorkManager может бросать
 * IllegalStateException при вызове getInstance(). В таком случае фоновые задачи следует
 * пропустить, но приложение не должно падать.
 */
object WorkManagerSafe {
    private const val TAG = "${Constants.LOG_TAG}_WorkManagerSafe"

    fun getInstanceOrNull(context: Context): WorkManager? {
        return try {
            WorkManager.getInstance(context)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "WorkManager недоступен/не инициализирован: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Неожиданная ошибка получения WorkManager: ${e.message}", e)
            null
        }
    }
}








