package com.wifiguard.core.ui.testing

/**
 * Теги для UI-тестов (Compose).
 *
 * ВАЖНО: теги не локализуются и не должны меняться без крайней необходимости,
 * иначе тесты станут нестабильными.
 */
object UiTestTags {
    const val SCANNER_SCREEN = "scanner_screen"
    const val SCANNER_TOP_APP_BAR = "scanner_top_app_bar"
    const val SCANNER_ACTION_REFRESH = "scanner_action_refresh"
    const val SCANNER_ACTION_ANALYSIS = "scanner_action_analysis"
    const val SCANNER_ACTION_SETTINGS = "scanner_action_settings"
    const val SCANNER_NETWORKS_LIST = "scanner_networks_list"

    fun networkCard(bssid: String): String = "network_card_$bssid"

    const val NETWORK_DETAILS_MODAL = "network_details_modal"
    const val NETWORK_DETAILS_MODAL_CLOSE = "network_details_modal_close"

    const val SETTINGS_SCREEN = "settings_screen"
    const val SETTINGS_TOP_APP_BAR = "settings_top_app_bar"
    const val SETTINGS_NAV_BACK = "settings_nav_back"
    const val SETTINGS_ITEM_THEME = "settings_item_theme"
    const val SETTINGS_THEME_DIALOG = "settings_theme_dialog"

    const val ANALYSIS_SCREEN = "analysis_screen"
    const val ANALYSIS_NAV_BACK = "analysis_nav_back"
}






