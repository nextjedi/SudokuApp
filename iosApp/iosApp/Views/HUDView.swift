//
//  HUDView.swift
//  Sudoku Brain Gym — top HUD capsule
//
//  Single capsule per HIG (no stacking glass). Contents:
//    Difficulty · ● ● ○ mistakes · ⏱ 03:21 · Score 2,340
//
//  Accessibility:
//   - The capsule is ONE accessibility element with a composite label so
//     VoiceOver reads it as a unit, not five sub-strings.
//
//  References:
//  - HIG Live activities / HUD pattern: https://developer.apple.com/design/human-interface-guidelines/live-activities
//

import SwiftUI

struct HUDView: View {

    let snapshot: BoardSnapshot
    let highContrast: Bool

    @Environment(AccessibilityFlags.self) private var a11y

    var body: some View {
        HStack(spacing: 12) {
            difficultyChip
            Divider().frame(height: 18)
            mistakesIndicator
            Divider().frame(height: 18)
            timerLabel
            Divider().frame(height: 18)
            scoreLabel
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .adaptiveGlass(.regular, in: Capsule(),
                       flags: a11y, highContrast: highContrast)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(compositeLabel)
    }

    // MARK: - Sub-elements

    private var difficultyChip: some View {
        Text(snapshot.difficulty.label)
            .font(AppTypography.subheadline)
            .fontWeight(.semibold)
    }

    private var mistakesIndicator: some View {
        HStack(spacing: 3) {
            ForEach(0..<max(0, snapshot.mistakeLimit), id: \.self) { i in
                Circle()
                    .fill(i < snapshot.mistakes
                          ? Color.red
                          : Color.secondary.opacity(0.4))
                    .frame(width: 8, height: 8)
                    .accessibilityHidden(true)
            }
        }
    }

    private var timerLabel: some View {
        HStack(spacing: 4) {
            Image(systemName: "timer")
                .imageScale(.small)
                .accessibilityHidden(true)
            Text(formatted(seconds: snapshot.elapsedSeconds))
                .font(AppTypography.timer)
                .monospacedDigit()
        }
    }

    private var scoreLabel: some View {
        Text("\(snapshot.score)")
            .font(AppTypography.hudNumeric)
            .monospacedDigit()
    }

    // MARK: - Composite a11y label

    private var compositeLabel: String {
        "Difficulty \(snapshot.difficulty.label). " +
        "Mistakes \(snapshot.mistakes) of \(snapshot.mistakeLimit). " +
        "Timer \(spokenTime(seconds: snapshot.elapsedSeconds)). " +
        "Score \(snapshot.score)."
    }

    // MARK: - Format helpers

    private func formatted(seconds: Int) -> String {
        let m = seconds / 60, s = seconds % 60
        return String(format: "%02d:%02d", m, s)
    }

    private func spokenTime(seconds: Int) -> String {
        let m = seconds / 60, s = seconds % 60
        if m == 0 { return "\(s) seconds" }
        return "\(m) minutes \(s) seconds"
    }
}
