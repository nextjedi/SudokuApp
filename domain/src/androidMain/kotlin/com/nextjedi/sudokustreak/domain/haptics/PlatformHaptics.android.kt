package com.nextjedi.sudokustreak.domain.haptics

/**
 * Android actual for [PlatformHaptics].
 *
 * TODO actual: implement in Wave 2.
 *
 * Wave 2 outline:
 * - Inject `Vibrator` / `VibratorManager` (API 31+ uses `VibratorManager.getDefaultVibrator()`).
 * - Use `VibrationEffect.createPredefined(EFFECT_TICK / EFFECT_CLICK)` on API 29+.
 * - Fallback to `Vibrator.vibrate(long)` on older devices.
 * - Honor `AppSettings.hapticsEnabled` at the ViewModel call site (NOT here).
 */
actual class PlatformHaptics {
    actual fun tap() {
        // TODO actual: VibrationEffect.EFFECT_TICK in Wave 2.
    }

    actual fun success() {
        // TODO actual: VibrationEffect.createWaveform success pattern in Wave 2.
    }

    actual fun warning() {
        // TODO actual: VibrationEffect.EFFECT_DOUBLE_CLICK in Wave 2.
    }

    actual fun error() {
        // TODO actual: VibrationEffect.createWaveform error pattern in Wave 2.
    }
}
