package com.nextjedi.sudokustreak.android

import android.app.Application
import com.nextjedi.sudokustreak.android.sensor.SensorServiceProvider

/**
 * Process-wide [Application] subclass for the Brain Gym Android app.
 *
 * Currently responsible for:
 *
 * 1. **Eagerly building the singleton [com.nextjedi.sudokustreak.domain.sensor.SensorService]**
 *    via [SensorServiceProvider.get]. We do this at Application scope rather than
 *    inside the first Activity so the (small) cost of querying `SENSOR_SERVICE`
 *    and `POWER_SERVICE` is paid once, off the screen-render critical path.
 *    Lifecycle attachment (`registerListener`) happens in `MainActivity.onCreate`
 *    via `SensorServiceProvider.bind(this, this)`.
 *
 * Future Wave 2 wiring (not yet in scope):
 * - DataStore for [com.nextjedi.sudokustreak.domain.settings.AppSettings].
 * - Dependency injection container if any.
 * - Crash reporter init (Firebase / etc.).
 */
class SudokuApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Pre-construct the SensorService singleton. NO listeners attach here —
        // that happens lazily on the first lifecycle ON_RESUME event from
        // MainActivity.onCreate's call to SensorServiceProvider.bind(...).
        SensorServiceProvider.get(this)
    }
}
