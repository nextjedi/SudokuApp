package com.nextjedi.sudokustreak.android.storage

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Single source of truth for the stats `Preferences.Key<*>` set.
 *
 * Stats live in the legacy `DataStore<Preferences>` (file `sudoku_prefs`) per the
 * locked decision in `test-plan/00-SYNTHESIS.md` §6 (3): "stats remain in
 * `DataStore<Preferences>` while settings move to JSON". Three call sites consume
 * the same key set:
 *
 * - [com.nextjedi.sudokustreak.android.viewmodel.GameViewModel] — writes (records
 *   wins, increments games-played, updates streak).
 * - [com.nextjedi.sudokustreak.android.viewmodel.StatsViewModel] — reads (flow +
 *   reset).
 * - [com.nextjedi.sudokustreak.android.stats.PreferencesStatsResetter] — clears
 *   the entire bag on "Delete All My Data".
 *
 * Before this object existed, each call site re-declared `intPreferencesKey("games_played")`
 * etc. inside a private companion object. Renaming a key (e.g. `total_time` →
 * `total_time_seconds`) required editing three files and the audit found one place
 * where `StatsViewModel.KEY_BEST_EXPERT` had drifted from `GameViewModel.KEY_BEST_EXPERT`
 * because the string literal was duplicated. This object eliminates that risk.
 *
 * ## Adding a new stat key
 *
 * 1. Add the `val KEY_FOO = …` field below.
 * 2. Add it to [allStatsKeys] so [com.nextjedi.sudokustreak.android.stats.PreferencesStatsResetter]
 *    wipes it on "Delete All My Data".
 * 3. Wire it into [com.nextjedi.sudokustreak.android.viewmodel.StatsViewModel] +
 *    [com.nextjedi.sudokustreak.android.viewmodel.GameViewModel] as appropriate.
 *
 * ## Cross-platform note
 *
 * iOS mirrors this with `s_stats_*` keys in `UserDefaults.standard` (see
 * `test-plan/00-SYNTHESIS.md`). Web mirrors via `localStorage`. The string-literal
 * "wire format" must stay stable across all three for the migration story to hold.
 */
object StatsKeys {
    val KEY_GAMES_PLAYED = intPreferencesKey("games_played")
    val KEY_GAMES_WON = intPreferencesKey("games_won")
    val KEY_TOTAL_TIME = longPreferencesKey("total_time")

    val KEY_BEST_EASY = longPreferencesKey("best_easy")
    val KEY_BEST_MEDIUM = longPreferencesKey("best_medium")
    val KEY_BEST_HARD = longPreferencesKey("best_hard")
    val KEY_BEST_EXPERT = longPreferencesKey("best_expert")

    val KEY_STREAK = intPreferencesKey("streak")
    val KEY_LAST_PLAYED = stringPreferencesKey("last_played")

    /**
     * Every stats key in this object. Used by
     * [com.nextjedi.sudokustreak.android.stats.PreferencesStatsResetter.reset] to wipe
     * only the stats subset (when paired with a `MutablePreferences.remove(key)` loop)
     * — though the current production implementation calls `prefs.clear()` because
     * legacy installs may have orphan settings keys from before Wave 2.
     */
    val allStatsKeys: List<Preferences.Key<*>> = listOf(
        KEY_GAMES_PLAYED,
        KEY_GAMES_WON,
        KEY_TOTAL_TIME,
        KEY_BEST_EASY,
        KEY_BEST_MEDIUM,
        KEY_BEST_HARD,
        KEY_BEST_EXPERT,
        KEY_STREAK,
        KEY_LAST_PLAYED,
    )
}
