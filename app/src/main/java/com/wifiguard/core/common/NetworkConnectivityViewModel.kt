package com.wifiguard.core.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel для отслеживания состояния сети
 */
@HiltViewModel
class NetworkConnectivityViewModel @Inject constructor(
    private val networkMonitor: NetworkMonitor
) : ViewModel() {
    
    private val _networkStatus = MutableStateFlow(NetworkConnectivityState())
    val networkStatus: StateFlow<NetworkConnectivityState> = _networkStatus.asStateFlow()
    
    init {
        observeNetworkChanges()
    }
    
    private fun observeNetworkChanges() {
        viewModelScope.launch {
            networkMonitor.observeNetworkStatus()
                .combine(networkMonitor.observeConnectionType()) { isOnline, connectionType ->
                    NetworkConnectivityState(
                        isOnline = isOnline,
                        connectionType = connectionType,
                        lastUpdated = System.currentTimeMillis()
                    )
                }
                .distinctUntilChanged()
                .collect { state ->
                    _networkStatus.value = state
                }
        }
    }
    
    /**
     * Проверить текущее состояние сети
     */
    fun checkNetworkStatus() {
        _networkStatus.value = NetworkConnectivityState(
            isOnline = networkMonitor.isOnline(),
            connectionType = networkMonitor.getConnectionType(),
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * Проверить подключение к Wi-Fi
     */
    fun isWifiConnected(): Boolean = networkMonitor.isWifiConnected()
    
    /**
     * Проверить подключение к мобильной сети
     */
    fun isCellularConnected(): Boolean = networkMonitor.isCellularConnected()
}

/**
 * Состояние сетевого подключения
 */
data class NetworkConnectivityState(
    val isOnline: Boolean = false,
    val connectionType: ConnectionType = ConnectionType.NONE,
    val lastUpdated: Long = 0L
) {
    /**
     * Проверяет, было ли обновление в последние 30 секунд
     */
    val isFresh: Boolean
        get() = System.currentTimeMillis() - lastUpdated < 30_000
}