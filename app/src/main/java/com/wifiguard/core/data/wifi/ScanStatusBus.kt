package com.wifiguard.core.data.wifi

import com.wifiguard.core.domain.model.WifiScanStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Общая шина состояния сканирования.
 *
 * Зачем нужна:
 * - ForegroundService выполняет скан и пишет в БД, но UI должен видеть прогресс/ошибки.
 * - Нельзя полагаться только на уведомление: пользователь может быть в приложении и ждать обновления экрана.
 *
 * Примечание: состояние in-memory. После убийства процесса сбрасывается в [ScanStatusState.Idle].
 */
@Singleton
class ScanStatusBus @Inject constructor() {

    private val _state = MutableStateFlow<ScanStatusState>(ScanStatusState.Idle)
    val state: StateFlow<ScanStatusState> = _state.asStateFlow()

    fun update(newState: ScanStatusState) {
        _state.value = newState
    }

    fun reset() {
        _state.value = ScanStatusState.Idle
    }
}

/**
 * Состояние процесса сканирования для UI.
 *
 * Держим отдельно от [WifiScanStatus], потому что нам важны этапы (скан/обработка/сохранение),
 * а также человекочитаемые тексты.
 */
sealed interface ScanStatusState {

    data object Idle : ScanStatusState

    /**
     * Пользователь/экран инициировал запуск скана.
     */
    data class Starting(
        val startedAt: Long = System.currentTimeMillis()
    ) : ScanStatusState

    /**
     * Запрос активного сканирования отправлен в систему.
     */
    data class Scanning(
        val startedAt: Long = System.currentTimeMillis()
    ) : ScanStatusState

    /**
     * Обработка результатов (маппинг/сохранение/анализ).
     */
    data class Processing(
        val networksCount: Int? = null,
        val startedAt: Long = System.currentTimeMillis()
    ) : ScanStatusState

    /**
     * Итоговый статус от системы/сканера.
     */
    data class Result(
        val status: WifiScanStatus,
        val timestamp: Long = System.currentTimeMillis()
    ) : ScanStatusState

    /**
     * Успешное завершение полного цикла (скан -> сохранение -> анализ).
     */
    data class Completed(
        val networksCount: Int,
        val threatsCount: Int,
        val completedAt: Long = System.currentTimeMillis()
    ) : ScanStatusState
}


