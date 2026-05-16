package com.nextjedi.sudokustreak.domain.sensor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Web (wasmJs) actual for [SensorService].
 *
 * Web has no proximity / ambient-light access from JS in a privacy-respecting way,
 * and Generic Sensor API is not yet broadly available. Permanently no-op.
 */
actual class SensorService {
    actual fun proximityEvents(): Flow<ProximityEvent> = emptyFlow()
    actual fun ambientLightEvents(): Flow<AmbientLightEvent> = emptyFlow()
    actual fun tiltEvents(): Flow<TiltEvent> = emptyFlow()
    actual fun start() {}
    actual fun stop() {}
}
