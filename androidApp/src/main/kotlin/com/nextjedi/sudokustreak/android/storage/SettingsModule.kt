package com.nextjedi.sudokustreak.android.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.nextjedi.sudokustreak.android.analytics.AnalyticsService
import com.nextjedi.sudokustreak.android.analytics.NoOpAnalyticsService
import com.nextjedi.sudokustreak.android.stats.PreferencesStatsResetter
import com.nextjedi.sudokustreak.android.stats.StatsResetter
import com.nextjedi.sudokustreak.domain.settings.AppSettingsRepository

/**
 * Hand-rolled DI module for the settings-screen dependency graph.
 *
 * We deliberately don't pull in Hilt / Dagger here because:
 * 1. The android app currently uses `ViewModelProvider.Factory` directly (see
 *    [com.nextjedi.sudokustreak.android.DataStoreViewModelFactory]) — adding a DI graph
 *    is out of scope for this agent.
 * 2. The set of singletons is tiny (3 today): the settings repo, the stats resetter,
 *    the analytics service.
 *
 * Each singleton is lazily initialised on first access and reused thereafter. The
 * double-checked locking pattern (`synchronized` after a quick null check) keeps the
 * hot path lock-free.
 *
 * ## How call sites use this module
 *
 * ```
 * // MainActivity.kt
 * val repo = SettingsModule.appSettingsRepository(this)
 * val statsResetter = SettingsModule.statsResetter(this, dataStore)
 * val analytics = SettingsModule.analyticsService()
 * val vm = SettingsViewModel(repo, statsResetter, analytics)
 * ```
 *
 * ## Test override
 *
 * Unit tests construct their own `FakeAppSettingsRepository` / `FakeStatsResetter` /
 * `NoOpAnalyticsService` and bypass this module entirely. Production code is the only
 * caller — there's no need to expose a "test override" hook.
 */
object SettingsModule {

    @Volatile
    private var repoInstance: AppSettingsRepository? = null

    @Volatile
    private var analyticsInstance: AnalyticsService? = null

    /**
     * Returns the singleton [AppSettingsRepository] backed by a typed
     * `DataStore<AppSettings>` rooted at the application's `filesDir`.
     *
     * Always pass any [Context] — we capture only `applicationContext` internally so
     * passing an Activity is safe.
     */
    fun appSettingsRepository(context: Context): AppSettingsRepository {
        // Fast path — no lock.
        repoInstance?.let { return it }
        return synchronized(this) {
            repoInstance ?: AppSettingsDataStoreRepository(context.applicationContext)
                .also { repoInstance = it }
        }
    }

    /**
     * Returns a fresh [StatsResetter] each time. Stats reset is a transactional verb,
     * not a cached service — keeping it stateless avoids leaking `DataStore<Preferences>`
     * references through a singleton field.
     */
    fun statsResetter(
        @Suppress("UNUSED_PARAMETER") context: Context,
        dataStore: DataStore<Preferences>,
    ): StatsResetter = PreferencesStatsResetter(dataStore)

    /**
     * Returns the singleton [AnalyticsService]. Defaults to [NoOpAnalyticsService] until
     * the PostHog agent wires their real implementation through this module.
     *
     * When PostHog lands, swap this body to construct the production client; existing
     * callers don't need to change.
     */
    fun analyticsService(): AnalyticsService {
        analyticsInstance?.let { return it }
        return synchronized(this) {
            analyticsInstance ?: NoOpAnalyticsService().also { analyticsInstance = it }
        }
    }

    /**
     * **Test-only.** Clears the module's singleton cache. Production code MUST NOT call
     * this — flipping a singleton at runtime breaks anyone holding the old reference.
     *
     * Unit / instrumented tests that need a clean module state between runs should call
     * this from an `@After` hook.
     */
    internal fun resetForTesting() {
        synchronized(this) {
            repoInstance = null
            analyticsInstance = null
        }
    }
}
