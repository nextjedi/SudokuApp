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
import com.nextjedi.sudokustreak.android.viewmodel.SolverPhase

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
            // Wave-2 settings: `showTimer` replaced the prior `timerEnabled` field.
            if (settings.showTimer) {
                Text(
                    text = formatTime(state.elapsedSeconds),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )
            }
        }

        // Grid — pass solver phase and current step for notes elimination display
        SudokuGrid(
            grid = state.grid,
            selectedCell = state.selectedCell,
            onCellClick = { r, c -> gameViewModel.selectCell(r, c) },
            highlightEnabled = settings.highlightEnabled,
            solverHintCells = state.solverHintCells,
            solverFillingCell = state.solverFillingCell,
            currentSolverPhase = state.currentSolverPhase,
            currentStep = state.solverSteps.lastOrNull(),
            modifier = Modifier.padding(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Reasoning panel — shown whenever there's something to say
        if (state.currentSolverReasoning.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E2A38)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Strategy badge — shown when solver active and we have steps
                    val lastStrategy = state.solverSteps.lastOrNull()?.strategy
                    if (state.isSolverActive && lastStrategy != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Blue500.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = lastStrategy,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = state.currentSolverReasoning,
                        fontSize = 13.sp,
                        color = Color(0xFFE0E0E0)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Solver step log — last 3 steps shown when solver has run
        if (state.solverSteps.isNotEmpty() || state.isSolverActive) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(SolverYellow, RoundedCornerShape(10.dp))
                    .border(1.dp, SolverYellowBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
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

        // Number pad
        NumberPad(
            onNumberPress = { gameViewModel.enterNumber(it) },
            onErase = { gameViewModel.eraseCell() },
            disabled = state.isCompleted || state.isGameOver
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Hint / Step / Solve row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Hint button
            OutlinedButton(
                onClick = { gameViewModel.requestHint() },
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(10.dp),
                enabled = !state.isCompleted && !state.isGameOver,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (state.hintLevel > 0) Color(0xFFF57C00) else Navy
                )
            ) {
                Text(
                    text = if (state.hintLevel > 0) "Hint ${state.hintLevel}/5" else "Hint",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Next Step button
            OutlinedButton(
                onClick = { gameViewModel.startSolver(maxCells = 1) },
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(10.dp),
                enabled = !state.isCompleted && !state.isGameOver && !state.isSolverActive,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue500)
            ) {
                Text("Next Step", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            // Solve for me button
            Button(
                onClick = { gameViewModel.startSolver(maxCells = 81) },
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isSolverActive) ErrorRed else Blue500
                ),
                enabled = !state.isCompleted && !state.isGameOver
            ) {
                Text(
                    text = if (state.isSolverActive) "Stop" else "Solve",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Clear hint button — only visible when hint is active
        if (state.hintLevel > 0) {
            TextButton(
                onClick = { gameViewModel.clearHint() },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text("Clear Hint", fontSize = 13.sp, color = SlateGray)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                listOf(500 to "Fast", 1500 to "Normal", 3000 to "Slow").forEach { (ms, label) ->
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

        // Main action buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
