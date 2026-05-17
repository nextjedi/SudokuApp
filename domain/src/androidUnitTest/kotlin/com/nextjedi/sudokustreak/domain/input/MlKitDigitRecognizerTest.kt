package com.nextjedi.sudokustreak.domain.input

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for [MlKitDigitRecognizer] that do NOT invoke real ML Kit.
 *
 * The production [MlKitDigitRecognizer] holds a `lazy` ML Kit client which is only
 * constructed on first `recognize()` call. We verify:
 *  - The class is instantiable on the Robolectric JVM (no Android-side init throw).
 *  - The `EN_TAG` constant matches the bundled-model identifier.
 *  - The digit-text filter set contains exactly 1..9 (no zero — TC-AS8b) and no letters.
 *  - An empty stroke returns [DigitRecognitionResult.Unrecognized] without invoking ML Kit.
 *
 * The behavioral tests (confidence threshold, digit filter, latency capture,
 * error handling, cancellation cooperation) are covered by [StylusInputManagerTest]
 * via the [DigitRecognizer] mock — that's where the contract lives.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MlKitDigitRecognizerTest {

    private val context: Context by lazy { ApplicationProvider.getApplicationContext() }

    @Test
    fun instantiates_without_throwing() {
        val recognizer = MlKitDigitRecognizer(context)
        assertNotNull(recognizer)
    }

    @Test
    fun en_tag_constant_is_lowercase_en() {
        assertEquals("en", MlKitDigitRecognizer.EN_TAG)
    }

    @Test
    fun digit_text_set_is_one_through_nine_inclusive() {
        val expected = setOf("1", "2", "3", "4", "5", "6", "7", "8", "9")
        assertEquals(expected, MlKitDigitRecognizer.DIGIT_TEXTS)
    }

    @Test
    fun digit_text_set_excludes_zero() {
        // TC-AS8b: Sudoku only writes 1..9. Zero must never satisfy the filter.
        assertTrue("0" !in MlKitDigitRecognizer.DIGIT_TEXTS)
    }

    @Test
    fun digit_text_set_excludes_letters() {
        // TC-AS8: a high-confidence "A" must be filtered upstream of the threshold check.
        assertTrue("A" !in MlKitDigitRecognizer.DIGIT_TEXTS)
        assertTrue("a" !in MlKitDigitRecognizer.DIGIT_TEXTS)
        assertTrue("$" !in MlKitDigitRecognizer.DIGIT_TEXTS)
    }

    @Test
    fun empty_stroke_returns_unrecognized_without_invoking_mlkit() = runTest {
        // Lazy ML Kit client never has a chance to load because the early-return
        // guard fires first. This proves the empty-input edge case is safe in
        // production without needing a network/AAR-asset round-trip in unit tests.
        val recognizer = MlKitDigitRecognizer(context)
        val r = recognizer.recognize(stroke = emptyList(), confidenceThreshold = 0.75f)
        assertEquals(DigitRecognitionResult.Unrecognized, r)
    }
}
