//
//  SudokuGridCanvas.swift
//  Sudoku Brain Gym — 9×9 grid with PER-CELL accessibility semantics
//
//  ============================================================
//  CRITICAL: Lena P0-A11Y-001 — Blind users MUST be able to play.
//  ============================================================
//
//  Implementation rules:
//
//   1. Each cell is a separate `.accessibilityElement(children: .ignore)`
//      with explicit `.accessibilityLabel`, `.accessibilityValue`,
//      `.accessibilityHint`, and `.accessibilityAddTraits`.
//
//   2. Label format: "Row R, Column C, Box B" (1-indexed).
//      Value format:
//        - filled: "given 7" or "7"
//        - empty:  "empty, candidates 2 4 9" (or just "empty" if no notes)
//      Hint: "Locked, given clue" for given cells.
//            "Double-tap to enter a digit" for editable cells.
//
//   3. Selection state: when a cell is selected, it announces ", selected"
//      via `.accessibilityAddTraits(.isSelected)`.
//
//   4. Box boundaries (row 3↔4 and 6↔7, col 3↔4 and 6↔7) get heavier
//      borders — visual cue for sighted users. Sub-grid boundary
//      announcements live in GameViewModel (J-7 item 3, P3-A11Y-025).
//
//   5. The grid is NOT a single big Canvas. It's a `LazyVGrid` of 81
//      `SudokuCellView` instances, each with its own semantics. This is
//      the only structurally correct way for VoiceOver to focus per cell.
//
//   6. Mistake state gets BOTH color AND dashed border (Lena P0-3 — color-
//      blind users with achromatopsia still see the cue).
//
//  References:
//  - .accessibilityElement:    https://developer.apple.com/documentation/swiftui/view/accessibilityelement(children:)
//  - .accessibilityLabel:      https://developer.apple.com/documentation/swiftui/view/accessibilitylabel(_:)
//  - .accessibilityValue:      https://developer.apple.com/documentation/swiftui/view/accessibilityvalue(_:)
//  - .accessibilityHint:       https://developer.apple.com/documentation/swiftui/view/accessibilityhint(_:)
//  - .accessibilityAddTraits:  https://developer.apple.com/documentation/swiftui/view/accessibilityaddtraits(_:)
//  - .accessibilitySortPriority: https://developer.apple.com/documentation/swiftui/view/accessibilitysortpriority(_:)
//

import SwiftUI

struct SudokuGridCanvas: View {

    let snapshot: BoardSnapshot
    let highContrast: Bool
    let colorBlindMode: AppColorBlindMode
    let verboseAnnouncements: Bool

    /// Callback when a cell is tapped (used for selection).
    let onSelect: (CellCoord) -> Void

    var body: some View {
        GeometryReader { geo in
            let side = min(geo.size.width, geo.size.height)
            let cellSize = side / 9
            VStack(spacing: 0) {
                ForEach(0..<9, id: \.self) { row in
                    HStack(spacing: 0) {
                        ForEach(0..<9, id: \.self) { col in
                            let coord = CellCoord(row: row, col: col)
                            let cell = snapshot.cell(at: coord)
                            SudokuCellView(
                                coord: coord,
                                cell: cell,
                                isSelected: snapshot.selectedCell == coord,
                                isPeer: isPeer(of: snapshot.selectedCell,
                                               coord: coord),
                                sameValueAsSelected:
                                    sharesValueWithSelected(cell: cell),
                                cellSize: cellSize,
                                highContrast: highContrast,
                                colorBlindMode: colorBlindMode,
                                verboseAnnouncements: verboseAnnouncements,
                                onTap: { onSelect(coord) }
                            )
                            .frame(width: cellSize, height: cellSize)
                            // Heavy borders on 3×3 box boundaries.
                            .overlay(alignment: .leading) {
                                if col == 3 || col == 6 {
                                    Rectangle()
                                        .frame(width: 2)
                                        .foregroundStyle(.primary)
                                        .accessibilityHidden(true)
                                }
                            }
                            .overlay(alignment: .top) {
                                if row == 3 || row == 6 {
                                    Rectangle()
                                        .frame(height: 2)
                                        .foregroundStyle(.primary)
                                        .accessibilityHidden(true)
                                }
                            }
                            // Sort priority: VoiceOver focus order =
                            // typewriter (R1C1 → R1C9 → R2C1 → …).
                            // SwiftUI's default doc order = our HStack/VStack
                            // order, which IS typewriter. We set priorities
                            // explicitly so future grid refactors can't break
                            // it accidentally.
                            .accessibilitySortPriority(
                                Double(81 - (row * 9 + col)))
                        }
                    }
                }
            }
            .frame(width: side, height: side)
            .border(Color.primary, width: 2)
        }
        .aspectRatio(1, contentMode: .fit)
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Sudoku grid, 9 rows by 9 columns")
    }

    // MARK: - Helpers

    private func isPeer(of selected: CellCoord?, coord: CellCoord) -> Bool {
        guard let s = selected else { return false }
        return s.row == coord.row || s.col == coord.col || s.box == coord.box
    }

    private func sharesValueWithSelected(cell: CellState) -> Bool {
        guard let s = snapshot.selectedCell else { return false }
        let v = snapshot.cell(at: s).value
        return v != 0 && v == cell.value
    }
}

// MARK: - Cell view

private struct SudokuCellView: View {

    let coord: CellCoord
    let cell: CellState
    let isSelected: Bool
    let isPeer: Bool
    let sameValueAsSelected: Bool
    let cellSize: CGFloat
    let highContrast: Bool
    let colorBlindMode: AppColorBlindMode
    let verboseAnnouncements: Bool
    let onTap: () -> Void

    var body: some View {
        ZStack {
            backgroundFill
            borderOverlay
            content
        }
        .contentShape(Rectangle())
        .onTapGesture { onTap() }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityValue(value)
        .accessibilityHint(hint)
        .accessibilityAddTraits(traits)
        .accessibilityRemoveTraits(isSelected ? [] : [.isSelected])
    }

    // MARK: Visuals

    @ViewBuilder
    private var backgroundFill: some View {
        if isSelected {
            Color.accentColor.opacity(0.35)
        } else if sameValueAsSelected {
            Color.green.opacity(0.18)
        } else if isPeer {
            Color.accentColor.opacity(0.10)
        } else {
            Color(.systemBackground)
        }
    }

    @ViewBuilder
    private var borderOverlay: some View {
        // Light per-cell separator. Heavy box borders are drawn by the parent.
        Rectangle()
            .strokeBorder(Color.primary.opacity(0.30),
                          lineWidth: highContrast ? 1.5 : 0.5)
        if cell.isError {
            // Lena P0-3: color-blind users get a SHAPE cue (dashed) on top
            // of the color. The dash pattern is preserved across
            // colorBlindMode/highContrast.
            Rectangle()
                .strokeBorder(
                    Color.red,
                    style: StrokeStyle(lineWidth: highContrast ? 3 : 2,
                                       dash: [4, 3])
                )
                .accessibilityHidden(true)
        }
    }

    @ViewBuilder
    private var content: some View {
        if cell.value != 0 {
            Text("\(cell.value)")
                .font(AppTypography.gridDigit(baseSize: 24, cellSize: cellSize))
                .foregroundStyle(cell.isGiven ? Color.primary
                                              : Color.accentColor)
                .fontWeight(cell.isGiven ? .bold : .semibold)
        } else if !cell.candidates.isEmpty {
            candidatesGrid
        }
    }

    /// Mini 3×3 of candidate digits in an empty cell.
    private var candidatesGrid: some View {
        VStack(spacing: 0) {
            ForEach(0..<3, id: \.self) { r in
                HStack(spacing: 0) {
                    ForEach(0..<3, id: \.self) { c in
                        let n = r * 3 + c + 1
                        Text(cell.candidates.contains(n) ? "\(n)" : "")
                            .font(AppTypography.gridCandidate(
                                cellSize: cellSize))
                            .foregroundStyle(.secondary)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    }
                }
            }
        }
        .padding(2)
    }

    // MARK: Accessibility strings

    private var label: String {
        // "Row 3, Column 5, Box 5"
        "Row \(coord.row + 1), Column \(coord.col + 1), Box \(coord.box + 1)"
    }

    private var value: String {
        if cell.value != 0 {
            return cell.isGiven ? "given \(cell.value)" : "\(cell.value)"
        }
        if cell.candidates.isEmpty { return "empty" }
        if !verboseAnnouncements { return "empty" }
        let list = cell.candidates.sorted()
                                  .map(String.init)
                                  .joined(separator: " ")
        return "empty, candidates \(list)"
    }

    private var hint: String {
        if cell.isGiven { return "Locked, given clue." }
        if cell.value != 0 { return "Double-tap to change." }
        return "Double-tap to enter a digit."
    }

    private var traits: AccessibilityTraits {
        var t: AccessibilityTraits = isSelected ? [.isSelected] : []
        if cell.isGiven { t.insert(.isStaticText) }
        if !cell.isGiven { t.insert(.isButton) }
        return t
    }
}
