package com.nextjedi.sudokustreak.domain.sensor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

/**
 * Wave-2 sensor flow operators — extracted from [SensorService] so they can be
 * unit-tested in isolation (no [android.hardware.SensorManager] / Robolectric needed
 * for the operator-only tests).
 *
 * Three operators:
 *
 * 1. [debouncedProximity] — requires sustained NEAR (300ms) or FAR (500ms) before
 *    re-emitting. Eliminates "pocket flicker" where the proximity reading toggles
 *    NEAR/FAR within tens of milliseconds because the user's hand brushes the
 *    sensor. See REVAMP_PLAN.md §10 ("Proximity debounce (resolves H6)").
 *
 * 2. [hysteresisLight] — emits only on cross-threshold transitions and at most
 *    once per [window]. The 5s window prevents theme flapping when the user's
 *    desk lamp flickers around 50 lux. See REVAMP_PLAN.md §10 (5s hysteresis on
 *    50/100 lux thresholds).
 *
 * 3. [movingAverageTilt] — 4-point moving average dampens single-spike outliers
 *    from the rotation vector. The Android fusion DSP can return a 1.0 rad spike
 *    every ~50ms if accel is bumped; without smoothing the parallax effect feels
 *    twitchy. See REVAMP_PLAN.md §10 ("4-point moving average on tilt").
 *
 * All three are PURE: they take an upstream [Flow] of raw readings and emit a
 * downstream [Flow] of filtered events. State is held in flow-collector scope
 * and reset every time the flow is re-subscribed (so `stop()` followed by
 * `start()` gives a fresh moving-average buffer — see [TestCases] in
 * `SensorServiceTest.tiltEvents_afterStopStart_freshMovingAverage`).
 */

// ---------------------------------------------------------------------------
// Threshold constants — exposed so tests can verify exact values.
// ---------------------------------------------------------------------------

/** Proximity debounce: NEAR must be sustained for [NEAR_DEBOUNCE_MS] before emit. */
internal const val NEAR_DEBOUNCE_MS: Long = 300L

/** Proximity debounce: FAR must be sustained for [FAR_DEBOUNCE_MS] before emit. */
internal const val FAR_DEBOUNCE_MS: Long = 500L

/** Hysteresis low threshold: lux below this is "DIM". */
internal const val LIGHT_LOW_LUX: Float = 50f

/** Hysteresis high threshold: lux above this is "BRIGHT". */
internal const val LIGHT_HIGH_LUX: Float = 100f

/** Hysteresis window: emit at most one transition every [LIGHT_HYSTERESIS_WINDOW_MS]. */
internal const val LIGHT_HYSTERESIS_WINDOW_MS: Long = 5_000L

/** Tilt moving-average window size. */
internal const val TILT_MOVING_AVERAGE_WINDOW: Int = 4

// ---------------------------------------------------------------------------
// Internal "raw" types — what the SensorEventListener actually emits before
// any filtering. Kept internal so they don't pollute the public expect API.
// ---------------------------------------------------------------------------

/**
 * Raw proximity reading from the SensorManager. `centimeters == 0f` is NEAR on
 * essentially every Android device; values past the sensor's max-range constant
 * are FAR. We normalize to a boolean inside [debouncedProximity] so callers
 * don't have to deal with the per-device max-range value.
 */
internal data class RawProximity(val centimeters: Float, val timestampMs: Long)

/** Raw light reading in lux from `SensorEvent.values[0]`. */
internal data class RawLight(val lux: Float, val timestampMs: Long)

/**
 * Raw rotation reading derived from `SensorEvent.values` (a 4-component
 * rotation vector). Pitch and roll are computed via
 * [android.hardware.SensorManager.getOrientation] outside this file.
 */
internal data class RawTilt(val pitch: Float, val roll: Float, val timestampMs: Long)

// ---------------------------------------------------------------------------
// Operator: proximity debounce (asymmetric NEAR/FAR thresholds).
// ---------------------------------------------------------------------------

/**
 * Holds a candidate proximity state until it has been sustained for the
 * appropriate threshold ([NEAR_DEBOUNCE_MS] for NEAR, [FAR_DEBOUNCE_MS] for FAR).
 * Only on sustained-enough confirmation do we emit downstream.
 *
 * Note: this is intentionally not [kotlinx.coroutines.flow.debounce] — that
 * operator uses a single threshold for both directions. Pocket-flicker has
 * different physics from re-entering the user's pocket (briefer / sharper).
 *
 * @param nearWindow how long the sensor must report NEAR continuously before
 *   we emit `ProximityEvent.Near`. Default `300.ms`.
 * @param farWindow same for `Far`. Default `500.ms`.
 * @param now monotonic time provider in milliseconds. Production passes
 *   `System.currentTimeMillis()`; tests pass a virtual-time advancer.
 */
internal fun Flow<RawProximity>.debouncedProximity(
    nearWindow: Long = NEAR_DEBOUNCE_MS,
    farWindow: Long = FAR_DEBOUNCE_MS,
): Flow<ProximityEvent> = flow {
    // State machine:
    //  - lastEmitted: which event we last forwarded downstream (null = none yet).
    //  - candidate:   the state we are currently accumulating evidence for.
    //  - candidateStartMs: when we first saw `candidate`.
    var lastEmitted: ProximityEvent? = null
    var candidate: ProximityEvent? = null
    var candidateStartMs: Long = 0L

    collect { reading ->
        // Per the Android sensor framework, "0 cm" is always NEAR. Anything
        // above the documented "near" boundary (~5cm on most devices) is FAR.
        // We treat anything at exactly 0f as NEAR — the raw event before
        // debounce comes from the SensorEventListener which already coerces.
        val incoming: ProximityEvent =
            if (reading.centimeters <= 0.5f) ProximityEvent.Near else ProximityEvent.Far

        if (incoming == lastEmitted) {
            // Already in this state — reset any candidate so a quick flip-back
            // doesn't latch a transition.
            candidate = null
            return@collect
        }

        if (incoming != candidate) {
            // New candidate transition starting — reset timer.
            candidate = incoming
            candidateStartMs = reading.timestampMs
            return@collect
        }

        // Same candidate as last reading — check sustained duration.
        val threshold = if (incoming == ProximityEvent.Near) nearWindow else farWindow
        if (reading.timestampMs - candidateStartMs >= threshold) {
            lastEmitted = incoming
            candidate = null
            emit(incoming)
        }
    }
}

// ---------------------------------------------------------------------------
// Operator: ambient-light hysteresis with a 5s window.
// ---------------------------------------------------------------------------

/**
 * Classifies a lux reading into a coarse band using two thresholds with a
 * "dead band" in between. The classifier is sticky: once we are in BRIGHT, a
 * dip to 60 lux does not flip us back to DIM (it must drop below [LIGHT_LOW_LUX]).
 *
 * Internal because the public API just emits [AmbientLightEvent] with the
 * actual lux value; this band exists only to decide WHEN to emit.
 */
internal enum class LightBand { DIM, BRIGHT, UNKNOWN }

internal class LightHysteresis(
    private val low: Float = LIGHT_LOW_LUX,
    private val high: Float = LIGHT_HIGH_LUX,
) {
    private var state: LightBand = LightBand.UNKNOWN

    fun classify(lux: Float): LightBand {
        state = when {
            lux < low -> LightBand.DIM
            lux > high -> LightBand.BRIGHT
            else -> state  // dead band — keep previous (or UNKNOWN on first call inside band)
        }
        return state
    }

    fun reset() {
        state = LightBand.UNKNOWN
    }
}

/**
 * Emit a downstream [AmbientLightEvent] only when:
 *   1. The [LightHysteresis] band changes from the previously emitted band.
 *   2. At least [window] ms have elapsed since the previous downstream emit.
 *
 * Both gates apply — rule 1 prevents redundant emits in the dead band, rule 2
 * prevents the user's theme from flipping every 100ms when light is hovering
 * around a threshold.
 */
internal fun Flow<RawLight>.hysteresisLight(
    lowLux: Float = LIGHT_LOW_LUX,
    highLux: Float = LIGHT_HIGH_LUX,
    window: Long = LIGHT_HYSTERESIS_WINDOW_MS,
): Flow<AmbientLightEvent> = flow {
    val hysteresis = LightHysteresis(low = lowLux, high = highLux)
    var lastEmittedBand: LightBand = LightBand.UNKNOWN
    var lastEmitTimeMs: Long = Long.MIN_VALUE

    collect { reading ->
        val band = hysteresis.classify(reading.lux)
        if (band == LightBand.UNKNOWN) {
            // Still in the dead band on first reading — wait for definitive crossing.
            return@collect
        }
        if (band == lastEmittedBand) return@collect

        val elapsed = reading.timestampMs - lastEmitTimeMs
        if (elapsed < window && lastEmittedBand != LightBand.UNKNOWN) {
            // Inside the 5s debounce window — suppress the emit. The next
            // reading after the window will re-evaluate and emit if the band
            // is still different.
            return@collect
        }

        lastEmittedBand = band
        lastEmitTimeMs = reading.timestampMs
        emit(AmbientLightEvent(lux = reading.lux))
    }
}

// ---------------------------------------------------------------------------
// Operator: tilt moving average.
// ---------------------------------------------------------------------------

/**
 * Fixed-window moving average. The buffer is a circular ring of size [window].
 * Until [window] samples have arrived, the average is computed across however
 * many we have so far — so the very first reading echoes through unchanged.
 *
 * Public-ish (`internal`) so [SensorServiceTest.movingAverage_4points_returnsSimpleMean]
 * can verify the math without booting the full sensor stack.
 */
internal class MovingAverage(val window: Int = TILT_MOVING_AVERAGE_WINDOW) {
    init { require(window > 0) { "moving-average window must be >0, got $window" } }
    private val ring = FloatArray(window)
    private var count: Int = 0
    private var nextIndex: Int = 0

    fun add(sample: Float) {
        ring[nextIndex] = sample
        nextIndex = (nextIndex + 1) % window
        if (count < window) count++
    }

    val value: Float
        get() {
            if (count == 0) return 0f
            var sum = 0f
            for (i in 0 until count) sum += ring[i]
            return sum / count
        }

    fun reset() {
        for (i in ring.indices) ring[i] = 0f
        count = 0
        nextIndex = 0
    }
}

/**
 * Applies a 4-point moving average to pitch + roll independently. A single
 * outlier therefore contributes 1/4 of its delta to the output, which is the
 * smoothing target for the parallax effect.
 *
 * Resets state every time the flow is re-collected (KMP standard:
 * `flow { ... }` creates a fresh state per collector).
 */
internal fun Flow<RawTilt>.movingAverageTilt(
    window: Int = TILT_MOVING_AVERAGE_WINDOW,
): Flow<TiltEvent> = flow {
    val pitchAvg = MovingAverage(window)
    val rollAvg = MovingAverage(window)
    collect { raw ->
        pitchAvg.add(raw.pitch)
        rollAvg.add(raw.roll)
        emit(TiltEvent(pitch = pitchAvg.value, roll = rollAvg.value))
    }
}

// ---------------------------------------------------------------------------
// Helper: convert a [kotlin.time.Duration] to milliseconds without pulling
// in the inline `.inWholeMilliseconds` — keeps the call sites tidy.
// ---------------------------------------------------------------------------

internal fun Duration.toMs(): Long = this.inWholeMilliseconds
