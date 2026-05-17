package com.nextjedi.sudokustreak.domain.input

/**
 * Android digit-recognition contract. Sits between [StylusInputManager] and the
 * concrete recognizer ([MlKitDigitRecognizer] in production) so unit tests can
 * substitute a deterministic fake without booting ML Kit.
 *
 * ## Contract
 *
 * - Must be safe to call from any coroutine; production implementation runs the
 *   actual model on [kotlinx.coroutines.Dispatchers.Default] (see PC-6 §6 of
 *   `test-plan/06-stylus-sensors-privacy-review.md` — never on Main).
 * - Must NOT throw. Any internal failure (model load, OOM, cancellation cleanup)
 *   must be returned as [DigitRecognitionResult.Error] so the UI can silently
 *   fall back to manual input — never blame the user (REVAMP_PLAN.md §8 UX rule).
 * - Must filter candidates to digits 1..9 only. Zero is rejected because Sudoku
 *   only places 1..9 (TC-AS8b). Letters/symbols rejected (TC-AS8).
 * - Must apply the confidence threshold AFTER filtering to digits, so a low-scoring
 *   "5" does not let a high-scoring "A" through (PC-6 in the synthesis doc).
 *
 * The interface exists in `androidMain` (not `commonMain`) on purpose: ML Kit is
 * Android-specific, and the iOS pipeline uses Core ML + Vision with a different
 * contract (see `StylusInputManager.ios.kt` Wave-2 outline).
 */
interface DigitRecognizer {
    /**
     * Recognize a single completed stroke (or multi-stroke session — caller decides
     * when to commit by buffering across debounce windows). Returns within ~120ms
     * P95 on Pixel 6 with the bundled `en` model.
     *
     * @param stroke ordered list of points from [StylusEvent.StrokeBegin] through
     *               [StylusEvent.StrokeEnd]. Coordinates are in cell-local space.
     * @param confidenceThreshold minimum score for a candidate to count as
     *                            [DigitRecognitionResult.Recognized]; below this,
     *                            [DigitRecognitionResult.Unrecognized] is returned.
     */
    suspend fun recognize(
        stroke: List<StylusPoint>,
        confidenceThreshold: Float
    ): DigitRecognitionResult
}
