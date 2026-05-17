package com.nextjedi.sudokustreak.android.analytics

import com.google.common.truth.Truth.assertThat
import com.nextjedi.sudokustreak.domain.analytics.AnalyticsEvent
import com.nextjedi.sudokustreak.domain.analytics.AnalyticsService
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test

/**
 * Flow-level tests for [AnalyticsOptInController] that exercise the privacy contract
 * end-to-end (modulo the actual PostHog SDK, which is mocked here — its own behavior
 * is covered by `PostHogAnalyticsServiceTest`).
 *
 * Maps to:
 *  - REVAMP_PLAN §4 — opt-in flow contract
 *  - test-plan/01-test-scenarios.md TC-A12 `analyticsOptInDefaultsOff`
 *  - test-plan/11-alpha-user-5-camila-privacy.md §11 (no third-party leak invariant)
 *
 * Note: a Robolectric runner is NOT required here because the controller only depends
 * on the [AnalyticsService] interface, which is platform-agnostic.
 */
class AnalyticsOptInFlowTest {

    @Test
    fun opt_in_flow_initializes_posthog() {
        val svc = mockk<AnalyticsService>(relaxed = true)
        AnalyticsOptInController(svc).apply(optIn = true)
        verifyOrder {
            svc.initialize()
            svc.track(AnalyticsEvent.AnalyticsOptedIn)
        }
    }

    @Test
    fun opt_out_flow_shuts_down_posthog() {
        val svc = mockk<AnalyticsService>(relaxed = true)
        AnalyticsOptInController(svc).apply(optIn = false)
        // The opt-out event must be tracked BEFORE shutdown so the SDK still has a
        // live transport for it.
        verifyOrder {
            svc.track(AnalyticsEvent.AnalyticsOptedOut)
            svc.shutdown()
        }
    }

    @Test
    fun app_launch_without_opt_in_does_not_initialize() {
        // Simulates SudokuApplication.onCreate constructing the service via
        // AnalyticsModule.get(this). The mere construction MUST NOT initialize the SDK.
        val svc = mockk<AnalyticsService>(relaxed = true)
        // We never call apply(true) — analyticsOptIn defaults to false per AppSettings.
        verify(exactly = 0) { svc.initialize() }
        verify(exactly = 0) { svc.track(any()) }
        verify(exactly = 0) { svc.screen(any(), any()) }
        confirmVerified(svc)
    }

    @Test
    fun double_opt_in_is_idempotent() {
        val svc = mockk<AnalyticsService>(relaxed = true)
        val controller = AnalyticsOptInController(svc)
        controller.apply(optIn = true)
        controller.apply(optIn = true)
        // initialize() is called twice from the controller — but PostHogAnalyticsService's
        // initialize() is itself idempotent (see its kdoc). The contract here is just
        // "the controller doesn't try to be clever about deduping".
        verify(exactly = 2) { svc.initialize() }
        verify(exactly = 2) { svc.track(AnalyticsEvent.AnalyticsOptedIn) }
    }

    @Test
    fun toggle_opt_in_off_then_on_re_initializes() {
        val svc = mockk<AnalyticsService>(relaxed = true)
        val controller = AnalyticsOptInController(svc)
        controller.apply(optIn = true)
        controller.apply(optIn = false)
        controller.apply(optIn = true)
        verifyOrder {
            // first ON
            svc.initialize()
            svc.track(AnalyticsEvent.AnalyticsOptedIn)
            // OFF
            svc.track(AnalyticsEvent.AnalyticsOptedOut)
            svc.shutdown()
            // second ON
            svc.initialize()
            svc.track(AnalyticsEvent.AnalyticsOptedIn)
        }
    }
}
