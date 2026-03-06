package com.nextjedi.sudokustreak.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nextjedi.sudokustreak.android.ui.screens.GameScreen
import com.nextjedi.sudokustreak.android.ui.screens.HomeScreen
import com.nextjedi.sudokustreak.android.ui.screens.SettingsScreen
import com.nextjedi.sudokustreak.android.ui.screens.StatsScreen
import com.nextjedi.sudokustreak.android.viewmodel.GameViewModel
import com.nextjedi.sudokustreak.android.viewmodel.SettingsViewModel
import com.nextjedi.sudokustreak.android.viewmodel.StatsViewModel

object Routes {
    const val HOME = "home"
    const val GAME = "game"
    const val SETTINGS = "settings"
    const val STATS = "stats"
}

@Composable
fun AppNavigation(
    gameViewModel: GameViewModel,
    settingsViewModel: SettingsViewModel,
    statsViewModel: StatsViewModel
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                gameViewModel = gameViewModel,
                statsViewModel = statsViewModel,
                onStartGame = { navController.navigate(Routes.GAME) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenStats = { navController.navigate(Routes.STATS) }
            )
        }
        composable(Routes.GAME) {
            GameScreen(
                gameViewModel = gameViewModel,
                settingsViewModel = settingsViewModel,
                onExit = { navController.popBackStack(Routes.HOME, inclusive = false) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                statsViewModel = statsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.STATS) {
            StatsScreen(
                statsViewModel = statsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
