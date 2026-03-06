import SwiftUI

@main
struct SudokuBrainGymApp: App {
    @StateObject private var gameViewModel = GameViewModel()
    @StateObject private var settingsViewModel = SettingsViewModel()
    @StateObject private var statsViewModel = StatsViewModel()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(gameViewModel)
                .environmentObject(settingsViewModel)
                .environmentObject(statsViewModel)
        }
    }
}
