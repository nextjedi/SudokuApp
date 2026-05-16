package com.nextjedi.sudokustreak.domain.sensor

import kotlinx.coroutines.flow.Flow

/**
 * Cross-platform sensor pipeline. Three independently consumable hot flows:
 * proximity, ambient light, tilt.
 *
 * ## Lifecycle
 *
 * Sensors are battery-expensive. The actual MUST register listeners only after
 * [start] and unregister on [stop]. The standard binding is:
 * - Android: `LifecycleEventObserver` → `start()` on `ON_RESUME`, `stop()` on `ON_PAUSE`.
 *   See test-plan/01-test-scenarios.md TC-SE5 (`unregistersOnPause`).
 * - iOS: `start()` on `viewDidAppear` / scene activation; `stop()` on `didEnterBackground`.
 *
 * ## Sampling
 *
 * - Proximity: requires 300ms sustained NEAR before emission; 500ms sustained FAR
 *   to return. See audit H6 (anti-chatter policy).
 * - Ambient light: 4-point moving average; 5s hysteresis on 50/100 lux thresholds.
 *   See REVAMP_PLAN.md §10.
 * - Tilt: `SENSOR_DELAY_UI` (60ms) on Android — `SENSOR_DELAY_GAME` wakes the fusion
 *   DSP and burns battery (PC-13 in test-plan/00-SYNTHESIS.md).
 *
 * ## iOS notes
 *
 * iPad has no proximity sensor — the actual must guard `userInterfaceIdiom == .phone`
 * and emit nothing from [proximityEvents] on iPad. iOS also has no public ambient
 * light API; [ambientLightEvents] is a no-op on iOS (Settings row will show
 * "Not available on this device"). See REVAMP_PLAN.md §11.
 *
 * TODO actual: implement in Wave 2 (Android + iOS) — see REVAMP_PLAN.md §10 and §11.
 */
expect class SensorService {
    fun proximityEvents(): Flow<ProximityEvent>
    fun ambientLightEvents(): Flow<AmbientLightEvent>
    fun tiltEvents(): Flow<TiltEvent>

    /** Register platform sensor listeners. Idempotent — calling twice is a no-op. */
    fun start()

    /** Unregister platform sensor listeners. Idempotent. */
    fun stop()
}

/**
 * Proximity sensor state. Most Android phones report distance in cm, but the boundary
 * between NEAR and FAR is hardware-specific and noisy — the actual normalizes to
 * this two-state enum after debouncing.
 */
sealed class ProximityEvent {
    data object Near : ProximityEvent()
    data object Far : ProximityEvent()
}

/**
 * Ambient light reading in lux. Smoothed by a 4-point moving average inside the actual.
 * Consumers should apply 5s hysteresis at theme-switch boundaries (50 lux → DARK,
 * 100 lux → back to SYSTEM).
 */
data class AmbientLightEvent(val lux: Float)

/**
 * Device tilt in radians.
 *
 * @property pitch rotation around the X axis (toward/away from user).
 * @property roll rotation around the Z axis (twist left/right).
 *
 * Mapped to a notes-offset of `clamp(roll * 8dp, -8dp, +8dp)` inside the Game screen
 * when `AppSettings.tiltParallaxEnabled && !reduceMotion`.
 */
data class TiltEvent(val pitch: Float, val roll: Float)
