package com.nextjedi.sudokustreak.domain.sensor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * iOS actual for [SensorService].
 *
 * TODO actual: implement in Wave 2.
 *
 * Wave 2 outline (per REVAMP_PLAN.md §11):
 * - Proximity: `UIDevice.current.isProximityMonitoringEnabled = true` +
 *   `proximityStateDidChangeNotification`. Guard `userInterfaceIdiom == .phone`
 *   (iPad has no proximity sensor — silent no-op + UI "Not available on iPad").
 * - Ambient light: NO public API on iOS. [ambientLightEvents] returns an empty Flow;
 *   the Settings row is disabled with "Not available on this device" message.
 * - Tilt: `CMMotionManager.startDeviceMotionUpdates` → `attitude.roll/.pitch`.
 * - Stop on `didEnterBackground` (see TC-IS-S4).
 *
 * Requires `NSMotionUsageDescription` in Info.plist + `PrivacyInfo.xcprivacy`
 * declaration (P0-8 in test-plan/00-SYNTHESIS.md).
 */
actual class SensorService {
    actual fun proximityEvents(): Flow<ProximityEvent> {
        // TODO actual: UIDevice proximity bridge in Wave 2.
        return emptyFlow()
    }

    actual fun ambientLightEvents(): Flow<AmbientLightEvent> {
        // TODO actual: iOS has no public ambient light API — permanently empty Flow.
        return emptyFlow()
    }

    actual fun tiltEvents(): Flow<TiltEvent> {
        // TODO actual: CMMotionManager bridge in Wave 2.
        return emptyFlow()
    }

    actual fun start() {
        // TODO actual: UIDevice.isProximityMonitoringEnabled = true + CMMotionManager start.
    }

    actual fun stop() {
        // TODO actual: UIDevice.isProximityMonitoringEnabled = false + CMMotionManager stop.
    }
}
