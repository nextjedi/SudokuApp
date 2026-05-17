//
//  AccessibilityFlags.swift
//  Sudoku Brain Gym — live accessibility flags
//
//  @Observable wrapper around `UIAccessibility` flags so SwiftUI views can
//  react to live toggles (Reduce Transparency / Reduce Motion / VoiceOver /
//  Darker Colors) WITHOUT a relaunch.
//
//  This addresses iOS reviewer P1-A11Y-010: previously `UIAccessibility.is*`
//  reads were one-shot (at view body evaluation), so toggling
//  Settings.app → Accessibility didn't update glass surfaces until
//  the app was closed/reopened.
//
//  Usage:
//    @State private var flags = AccessibilityFlags()    // in App
//    .environment(flags)                                 // pass down
//    @Environment(AccessibilityFlags.self) var a11y      // read in leaves
//    view.adaptiveGlass(.regular, flags: a11y)
//
//  References:
//  - UIAccessibility:                                       https://developer.apple.com/documentation/uikit/uiaccessibility
//  - reduceTransparencyStatusDidChangeNotification:         https://developer.apple.com/documentation/uikit/uiaccessibility/reducetransparencystatusdidchangenotification
//  - reduceMotionStatusDidChangeNotification:               https://developer.apple.com/documentation/uikit/uiaccessibility/reducemotionstatusdidchangenotification
//  - voiceOverStatusDidChangeNotification:                  https://developer.apple.com/documentation/uikit/uiaccessibility/voiceoverstatusdidchangenotification
//

import UIKit
import Observation

@MainActor
@Observable
final class AccessibilityFlags {

    // MARK: - Flags

    var reduceTransparency: Bool = UIAccessibility.isReduceTransparencyEnabled
    var reduceMotion: Bool       = UIAccessibility.isReduceMotionEnabled
    var voiceOver: Bool          = UIAccessibility.isVoiceOverRunning
    var darkerSystemColors: Bool = UIAccessibility.isDarkerSystemColorsEnabled
    var boldText: Bool           = UIAccessibility.isBoldTextEnabled
    var differentiateWithoutColor: Bool =
        UIAccessibility.shouldDifferentiateWithoutColor

    // MARK: - Init

    init() {
        let nc = NotificationCenter.default
        nc.addObserver(forName: UIAccessibility.reduceTransparencyStatusDidChangeNotification,
                       object: nil, queue: .main) { [weak self] _ in
            self?.reduceTransparency = UIAccessibility.isReduceTransparencyEnabled
        }
        nc.addObserver(forName: UIAccessibility.reduceMotionStatusDidChangeNotification,
                       object: nil, queue: .main) { [weak self] _ in
            self?.reduceMotion = UIAccessibility.isReduceMotionEnabled
        }
        nc.addObserver(forName: UIAccessibility.voiceOverStatusDidChangeNotification,
                       object: nil, queue: .main) { [weak self] _ in
            self?.voiceOver = UIAccessibility.isVoiceOverRunning
        }
        nc.addObserver(forName: UIAccessibility.darkerSystemColorsStatusDidChangeNotification,
                       object: nil, queue: .main) { [weak self] _ in
            self?.darkerSystemColors = UIAccessibility.isDarkerSystemColorsEnabled
        }
        nc.addObserver(forName: UIAccessibility.boldTextStatusDidChangeNotification,
                       object: nil, queue: .main) { [weak self] _ in
            self?.boldText = UIAccessibility.isBoldTextEnabled
        }
        nc.addObserver(forName: UIAccessibility.differentiateWithoutColorDidChangeNotification,
                       object: nil, queue: .main) { [weak self] _ in
            self?.differentiateWithoutColor =
                UIAccessibility.shouldDifferentiateWithoutColor
        }
    }

    // MARK: - Bootstrap

    /// Called once from `SudokuApp.init`. Currently a no-op besides forcing
    /// the singleton to be touched early, but kept as a seam for tests +
    /// future static observers.
    static func bootstrap() {
        _ = UIAccessibility.isReduceTransparencyEnabled
    }

    // MARK: - Announcement helpers

    /// Post a polite VoiceOver announcement. Used by GameViewModel on mistake
    /// (Lena P0-A11Y-002), hint, win, and proximity-pause events.
    func announce(_ text: String) {
        UIAccessibility.post(notification: .announcement, argument: text)
    }
}
