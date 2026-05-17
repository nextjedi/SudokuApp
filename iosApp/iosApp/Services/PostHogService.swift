//
//  PostHogService.swift
//  Sudoku Brain Gym — analytics, OPT-IN ONLY
//
//  Per Camila P0-14: `analyticsOptIn` defaults to false; the SDK must NOT
//  initialize on launch. We only call `initialize()` after the user flips
//  the toggle in Settings → Data & Privacy.
//
//  We pick PostHog because:
//   - Self-hostable (matches "all on-device" privacy story).
//   - First-class user-opt-out + delete-my-data flows.
//   - Open-source SDK we can audit.
//
//  If/when analytics ship, the PrivacyInfo.xcprivacy will also need to add
//  `NSPrivacyCollectedDataTypes` entries (currently we declare "Data Not
//  Collected").
//
//  References:
//  - PostHog iOS:   https://posthog.com/docs/libraries/ios
//  - Apple privacy nutrition labels:
//                    https://developer.apple.com/app-store/app-privacy-details/
//

import Foundation

@MainActor
final class PostHogService {

    static let shared = PostHogService()

    private(set) var isInitialized: Bool = false

    private init() {}

    /// Call ONLY after the user explicitly opts in via Settings.
    /// Idempotent — repeated calls are safe.
    func initialize(apiKey: String, host: String) {
        guard !isInitialized else { return }
        // TODO(Phase 2): wire `PostHogSDK.shared.setup(...)` once the
        // PostHog SDK is added via SPM in Phase 2. Until then this is a
        // no-op so the toggle works as a future-compatible seam.
        isInitialized = true
    }

    /// Reset/disable analytics. Called when user toggles opt-in OFF, or from
    /// "Delete All My Data". Sends an `opt_out_capturing` then no-ops further.
    func optOutAndShutdown() {
        guard isInitialized else { return }
        // TODO(Phase 2):
        //   PostHogSDK.shared.optOut()
        //   PostHogSDK.shared.reset()
        //   PostHogSDK.shared.close()
        isInitialized = false
    }

    /// Capture an event. No-op when not initialized (i.e., user hasn't
    /// opted in). This is the SAFE default — accidental capture calls are
    /// inert.
    func capture(_ event: String, properties: [String: Any]? = nil) {
        guard isInitialized else { return }
        // TODO(Phase 2): PostHogSDK.shared.capture(event, properties: properties)
    }
}
