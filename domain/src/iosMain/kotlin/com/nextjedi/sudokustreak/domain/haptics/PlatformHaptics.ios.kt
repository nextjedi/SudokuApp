package com.nextjedi.sudokustreak.domain.haptics

/**
 * iOS actual for [PlatformHaptics].
 *
 * TODO actual: implement in Wave 2.
 *
 * Wave 2 outline:
 * - [tap]: `UIImpactFeedbackGenerator(style: .light).impactOccurred()`
 * - [success]: `UINotificationFeedbackGenerator().notificationOccurred(.success)`
 * - [warning]: `UINotificationFeedbackGenerator().notificationOccurred(.warning)`
 * - [error]: `UINotificationFeedbackGenerator().notificationOccurred(.error)`
 *
 * Reuse a single generator per type and call `prepare()` ahead of expected use to
 * avoid first-call latency on cold paths.
 */
actual class PlatformHaptics {
    actual fun tap() {
        // TODO actual: UIImpactFeedbackGenerator(.light) in Wave 2.
    }

    actual fun success() {
        // TODO actual: UINotificationFeedbackGenerator(.success) in Wave 2.
    }

    actual fun warning() {
        // TODO actual: UINotificationFeedbackGenerator(.warning) in Wave 2.
    }

    actual fun error() {
        // TODO actual: UINotificationFeedbackGenerator(.error) in Wave 2.
    }
}
