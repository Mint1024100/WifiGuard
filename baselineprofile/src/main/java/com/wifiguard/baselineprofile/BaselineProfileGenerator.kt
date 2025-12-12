package com.wifiguard.baselineprofile

import android.os.Build
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
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        // Baseline Profile можно собирать на API 33+ (или на root-девайсе с adb root).
        // Чтобы тесты не падали на API < 33, помечаем прогон как skipped.
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)

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


