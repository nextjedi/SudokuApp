package com.nextjedi.sudokustreak.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Master Sudoku theme for the Android app. Replaces the legacy single-light
 * `SudokuTheme` which imported [isSystemInDarkTheme] but never consumed it.
 *
 * @param themeMode one of [ThemeMode.SYSTEM] / [ThemeMode.LIGHT] / [ThemeMode.DARK] /
 *   [ThemeMode.AMOLED]. Defaults to SYSTEM.
 * @param colorBlindMode optional palette override for deuteranopia / protanopia.
 *   When set, dynamic colour is forced off so luminance separation is guaranteed.
 * @param useDynamicColor opt-IN flag for Android 12+ dynamic (Material You) colour.
 *   Default `false` per spec — turn on only when the user explicitly enables it AND
 *   colorBlindMode is NONE.
 * @param reduceMotion when true, callers should avoid expressive motion specs. This
 *   theme propagates the flag via [LocalReduceMotion]; consume from animation specs.
 *
 * ## Note on `MaterialExpressiveTheme` / `MotionScheme.expressive()`
 *
 * The M3E review (`test-plan/04-android-m3e-review.md` §2) recommended adopting
 * `MaterialExpressiveTheme` + `MotionScheme.expressive()` from M3 1.4.0 stable.
 * **Verification on 2026-05-16 against the actual `material3-android-1.4.0.aar` shows
 * both APIs are still `internal` in 1.4.0** — the Kotlin metadata marks
 * `MaterialExpressiveTheme`, `MotionScheme`, and `ExperimentalMaterial3ExpressiveApi`
 * as not-yet-public. They graduate to public in 1.5.0-alpha.
 *
 * Decision: ship with the standard `MaterialTheme(...)` on 1.4.0 stable + propagate
 * the [LocalReduceMotion] CompositionLocal so individual call sites can opt into
 * standard `tween` / `spring` animations and respect reduce-motion. Re-evaluate
 * when M3 1.5.0 stable lands.
 */
@Composable
fun SudokuTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorBlindMode: ColorBlindMode = ColorBlindMode.NONE,
    useDynamicColor: Boolean = false,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AMOLED -> true
    }

    // Color-blind palettes always need a static scheme; dynamic colour is opt-in.
    val effectiveDynamic = useDynamicColor &&
        !colorBlindMode.needsStaticPalette() &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val baseScheme: ColorScheme = remember(themeMode, isDark, effectiveDynamic, context) {
        when {
            themeMode == ThemeMode.AMOLED -> AmoledColors
            effectiveDynamic -> if (isDark) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
            isDark -> DarkColors
            else -> LightColors
        }
    }

    val colorScheme: ColorScheme = remember(baseScheme, colorBlindMode) {
        applyColorBlindOverlay(baseScheme, colorBlindMode)
    }

    CompositionLocalProvider(
        LocalSudokuColors provides SudokuExtendedColors.from(baseScheme, colorBlindMode),
        LocalReduceTransparency provides false, // default; can be flipped from AppSettings.
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography,
            shapes = MaterialTheme.shapes,
            content = content,
        )
    }
}

// ---------- Built-in colour schemes ----------

internal val LightColors: ColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryContainerLight,
    onPrimaryContainer = BrandOnPrimaryContainerLight,
    secondary = BrandTeal,
    onSecondary = BrandNavyText,
    tertiary = BrandAmber,
    onTertiary = BrandNavyText,
    background = BrandBackgroundLight,
    onBackground = BrandOnSurfaceLight,
    surface = BrandSurfaceLight,
    onSurface = BrandOnSurfaceLight,
    surfaceContainer = BrandSurfaceContainerLight,
    surfaceContainerHigh = BrandSurfaceContainerHighLight,
    error = BrandErrorRed,
    onError = Color.White,
)

internal val DarkColors: ColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryContainerDark,
    onPrimaryContainer = BrandOnPrimaryContainerDark,
    secondary = BrandTeal,
    onSecondary = BrandNavyText,
    tertiary = BrandAmber,
    onTertiary = BrandNavyText,
    background = BrandBackgroundDark,
    onBackground = BrandOnSurfaceDark,
    surface = BrandSurfaceDark,
    onSurface = BrandOnSurfaceDark,
    surfaceContainer = BrandSurfaceContainerDark,
    surfaceContainerHigh = BrandSurfaceContainerHighDark,
    error = BrandErrorRed,
    onError = Color.White,
)

/**
 * AMOLED is DarkColors with pure-black background / surface tokens — saves OLED battery
 * (each black pixel turns the OLED off entirely).
 */
internal val AmoledColors: ColorScheme = DarkColors.copy(
    background = BrandSurfaceAmoled,
    surface = BrandSurfaceAmoled,
    surfaceContainer = BrandSurfaceAmoledContainer,
    surfaceContainerHigh = BrandSurfaceAmoledContainerHigh,
)

// ---------- Color-blind overlay ----------

/**
 * Apply a color-blind palette override on top of an arbitrary base scheme. Only the
 * tertiary (amber slot) and secondary (teal slot) are swapped; primary / surface /
 * error remain unchanged.
 */
internal fun applyColorBlindOverlay(
    base: ColorScheme,
    mode: ColorBlindMode,
): ColorScheme = when (mode) {
    ColorBlindMode.NONE -> base
    ColorBlindMode.DEUTERANOPIA -> base.copy(
        tertiary = BrandAmberDeuteranopia,
        secondary = BrandTealDeuteranopia,
    )
    ColorBlindMode.PROTANOPIA -> base.copy(
        tertiary = BrandAmberProtanopia,
        secondary = BrandTealProtanopia,
    )
}

// ---------- Extended colour CompositionLocal ----------

/**
 * Sudoku-specific colours that don't map cleanly to the standard M3 `ColorScheme`
 * roles. Consume via [LocalSudokuColors] inside any composable that needs solver-state
 * or notes colours.
 *
 *   val ext = LocalSudokuColors.current
 *   Box(Modifier.background(ext.solverHintAmber))
 */
data class SudokuExtendedColors(
    val solverFillingYellow: Color,
    val solverHintAmber: Color,
    val solverHintedYellow: Color,
    val solverFilledTeal: Color,
    val notesGray: Color,
    val streakOrange: Color,
    val streakOrangeLight: Color,
    val borderFaint: Color,
    val borderMedium: Color,
) {
    companion object {
        /**
         * Build an extended-colour bundle for the supplied base scheme + color-blind
         * mode. The base scheme is currently unused for the extended slots (they're
         * brand tokens that look the same in light/dark) but the param keeps the
         * door open for future light/dark variants.
         */
        @Suppress("UNUSED_PARAMETER")
        fun from(base: ColorScheme, mode: ColorBlindMode): SudokuExtendedColors {
            val (hintAmber, filledTeal) = when (mode) {
                ColorBlindMode.NONE -> BrandSolverHintAmber to BrandSolverFilledTeal
                ColorBlindMode.DEUTERANOPIA ->
                    BrandSolverHintAmberDeuteranopia to BrandSolverFilledTealDeuteranopia
                ColorBlindMode.PROTANOPIA ->
                    BrandSolverHintAmberProtanopia to BrandSolverFilledTealProtanopia
            }
            return SudokuExtendedColors(
                solverFillingYellow = BrandSolverFillingYellow,
                solverHintAmber = hintAmber,
                solverHintedYellow = BrandSolverHintedYellow,
                solverFilledTeal = filledTeal,
                notesGray = BrandNotesGray,
                streakOrange = BrandStreakOrange,
                streakOrangeLight = BrandStreakOrangeLight,
                borderFaint = BrandBorderLightFaint,
                borderMedium = BrandBorderLightMedium,
            )
        }
    }
}

/**
 * CompositionLocal exposing [SudokuExtendedColors]. Provided by [SudokuTheme]; reads
 * before that error out at call time with a clear message.
 */
val LocalSudokuColors = staticCompositionLocalOf<SudokuExtendedColors> {
    error("LocalSudokuColors not provided — wrap your content in SudokuTheme {}")
}

/**
 * Accessibility opt-in: when true, [glassyTint] never applies a BlurEffect even on
 * API 31+. Used by users who report motion / transparency sensitivity (Lena's audit,
 * TC-A11Y-5).
 *
 * Defaults to false; flip from AppSettings.reduceTransparency.
 */
val LocalReduceTransparency = compositionLocalOf { false }

/**
 * Accessibility opt-in: when true, callers should skip expressive motion specs and
 * prefer near-instant animations (`tween(50)` or `snap()`). Honour
 * `Settings.Global.TRANSITION_ANIMATION_SCALE == 0` at the AppSettings layer.
 *
 * Defaults to false.
 */
val LocalReduceMotion = compositionLocalOf { false }
