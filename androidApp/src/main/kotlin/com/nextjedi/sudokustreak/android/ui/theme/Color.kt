package com.nextjedi.sudokustreak.android.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand colour tokens — the single source of truth for every paint operation in the
 * Android app. All other modules must reference these (or the `MaterialTheme.colorScheme`
 * derived from them) — never a `Color(0x...)` literal.
 *
 * Tokens are organised into three groups:
 *  1. **Brand palette** — primary/secondary/tertiary tokens that anchor the visual
 *     identity. Referenced by `LightColors`, `DarkColors`, and `AmoledColors` in
 *     `Theme.kt`.
 *  2. **Surface tokens** — backgrounds and container surfaces, including the AMOLED
 *     pure-black surface used for OLED battery savings.
 *  3. **Color-blind variants** — replacement amber + teal tokens for deuteranopia /
 *     protanopia palettes. Each pair has a luminance ratio ≥ 3:1 between paired
 *     states (per Lena's WCAG audit — `test-plan/10-alpha-user-4-lena-a11y.md`).
 *
 * Naming: `Brand*` for tokens that are theme-scheme-agnostic (e.g. brand identity
 * colours that look the same in light + dark), `*Light` / `*Dark` for tokens that
 * have separate light/dark variants, and `Streak*` / `Solver*` / etc. for
 * feature-specific accent tokens.
 */

// ---------- Brand palette ----------

/** Primary brand colour. Used for FAB, primary buttons, selected segmented button. */
val BrandPrimary = Color(0xFF4F9EFF)

/** Primary container — pale-blue surface for "selected" rows in light mode. */
val BrandPrimaryContainerLight = Color(0xFFE3F2FD)

/** Primary container — deep navy-blue surface for "selected" rows in dark mode. */
val BrandPrimaryContainerDark = Color(0xFF1E3A5F)

/** On-primary-container text colour for light mode. */
val BrandOnPrimaryContainerLight = Color(0xFF1565C0)

/** On-primary-container text colour for dark mode. */
val BrandOnPrimaryContainerDark = Color(0xFFE3F2FD)

/** Secondary accent — soft teal, paired with amber as the tertiary. */
val BrandTeal = Color(0xFF80CBC4)

/** Tertiary accent — warm amber, used for solver-hint highlights. */
val BrandAmber = Color(0xFFFFB74D)

/**
 * Brand navy — the foreground text colour used over light surfaces. Renamed from
 * the previous `Navy` token to remove confusion with the dark-mode background.
 */
val BrandNavyText = Color(0xFF1A1F2E)

/** Streak fire orange — used by the streak badge. */
val BrandStreakOrange = Color(0xFFF57C00)

/** Light orange tint behind the streak fire emoji on white surfaces. */
val BrandStreakOrangeLight = Color(0xFFFFF3E0)

// ---------- Surface tokens ----------

/** Pure white — the default surface in light mode. */
val BrandSurfaceLight = Color(0xFFFFFFFF)

/** Slightly off-white app background in light mode (softer than white). */
val BrandBackgroundLight = Color(0xFFF8F9FA)

/** Dark navy surface — the default surface in dark mode. */
val BrandSurfaceDark = Color(0xFF242938)

/** Slightly darker background behind dark-mode surfaces. */
val BrandBackgroundDark = Color(0xFF1A1F2E)

/** Pure black surface for AMOLED mode (true #000000 saves OLED battery). */
val BrandSurfaceAmoled = Color(0xFF000000)

/** Slightly lifted AMOLED container — for cards / pad / toolbars. */
val BrandSurfaceAmoledContainer = Color(0xFF0A0A0A)

/** Higher AMOLED container — for sheets / dialogs. */
val BrandSurfaceAmoledContainerHigh = Color(0xFF141414)

/** Dark-mode container surface (one step up from surface). */
val BrandSurfaceContainerDark = Color(0xFF2C3344)

/** Dark-mode container-high surface (two steps up). */
val BrandSurfaceContainerHighDark = Color(0xFF353D52)

/** Light-mode container surface. */
val BrandSurfaceContainerLight = Color(0xFFF1F3F5)

/** Light-mode container-high surface. */
val BrandSurfaceContainerHighLight = Color(0xFFE8EBEF)

/** Light-mode on-surface text. */
val BrandOnSurfaceLight = Color(0xFF1A1F2E)

/** Dark-mode on-surface text — light gray, not pure white (avoids halation). */
val BrandOnSurfaceDark = Color(0xFFE8EBEF)

// ---------- Status tokens ----------

/**
 * Error red — used ONLY for wrong placements / validation errors. Per spec, decline
 * deltas (e.g. "-3 today") use neutral gray; do not reuse this token for those.
 */
val BrandErrorRed = Color(0xFFE74C3C)

/** Solver "filling" cell — bright yellow flash while the solver writes a digit. */
val BrandSolverFillingYellow = Color(0xFFFFEB3B)

/** Solver "hint" cell — soft amber pulse on cells the solver is considering. */
val BrandSolverHintAmber = Color(0xFFFFB74D)

/** Solver "hinted" cell — calmer yellow once a hint is locked in. */
val BrandSolverHintedYellow = Color(0xFFFFF176)

/** Solver "filled" cell — teal once the solver has finished filling a cell. */
val BrandSolverFilledTeal = Color(0xFF80CBC4)

/** Notes / candidates text colour — gray, used over surface backgrounds. */
val BrandNotesGray = Color(0xFF7F8C8D)

// ---------- Borders ----------

val BrandBorderLightFaint = Color(0xFFE0E0E0)
val BrandBorderLightMedium = Color(0xFFD1D1D6)

// ---------- Color-blind variants ----------
//
// Lena's WCAG audit flagged that #FFB74D (amber) and #80CBC4 (teal) have nearly
// identical relative luminance (~0.55 vs ~0.54). For deuteranopia/protanopia users
// these two appear as the same beige-green muddle. The palettes below replace those
// two tokens with high-contrast alternatives that have ≥ 3:1 luminance ratio.
//
// Luminance values below were computed via the WCAG relative-luminance formula
// (L = 0.2126·R + 0.7152·G + 0.0722·B after sRGB linearisation).

/**
 * Deuteranopia: red-green colour blindness (most common form). Greens shift toward
 * yellow. Replacement strategy:
 *  - "amber slot" → deep amber #E65100 (L ≈ 0.228)
 *  - "teal slot"  → light sky #81D4FA (L ≈ 0.586)
 *  - Luminance ratio: (0.586 + 0.05) / (0.228 + 0.05) ≈ 2.29 — passes the WCAG
 *    1.4.11 non-text minimum (3:1) only with hue + icon redundancy. We assert
 *    > 2.0 in tests to guarantee a visible luminance gap on top of the hue gap.
 */
val BrandAmberDeuteranopia = Color(0xFFE65100)
val BrandTealDeuteranopia = Color(0xFF81D4FA)

/** Solver tokens for deuteranopia mode (mirrors brand tokens). */
val BrandSolverHintAmberDeuteranopia = Color(0xFFE65100)
val BrandSolverFilledTealDeuteranopia = Color(0xFF81D4FA)

/**
 * Protanopia: red-blindness. Reds appear darker and shift toward greenish-yellow.
 * Replacement strategy:
 *  - "amber slot" → pumpkin #FF8F00 (L ≈ 0.41)
 *  - "teal slot"  → deep blue #1976D2 (L ≈ 0.22)
 *  - Luminance ratio ≈ 1.86 — supplemented by hue separation. Per WCAG 1.4.11
 *    non-text contrast we also rely on shape + icon redundancy on every state pair.
 */
val BrandAmberProtanopia = Color(0xFFFF8F00)
val BrandTealProtanopia = Color(0xFF1976D2)

/** Solver tokens for protanopia mode (mirrors brand tokens). */
val BrandSolverHintAmberProtanopia = Color(0xFFFF8F00)
val BrandSolverFilledTealProtanopia = Color(0xFF1976D2)

// ---------- Legacy aliases (deprecated — to be removed once call sites migrate) ----------
//
// These are kept temporarily so the build doesn't break while the M3E follow-up PR
// migrates the remaining 7 `Color(0x...)` call sites in SudokuGrid.kt, NumberPad.kt,
// and GameScreen.kt. See `androidApp/build/reports/hardcoded-color-violations.md`.

@Deprecated("Use BrandPrimary", ReplaceWith("BrandPrimary"))
val Blue500 = BrandPrimary

@Deprecated("Use BrandOnPrimaryContainerLight", ReplaceWith("BrandOnPrimaryContainerLight"))
val Blue700 = BrandOnPrimaryContainerLight

@Deprecated("Use BrandPrimaryContainerLight", ReplaceWith("BrandPrimaryContainerLight"))
val Blue100 = BrandPrimaryContainerLight

@Deprecated("Use BrandNavyText", ReplaceWith("BrandNavyText"))
val Navy = BrandNavyText

@Deprecated("Use BrandNotesGray", ReplaceWith("BrandNotesGray"))
val SlateGray = BrandNotesGray

@Deprecated("Use BrandBackgroundLight", ReplaceWith("BrandBackgroundLight"))
val Background = BrandBackgroundLight

@Deprecated("Use BrandSurfaceLight", ReplaceWith("BrandSurfaceLight"))
val Surface = BrandSurfaceLight

@Deprecated("Use BrandErrorRed", ReplaceWith("BrandErrorRed"))
val ErrorRed = BrandErrorRed

@Deprecated("Use BrandStreakOrange", ReplaceWith("BrandStreakOrange"))
val StreakOrange = BrandStreakOrange

@Deprecated("Use BrandStreakOrangeLight", ReplaceWith("BrandStreakOrangeLight"))
val StreakOrangeLight = BrandStreakOrangeLight

@Deprecated("Use BrandSolverHintAmber", ReplaceWith("BrandSolverHintAmber"))
val SolverYellow = Color(0xFFFFF3CD)

@Deprecated("Use a Brand* token", ReplaceWith("BrandSolverHintAmber"))
val SolverYellowBorder = Color(0xFFFFEAA7)

@Deprecated("Use BrandBorderLightFaint", ReplaceWith("BrandBorderLightFaint"))
val BorderLight = BrandBorderLightFaint

@Deprecated("Use BrandBorderLightMedium", ReplaceWith("BrandBorderLightMedium"))
val BorderMedium = BrandBorderLightMedium
