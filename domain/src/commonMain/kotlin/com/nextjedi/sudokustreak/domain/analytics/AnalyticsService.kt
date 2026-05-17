package com.nextjedi.sudokustreak.domain.analytics

/**
 * KMP-shared analytics contract.
 *
 * Backed on Android by `PostHogAnalyticsService` (self-hosted PostHog) and on iOS by
 * `PostHogService.swift`. Per locked decision §18.7 in `REVAMP_PLAN.md` and the privacy
 * audit `test-plan/11-alpha-user-5-camila-privacy.md`, the implementation MUST:
 *
 * 1. **Never instantiate the underlying SDK before opt-in.** Construction of the service
 *    is allowed at app startup (so ViewModels can receive a non-null reference), but
 *    [initialize] must be a no-op until the user flips `AppSettings.analyticsOptIn = true`.
 * 2. **Never collect PII.** No email, name, phone, ad id, IMEI, Android ID, IDFA, IDFV.
 *    The distinct id is an anonymous UUID persisted to local storage on first init.
 * 3. **Buffer pre-opt-in events to disk only.** Any [track] call made while
 *    [isInitialized] is false writes a single JSON line to a rotating local log file.
 *    The buffer is **deleted** (never transmitted) when [initialize] succeeds — events
 *    recorded before consent must not be uploaded after consent.
 * 4. **Refuse to initialize when the self-hosted host is misconfigured.** A blank host
 *    or the placeholder `https://posthog.example.local` MUST short-circuit [initialize]
 *    to a no-op so a forgotten `gradle.properties` cannot accidentally hit posthog.com.
 *
 * Implementations are responsible for thread-safety of [initialize] / [shutdown] and for
 * not propagating exceptions from the underlying transport — analytics failures are
 * silent by design.
 */
interface AnalyticsService {
    /**
     * True once the underlying SDK has been set up and is allowed to transmit events.
     * Implementations must keep this `false` until [initialize] succeeds, and flip it
     * back to `false` on [shutdown].
     */
    val isInitialized: Boolean

    /**
     * Initialize the underlying analytics SDK. **Only call after** the user has opted
     * in via `AppSettings.analyticsOptIn = true`. Calling more than once is a no-op.
     *
     * Implementations MUST short-circuit (without instantiating the SDK) when the
     * configured self-hosted host is blank or equal to the placeholder
     * `https://posthog.example.local`.
     */
    fun initialize()

    /**
     * Tear down the underlying SDK and flush any in-memory state. Implementations MUST
     * NOT transmit buffered events after shutdown — they may either be discarded or
     * flushed to the local disk-only buffer (which is itself never transmitted).
     */
    fun shutdown()

    /**
     * Record a typed analytics event. When [isInitialized] is false, implementations
     * MUST write the event to the local disk-only buffer (see [AnalyticsService] kdoc).
     * Buffered events are deleted, never uploaded, on a subsequent [initialize] call.
     */
    fun track(event: AnalyticsEvent)

    /**
     * Record a screen-view event. No-op when [isInitialized] is false (screen events
     * are not buffered — they are unbounded and would dominate the on-disk buffer).
     */
    fun screen(name: String, properties: Map<String, String> = emptyMap())
}

/**
 * Closed set of analytics events. New event types MUST be added here (not as a generic
 * `String` `track()` call) so the privacy invariant test `no_pii_in_event_properties`
 * stays exhaustive — adding a new event surface forces the test to enumerate it and
 * fail the build if it carries PII.
 */
sealed class AnalyticsEvent(
    val name: String,
    val properties: Map<String, Any?> = emptyMap()
) {
    /** First time the app is launched after install. */
    object FirstLaunch : AnalyticsEvent("first_launch")

    /** A new game was started. Difficulty is one of "EASY" / "MEDIUM" / "HARD" / "EXPERT". */
    data class GameStarted(val difficulty: String) :
        AnalyticsEvent("game_started", mapOf("difficulty" to difficulty))

    /** A game was completed successfully. */
    data class GameCompleted(
        val difficulty: String,
        val timeSeconds: Int,
        val mistakes: Int
    ) : AnalyticsEvent(
        "game_completed",
        mapOf(
            "difficulty" to difficulty,
            "time_s" to timeSeconds,
            "mistakes" to mistakes
        )
    )

    /** Fired once per install when stylus input is first detected (auto-detect path). */
    object StylusFirstUse : AnalyticsEvent("stylus_first_use")

    /** A setting was changed. [key] is the field name on `AppSettings` (no value sent — that could leak PII via custom strings if the schema ever adds free-text fields). */
    data class SettingsChanged(val key: String) :
        AnalyticsEvent("settings_changed", mapOf("key" to key))

    /** User toggled analytics opt-in ON. Fired immediately after `initialize()` succeeds. */
    object AnalyticsOptedIn : AnalyticsEvent("analytics_opted_in")

    /** User toggled analytics opt-in OFF. Fired immediately before `shutdown()`. */
    object AnalyticsOptedOut : AnalyticsEvent("analytics_opted_out")
}

/**
 * Default implementation used in tests and as a fallback when the platform does not
 * supply a real implementation. Every method is a no-op; [isInitialized] is always
 * `false`. This is what consumers receive before opt-in.
 */
class NoOpAnalyticsService : AnalyticsService {
    override val isInitialized: Boolean = false
    override fun initialize() { /* no-op */ }
    override fun shutdown() { /* no-op */ }
    override fun track(event: AnalyticsEvent) { /* no-op */ }
    override fun screen(name: String, properties: Map<String, String>) { /* no-op */ }
}
