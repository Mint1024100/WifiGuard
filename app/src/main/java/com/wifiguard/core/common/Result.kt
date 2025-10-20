package com.wifiguard.core.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import com.wifiguard.core.domain.model.WifiScanResult

/**
 * Результат операции с обработкой ошибок
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

/**
 * Сериализуемый результат для сохранения в состоянии
 */
@Serializable
data class SerializableResultWrapper(
    val type: String,
    val data: String? = null,
    val exceptionClassName: String? = null,
    val message: String? = null
)

/**
 * Extension для преобразования Result<List<WifiScanResult>> в SerializableResult
 */
@OptIn(InternalSerializationApi::class)
fun Result<List<WifiScanResult>>.toSerializableForWifiList(): SerializableResultWrapper {
    return when (this) {
        is Result.Success -> {
            val jsonData = try {
                Json.encodeToString(this.data)
            } catch (e: Exception) {
                "\"Serialization error: ${e.message}\""
            }
            SerializableResultWrapper("Success", jsonData, null, null)
        }
        is Result.Error -> SerializableResultWrapper("Error", null, this.exception.javaClass.name, this.message)
        is Result.Loading -> SerializableResultWrapper("Loading", null, null, null)
    }
}

/**
 * Extension для преобразования SerializableResult обратно в Result для List<WifiScanResult>
 */
@OptIn(InternalSerializationApi::class)
fun SerializableResultWrapper.toResultForWifiList(): Result<List<WifiScanResult>> {
    return when (this.type) {
        "Success" -> {
            if (this.data != null) {
                try {
                    val deserializedData = Json.decodeFromString<List<WifiScanResult>>(this.data)
                    Result.Success(deserializedData)
                } catch (e: Exception) {
                    // В случае ошибки десериализации данных возвращаем ошибку
                    Result.Error(Exception("Deserialization error: ${e.message}"), "Ошибка десериализации данных")
                }
            } else {
                Result.Error(Exception("Missing data for Success result"), "Отсутствуют данные для результата Success")
            }
        }
        "Error" -> {
            val exception = try {
                if (this.exceptionClassName != null) {
                    Class.forName(this.exceptionClassName).getConstructor(String::class.java).newInstance(this.message ?: "") as Throwable
                } else {
                    Exception(this.message ?: "Неизвестная ошибка")
                }
            } catch (e: Exception) {
                Exception(this.message ?: "Неизвестная ошибка")
            }
            Result.Error(exception, this.message)
        }
        "Loading" -> Result.Loading
        else -> Result.Error(Exception("Unknown result type: ${this.type}"), "Неизвестный тип результата")
    }
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