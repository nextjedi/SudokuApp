package com.nextjedi.sudokustreak.android.stats

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit

/**
 * Clears all persisted stats keys (the `KEY_GAMES_PLAYED`, `KEY_BEST_*`, `KEY_STREAK`, …
 * family that lives in the legacy `DataStore<Preferences>` per locked decision §6 (3)).
 *
 * Used by Settings → Data & Privacy → **Delete All My Data** (P0-13 in
 * test-plan/00-SYNTHESIS.md / Camila's privacy audit / GDPR Art. 17 + CCPA §1798.105).
 *
 * ## Why a separate interface?
 *
 * - Stats reset is a different concern from settings reset. Settings live in
 *   `DataStore<AppSettings>` (typed JSON); stats live in `DataStore<Preferences>`
 *   (untyped KV). The settings repository's `reset()` clears the first; this clears
 *   the second.
 * - Decoupling the two stores lets the settings agent unit-test the deletion flow
 *   without instantiating a Preferences DataStore — the test plugs in a fake.
 * - The stats agent owns the schema; they update [PreferencesStatsResetter] when new
 *   stats keys are added.
 *
 * ## Lifecycle
 *
 * Safe to call from any coroutine context — the implementation does its own
 * `dataStore.edit { … }` which trampolines to the DataStore single-writer scope.
 */
fun interface StatsResetter {
    /**
     * Clear every persisted stats key. Returns after the write has been flushed —
     * callers may chain follow-up cleanup (saved games, replays, analytics shutdown)
     * once this resumes.
     */
    suspend fun reset()
}

/**
 * Default implementation backed by the legacy `DataStore<Preferences>`. Mirrors the
 * key set used by [com.nextjedi.sudokustreak.android.viewmodel.StatsViewModel] +
 * [com.nextjedi.sudokustreak.android.viewmodel.GameViewModel].
 *
 * **When new stats keys are added,** add them here as well — otherwise Delete All
 * My Data leaves orphan keys behind (Camila's audit blocks shipping if any survive).
 */
class PreferencesStatsResetter(
    private val dataStore: DataStore<Preferences>,
) : StatsResetter {

    override suspend fun reset() {
        dataStore.edit { prefs ->
            // Mirror the StatsViewModel + GameViewModel companion-object key set.
            // Using `prefs.clear()` would also wipe legacy settings keys that the
            // pre-Wave-2 SettingsViewModel wrote into the same store — that's the
            // intended behaviour for "Delete All My Data" (a clean factory state).
            //
            // After Wave 2 the legacy settings keys are no longer written, but old
            // installs may still have them on disk; `clear()` purges those too.
            prefs.clear()
        }
    }
}

/**
 * Test double — counts reset invocations so unit tests can verify the Delete All
 * My Data flow fans out to the stats store.
 */
class FakeStatsResetter : StatsResetter {
    @Volatile
    var resetCount: Int = 0
        private set

    override suspend fun reset() {
        resetCount += 1
    }
}
