# mistake_tone.ogg — placeholder

This directory is the Android `res/raw/` location where the mistake tone
audio file `mistake_tone.ogg` is expected to live.

## What this file is

The mistake tone is a short (~120 ms) downward two-note motif played when the
user places a wrong digit. It is the *audio* leg of the multi-modal mistake
feedback contract specified in `REVAMP_PLAN.md` §6 + this phase's a11y
deliverables:

1. Visual — red cell fill + dashed border (already implemented).
2. Haptic — `HapticFeedbackConstants.REJECT` (see `tryHapticReject` in
   `androidApp/src/main/kotlin/com/nextjedi/sudokustreak/android/a11y/AccessibilityHelpers.kt`).
3. Audio — this file, gated by `AppSettings.soundEnabled`.
4. Screen-reader announcement — `announceForAccessibility(...)` bypasses
   `soundEnabled` because it is semantic content, not entertainment audio.

## Why this is a markdown placeholder

I cannot ship a real `.ogg` binary from text-only edits. The asset must be
authored by the audio agent in a follow-up wave:

- Tone: two notes, F4 (350 Hz) → C4 (262 Hz), each ~60 ms
- Envelope: 5 ms attack, 100 ms decay, no sustain
- Format: Ogg Vorbis, mono, 44.1 kHz, ~12 KB total
- Loudness: target −18 LUFS so it doesn't startle headphone users

When the real file lands, it should be saved as
`androidApp/src/main/res/raw/mistake_tone.ogg` and this placeholder removed.

## Reference call site

```kotlin
LaunchedEffect(uiState.lastMistakeId) {
    val mistake = uiState.lastMistake ?: return@LaunchedEffect
    // ... visual is already done via the cell render
    tryHapticReject(view, settings.hapticsEnabled)
    if (settings.soundEnabled) {
        // MediaPlayer.create(context, R.raw.mistake_tone).start()
    }
    val msg = mistakeAnnouncement(mistake.row, mistake.col, mistake.attemptedValue)
    announceForAccessibility(view, msg)
}
```
