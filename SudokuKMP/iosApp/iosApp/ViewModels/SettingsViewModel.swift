import Foundation
import Combine

struct SettingsState {
    var soundEnabled: Bool = true
    var highlightEnabled: Bool = true
    var timerEnabled: Bool = true
    var solverSpeedMs: Int = 1500
    var solverHelpCells: Int = 3
}

class SettingsViewModel: ObservableObject {
    @Published var settings = SettingsState()
    private let ud = UserDefaults.standard

    init() { load() }

    func toggleSound() { settings.soundEnabled.toggle(); save() }
    func toggleHighlight() { settings.highlightEnabled.toggle(); save() }
    func toggleTimer() { settings.timerEnabled.toggle(); save() }

    func setSolverSpeed(_ ms: Int) { settings.solverSpeedMs = ms; save() }
    func setSolverHelpCells(_ n: Int) { settings.solverHelpCells = max(1, min(9, n)); save() }

    private func load() {
        settings = SettingsState(
            soundEnabled: ud.object(forKey: "s_sound") as? Bool ?? true,
            highlightEnabled: ud.object(forKey: "s_highlight") as? Bool ?? true,
            timerEnabled: ud.object(forKey: "s_timer") as? Bool ?? true,
            solverSpeedMs: ud.object(forKey: "s_solver_speed") as? Int ?? 1500,
            solverHelpCells: ud.object(forKey: "s_solver_cells") as? Int ?? 3
        )
    }

    private func save() {
        ud.set(settings.soundEnabled, forKey: "s_sound")
        ud.set(settings.highlightEnabled, forKey: "s_highlight")
        ud.set(settings.timerEnabled, forKey: "s_timer")
        ud.set(settings.solverSpeedMs, forKey: "s_solver_speed")
        ud.set(settings.solverHelpCells, forKey: "s_solver_cells")
    }
}
