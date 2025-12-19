package com.wifiguard.feature.scanner

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.wifiguard.MainActivity
import com.wifiguard.core.ui.testing.UiTestTags
import com.wifiguard.testing.FakeWifiScanner
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Критические UI сценарии:
 * - старт приложения → экран сканера
 * - открытие деталей сети
 * - переходы в Настройки и Анализ
 */
@HiltAndroidTest
@SdkSuppress(maxSdkVersion = 34)
@RunWith(AndroidJUnit4::class)
class ScannerCriticalUiTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val permissionRule: GrantPermissionRule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        GrantPermissionRule.grant(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
    }

    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var fakeWifiScanner: FakeWifiScanner

    @Before
    fun setup() {
        hiltRule.inject()
        fakeWifiScanner.resetToDefaults()
    }

    @Test
    fun scanner_showsNetworks_andOpensDetailsModal() {
        // Экран сканера отображается
        composeRule.onNodeWithTag(UiTestTags.SCANNER_SCREEN).assertIsDisplayed()

        // В списке есть тестовые сети
        composeRule.onNodeWithText("Test WiFi 1").assertIsDisplayed()
        composeRule.onNodeWithText("Test WiFi 2").assertIsDisplayed()

        // Открываем детали первой сети
        composeRule.onNodeWithText("Test WiFi 1").performClick()

        // Появилось модальное окно
        composeRule.onNodeWithTag(UiTestTags.NETWORK_DETAILS_MODAL).assertIsDisplayed()

        // Закрываем модалку
        composeRule.onNodeWithTag(UiTestTags.NETWORK_DETAILS_MODAL_CLOSE).performClick()

        // Возвращаемся к списку
        composeRule.onNodeWithTag(UiTestTags.SCANNER_NETWORKS_LIST).assertIsDisplayed()
    }

    @Test
    fun scanner_navigatesToSettings_andBack() {
        composeRule.onNodeWithTag(UiTestTags.SCANNER_SCREEN).assertIsDisplayed()

        // Переход в настройки
        composeRule.onNodeWithTag(UiTestTags.SCANNER_ACTION_SETTINGS).performClick()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_SCREEN).assertIsDisplayed()

        // Открываем диалог выбора темы (как критичный пользовательский сценарий)
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_ITEM_THEME).performClick()
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_THEME_DIALOG).assertIsDisplayed()

        // Возврат назад
        composeRule.onNodeWithTag(UiTestTags.SETTINGS_NAV_BACK).performClick()
        composeRule.onNodeWithTag(UiTestTags.SCANNER_SCREEN).assertIsDisplayed()
    }

    @Test
    fun scanner_navigatesToAnalysis_andBack() {
        composeRule.onNodeWithTag(UiTestTags.SCANNER_SCREEN).assertIsDisplayed()

        // Переход в анализ
        composeRule.onNodeWithTag(UiTestTags.SCANNER_ACTION_ANALYSIS).performClick()
        composeRule.onNodeWithTag(UiTestTags.ANALYSIS_SCREEN).assertIsDisplayed()

        // Назад (используем стабильный testTag)
        composeRule.onNodeWithTag(UiTestTags.ANALYSIS_NAV_BACK).performClick()
        composeRule.onNodeWithTag(UiTestTags.SCANNER_SCREEN).assertIsDisplayed()
    }
}






