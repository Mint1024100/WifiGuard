package com.wifiguard.feature.scanner.presentation

import com.wifiguard.core.common.Resource
import com.wifiguard.feature.scanner.domain.model.WifiInfo

/**
 * UI состояние для экрана сканирования WiFi сетей.
 * Содержит информацию о состоянии загрузки, списке сетей и статусе разрешений.
 */
data class ScannerUiState(
    /**
     * Состояние операции сканирования сетей.
     * Оборачивает список сетей в Resource для обработки состояний загрузки/ошибки.
     */
    val networksResource: Resource<List<WifiInfo>> = Resource.Loading(),
    
    /**
     * Флаг активного сканирования.
     * True - сканирование выполняется, false - завершено/остановлено.
     */
    val isScanning: Boolean = false,
    
    /**
     * Статус разрешений для сканирования WiFi.
     * True - разрешения предоставлены, false - требуется запрос разрешений.
     */
    val hasLocationPermission: Boolean = false,
    
    /**
     * Сообщение об ошибке для отображения пользователю.
     * Null если ошибок нет.
     */
    val errorMessage: String? = null
) {
    /**
     * Вспомогательное свойство для получения списка сетей из Resource.
     * Возвращает пустой список если данные недоступны.
     */
    val networks: List<WifiInfo>
        get() = (networksResource as? Resource.Success)?.data ?: emptyList()
    
    /**
     * Проверяет, есть ли ошибка в состоянии ресурса.
     */
    val hasError: Boolean
        get() = networksResource is Resource.Error || errorMessage != null
    
    /**
     * Проверяет, находится ли система в состоянии загрузки.
     */
    val isLoading: Boolean
        get() = networksResource is Resource.Loading || isScanning
}