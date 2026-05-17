package com.nextjedi.sudokustreak.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nextjedi.sudokustreak.android.sensor.SensorServiceProvider
import com.nextjedi.sudokustreak.android.storage.SettingsModule
import com.nextjedi.sudokustreak.android.ui.navigation.AppNavigation
import com.nextjedi.sudokustreak.android.ui.theme.SudokuTheme
import com.nextjedi.sudokustreak.android.viewmodel.GameViewModel
import com.nextjedi.sudokustreak.android.viewmodel.SettingsViewModel
import com.nextjedi.sudokustreak.android.viewmodel.StatsViewModel
import com.nextjedi.sudokustreak.domain.settings.AppSettings
import com.nextjedi.sudokustreak.domain.settings.ColorBlindMode as DomainColorBlindMode
import com.nextjedi.sudokustreak.domain.settings.ThemeMode as DomainThemeMode
import com.nextjedi.sudokustreak.android.ui.theme.ColorBlindMode as ThemeColorBlindMode
import com.nextjedi.sudokustreak.android.ui.theme.ThemeMode as ThemeThemeMode

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by lazy {
        ViewModelProvider(this, DataStoreViewModelFactory(this))[GameViewModel::class.java]
    }
    private val settingsViewModel: SettingsViewModel by lazy {
        ViewModelProvider(this, DataStoreViewModelFactory(this))[SettingsViewModel::class.java]
    }
    private val statsViewModel: StatsViewModel by lazy {
        ViewModelProvider(this, DataStoreViewModelFactory(this))[StatsViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lifecycle-aware sensor binding (Wave 2 sensor agent) — listeners register
        // on ON_RESUME, unregister on ON_PAUSE. Idempotent across configuration changes.
        SensorServiceProvider.bind(owner = this, context = this)

        enableEdgeToEdge()
        setContent {
            // Live-collect the AppSettings so theme + accessibility flags update
            // immediately when the user flips a Settings toggle (TC-A4 — pickDarkTheme
            // → colour scheme becomes dark within the same frame).
            val settings by settingsViewModel.settings.collectAsState(initial = AppSettings())
            SudokuTheme(
                themeMode = settings.themeMode.toThemeLayer(),
                colorBlindMode = settings.colorBlindMode.toThemeLayer(),
                useDynamicColor = settings.useDynamicColor,
                reduceMotion = settings.reduceMotion,
            ) {
                AppNavigation(
                    gameViewModel = gameViewModel,
                    settingsViewModel = settingsViewModel,
                    statsViewModel = statsViewModel
                )
            }
        }
    }
}

/**
 * The Theme layer ships its OWN copies of `ThemeMode` + `ColorBlindMode` (see
 * `ui/theme/ThemeMode.kt`) because they were written before `:domain` existed. Until a
 * follow-up agent collapses the two pairs, we map across the boundary here.
 */
private fun DomainThemeMode.toThemeLayer(): ThemeThemeMode = when (this) {
    DomainThemeMode.SYSTEM -> ThemeThemeMode.SYSTEM
    DomainThemeMode.LIGHT -> ThemeThemeMode.LIGHT
    DomainThemeMode.DARK -> ThemeThemeMode.DARK
    DomainThemeMode.AMOLED -> ThemeThemeMode.AMOLED
}

private fun DomainColorBlindMode.toThemeLayer(): ThemeColorBlindMode = when (this) {
    DomainColorBlindMode.NONE -> ThemeColorBlindMode.NONE
    DomainColorBlindMode.DEUTERANOPIA -> ThemeColorBlindMode.DEUTERANOPIA
    DomainColorBlindMode.PROTANOPIA -> ThemeColorBlindMode.PROTANOPIA
}

class DataStoreViewModelFactory(
    private val context: android.content.Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(GameViewModel::class.java) ->
            GameViewModel(context.dataStore) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            // Wave-2 wiring: typed AppSettingsRepository + decoupled stats reset +
            // PostHog-backed analytics facade (NoOp until the analytics agent lands).
            SettingsViewModel(
                repository = SettingsModule.appSettingsRepository(context),
                statsResetter = SettingsModule.statsResetter(context, context.dataStore),
                analyticsService = SettingsModule.analyticsService(),
            ) as T
        modelClass.isAssignableFrom(StatsViewModel::class.java) ->
            StatsViewModel(context.dataStore) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
