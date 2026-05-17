package com.nextjedi.sudokustreak.domain.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Android actual for [SensorService] — Wave 2.
 *
 * Wires three platform sensors as hot [kotlinx.coroutines.flow.Flow] streams:
 * - `Sensor.TYPE_PROXIMITY` → [proximityEvents], debounced 300ms NEAR / 500ms FAR.
 * - `Sensor.TYPE_LIGHT` → [ambientLightEvents], 5s hysteresis on 50/100 lux.
 * - `Sensor.TYPE_ROTATION_VECTOR` → [tiltEvents], 4-point moving average on pitch + roll.
 *
 * ## Construction
 *
 * The KMP `expect class SensorService` declares no constructor, so the actual must
 * provide a no-arg primary constructor for cross-platform conformance. Real Android
 * usage MUST go through the [Companion.create] factory (or the
 * `SensorServiceProvider` in `:androidApp`) which passes the Context.
 *
 * Calling sensor APIs ([start], [observe], or any of the flow accessors) on an
 * instance constructed via the no-arg constructor without first calling [attach]
 * is a programming error and will throw [IllegalStateException].
 *
 * ## Battery / sampling
 *
 * - Rotation vector uses **`SENSOR_DELAY_UI` (~60ms)** in normal mode, NOT
 *   `SENSOR_DELAY_GAME` (~20ms). The faster rate wakes the fusion DSP + accel
 *   + gyro and burns ~10-15 mA continuously on a Pixel 6 (test-plan/
 *   06-stylus-sensors-privacy-review.md §8). Parallax doesn't need 50 Hz.
 * - Proximity + light use whatever the driver gives (already low-rate).
 * - **Battery Saver override (P1-21):** when [PowerManager.isPowerSaveMode] is
 *   true, rotation vector is downgraded to `SENSOR_DELAY_NORMAL` (~200ms) AND
 *   tilt event emission is suppressed entirely so the parallax effect goes
 *   silent. This does NOT flip the user's persisted `tiltParallaxEnabled`
 *   setting — it is a runtime override that disappears as soon as Power Save
 *   turns off.
 *
 * ## Lifecycle
 *
 * Sensors are battery-expensive. Bind to a [LifecycleOwner] via [observe] and
 * the service will register listeners on `ON_RESUME` and unregister on `ON_PAUSE`.
 * Calling [start] / [stop] directly is also supported — both methods are
 * idempotent.
 *
 * ## Threading
 *
 * `SensorEventListener.onSensorChanged` is delivered on the main thread by
 * default (the Android sensor framework dispatches to whichever Looper
 * `registerListener` was called from). Smoothing operators run on
 * [Dispatchers.Default] via [flowOn], so the main thread only has to do the
 * single `MutableSharedFlow.tryEmit`.
 *
 * ## Permissions
 *
 * None of the three sensors require runtime permissions. Add
 * `<uses-feature android:required="false"/>` declarations to AndroidManifest
 * so the Play Store doesn't filter the listing out for devices that lack
 * (e.g.) a proximity sensor.
 */
actual class SensorService constructor() {

    // ----- Initialization state ----------------------------------------------
    //
    // The expect class declares no constructor → the actual provides an explicit
    // no-arg primary constructor for KMP conformance. Real Android usage attaches
    // a Context via [attach] (or via the [Companion.create] factory). Tests pass
    // mocked SensorManager + PowerManager directly through the internal
    // secondary constructor.
    private var sensorManager: SensorManager? = null
    private var powerManager: PowerManager? = null
    private var backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default

    /**
     * Wall-clock time provider used to timestamp sensor readings. Defaults to
     * [System.currentTimeMillis]. Injectable so unit tests can supply a virtual
     * clock without depending on `ShadowSystemClock` behaving in PAUSED mode
     * (which on Robolectric 4.13 does not always advance `currentTimeMillis`
     * deterministically when combined with `UnconfinedTestDispatcher`).
     */
    private var nowMs: () -> Long = { System.currentTimeMillis() }

    /**
     * Internal secondary constructor used by [Companion.create] and tests. Direct
     * construction with a Context is preferred over [attach] when possible.
     */
    internal constructor(
        sensorManager: SensorManager,
        powerManager: PowerManager,
        backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
        nowMs: () -> Long = { System.currentTimeMillis() },
    ) : this() {
        this.sensorManager = sensorManager
        this.powerManager = powerManager
        this.backgroundDispatcher = backgroundDispatcher
        this.nowMs = nowMs
    }

    /**
     * Late-bind a Context to a no-arg-constructed [SensorService]. Idempotent —
     * subsequent calls are ignored.
     */
    fun attach(context: Context) {
        if (sensorManager != null && powerManager != null) return
        val app = context.applicationContext
        sensorManager = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        powerManager = app.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    private fun requireSensorManager(): SensorManager = sensorManager
        ?: error("SensorService not attached — call attach(Context) or use SensorService.create(Context).")

    private fun requirePowerManager(): PowerManager = powerManager
        ?: error("SensorService not attached — call attach(Context) or use SensorService.create(Context).")

    // ----- Hot flows ----------------------------------------------------------
    //
    // Bounded MutableSharedFlow:
    //  - replay = 0    (consumers only see events after they subscribe)
    //  - extraBufferCapacity ≥ 8 / 16   (sensor bursts during start() should not be lost)
    //  - onBufferOverflow = DROP_OLDEST (the freshest reading is always more useful than a
    //                                    stale one — never block the sensor framework's main thread)
    private val _proximityRaw: MutableSharedFlow<RawProximity> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val _lightRaw: MutableSharedFlow<RawLight> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val _tiltRaw: MutableSharedFlow<RawTilt> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 16,  // rotation vector fires ~16 Hz; larger buffer prevents loss
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    // ----- Registered listener handles ---------------------------------------
    //
    // We keep separate listener instances per sensor for two reasons:
    //  1. SensorManager.registerListener(SensorEventListener, Sensor, rate) takes
    //     a single sensor — combining into one listener loses per-sensor sample rate.
    //  2. unregisterListener(SensorEventListener) without a specific Sensor argument
    //     removes ALL of that listener's registrations — useful for stop().
    private var proximityListener: SensorEventListener? = null
    private var lightListener: SensorEventListener? = null
    private var rotationListener: SensorEventListener? = null

    // Per-sensor sample rate chosen at registration time — saved so tests can verify.
    private var lastRotationSampleRate: Int = -1

    private var registered: Boolean = false
    private val registrationLock: Any = Any()  // guards start()/stop() reentrancy

    // ----- Lifecycle observer (held so we can unbind without leaking) --------
    private var lifecycleObserver: LifecycleEventObserver? = null
    private var boundLifecycle: Lifecycle? = null

    // ----- Test hooks --------------------------------------------------------
    //
    // Exposed for tests to verify registration without touching SensorManager internals.
    internal val isRegistered: Boolean get() = registered
    internal val proximityListenerInternal: SensorEventListener? get() = proximityListener
    internal val lightListenerInternal: SensorEventListener? get() = lightListener
    internal val rotationListenerInternal: SensorEventListener? get() = rotationListener
    internal val rotationSampleRateInternal: Int get() = lastRotationSampleRate

    // ----- Public Flow API ---------------------------------------------------

    actual fun proximityEvents(): Flow<ProximityEvent> =
        _proximityRaw
            .asSharedFlow()
            .debouncedProximity()
            .flowOn(backgroundDispatcher)

    actual fun ambientLightEvents(): Flow<AmbientLightEvent> =
        _lightRaw
            .asSharedFlow()
            .hysteresisLight()
            .flowOn(backgroundDispatcher)

    /**
     * Tilt event stream. Suppressed entirely when [PowerManager.isPowerSaveMode]
     * is `true` (Battery Saver override — see class kdoc). The check is per-emit
     * rather than per-subscription so a Battery-Saver toggle while the user is
     * playing immediately stops tilt events.
     */
    actual fun tiltEvents(): Flow<TiltEvent> =
        _tiltRaw
            .asSharedFlow()
            .movingAverageTilt()
            // Per-emit Battery-Saver check. Using a custom `flow {}` filter (rather
            // than `.filter { !isPowerSaveMode }`) so the predicate is re-read on
            // every emit — a Power-Save toggle while the user is playing immediately
            // stops tilt events. `.filter { ... }` would have the same behaviour at
            // runtime; the explicit `flow { collect { if ... emit }}` is just more
            // readable.
            .let { upstream ->
                flow {
                    upstream.collect { value ->
                        // requirePowerManager would throw if no Context attached; we
                        // tolerate "no Battery-Saver check" for the no-arg path to keep
                        // this filter side-effect-free outside a normal session.
                        val pm = powerManager
                        if (pm == null || !pm.isPowerSaveMode) emit(value)
                    }
                }
            }
            .flowOn(backgroundDispatcher)

    // ----- Lifecycle binding -------------------------------------------------

    /**
     * Bind to a [LifecycleOwner]. Listeners are registered on `ON_RESUME` and
     * unregistered on `ON_PAUSE`. Binding is idempotent — observing the same
     * owner twice is a no-op. Observing a new owner removes the previous binding.
     */
    fun observe(owner: LifecycleOwner) {
        // If we are already bound to this exact lifecycle, no-op.
        if (boundLifecycle === owner.lifecycle && lifecycleObserver != null) return

        // Remove any previous observer (binding to a new owner replaces the old one).
        lifecycleObserver?.let { boundLifecycle?.removeObserver(it) }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_PAUSE -> stop()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        lifecycleObserver = observer
        boundLifecycle = owner.lifecycle

        // If the owner is already past RESUMED when we bind, trigger start()
        // immediately — LifecycleEventObserver only fires on TRANSITIONS, not
        // current state. Without this, `observe(activityAlreadyResumed)` would
        // silently never register anything until the next pause+resume cycle.
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            start()
        }
    }

    // ----- Registration ------------------------------------------------------

    /**
     * Register all available sensor listeners. Idempotent — calling twice in a
     * row only produces one registration per sensor. Battery-Saver mode is read
     * here and used to choose rotation-vector sample rate.
     */
    actual fun start() {
        synchronized(registrationLock) {
            if (registered) return
            val sm = requireSensorManager()
            val pm = requirePowerManager()

            // Choose rotation rate: Battery Saver downgrades 60ms → 200ms.
            val rotationRate = if (pm.isPowerSaveMode) {
                SensorManager.SENSOR_DELAY_NORMAL  // ~200ms
            } else {
                SensorManager.SENSOR_DELAY_UI      // ~60ms (per REVAMP_PLAN §10, NOT _GAME)
            }
            lastRotationSampleRate = rotationRate

            // Proximity — value 0 is NEAR; >sensor.maximumRange is FAR (most devices
            // top out at 5 cm). We pass the raw centimeters; the debounce operator
            // does the boolean coercion.
            sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let { sensor ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val cm = event.values.getOrNull(0) ?: return
                        _proximityRaw.tryEmit(RawProximity(cm, nowMs()))
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                proximityListener = listener
            }

            // Light — single-value lux reading.
            sm.getDefaultSensor(Sensor.TYPE_LIGHT)?.let { sensor ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val lux = event.values.getOrNull(0) ?: return
                        _lightRaw.tryEmit(RawLight(lux, nowMs()))
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                lightListener = listener
            }

            // Rotation vector — 4-component quaternion-like vector. We convert
            // via SensorManager.getRotationMatrixFromVector + getOrientation
            // to extract pitch + roll (yaw is unused — see TiltEvent kdoc).
            sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let { sensor ->
                val rotationMatrix = FloatArray(9)
                val orientation = FloatArray(3)
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        // getRotationMatrixFromVector requires a 4- or 5-element
                        // rotation vector. Robolectric's fake events may produce
                        // shorter arrays — guard defensively.
                        if (event.values.size < 4) return
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        // orientation = [azimuth, pitch, roll], all in radians.
                        val pitch = orientation[1]
                        val roll = orientation[2]
                        _tiltRaw.tryEmit(RawTilt(pitch, roll, nowMs()))
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                sm.registerListener(listener, sensor, rotationRate)
                rotationListener = listener
            }

            registered = true
        }
    }

    /**
     * Unregister all sensor listeners. Idempotent. Listener references are
     * dropped so the GC can reclaim them — no leak across start/stop cycles.
     */
    actual fun stop() {
        synchronized(registrationLock) {
            if (!registered) return
            val sm = sensorManager ?: return
            proximityListener?.let { sm.unregisterListener(it) }
            lightListener?.let { sm.unregisterListener(it) }
            rotationListener?.let { sm.unregisterListener(it) }
            proximityListener = null
            lightListener = null
            rotationListener = null
            registered = false
        }
    }

    companion object {
        /**
         * Recommended way to construct a [SensorService] on Android. Pulls
         * `SENSOR_SERVICE` + `POWER_SERVICE` from the application Context.
         */
        fun create(context: Context): SensorService {
            val app = context.applicationContext
            val sm = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager
            return SensorService(sm, pm)
        }
    }
}
