package com.wifiguard.core.updates

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Проверка обновлений через Google Play In-App Updates.
 *
 * ВАЖНО: вне Google Play (например, side-load) метод безопасно деградирует в no-op.
 */
@Singleton
class PlayInAppUpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context
) : AppUpdateChecker {

    companion object {
        private const val TAG = "WifiGuardUpdate"

        // Чтобы не дергать Play Core слишком часто.
        private const val MIN_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L // 6 часов
    }

    private var lastCheckAt: Long = 0L
    private var activityResultLauncher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>? = null
    private var appUpdateManager: AppUpdateManager? = null
    private var installStateListener: InstallStateUpdatedListener? = null

    /**
     * Инициализация ActivityResultLauncher для нового API обновлений.
     * Должна быть вызвана из Activity (например, в onCreate).
     */
    override fun initializeLauncher(activity: Activity) {
        if (activity !is ComponentActivity) {
            Log.w(TAG, "Activity не является ComponentActivity, используем fallback")
            return
        }

        if (activityResultLauncher == null) {
            activityResultLauncher = activity.registerForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                
                if (result.resultCode != Activity.RESULT_OK) {
                    Log.d(TAG, "Пользователь отменил обновление или произошла ошибка: resultCode=${result.resultCode}")
                } else {
                    Log.d(TAG, "Обновление успешно установлено")
                }
            }
        }
        
        // Инициализируем AppUpdateManager и устанавливаем слушатель состояния установки
        initializeInstallStateListener()
    }
    
    /**
     * Инициализация слушателя состояния установки обновлений.
     */
    private fun initializeInstallStateListener() {
        if (appUpdateManager == null) {
            appUpdateManager = AppUpdateManagerFactory.create(context)
        }
        
        // Удаляем предыдущий слушатель, если он был установлен
        installStateListener?.let { listener ->
            appUpdateManager?.unregisterListener(listener)
        }
        
        // Создаем новый слушатель
        installStateListener = InstallStateUpdatedListener { state: InstallState ->
            
            when (state.installStatus()) {
                InstallStatus.DOWNLOADING -> {
                    val progress = if (state.totalBytesToDownload() > 0) {
                        (state.bytesDownloaded() * 100 / state.totalBytesToDownload()).toInt()
                    } else {
                        0
                    }
                    Log.d(TAG, "Скачивание обновления: $progress% (${state.bytesDownloaded()}/${state.totalBytesToDownload()} байт)")
                }
                InstallStatus.DOWNLOADED -> {
                    Log.d(TAG, "Обновление скачано, готово к установке")
                }
                InstallStatus.INSTALLING -> {
                    Log.d(TAG, "Установка обновления...")
                }
                InstallStatus.INSTALLED -> {
                    Log.d(TAG, "Обновление успешно установлено")
                    // Удаляем слушатель после успешной установки
                    installStateListener?.let { listener ->
                        appUpdateManager?.unregisterListener(listener)
                    }
                }
                InstallStatus.FAILED -> {
                    val errorCode = state.installErrorCode()
                    val errorMessage = when (errorCode) {
                        com.google.android.play.core.install.model.InstallErrorCode.ERROR_DOWNLOAD_NOT_PRESENT -> "Файл обновления не найден"
                        com.google.android.play.core.install.model.InstallErrorCode.ERROR_INSTALL_NOT_ALLOWED -> "Установка не разрешена"
                        com.google.android.play.core.install.model.InstallErrorCode.ERROR_INSTALL_UNAVAILABLE -> "Установка недоступна"
                        com.google.android.play.core.install.model.InstallErrorCode.ERROR_INTERNAL_ERROR -> "Внутренняя ошибка"
                        com.google.android.play.core.install.model.InstallErrorCode.ERROR_INVALID_REQUEST -> "Неверный запрос"
                        com.google.android.play.core.install.model.InstallErrorCode.ERROR_PLAY_STORE_NOT_FOUND -> "Google Play не найден"
                        else -> "Неизвестная ошибка (код: $errorCode)"
                    }
                    
                    
                    Log.e(TAG, "Ошибка установки обновления: $errorMessage (код: $errorCode)")
                }
                InstallStatus.CANCELED -> {
                    Log.d(TAG, "Установка обновления отменена")
                }
                InstallStatus.PENDING -> {
                    Log.d(TAG, "Установка обновления ожидает")
                }
                else -> {
                    Log.d(TAG, "Неизвестное состояние установки: ${state.installStatus()}")
                }
            }
        }
        
        // Регистрируем слушатель
        installStateListener?.let { listener ->
            appUpdateManager?.registerListener(listener)
        }
    }

    override fun onResume(activity: Activity) {
        // Инициализируем launcher при первом вызове, если это ComponentActivity
        if (activityResultLauncher == null && activity is ComponentActivity) {
            initializeLauncher(activity)
        }

        val now = System.currentTimeMillis()
        if (now - lastCheckAt < MIN_CHECK_INTERVAL_MS) return
        lastCheckAt = now

        runCatching {
            if (appUpdateManager == null) {
                appUpdateManager = AppUpdateManagerFactory.create(context)
            }
            
            
            val updateInfoTask = appUpdateManager?.appUpdateInfo
            if (updateInfoTask == null) {
                Log.w(TAG, "AppUpdateManager или appUpdateInfo недоступен")
                return@runCatching
            }
            
            updateInfoTask
                .addOnSuccessListener { info: AppUpdateInfo ->
                    
                    // Если обновление уже было запущено и прервано, пытаемся продолжить.
                    if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        Log.d(TAG, "Обновление уже в процессе, продолжаем...")
                        tryStartUpdate(activity, info, AppUpdateType.IMMEDIATE)
                        return@addOnSuccessListener
                    }
                    
                    // Если обновление уже скачано, запускаем установку
                    if (info.installStatus() == InstallStatus.DOWNLOADED) {
                        Log.d(TAG, "Обновление скачано, запускаем установку...")
                        appUpdateManager?.completeUpdate()
                        return@addOnSuccessListener
                    }

                    val updateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    if (!updateAvailable) {
                        Log.d(TAG, "Обновления недоступны")
                        return@addOnSuccessListener
                    }

                    // Выбираем тип обновления. Для безопасности UX используем FLEXIBLE,
                    // а IMMEDIATE оставляем для особых случаев (например, критическая уязвимость).
                    val type = when {
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
                        info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                        else -> null
                    } ?: run {
                        Log.d(TAG, "Тип обновления не разрешен")
                        return@addOnSuccessListener
                    }

                    tryStartUpdate(activity, info, type)
                }
                .addOnFailureListener { e: Throwable ->
                    
                    Log.d(TAG, "Проверка обновлений недоступна: ${e.message}", e)
                }
        }.onFailure { e: Throwable ->
            // Вне Play Store/при проблемах Play Services просто игнорируем.
            Log.d(TAG, "In-App Updates недоступны: ${e.message}", e)
        }
    }

    private fun tryStartUpdate(
        activity: Activity,
        info: AppUpdateInfo,
        updateType: Int
    ) {
        val launcher = activityResultLauncher
        if (launcher == null) {
            Log.w(TAG, "ActivityResultLauncher не инициализирован, пропускаем обновление")
            return
        }

        runCatching {
            if (appUpdateManager == null) {
                appUpdateManager = AppUpdateManagerFactory.create(context)
            }

            // Используем новый API с AppUpdateOptions
            val options = AppUpdateOptions.newBuilder(updateType).build()
            

            val result = appUpdateManager?.startUpdateFlowForResult(
                info,
                launcher,
                options
            )
            
            
            if (result == null) {
                Log.w(TAG, "startUpdateFlowForResult вернул null")
            }
        }.onFailure { e: Throwable ->
            
            Log.d(TAG, "Не удалось запустить сценарий обновления: ${e.message}", e)
        }
    }
}






