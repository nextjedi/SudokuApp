package com.nextjedi.sudokustreak.android.analytics

/**
 * Privacy-respecting analytics facade.
 *
 * **Locked decision §7 (test-plan/00-SYNTHESIS.md / REVAMP_PLAN.md §3):**
 * - `AppSettings.analyticsOptIn` defaults to **false**.
 * - No SDK is instantiated, no tracking IDs created, and no events are sent until
 *   the user explicitly opts in via Settings → Data & Privacy → Analytics.
 * - Backed by **self-hosted PostHog** (no data leaves our infra).
 *
 * This interface is the contract the Settings agent ships in Wave 2 — the PostHog
 * agent provides the real implementation in their commit. Until then, [NoOpAnalyticsService]
 * is wired so the toggle still flips state but no network calls occur.
 *
 * ## Lifecycle rules
 *
 * - [initialize] is called exactly once, lazily, the first time the user opts in.
 *   Implementations MUST be idempotent — calling it twice while already initialised
 *   is a no-op.
 * - [shutdown] is called when the user opts out OR when the "Delete All My Data" flow
 *   runs (Camila's privacy audit, P0-13). Implementations MUST flush + close the SDK,
 *   purge all in-memory state, and ensure no further calls happen until [initialize]
 *   is invoked again.
 * - [isInitialized] is exposed for unit tests; production code never needs to read it.
 *
 * ## Why a separate interface instead of touching AppSettings directly?
 *
 * Pure separation of concerns: the repository's only job is to persist a value; deciding
 * whether to spin up an SDK lives at the application layer.
 */
interface AnalyticsService {

    /**
     * True once [initialize] has run and [shutdown] has NOT been called since.
     * Test-only — UI never reads this; UI reads [com.nextjedi.sudokustreak.domain.settings.AppSettings.analyticsOptIn].
     */
    val isInitialized: Boolean

    /**
     * Bring up the PostHog SDK (or the equivalent self-hosted client). Called exactly
     * once when the user flips the Analytics toggle ON.
     *
     * Implementations MUST:
     * - be safe to call from a coroutine (settings update site is `viewModelScope.launch`),
     * - be idempotent (a second call while initialised is a no-op),
     * - never block the caller for more than a few milliseconds — defer network handshakes
     *   to a background thread.
     */
    fun initialize()

    /**
     * Tear down the SDK. Called when the user flips the toggle OFF or runs Delete All
     * My Data. Implementations MUST flush queued events, close pipelines, and reset
     * any in-memory state.
     */
    fun shutdown()
}

/**
 * Default safe implementation used until the PostHog agent ships the real one.
 *
 * - Tracks the in-memory boolean so unit tests can verify `initialize()` / `shutdown()`
 *   are called in the right order.
 * - Performs zero IO so flipping the toggle never blocks or warms a cold SDK.
 *
 * **DO NOT** delete this class when PostHog lands — keep it for tests + debug builds
 * where we don't want network IO during instrumentation.
 */
class NoOpAnalyticsService : AnalyticsService {

    @Volatile
    private var initialized: Boolean = false

    override val isInitialized: Boolean
        get() = initialized

    override fun initialize() {
        // Idempotent — multiple initialise calls collapse to one.
        initialized = true
    }

    override fun shutdown() {
        initialized = false
    }
}
