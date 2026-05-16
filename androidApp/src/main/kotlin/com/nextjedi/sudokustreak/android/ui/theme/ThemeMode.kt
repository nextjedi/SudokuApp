package com.nextjedi.sudokustreak.android.ui.theme

/**
 * User-selectable theme mode. Stored in `AppSettings.themeMode` (DataStore key
 * `theme_mode`, default `SYSTEM`).
 *
 * - `SYSTEM`: honour the OS dark-mode setting via [androidx.compose.foundation.isSystemInDarkTheme].
 * - `LIGHT`: force light palette regardless of OS setting.
 * - `DARK`:  force dark palette regardless of OS setting.
 * - `AMOLED`: force the AMOLED palette — pure-black background for OLED battery savings.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED,
}

/**
 * Color-blind palette override. Stored in `AppSettings.colorBlindMode` (DataStore key
 * `color_blind_mode`, default `NONE`).
 *
 * Selecting a non-NONE mode swaps the amber + teal tokens (and their state pairs) for
 * variants with a higher luminance ratio per WCAG 1.4.11. The base scheme (primary,
 * surfaces, errors) is unchanged.
 *
 * Dynamic colour (`useDynamicColor = true`) is automatically disabled when this is set
 * to anything other than `NONE`, because the OS palette cannot guarantee the required
 * luminance separation. See [needsStaticPalette].
 */
enum class ColorBlindMode {
    /** No override — use the brand amber/teal pair. */
    NONE,

    /**
     * Deuteranopia (red-green CB, most common). Replaces amber → deep #E65100,
     * teal → sky #4FC3F7. See `Color.kt` for luminance math.
     */
    DEUTERANOPIA,

    /**
     * Protanopia (red-blind). Replaces amber → pumpkin #FF8F00, teal → deep blue
     * #1976D2.
     */
    PROTANOPIA;

    /**
     * Whether selecting this mode requires the app to use the static brand palette
     * (i.e. NOT Android 12+ dynamic colour). Always true for non-NONE modes.
     */
    fun needsStaticPalette(): Boolean = this != NONE
}
