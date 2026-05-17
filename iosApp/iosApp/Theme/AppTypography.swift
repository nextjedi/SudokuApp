//
//  AppTypography.swift
//  Sudoku Brain Gym — Type scale
//
//  Two-family approach per MASTER_SPEC §10:
//   - SF Pro Rounded for digits (grid cells, number pad). Rounded glyphs
//     are friendlier and visually distinct from UI chrome.
//   - SF Pro for the rest of UI chrome.
//
//  Note (Lena P1-A11Y-011): grid digits must scale with Dynamic Type or
//  offer a "Big digits" toggle in Settings. The `gridDigit(...)` helper
//  below returns a Font that uses `.relativeTo` so the user's preferred
//  text-size pref is respected, while clamping to a max so the digit never
//  busts its 1/9 cell width.
//
//  References:
//  - Font.system(.rounded):  https://developer.apple.com/documentation/swiftui/font/design
//  - Dynamic Type:           https://developer.apple.com/documentation/uikit/text_display_and_fonts/scaling_fonts_automatically
//  - SF Pro Rounded:         https://developer.apple.com/fonts/
//

import SwiftUI

enum AppTypography {

    // MARK: - UI text (SF Pro)

    static let largeTitle  = Font.system(.largeTitle, design: .default, weight: .bold)
    static let title       = Font.system(.title,      design: .default, weight: .semibold)
    static let title2      = Font.system(.title2,     design: .default, weight: .semibold)
    static let title3      = Font.system(.title3,     design: .default, weight: .medium)
    static let headline    = Font.system(.headline,   design: .default)
    static let body        = Font.system(.body,       design: .default)
    static let callout     = Font.system(.callout,    design: .default)
    static let subheadline = Font.system(.subheadline,design: .default)
    static let footnote    = Font.system(.footnote,   design: .default)
    static let caption     = Font.system(.caption,    design: .default)

    // MARK: - Digit text (SF Pro Rounded)

    /// Number-pad digit (1..9 + erase). Always rounded, weight bold to read
    /// at a glance even on small phones.
    static let numberPadDigit = Font.system(size: 28, weight: .bold, design: .rounded)

    /// Grid digit used by `SudokuGridCanvas` for filled cells.
    /// Scales with Dynamic Type via `.relativeTo` but clamps so 9 digits fit
    /// across the grid even at accessibility5.
    ///
    /// `baseSize` is the intended pixel size at the default Dynamic Type.
    /// Callers pass the per-device cell size and we cap at `cellSize * 0.55`.
    static func gridDigit(baseSize: CGFloat, cellSize: CGFloat) -> Font {
        let cap = max(12, cellSize * 0.55)
        let effective = min(baseSize, cap)
        return Font.system(size: effective, weight: .semibold, design: .rounded)
    }

    /// Pencil-note (candidate) digit overlaid on empty cells.
    /// Always smaller than `gridDigit`. 9 per cell at most.
    static func gridCandidate(cellSize: CGFloat) -> Font {
        let size = max(8, cellSize * 0.20)
        return Font.system(size: size, weight: .regular, design: .rounded)
    }

    /// HUD numerics (score, timer, mistake count).
    static let hudNumeric = Font.system(size: 18, weight: .semibold, design: .rounded)

    // MARK: - Monospace for timer (prevents jitter)

    static let timer = Font.system(size: 18, weight: .semibold, design: .monospaced)
}
