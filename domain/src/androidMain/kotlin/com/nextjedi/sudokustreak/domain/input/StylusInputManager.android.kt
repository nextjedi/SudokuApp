package com.nextjedi.sudokustreak.domain.input

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Android actual for [StylusInputManager].
 *
 * TODO actual: implement in Wave 2.
 *
 * Wave 2 outline:
 * - Bind to a Compose `Modifier.pointerInput(stylusMode, currentCell) { … }` filtering
 *   `PointerType.Stylus`. Finger touches must pass through to the default tap handler.
 * - Capture historical points via `MotionEvent.getHistoricalX/Y/Pressure` for accurate
 *   stroke trails (see TC-AS3 in test-plan/01-test-scenarios.md).
 * - Recognizer: custom TFLite digit classifier (locked decision §6 (2)) — NOT ML Kit
 *   Digital Ink with the bogus `zxx-Zsym-x-ipa` tag (see PC-6).
 * - Hover events: `ACTION_HOVER_ENTER/EXIT` → ghost-digit preview.
 * - S-Pen side button: `KEYCODE_STYLUS_BUTTON_PRIMARY` (P1-24).
 */
actual class StylusInputManager {
    actual fun events(): Flow<StylusEvent> {
        // TODO actual: emit real MotionEvent-driven stylus events in Wave 2.
        return emptyFlow()
    }

    actual suspend fun recognize(stroke: List<StylusPoint>): DigitRecognitionResult {
        // TODO actual: invoke TFLite digit classifier in Wave 2.
        return DigitRecognitionResult.Unrecognized
    }
}
