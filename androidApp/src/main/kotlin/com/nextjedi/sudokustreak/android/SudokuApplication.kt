package com.nextjedi.sudokustreak.android

import android.app.Application
import com.nextjedi.sudokustreak.android.analytics.AnalyticsModule
import com.nextjedi.sudokustreak.android.sensor.SensorServiceProvider

/**
 * Process-wide [Application] subclass for the Brain Gym Android app.
 *
 * Responsibilities:
 *
 * 1. **Eagerly build the singleton [com.nextjedi.sudokustreak.domain.sensor.SensorService]**
 *    via [SensorServiceProvider.get]. The cost of querying `SENSOR_SERVICE` +
 *    `POWER_SERVICE` is paid once, off the screen-render critical path. Lifecycle
 *    attachment (`registerListener`) happens in `MainActivity.onCreate` via
 *    `SensorServiceProvider.bind(this, this)`.
 *
 * 2. **Pre-construct the [com.nextjedi.sudokustreak.domain.analytics.AnalyticsService]**
 *    via [AnalyticsModule.get] so dependent ViewModels can be constructed with a
 *    non-null reference. **Privacy contract — locked decision §18.7:** this DOES NOT
 *    call `initialize()`. The SDK only initializes after the user flips
 *    `AppSettings.analyticsOptIn = true` inside `SettingsViewModel`. See
 *    `PostHogAnalyticsService` kdoc and `app_launch_without_opt_in_does_not_initialize`.
 */
class SudokuApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Pre-construct the SensorService singleton. NO listeners attach here —
        // that happens lazily on the first lifecycle ON_RESUME event from
        // MainActivity.onCreate's call to SensorServiceProvider.bind(...).
        SensorServiceProvider.get(this)

        // Pre-construct the analytics service. initialize() is intentionally NOT called —
        // it fires only after analyticsOptIn opts in.
        AnalyticsModule.get(this)
    }
}
