package com.nextjedi.sudokustreak.android.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextjedi.sudokustreak.engine.PuzzleGenerator
import com.nextjedi.sudokustreak.engine.SudokuSolver
import com.nextjedi.sudokustreak.engine.SudokuValidator
import com.nextjedi.sudokustreak.model.Difficulty
import com.nextjedi.sudokustreak.model.SolverStep
import com.nextjedi.sudokustreak.model.SudokuCell
import com.nextjedi.sudokustreak.model.SudokuGrid
import com.nextjedi.sudokustreak.model.emptyGrid
import com.nextjedi.sudokustreak.model.toIntGrid
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GameUiState(
    val grid: SudokuGrid = emptyGrid(),
    val selectedCell: Pair<Int, Int>? = null,
    val difficulty: Difficulty = Difficulty.EASY,
    val isCompleted: Boolean = false,
    val elapsedSeconds: Int = 0,
    val mistakes: Int = 0,
    val maxMistakes: Int = 3,
    val isSolverActive: Boolean = false,
    val solverHintCells: List<Pair<Int, Int>> = emptyList(),
    val solverFillingCell: Pair<Int, Int>? = null,
    val solverSteps: List<SolverStep> = emptyList(),
    val currentSolverReasoning: String = "",
    val isGameOver: Boolean = false,
    val showCompletionDialog: Boolean = false
)

class GameViewModel(private val dataStore: DataStore<Preferences>) : ViewModel() {

    private val validator = SudokuValidator()
    private val generator = PuzzleGenerator(validator)
    private val solver = SudokuSolver(validator)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var solverJob: Job? = null

    companion object {
        private val KEY_GAMES_PLAYED = intPreferencesKey("games_played")
        private val KEY_GAMES_WON = intPreferencesKey("games_won")
        private val KEY_TOTAL_TIME = longPreferencesKey("total_time")
        private val KEY_BEST_EASY = longPreferencesKey("best_easy")
        private val KEY_BEST_MEDIUM = longPreferencesKey("best_medium")
        private val KEY_BEST_HARD = longPreferencesKey("best_hard")
        private val KEY_STREAK = intPreferencesKey("streak")
        private val KEY_LAST_PLAYED = stringPreferencesKey("last_played")
    }

    fun startNewGame(difficulty: Difficulty = _uiState.value.difficulty) {
        stopSolver()
        timerJob?.cancel()

        val puzzle = generator.generatePuzzle(difficulty)
        _uiState.update {
            GameUiState(
                grid = puzzle,
                difficulty = difficulty,
                elapsedSeconds = 0
            )
        }

        startTimer()
        incrementGamesPlayed()
        updateStreak()
    }

    fun selectCell(row: Int, col: Int) {
        if (_uiState.value.isCompleted || _uiState.value.isGameOver) return
        _uiState.update { it.copy(selectedCell = Pair(row, col)) }
    }

    fun enterNumber(num: Int) {
        val state = _uiState.value
        if (state.isCompleted || state.isGameOver || state.selectedCell == null) return

        val (row, col) = state.selectedCell
        val cell = state.grid[row][col]
        if (cell.isInitial) return

        if (num != 0) {
            val intGrid = state.grid.toIntGrid()
            intGrid[row][col] = 0 // temporarily clear to validate
            if (!validator.isValidMove(intGrid, row, col, num)) {
                val newMistakes = state.mistakes + 1
                _uiState.update { it.copy(mistakes = newMistakes) }
                if (newMistakes >= state.maxMistakes) {
                    timerJob?.cancel()
                    _uiState.update { it.copy(isGameOver = true) }
                }
                return
            }
        }

        val newGrid = state.grid.mapIndexed { r, row_ ->
            row_.mapIndexed { c, cell_ ->
                if (r == row && c == col) cell_.copy(value = num) else cell_
            }
        }

        _uiState.update { it.copy(grid = newGrid) }
        checkCompletion()
    }

    fun eraseCell() {
        val state = _uiState.value
        if (state.isCompleted || state.isGameOver || state.selectedCell == null) return
        val (row, col) = state.selectedCell
        if (state.grid[row][col].isInitial) return
        enterNumber(0)
    }

    fun startSolver(stepDelayMs: Long = 1500L, maxCells: Int = 3) {
        if (_uiState.value.isSolverActive) {
            stopSolver()
            return
        }
        _uiState.update { it.copy(isSolverActive = true, solverSteps = emptyList()) }
        solverJob = viewModelScope.launch {
            var cellsFilled = 0
            while (cellsFilled < maxCells) {
                val step = solver.findNextStep(_uiState.value.grid) ?: break
                _uiState.update { state ->
                    state.copy(
                        solverFillingCell = Pair(step.row, step.col),
                        currentSolverReasoning = step.reasoning
                    )
                }
                delay(stepDelayMs)
                val state = _uiState.value
                val newGrid = state.grid.mapIndexed { r, row ->
                    row.mapIndexed { c, cell ->
                        if (r == step.row && c == step.col)
                            cell.copy(value = step.value, isSolverFilled = true)
                        else cell
                    }
                }
                _uiState.update { it.copy(
                    grid = newGrid,
                    solverFillingCell = null,
                    solverSteps = it.solverSteps + step
                ) }
                cellsFilled++
                checkCompletion()
                if (_uiState.value.isCompleted) break
            }
            _uiState.update { it.copy(isSolverActive = false) }
        }
    }

    fun stopSolver() {
        solverJob?.cancel()
        _uiState.update { it.copy(isSolverActive = false, solverFillingCell = null) }
    }

    fun dismissCompletionDialog() {
        _uiState.update { it.copy(showCompletionDialog = false) }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    private fun checkCompletion() {
        val state = _uiState.value
        if (validator.isComplete(state.grid) && validator.isValidSolution(state.grid)) {
            timerJob?.cancel()
            stopSolver()
            _uiState.update { it.copy(isCompleted = true, showCompletionDialog = true) }
            recordWin()
        }
    }

    private fun incrementGamesPlayed() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_GAMES_PLAYED] = (prefs[KEY_GAMES_PLAYED] ?: 0) + 1
            }
        }
    }

    private fun recordWin() {
        viewModelScope.launch {
            val elapsed = _uiState.value.elapsedSeconds.toLong()
            val diffKey = when (_uiState.value.difficulty) {
                Difficulty.EASY -> KEY_BEST_EASY
                Difficulty.MEDIUM -> KEY_BEST_MEDIUM
                Difficulty.HARD -> KEY_BEST_HARD
            }
            dataStore.edit { prefs ->
                prefs[KEY_GAMES_WON] = (prefs[KEY_GAMES_WON] ?: 0) + 1
                prefs[KEY_TOTAL_TIME] = (prefs[KEY_TOTAL_TIME] ?: 0L) + elapsed
                val current = prefs[diffKey] ?: Long.MAX_VALUE
                if (elapsed < current) prefs[diffKey] = elapsed
            }
        }
    }

    private fun updateStreak() {
        viewModelScope.launch {
            val prefs = dataStore.data.first()
            val lastPlayed = prefs[KEY_LAST_PLAYED] ?: ""
            val today = todayString()
            val currentStreak = prefs[KEY_STREAK] ?: 0
            val newStreak = when {
                lastPlayed == today -> currentStreak
                lastPlayed == yesterdayString() -> currentStreak + 1
                lastPlayed.isEmpty() -> 1
                else -> 1
            }
            dataStore.edit {
                it[KEY_STREAK] = newStreak
                it[KEY_LAST_PLAYED] = today
            }
        }
    }

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun yesterdayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() - 86_400_000L))

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        solverJob?.cancel()
    }
}
