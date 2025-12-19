package com.wifiguard.core.updates

import android.app.Activity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * No-op реализация (например, для вкусов/сборок без Play Core).
 */
@Singleton
class NoOpAppUpdateChecker @Inject constructor() : AppUpdateChecker {
    override fun onResume(activity: Activity) {
        // intentionally no-op
    }

    override fun initializeLauncher(activity: Activity) {
        // intentionally no-op
    }
}






