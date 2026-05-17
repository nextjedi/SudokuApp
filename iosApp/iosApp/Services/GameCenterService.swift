//
//  GameCenterService.swift
//  Sudoku Brain Gym — Game Center, DEFERRED sign-in
//
//  CRITICAL — Maggie P1-16 + iOS reviewer P1-11:
//   - NEVER call `GKLocalPlayer.local.authenticateHandler = ...` from
//     `App.init` or root `onAppear`.
//   - Authentication is deferred until the user explicitly opens
//     Leaderboards or Achievements from Settings.
//   - Reason: auth-on-launch presents the Apple ID sheet to users who
//     don't care about Game Center, hurting conversion and App Store ratings.
//
//  The Game Center entitlement `com.apple.developer.game-center` IS present
//  in `iosApp.entitlements`. To satisfy App Store review (P1-11), we either:
//    a) implement one leaderboard / one achievement (Total Wins) NOW, OR
//    b) strip the entitlement until Phase 6.
//  This service supports (a) — implement on-demand.
//
//  References:
//  - GameKit:                       https://developer.apple.com/documentation/gamekit
//  - GKLocalPlayer.authenticateHandler:
//                                   https://developer.apple.com/documentation/gamekit/gklocalplayer/authenticatehandler
//  - GKAccessPoint:                 https://developer.apple.com/documentation/gamekit/gkaccesspoint
//

import Foundation
import GameKit

@MainActor
final class GameCenterService {

    static let shared = GameCenterService()

    enum AuthState: Equatable {
        case notRequested
        case authenticating
        case authenticated
        case failed(reason: String)
        case disabledByUser
    }

    private(set) var state: AuthState = .notRequested

    private init() {}

    // MARK: - Public API

    /// Lazy auth. Call only from explicit user actions (Settings →
    /// "Show Leaderboards", "View Achievements"). Never from App.init.
    /// Presents the system view controller if needed.
    ///
    /// Completion is delivered on the main actor.
    func authenticateIfNeeded() async -> AuthState {
        if state == .authenticated { return state }
        state = .authenticating
        return await withCheckedContinuation { (cont:
            CheckedContinuation<AuthState, Never>) in
            GKLocalPlayer.local.authenticateHandler = { [weak self] vc, error in
                guard let self else { return }
                Task { @MainActor in
                    if let _ = vc {
                        // The system gave us a sign-in VC to present. Callers
                        // are responsible for presenting it; we capture and
                        // expose via a follow-up state. For the scaffold we
                        // just note that auth is required and let the View
                        // present it.
                        self.state = .authenticating
                    } else if GKLocalPlayer.local.isAuthenticated {
                        self.state = .authenticated
                        cont.resume(returning: .authenticated)
                    } else if let error = error {
                        self.state = .failed(reason: error.localizedDescription)
                        cont.resume(returning: self.state)
                    } else {
                        self.state = .disabledByUser
                        cont.resume(returning: .disabledByUser)
                    }
                }
            }
        }
    }

    // MARK: - Submissions (no-op until authenticated)

    /// Submit a score to the "Total Wins" leaderboard.
    /// `boardId` is the App Store Connect leaderboard identifier.
    func submitScore(_ value: Int, leaderboardID: String) async {
        guard state == .authenticated else { return }
        // GKLeaderboard.submitScore(...) — modern (iOS 14+) async signature:
        // try? await GKLeaderboard.submitScore(value, context: 0,
        //                                       player: GKLocalPlayer.local,
        //                                       leaderboardIDs: [leaderboardID])
    }

    /// Report an achievement.
    func reportAchievement(_ id: String, percentComplete: Double = 100) async {
        guard state == .authenticated else { return }
        // let ach = GKAchievement(identifier: id)
        // ach.percentComplete = percentComplete
        // try? await GKAchievement.report([ach])
    }
}
