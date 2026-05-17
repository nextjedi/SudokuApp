//
//  SensorService.swift
//  Sudoku Brain Gym — CoreMotion + UIDevice proximity
//
//  Provides:
//   - Tilt parallax via `CMMotionManager.startDeviceMotionUpdates` at 30 Hz.
//   - Proximity auto-pause via `UIDevice.proximityState` — iPhone ONLY.
//
//  Critical constraints:
//   - **NEVER** call `isProximityMonitoringEnabled = true` on iPad. The setter
//     silently no-ops, but we still guard explicitly so the SettingsView can
//     surface "Not available on iPad" copy. (See `06-stylus-sensors-privacy-
//     review.md` §10.2 and `05-ios-liquid-glass-review.md` §11.)
//   - Motion updates run at 30 Hz (1/30 s). Higher rates wake the high-rate
//     motion service unnecessarily.
//   - All sensors stop on `scenePhase != .active`.
//   - Reduce Motion or High Contrast in AppSettings BOTH suppress tilt
//     start (Lena P1-A11Y-015: VoiceOver should also suppress, applied in
//     GameViewModel).
//
//  NSMotionUsageDescription MUST be present in Info.plist or
//  `startDeviceMotionUpdates` will terminate the app. We add the key in
//  project.yml.
//
//  Privacy invariants (Camila P0):
//   - Sensor values NEVER written to UserDefaults.
//   - Sensor values NEVER sent to any analytics endpoint.
//   - Held in @Observable in-memory state only.
//
//  References:
//  - CMMotionManager:                https://developer.apple.com/documentation/coremotion/cmmotionmanager
//  - startDeviceMotionUpdates:       https://developer.apple.com/documentation/coremotion/cmmotionmanager/startdevicemotionupdates()
//  - UIDevice proximityState:        https://developer.apple.com/documentation/uikit/uidevice/proximitystate
//  - isProximityMonitoringEnabled:   https://developer.apple.com/documentation/uikit/uidevice/isproximitymonitoringenabled
//  - NSMotionUsageDescription:       https://developer.apple.com/documentation/bundleresources/information-property-list/nsmotionusagedescription
//

import Foundation
import CoreMotion
import UIKit
import Observation

@MainActor
@Observable
final class SensorService {

    static let shared = SensorService()

    // MARK: - Published outputs

    /// Pitch in radians, smoothed. 0 = device flat.
    private(set) var pitch: Double = 0
    /// Roll in radians, smoothed. 0 = device flat.
    private(set) var roll: Double = 0
    /// Proximity state. `true` means near (e.g., to ear) on iPhone.
    private(set) var isNear: Bool = false

    // MARK: - Availability flags (read by SettingsView for row-disabling)

    /// True if `CMMotionManager.isDeviceMotionAvailable`.
    var isTiltAvailable: Bool { manager.isDeviceMotionAvailable }

    /// True ONLY on iPhone. iPad and iPod don't expose this sensor.
    /// (Tested via `isProximityMonitoringEnabled` round-trip per Apple docs.)
    var isProximityAvailable: Bool {
        // Round-trip the setter to ask the system. We restore the value
        // straight away so we don't leave proximity monitoring on.
        guard UIDevice.current.userInterfaceIdiom == .phone else { return false }
        let original = UIDevice.current.isProximityMonitoringEnabled
        UIDevice.current.isProximityMonitoringEnabled = true
        let supported = UIDevice.current.isProximityMonitoringEnabled
        UIDevice.current.isProximityMonitoringEnabled = original
        return supported
    }

    // MARK: - Private

    private let manager = CMMotionManager()
    private var isProximityActive = false

    private init() {}

    // MARK: - Tilt

    /// Start device-motion updates at 30 Hz. Idempotent.
    /// Caller is responsible for honoring Reduce Motion + user pref + VoiceOver.
    func startTilt() {
        guard manager.isDeviceMotionAvailable else { return }
        guard !manager.isDeviceMotionActive else { return }
        // 30 Hz — modest battery, smooth parallax. Per Apple's energy-best-
        // practices guide, pick the longest acceptable interval.
        manager.deviceMotionUpdateInterval = 1.0 / 30.0
        manager.startDeviceMotionUpdates(to: .main) { [weak self] motion, _ in
            guard let self, let m = motion else { return }
            // Smooth via simple EMA (alpha = 0.4). This dampens jitter
            // without adding visible lag.
            let alpha = 0.4
            self.pitch = alpha * m.attitude.pitch + (1 - alpha) * self.pitch
            self.roll  = alpha * m.attitude.roll  + (1 - alpha) * self.roll
        }
    }

    func stopTilt() {
        if manager.isDeviceMotionActive {
            manager.stopDeviceMotionUpdates()
        }
        pitch = 0
        roll = 0
    }

    // MARK: - Proximity (iPhone only)

    /// Start proximity monitoring on iPhone. No-op on iPad.
    /// Returns `true` if monitoring was actually enabled.
    @discardableResult
    func startProximity() -> Bool {
        // *** Explicit iPad guard (P0 from convergent finding) ***
        guard UIDevice.current.userInterfaceIdiom == .phone else {
            isProximityActive = false
            return false
        }
        UIDevice.current.isProximityMonitoringEnabled = true
        // The setter silently no-ops on unsupported hardware. Round-trip
        // verify.
        guard UIDevice.current.isProximityMonitoringEnabled else {
            isProximityActive = false
            return false
        }
        isProximityActive = true
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleProximityChange),
            name: UIDevice.proximityStateDidChangeNotification,
            object: nil
        )
        return true
    }

    func stopProximity() {
        NotificationCenter.default.removeObserver(
            self,
            name: UIDevice.proximityStateDidChangeNotification,
            object: nil
        )
        if UIDevice.current.userInterfaceIdiom == .phone {
            UIDevice.current.isProximityMonitoringEnabled = false
        }
        isProximityActive = false
        isNear = false
    }

    @objc private func handleProximityChange() {
        isNear = UIDevice.current.proximityState
    }

    // MARK: - Lifecycle convenience

    /// Call from GameView's `onDisappear`/`scenePhase` handler to stop
    /// EVERY sensor regardless of which started.
    func stopAll() {
        stopTilt()
        stopProximity()
    }
}
