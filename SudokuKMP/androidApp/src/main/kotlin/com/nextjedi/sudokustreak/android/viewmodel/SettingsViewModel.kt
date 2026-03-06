package com.nextjedi.sudokustreak.android.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsState(
    val soundEnabled: Boolean = true,
    val highlightEnabled: Boolean = true,
    val timerEnabled: Boolean = true,
    val solverSpeedMs: Int = 1500,
    val solverHelpCells: Int = 3
)

class SettingsViewModel(private val dataStore: DataStore<Preferences>) : ViewModel() {

    companion object {
        val KEY_SOUND = booleanPreferencesKey("sound_enabled")
        val KEY_HIGHLIGHT = booleanPreferencesKey("highlight_enabled")
        val KEY_TIMER = booleanPreferencesKey("timer_enabled")
        val KEY_SOLVER_SPEED = intPreferencesKey("solver_speed_ms")
        val KEY_SOLVER_CELLS = intPreferencesKey("solver_help_cells")
    }

    val settings: StateFlow<SettingsState> = dataStore.data.map { prefs ->
        SettingsState(
            soundEnabled = prefs[KEY_SOUND] ?: true,
            highlightEnabled = prefs[KEY_HIGHLIGHT] ?: true,
            timerEnabled = prefs[KEY_TIMER] ?: true,
            solverSpeedMs = prefs[KEY_SOLVER_SPEED] ?: 1500,
            solverHelpCells = prefs[KEY_SOLVER_CELLS] ?: 3
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())

    fun toggleSound() = toggle(KEY_SOUND) { settings.value.soundEnabled }
    fun toggleHighlight() = toggle(KEY_HIGHLIGHT) { settings.value.highlightEnabled }
    fun toggleTimer() = toggle(KEY_TIMER) { settings.value.timerEnabled }

    fun setSolverSpeed(ms: Int) = viewModelScope.launch {
        dataStore.edit { it[KEY_SOLVER_SPEED] = ms }
    }

    fun setSolverHelpCells(count: Int) = viewModelScope.launch {
        dataStore.edit { it[KEY_SOLVER_CELLS] = count.coerceIn(1, 9) }
    }

    private fun toggle(key: Preferences.Key<Boolean>, current: () -> Boolean) {
        viewModelScope.launch { dataStore.edit { it[key] = !current() } }
    }
}
