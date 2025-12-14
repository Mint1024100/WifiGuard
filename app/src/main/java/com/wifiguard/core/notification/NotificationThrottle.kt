package com.wifiguard.core.notification

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe throttling для предотвращения спама уведомлений.
 *
 * Механика:
 * - `shouldShow(key)` отвечает, можно ли показывать уведомление сейчас.
 * - `markShown(key)` фиксирует факт показа и очищает старые записи.
 */
internal class NotificationThrottle(
    private val intervalMs: Long,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val cleanupWindowMs: Long = 60 * 60 * 1000L // 1 час
) {
    private val cache = ConcurrentHashMap<String, Long>()

    fun shouldShow(key: String): Boolean {
        val lastTime = cache[key] ?: return true
        return (nowMs() - lastTime) >= intervalMs
    }

    fun markShown(key: String) {
        val now = nowMs()
        cache[key] = now

        // Очищаем старые записи для ограничения роста памяти
        val cutoff = now - cleanupWindowMs
        cache.entries.removeIf { it.value < cutoff }
    }
}





