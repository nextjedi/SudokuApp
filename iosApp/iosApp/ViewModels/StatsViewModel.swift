//
//  StatsViewModel.swift
//  Sudoku Brain Gym — Stats screen state
//
//  Reads aggregated stats from the engine. Stats are stored in the KMP
//  shared module's `AppStats` data class (separate from AppSettings to
//  decouple settings migration from stats persistence — see PC-4).
//
//  References:
//  - @Observable:  https://developer.apple.com/documentation/observation/observable()
//

import Foundation
import Observation

@MainActor
@Observable
final class StatsViewModel {

    // MARK: - Published

    private(set) var totalGames: Int = 0
    private(set) var totalWins: Int = 0
    private(set) var winsByDifficulty: [Difficulty: Int] = [:]
    private(set) var bestTimeByDifficulty: [Difficulty: Int] = [:]
    private(set) var currentStreak: Int = 0
    private(set) var longestStreak: Int = 0

    // MARK: - Dependencies

    private let engine: KMPEngineAdapter

    init(engine: KMPEngineAdapter = .shared) {
        self.engine = engine
    }

    // MARK: - Lifecycle

    func onAppear() {
        // TODO(P0-4): pull from engine.stats once the KMP framework is wired.
        // Placeholder values mean the StatsView renders with zeros until
        // wired.
    }

    // MARK: - Computed

    var winRatePercent: Int {
        guard totalGames > 0 else { return 0 }
        return Int(round(Double(totalWins) / Double(totalGames) * 100))
    }
}
