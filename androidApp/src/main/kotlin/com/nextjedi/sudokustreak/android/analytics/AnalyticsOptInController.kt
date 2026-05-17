package com.nextjedi.sudokustreak.android.analytics

import com.nextjedi.sudokustreak.domain.analytics.AnalyticsEvent
import com.nextjedi.sudokustreak.domain.analytics.AnalyticsService

/**
 * Thin policy layer that converts a boolean `analyticsOptIn` change from the Settings
 * ViewModel into the correct lifecycle calls on [AnalyticsService].
 *
 * Lives outside of `SettingsViewModel` so:
 *  - the Settings UI agent can wire this in via a single line
 *    (`AnalyticsOptInController(analyticsService).apply(newValue)`), and
 *  - the opt-in -> initialize / opt-out -> shutdown contract is unit-testable in
 *    isolation (see `AnalyticsOptInFlowTest`).
 *
 * Ordering invariants:
 *  - On opt-IN: `initialize()` is called FIRST, then [AnalyticsEvent.AnalyticsOptedIn]
 *    is tracked. This way the opt-in event itself is captured (it would be buffered
 *    and then deleted if we tracked-then-initialized).
 *  - On opt-OUT: [AnalyticsEvent.AnalyticsOptedOut] is tracked FIRST while the SDK is
 *    still up, then `shutdown()`. After shutdown, nothing else can be transmitted.
 */
class AnalyticsOptInController(
    private val analyticsService: AnalyticsService
) {
    /**
     * Apply the new opt-in state. Idempotent: re-applying the same value is safe
     * (the underlying [AnalyticsService.initialize] / [AnalyticsService.shutdown] are
     * themselves idempotent).
     */
    fun apply(optIn: Boolean) {
        if (optIn) {
            // initialize() may no-op if host is unconfigured; the AnalyticsOptedIn event
            // will then be buffered to disk and deleted on a later successful init —
            // which is the intended privacy behavior. No special handling needed here.
            analyticsService.initialize()
            analyticsService.track(AnalyticsEvent.AnalyticsOptedIn)
        } else {
            // Track BEFORE shutdown so the SDK still has a live transport for the event.
            analyticsService.track(AnalyticsEvent.AnalyticsOptedOut)
            analyticsService.shutdown()
        }
    }
}
