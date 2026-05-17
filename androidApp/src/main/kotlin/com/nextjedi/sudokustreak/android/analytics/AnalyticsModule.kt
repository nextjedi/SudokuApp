package com.nextjedi.sudokustreak.android.analytics

import android.content.Context
import com.nextjedi.sudoku.BuildConfig
import com.nextjedi.sudokustreak.domain.analytics.AnalyticsService

/**
 * Process-wide accessor for the single [AnalyticsService] instance.
 *
 * Why a hand-rolled singleton instead of Hilt / Koin?
 *  - Wave-1 has zero DI framework wired in (see REVAMP_PLAN §0).
 *  - Constructing the service is cheap; only `initialize()` is expensive, and that is
 *    explicitly NOT called here (callers in `SettingsViewModel` invoke it only after
 *    the user flips `analyticsOptIn = true`).
 *  - A tiny static accessor matches the iOS `PostHogService` shape (singleton inside
 *    the `AppSettings` `didSet`) so the two platforms are symmetric.
 *
 * The API key + host come from [BuildConfig] which is wired in
 * `androidApp/build.gradle.kts` via `buildConfigField("String", "POSTHOG_HOST", …)` from
 * `gradle.properties`. Defaults are placeholders that [PostHogAnalyticsService.initialize]
 * refuses to honor — see its kdoc for the "never accidentally hit posthog.com" guarantee.
 *
 * Test seam: [overrideForTests] installs a caller-supplied implementation (typically a
 * MockK mock or a `NoOpAnalyticsService`). Always [reset] in test `@After`.
 */
object AnalyticsModule {

    @Volatile
    private var instance: AnalyticsService? = null

    /**
     * Get or lazily construct the process-wide [AnalyticsService]. Does NOT call
     * [AnalyticsService.initialize] — that only happens after opt-in.
     */
    fun get(context: Context): AnalyticsService {
        val cached = instance
        if (cached != null) return cached
        return synchronized(this) {
            instance ?: PostHogAnalyticsService(
                context = context.applicationContext,
                apiKey = BuildConfig.POSTHOG_API_KEY,
                host = BuildConfig.POSTHOG_HOST
            ).also { instance = it }
        }
    }

    /**
     * Test seam. Replaces the cached instance with [service]. Production code MUST NOT
     * call this. Tests SHOULD call [reset] in `@After` to avoid leaking state across tests.
     */
    internal fun overrideForTests(service: AnalyticsService) {
        synchronized(this) { instance = service }
    }

    /** Test seam. Clears the cached instance. */
    internal fun reset() {
        synchronized(this) { instance = null }
    }
}
