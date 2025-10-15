package com.wifiguard.core.common

import android.util.Log
import com.wifiguard.BuildConfig

/**
 * Централизованная система логирования
 * Автоматически отключается в release сборках
 */
object Logger {
    
    private const val TAG = "WifiGuard"
    private val isDebug = BuildConfig.DEBUG
    
    fun d(message: String, tag: String = TAG) {
        if (isDebug) {
            Log.d(tag, message)
        }
    }
    
    fun i(message: String, tag: String = TAG) {
        if (isDebug) {
            Log.i(tag, message)
        }
    }
    
    fun w(message: String, tag: String = TAG) {
        if (isDebug) {
            Log.w(tag, message)
        }
    }
    
    fun e(message: String, throwable: Throwable? = null, tag: String = TAG) {
        if (isDebug) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        } else {
            // В production отправляйте критичные ошибки в Crashlytics/Firebase
            // if (throwable != null) {
            //     FirebaseCrashlytics.getInstance().recordException(throwable)
            // }
        }
    }
    
    fun v(message: String, tag: String = TAG) {
        if (isDebug) {
            Log.v(tag, message)
        }
    }
    
    /**
     * Логирование производительности
     */
    inline fun <T> measureTime(operation: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val endTime = System.currentTimeMillis()
        d("[$operation] took ${endTime - startTime}ms")
        return result
    }
    
    /**
     * Логирование ошибок безопасности
     */
    fun security(message: String, throwable: Throwable? = null) {
        // Всегда логируем ошибки безопасности
        if (throwable != null) {
            Log.wtf("SECURITY", message, throwable)
        } else {
            Log.wtf("SECURITY", message)
        }
        
        // В production отправляем в специальную систему мониторинга
        if (!isDebug) {
            // FirebaseCrashlytics.getInstance().recordException(SecurityException(message))
        }
    }
}

// Extension функции для удобного логирования
fun Any.logd(message: String) {
    Logger.d(message, this::class.java.simpleName)
}

fun Any.logi(message: String) {
    Logger.i(message, this::class.java.simpleName)
}

fun Any.logw(message: String) {
    Logger.w(message, this::class.java.simpleName)
}

fun Any.loge(message: String, throwable: Throwable? = null) {
    Logger.e(message, throwable, this::class.java.simpleName)
}

fun Any.logv(message: String) {
    Logger.v(message, this::class.java.simpleName)
}

fun Any.logSecurity(message: String, throwable: Throwable? = null) {
    Logger.security(message, throwable)
}