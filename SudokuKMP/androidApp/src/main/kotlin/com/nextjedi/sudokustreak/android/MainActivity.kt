package com.nextjedi.sudokustreak.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nextjedi.sudokustreak.android.ui.navigation.AppNavigation
import com.nextjedi.sudokustreak.android.ui.theme.SudokuTheme
import com.nextjedi.sudokustreak.android.viewmodel.GameViewModel
import com.nextjedi.sudokustreak.android.viewmodel.SettingsViewModel
import com.nextjedi.sudokustreak.android.viewmodel.StatsViewModel

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by lazy {
        ViewModelProvider(this, DataStoreViewModelFactory(dataStore))[GameViewModel::class.java]
    }
    private val settingsViewModel: SettingsViewModel by lazy {
        ViewModelProvider(this, DataStoreViewModelFactory(dataStore))[SettingsViewModel::class.java]
    }
    private val statsViewModel: StatsViewModel by lazy {
        ViewModelProvider(this, DataStoreViewModelFactory(dataStore))[StatsViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SudokuTheme {
                AppNavigation(
                    gameViewModel = gameViewModel,
                    settingsViewModel = settingsViewModel,
                    statsViewModel = statsViewModel
                )
            }
        }
    }
}

class DataStoreViewModelFactory(
    private val context: android.content.Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(GameViewModel::class.java) ->
            GameViewModel(context.dataStore) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(context.dataStore) as T
        modelClass.isAssignableFrom(StatsViewModel::class.java) ->
            StatsViewModel(context.dataStore) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
