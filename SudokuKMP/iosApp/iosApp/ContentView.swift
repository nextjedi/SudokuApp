import SwiftUI

enum AppRoute {
    case home
    case game
    case settings
    case stats
}

struct ContentView: View {
    @State private var route: AppRoute = .home
    @EnvironmentObject var gameViewModel: GameViewModel
    @EnvironmentObject var settingsViewModel: SettingsViewModel
    @EnvironmentObject var statsViewModel: StatsViewModel

    var body: some View {
        switch route {
        case .home:
            HomeView(
                onStartGame: { route = .game },
                onOpenSettings: { route = .settings },
                onOpenStats: { route = .stats }
            )
        case .game:
            GameView(onExit: { route = .home })
        case .settings:
            SettingsView(onBack: { route = .home })
        case .stats:
            StatsView(onBack: { route = .home })
        }
    }
}
