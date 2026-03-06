import Foundation
import Combine

struct StatsState {
    var gamesPlayed: Int = 0
    var gamesWon: Int = 0
    var totalTimeSec: Int = 0
    var bestEasySec: Int = 0
    var bestMediumSec: Int = 0
    var bestHardSec: Int = 0
    var currentStreak: Int = 0
    var lastPlayedDate: String = ""

    var winRate: Int { gamesPlayed > 0 ? (gamesWon * 100 / gamesPlayed) : 0 }
    var averageTimeSec: Int { gamesWon > 0 ? totalTimeSec / gamesWon : 0 }
}

class StatsViewModel: ObservableObject {
    @Published var stats = StatsState()
    private let ud = UserDefaults.standard

    init() { load() }

    func refresh() { load() }

    func resetStats() {
        ["games_played","games_won","total_time","best_easy","best_medium",
         "best_hard","streak","last_played"].forEach { ud.removeObject(forKey: $0) }
        load()
    }

    private func load() {
        stats = StatsState(
            gamesPlayed: ud.integer(forKey: "games_played"),
            gamesWon: ud.integer(forKey: "games_won"),
            totalTimeSec: ud.integer(forKey: "total_time"),
            bestEasySec: ud.integer(forKey: "best_easy"),
            bestMediumSec: ud.integer(forKey: "best_medium"),
            bestHardSec: ud.integer(forKey: "best_hard"),
            currentStreak: ud.integer(forKey: "streak"),
            lastPlayedDate: ud.string(forKey: "last_played") ?? ""
        )
    }
}
