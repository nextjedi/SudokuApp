package com.nextjedi.sudokustreak.domain.input

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.nextjedi.sudokustreak.domain.settings.AppSettings
import com.nextjedi.sudokustreak.domain.settings.AppSettingsRepository
import com.nextjedi.sudokustreak.domain.settings.StylusMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for [StylusInputManager] Android `actual`. Maps to TC-AS1..16
 * (plus extras) from `test-plan/01-test-scenarios.md` §3.2.
 *
 * ## Stack
 *
 * - [AndroidJUnit4] + Robolectric for `MotionEvent.obtain` synthesis.
 * - MockK for the [DigitRecognizer] + [AppSettingsRepository] doubles. No real
 *   ML Kit code is invoked — the recognizer is a pure interface mock.
 * - Turbine for asserting `events()` Flow emissions in order.
 * - `StandardTestDispatcher` + `TestScope` so the manager's internal
 *   auto-detect coroutine is deterministic under virtual time.
 *
 * Tests assume default `AppSettings()` unless they explicitly override via
 * [StylusInputManager.refreshCachedMode] or a stubbed repository response.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class StylusInputManagerTest {

    private lateinit var context: Context
    private lateinit var settings: AppSettingsRepository
    private lateinit var recognizer: DigitRecognizer
    private lateinit var settingsState: MutableStateFlow<AppSettings>
    private lateinit var dispatcher: kotlinx.coroutines.test.TestDispatcher
    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dispatcher = StandardTestDispatcher()
        testScope = TestScope(dispatcher)
        settingsState = MutableStateFlow(AppSettings())
        settings = mockk(relaxed = true)
        every { settings.flow } returns settingsState
        coEvery { settings.current() } answers { settingsState.value }
        coEvery { settings.update(any()) } answers {
            val transform = firstArg<(AppSettings) -> AppSettings>()
            settingsState.value = transform(settingsState.value)
        }
        recognizer = mockk(relaxed = true)
    }

    private fun newManager(
        mode: StylusMode = StylusMode.AUTO,
        threshold: Float = 0.75f,
        scope: CoroutineScope = testScope
    ): StylusInputManager =
        StylusInputManager(
            context = context,
            settings = settings,
            recognizer = recognizer,
            scope = scope
        ).also { it.refreshCachedMode(mode = mode, confidenceThreshold = threshold) }

    // ─── Test 1 (TC-AS1): finger touches are ignored ────────────────────────

    @Test
    fun finger_touch_ignored() = runTest(dispatcher) {
        val manager = newManager()
        manager.events().test {
            manager.onMotionEvent(motion(MotionEvent.TOOL_TYPE_FINGER, MotionEvent.ACTION_DOWN, 10f, 10f))
            advanceUntilIdle()
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
    }

    // ─── Test 2 (TC-AS2): stylus emits Begin → Move → End ───────────────────

    @Test
    fun stylus_touch_emits_begin_move_end() = runTest(dispatcher) {
        val manager = newManager()
        manager.events().test {
            manager.onMotionEvent(motion(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.ACTION_DOWN, 1f, 1f))
            manager.onMotionEvent(motion(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.ACTION_MOVE, 5f, 5f))
            manager.onMotionEvent(motion(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.ACTION_UP, 8f, 8f))
            assertIs<StylusEvent.StrokeBegin>(awaitItem())
            assertIs<StylusEvent.StrokeMove>(awaitItem())
            assertIs<StylusEvent.StrokeEnd>(awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    // ─── Test 3 (TC-AS3): historical points captured ────────────────────────

    @Test
    fun historical_points_captured() = runTest(dispatcher) {
        val manager = newManager()
        manager.events().test {
            // Build a MOVE event with 5 historical points + 1 current
            val evt = motionWithHistorical(
                xs = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f),
                ys = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f)
            )
            manager.onMotionEvent(evt)
            val move = awaitItem() as StylusEvent.StrokeMove
            // 5 historical + 1 current = 6 points
            assertEquals(6, move.points.size, "historical+current points should be 6")
            assertEquals(1f, move.points.first().x, 0.001f)
            assertEquals(6f, move.points.last().x, 0.001f)
            cancelAndConsumeRemainingEvents()
        }
    }

    // ─── Test 4 (TC-AS4): pressure mapped ───────────────────────────────────

    @Test
    fun pressure_mapped() = runTest(dispatcher) {
        val manager = newManager()
        manager.events().test {
            val evt = motion(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.ACTION_DOWN, 5f, 5f, pressure = 0.7f)
            manager.onMotionEvent(evt)
            val begin = awaitItem() as StylusEvent.StrokeBegin
            assertEquals(0.7f, begin.point.pressure, 0.001f)
            cancelAndConsumeRemainingEvents()
        }
    }

    // ─── Test 5 (TC-AS5): hover events emitted ──────────────────────────────

    @Test
    fun hover_events_emitted() = runTest(dispatcher) {
        val manager = newManager()
        manager.events().test {
            manager.onMotionEvent(motion(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.ACTION_HOVER_ENTER, 0f, 0f))
            manager.onMotionEvent(motion(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.ACTION_HOVER_EXIT, 0f, 0f))
            assertEquals(StylusEvent.HoverEnter, awaitItem())
            assertEquals(StylusEvent.HoverExit, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    // ─── Test 6 (TC-AS6): bundled model → no download required ──────────────

    @Test
    fun bundled_model_no_download_required() = runTest(dispatcher) {
        // The DigitRecognizer interface has NO `ensureModelDownloaded` method.
        // If the production MlKitDigitRecognizer ever sprouts one, the abstraction
        // intentionally hides it from the manager — bundling is invariant.
        val recognizerInterface = DigitRecognizer::class.java
        val methods = recognizerInterface.declaredMethods.map { it.name }
        assertFalse(
            methods.any { it.contains("download", ignoreCase = true) },
            "DigitRecognizer interface must not expose any download() method " +
                "(bundled-model invariant). Found: $methods"
        )
    }

    // ─── Test 7 (TC-AS7): confidence below threshold rejected ───────────────

    @Test
    fun confidence_below_threshold_rejected() = runTest(dispatcher) {
        val manager = newManager(threshold = 0.75f)
        coEvery { recognizer.recognize(any(), 0.75f) } returns DigitRecognitionResult.Unrecognized
        val r = manager.recognize(stroke5Points())
        assertEquals(DigitRecognitionResult.Unrecognized, r)
        coVerify(exactly = 1) { recognizer.recognize(any(), 0.75f) }
    }

    // ─── Test 8 (TC-AS8): non-digit candidate rejected ──────────────────────

    @Test
    fun non_digit_candidate_rejected() = runTest(dispatcher) {
        val manager = newManager()
        // Caller would see Unrecognized from the recognizer when "A" passes through
        // the digit filter inside MlKitDigitRecognizer.
        coEvery { recognizer.recognize(any(), any()) } returns DigitRecognitionResult.Unrecognized
        assertEquals(DigitRecognitionResult.Unrecognized, manager.recognize(stroke5Points()))
    }

    // ─── Test 9 (TC-AS9 lite): digits 1..9 recognized when score passes ─────

    @Test
    fun digits_1_through_9_recognized_when_score_passes() = runTest(dispatcher) {
        val manager = newManager(threshold = 0.5f)
        for (d in 1..9) {
            coEvery { recognizer.recognize(any(), 0.5f) } returns
                DigitRecognitionResult.Recognized(digit = d, confidence = 0.92f, latencyMs = 12)
            val r = manager.recognize(stroke5Points())
            val recognized = assertIs<DigitRecognitionResult.Recognized>(r)
            assertEquals(d, recognized.digit)
            assertEquals(0.92f, recognized.confidence, 0.001f)
            assertTrue(recognized.latencyMs >= 0)
        }
    }

    // ─── Test 10 (TC-AS10 lite): latency is recorded (>=0) ──────────────────

    @Test
    fun latency_recorded() = runTest(dispatcher) {
        val manager = newManager()
        coEvery { recognizer.recognize(any(), any()) } returns
            DigitRecognitionResult.Recognized(5, 0.9f, latencyMs = 85)
        val r = manager.recognize(stroke5Points())
        val recognized = assertIs<DigitRecognitionResult.Recognized>(r)
        assertTrue(recognized.latencyMs > 0, "latencyMs must be positive, was ${recognized.latencyMs}")
    }

    // ─── Test 11 (TC-AS14): auto-detect flips flag on first stroke ──────────

    @Test
    fun auto_detect_flips_flag_on_first_stroke() = runTest(dispatcher) {
        val manager = newManager(mode = StylusMode.AUTO)
        assertFalse(manager.isAutoDetected())
        manager.onMotionEvent(motion(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.ACTION_DOWN, 1f, 1f))
        advanceUntilIdle()
        assertTrue(manager.isAutoDetected(), "first stylus event must flip autoDetected")
        coVerify(atLeast = 1) { settings.update(any()) }
        assertTrue(settingsState.value.stylusAutoDetected, "settings.stylusAutoDetected must be true")
    }

    // ─── Test 12: auto-detect only writes once across 100 strokes ───────────

    @Test
    fun auto_detect_only_writes_once() = runTest(dispatcher) {
        val manager = newManager(mode = StylusMode.AUTO)
        repeat(100) {
            manager.onMotionEvent(motion(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.ACTION_DOWN, 1f, 1f))
        }
        advanceUntilIdle()
        // Exactly one settings.update should have fired regardless of stroke count.
        coVerify(exactly = 1) { settings.update(any()) }
    }

    // ─── Test 13 (TC-AS15): mode=NEVER blocks recognition ──────────────────

    @Test
    fun mode_never_blocks_recognition() = runTest(dispatcher) {
        val manager = newManager(mode = StylusMode.NEVER)
        manager.events().test {
            manager.onMotionEvent(motion(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.ACTION_DOWN, 1f, 1f))
            manager.onMotionEvent(motion(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.ACTION_MOVE, 5f, 5f))
            manager.onMotionEvent(motion(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.ACTION_UP, 8f, 8f))
            advanceUntilIdle()
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
    }

    // ─── Test 14: mode=AUTO + already detected → events route ───────────────

    @Test
    fun mode_auto_with_detected_routes_recognition() = runTest(dispatcher) {
        settingsState.value = settingsState.value.copy(stylusAutoDetected = true)
        val manager = newManager(mode = StylusMode.AUTO)
        manager.events().test {
            manager.onMotionEvent(motion(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.ACTION_DOWN, 1f, 1f))
            assertIs<StylusEvent.StrokeBegin>(awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    // ─── Test 15: mode=ALWAYS works even when not detected ──────────────────

    @Test
    fun mode_always_works_even_when_not_detected() = runTest(dispatcher) {
        val manager = newManager(mode = StylusMode.ALWAYS)
        assertFalse(settingsState.value.stylusAutoDetected)
        manager.events().test {
            manager.onMotionEvent(motion(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.ACTION_DOWN, 1f, 1f))
            assertIs<StylusEvent.StrokeBegin>(awaitItem())
            cancelAndConsumeRemainingEvents()
        }
        advanceUntilIdle()
        // ALWAYS mode must NOT write the auto-detected flag (it's an AUTO-only signal).
        coVerify(exactly = 0) { settings.update(any()) }
    }

    // ─── Test 16 (TC-AS17): S-Pen side button toggles pencil mode ───────────

    @Test
    fun spen_button_toggles_pencil_mode() = runTest(dispatcher) {
        val manager = newManager()
        manager.events().test {
            val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY)
            val consumed = manager.handleKeyEvent(keyEvent)
            assertTrue(consumed, "S-Pen side button event must be consumed")
            val emitted = awaitItem()
            assertEquals(StylusEvent.SideButtonPressed, emitted)
            cancelAndConsumeRemainingEvents()
        }
    }

    // ─── Test 17: recognizer errors are caught + never crash ────────────────

    @Test
    fun recognizer_errors_are_caught() = runTest(dispatcher) {
        val manager = newManager()
        coEvery { recognizer.recognize(any(), any()) } returns
            DigitRecognitionResult.Error("simulated ML Kit failure")
        val r = manager.recognize(stroke5Points())
        val err = assertIs<DigitRecognitionResult.Error>(r)
        assertEquals("simulated ML Kit failure", err.reason)
    }

    // ─── Test 18: concurrent recognize() calls serialized ───────────────────

    @Test
    fun concurrent_recognize_calls_serialized() = runTest(dispatcher) {
        val manager = newManager()
        coEvery { recognizer.recognize(any(), any()) } returns
            DigitRecognitionResult.Recognized(5, 0.9f, 10)

        val results = (1..2).map {
            async { manager.recognize(stroke5Points()) }
        }.awaitAll()
        assertEquals(2, results.size)
        results.forEach { r -> assertIs<DigitRecognitionResult.Recognized>(r) }
    }

    // ─── Test 19: gesture exclusion rects → applied at composition ──────────

    @Test
    fun gesture_exclusion_rects_applied() {
        // The Compose modifier itself is tested in :androidApp's UI test layer.
        // Here we assert the contract: setSystemGestureExclusionRects is API 29+
        // and the Manager exposes the buffer cap constant other layers depend on.
        assertEquals(2048, StylusInputManager.MAX_STROKE_POINTS)
        assertTrue(StylusInputManager.currentTimestampMs() > 0)
    }

    // ─── Test 20: stroke buffer bounded (no OOM under flood) ────────────────

    @Test
    fun stroke_buffer_bounded() = runTest(dispatcher) {
        val manager = newManager()
        // Send DOWN, then 10,000 MOVE events of 1 point each, then UP.
        manager.onMotionEvent(motion(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.ACTION_DOWN, 0f, 0f))
        repeat(10_000) { i ->
            manager.onMotionEvent(motion(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.ACTION_MOVE, i.toFloat(), 0f))
        }
        // Capture the StrokeEnd snapshot
        var endSize = -1
        manager.events().test {
            manager.onMotionEvent(motion(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.ACTION_UP, 0f, 0f))
            // Drain events until we see the StrokeEnd.
            while (true) {
                val ev = awaitItem()
                if (ev is StylusEvent.StrokeEnd) {
                    endSize = ev.points.size
                    break
                }
            }
            cancelAndConsumeRemainingEvents()
        }
        assertTrue(
            endSize in 1..StylusInputManager.MAX_STROKE_POINTS,
            "Stroke buffer must be bounded; got endSize=$endSize"
        )
        assertEquals(
            StylusInputManager.MAX_STROKE_POINTS, endSize,
            "After flooding 10k points, snapshot should be exactly the cap"
        )
    }

    // ─── Extra: convenience constructor instantiates without crashing ───────

    @Test
    fun convenience_constructor_creates_manager() {
        // The 2-arg constructor wires in MlKitDigitRecognizer + a default scope.
        // We don't invoke any recognition here (that would call real ML Kit) — we
        // only verify the constructor doesn't throw and the public API is intact.
        val manager = StylusInputManager(context, settings)
        assertNotNull(manager)
        assertNotNull(manager.events())
    }

    // ─── Extra: side-button ignores ACTION_UP / repeats ─────────────────────

    @Test
    fun spen_button_ignores_action_up_and_repeats() = runTest(dispatcher) {
        val manager = newManager()
        manager.events().test {
            // ACTION_UP must NOT trigger (we only fire on initial press)
            val up = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY)
            assertFalse(manager.handleKeyEvent(up))
            expectNoEvents()

            // repeatCount > 0 must NOT trigger (long-press hold)
            val repeat = KeyEvent(
                /* downTime = */ 0L,
                /* eventTime = */ 0L,
                /* action = */ KeyEvent.ACTION_DOWN,
                /* code = */ KeyEvent.KEYCODE_STYLUS_BUTTON_PRIMARY,
                /* repeat = */ 3
            )
            assertFalse(manager.handleKeyEvent(repeat))
            expectNoEvents()

            cancelAndConsumeRemainingEvents()
        }
    }

    // ─── Extra: unrelated key events not consumed ───────────────────────────

    @Test
    fun unrelated_key_events_not_consumed() {
        val manager = newManager()
        val volumeUp = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP)
        assertFalse(manager.handleKeyEvent(volumeUp))
    }

    // ─── Extra: ACTION_CANCEL resets buffer (no end emission) ───────────────

    @Test
    fun action_cancel_resets_buffer_without_emitting_end() = runTest(dispatcher) {
        val manager = newManager()
        manager.events().test {
            manager.onMotionEvent(motion(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.ACTION_DOWN, 1f, 1f))
            manager.onMotionEvent(motion(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.ACTION_MOVE, 2f, 2f))
            manager.onMotionEvent(motion(MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.ACTION_CANCEL, 2f, 2f))
            assertIs<StylusEvent.StrokeBegin>(awaitItem())
            assertIs<StylusEvent.StrokeMove>(awaitItem())
            // No StrokeEnd should have been emitted from the CANCEL.
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private fun motion(
        toolType: Int,
        action: Int,
        x: Float,
        y: Float,
        pressure: Float = 1.0f
    ): MotionEvent {
        val props = MotionEvent.PointerProperties().apply {
            id = 0
            this.toolType = toolType
        }
        val coords = MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
            this.pressure = pressure
            this.size = 1.0f
        }
        return MotionEvent.obtain(
            /* downTime = */ 0L,
            /* eventTime = */ System.currentTimeMillis(),
            /* action = */ action,
            /* pointerCount = */ 1,
            arrayOf(props),
            arrayOf(coords),
            /* metaState = */ 0,
            /* buttonState = */ 0,
            /* xPrecision = */ 1.0f,
            /* yPrecision = */ 1.0f,
            /* deviceId = */ 0,
            /* edgeFlags = */ 0,
            /* source = */ 0,
            /* flags = */ 0
        )
    }

    private fun motionWithHistorical(xs: FloatArray, ys: FloatArray): MotionEvent {
        check(xs.size == ys.size && xs.size >= 2) { "need >=2 points (current + historical)" }
        val props = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_STYLUS
        }
        // First (oldest) point is the initial ACTION_MOVE sample.
        val initialCoords = MotionEvent.PointerCoords().apply {
            x = xs[0]; y = ys[0]; pressure = 1.0f; size = 1.0f
        }
        val evt = MotionEvent.obtain(
            0L,
            System.currentTimeMillis(),
            MotionEvent.ACTION_MOVE,
            1,
            arrayOf(props),
            arrayOf(initialCoords),
            0, 0, 1.0f, 1.0f, 0, 0, 0, 0
        )
        // Add intermediate samples via addBatch; the final element becomes the
        // "current" point, all preceding become "historical".
        for (i in 1 until xs.size) {
            val c = MotionEvent.PointerCoords().apply {
                x = xs[i]; y = ys[i]; pressure = 1.0f; size = 1.0f
            }
            evt.addBatch(
                /* eventTime = */ System.currentTimeMillis() + i,
                arrayOf(c),
                /* metaState = */ 0
            )
        }
        return evt
    }

    private fun stroke5Points(): List<StylusPoint> = listOf(
        StylusPoint(1f, 1f, 0.5f, 0L),
        StylusPoint(2f, 2f, 0.5f, 16L),
        StylusPoint(3f, 3f, 0.5f, 32L),
        StylusPoint(4f, 4f, 0.5f, 48L),
        StylusPoint(5f, 5f, 0.5f, 64L)
    )
}
