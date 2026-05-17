package com.nextjedi.sudokustreak.domain.sensor

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the pure flow operators in [SensorFlowOperators]. No
 * Android / SensorManager / Robolectric required — the operators are pure
 * upstream-to-downstream Flow transforms.
 *
 * Maps to test plan IDs in test-plan/01-test-scenarios.md §3.4:
 *  - TC-SE-AVG : [movingAverage_4points_returnsSimpleMean]
 *  - TC-SE-HYS : [lightHysteresis_classifiesCorrectlyAcrossThresholds]
 *  - TC-SE3   : [hysteresisLight_rapidFlicker_emitsAtMostOneTransition]
 *  - TC-SE-RST : [movingAverage_reset_clearsBuffer]
 *  - TC-SE-RST : [movingAverageTilt_freshCollector_startsFromZero]
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SensorFlowOperatorsTest {

    // ------------------------------------------------------------------
    // MovingAverage class — pure math, no flow.
    // ------------------------------------------------------------------

    @Test
    fun `movingAverage 4-points returns simple mean`() {
        val ma = MovingAverage(window = 4)
        ma.add(0f); ma.add(2f); ma.add(4f); ma.add(6f)
        assertEquals(3f, ma.value, 0.0001f)
    }

    @Test
    fun `movingAverage 1-point returns sample as-is`() {
        val ma = MovingAverage(window = 4)
        ma.add(2.5f)
        assertEquals(2.5f, ma.value, 0.0001f)
    }

    @Test
    fun `movingAverage circular buffer drops oldest at window`() {
        val ma = MovingAverage(window = 4)
        ma.add(0f); ma.add(0f); ma.add(0f); ma.add(0f)
        assertEquals(0f, ma.value, 0.0001f)
        ma.add(8f)  // oldest (0) drops; window is now [0,0,0,8]
        assertEquals(2f, ma.value, 0.0001f)
        ma.add(8f)
        assertEquals(4f, ma.value, 0.0001f)
    }

    @Test
    fun `movingAverage reset clears buffer`() {
        val ma = MovingAverage(window = 4)
        repeat(4) { ma.add(10f) }
        assertEquals(10f, ma.value, 0.0001f)
        ma.reset()
        assertEquals(0f, ma.value, 0.0001f)
        ma.add(5f)
        assertEquals(5f, ma.value, 0.0001f)
    }

    @Test
    fun `movingAverage single-spike outlier is dampened`() {
        // Sequence [0.1, 0.1, 1.0, 0.1, 0.1] over a 4-window → average at index 3
        // is (0.1+1.0+0.1+0.1)/4 = 0.325 (vs raw 0.1) — outlier dampened by ~3x.
        val ma = MovingAverage(window = 4)
        listOf(0.1f, 0.1f, 1.0f, 0.1f).forEach { ma.add(it) }
        val avgWithOutlier = ma.value
        assertTrue(
            avgWithOutlier < 0.5f,
            "spike of 1.0 should be averaged to <0.5 in window of 4, got $avgWithOutlier",
        )
        ma.add(0.1f)
        assertTrue(
            ma.value < 0.4f,
            "after outlier slides 1 step into window, average should drop further, got ${ma.value}",
        )
    }

    // ------------------------------------------------------------------
    // LightHysteresis class — sticky classifier.
    // ------------------------------------------------------------------

    @Test
    fun `lightHysteresis classifies correctly across thresholds`() {
        val h = LightHysteresis(low = 50f, high = 100f)

        // First reading inside the dead band → UNKNOWN (we don't know which way to go yet).
        assertEquals(LightBand.UNKNOWN, h.classify(75f))

        // Drop below the LOW threshold → DIM.
        assertEquals(LightBand.DIM, h.classify(30f))

        // Inside dead band — stay DIM (sticky).
        assertEquals(LightBand.DIM, h.classify(75f))

        // Cross HIGH threshold → BRIGHT.
        assertEquals(LightBand.BRIGHT, h.classify(105f))

        // Inside dead band — stay BRIGHT.
        assertEquals(LightBand.BRIGHT, h.classify(75f))

        // Drop below LOW → flip to DIM.
        assertEquals(LightBand.DIM, h.classify(40f))
    }

    @Test
    fun `lightHysteresis reset returns to UNKNOWN`() {
        val h = LightHysteresis()
        h.classify(30f)
        assertEquals(LightBand.DIM, h.classify(75f))  // still DIM after dead-band reading
        h.reset()
        assertEquals(LightBand.UNKNOWN, h.classify(75f))  // back to UNKNOWN
    }

    // ------------------------------------------------------------------
    // hysteresisLight flow operator — debounce + sticky classification.
    // ------------------------------------------------------------------

    @Test
    fun `hysteresisLight rapid flicker emits at most one transition`() = runTest {
        // Pattern: 30, 55, 30, 55 lux all within a single second. The 5s window
        // means only the very first cross-threshold transition (30 → DIM) should
        // be emitted. The 55-lux samples sit in the dead band so they don't
        // even attempt to flip the band — exactly per TC-SE3.
        val upstream = flow {
            emit(RawLight(lux = 30f, timestampMs = 1000L))
            emit(RawLight(lux = 55f, timestampMs = 1100L))
            emit(RawLight(lux = 30f, timestampMs = 1200L))
            emit(RawLight(lux = 55f, timestampMs = 1300L))
        }
        val out = upstream.hysteresisLight().toList()
        assertEquals(1, out.size, "expected 1 transition, got ${out.size}: $out")
        assertEquals(30f, out[0].lux, 0.001f)
    }

    @Test
    fun `hysteresisLight ignores dead-band-only readings`() = runTest {
        // All readings sit in the dead band [50, 100] → no emit ever.
        val upstream = flow {
            emit(RawLight(lux = 60f, timestampMs = 1000L))
            emit(RawLight(lux = 70f, timestampMs = 2000L))
            emit(RawLight(lux = 80f, timestampMs = 3000L))
            emit(RawLight(lux = 90f, timestampMs = 4000L))
        }
        val out = upstream.hysteresisLight().toList()
        assertEquals(0, out.size, "no cross-threshold readings → no emits, got $out")
    }

    @Test
    fun `hysteresisLight emits separated transitions outside window`() = runTest {
        // Drop → DIM emit at t=1000.
        // Rise → BRIGHT emit must wait for t-1000 ≥ 5000 → emit at t≥6000.
        val upstream = flow {
            emit(RawLight(lux = 30f, timestampMs = 1000L))    // emit DIM
            emit(RawLight(lux = 110f, timestampMs = 2000L))   // suppressed (inside 5s window)
            emit(RawLight(lux = 110f, timestampMs = 6500L))   // emit BRIGHT (outside window)
        }
        val out = upstream.hysteresisLight().toList()
        assertEquals(2, out.size, "expected DIM + BRIGHT, got $out")
        assertEquals(30f, out[0].lux, 0.001f)
        assertEquals(110f, out[1].lux, 0.001f)
    }

    // ------------------------------------------------------------------
    // movingAverageTilt flow operator.
    // ------------------------------------------------------------------

    @Test
    fun `movingAverageTilt freshCollector starts from zero buffer`() = runTest {
        // First sample echoes through (1-element average == sample itself).
        flow {
            emit(RawTilt(pitch = 0.5f, roll = -0.3f, timestampMs = 1L))
        }.movingAverageTilt().test {
            val first = awaitItem()
            assertEquals(0.5f, first.pitch, 0.0001f)
            assertEquals(-0.3f, first.roll, 0.0001f)
            awaitComplete()
        }
    }

    @Test
    fun `movingAverageTilt dampens single-spike outlier sequence`() = runTest {
        // Sequence [0.1, 0.1, 1.0, 0.1, 0.1] — the 1.0 spike should ride into a
        // window that averages it down to ~0.28-0.33 by the 4th emit.
        val emitted = flow {
            emit(RawTilt(0.1f, 0f, 1L))
            emit(RawTilt(0.1f, 0f, 2L))
            emit(RawTilt(1.0f, 0f, 3L))
            emit(RawTilt(0.1f, 0f, 4L))
            emit(RawTilt(0.1f, 0f, 5L))
        }.movingAverageTilt().toList()

        assertEquals(5, emitted.size)
        // After the spike (index 2): (0.1+0.1+1.0)/3 ≈ 0.4 — already dampened.
        assertTrue(
            emitted[2].pitch < 0.45f,
            "spike at index 2 should be averaged with 2 prior samples, got ${emitted[2].pitch}",
        )
        // Two samples after the spike (index 4): (0.1+1.0+0.1+0.1)/4 = 0.325.
        assertTrue(
            emitted[4].pitch < 0.4f,
            "spike at index 4 should be averaged across full window, got ${emitted[4].pitch}",
        )
        // The raw sample at index 2 was 1.0 → smoothed should be much lower.
        assertNotEquals(1.0f, emitted[2].pitch, "spike must not pass through unmodified")
    }

    // ------------------------------------------------------------------
    // debouncedProximity flow operator.
    // ------------------------------------------------------------------

    @Test
    fun `debouncedProximity NEAR sustained emits NEAR`() = runTest {
        // Sustained NEAR readings 300ms apart → emit NEAR.
        flow {
            emit(RawProximity(centimeters = 0f, timestampMs = 0L))
            emit(RawProximity(centimeters = 0f, timestampMs = 350L))
        }.debouncedProximity().test {
            assertEquals(ProximityEvent.Near, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `debouncedProximity FAR sustained emits FAR after NEAR`() = runTest {
        // Sequence: NEAR sustained → emit NEAR; then FAR sustained 500ms → emit FAR.
        val out = flow {
            emit(RawProximity(0f, 0L))
            emit(RawProximity(0f, 350L))     // emit NEAR
            emit(RawProximity(5f, 500L))     // candidate FAR starts
            emit(RawProximity(5f, 1100L))    // 600ms sustained — emit FAR
        }.debouncedProximity().toList()

        assertEquals(2, out.size, "expected NEAR then FAR, got $out")
        assertEquals(ProximityEvent.Near, out[0])
        assertEquals(ProximityEvent.Far, out[1])
    }

    @Test
    fun `debouncedProximity NEAR-FAR-NEAR within 100ms emits nothing (chatter)`() = runTest {
        // Pocket-flicker: 3 readings within 100ms total — neither NEAR nor FAR
        // sustained for its threshold (300/500ms). No emit.
        val out = flow {
            emit(RawProximity(0f, 0L))
            emit(RawProximity(5f, 30L))
            emit(RawProximity(0f, 70L))
        }.debouncedProximity().toList()
        assertEquals(0, out.size, "expected NO emits (chatter debounced), got $out")
    }

    @Test
    fun `debouncedProximity sustained FAR alone emits FAR (no prior NEAR)`() = runTest {
        // No prior state — first sustained FAR (500ms) emits FAR.
        val out = flow {
            emit(RawProximity(8f, 0L))
            emit(RawProximity(8f, 600L))
        }.debouncedProximity().toList()
        assertEquals(1, out.size)
        assertEquals(ProximityEvent.Far, out[0])
    }
}
