//
//  HapticsService.swift
//  Sudoku Brain Gym — UIKit haptic wrappers
//
//  Thin wrapper around `UIImpactFeedbackGenerator` and `UINotificationFeedback-
//  Generator`. Centralized so:
//   - We respect `AppSettings.hapticsEnabled` in ONE place.
//   - Lena P0-A11Y-002 mistake-feedback parity: when haptics are OFF we don't
//     silently swallow — the GameViewModel posts the announcement+audio
//     anyway, so the user still has feedback parity.
//
//  References:
//  - UIImpactFeedbackGenerator:        https://developer.apple.com/documentation/uikit/uiimpactfeedbackgenerator
//  - UINotificationFeedbackGenerator:  https://developer.apple.com/documentation/uikit/uinotificationfeedbackgenerator
//  - UISelectionFeedbackGenerator:     https://developer.apple.com/documentation/uikit/uiselectionfeedbackgenerator
//

import UIKit

@MainActor
final class HapticsService {

    static let shared = HapticsService()

    /// Toggled by SettingsViewModel when the user changes `hapticsEnabled`.
    var isEnabled: Bool = true

    private let impactLight  = UIImpactFeedbackGenerator(style: .light)
    private let impactMedium = UIImpactFeedbackGenerator(style: .medium)
    private let impactHeavy  = UIImpactFeedbackGenerator(style: .heavy)
    private let selection    = UISelectionFeedbackGenerator()
    private let notification = UINotificationFeedbackGenerator()

    private init() {
        // Pre-prepare so the first tap is responsive.
        impactLight.prepare()
        impactMedium.prepare()
    }

    // MARK: - Common events

    func tap()           { if isEnabled { impactLight.impactOccurred() } }
    func tapMedium()     { if isEnabled { impactMedium.impactOccurred() } }
    func tapHeavy()      { if isEnabled { impactHeavy.impactOccurred() } }
    func selectionTick() { if isEnabled { selection.selectionChanged() } }

    func success() {
        if isEnabled { notification.notificationOccurred(.success) }
    }
    func warning() {
        if isEnabled { notification.notificationOccurred(.warning) }
    }
    func error() {
        if isEnabled { notification.notificationOccurred(.error) }
    }
}
