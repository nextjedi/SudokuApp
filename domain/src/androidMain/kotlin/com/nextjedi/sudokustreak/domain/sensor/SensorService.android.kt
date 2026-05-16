package com.nextjedi.sudokustreak.domain.sensor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Android actual for [SensorService].
 *
 * TODO actual: implement in Wave 2.
 *
 * Wave 2 outline:
 * - Use `SensorManager` + `Sensor.TYPE_PROXIMITY / TYPE_LIGHT / TYPE_ROTATION_VECTOR`.
 * - Register listeners at `SENSOR_DELAY_UI` (60ms — NOT `_GAME`, see PC-13).
 * - Proximity: 300ms NEAR / 500ms FAR debounce (audit H6).
 * - Ambient light: 4-point moving average + 5s hysteresis at 50/100 lux.
 * - Tilt: smoothed via 4-point moving average.
 * - Bound to `Lifecycle.Resumed` — `start()` on `ON_RESUME`, `stop()` on `ON_PAUSE`.
 */
actual class SensorService {
    actual fun proximityEvents(): Flow<ProximityEvent> {
        // TODO actual: register Sensor.TYPE_PROXIMITY in Wave 2.
        return emptyFlow()
    }

    actual fun ambientLightEvents(): Flow<AmbientLightEvent> {
        // TODO actual: register Sensor.TYPE_LIGHT in Wave 2.
        return emptyFlow()
    }

    actual fun tiltEvents(): Flow<TiltEvent> {
        // TODO actual: register Sensor.TYPE_ROTATION_VECTOR in Wave 2.
        return emptyFlow()
    }

    actual fun start() {
        // TODO actual: SensorManager.registerListener in Wave 2.
    }

    actual fun stop() {
        // TODO actual: SensorManager.unregisterListener in Wave 2.
    }
}
