//
//  GameViewModel.swift
//  Sudoku Brain Gym — Game screen state
//
//  - @Observable: SwiftUI iOS 17+ pattern (preferred over ObservableObject).
//  - @MainActor at TYPE level: required because we mutate @Observable
//    properties from KMP flow callbacks. Kotlin/Native delivers on whatever
//    dispatcher it likes; we MUST hop to main.
//
//  Mistake-feedback parity (Lena P0-A11Y-002):
//   - Visual: color + (in High Contrast / color-blind) dashed border.
//   - Audio: error tone if `soundEnabled`.
//   - Haptic: `error()` notification haptic if `hapticsEnabled`.
//   - Spoken: UIAccessibility.post(.announcement, "Cell R3C5: wrong value 7.
//             2 of 3 mistakes used.") — fires UNCONDITIONALLY (regardless
//             of sound/haptic settings) so a blind user with both off still
//             receives feedback via VoiceOver.
//
//  References:
//  - @Observable:               https://developer.apple.com/documentation/observation/observable()
//  - @MainActor isolation:      https://developer.apple.com/documentation/swift/mainactor
//  - UIAccessibility.post:      https://developer.apple.com/documentation/uikit/uiaccessibility/1615173-post
//

import Foundation
import Observation
import PencilKit
import UIKit

@MainActor
@Observable
final class GameViewModel {

    // MARK: - Published state

    private(set) var snapshot: BoardSnapshot = .empty
    private(set) var lastHint: HintResult?
    private(set) var isThinking: Bool = false
    private(set) var recognizerStatus: String?    // "Recognizing…", "Wrote 5", etc

    /// Pending pencil drawing (per-cell ephemeral). Cleared on commit or
    /// cell deselect.
    var pendingDrawing: PKDrawing = PKDrawing()

    /// Notes-mode toggle. UI-only state — placed digits switch to candidate
    /// vs main value based on this flag.
    var notesMode: Bool = false

    // MARK: - Dependencies

    private let engine: KMPEngineAdapter
    private let stylus: StylusInputService
    private let sensors: SensorService
    private let haptics: HapticsService
    private let a11y: AccessibilityFlags
    private let settings: AppSettingsStore

    private var subscription: Cancellable?

    // MARK: - Init

    init(
        engine: KMPEngineAdapter = .shared,
        stylus: StylusInputService = StylusInputService(),
        sensors: SensorService = .shared,
        haptics: HapticsService = .shared,
        a11y: AccessibilityFlags = AccessibilityFlags(),
        settings: AppSettingsStore = .shared
    ) {
        self.engine = engine
        self.stylus = stylus
        self.sensors = sensors
        self.haptics = haptics
        self.a11y = a11y
        self.settings = settings

        // Subscribe to engine snapshots. The handler runs on MainActor
        // because we use `@MainActor (BoardSnapshot) -> Void`.
        self.subscription = engine.observeBoard { [weak self] snap in
            self?.snapshot = snap
        }
    }

    deinit {
        subscription?.cancel()
    }

    // MARK: - Lifecycle

    func onAppear() {
        // Start sensors only on the Game screen, only with the user opted-in.
        let s = settings.settings
        if s.tiltParallaxEnabled,
           !a11y.reduceMotion,
           !a11y.voiceOver           // Lena P1-A11Y-015: VoiceOver suppresses
        {
            sensors.startTilt()
        }
        if s.proximityAutoPauseEnabled, sensors.isProximityAvailable {
            sensors.startProximity()
        }
    }

    func onDisappear() {
        sensors.stopAll()
    }

    // MARK: - Selection

    func select(cell coord: CellCoord) {
        // Update local snapshot's selectedCell. The engine doesn't own
        // selection (it's a pure UI concern), so we synthesize a new
        // BoardSnapshot mirroring the previous engine state.
        snapshot = BoardSnapshot(
            cells: snapshot.cells,
            mistakes: snapshot.mistakes,
            mistakeLimit: snapshot.mistakeLimit,
            score: snapshot.score,
            elapsedSeconds: snapshot.elapsedSeconds,
            difficulty: snapshot.difficulty,
            phase: snapshot.phase,
            selectedCell: coord
        )
        haptics.selectionTick()
        pendingDrawing = PKDrawing()
    }

    // MARK: - Digit placement

    /// Place a digit at the currently selected cell.
    func place(digit: Int) async {
        guard let coord = snapshot.selectedCell else { return }
        if notesMode {
            snapshot = await engine.toggleCandidate(at: coord, digit: digit)
            haptics.tap()
            return
        }
        let result = await engine.place(at: coord, digit: digit)
        await deliverFeedback(for: result)
    }

    /// Clear the value of the currently selected cell.
    func clearSelectedCell() async {
        guard let coord = snapshot.selectedCell else { return }
        _ = await engine.clearCell(at: coord)
        haptics.tap()
    }

    /// Undo last move.
    func undo() async {
        _ = await engine.undo()
        haptics.tap()
    }

    /// Ask the engine for a hint.
    func requestHint() async {
        isThinking = true
        defer { isThinking = false }
        let hint = await engine.hint()
        lastHint = hint
        if let name = hint.techniqueName {
            a11y.announce("Hint: \(name).")
        }
    }

    // MARK: - Stylus

    /// Called when the user lifts the pencil after writing in a cell.
    /// Routes through the recognizer and, on success, places the digit.
    func commitPendingDrawing() async {
        guard let coord = snapshot.selectedCell else { return }
        let s = settings.settings
        stylus.confidenceThreshold = s.stylusConfidence.floatValue
        recognizerStatus = "Recognizing…"
        let result = await stylus.recognize(pendingDrawing)
        switch result {
        case let .recognized(digit, confidence, latencyMs):
            recognizerStatus = "Wrote \(digit) (\(Int(confidence * 100))%)"
            pendingDrawing = PKDrawing()
            // Important: confidence is informational — the recognizer
            // already gated by threshold.
            _ = await engine.place(at: coord, digit: digit)
            a11y.announce("Recognized digit \(digit).")
            // Light telemetry for in-house perf graphs (no transmission).
            _ = latencyMs
        case .unrecognized:
            recognizerStatus = "Couldn't read. Try again or tap a digit."
            a11y.announce("Couldn't read. Try writing more clearly or tap a digit.")
            haptics.warning()
        case let .error(reason):
            recognizerStatus = "Recognizer error: \(reason)"
        }
    }

    // MARK: - Mistake feedback (Lena P0-A11Y-002 — tri-modal)

    private func deliverFeedback(for result: MoveResult) async {
        switch result.kind {
        case .accepted:
            haptics.tap()
        case .mistake(let info):
            // Audio (respects soundEnabled)
            if settings.settings.soundEnabled {
                // System sound TBD via AVFoundation in Phase 2.
            }
            // Haptic (respects hapticsEnabled — see HapticsService.isEnabled)
            haptics.error()
            // Spoken announcement — UNCONDITIONAL, ensures parity for blind
            // users regardless of sound/haptic toggles.
            let msg = "Cell R\(info.coord.row + 1)C\(info.coord.col + 1): " +
                      "wrong value \(info.attemptedValue). " +
                      "\(info.mistakeIndex) of \(info.limit) mistakes used."
            a11y.announce(msg)
        case .ignored:
            // Quiet — given clues etc. Don't spam announcements.
            break
        }
    }
}
