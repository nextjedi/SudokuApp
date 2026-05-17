//
//  AppSettings.swift
//  Sudoku Brain Gym — Swift mirror of the shared KMP AppSettings data class
//
//  This struct must stay in shape parity with:
//      shared/src/commonMain/kotlin/.../domain/settings/AppSettings.kt
//
//  Versioning rule (REVAMP_PLAN §3, P0-2):
//   - `schemaVersion` is the FIRST field.
//   - When KMP bumps schemaVersion (e.g. 1 → 2), this file must update too,
//     and `AppSettingsStore.migrate(...)` must handle the upgrade.
//
//  This struct intentionally does NOT import the `shared` framework. The
//  bridge happens in `KMPEngineAdapter` at boundary; mirroring the data
//  class on each side avoids passing Kotlin objects across the
//  Swift/Kotlin boundary at every read (per `05-ios-liquid-glass-review.md`
//  §4 "Wrong #4 — holding Kotlin objects across the boundary").
//
//  References:
//  - Codable:                https://developer.apple.com/documentation/swift/codable
//  - JSONEncoder/Decoder:    https://developer.apple.com/documentation/foundation/jsondecoder
//

import Foundation
import SwiftUI

// MARK: - Enums (mirror KMP)

enum ThemeMode: String, Codable, CaseIterable {
    case system
    case light
    case dark
    case amoled
}

enum StylusMode: String, Codable, CaseIterable {
    case auto       // pencil works when paired; finger still works
    case always     // pencil-only inside the cell (finger blocked)
    case never      // pencil disabled (finger only)
}

enum MistakeLimit: Int, Codable, CaseIterable {
    case one   = 1
    case three = 3
    case five  = 5
    case unlimited = 999
}

enum HintDepth: Int, Codable, CaseIterable {
    case minimal = 1
    case basic   = 2
    case medium  = 3
    case deep    = 4
    case full    = 5
}

enum ConfidenceTier: String, Codable, CaseIterable {
    case low     // 0.60 threshold
    case medium  // 0.75 threshold
    case high    // 0.90 threshold

    var floatValue: Float {
        switch self {
        case .low:    return 0.60
        case .medium: return 0.75
        case .high:   return 0.90
        }
    }
}

// MARK: - The struct

/// Mirrors KMP `AppSettings`. Same field names (camelCase on both sides
/// because Kotlin uses camelCase).
struct AppSettings: Codable, Equatable {

    // -- Schema versioning (PC-5, P0-2) ------------------------------------
    /// Always FIRST. Bump in lockstep with the KMP `AppSettings.schemaVersion`.
    var schemaVersion: Int = 1

    // -- Gameplay ----------------------------------------------------------
    var mistakeLimit: MistakeLimit = .three
    var autoCheckMistakes: Bool = true
    var highlightPeers: Bool = true
    var highlightSameValue: Bool = true
    var autoEraseCandidates: Bool = true
    var showTimer: Bool = true

    // -- Stylus & Pencil ---------------------------------------------------
    var stylusMode: StylusMode = .auto
    var stylusConfidence: ConfidenceTier = .medium
    var stylusPressureBoldNotes: Bool = true
    var stylusWristRejection: Bool = true
    /// Set on first observed `UITouch.type == .pencil`. Persisted so we can
    /// distinguish "user has a pencil but it's not currently paired" vs
    /// "user has never paired a pencil" (the latter hides the Test Pencil row).
    var stylusAutoDetected: Bool = false
    /// Stroke-end debounce in milliseconds. Per Stylus-reviewer §5, default
    /// 400 ms (not 80 ms — multi-stroke 4/5/7 with crossbar need ≥250 ms).
    var stylusEndOfStrokeMs: Int = 400

    // -- Sensors -----------------------------------------------------------
    var proximityAutoPauseEnabled: Bool = false   // off by default; opt-in
    var tiltParallaxEnabled: Bool = false         // off by default
    /// On iPad we ignore `proximityAutoPauseEnabled` and show row disabled
    /// with "Not available on iPad — no earpiece sensor" hint.

    // -- Presentation ------------------------------------------------------
    var themeMode: ThemeMode = .system
    var colorBlindMode: AppColorBlindMode = .none
    var highContrast: Bool = false
    var reduceMotion: Bool = false                // mirrors UIAccessibility flag
                                                  // unless user overrides

    // -- Solver & Hints ----------------------------------------------------
    var hintDepth: HintDepth = .medium
    var explainTechniques: Bool = true

    // -- Accessibility (extras beyond reduceMotion / highContrast) ---------
    var dynamicTypeDigits: Bool = true            // grid digits respect Dynamic Type
    var verboseAnnouncements: Bool = true         // includes "candidates 2 4 9"
    var keyboardShortcuts: Bool = true            // iPad/Mac Catalyst hardware kb

    // -- Audio & Haptics ---------------------------------------------------
    var soundEnabled: Bool = true
    var hapticsEnabled: Bool = true
    var musicEnabled: Bool = false

    // -- Data & Privacy ----------------------------------------------------
    var analyticsOptIn: Bool = false              // OFF by default (Camila P0-14)
    var crashReportingOptIn: Bool = false         // OFF by default

    // MARK: - Derived

    /// SwiftUI `preferredColorScheme` derived from `themeMode`. AMOLED maps
    /// to `.dark` because SwiftUI doesn't expose a separate AMOLED scheme;
    /// the AMOLED-specific surface swap happens inside `AppColors`.
    var preferredColorScheme: ColorScheme? {
        switch themeMode {
        case .system: return nil
        case .light:  return .light
        case .dark, .amoled: return .dark
        }
    }
}
