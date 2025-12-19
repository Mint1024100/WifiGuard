# Технический отчёт по предупреждениям сборки Gradle

**Дата анализа:** 2024-12-15  
**Команда сборки:** `./gradlew assembleDebug`  
**Результат сборки:** BUILD SUCCESSFUL  
**Время сборки:** 29s

---

## Резюме

Обнаружено **4 предупреждения** уровня `w:` (warning):
- **3 предупреждения** в модуле `:benchmark` (Compose Opt-in маркеры)
- **1 предупреждение** в модуле `:app` (Deprecated API)

Все предупреждения не блокируют сборку, но требуют исправления для обеспечения совместимости с будущими версиями библиотек и соблюдения best practices.

---

## Детальный анализ предупреждений

### W-001: Unresolved Opt-in marker `androidx.compose.material3.ExperimentalMaterial3Api`

**Модуль:** `:benchmark`  
**Вариант сборки:** `debug`  
**Триггер-строка:**
```
w: Opt-in requirement marker androidx.compose.material3.ExperimentalMaterial3Api is unresolved. Please make sure it's present in the module dependencies
```

**Тип:** Compose Opt-in / Dependency  
**Серьёзность:** Medium

**Обоснование серьёзности:**
- Не блокирует сборку, но может привести к проблемам при использовании экспериментальных API Compose Material3
- В будущих версиях Kotlin Compiler может стать ошибкой компиляции
- Указывает на неполную конфигурацию зависимостей в benchmark модуле

**Влияние:**
- **На сборку:** Нет (сборка успешна)
- **На runtime:** Нет (benchmark модуль не содержит Compose UI кода)
- **На размер APK:** Нет
- **На future compatibility:** Возможно — при обновлении Kotlin Compiler или Compose может стать ошибкой

**Вероятная причина:**
Модуль `:benchmark` не имеет зависимостей на Compose библиотеки (Material3, Foundation, Animation), но транзитивные зависимости или код приложения (`:app`), который тестируется через macrobenchmark, использует экспериментальные API с opt-in маркерами. Kotlin Compiler видит эти маркеры в classpath, но не может их разрешить, так как соответствующие библиотеки отсутствуют в зависимостях benchmark модуля.

**Как проверить:**
```bash
# Проверить зависимости benchmark модуля
./gradlew :benchmark:dependencies --configuration debugCompileClasspath | grep compose

# Проверить использование opt-in маркеров в app модуле
grep -r "@OptIn\|@ExperimentalMaterial3Api\|@ExperimentalFoundationApi\|@ExperimentalAnimationApi" app/src/
```

**Рекомендации по исправлению:**

**Вариант 1 (рекомендуемый):** Добавить Compose BOM и необходимые библиотеки в `benchmark/build.gradle.kts`:

```kotlin
dependencies {
    // ... существующие зависимости ...
    
    // Добавить Compose BOM для разрешения opt-in маркеров
    implementation(platform(libs.androidx.compose.bom))
    
    // Добавить библиотеки с opt-in маркерами (compileOnly достаточно)
    compileOnly(libs.androidx.compose.material3)
    compileOnly("androidx.compose.foundation:foundation")
    compileOnly("androidx.compose.animation:animation")
}
```

**Вариант 2:** Если benchmark не использует Compose напрямую, можно добавить только compileOnly зависимости для маркеров:

```kotlin
dependencies {
    // ... существующие зависимости ...
    
    // Только для разрешения opt-in маркеров, не включаются в APK
    compileOnly(platform(libs.androidx.compose.bom))
    compileOnly("androidx.compose.material3:material3")
    compileOnly("androidx.compose.foundation:foundation")
    compileOnly("androidx.compose.animation:animation")
}
```

**Риски исправления:**
- Минимальные: добавление `compileOnly` зависимостей не увеличит размер APK
- Возможен конфликт версий, если версии Compose в benchmark и app не совпадают (решается через BOM)

---

### W-002: Unresolved Opt-in marker `androidx.compose.foundation.ExperimentalFoundationApi`

**Модуль:** `:benchmark`  
**Вариант сборки:** `debug`  
**Триггер-строка:**
```
w: Opt-in requirement marker androidx.compose.foundation.ExperimentalFoundationApi is unresolved. Please make sure it's present in the module dependencies
```

**Тип:** Compose Opt-in / Dependency  
**Серьёзность:** Medium

**Обоснование серьёзности:** Аналогично W-001

**Влияние:** Аналогично W-001

**Вероятная причина:** Аналогично W-001

**Как проверить:** Аналогично W-001

**Рекомендации по исправлению:** См. W-001 (исправление всех трёх маркеров выполняется одним изменением)

**Риски исправления:** Аналогично W-001

---

### W-003: Unresolved Opt-in marker `androidx.compose.animation.ExperimentalAnimationApi`

**Модуль:** `:benchmark`  
**Вариант сборки:** `debug`  
**Триггер-строка:**
```
w: Opt-in requirement marker androidx.compose.animation.ExperimentalAnimationApi is unresolved. Please make sure it's present in the module dependencies
```

**Тип:** Compose Opt-in / Dependency  
**Серьёзность:** Medium

**Обоснование серьёзности:** Аналогично W-001

**Влияние:** Аналогично W-001

**Вероятная причина:** Аналогично W-001

**Как проверить:** Аналогично W-001

**Рекомендации по исправлению:** См. W-001 (исправление всех трёх маркеров выполняется одним изменением)

**Риски исправления:** Аналогично W-001

---

### W-004: Deprecated method `startUpdateFlowForResult` in PlayInAppUpdateChecker

**Модуль:** `:app`  
**Вариант сборки:** `debug`  
**Триггер-строка:**
```
w: file:///Users/mint1024/Desktop/%D0%B0%D0%BD%D0%B4%D1%80%D0%BE%D0%B8%D0%B4/app/src/main/java/com/wifiguard/core/updates/PlayInAppUpdateChecker.kt:79:30 'fun startUpdateFlowForResult(p0: AppUpdateInfo, p1: Int, p2: Activity, p3: Int): Boolean' is deprecated. Deprecated in Java.
```

**Тип:** Deprecation / API Migration  
**Серьёзность:** High

**Обоснование серьёзности:**
- Deprecated API может быть удалён в будущих версиях библиотеки Google Play Core
- Текущая реализация использует устаревший подход с `Activity.startActivityForResult()`, который deprecated в Android
- Новая версия API использует современный `ActivityResultLauncher`, что соответствует best practices Android

**Влияние:**
- **На сборку:** Нет (сборка успешна)
- **На runtime:** Нет (текущая версия библиотеки поддерживает deprecated метод)
- **На размер APK:** Нет
- **На future compatibility:** Высокое — метод может быть удалён в будущих версиях, что приведёт к ошибке компиляции

**Вероятная причина:**
Использование устаревшего метода `AppUpdateManager.startUpdateFlowForResult(AppUpdateInfo, Int, Activity, Int)`, который был заменён на версию с `ActivityResultLauncher` и `AppUpdateOptions` для соответствия современным Android API.

**Как проверить:**
```bash
# Проверить версию библиотеки
./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep "app-update"

# Проверить использование deprecated метода
grep -n "startUpdateFlowForResult" app/src/main/java/com/wifiguard/core/updates/PlayInAppUpdateChecker.kt
```

**Рекомендации по исправлению:**

**Важно:** `MainActivity` уже использует `ComponentActivity` и имеет примеры использования `ActivityResultLauncher`, что упрощает миграцию.

1. **Обновить `PlayInAppUpdateChecker.kt`** для использования нового API:

```kotlin
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
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayInAppUpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context
) : AppUpdateChecker {

    companion object {
        private const val TAG = "WifiGuardUpdate"
        private const val MIN_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L // 6 часов
    }

    private var lastCheckAt: Long = 0L
    private var activityResultLauncher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>? = null

    /**
     * Инициализация ActivityResultLauncher должна быть вызвана из Activity
     * (например, в onCreate или onStart)
     */
    fun initializeLauncher(activity: Activity) {
        if (activity !is ComponentActivity) {
            Log.w(TAG, "Activity не является ComponentActivity, используем fallback")
            return
        }
        
        if (activityResultLauncher == null) {
            activityResultLauncher = activity.registerForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                if (result.resultCode != Activity.RESULT_OK) {
                    Log.d(TAG, "Пользователь отменил обновление или произошла ошибка")
                }
            }
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
            val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { info: AppUpdateInfo ->
                    if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        tryStartUpdate(activity, info, AppUpdateType.IMMEDIATE)
                        return@addOnSuccessListener
                    }

                    val updateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    if (!updateAvailable) return@addOnSuccessListener

                    val type = when {
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
                        info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                        else -> null
                    } ?: return@addOnSuccessListener

                    tryStartUpdate(activity, info, type)
                }
                .addOnFailureListener { e: Throwable ->
                    Log.d(TAG, "Проверка обновлений недоступна: ${e.message}")
                }
        }.onFailure { e: Throwable ->
            Log.d(TAG, "In-App Updates недоступны: ${e.message}")
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
            val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)
            
            // Используем новый API с AppUpdateOptions
            val options = AppUpdateOptions.newBuilder(updateType).build()
            
            appUpdateManager.startUpdateFlowForResult(
                info,
                launcher,
                options
            )
        }.onFailure { e: Throwable ->
            Log.d(TAG, "Не удалось запустить сценарий обновления: ${e.message}")
        }
    }
}
```

2. **Обновить интерфейс `AppUpdateChecker`** для добавления метода инициализации:

```kotlin
// app/src/main/java/com/wifiguard/core/updates/AppUpdateChecker.kt
package com.wifiguard.core.updates

import android.app.Activity

/**
 * Проверка и запуск сценариев обновления приложения.
 */
interface AppUpdateChecker {
    fun onResume(activity: Activity)
    
    /**
     * Инициализация ActivityResultLauncher для нового API обновлений.
     * Должна быть вызвана из Activity (например, в onCreate).
     */
    fun initializeLauncher(activity: Activity)
}
```

3. **Обновить `MainActivity.kt`** для вызова инициализации:

```kotlin
// В MainActivity.onCreate(), после setContent:
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // ... существующий код ...
    
    // Инициализируем launcher для обновлений
    appUpdateChecker.initializeLauncher(this)
    
    // ... остальной код ...
}
```

4. **Обновить `NoOpAppUpdateChecker.kt`** (если используется):

```kotlin
// Добавить пустую реализацию метода initializeLauncher
override fun initializeLauncher(activity: Activity) {
    // No-op для fallback реализации
}
```

**Риски исправления:**
- **Средние:** Требуется изменение архитектуры (добавление метода инициализации)
- Возможна необходимость обновления всех мест использования `AppUpdateChecker`
- Требуется тестирование на реальных устройствах для проверки работы нового API
- Необходимо убедиться, что `activity-result` библиотека присутствует в зависимостях (обычно включена в `androidx.activity:activity-ktx`)

**Проверка зависимостей:**
```kotlin
// В app/build.gradle.kts должна быть:
implementation(libs.androidx.activity.compose) // или
implementation("androidx.activity:activity-ktx:1.9.1")
```

---

## Таблица предупреждений

| ID | Warning | Module | Severity | Fix Summary |
|---|---|---|---|---|
| W-001 | Unresolved Opt-in marker `ExperimentalMaterial3Api` | `:benchmark` | Medium | Добавить Compose BOM и compileOnly зависимости в `benchmark/build.gradle.kts` |
| W-002 | Unresolved Opt-in marker `ExperimentalFoundationApi` | `:benchmark` | Medium | Добавить Compose BOM и compileOnly зависимости в `benchmark/build.gradle.kts` |
| W-003 | Unresolved Opt-in marker `ExperimentalAnimationApi` | `:benchmark` | Medium | Добавить Compose BOM и compileOnly зависимости в `benchmark/build.gradle.kts` |
| W-004 | Deprecated `startUpdateFlowForResult` method | `:app` | High | Мигрировать на новый API с `ActivityResultLauncher` и `AppUpdateOptions` |

---

## Action Items (чеклист)

### Приоритет 1 (High) — Критично для future compatibility

- [ ] **W-004:** Мигрировать `PlayInAppUpdateChecker.kt` на новый API `startUpdateFlowForResult` с `ActivityResultLauncher`
  - [ ] Обновить `PlayInAppUpdateChecker.kt`: заменить deprecated метод на новый API с `AppUpdateOptions`
  - [ ] Добавить поле `activityResultLauncher` и метод `initializeLauncher()` в `PlayInAppUpdateChecker`
  - [ ] Обновить метод `tryStartUpdate()` для использования `AppUpdateOptions.newBuilder(updateType).build()`
  - [ ] Обновить интерфейс `AppUpdateChecker.kt`: добавить метод `initializeLauncher(activity: Activity)`
  - [ ] Обновить `NoOpAppUpdateChecker.kt`: добавить пустую реализацию `initializeLauncher()`
  - [ ] Вызвать `appUpdateChecker.initializeLauncher(this)` в `MainActivity.onCreate()` после `setContent`
  - [ ] Протестировать на реальном устройстве с Google Play Services
  - [ ] Убедиться, что `androidx.activity:activity-ktx` присутствует в зависимостях (уже есть через `androidx.activity:activity-compose`)

### Приоритет 2 (Medium) — Рекомендуется исправить

- [ ] **W-001, W-002, W-003:** Исправить unresolved Opt-in маркеры в `:benchmark` модуле
  - [ ] Открыть `benchmark/build.gradle.kts`
  - [ ] Добавить Compose BOM в секцию `dependencies`:
    ```kotlin
    implementation(platform(libs.androidx.compose.bom))
    ```
  - [ ] Добавить compileOnly зависимости для opt-in маркеров:
    ```kotlin
    compileOnly(libs.androidx.compose.material3)
    compileOnly("androidx.compose.foundation:foundation")
    compileOnly("androidx.compose.animation:animation")
    ```
  - [ ] Выполнить `./gradlew :benchmark:compileDebugKotlin` для проверки
  - [ ] Убедиться, что предупреждения исчезли

### Проверка после исправлений

- [ ] Выполнить полную очистку и сборку:
  ```bash
  ./gradlew clean
  ./gradlew assembleDebug
  ```
- [ ] Убедиться, что все предупреждения уровня `w:` устранены
- [ ] Проверить, что сборка по-прежнему успешна
- [ ] Запустить unit-тесты: `./gradlew test`
- [ ] Запустить android-тесты: `./gradlew connectedAndroidTest` (если доступно устройство)

---

## Дополнительные замечания

### SKIPPED задачи
В логе присутствуют задачи со статусом `SKIPPED`:
- `:app:checkKotlinGradlePluginConfigurationErrors SKIPPED`
- `:benchmark:checkKotlinGradlePluginConfigurationErrors SKIPPED`

Это нормальное поведение Gradle — задачи пропускаются, если их условия не выполнены или они не требуются для текущей сборки. Не требует исправления.

### UP-TO-DATE задачи
Большинство задач имеют статус `UP-TO-DATE`, что указывает на корректную работу Gradle Build Cache и инкрементальной компиляции. Это ожидаемое поведение.

### Configuration Cache
В логе присутствует сообщение:
```
Calculating task graph as configuration cache cannot be reused because file 'keystore.properties' has changed.
```

Это информационное сообщение, указывающее, что Gradle пересчитывает граф задач из-за изменения файла конфигурации. Не является предупреждением.

---

## Заключение

Все обнаруженные предупреждения не критичны для текущей сборки, но требуют исправления для обеспечения совместимости с будущими версиями библиотек и соблюдения best practices Android разработки.

**Рекомендуемый порядок исправления:**
1. Сначала исправить W-004 (High priority) — deprecated API
2. Затем исправить W-001, W-002, W-003 (Medium priority) — opt-in маркеры

**Оценка времени на исправление:**
- W-004: ~30-60 минут (включая тестирование)
- W-001, W-002, W-003: ~5-10 минут

**Общее время:** ~1 час



