package com.wifiguard.baselineprofile

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Генератор Baseline Profile для ускорения запуска и первых переходов.
 *
 * Важно: это не влияет на UI/функционал, а только помогает ART заранее компилировать горячий код.
 * Baseline Profile требует API 28+ (Android 9.0+).
 */
@RequiresApi(Build.VERSION_CODES.P) // API 28 - минимальная версия для Baseline Profile
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        // Baseline Profile можно собирать на API 28+ (или на root-девайсе с adb root).
        // Чтобы тесты не падали на API < 28, помечаем прогон как skipped.
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)

        baselineProfileRule.collect(
            packageName = TARGET_PACKAGE,
        ) {
            // Минимальный стабильный сценарий: cold start и ожидание первого кадра.
            pressHome()
            startActivityAndWait()
        }
    }

    private companion object {
        private const val TARGET_PACKAGE = "com.wifiguard"
    }
}












