//
//  SettingsViewModel.swift
//  Sudoku Brain Gym — Settings screen state
//
//  Thin wrapper around AppSettingsStore + cross-cutting actions:
//   - Replay tutorial (Maggie P1-19)
//   - Delete All My Data (Camila P0-13)
//   - Reset stylus calibration (Stylus reviewer §16 TC-PR4)
//
//  References:
//  - @Observable:  https://developer.apple.com/documentation/observation/observable()
//

import Foundation
import Observation

@MainActor
@Observable
final class SettingsViewModel {

    // MARK: - Search

    /// Bound to `.searchable(text:)` (Maggie P1-18).
    var searchQuery: String = ""

    /// Sheet presentation for the "Test Stylus" modal.
    var showStylusTest: Bool = false

    /// Sheet presentation for the destructive "Delete All My Data" confirmation.
    var showDeleteAllConfirm: Bool = false

    // MARK: - Dependencies

    private let store: AppSettingsStore
    private let analytics: PostHogService

    init(
        store: AppSettingsStore = .shared,
        analytics: PostHogService = .shared
    ) {
        self.store = store
        self.analytics = analytics
    }

    // MARK: - Settings access

    var settings: AppSettings { store.settings }

    /// Binding-style update helpers used by SwiftUI `.toggle/.picker`.
    func setStylusMode(_ mode: StylusMode) {
        store.update { $0.stylusMode = mode }
    }

    func setConfidence(_ tier: ConfidenceTier) {
        store.update { $0.stylusConfidence = tier }
    }

    func setTheme(_ mode: ThemeMode) {
        store.update { $0.themeMode = mode }
    }

    func setColorBlindMode(_ mode: AppColorBlindMode) {
        store.update { $0.colorBlindMode = mode }
    }

    func setHighContrast(_ value: Bool) {
        store.update { $0.highContrast = value }
    }

    func setReduceMotion(_ value: Bool) {
        store.update { $0.reduceMotion = value }
    }

    func setTiltParallax(_ value: Bool) {
        store.update { $0.tiltParallaxEnabled = value }
    }

    func setProximityAutoPause(_ value: Bool) {
        store.update { $0.proximityAutoPauseEnabled = value }
    }

    func setSoundEnabled(_ value: Bool) {
        store.update { $0.soundEnabled = value }
    }

    func setHapticsEnabled(_ value: Bool) {
        store.update { $0.hapticsEnabled = value }
        HapticsService.shared.isEnabled = value
    }

    func setMistakeLimit(_ limit: MistakeLimit) {
        store.update { $0.mistakeLimit = limit }
    }

    func setHintDepth(_ depth: HintDepth) {
        store.update { $0.hintDepth = depth }
    }

    func setAnalyticsOptIn(_ value: Bool) {
        store.update { $0.analyticsOptIn = value }
        if value {
            // Wire real keys via Info.plist in Phase 2.
            analytics.initialize(apiKey: "<configure-in-info-plist>",
                                 host: "https://us.i.posthog.com")
        } else {
            analytics.optOutAndShutdown()
        }
    }

    // MARK: - Destructive actions

    /// Camila P0-13: "Delete All My Data". UI must show a confirmation
    /// sheet (`showDeleteAllConfirm = true`) BEFORE calling this.
    func deleteAllMyData() {
        store.deleteAllUserData()
        analytics.optOutAndShutdown()
    }

    /// Maggie P1-19: replay tutorial flow.
    func replayTutorial() {
        // Tutorial is owned by HomeView via a `@AppStorage("onboarded")`
        // sentinel. Toggling it here causes HomeView to relaunch the
        // walkthrough on next appear.
        UserDefaults.standard.set(false, forKey: "onboarded")
    }

    /// Stylus calibration reset (future-compatibility seam).
    func resetStylusCalibration() {
        store.update { $0.stylusAutoDetected = false }
        // When adaptive calibration ships (Phase 3+), clear that bundle too.
    }
}
