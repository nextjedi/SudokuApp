package com.nextjedi.sudokustreak.domain.haptics

/**
 * Cross-platform haptic feedback.
 *
 * All methods respect `AppSettings.hapticsEnabled` at the call site — the actual is
 * a thin platform wrapper and does not consult settings on its own. ViewModels MUST
 * gate calls behind the setting before invocation.
 *
 * Platform mappings:
 * - Android: `Vibrator.vibrate(VibrationEffect.createPredefined(…))` on API 29+,
 *   `Vibrator.vibrate(long)` fallback on older.
 * - iOS: `UIImpactFeedbackGenerator` (.light/.medium) for [tap], `UINotificationFeedbackGenerator`
 *   for [success]/[warning]/[error].
 * - Web: no-op (browsers don't expose haptics consistently).
 *
 * TODO actual: implement in Wave 2 — see REVAMP_PLAN.md §1 + §6.
 */
expect class PlatformHaptics {
    /** Light tap. Used on number-pad press, cell selection. */
    fun tap()

    /** Success cue. Used on puzzle completion, recognized digit placement. */
    fun success()

    /** Warning cue. Used on mistake (paired with audio + announceForAccessibility per P0-10). */
    fun warning()

    /** Error cue. Used on solver failure or unrecognized stroke. */
    fun error()
}
