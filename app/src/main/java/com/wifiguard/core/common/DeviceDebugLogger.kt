package com.wifiguard.core.common

import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * Утилита для отладочного NDJSON-логирования на устройстве.
 *
 * ВАЖНО:
 * - Пишет только технические данные (без секретов/PII).
 * - Файл создаётся в директории приложения (external files), чтобы его было легко выгрузить.
 */
object DeviceDebugLogger {

    private const val SESSION_ID = "debug-session"
    private const val FILE_NAME = "wifiguard_debug.ndjson"

    @Volatile
    private var activeRunId: String = createRunId()

    /**
     * Текущий runId для группировки логов одного запуска приложения.
     */
    fun currentRunId(): String = activeRunId

    /**
     * Генерирует новый runId (например, на старте приложения).
     */
    fun startNewRun(): String {
        activeRunId = createRunId()
        return activeRunId
    }

    fun clear(context: Context) {
        runCatching { 
            val logFile = file(context)
            val deleted = logFile.delete()
            // #region agent log
            Log.d("DeviceDebugLogger", "Очистка лог-файла: ${logFile.absolutePath}, удален=$deleted")
            // #endregion
        }.onFailure { e ->
            // #region agent log
            Log.e("DeviceDebugLogger", "Ошибка очистки лог-файла: ${e.message}", e)
            // #endregion
        }
    }

    fun log(
        context: Context,
        runId: String,
        hypothesisId: String,
        location: String,
        message: String,
        data: JSONObject = JSONObject(),
    ) {
        runCatching {
            val payload = JSONObject().apply {
                put("sessionId", SESSION_ID)
                put("runId", runId)
                put("hypothesisId", hypothesisId)
                put("location", location)
                put("message", message)
                put("timestamp", System.currentTimeMillis())
                put("data", data)
            }
            val logFile = file(context)
            // ИСПРАВЛЕНО: Создаем директорию, если она не существует
            logFile.parentFile?.mkdirs()
            // ИСПРАВЛЕНО: Используем appendText с обработкой ошибок
            logFile.appendText(payload.toString() + "\n")
        }.onFailure { e ->
            // #region agent log
            Log.e("DeviceDebugLogger", "Ошибка записи в лог-файл: ${e.message}", e)
            // #endregion
        }
    }

    fun logAppStart(context: Context, runId: String) {
        log(
            context = context,
            runId = runId,
            hypothesisId = "ENV",
            location = "DeviceDebugLogger.kt:logAppStart",
            message = "Старт приложения (устройство/SDK)",
            data = JSONObject().apply {
                put("sdkInt", Build.VERSION.SDK_INT)
                put("release", Build.VERSION.RELEASE ?: "unknown")
                put("manufacturer", Build.MANUFACTURER ?: "unknown")
                put("brand", Build.BRAND ?: "unknown")
                put("model", Build.MODEL ?: "unknown")
            }
        )
    }

    fun isLocationEnabled(context: Context): Boolean {
        return runCatching {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            // Android 9+ (P) предоставляет более надёжный флаг, чем проверка отдельных провайдеров.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lm.isLocationEnabled
            } else {
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }
        }.getOrDefault(false)
    }

    fun filePathForUser(context: Context): String {
        return file(context).absolutePath
    }

    fun getFile(context: Context): File {
        return file(context)
    }

    private fun file(context: Context): File {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val logFile = File(dir, FILE_NAME)
        // #region agent log
        Log.d("DeviceDebugLogger", "Путь к лог-файлу: ${logFile.absolutePath}, exists=${logFile.exists()}, canRead=${logFile.canRead()}, dir=${dir.absolutePath}")
        // #endregion
        return logFile
    }

    private fun createRunId(): String {
        // Не используем UUID, чтобы избежать лишних зависимостей/аллоков в критичном пути старта.
        // Достаточно уникальности по времени.
        return "run_" + System.currentTimeMillis().toString(36)
    }
}

