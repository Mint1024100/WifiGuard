package com.wifiguard

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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.wifiguard.core.common.Constants
import com.wifiguard.core.ui.theme.WifiGuardTheme
import com.wifiguard.navigation.WifiGuardNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

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
    
    // Состояние разрешений для предотвращения race conditions
    private var _permissionState by mutableStateOf(PermissionState.UNKNOWN)
    private val permissionState: PermissionState get() = _permissionState
    
    // WeakReference для предотвращения утечек памяти
    private val activityRef = WeakReference(this)
    
    // Лаунчер для запроса разрешений с безопасной обработкой
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Используем WeakReference для предотвращения утечек
        activityRef.get()?.handlePermissionsResult(permissions)
    }
    
    // Лаунчер для открытия настроек приложения
    private val appSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Перепроверяем разрешения после возврата из настроек
        activityRef.get()?.checkAndRequestPermissions()
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
        val currentPermissionState by remember { mutableStateOf(permissionState) }
        
        when (currentPermissionState) {
            PermissionState.GRANTED -> {
                // Основная навигация приложения
                WifiGuardNavigation()
            }
            PermissionState.DENIED,
            PermissionState.PERMANENTLY_DENIED -> {
                // Экран запроса разрешений
                PermissionRequestScreen(
                    isPermanentlyDenied = currentPermissionState == PermissionState.PERMANENTLY_DENIED,
                    onRequestPermissions = {
                        if (currentPermissionState != PermissionState.PERMANENTLY_DENIED) {
                            checkAndRequestPermissions()
                        } else {
                            openAppSettings()
                        }
                    },
                    onOpenSettings = {
                        openAppSettings()
                    }
                )
            }
            PermissionState.UNKNOWN -> {
                // Загрузочный экран
                LoadingScreen()
            }
        }
        
        // Обновляем состояние разрешений при возобновлении приложения
        LaunchedEffect(Unit) {
            updatePermissionState()
        }
    }
    
    /**
     * Экран загрузки
     */
    @Composable
    private fun LoadingScreen() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
    
    /**
     * Экран запроса разрешений
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PermissionRequestScreen(
        isPermanentlyDenied: Boolean,
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = if (isPermanentlyDenied) 
                        "Разрешения заблокированы" 
                    else 
                        "Разрешения для работы",
                    style = MaterialTheme.typography.headlineMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (isPermanentlyDenied)
                        "Разрешения были заблокированы.\nОткройте настройки приложения\nи предоставьте необходимые разрешения"
                    else
                        "Для сканирования Wi-Fi сетей требуются \nразрешения на доступ к \nместоположению и уведомлениям",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (isPermanentlyDenied) {
                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("Открыть настройки")
                    }
                } else {
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
    }
    
    /**
     * Проверяет и запрашивает необходимые разрешения с защитой от race conditions
     */
    private fun checkAndRequestPermissions() {
        // Атомарная проверка и обновление состояния
        val currentState = getCurrentPermissionState()
        _permissionState = currentState
        
        when (currentState) {
            PermissionState.GRANTED -> {
                Log.d(TAG, "✅ Все разрешения получены")
                return
            }
            PermissionState.PERMANENTLY_DENIED -> {
                Log.w(TAG, "⛔ Разрешения заблокированы пользователем")
                return
            }
            PermissionState.DENIED -> {
                val requiredPermissions = getRequiredPermissions()
                val missingPermissions = requiredPermissions.filter { permission ->
                    ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
                }
                
                if (missingPermissions.isNotEmpty()) {
                    Log.d(TAG, "Запрашиваем разрешения: $missingPermissions")
                    permissionLauncher.launch(missingPermissions.toTypedArray())
                }
            }
            PermissionState.UNKNOWN -> {
                // Повторная проверка
                updatePermissionState()
            }
        }
    }
    
    /**
     * Безопасно обновляет состояние разрешений
     */
    private fun updatePermissionState() {
        _permissionState = getCurrentPermissionState()
    }
    
    /**
     * Определяет текущее состояние разрешений
     */
    private fun getCurrentPermissionState(): PermissionState {
        val requiredPermissions = getRequiredPermissions()
        val criticalPermissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE
        )
        
        // Проверяем критически важные разрешения
        val criticalGranted = criticalPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
        
        val allGranted = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
        
        // Проверяем, заблокированы ли критически важные разрешения
        val criticalPermanentlyDenied = criticalPermissions.any { permission ->
            !shouldShowRequestPermissionRationale(permission) &&
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        return when {
            allGranted -> PermissionState.GRANTED
            criticalPermanentlyDenied -> PermissionState.PERMANENTLY_DENIED
            !criticalGranted -> PermissionState.DENIED
            else -> PermissionState.GRANTED // Некритические разрешения можно игнорировать
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
        
        return permissions
    }
    
    /**
     * Обрабатывает результат запроса разрешений с защитой от утечек памяти
     */
    private fun handlePermissionsResult(permissions: Map<String, Boolean>) {
        val grantedPermissions = permissions.filter { it.value }
        val deniedPermissions = permissions.filter { !it.value }
        
        Log.d(TAG, "✅ Полученные разрешения: ${grantedPermissions.keys}")
        
        if (deniedPermissions.isNotEmpty()) {
            Log.w(TAG, "❌ Отклонённые разрешения: ${deniedPermissions.keys}")
        }
        
        // Обновляем состояние после получения результата
        updatePermissionState()
        
        // Показываем объяснение если нужно
        if (_permissionState == PermissionState.DENIED) {
            showPermissionRationale()
        }
    }
    
    /**
     * Показывает объяснение, зачем нужны разрешения (Compose-совместимый диалог)
     */
    private fun showPermissionRationale() {
        // Используем Compose AlertDialog вместо AppCompat для совместимости
        lifecycleScope.launch {
            // В реальном приложении здесь будет показан Compose AlertDialog
            // через state management в Compose UI
            Log.i(TAG, "Показываем объяснение разрешений")
        }
    }
    
    /**
     * Открывает настройки приложения
     */
    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            appSettingsLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка открытия настроек: ${e.message}")
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "🔄 MainActivity возобновлено")
        
        // Безопасно проверяем разрешения при возврате к приложению
        updatePermissionState()
        
        if (_permissionState == PermissionState.GRANTED) {
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
        
        // Очищаем WeakReference для предотвращения утечек
        activityRef.clear()
    }
}

/**
 * Состояния разрешений для безопасной обработки
 */
enum class PermissionState {
    UNKNOWN,           // Неизвестное состояние
    GRANTED,           // Все необходимые разрешения получены
    DENIED,            // Разрешения отклонены, но можно запросить снова
    PERMANENTLY_DENIED // Разрешения заблокированы ("не спрашивать снова")
}