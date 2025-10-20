package com.wifiguard.core.data.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Log
import com.wifiguard.core.common.Constants

/**
 * BroadcastReceiver для получения результатов сканирования Wi-Fi
 */
class WifiScanReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "${Constants.LOG_TAG}_WifiScanReceiver"
    }
    
    var onScanResults: ((List<android.net.wifi.ScanResult>) -> Unit)? = null
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> {
                Log.d(TAG, "Получены результаты сканирования Wi-Fi")
                
                try {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val scanResults = wifiManager.scanResults
                    
                    Log.d(TAG, "Обнаружено сетей: ${scanResults.size}")
                    
                    onScanResults?.invoke(scanResults)
                } catch (e: SecurityException) {
                    // На Android 9+ требуется разрешение ACCESS_FINE_LOCATION для получения результатов
                    Log.e(TAG, "Нет разрешения для получения результатов сканирования: ${e.message}", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка при получении результатов сканирования: ${e.message}", e)
                }
            }
            else -> {
                Log.d(TAG, "Получено неожиданное действие: ${intent.action}")
            }
        }
    }
}