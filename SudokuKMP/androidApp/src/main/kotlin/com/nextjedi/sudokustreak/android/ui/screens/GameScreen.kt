package com.nextjedi.sudokustreak.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nextjedi.sudokustreak.android.ui.components.NumberPad
import com.nextjedi.sudokustreak.android.ui.components.SudokuGrid
import com.nextjedi.sudokustreak.android.ui.theme.*
import com.nextjedi.sudokustreak.android.viewmodel.GameViewModel
import com.nextjedi.sudokustreak.android.viewmodel.SettingsViewModel

@Composable
fun GameScreen(
    gameViewModel: GameViewModel,
    settingsViewModel: SettingsViewModel,
    onExit: () -> Unit
) {
    val state by gameViewModel.uiState.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    // Completion dialog
    if (state.showCompletionDialog) {
        AlertDialog(
            onDismissRequest = { gameViewModel.dismissCompletionDialog() },
            title = { Text("Congratulations! 🎉") },
            text = {
                Text("Puzzle completed in ${formatTime(state.elapsedSeconds)}!")
            },
            confirmButton = {
                TextButton(onClick = {
                    gameViewModel.dismissCompletionDialog()
                    onExit()
                }) { Text("Home") }
            },
            dismissButton = {
                TextButton(onClick = {
                    gameViewModel.dismissCompletionDialog()
                    gameViewModel.startNewGame()
                }) { Text("New Game") }
            }
        )
    }

    // Game over dialog
    if (state.isGameOver) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Game Over") },
            text = { Text("You made ${state.maxMistakes} mistakes. Better luck next time!") },
            confirmButton = {
                TextButton(onClick = {
                    gameViewModel.startNewGame()
                }) { Text("Try Again") }
            },
            dismissButton = {
                TextButton(onClick = onExit) { Text("Home") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = state.difficulty.label,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )
                Text(
                    text = "❌ ${state.mistakes}/${state.maxMistakes}",
                    fontSize = 15.sp,
                    color = ErrorRed
                )
            }
            if (settings.timerEnabled) {
                Text(
                    text = formatTime(state.elapsedSeconds),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )
            }
        }

        // Grid
        SudokuGrid(
            grid = state.grid,
            selectedCell = state.selectedCell,
            onCellClick = { r, c -> gameViewModel.selectCell(r, c) },
            highlightEnabled = settings.highlightEnabled,
            solverHintCells = state.solverHintCells,
            solverFillingCell = state.solverFillingCell,
            modifier = Modifier.padding(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Number pad
        NumberPad(
            onNumberPress = { gameViewModel.enterNumber(it) },
            onErase = { gameViewModel.eraseCell() },
            disabled = state.isCompleted || state.isGameOver
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Solver section
        if (state.solverSteps.isNotEmpty() || state.isSolverActive) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(SolverYellow, RoundedCornerShape(10.dp))
                    .border(1.dp, SolverYellowBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                if (state.currentSolverReasoning.isNotEmpty()) {
                    Text(
                        text = state.currentSolverReasoning,
                        fontSize = 13.sp,
                        color = Color(0xFF856404),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                state.solverSteps.takeLast(3).forEachIndexed { i, step ->
                    Text(
                        text = "Step ${state.solverSteps.size - state.solverSteps.takeLast(3).size + i + 1}: ${step.reasoning}",
                        fontSize = 12.sp,
                        color = Navy.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Solver speed selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Speed:", fontSize = 14.sp, color = SlateGray)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(500 to "0.5s", 1500 to "1.5s", 3000 to "3s").forEach { (ms, label) ->
                    val active = settings.solverSpeedMs == ms
                    OutlinedButton(
                        onClick = { settingsViewModel.setSolverSpeed(ms) },
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (active) Blue500 else Color.White,
                            contentColor = if (active) Color.White else Navy
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Solver cells:", fontSize = 14.sp, color = SlateGray)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1, 3, 5, 9).forEach { n ->
                    val active = settings.solverHelpCells == n
                    OutlinedButton(
                        onClick = { settingsViewModel.setSolverHelpCells(n) },
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (active) Blue500 else Color.White,
                            contentColor = if (active) Color.White else Navy
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                    ) { Text("$n", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { gameViewModel.startSolver(
                    settings.solverSpeedMs.toLong(),
                    settings.solverHelpCells
                ) },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isSolverActive) ErrorRed else Blue500
                ),
                enabled = !state.isCompleted && !state.isGameOver
            ) {
                Text(
                    if (state.isSolverActive) "Stop" else "Hint",
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = { gameViewModel.startNewGame() },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("New Game", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
            ) {
                Text("Exit", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
