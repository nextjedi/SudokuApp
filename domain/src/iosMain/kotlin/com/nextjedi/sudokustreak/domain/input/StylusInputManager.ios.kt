package com.nextjedi.sudokustreak.domain.input

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * iOS actual for [StylusInputManager].
 *
 * TODO actual: implement in Wave 2.
 *
 * Wave 2 outline (per REVAMP_PLAN.md §9 + corrections PC-8/PC-9):
 * - Swift-side `PKCanvasView` with `drawingPolicy = .pencilOnly`. Kotlin/Native
 *   side wraps a thin Obj-C bridge that produces `StylusEvent`s.
 * - Recognition pipeline (corrected order from synthesis):
 *   1. Core ML MNIST primary (28×28 grayscale, 20×20 centered + 4px padding). ~5ms on A15.
 *   2. Vision `VNRecognizeTextRequest` with `customWords = ["1"..."9"]` as weak fallback.
 *   3. Both fail → Unrecognized.
 * - `UIPencilInteraction` delegate handles squeeze (Pencil Pro) + double-tap (Pencil 2),
 *   respecting `preferredTapAction` (P1-12).
 */
actual class StylusInputManager {
    actual fun events(): Flow<StylusEvent> {
        // TODO actual: bridge PKCanvasView events in Wave 2.
        return emptyFlow()
    }

    actual suspend fun recognize(stroke: List<StylusPoint>): DigitRecognitionResult {
        // TODO actual: Core ML MNIST + Vision fallback in Wave 2.
        return DigitRecognitionResult.Unrecognized
    }
}
