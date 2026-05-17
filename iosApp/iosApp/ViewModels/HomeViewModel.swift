//
//  HomeViewModel.swift
//  Sudoku Brain Gym — Home screen state
//
//  Surfaces:
//   - Daily Challenge (deterministic from date, no network call — Camila P1-30).
//   - Continue (only if saved game exists).
//   - Today's Workout.
//   - New Game CTA.
//   - Stats peek.
//
//  References:
//  - @Observable:           https://developer.apple.com/documentation/observation/observable()
//

import Foundation
import Observation

@MainActor
@Observable
final class HomeViewModel {

    // MARK: - Published

    private(set) var hasSavedGame: Bool = false
    private(set) var dailyChallengeDifficulty: Difficulty = .medium
    private(set) var streakDays: Int = 0
    private(set) var lastPlayedDifficulty: Difficulty?

    // MARK: - Dependencies

    private let engine: KMPEngineAdapter
    private let settings: AppSettingsStore

    init(
        engine: KMPEngineAdapter = .shared,
        settings: AppSettingsStore = .shared
    ) {
        self.engine = engine
        self.settings = settings
    }

    // MARK: - Lifecycle

    func onAppear() {
        // Check saved-game presence (we use a key in UserDefaults; the actual
        // saved game lives in the engine's persistence). Hidden-by-default.
        hasSavedGame = UserDefaults.standard
            .object(forKey: "SavedGame.v1") != nil
        dailyChallengeDifficulty = deterministicDailyDifficulty(for: Date())
    }

    // MARK: - Actions

    func startNewGame(difficulty: Difficulty) async {
        _ = await engine.newGame(difficulty: difficulty)
        lastPlayedDifficulty = difficulty
    }

    func continueGame() async {
        // Continue is a no-op on the engine side until we wire saved-game
        // restoration in Phase 2.
    }

    // MARK: - Daily challenge generator
    //
    // Deterministic from the date. No network. Same on every device.
    // (Camila P1-30.)
    private func deterministicDailyDifficulty(for date: Date)
        -> Difficulty
    {
        let cal = Calendar(identifier: .gregorian)
        let dayOfYear = cal.ordinality(of: .day, in: .year, for: date) ?? 1
        // Cycle: Easy, Medium, Hard, Expert, Master, Medium, Easy, …
        let cycle: [Difficulty] = [.easy, .medium, .hard, .expert,
                                   .master, .medium, .easy]
        return cycle[dayOfYear % cycle.count]
    }
}
