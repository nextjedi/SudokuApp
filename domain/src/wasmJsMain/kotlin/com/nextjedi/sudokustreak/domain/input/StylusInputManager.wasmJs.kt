package com.nextjedi.sudokustreak.domain.input

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Web (wasmJs) actual for [StylusInputManager].
 *
 * TODO actual: implement in Wave 2 (if web stylus support is in scope; otherwise
 * permanently a no-op since most desktop users have a mouse).
 *
 * Future option: use the Pointer Events API to read `pointerType === 'pen'` and
 * the Web Handwriting Recognition API (Chromium-only as of 2026).
 */
actual class StylusInputManager {
    actual fun events(): Flow<StylusEvent> = emptyFlow()
    actual suspend fun recognize(stroke: List<StylusPoint>): DigitRecognitionResult =
        DigitRecognitionResult.Unrecognized
}
