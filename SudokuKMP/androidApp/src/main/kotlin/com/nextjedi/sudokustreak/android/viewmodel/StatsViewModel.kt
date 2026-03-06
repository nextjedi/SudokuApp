package com.nextjedi.sudokustreak.android.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StatsState(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val totalTimeSec: Long = 0L,
    val bestEasySec: Long = 0L,   // 0 = not set
    val bestMediumSec: Long = 0L,
    val bestHardSec: Long = 0L,
    val currentStreak: Int = 0,
    val lastPlayedDate: String = ""
) {
    val winRate: Int get() = if (gamesPlayed > 0) (gamesWon * 100 / gamesPlayed) else 0
    val averageTimeSec: Long get() = if (gamesWon > 0) totalTimeSec / gamesWon else 0L
}

class StatsViewModel(private val dataStore: DataStore<Preferences>) : ViewModel() {

    companion object {
        val KEY_GAMES_PLAYED = intPreferencesKey("games_played")
        val KEY_GAMES_WON = intPreferencesKey("games_won")
        val KEY_TOTAL_TIME = longPreferencesKey("total_time")
        val KEY_BEST_EASY = longPreferencesKey("best_easy")
        val KEY_BEST_MEDIUM = longPreferencesKey("best_medium")
        val KEY_BEST_HARD = longPreferencesKey("best_hard")
        val KEY_STREAK = intPreferencesKey("streak")
        val KEY_LAST_PLAYED = stringPreferencesKey("last_played")
    }

    val stats: StateFlow<StatsState> = dataStore.data.map { prefs ->
        StatsState(
            gamesPlayed = prefs[KEY_GAMES_PLAYED] ?: 0,
            gamesWon = prefs[KEY_GAMES_WON] ?: 0,
            totalTimeSec = prefs[KEY_TOTAL_TIME] ?: 0L,
            bestEasySec = prefs[KEY_BEST_EASY] ?: 0L,
            bestMediumSec = prefs[KEY_BEST_MEDIUM] ?: 0L,
            bestHardSec = prefs[KEY_BEST_HARD] ?: 0L,
            currentStreak = prefs[KEY_STREAK] ?: 0,
            lastPlayedDate = prefs[KEY_LAST_PLAYED] ?: ""
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StatsState())

    fun resetStats() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_GAMES_PLAYED] = 0
                prefs[KEY_GAMES_WON] = 0
                prefs[KEY_TOTAL_TIME] = 0L
                prefs[KEY_BEST_EASY] = 0L
                prefs[KEY_BEST_MEDIUM] = 0L
                prefs[KEY_BEST_HARD] = 0L
                prefs[KEY_STREAK] = 0
                prefs[KEY_LAST_PLAYED] = ""
            }
        }
    }
}
