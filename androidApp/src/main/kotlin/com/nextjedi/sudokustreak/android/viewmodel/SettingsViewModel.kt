package com.nextjedi.sudokustreak.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextjedi.sudokustreak.android.analytics.AnalyticsService
import com.nextjedi.sudokustreak.android.stats.StatsResetter
import com.nextjedi.sudokustreak.domain.settings.AppSettings
import com.nextjedi.sudokustreak.domain.settings.AppSettingsRepository
import com.nextjedi.sudokustreak.domain.settings.ColorBlindMode
import com.nextjedi.sudokustreak.domain.settings.StylusMode
import com.nextjedi.sudokustreak.domain.settings.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Brain-Gym settings screen.
 *
 * ### Why this replaces the prior [SettingsViewModel]
 *
 * The original VM exposed a hand-rolled [SettingsState] data class with seven fields
 * and persisted via `DataStore<Preferences>` using `booleanPreferencesKey(...)` /
 * `intPreferencesKey(...)`. Wave 1 of the Brain-Gym revamp moved the source of truth
 * into the `:domain` module as a 27-field [AppSettings] data class with a typed
 * JSON repository ([AppSettingsRepository]) and a forward-compatible
 * [com.nextjedi.sudokustreak.domain.settings.AppSettingsMigrator]. This VM now:
 *
 * 1. Reads the full [AppSettings] flow — no more lossy projection.
 * 2. Writes through [AppSettingsRepository.update] — atomic, single-writer, and
 *    cross-platform (iOS + web mirror the same envelope).
 * 3. Side-effects analytics + stats reset alongside settings flips where the
 *    `AppSettings` field changes alone aren't enough (e.g. flipping `analyticsOptIn`
 *    needs to either start or stop the PostHog SDK, not just persist a bit).
 *
 * ### Public surface used by other screens
 *
 * - `GameScreen` reads `settings.showTimer`, `settings.highlightEnabled`, and
 *   `settings.solverSpeedMs`, and calls `settingsViewModel.setSolverSpeed(ms)`. The
 *   field renames are documented in the build report — GameScreen was updated to
 *   consume the new field names in the same commit.
 *
 * ### Constructor injection
 *
 * - [repository]: the typed settings store. Production wiring uses
 *   [com.nextjedi.sudokustreak.android.storage.SettingsModule.appSettingsRepository].
 * - [statsResetter]: clears the legacy `DataStore<Preferences>` keys (games-played,
 *   best-times, …). Decoupled because stats stay in Preferences (locked decision §6 (3)).
 * - [analyticsService]: PostHog facade. We never construct it eagerly here — the VM
 *   only calls [AnalyticsService.initialize] when the user opts in, and
 *   [AnalyticsService.shutdown] when they opt out or run Delete All My Data.
 */
class SettingsViewModel(
    private val repository: AppSettingsRepository,
    private val statsResetter: StatsResetter,
    private val analyticsService: AnalyticsService,
) : ViewModel() {

    /**
     * Hot snapshot of [AppSettings] for the UI. Started lazily with a 5-second timeout
     * — when no Compose subscribers are around, the upstream Flow is paused and the
     * DataStore Flow stops emitting. This avoids holding a DataStore reader during
     * background ProcessLifecycleOwner.STOPPED states.
     */
    val settings: StateFlow<AppSettings> = repository.flow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    // ---- Generic helpers ----

    /**
     * Apply [transform] atomically through the repository.
     *
     * The transform may run more than once under contention — keep it pure. All the
     * named setters below funnel through this helper so the testing surface is one
     * function instead of N.
     */
    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repository.update(transform) }
    }

    // ---- Gameplay ----

    fun setMistakeLimit(limit: Int) = update { it.copy(mistakeLimit = limit) }
    fun setAutoNotes(enabled: Boolean) = update { it.copy(autoNotesEnabled = enabled) }
    fun setFastPencil(enabled: Boolean) = update { it.copy(fastPencilEnabled = enabled) }
    fun setNumberFirstMode(enabled: Boolean) = update { it.copy(numberFirstModeEnabled = enabled) }
    fun setMidGameBoost(enabled: Boolean) = update { it.copy(midGameBoostEnabled = enabled) }

    // ---- Stylus ----

    fun setStylusMode(mode: StylusMode) = update { it.copy(stylusMode = mode) }
    fun setStylusConfidence(value: Float) = update {
        // Clamp to the 0.6f..0.9f range exposed by the slider per REVAMP_PLAN.md §4.
        it.copy(stylusConfidenceThreshold = value.coerceIn(0.6f, 0.9f))
    }
    fun setStylusPressureBold(enabled: Boolean) = update { it.copy(stylusPressureToBoldNotes = enabled) }
    fun setStylusWristRejection(enabled: Boolean) = update { it.copy(stylusWristRejection = enabled) }
    fun setStylusDebounceMs(ms: Int) = update {
        // Clamp to the 250..400ms range from the spec (TC-A15 / `stylusDebounceSliderInRange`).
        it.copy(stylusDebounceMs = ms.coerceIn(250, 400))
    }

    /**
     * Called by the stylus auto-detect hook when a `TOOL_TYPE_STYLUS` MotionEvent is
     * observed anywhere in the app for the first time (see REVAMP_PLAN.md §4
     * "Auto-detect flow"). Idempotent; only writes when the flag is currently false.
     */
    fun markStylusDetected() = update {
        if (it.stylusAutoDetected) it else it.copy(stylusAutoDetected = true)
    }

    // ---- Sensors ----

    fun setProximityAutoPause(enabled: Boolean) = update { it.copy(proximityAutoPauseEnabled = enabled) }
    fun setAmbientLightAutoTheme(enabled: Boolean) = update { it.copy(ambientLightAutoThemeEnabled = enabled) }
    fun setTiltParallax(enabled: Boolean) = update { it.copy(tiltParallaxEnabled = enabled) }

    // ---- Presentation ----

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    fun setUseDynamicColor(enabled: Boolean) = update {
        // Defensive — even if the UI gates the toggle when colour-blind mode is set,
        // a stale tap shouldn't bring dynamic colour back. Re-apply the colour-blind
        // exclusion here.
        if (enabled && it.colorBlindMode != ColorBlindMode.NONE) it
        else it.copy(useDynamicColor = enabled)
    }

    fun setColorBlindMode(mode: ColorBlindMode) = update {
        // Force-disable dynamic colour when a colour-blind palette is selected; the OS
        // dynamic palette cannot guarantee the luminance separation our overlay relies
        // on. See test-plan/00-SYNTHESIS.md and REVAMP_PLAN.md §4.
        val dyn = if (mode != ColorBlindMode.NONE) false else it.useDynamicColor
        it.copy(colorBlindMode = mode, useDynamicColor = dyn)
    }

    fun setHighlight(enabled: Boolean) = update { it.copy(highlightEnabled = enabled) }
    fun setShowRemainingCount(enabled: Boolean) = update { it.copy(showRemainingCount = enabled) }
    fun setShowTimer(enabled: Boolean) = update { it.copy(showTimer = enabled) }
    fun setAnimatedDigits(enabled: Boolean) = update { it.copy(animatedDigits = enabled) }

    // ---- Solver & Hints ----

    fun setHintDepth(depth: Int) = update { it.copy(hintDepth = depth.coerceIn(1, 5)) }
    fun setSolverSpeed(ms: Int) = update { it.copy(solverSpeedMs = ms) }
    fun setSolverHelpCells(count: Int) = update { it.copy(solverHelpCells = count.coerceIn(1, 9)) }

    // ---- Accessibility ----

    fun setReduceMotion(enabled: Boolean) = update { it.copy(reduceMotion = enabled) }
    fun setHighContrast(enabled: Boolean) = update { it.copy(highContrast = enabled) }
    fun setLargeText(enabled: Boolean) = update { it.copy(largeText = enabled) }

    // ---- Audio / Haptics ----

    fun setSound(enabled: Boolean) = update { it.copy(soundEnabled = enabled) }
    fun setHaptics(enabled: Boolean) = update { it.copy(hapticsEnabled = enabled) }
    fun setMusic(enabled: Boolean) = update { it.copy(musicEnabled = enabled) }

    // ---- Data & Privacy ----

    /**
     * Flip the analytics opt-in *and* spin the PostHog SDK up or down accordingly.
     *
     * Order matters:
     * - When opting **in**, persist first, then initialise. If init throws (e.g. no
     *   network), the bit is already persisted and the next app launch will retry.
     * - When opting **out**, persist first, then shutdown. Shutting down before
     *   persisting would race against any in-flight event the SDK is mid-flush.
     */
    fun setAnalyticsOptIn(enabled: Boolean) {
        viewModelScope.launch {
            repository.update { it.copy(analyticsOptIn = enabled) }
            if (enabled) analyticsService.initialize() else analyticsService.shutdown()
        }
    }

    /**
     * **GDPR Art. 17 / CCPA §1798.105 — Right to Erasure.** Reset every persisted
     * value to factory defaults across all stores:
     *
     * 1. Settings DataStore — typed `AppSettings()` defaults via [AppSettingsRepository.reset].
     * 2. Stats DataStore — legacy `Preferences` keys via [StatsResetter.reset].
     * 3. Analytics SDK — fully shut down to purge any in-memory state + queued events.
     *
     * Saved games + replays are wiped by their respective owners. As of Wave 2 those
     * still live in-memory in `GameViewModel` and reset implicitly on process death,
     * so no extra call is needed here. When persistent saves land (Wave 3) this method
     * MUST be extended to clear that store too.
     */
    fun deleteAllMyData() {
        viewModelScope.launch {
            repository.reset()
            statsResetter.reset()
            analyticsService.shutdown()
        }
    }

    // ---- Legacy aliases — preserved for older call sites ----

    /**
     * Pre-Wave-2 [GameScreen] / [StatsScreen] callers may still invoke `toggleSound()`
     * / `toggleHighlight()` / `toggleTimer()`. Keeping these as thin shims avoids a
     * cross-file rename in this commit; remove them once every consumer migrates to
     * the typed setters above.
     */
    @Deprecated(
        message = "Use setSound(!settings.value.soundEnabled) — kept temporarily for legacy callers.",
        replaceWith = ReplaceWith("setSound(!settings.value.soundEnabled)"),
    )
    fun toggleSound() = setSound(!settings.value.soundEnabled)

    @Deprecated(
        message = "Use setHighlight(!settings.value.highlightEnabled) — kept temporarily for legacy callers.",
        replaceWith = ReplaceWith("setHighlight(!settings.value.highlightEnabled)"),
    )
    fun toggleHighlight() = setHighlight(!settings.value.highlightEnabled)

    @Deprecated(
        message = "Use setShowTimer(!settings.value.showTimer) — kept temporarily for legacy callers.",
        replaceWith = ReplaceWith("setShowTimer(!settings.value.showTimer)"),
    )
    fun toggleTimer() = setShowTimer(!settings.value.showTimer)
}
