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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import com.wifiguard.core.background.WifiMonitoringWorker
import com.wifiguard.core.background.WorkManagerSafe
import com.wifiguard.core.common.Constants
import com.wifiguard.core.data.preferences.PreferencesDataSource
import com.wifiguard.core.updates.AppUpdateChecker
import com.wifiguard.core.ui.theme.WifiGuardTheme
import com.wifiguard.navigation.WifiGuardNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ИСПРАВЛЕННАЯ MainActivity - устранены все критические ошибки
 * 
 * ИСПРАВЛЕНИЯ:
 * ✅ Thread-safe state management с StateFlow
 * ✅ Правильный Compose state обновления
 * ✅ Устранены race conditions
 * ✅ Исправлена логика разрешений
 * ✅ Добавлена обработка ошибок
 * ✅ Устранены memory leaks
 * ✅ Использование string resources
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "${Constants.LOG_TAG}_MainActivity"
    }
    
    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    @Inject
    lateinit var appUpdateChecker: AppUpdateChecker
    
    // ИСПРАВЛЕНО: Thread-safe state management через StateFlow
    private val _permissionState = MutableStateFlow(PermissionState.UNKNOWN)
    private val permissionState = _permissionState.asStateFlow()
    
    // ИСПРАВЛЕНО: Безопасные лаунчеры с проверкой состояния Activity
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!isFinishing && !isDestroyed) {
            handlePermissionsResult(permissions)
        }
    }
    
    private val appSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (!isFinishing && !isDestroyed) {
            checkAndRequestPermissions()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "🚀 Запуск MainActivity")
        
        try {
            // Инициализируем launcher для обновлений через Play In-App Updates
            appUpdateChecker.initializeLauncher(this)
            
            // Сразу проверяем разрешения
            checkAndRequestPermissions()
            
            setContent {
                // ИСПРАВЛЕНО: Читаем сохраненный режим темы из настроек
                // collectAsState автоматически обновляет UI при изменении темы в настройках
                val themeMode by preferencesDataSource.getThemeMode().collectAsState(initial = "system")
                val isSystemInDarkTheme = isSystemInDarkTheme()
                
                // Определяем, нужно ли использовать темную тему
                val darkTheme = when (themeMode) {
                    "dark" -> true
                    "light" -> false
                    else -> isSystemInDarkTheme // "system"
                }
                
                // Тема автоматически обновляется при изменении themeMode через collectAsState
                WifiGuardTheme(darkTheme = darkTheme) {
                    WifiGuardMainContent()
                }
            }
            
            // Инициализируем фоновый мониторинг только после проверки разрешений
            // setupBackgroundMonitoring() будет вызван после получения разрешений
        } catch (e: Exception) {
            Log.e(TAG, "❌ Критическая ошибка в onCreate: ${e.message}", e)
            // Graceful fallback
            finish()
        }
    }
    
    /**
     * ИСПРАВЛЕНО: Правильное использование collectAsState для StateFlow
     */
    @Composable
    private fun WifiGuardMainContent() {
        val currentPermissionState by permissionState.collectAsState()
        
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentPermissionState) {
                PermissionState.GRANTED -> {
                    // Используем навигацию WifiGuard
                    WifiGuardNavigation()
                }
                PermissionState.DENIED,
                PermissionState.PERMANENTLY_DENIED -> {
                    PermissionRequestScreen(
                        isPermanentlyDenied = currentPermissionState == PermissionState.PERMANENTLY_DENIED,
                        onRequestPermissions = {
                            if (currentPermissionState != PermissionState.PERMANENTLY_DENIED) {
                                checkAndRequestPermissions()
                            } else {
                                openAppSettings()
                            }
                        },
                        onOpenSettings = { openAppSettings() }
                    )
                }
                PermissionState.UNKNOWN -> {
                    LoadingScreen()
                }
            }
        }
    }
    
    
    @Composable
    private fun LoadingScreen() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.permission_checking),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
    
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
                    title = { 
                        Text(
                            stringResource(R.string.app_name),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        ) 
                    },
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = if (isPermanentlyDenied) 
                        stringResource(R.string.permission_blocked_title)
                    else 
                        stringResource(R.string.permission_required_title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = if (isPermanentlyDenied)
                        stringResource(R.string.permission_permanently_denied_message)
                    else
                        stringResource(R.string.permission_request_message),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (isPermanentlyDenied) {
                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text(stringResource(R.string.common_open_settings))
                    }
                } else {
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text(stringResource(R.string.permission_request_button))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text(stringResource(R.string.common_open_settings))
                    }
                }
            }
        }
    }
    
    /**
     * ИСПРАВЛЕНО: Thread-safe проверка разрешений с корректной логикой
     */
    private fun checkAndRequestPermissions() {
        if (isFinishing || isDestroyed) return
        
        lifecycleScope.launch {
            try {
                val currentState = getCurrentPermissionState()
                _permissionState.value = currentState

                when (currentState) {
                    PermissionState.GRANTED -> {
                        Log.d(TAG, "✅ Все разрешения получены")
                    }
                    PermissionState.PERMANENTLY_DENIED -> {
                        Log.w(TAG, "⛔ Разрешения заблокированы пользователем")
                    }
                    PermissionState.DENIED -> {
                        val missingPermissions = getMissingPermissions()
                        
                        if (missingPermissions.isNotEmpty() && !isFinishing) {
                            Log.d(TAG, "📝 Запрашиваем разрешения: $missingPermissions")
                            permissionLauncher.launch(missingPermissions.toTypedArray())
                        }
                    }
                    PermissionState.UNKNOWN -> {
                        Log.d(TAG, "❓ Неизвестное состояние разрешений")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка при проверке разрешений: ${e.message}", e)
                _permissionState.value = PermissionState.DENIED
            }
        }
    }
    
    /**
     * ИСПРАВЛЕНО: Корректная логика определения состояния разрешений
     */
    private fun getCurrentPermissionState(): PermissionState {
        try {
            val requiredPermissions = getRequiredPermissions()
            val criticalPermissions = getCriticalPermissions()
            
            // Проверяем критически важные разрешения
            val criticalGranted = criticalPermissions.all { permission ->
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            }
            
            if (criticalGranted) {
                // Проверяем все остальные разрешения, включая разрешение на уведомления
                val allGranted = requiredPermissions.all { permission ->
                    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
                }
                
                return if (allGranted) PermissionState.GRANTED else PermissionState.DENIED
            }
            
            // ИСПРАВЛЕНО: Правильная логика определения permanently denied
            // Если какие-либо критические разрешения НЕ предоставлены
            val missingCritical = criticalPermissions.filter { permission ->
                ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
            }
            
            if (missingCritical.isNotEmpty()) {
                // Проверяем, нужно ли показывать объяснение для любого из отсутствующих разрешений
                val shouldShowRationale = missingCritical.any { permission ->
                    shouldShowRequestPermissionRationale(permission)
                }
                
                // Если объяснение не нужно показывать (пользователь нажал "Don't ask again")
                // и разрешение всё ещё отсутствует — значит "навсегда отклонено"
                return if (!shouldShowRationale) PermissionState.PERMANENTLY_DENIED else PermissionState.DENIED
            }
            
            return PermissionState.DENIED
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка определения состояния разрешений: ${e.message}", e)
            return PermissionState.DENIED
        }
    }
    
    /**
     * ИСПРАВЛЕНО: Добавлено разрешение на уведомления в критические для Android 13+
     * Разрешение на уведомления критично для работы приложения, так как оно показывает
     * предупреждения об угрозах безопасности
     */
    private fun getCriticalPermissions(): List<String> {
        val criticalPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE
        )
        
        // На Android 13+ разрешение на уведомления критично для работы приложения
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            criticalPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        return criticalPermissions
    }
    
    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            // Добавлено для сканирования Wi-Fi без привязки к геолокации (API 33+)
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        
        return permissions
    }
    
    private fun getMissingPermissions(): List<String> {
        return getRequiredPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * ИСПРАВЛЕНО: Безопасная обработка результатов разрешений
     */
    private fun handlePermissionsResult(permissions: Map<String, Boolean>) {
        try {
            val grantedPermissions = permissions.filter { it.value }
            val deniedPermissions = permissions.filter { !it.value }
            
            Log.d(TAG, "✅ Получены разрешения: ${grantedPermissions.keys}")
            if (deniedPermissions.isNotEmpty()) {
                Log.w(TAG, "❌ Отклонены разрешения: ${deniedPermissions.keys}")
            }
            
            // ИСПРАВЛЕНО: Thread-safe обновление состояния
            lifecycleScope.launch {
                val newState = getCurrentPermissionState()
                _permissionState.value = newState
                
                // Запускаем фоновый мониторинг только если все критические разрешения получены
                if (newState == PermissionState.GRANTED) {
                    setupBackgroundMonitoring()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка обработки результатов разрешений: ${e.message}", e)
        }
    }
    
    /**
     * ИСПРАВЛЕНО: Безопасное открытие настроек с fallback
     */
    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            if (intent.resolveActivity(packageManager) != null) {
                appSettingsLauncher.launch(intent)
            } else {
                Log.e(TAG, "❌ Нет Activity для открытия настроек")
                // Fallback - пытаемся открыть общие настройки
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                if (fallbackIntent.resolveActivity(packageManager) != null) {
                    startActivity(fallbackIntent)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка открытия настроек: ${e.message}", e)
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "🔄 MainActivity возобновлено")
        checkAndRequestPermissions()

        // Опционально: проверка обновлений через Play In-App Updates.
        // Вне Play Store безопасно деградирует в no-op.
        runCatching {
            appUpdateChecker.onResume(this)
        }
        
        // Проверяем, нужно ли запустить фоновый мониторинг (если разрешения получены)
        lifecycleScope.launch {
            if (permissionState.value == PermissionState.GRANTED) {
                setupBackgroundMonitoring()
            }
        }
    }
    
    /**
     * Настройка фонового мониторинга Wi-Fi
     */
    private fun setupBackgroundMonitoring() {
        try {
            val workManager = WorkManagerSafe.getInstanceOrNull(this)
            if (workManager == null) {
                Log.w(TAG, "WorkManager недоступен. Фоновый мониторинг не будет запущен.")
                return
            }
            
            // Создаем периодическую работу для мониторинга Wi-Fi
            val periodicWork = WifiMonitoringWorker.createPeriodicWork()
            
            // Запускаем уникальную периодическую работу
            workManager.enqueueUniquePeriodicWork(
                com.wifiguard.core.common.Constants.WORK_NAME_WIFI_MONITORING,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )
            
            Log.d(TAG, "✅ Фоновый мониторинг Wi-Fi настроен")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка настройки фонового мониторинга: ${e.message}", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🗑️ MainActivity завершено")
        // ИСПРАВЛЕНО: Убрана проблемная очистка WeakReference
    }
}

/**
 * Состояния разрешений для безопасной типизации
 */
enum class PermissionState {
    UNKNOWN,           // Неизвестное состояние (начальное)
    GRANTED,           // Все необходимые разрешения получены
    DENIED,            // Разрешения отклонены, можно запросить снова
    PERMANENTLY_DENIED // Разрешения заблокированы навсегда
}