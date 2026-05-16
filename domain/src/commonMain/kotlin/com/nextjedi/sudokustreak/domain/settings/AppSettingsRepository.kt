package com.nextjedi.sudokustreak.domain.settings

import kotlinx.coroutines.flow.Flow

/**
 * Reactive repository for [AppSettings].
 *
 * ## Contract
 *
 * - **All reads happen via [flow] or [current].** Never read settings synchronously
 *   on the main thread; the write path is `suspend` to keep DataStore off Main.
 * - **All writes happen via [update].** Pass a pure transform; the implementation
 *   is responsible for atomic read-modify-write so concurrent updates do not lose data.
 * - **[reset] restores defaults.** Used by Settings → Data & Privacy → "Delete All My Data"
 *   (P0-13 in test-plan/00-SYNTHESIS.md).
 *
 * Platform implementations live in Wave 2:
 * - Android: `DataStoreAppSettingsRepository` backed by `DataStore<AppSettings>` JSON.
 * - iOS: `UserDefaultsAppSettingsRepository` backed by `UserDefaults` suite.
 * - Web: `LocalStorageAppSettingsRepository` backed by `localStorage`.
 *
 * All three serialize via [AppSettingsMigrator] so on-disk format is identical.
 */
interface AppSettingsRepository {
    /**
     * Hot flow that emits the current settings on subscription and every time
     * [update] or [reset] is called. Backed by a SharedFlow / DataStore Flow.
     * Never completes during normal app lifetime.
     */
    val flow: Flow<AppSettings>

    /**
     * Suspend-fetch the current settings. Equivalent to `flow.first()` but with
     * a clearer call site. Use when a one-shot read is needed inside a coroutine.
     */
    suspend fun current(): AppSettings

    /**
     * Atomically read, transform, and write settings. The [transform] block MUST be
     * pure — it may be invoked more than once under contention. Returns after the
     * write has been persisted.
     *
     * Example:
     * ```
     * repo.update { it.copy(midGameBoostEnabled = true) }
     * ```
     */
    suspend fun update(transform: (AppSettings) -> AppSettings)

    /**
     * Reset all settings to their default values. Used by GDPR Art. 17 / CCPA §1798.105
     * "Delete All My Data" flow. Stats reset is a separate concern.
     */
    suspend fun reset()
}
