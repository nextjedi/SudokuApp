package com.nextjedi.sudokustreak.domain.input

import kotlinx.coroutines.flow.Flow

/**
 * Cross-platform stylus input pipeline.
 *
 * Lifecycle (per platform `actual`):
 * - Android: bound to a Compose `Modifier.pointerInput(stylusMode, currentCell) { … }`
 *   that filters `PointerType.Stylus`. Finger touches do not flow through here — they
 *   pass through to the default tap handler. Recognition uses a custom TFLite digit
 *   classifier (locked decision in test-plan/00-SYNTHESIS.md §6 (2)).
 * - iOS: wraps a `PKCanvasView` with `drawingPolicy = .pencilOnly` so finger
 *   pass-through is automatic. Recognition uses Core ML MNIST primary
 *   (28×28 grayscale, ~5ms on A15) with Vision OCR as a weak sanity-check fallback.
 *   See test-plan/00-SYNTHESIS.md §1 PC-8/PC-9 for the corrected pipeline order.
 *
 * ## Contract
 *
 * - [events] is a hot Flow. Subscribers see only events that occur after subscription.
 * - [recognize] is invoked once per stroke session on `StrokeEnd`. It MUST NOT be
 *   called from inside the [events] subscription handler — back-pressure would
 *   stall the pen-trail rendering pipeline.
 * - Confidence threshold filtering happens in the actual; results below
 *   `AppSettings.stylusConfidenceThreshold` come back as [DigitRecognitionResult.Unrecognized].
 *
 * TODO actual: implement in Wave 2 (Android + iOS) — see REVAMP_PLAN.md §8 and §9.
 */
expect class StylusInputManager {
    /**
     * Hot flow of stylus events. Begins emitting after the actual is bound to the
     * surface (e.g., `Modifier.pointerInput` on Android, `PKCanvasView` on iOS).
     */
    fun events(): Flow<StylusEvent>

    /**
     * Recognize a completed stroke (or set of strokes, for multi-stroke digits like
     * 4, 5, 7 with crossbars). Suspends until the on-device classifier completes;
     * P95 latency target < 120ms.
     *
     * @param stroke the collected points from `StrokeBegin` through `StrokeEnd`.
     *               Coordinates are in cell-local space (0..cellSize).
     */
    suspend fun recognize(stroke: List<StylusPoint>): DigitRecognitionResult
}

/**
 * Stylus pointer events emitted by [StylusInputManager.events].
 *
 * - [StrokeBegin]: stylus tip first contacted the surface.
 * - [StrokeMove]: a batch of points received between begins and ends. Multiple
 *   `StrokeMove`s per stroke are normal — historical points are batched into one
 *   emission per Android `MotionEvent` / iOS `UITouch` callback.
 * - [StrokeEnd]: stylus tip lifted. Recognition is fired against the accumulated
 *   points by the caller.
 * - [HoverEnter] / [HoverExit]: stylus is near the surface but not touching it.
 *   Used to render a ghost-digit preview on supported devices (S-Pen, Apple Pencil 2).
 * - [SideButtonPressed]: stylus side button click — S-Pen `KEYCODE_STYLUS_BUTTON_PRIMARY`
 *   on Android, `UIPencilInteraction` squeeze on iOS. Used to toggle pencil-notes mode
 *   (Apple Pencil squeeze parity, resolves P0-12 / P1-24 in test-plan/00-SYNTHESIS.md).
 */
sealed class StylusEvent {
    data class StrokeBegin(val point: StylusPoint) : StylusEvent()
    data class StrokeMove(val points: List<StylusPoint>) : StylusEvent()
    data class StrokeEnd(val points: List<StylusPoint>) : StylusEvent()
    data object HoverEnter : StylusEvent()
    data object HoverExit : StylusEvent()
    data object SideButtonPressed : StylusEvent()
}

/**
 * A single stylus sample.
 *
 * @property x cell-local X in pixels.
 * @property y cell-local Y in pixels.
 * @property pressure normalized [0f..1f]. Above 0.6 typically renders bold notes
 *                    when [com.nextjedi.sudokustreak.domain.settings.AppSettings.stylusPressureToBoldNotes]
 *                    is true.
 * @property tMs monotonic-ish event timestamp in milliseconds. Used for stroke
 *               debouncing and latency benchmarks.
 */
data class StylusPoint(
    val x: Float,
    val y: Float,
    val pressure: Float,
    val tMs: Long
)

/**
 * Outcome of a digit-recognition pass.
 *
 * - [Recognized]: digit 1–9 with the model's confidence and per-call latency.
 *   The caller is responsible for placing the digit in the selected cell and
 *   firing the success haptic.
 * - [Unrecognized]: model output was below threshold, was not a digit (e.g., a
 *   scribble), or was a digit outside 1–9. UI fades the trail and optionally
 *   shakes the cell.
 * - [Error]: model failed to load, ran out of memory, or another runtime fault.
 *   UI should silently fall back to manual input — never blame the user.
 */
sealed class DigitRecognitionResult {
    data class Recognized(
        val digit: Int,
        val confidence: Float,
        val latencyMs: Int
    ) : DigitRecognitionResult()

    data object Unrecognized : DigitRecognitionResult()

    data class Error(val reason: String) : DigitRecognitionResult()
}
