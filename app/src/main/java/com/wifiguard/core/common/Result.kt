package com.wifiguard.core.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Результат операции с обработкой ошибок
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

/**
 * Extension для Flow с обработкой ошибок
 */
fun <T> Flow<T>.asResult(): Flow<Result<T>> = flow {
    emit(Result.Loading)
    try {
        collect { value ->
            emit(Result.Success(value))
        }
    } catch (e: Exception) {
        emit(Result.Error(e, e.message))
    }
}

/**
 * Exception для пользовательских ошибок
 */
sealed class AppException(message: String) : Exception(message) {
    class NoPermissionException(message: String = "Нет необходимых разрешений") : AppException(message)
    class WifiDisabledException(message: String = "Wi-Fi выключен") : AppException(message)
    class NetworkException(message: String = "Ошибка сети") : AppException(message)
    class DatabaseException(message: String = "Ошибка базы данных") : AppException(message)
    class UnknownException(message: String = "Неизвестная ошибка") : AppException(message)
}

/**
 * Преобразование Exception в понятное пользовательское сообщение
 */
fun Throwable.toUserMessage(): String {
    return when (this) {
        is AppException.NoPermissionException -> this.message ?: "Нет необходимых разрешений"
        is AppException.WifiDisabledException -> "Wi-Fi выключен. Включите Wi-Fi для сканирования."
        is AppException.NetworkException -> "Ошибка сети. Проверьте подключение."
        is AppException.DatabaseException -> "Ошибка сохранения данных"
        is SecurityException -> "Нет разрешения на сканирование Wi-Fi"
        is IllegalStateException -> this.message ?: "Неверное состояние приложения"
        else -> this.message ?: "Произошла ошибка. Попробуйте еще раз."
    }
}