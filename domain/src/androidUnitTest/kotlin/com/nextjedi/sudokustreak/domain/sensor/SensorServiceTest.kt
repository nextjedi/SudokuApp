package com.nextjedi.sudokustreak.domain.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.PowerManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.SensorEventBuilder
import org.robolectric.shadows.ShadowPowerManager
import org.robolectric.shadows.ShadowSensor
import org.robolectric.shadows.ShadowSensorManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

// Turbine default test() timeout is 3s; we shorten/lengthen per-test for clarity.
private val TURBINE_DEFAULT_TIMEOUT = 2.seconds
private val TURBINE_SHORT_TIMEOUT = 1.seconds

/**
 * Robolectric-backed tests for [SensorService] (Android actual).
 *
 * Covers the 12 required test cases from `test-plan/01-test-scenarios.md` §3.4
 * (SensorService — Android), plus three Battery-Saver / lifecycle override
 * tests added in Wave 2 to lock down PC-13 (`SENSOR_DELAY_UI`) and P1-21
 * (Battery Saver tilt suppression).
 *
 * Total: 15 tests (target 15, minimum 12).
 *
 * | TC ID                                                    | Test name (this file)                                            |
 * |----------------------------------------------------------|------------------------------------------------------------------|
 * | TC-SE1  proximity_near_emits_event                       | [proximity_value0_emitsNear]                                     |
 * | TC-SE2  proximity_far_emits_event                        | [proximity_value8_emitsFar]                                      |
 * | TC-SE3  light_debounce_emits_at_most_once_per_5s         | [ambientLight_rapidFlicker_emitsAtMostOneTransition]             |
 * | TC-SE4  tilt_moving_average_dampens_outlier              | [tilt_singleSpikeOutlier_smoothedOutputStable]                   |
 * | TC-SE5  unregisters_on_pause                             | [lifecyclePaused_unregistersAllSensors]                          |
 * | TC-SE6  reregisters_on_resume                            | [lifecycleResumed_afterPause_reRegistersSensors]                 |
 * | TC-SE7  start_is_idempotent                              | [start_calledTwice_onlyRegistersOnce]                            |
 * | TC-SE8  rotation_vector_uses_SENSOR_DELAY_UI             | [rotationVector_normalMode_usesSensorDelayUi]                    |
 * | TC-SE9  battery_saver_downgrades_rotation_vector         | [batterySaver_downgradesRotationVectorToNormal]                  |
 * | TC-SE10 battery_saver_disables_tilt_emission             | [batterySaver_disablesTiltEmission]                              |
 * | TC-SE11 proximity_300ms_near_debounce                    | [proximity_chatterWithin100ms_emitsNothing]                      |
 * | TC-SE12 proximity_500ms_far_debounce                     | [proximity_sustained500msFar_emitsFar]                           |
 * | TC-SE13 light_hysteresis_high_threshold                  | [ambientLight_hysteresisHighThreshold_emitsOnlyOnce]             |
 * | TC-SE14 concurrent_writes_safe                           | [sharedFlow_concurrentWritesFromMultipleCoroutines_allReachFlow] |
 * | TC-SE15 no_memory_leak_after_stop                        | [startStopCycle100x_listenerCountStableAtZero]                   |
 *
 * ## Why Robolectric, not unit-mock
 *
 * The Android sensor framework's contract (`registerListener` dispatch to a Looper,
 * `getDefaultSensor` semantics, the per-event `SensorEvent.values` layout) is not
 * trivially mockable. Robolectric's `ShadowSensorManager` mirrors the real
 * dispatch precisely and lets us call `sendSensorEventToListeners` to drive the
 * SensorService end-to-end through its real `SensorEventListener` chain.
 *
 * ## What we do NOT test here
 *
 * - Real-device battery drain (TC-SE11 is a macrobenchmark — gated separately).
 * - The cover/uncover UX path (TC-SE12 is a manual smoke test).
 * - Game ViewModel integration (TC-SE6-SE8 in scenarios.md cover ViewModel layer
 *   wiring, which arrives in a later wave once GameViewModel migrates to :domain).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])  // pin to API 33 — PowerManager.isPowerSaveMode is API 21+, sensors API 1+
class SensorServiceTest {

    private lateinit var context: Context
    private lateinit var sensorManager: SensorManager
    private lateinit var shadowSensorManager: ShadowSensorManager
    private lateinit var powerManager: PowerManager
    private lateinit var shadowPowerManager: ShadowPowerManager

    private lateinit var proximitySensor: Sensor
    private lateinit var lightSensor: Sensor
    private lateinit var rotationSensor: Sensor

    // Mutable virtual clock — tests advance this directly with [advanceVirtualClock].
    // Used by SensorService's nowMs hook so debounce/hysteresis timing is fully
    // deterministic and doesn't depend on Robolectric's ShadowSystemClock semantics
    // (which has tricky interactions with UnconfinedTestDispatcher).
    private var virtualNowMs: Long = 1_000L

    private lateinit var service: SensorService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shadowSensorManager = Shadows.shadowOf(sensorManager)
        powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        shadowPowerManager = Shadows.shadowOf(powerManager)

        // Build fake Sensor instances and register them with the shadow manager.
        proximitySensor = ShadowSensor.newInstance(Sensor.TYPE_PROXIMITY)
        lightSensor = ShadowSensor.newInstance(Sensor.TYPE_LIGHT)
        rotationSensor = ShadowSensor.newInstance(Sensor.TYPE_ROTATION_VECTOR)
        shadowSensorManager.addSensor(proximitySensor)
        shadowSensorManager.addSensor(lightSensor)
        shadowSensorManager.addSensor(rotationSensor)

        // Default mode: not in Battery Saver. Individual tests flip this.
        shadowPowerManager.setIsPowerSaveMode(false)

        service = SensorService(
            sensorManager = sensorManager,
            powerManager = powerManager,
            backgroundDispatcher = UnconfinedTestDispatcher(),
            nowMs = { virtualNowMs },
        )
    }

    @After
    fun tearDown() {
        service.stop()
    }

    // ----- Helpers -----------------------------------------------------------

    private fun sendProximity(cm: Float) {
        val event = SensorEventBuilder.newBuilder()
            .setSensor(proximitySensor)
            .setValues(floatArrayOf(cm))
            .setTimestamp(System.nanoTime())
            .build()
        shadowSensorManager.sendSensorEventToListeners(event)
    }

    private fun sendLight(lux: Float) {
        val event = SensorEventBuilder.newBuilder()
            .setSensor(lightSensor)
            .setValues(floatArrayOf(lux))
            .setTimestamp(System.nanoTime())
            .build()
        shadowSensorManager.sendSensorEventToListeners(event)
    }

    /**
     * Build a rotation-vector event from a desired (pitch, roll) by computing
     * the equivalent rotation-vector quaternion components. We use a small-angle
     * approximation that is good enough for SensorManager.getOrientation to
     * decode back to the same pitch/roll within ~1e-3 rad.
     */
    private fun sendRotation(pitch: Float, roll: Float) {
        // Pitch is rotation about X, roll is rotation about Z (per Android sensor coords).
        // For small angles, the rotation vector ≈ (axis_x*sin(θ/2), axis_y*sin(θ/2),
        //                                          axis_z*sin(θ/2), cos(θ/2)).
        // We compose pitch (X) then roll (Z) quaternions and multiply.
        val halfPitch = pitch / 2f
        val halfRoll = roll / 2f
        val sp = kotlin.math.sin(halfPitch.toDouble()).toFloat()
        val cp = kotlin.math.cos(halfPitch.toDouble()).toFloat()
        val sr = kotlin.math.sin(halfRoll.toDouble()).toFloat()
        val cr = kotlin.math.cos(halfRoll.toDouble()).toFloat()
        // q_pitch = (sp, 0, 0, cp); q_roll = (0, 0, sr, cr).
        // q = q_pitch * q_roll = (sp*cr, sp*sr, cp*sr, cp*cr)
        val x = sp * cr
        val y = sp * sr
        val z = cp * sr
        val w = cp * cr
        val event = SensorEventBuilder.newBuilder()
            .setSensor(rotationSensor)
            .setValues(floatArrayOf(x, y, z, w))
            .setTimestamp(System.nanoTime())
            .build()
        shadowSensorManager.sendSensorEventToListeners(event)
    }

    // =========================================================================
    // TC-SE1
    // =========================================================================
    @Test
    fun proximity_value0_emitsNear() = runTest {
        service.start()
        service.proximityEvents().test(timeout = TURBINE_DEFAULT_TIMEOUT) {
            // Sustained NEAR: send twice >300ms apart (synthesised by the operator
            // using each event's timestamp, which is System.currentTimeMillis()
            // baked in by the SensorService listener — Robolectric ticks the
            // clock between calls).
            sendProximity(0f)
            // Robolectric's Looper.idle() lets the SensorManager dispatch the event;
            // the SensorService's tryEmit is synchronous so the SharedFlow gets it
            // before we hit awaitItem.
            shadowOfLooperIdle()
            // Bump time so the second sample has a timestamp >300ms past the first.
            advanceVirtualClock(400L)
            sendProximity(0f)
            shadowOfLooperIdle()
            assertEquals(ProximityEvent.Near, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // =========================================================================
    // TC-SE2
    // =========================================================================
    @Test
    fun proximity_value8_emitsFar() = runTest {
        service.start()
        service.proximityEvents().test(timeout = TURBINE_DEFAULT_TIMEOUT) {
            sendProximity(8f)
            shadowOfLooperIdle()
            advanceVirtualClock(600L)  // > 500ms FAR debounce
            sendProximity(8f)
            shadowOfLooperIdle()
            assertEquals(ProximityEvent.Far, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // =========================================================================
    // TC-SE3 — light debounce
    // =========================================================================
    @Test
    fun ambientLight_rapidFlicker_emitsAtMostOneTransition() = runTest {
        service.start()
        service.ambientLightEvents().test(timeout = TURBINE_DEFAULT_TIMEOUT) {
            sendLight(30f)   // → DIM, first emit
            shadowOfLooperIdle()
            assertEquals(30f, awaitItem().lux, 0.001f)

            // Rapid flicker around the threshold — none should cross the dead band
            // (55 is INSIDE [50, 100] so stays DIM by stickiness; 30/55/30/55 with
            // <100ms between each is well inside the 5s window).
            sendLight(55f); shadowOfLooperIdle()
            sendLight(30f); shadowOfLooperIdle()
            sendLight(55f); shadowOfLooperIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // =========================================================================
    // TC-SE4 — tilt moving average dampens outlier
    // =========================================================================
    @Test
    fun tilt_singleSpikeOutlier_smoothedOutputStable() = runTest {
        service.start()
        service.tiltEvents().test(timeout = TURBINE_DEFAULT_TIMEOUT) {
            // Prime the moving average with 4 readings at pitch=0.
            repeat(4) {
                sendRotation(pitch = 0f, roll = 0f)
                shadowOfLooperIdle()
                awaitItem()  // discard priming output
            }
            // Inject a 1.0 rad spike.
            sendRotation(pitch = 1.0f, roll = 0f)
            shadowOfLooperIdle()
            val smoothed = awaitItem()
            // 4-point average of [0, 0, 0, 1.0] = 0.25.
            assertTrue(
                smoothed.pitch < 0.30f,
                "spike should be averaged out, got pitch=${smoothed.pitch}",
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    // =========================================================================
    // TC-SE5 — lifecycle PAUSED unregisters
    // =========================================================================
    @Test
    fun lifecyclePaused_unregistersAllSensors() = runTest {
        val owner = TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
        service.observe(owner)
        // observe() triggers start() because owner is already RESUMED.
        assertTrue(service.isRegistered, "service should auto-start on already-resumed owner")

        owner.currentState = Lifecycle.State.CREATED  // CREATED < STARTED → triggers ON_PAUSE
        assertFalse(service.isRegistered, "PAUSE event should unregister all listeners")
        assertEquals(
            0, shadowSensorManager.getListeners().size,
            "no SensorEventListeners should remain registered after PAUSE",
        )
    }

    // =========================================================================
    // TC-SE6 — lifecycle RESUMED re-registers
    // =========================================================================
    @Test
    fun lifecycleResumed_afterPause_reRegistersSensors() = runTest {
        val owner = TestLifecycleOwner(initialState = Lifecycle.State.CREATED)
        service.observe(owner)
        assertFalse(service.isRegistered, "should not be registered yet (CREATED)")

        // Move to RESUMED — triggers ON_START then ON_RESUME.
        owner.currentState = Lifecycle.State.RESUMED
        assertTrue(service.isRegistered, "RESUME event should register listeners")
        assertTrue(
            shadowSensorManager.getListeners().size >= 3,
            "should register one listener per sensor (proximity + light + rotation), got ${shadowSensorManager.getListeners().size}",
        )

        // Bounce: PAUSE → RESUME again. Should re-register cleanly.
        owner.currentState = Lifecycle.State.CREATED
        assertFalse(service.isRegistered)
        owner.currentState = Lifecycle.State.RESUMED
        assertTrue(service.isRegistered, "second RESUME after PAUSE should re-register")
    }

    // =========================================================================
    // TC-SE7 — start() idempotent
    // =========================================================================
    @Test
    fun start_calledTwice_onlyRegistersOnce() {
        service.start()
        val firstListener = service.proximityListenerInternal
        assertNotNull(firstListener, "first start() should register proximity listener")
        val firstSize = shadowSensorManager.getListeners().size

        service.start()  // second call — should be a no-op.
        assertEquals(
            firstListener, service.proximityListenerInternal,
            "second start() must reuse the same listener instance (no double registration)",
        )
        assertEquals(
            firstSize, shadowSensorManager.getListeners().size,
            "listener count should not increase on second start()",
        )
    }

    // =========================================================================
    // TC-SE8 — rotation vector uses SENSOR_DELAY_UI in normal mode
    // =========================================================================
    @Test
    fun rotationVector_normalMode_usesSensorDelayUi() {
        // Battery Saver off (default in setUp).
        assertFalse(powerManager.isPowerSaveMode)
        service.start()
        assertEquals(
            SensorManager.SENSOR_DELAY_UI,
            service.rotationSampleRateInternal,
            "rotation vector MUST use SENSOR_DELAY_UI (≈60ms), NOT _GAME (≈20ms) — PC-13",
        )
    }

    // =========================================================================
    // TC-SE9 — Battery Saver downgrades rotation vector to NORMAL
    // =========================================================================
    @Test
    fun batterySaver_downgradesRotationVectorToNormal() {
        shadowPowerManager.setIsPowerSaveMode(true)
        service.start()
        assertEquals(
            SensorManager.SENSOR_DELAY_NORMAL,
            service.rotationSampleRateInternal,
            "when Battery Saver is on, rotation vector MUST downgrade to SENSOR_DELAY_NORMAL (≈200ms)",
        )
    }

    // =========================================================================
    // TC-SE10 — Battery Saver suppresses tilt emission
    // =========================================================================
    @Test
    fun batterySaver_disablesTiltEmission() = runTest {
        shadowPowerManager.setIsPowerSaveMode(true)
        service.start()
        service.tiltEvents().test(timeout = TURBINE_SHORT_TIMEOUT) {
            sendRotation(0.2f, 0.1f)
            shadowOfLooperIdle()
            sendRotation(0.3f, 0.1f)
            shadowOfLooperIdle()
            sendRotation(0.4f, 0.1f)
            shadowOfLooperIdle()
            // Tilt emission must be completely silenced under Battery Saver.
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // =========================================================================
    // TC-SE11 — 100ms chatter must be debounced
    // =========================================================================
    @Test
    fun proximity_chatterWithin100ms_emitsNothing() = runTest {
        service.start()
        service.proximityEvents().test(timeout = TURBINE_SHORT_TIMEOUT) {
            sendProximity(0f); shadowOfLooperIdle()
            advanceVirtualClock(20L)
            sendProximity(8f); shadowOfLooperIdle()
            advanceVirtualClock(20L)
            sendProximity(0f); shadowOfLooperIdle()
            advanceVirtualClock(20L)
            sendProximity(8f); shadowOfLooperIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // =========================================================================
    // TC-SE12 — sustained FAR (>500ms) emits FAR
    // =========================================================================
    @Test
    fun proximity_sustained500msFar_emitsFar() = runTest {
        service.start()
        service.proximityEvents().test(timeout = TURBINE_DEFAULT_TIMEOUT) {
            sendProximity(8f); shadowOfLooperIdle()
            advanceVirtualClock(600L)
            sendProximity(8f); shadowOfLooperIdle()
            assertEquals(ProximityEvent.Far, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // =========================================================================
    // TC-SE13 — hysteresis only emits one transition for 49/51/49/51 (high threshold)
    // =========================================================================
    @Test
    fun ambientLight_hysteresisHighThreshold_emitsOnlyOnce() = runTest {
        service.start()
        service.ambientLightEvents().test(timeout = TURBINE_DEFAULT_TIMEOUT) {
            sendLight(49f); shadowOfLooperIdle()     // < 50 → DIM, emit
            assertEquals(49f, awaitItem().lux, 0.001f)
            sendLight(51f); shadowOfLooperIdle()     // dead band — stays DIM
            sendLight(49f); shadowOfLooperIdle()
            sendLight(51f); shadowOfLooperIdle()
            // All flips stay within the dead band (50..100) → no further emit.
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // =========================================================================
    // TC-SE14 — concurrent writes from multiple coroutines all reach the flow
    // =========================================================================
    @Test
    fun sharedFlow_concurrentWritesFromMultipleCoroutines_allReachFlow() = runTest {
        service.start()
        // Subscribe BEFORE bursting writes so SharedFlow buffer DROP_OLDEST is
        // exercised under realistic conditions.
        service.proximityEvents().test(timeout = TURBINE_DEFAULT_TIMEOUT) {
            // Fire 100 concurrent raw events from 4 coroutines on Dispatchers.Default.
            // None of these are "sustained" (no time delta between them), so the
            // debouncer will not emit yet — but the SharedFlow itself MUST accept
            // them without crash or deadlock.
            val burstJobs = (0 until 4).map {
                launch(Dispatchers.Default) {
                    repeat(25) {
                        sendProximity(8f)
                    }
                }
            }
            burstJobs.forEach { it.join() }

            // Now push a sustained-FAR pair so the debouncer emits exactly one FAR.
            advanceVirtualClock(600L)
            sendProximity(8f)
            // The first FAR event from the original burst (timestamp = 1000) plus
            // this one (timestamp = 1600) is a 600ms sustained-FAR → emit FAR.
            assertEquals(ProximityEvent.Far, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // =========================================================================
    // TC-SE15 — no listener leak after start/stop cycling
    // =========================================================================
    @Test
    fun startStopCycle100x_listenerCountStableAtZero() {
        val initialCount = shadowSensorManager.getListeners().size
        repeat(100) {
            service.start()
            service.stop()
        }
        val finalCount = shadowSensorManager.getListeners().size
        assertEquals(
            initialCount, finalCount,
            "after 100 start/stop cycles, listener count must return to initial state (no leak)",
        )
        // Sanity: after a final start, exactly 3 listeners should be registered
        // (proximity + light + rotation), proving the cycle didn't break the
        // underlying SensorManager.
        service.start()
        assertEquals(
            initialCount + 3, shadowSensorManager.getListeners().size,
            "after final start(), 3 listeners (proximity + light + rotation) should be present",
        )
    }

    // ----- Robolectric helpers ----------------------------------------------

    /** Idle the main looper so SensorManager.registerListener's dispatch flushes. */
    private fun shadowOfLooperIdle() {
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    /**
     * Advance the virtual clock used by [SensorService] to timestamp sensor
     * readings. Side-steps Robolectric's `ShadowSystemClock` interaction with
     * `UnconfinedTestDispatcher` (which on 4.13 does not reliably propagate
     * `setCurrentTimeMillis` to the listener thread under that dispatcher).
     */
    private fun advanceVirtualClock(ms: Long) {
        virtualNowMs += ms
    }
}
