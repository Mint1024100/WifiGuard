package com.wifiguard.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.wifiguard.core.common.Constants
import com.wifiguard.core.ui.theme.WifiGuardTheme
import com.wifiguard.navigation.WifiGuardNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Главная и единственная Activity приложения WifiGuard
 * 
 * Использует паттерн Single Activity с Jetpack Compose для:
 * - Оптимального потребления памяти
 * - Плавных анимаций переходов
 * - Упрощенной навигации
 * - Лёгкого тестирования
 * 
 * Основные обязанности:
 * - Управление разрешениями (локация, уведомления)
 * - Отображение навигационной структуры Compose
 * - Обработка состояний жизненного цикла
 * 
 * @author WifiGuard Development Team
 * @since 1.0.0
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "${Constants.LOG_TAG}_MainActivity"
    }
    
    // Лаунчер для запроса разрешений
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionsResult(permissions)
    }
    
    // Лаунчер для открытия настроек приложения
    private val appSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Перепроверяем разрешения после возврата из настроек
        checkAndRequestPermissions()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "🚀 Запуск MainActivity")
        
        // Проверяем разрешения при запуске
        checkAndRequestPermissions()
        
        setContent {
            WifiGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WifiGuardMainContent()
                }
            }
        }
    }
    
    /**
     * Главное содержимое приложения
     */
    @Composable
    private fun WifiGuardMainContent() {
        var permissionsGranted by remember { mutableStateOf(hasAllRequiredPermissions()) }
        
        if (permissionsGranted) {
            // Основная навигация приложения
            WifiGuardNavigation()
        } else {
            // Экран запроса разрешений
            PermissionRequestScreen(
                onRequestPermissions = {
                    checkAndRequestPermissions()
                },
                onOpenSettings = {
                    openAppSettings()
                }
            )
        }
        
        // Обновляем состояние разрешений при возобновлении приложения
        LaunchedEffect(Unit) {
            permissionsGranted = hasAllRequiredPermissions()
        }
    }
    
    /**
     * Экран запроса разрешений
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PermissionRequestScreen(
        onRequestPermissions: () -> Unit,
        onOpenSettings: () -> Unit
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("🛡️ WifiGuard") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { paddingValues ->
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Icon(
                    painter = androidx.compose.material.icons.Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Разрешения для работы",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Для сканирования Wi-Fi сетей требуются \nразрешения на доступ к \nместоположению и уведомлениям",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("Предоставить разрешения")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Text("Открыть настройки")
                }
            }
        }
    }
    
    /**
     * Проверяет и запрашивает необходимые разрешения
     */
    private fun checkAndRequestPermissions() {
        val requiredPermissions = getRequiredPermissions()
        val missingPermissions = requiredPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            Log.d(TAG, "Запрашиваем разрешения: $missingPermissions")
            permissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            Log.d(TAG, "✅ Все разрешения получены")
        }
    }
    
    /**
     * Возвращает список необходимых разрешений
     */
    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
        
        // Для Android 13+ добавляем разрешение на уведомления
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        // Для Android 10+ нужно FINE_LOCATION вместо COARSE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Уже добавлено ACCESS_FINE_LOCATION
        }
        
        return permissions
    }
    
    /**
     * Проверяет, есть ли все необходимые разрешения
     */
    private fun hasAllRequiredPermissions(): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Обрабатывает результат запроса разрешений
     */
    private fun handlePermissionsResult(permissions: Map<String, Boolean>) {
        val grantedPermissions = permissions.filter { it.value }
        val deniedPermissions = permissions.filter { !it.value }
        
        Log.d(TAG, "✅ Полученные разрешения: ${grantedPermissions.keys}")
        
        if (deniedPermissions.isNotEmpty()) {
            Log.w(TAG, "❌ Отклонённые разрешения: ${deniedPermissions.keys}")
            
            // Проверяем, есть ли критически необходимые разрешения
            val criticalPermissions = listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE
            )
            
            val criticalDenied = deniedPermissions.keys.any { it in criticalPermissions }
            
            if (criticalDenied) {
                showPermissionRationale()
            }
        }
        
        // Обновляем UI
        lifecycleScope.launch {
            // Триггерим recomposition
        }
    }
    
    /**
     * Показывает объяснение, зачем нужны разрешения
     */
    private fun showPermissionRationale() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🛡️ Необходимые разрешения")
            .setMessage(
                "WifiGuard нуждается в следующих разрешениях:\n\n" +
                "📍 Местоположение - для сканирования Wi-Fi сетей\n" +
                "📶 Wi-Fi - для анализа состояния сети\n" +
                "🔔 Уведомления - для предупреждений о угрозах"
            )
            .setPositiveButton("Предоставить") { _, _ ->
                checkAndRequestPermissions()
            }
            .setNegativeButton("Настройки") { _, _ ->
                openAppSettings()
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Открывает настройки приложения
     */
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        appSettingsLauncher.launch(intent)
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "🔄 MainActivity возобновлено")
        
        // Проверяем разрешения при возврате к приложению
        if (hasAllRequiredPermissions()) {
            Log.d(TAG, "✅ Разрешения подтверждены")
        }
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "⏸️ MainActivity приостановлено")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🗑️ MainActivity уничтожено")
    }
}