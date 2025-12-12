package com.wifiguard.core.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Монитор сетевой подключения
 * Отслеживает состояние интернет-соединения и его тип
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    /**
     * Проверяет наличие интернет-соединения
     */
    fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Определяет тип подключения
     */
    fun getConnectionType(): ConnectionType {
        val network = connectivityManager.activeNetwork ?: return ConnectionType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) 
            ?: return ConnectionType.NONE
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 
                ConnectionType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 
                ConnectionType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> 
                ConnectionType.ETHERNET
            else -> ConnectionType.UNKNOWN
        }
    }
    
    /**
     * Поток для отслеживания изменений сети
     * Эмитит true когда появляется подключение, false когда пропадает
     */
    fun observeNetworkStatus(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }
            
            override fun onLost(network: Network) {
                trySend(false)
            }
            
            override fun onUnavailable() {
                trySend(false)
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                val isOnline = capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                ) && capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
                
                trySend(isOnline)
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        // Отправляем текущий статус
        trySend(isOnline())
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
    
    /**
     * Поток для отслеживания типа подключения
     */
    fun observeConnectionType(): Flow<ConnectionType> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                trySend(getConnectionType())
            }
            
            override fun onLost(network: Network) {
                trySend(ConnectionType.NONE)
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        // Отправляем текущий тип подключения
        trySend(getConnectionType())
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
    
    /**
     * Проверяет подключение к Wi-Fi
     */
    fun isWifiConnected(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            
            // #region agent log
            try {
                val logJson = JSONObject().apply {
                    put("sessionId", "debug-session")
                    put("runId", "run1")
                    put("hypothesisId", "C")
                    put("location", "NetworkMonitor.kt:140")
                    put("message", "NetworkMonitor проверка WiFi подключения")
                    put("data", JSONObject().apply {
                        put("sdkVersion", Build.VERSION.SDK_INT)
                        put("networkIsNull", network == null)
                        put("capabilitiesIsNull", capabilities == null)
                        put("hasWifiTransport", capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false)
                        put("result", capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true)
                    })
                    put("timestamp", System.currentTimeMillis())
                }
                File("/Users/mint1024/Desktop/андроид/.cursor/debug.log").appendText("${logJson}\n")
            } catch (e: Exception) {}
            // #endregion
            
            if (network == null || capabilities == null) return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            return connectivityManager.activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI
        }
    }
    
    /**
     * Проверяет подключение к мобильной сети
     */
    fun isCellularConnected(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) 
                ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            @Suppress("DEPRECATION")
            return connectivityManager.activeNetworkInfo?.type == ConnectivityManager.TYPE_MOBILE
        }
    }
}

/**
 * Тип сетевого подключения
 */
enum class ConnectionType {
    WIFI, CELLULAR, ETHERNET, UNKNOWN, NONE
}

/**
 * Статус сетевого подключения
 */
sealed class NetworkStatus {
    object Available : NetworkStatus()
    object Lost : NetworkStatus()
    object Unavailable : NetworkStatus()
    
    data class Changed(val isOnline: Boolean, val type: ConnectionType) : NetworkStatus()
}