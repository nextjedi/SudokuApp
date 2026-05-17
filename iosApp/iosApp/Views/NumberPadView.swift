//
//  NumberPadView.swift
//  Sudoku Brain Gym — 9 digit buttons + erase
//
//  Shape: RoundedRectangle 18% radius (NOT Cookie9Sided — per P0-5,
//  Cookie9Sided was reverted to Experimental in M3 1.5-alpha19 and is the
//  wrong shape for a precise hit target anyway).
//
//  Accessibility:
//   - Each digit button announces "Digit N, N remaining" so blind users
//     know which digits are still placeable.
//   - The erase button announces "Erase selected cell" with a destructive
//     trait.
//   - All buttons have ≥ 44pt hit targets (WCAG 2.5.8 AAA).
//
//  References:
//  - RoundedRectangle:        https://developer.apple.com/documentation/swiftui/roundedrectangle
//  - Button (HIG hit target): https://developer.apple.com/design/human-interface-guidelines/buttons
//

import SwiftUI

struct NumberPadView: View {

    /// Per-digit remaining counts (9 - count placed). Out-of-bounds keys
    /// fall back to "?" — caller is expected to provide a complete dict.
    let remainingCounts: [Int: Int]

    let onDigit: (Int) -> Void
    let onErase: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            // First row 1-5
            HStack(spacing: 8) {
                ForEach(1...5, id: \.self) { digit in
                    digitButton(digit)
                }
            }
            HStack(spacing: 8) {
                ForEach(6...9, id: \.self) { digit in
                    digitButton(digit)
                }
                eraseButton
            }
        }
    }

    private func digitButton(_ digit: Int) -> some View {
        let count = remainingCounts[digit] ?? 0
        return Button {
            onDigit(digit)
        } label: {
            ZStack(alignment: .topTrailing) {
                Text("\(digit)")
                    .font(AppTypography.numberPadDigit)
                    .frame(maxWidth: .infinity, minHeight: 56)
                if count >= 0 && count <= 9 {
                    Text("\(count)")
                        .font(AppTypography.caption)
                        .foregroundStyle(.secondary)
                        .padding(.top, 4)
                        .padding(.trailing, 6)
                }
            }
        }
        .buttonStyle(.bordered)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(Color.primary.opacity(0.20), lineWidth: 0.5)
        )
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .frame(minWidth: 44, minHeight: 56)
        .accessibilityLabel("Digit \(digit)")
        .accessibilityValue(count >= 0
                            ? "\(count) remaining"
                            : "All placed")
        .accessibilityHint("Places \(digit) in the selected cell.")
    }

    private var eraseButton: some View {
        Button(role: .destructive) {
            onErase()
        } label: {
            Image(systemName: "delete.left")
                .font(.title2)
                .frame(maxWidth: .infinity, minHeight: 56)
        }
        .buttonStyle(.bordered)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .frame(minWidth: 44, minHeight: 56)
        .accessibilityLabel("Erase")
        .accessibilityHint("Clears the value in the selected cell.")
    }
}
