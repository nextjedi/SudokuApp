//
//  HomeView.swift
//  Sudoku Brain Gym — Home screen
//
//  Stack of cards:
//   - Daily Challenge (with streak flame)
//   - Continue (only if `hasSavedGame`)
//   - Today's Workout
//   - New Game CTA (difficulty picker)
//   - Stats peek
//
//  Accessibility notes (Lena J-1):
//   - Section title marked as a heading via `.accessibilityAddTraits(.isHeader)`.
//   - Cards use `.accessibilityElement(children: .combine)` so VoiceOver
//     reads them as ONE node, not five.
//   - The flame icon is decorative — `.accessibilityHidden(true)`.
//   - Continue card is REMOVED from tree (not alpha-zero) when no saved game.
//
//  References:
//  - .accessibilityElement: https://developer.apple.com/documentation/swiftui/view/accessibilityelement(children:)
//  - .accessibilityAddTraits: https://developer.apple.com/documentation/swiftui/view/accessibilityaddtraits(_:)
//

import SwiftUI

struct HomeView: View {

    @State private var vm = HomeViewModel()
    @Environment(AccessibilityFlags.self) private var a11y
    @Environment(AppSettingsStore.self) private var settingsStore

    @State private var showDifficultyPicker = false

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 16) {
                    dailyChallengeCard
                    if vm.hasSavedGame { continueCard }
                    workoutCard
                    newGameCard
                    statsPeekCard
                }
                .padding(16)
            }
            .navigationTitle("Sudoku Brain Gym")
            .background(Color(.systemGroupedBackground))
            .onAppear { vm.onAppear() }
        }
    }

    // MARK: - Daily challenge

    private var dailyChallengeCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: "flame.fill")
                    .foregroundStyle(.orange)
                    .accessibilityHidden(true)
                Text("Daily Challenge")
                    .font(AppTypography.title3)
                    .accessibilityAddTraits(.isHeader)
                Spacer()
            }
            Text("Today's puzzle: \(vm.dailyChallengeDifficulty.label)")
                .font(AppTypography.subheadline)
                .foregroundStyle(.secondary)
            Text("\(vm.streakDays)-day streak")
                .font(AppTypography.footnote)
                .foregroundStyle(.secondary)
            Button {
                Task { await vm.startNewGame(difficulty:
                                              vm.dailyChallengeDifficulty) }
            } label: {
                Label("Play today's puzzle", systemImage: "play.fill")
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(16)
        .adaptiveGlass(.regular,
                       in: RoundedRectangle(cornerRadius: 20),
                       flags: a11y,
                       highContrast: settingsStore.settings.highContrast)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(
            "Daily Challenge. Today: \(vm.dailyChallengeDifficulty.label). " +
            "Current streak: \(vm.streakDays) days."
        )
        .accessibilityHint("Activates to play today's puzzle.")
    }

    // MARK: - Continue

    private var continueCard: some View {
        Button {
            Task { await vm.continueGame() }
        } label: {
            HStack {
                Image(systemName: "arrow.uturn.forward.circle.fill")
                    .accessibilityHidden(true)
                VStack(alignment: .leading) {
                    Text("Continue").font(AppTypography.headline)
                    if let diff = vm.lastPlayedDifficulty {
                        Text(diff.label)
                            .font(AppTypography.footnote)
                            .foregroundStyle(.secondary)
                    }
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundStyle(.tertiary)
                    .accessibilityHidden(true)
            }
            .padding(16)
        }
        .buttonStyle(.plain)
        .adaptiveGlass(.regular, in: RoundedRectangle(cornerRadius: 20),
                       flags: a11y,
                       highContrast: settingsStore.settings.highContrast)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Continue your saved game")
        .accessibilityHint("Returns to your previous puzzle.")
    }

    // MARK: - Workout

    private var workoutCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Today's Workout")
                .font(AppTypography.title3)
                .accessibilityAddTraits(.isHeader)
            Text("3 puzzles · 1 easy, 1 medium, 1 hard")
                .font(AppTypography.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .adaptiveGlass(.regular,
                       in: RoundedRectangle(cornerRadius: 20),
                       flags: a11y,
                       highContrast: settingsStore.settings.highContrast)
        .accessibilityElement(children: .combine)
    }

    // MARK: - New Game

    private var newGameCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("New Game")
                .font(AppTypography.title3)
                .accessibilityAddTraits(.isHeader)
            ForEach(Difficulty.allCases, id: \.self) { diff in
                Button {
                    Task { await vm.startNewGame(difficulty: diff) }
                } label: {
                    HStack {
                        Text(diff.label)
                        Spacer()
                        Image(systemName: "chevron.right")
                            .foregroundStyle(.tertiary)
                            .accessibilityHidden(true)
                    }
                }
                .buttonStyle(.plain)
                .padding(.vertical, 6)
                .accessibilityLabel("Start a \(diff.label) game")
            }
        }
        .padding(16)
        .adaptiveGlass(.regular,
                       in: RoundedRectangle(cornerRadius: 20),
                       flags: a11y,
                       highContrast: settingsStore.settings.highContrast)
    }

    // MARK: - Stats peek

    private var statsPeekCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Stats peek")
                .font(AppTypography.title3)
                .accessibilityAddTraits(.isHeader)
            Text("\(0) games · \(0) wins")
                .font(AppTypography.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .adaptiveGlass(.regular,
                       in: RoundedRectangle(cornerRadius: 20),
                       flags: a11y,
                       highContrast: settingsStore.settings.highContrast)
        .accessibilityElement(children: .combine)
    }
}
