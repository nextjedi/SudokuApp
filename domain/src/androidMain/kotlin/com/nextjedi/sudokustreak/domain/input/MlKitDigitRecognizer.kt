package com.nextjedi.sudokustreak.domain.input

import android.content.Context
import android.os.SystemClock
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Production [DigitRecognizer] backed by ML Kit Digital Ink Recognition with the
 * bundled `en` text base model (REVAMP_PLAN.md §8, locked decision §6 (2) in
 * test-plan/00-SYNTHESIS.md).
 *
 * ## Why bundled `en` (not `zxx-Zsym-x-ipa`)
 *
 * Stylus-reviewer PC-6 caught the `zxx-Zsym-x-ipa` model code in an earlier plan as
 * non-existent. The actual ML Kit model identifier for English text is `"en"`, which
 * recognizes any handwritten word — including digits. The plan compensates by
 * filtering candidates to the digit set {1..9} after recognition (zero is excluded
 * because Sudoku only writes 1..9 — TC-AS8b).
 *
 * ## Why bundled (not download-on-first-use)
 *
 * Stylus-reviewer PC-7: cellular users with the WiFi-only download gate end up with
 * a broken feature the first time they try the pen. The text base model is ~20MB —
 * acceptable APK budget per `TC-AS21 apkSizeBudget()` (≤25MB delta). Bundling
 * eliminates the download path entirely: `RemoteModelManager.download()` is never
 * called, the constructor's `recognize()` call below uses the model packaged inside
 * the AAR. Test TC-AS6 asserts this by mocking the recognizer interface — no real
 * ML Kit invocation in unit tests.
 *
 * ## Threading
 *
 * Per §6 of `test-plan/06-stylus-sensors-privacy-review.md`, the recognizer MUST run
 * off-Main. We pin to [Dispatchers.Default] (CPU-bound classifier); the awaited
 * Task is internal to ML Kit, which already runs on its own thread pool, so this
 * dispatcher's only job is to keep the suspend boundary off the caller's dispatcher.
 *
 * Cancellation: a fresh stroke from the user should cancel any in-flight recognition
 * of the previous stroke (so a slow "5" recognition does not overwrite a freshly
 * placed "8"). [StylusInputManager.recognize] is `suspend`, so caller-side cancellation
 * (e.g., new stroke triggering `Job.cancelAndJoin`) propagates here naturally;
 * [CancellationException] is rethrown so coroutine semantics are preserved.
 *
 * ## Filtering & threshold
 *
 * Candidates are mapped from ML Kit's `RecognitionCandidate.text` to digits in two
 * steps:
 *  1. Reject any candidate whose `text !in {"1".."9"}` (drops "A", "$", "0", etc.).
 *  2. Among surviving digit candidates, reject those with `score < confidenceThreshold`.
 *  3. Of what remains, pick the highest-scoring digit.
 *
 * If no candidate survives → [DigitRecognitionResult.Unrecognized].
 * If ML Kit throws → [DigitRecognitionResult.Error].
 *
 * @param context used (currently) only to keep an Android-lifecycle handle in case a
 *                future revision needs `Context.assets` or a system service. The
 *                bundled-model path itself does not need a Context.
 */
class MlKitDigitRecognizer(
    @Suppress("UNUSED_PARAMETER") context: Context
) : DigitRecognizer {

    /**
     * Lazy ML Kit recognizer init. The model identifier must be non-null for the
     * `"en"` tag (we validate by `!!`). If ML Kit removes `"en"` in a future SDK
     * we want a fast hard fail at construction time rather than a quiet downgrade.
     */
    private val recognizer: DigitalInkRecognizer by lazy {
        val identifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(EN_TAG)
            ?: error("ML Kit Digital Ink: '$EN_TAG' identifier missing (SDK regression?)")
        val model = DigitalInkRecognitionModel.builder(identifier).build()
        DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model).build()
        )
    }

    override suspend fun recognize(
        stroke: List<StylusPoint>,
        confidenceThreshold: Float
    ): DigitRecognitionResult = withContext(Dispatchers.Default) {
        // Empty stroke → no-op. Avoids ML Kit IllegalArgumentException for empty ink.
        if (stroke.isEmpty()) return@withContext DigitRecognitionResult.Unrecognized

        val started = SystemClock.elapsedRealtime()
        try {
            val ink = buildInk(stroke)
            val result = recognizer.recognize(ink).await()
            val candidates = result.candidates ?: emptyList()

            val best = candidates
                .asSequence()
                .filter { candidate -> candidate.text in DIGIT_TEXTS }
                .filter { candidate -> candidate.score?.let { it >= confidenceThreshold } == true }
                .maxByOrNull { candidate -> candidate.score ?: 0f }
                ?: return@withContext DigitRecognitionResult.Unrecognized

            // The `text in DIGIT_TEXTS` filter guarantees the int parse succeeds.
            val digit = best.text.toIntOrNull()
                ?: return@withContext DigitRecognitionResult.Unrecognized
            val confidence = best.score ?: 0f
            val latency = (SystemClock.elapsedRealtime() - started).toInt().coerceAtLeast(0)

            DigitRecognitionResult.Recognized(
                digit = digit,
                confidence = confidence,
                latencyMs = latency
            )
        } catch (ce: CancellationException) {
            // Cooperate with structured concurrency — a new stroke that cancels the
            // outer scope must propagate, not be swallowed as Error.
            throw ce
        } catch (t: Throwable) {
            DigitRecognitionResult.Error(t.message ?: "ML Kit recognition failure")
        }
    }

    /**
     * Build an [Ink] from a single contiguous stroke. Multi-stroke recognition (4, 5,
     * 7) is handled upstream by [StylusInputManager] which buffers points across the
     * debounce window into one stroke list — see `stylusDebounceMs` in `AppSettings`.
     *
     * Coordinate scale: ML Kit expects pixel-space coordinates. Cell-local pixel
     * coordinates (what `StylusPoint.x/y` carry) work because the recognizer is scale-
     * tolerant via internal normalization.
     */
    private fun buildInk(stroke: List<StylusPoint>): Ink {
        val builder = Ink.Stroke.builder()
        stroke.forEach { p -> builder.addPoint(Ink.Point.create(p.x, p.y, p.tMs)) }
        return Ink.builder().addStroke(builder.build()).build()
    }

    companion object {
        /** ML Kit Digital Ink language tag for the bundled English text base model. */
        const val EN_TAG: String = "en"

        /**
         * Sudoku-legal digit set: 1..9 (zero excluded — TC-AS8b in test-scenarios).
         * Order is irrelevant; we compare via `Set.contains`.
         */
        @JvmField
        val DIGIT_TEXTS: Set<String> = setOf("1", "2", "3", "4", "5", "6", "7", "8", "9")
    }
}
