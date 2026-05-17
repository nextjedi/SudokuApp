package com.nextjedi.sudokustreak.domain.settings

import kotlinx.serialization.Serializable

/**
 * Single source of truth for all user-configurable application settings.
 *
 * Persisted by platform-specific implementations of [AppSettingsRepository]:
 * - Android: `DataStore<AppSettings>` JSON-serialized to `settings.json` (separate file from stats).
 * - iOS: `UserDefaults.suite("brain-gym")` JSON blob keyed at `app_settings_v1`.
 * - Web: `localStorage["app_settings"]`.
 *
 * Stats keys (`KEY_GAMES_PLAYED`, `KEY_BEST_*`, …) stay in `DataStore<Preferences>` per
 * the locked decision in test-plan/00-SYNTHESIS.md §6 (3): stats remain in Preferences
 * while settings move to JSON.
 *
 * ## Schema versioning
 *
 * [schemaVersion] is the FIRST field on purpose — see TC-S5c and the `serialized_first_key`
 * unit test. The Migrator dispatches on this field; an unknown future version falls back
 * to defaults rather than crashing on `MissingFieldException`.
 *
 * ## Defaults policy
 *
 * - [midGameBoostEnabled] = false (App Store complaint history + memory rule).
 * - [tiltParallaxEnabled] = false (motion sickness risk).
 * - [stylusMode] = AUTO (stylus is never paywalled — auto-detect on first stroke).
 * - [analyticsOptIn] = false (privacy: opt-in, not opt-out).
 * - [stylusDebounceMs] = 300 (supports multi-stroke digits 4/5/7 with crossbars).
 *
 * Any new field MUST be added at the end with a default value, and the migrator updated
 * to recognize the new schemaVersion.
 */
@Serializable
data class AppSettings(
    val schemaVersion: Int = 1,                              // MUST be first field

    // ---- Gameplay (5) ----
    val mistakeLimit: Int = 3,
    val autoNotesEnabled: Boolean = false,
    val fastPencilEnabled: Boolean = false,
    val numberFirstModeEnabled: Boolean = false,
    val midGameBoostEnabled: Boolean = false,                 // OFF by default per App Store complaint

    // ---- Presentation (5) ----
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val highlightEnabled: Boolean = true,
    val showRemainingCount: Boolean = true,
    val showTimer: Boolean = true,
    val animatedDigits: Boolean = true,

    // ---- Solver / Hints (3) ----
    val hintDepth: Int = 3,
    val solverSpeedMs: Int = 1500,
    val solverHelpCells: Int = 3,

    // ---- Stylus (6) — NEVER paywalled ----
    val stylusMode: StylusMode = StylusMode.AUTO,
    val stylusAutoDetected: Boolean = false,
    val stylusConfidenceThreshold: Float = 0.75f,
    val stylusPressureToBoldNotes: Boolean = true,
    val stylusWristRejection: Boolean = true,
    val stylusDebounceMs: Int = 300,                          // configurable multi-stroke window

    // ---- Sensors (3) ----
    val proximityAutoPauseEnabled: Boolean = true,
    val ambientLightAutoThemeEnabled: Boolean = true,
    val tiltParallaxEnabled: Boolean = false,                 // OFF: motion sickness risk

    // ---- Accessibility (4) ----
    val reduceMotion: Boolean = false,
    val highContrast: Boolean = false,
    val colorBlindMode: ColorBlindMode = ColorBlindMode.NONE,
    val largeText: Boolean = false,

    // ---- Audio / Haptics (3) + Data / Privacy (1) ----
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val musicEnabled: Boolean = false,
    val analyticsOptIn: Boolean = false,                      // opt-in only (locked decision 7)

    // ---- Presentation cont. (1) — Android 12+ dynamic color ----
    // Opt-IN only. Auto-disabled (forced false) when colorBlindMode != NONE because the
    // OS palette cannot guarantee the luminance separation required by the colour-blind
    // overlay. Persisted so the user setting survives a reboot + colour-blind toggle off.
    val useDynamicColor: Boolean = false
) {
    companion object {
        /** Current schema version. Increment when adding/removing/renaming fields. */
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }
}

/** Display theme. AMOLED is a true-black variant for OLED panels (battery saving). */
@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

/**
 * Stylus activation policy.
 * - [AUTO]: stylus is active when a stylus is detected; flips [AppSettings.stylusAutoDetected] true.
 * - [ALWAYS]: assume stylus available (debug or external stylus users).
 * - [NEVER]: never invoke recognizer — finger-only.
 */
@Serializable
enum class StylusMode { AUTO, ALWAYS, NEVER }

/**
 * Color-blind safe palette swaps. Coarse industry shorthand (see audit M3).
 *
 * - [NONE]: brand amber/teal pair unchanged.
 * - [DEUTERANOPIA]: red-green colour blindness (most common form). Replaces
 *   amber → deep `#E65100`, teal → light sky `#81D4FA` for ≥ 2:1 luminance gap.
 * - [PROTANOPIA]: red-blindness. Replaces amber → pumpkin `#FF8F00`, teal →
 *   deep blue `#1976D2` for ≥ 1.5:1 luminance gap (supplemented by hue + shape).
 */
@Serializable
enum class ColorBlindMode {
    NONE,
    DEUTERANOPIA,
    PROTANOPIA;

    /**
     * Whether selecting this mode requires the app to use the static brand palette
     * (i.e. NOT Android 12+ Material You dynamic colour). Always true for non-`NONE`
     * modes because the OS palette cannot guarantee the luminance separation our
     * colour-blind overlay relies on.
     *
     * Wired in `SettingsViewModel.setUseDynamicColor` and consumed in `SudokuTheme`
     * via `effectiveDynamic = useDynamicColor && !colorBlindMode.needsStaticPalette()`.
     */
    fun needsStaticPalette(): Boolean = this != NONE
}
