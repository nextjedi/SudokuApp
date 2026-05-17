//
//  StatsView.swift
//  Sudoku Brain Gym — Stats screen
//
//  Three sections (a11y headings):
//   1. Overall — total games, wins, win rate, current/longest streak
//   2. By difficulty — per-difficulty wins + best time
//   3. Stylus — accuracy + latency
//
//  References:
//  - Form(.grouped):  https://developer.apple.com/documentation/swiftui/form
//

import SwiftUI

struct StatsView: View {

    @State private var vm = StatsViewModel()

    var body: some View {
        NavigationStack {
            Form {
                Section("Overall") {
                    statRow("Total games", value: "\(vm.totalGames)")
                    statRow("Wins",        value: "\(vm.totalWins)")
                    statRow("Win rate",    value: "\(vm.winRatePercent)%")
                    statRow("Current streak", value: "\(vm.currentStreak) days")
                    statRow("Longest streak", value: "\(vm.longestStreak) days")
                }

                Section("By Difficulty") {
                    ForEach(Difficulty.allCases, id: \.self) { diff in
                        statRow(diff.label,
                                value: bestTimeLabel(for: diff))
                    }
                }

                Section {
                    statRow("Stylus accuracy", value: "—")
                    statRow("Stylus latency",  value: "—")
                } header: {
                    Text("Stylus")
                } footer: {
                    Text("Recognition runs entirely on-device. " +
                         "Stylus strokes are never persisted or transmitted.")
                }
            }
            .formStyle(.grouped)
            .navigationTitle("Stats")
            .onAppear { vm.onAppear() }
        }
    }

    private func statRow(_ label: String, value: String) -> some View {
        HStack {
            Text(label)
            Spacer()
            Text(value)
                .foregroundStyle(.secondary)
                .monospacedDigit()
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(label): \(value)")
    }

    private func bestTimeLabel(for difficulty: Difficulty) -> String {
        if let seconds = vm.bestTimeByDifficulty[difficulty], seconds > 0 {
            let m = seconds / 60, s = seconds % 60
            return String(format: "%d wins, best %02d:%02d",
                          vm.winsByDifficulty[difficulty] ?? 0, m, s)
        }
        return "—"
    }
}
