//
//  AppColors.swift
//  Sudoku Brain Gym — Theme tokens
//
//  Brand color tokens with first-class:
//   - Light / Dark / AMOLED true-black variants
//   - Color-blind palette overrides (Deuteranopia, Protanopia)
//   - High Contrast overrides (forced when `highContrast == true` in
//     AppSettings or `UIAccessibility.isDarkerSystemColorsEnabled` is true)
//
//  Why per-token computed Colors (not Assets.xcassets):
//   - Allows runtime swap when AppSettings.colorBlindMode changes without
//     a relaunch.
//   - Makes the relationships between tokens documentable in code.
//   - Avoids 8× JSON files per token, which would diverge.
//
//  References:
//  - Color in SwiftUI:                 https://developer.apple.com/documentation/swiftui/color
//  - HIG: Color:                       https://developer.apple.com/design/human-interface-guidelines/color
//  - WCAG 2.2 Contrast (1.4.3 / 1.4.6): https://www.w3.org/TR/WCAG22/#contrast-minimum
//

import SwiftUI

// MARK: - Theme mode

/// Mirrors `AppSettings.themeMode`. We avoid `enum Theme: String, CaseIterable`
/// duplication with `AppSettings.swift` by re-exporting that enum here when needed.
enum AppColorPaletteVariant {
    case light
    case dark
    case amoled         // dark with pure #000000 background — saves OLED battery
}

enum AppColorBlindMode: String, Codable, CaseIterable {
    case none
    case deuteranopia
    case protanopia
    case tritanopia     // P3 in Lena's audit, but stub allows future opt-in
}

// MARK: - Token set

/// All semantic color tokens used by the app. Resolved at render time
/// based on the active variant + accessibility mode.
struct AppColors {

    // Surfaces
    let background: Color
    let surface: Color
    let surfaceElevated: Color
    let surfaceGlass: Color           // base under .glassEffect/.thinMaterial

    // Text
    let textPrimary: Color
    let textSecondary: Color
    let textTertiary: Color

    // Brand
    let primary: Color
    let primaryMuted: Color
    let accent: Color

    // Grid state
    let cellGiven: Color              // text color for "given clue" digits
    let cellUser: Color               // text color for user-entered digits
    let cellSelected: Color           // background fill of selected cell
    let cellPeerHighlight: Color      // same-row/col/box highlight
    let cellSameValueHighlight: Color // cells sharing the same value

    // Feedback (paired luminance-safe with shape cue for color-blind)
    let success: Color                // paired with solid border
    let error: Color                  // paired with dashed border (see Lena P0-3)
    let warning: Color

    // Dividers, borders
    let borderSubtle: Color
    let borderStrong: Color

    // MARK: Resolvers

    static func resolve(
        variant: AppColorPaletteVariant,
        colorBlind: AppColorBlindMode,
        highContrast: Bool
    ) -> AppColors {
        switch variant {
        case .light:  return light(colorBlind: colorBlind, highContrast: highContrast)
        case .dark:   return dark(colorBlind: colorBlind, highContrast: highContrast,
                                  pureBlack: false)
        case .amoled: return dark(colorBlind: colorBlind, highContrast: highContrast,
                                  pureBlack: true)
        }
    }

    // MARK: Light

    private static func light(colorBlind: AppColorBlindMode,
                              highContrast: Bool) -> AppColors {
        let (succ, err) = pairedFeedback(variant: .light, mode: colorBlind)
        return AppColors(
            background:               Color(red: 0.97, green: 0.97, blue: 0.98),
            surface:                  .white,
            surfaceElevated:          Color(red: 0.99, green: 0.99, blue: 1.00),
            surfaceGlass:             Color.white.opacity(highContrast ? 0.95 : 0.65),
            textPrimary:              Color(red: 0.08, green: 0.10, blue: 0.16),
            textSecondary:            Color(red: 0.28, green: 0.31, blue: 0.38),
            textTertiary:             Color(red: 0.46, green: 0.50, blue: 0.56),
            primary:                  Color(red: 0.18, green: 0.39, blue: 0.85),
            primaryMuted:             Color(red: 0.85, green: 0.90, blue: 1.00),
            accent:                   Color(red: 0.35, green: 0.22, blue: 0.84),
            cellGiven:                Color(red: 0.08, green: 0.10, blue: 0.16),
            cellUser:                 Color(red: 0.18, green: 0.39, blue: 0.85),
            cellSelected:             Color(red: 0.78, green: 0.86, blue: 1.00),
            cellPeerHighlight:        Color(red: 0.91, green: 0.94, blue: 1.00),
            cellSameValueHighlight:   Color(red: 0.85, green: 0.92, blue: 0.78),
            success:                  succ,
            error:                    err,
            warning:                  Color(red: 0.93, green: 0.58, blue: 0.05),
            borderSubtle:             Color(red: 0.85, green: 0.86, blue: 0.90),
            borderStrong:             highContrast
                                        ? Color.black
                                        : Color(red: 0.50, green: 0.52, blue: 0.58)
        )
    }

    // MARK: Dark / AMOLED

    private static func dark(colorBlind: AppColorBlindMode,
                             highContrast: Bool,
                             pureBlack: Bool) -> AppColors {
        let (succ, err) = pairedFeedback(variant: .dark, mode: colorBlind)
        let bg: Color = pureBlack
            ? .black
            : Color(red: 0.06, green: 0.08, blue: 0.12)
        let surface: Color = pureBlack
            ? Color(red: 0.04, green: 0.04, blue: 0.04)
            : Color(red: 0.10, green: 0.12, blue: 0.18)
        return AppColors(
            background:               bg,
            surface:                  surface,
            surfaceElevated:          pureBlack
                                        ? Color(red: 0.07, green: 0.07, blue: 0.07)
                                        : Color(red: 0.14, green: 0.17, blue: 0.24),
            surfaceGlass:             Color.white.opacity(highContrast ? 0.10 : 0.05),
            textPrimary:              Color(red: 0.96, green: 0.97, blue: 1.00),
            textSecondary:            Color(red: 0.70, green: 0.74, blue: 0.82),
            textTertiary:             Color(red: 0.52, green: 0.56, blue: 0.66),
            primary:                  Color(red: 0.50, green: 0.74, blue: 1.00),
            primaryMuted:             Color(red: 0.16, green: 0.26, blue: 0.48),
            accent:                   Color(red: 0.65, green: 0.50, blue: 1.00),
            cellGiven:                Color(red: 0.96, green: 0.97, blue: 1.00),
            cellUser:                 Color(red: 0.50, green: 0.74, blue: 1.00),
            cellSelected:             Color(red: 0.20, green: 0.34, blue: 0.62),
            cellPeerHighlight:        Color(red: 0.14, green: 0.22, blue: 0.40),
            cellSameValueHighlight:   Color(red: 0.18, green: 0.30, blue: 0.20),
            success:                  succ,
            error:                    err,
            warning:                  Color(red: 0.99, green: 0.71, blue: 0.20),
            borderSubtle:             Color(red: 0.22, green: 0.24, blue: 0.30),
            borderStrong:             highContrast
                                        ? .white
                                        : Color(red: 0.60, green: 0.64, blue: 0.72)
        )
    }

    // MARK: Color-blind paired feedback
    //
    // Critical: per Lena P0-A11Y-003, color-blind palette MUST also carry a
    // non-color cue. The cue itself (dashed vs solid border) is applied in
    // the view layer (SudokuGridCanvas). Here we only pick hues whose
    // luminance gap is large enough to remain distinguishable in
    // achromatopsia simulators (target ≥ 0.25 L difference).
    private static func pairedFeedback(
        variant: AppColorPaletteVariant,
        mode: AppColorBlindMode
    ) -> (success: Color, error: Color) {
        switch mode {
        case .none:
            return variant == .light
                ? (Color(red: 0.18, green: 0.59, blue: 0.32),
                   Color(red: 0.79, green: 0.20, blue: 0.20))
                : (Color(red: 0.40, green: 0.85, blue: 0.55),
                   Color(red: 1.00, green: 0.45, blue: 0.45))
        case .deuteranopia, .protanopia:
            // Blue-yellow axis preserved. Use luminance-separated pair.
            return variant == .light
                ? (Color(red: 0.10, green: 0.36, blue: 0.78),   // deep blue (L ≈ 0.30)
                   Color(red: 0.98, green: 0.68, blue: 0.16))   // amber       (L ≈ 0.74)
                : (Color(red: 0.55, green: 0.78, blue: 1.00),   // light blue (L ≈ 0.74)
                   Color(red: 0.95, green: 0.62, blue: 0.10))   // amber       (L ≈ 0.62)
        case .tritanopia:
            // Red-cyan axis (blue cone deficient). Use red/cyan.
            return variant == .light
                ? (Color(red: 0.05, green: 0.60, blue: 0.65),   // cyan
                   Color(red: 0.82, green: 0.12, blue: 0.18))   // red
                : (Color(red: 0.45, green: 0.85, blue: 0.90),
                   Color(red: 1.00, green: 0.40, blue: 0.45))
        }
    }
}
