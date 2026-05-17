//
//  ContentView.swift
//  Sudoku Brain Gym
//
//  Root container. Adopts platform-correct navigation:
//   - iPhone → TabView (iOS 26 gives Liquid Glass for free on `.tabItem`)
//   - iPad   → NavigationSplitView (sidebar + detail, iPad-class HIG)
//
//  Rationale: Apple's own apps (Settings, Files, Mail) use NavigationSplitView
//  on iPad. TabView on iPad would lock us out of Stage Manager / external-
//  display layouts and break the iPad-class HIG.
//
//  References:
//  - TabView:               https://developer.apple.com/documentation/swiftui/tabview
//  - NavigationSplitView:   https://developer.apple.com/documentation/swiftui/navigationsplitview
//  - iPad layout HIG:       https://developer.apple.com/design/human-interface-guidelines/designing-for-ipad
//  - Liquid Glass on tabs:  https://developer.apple.com/videos/play/wwdc2025/219/
//

import SwiftUI

/// Top-level destinations. Used as `Tab` tags on iPhone and `selection`
/// in the iPad split view.
enum AppDestination: Hashable, CaseIterable, Identifiable {
    case home
    case game
    case stats
    case settings

    var id: Self { self }

    var title: String {
        switch self {
        case .home:     return "Home"
        case .game:     return "Play"
        case .stats:    return "Stats"
        case .settings: return "Settings"
        }
    }

    var systemImage: String {
        switch self {
        case .home:     return "house.fill"
        case .game:     return "square.grid.3x3.fill"
        case .stats:    return "chart.bar.fill"
        case .settings: return "gearshape.fill"
        }
    }
}

struct ContentView: View {

    @Environment(AppSettingsStore.self) private var settingsStore
    @Environment(AccessibilityFlags.self) private var a11y

    @State private var selection: AppDestination = .home

    var body: some View {
        // The `userInterfaceIdiom` check below is the canonical iPad/iPhone
        // branch in SwiftUI. We avoid `horizontalSizeClass == .regular` because
        // it also matches large iPhones in landscape, where TabView is still
        // the HIG-correct choice.
        if UIDevice.current.userInterfaceIdiom == .pad {
            IPadRoot(selection: $selection)
        } else {
            IPhoneRoot(selection: $selection)
        }
    }
}

// MARK: - iPhone (TabView)

private struct IPhoneRoot: View {
    @Binding var selection: AppDestination

    var body: some View {
        TabView(selection: $selection) {
            HomeView()
                .tabItem { Label(AppDestination.home.title,
                                 systemImage: AppDestination.home.systemImage) }
                .tag(AppDestination.home)

            GameView()
                .tabItem { Label(AppDestination.game.title,
                                 systemImage: AppDestination.game.systemImage) }
                .tag(AppDestination.game)

            StatsView()
                .tabItem { Label(AppDestination.stats.title,
                                 systemImage: AppDestination.stats.systemImage) }
                .tag(AppDestination.stats)

            SettingsView()
                .tabItem { Label(AppDestination.settings.title,
                                 systemImage: AppDestination.settings.systemImage) }
                .tag(AppDestination.settings)
        }
    }
}

// MARK: - iPad (NavigationSplitView)

private struct IPadRoot: View {
    @Binding var selection: AppDestination

    var body: some View {
        NavigationSplitView {
            List(AppDestination.allCases, selection: $selection) { dest in
                NavigationLink(value: dest) {
                    Label(dest.title, systemImage: dest.systemImage)
                }
            }
            .navigationTitle("Sudoku Brain Gym")
        } detail: {
            switch selection {
            case .home:     HomeView()
            case .game:     GameView()
            case .stats:    StatsView()
            case .settings: SettingsView()
            }
        }
    }
}
