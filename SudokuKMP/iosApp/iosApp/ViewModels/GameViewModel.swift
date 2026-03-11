import Foundation
import Combine

struct GameUIState {
    var grid: SudokuGrid = emptyGrid()
    var selectedCell: (row: Int, col: Int)? = nil
    var difficulty: Difficulty = .easy
    var isCompleted: Bool = false
    var elapsedSeconds: Int = 0
    var mistakes: Int = 0
    var maxMistakes: Int = 3
    var isSolverActive: Bool = false
    var solverFillingCell: (Int, Int)? = nil
    var solverSteps: [SolverStep] = []
    var currentSolverReasoning: String = ""
    var isGameOver: Bool = false
    var showCompletionDialog: Bool = false
}

@MainActor
class GameViewModel: ObservableObject {
    @Published var state = GameUIState()

    private let validator = SudokuValidator()
    private lazy var generator = PuzzleGenerator(validator: validator)
    private lazy var solver = SudokuSolver(validator: validator)

    private var timerTask: Task<Void, Never>?
    private var solverTask: Task<Void, Never>?

    private let userDefaults = UserDefaults.standard

    init() {
        startNewGame()
    }

    func startNewGame(difficulty: Difficulty? = nil) {
        stopSolver()
        timerTask?.cancel()
        let diff = difficulty ?? state.difficulty
        let puzzle = generator.generatePuzzle(difficulty: diff)
        state = GameUIState(
            grid: puzzle,
            difficulty: diff,
            elapsedSeconds: 0
        )
        startTimer()
        recordGamePlayed()
        updateStreak()
    }

    func selectCell(row: Int, col: Int) {
        guard !state.isCompleted && !state.isGameOver else { return }
        state.selectedCell = (row, col)
    }

    func enterNumber(_ num: Int) {
        guard !state.isCompleted, !state.isGameOver,
              let sel = state.selectedCell else { return }
        let cell = state.grid[sel.row][sel.col]
        guard !cell.isInitial else { return }

        if num != 0 {
            var intGrid = toIntGrid(state.grid)
            intGrid[sel.row][sel.col] = 0
            if !validator.isValidMove(grid: intGrid, row: sel.row, col: sel.col, num: num) {
                state.mistakes += 1
                if state.mistakes >= state.maxMistakes {
                    timerTask?.cancel()
                    state.isGameOver = true
                }
                return
            }
        }
        state.grid[sel.row][sel.col].value = num
        checkCompletion()
    }

    func eraseCell() {
        guard let sel = state.selectedCell,
              !state.grid[sel.row][sel.col].isInitial else { return }
        state.grid[sel.row][sel.col].value = 0
    }

    func startSolver(stepDelayMs: Int = 1500, maxCells: Int = 3) {
        if state.isSolverActive { stopSolver(); return }
        state.isSolverActive = true
        state.solverSteps = []

        solverTask = Task {
            var filled = 0
            while filled < maxCells {
                guard let step = solver.findNextStep(grid: state.grid) else { break }
                state.solverFillingCell = (step.row, step.col)
                state.currentSolverReasoning = step.reasoning

                try? await Task.sleep(nanoseconds: UInt64(stepDelayMs) * 1_000_000)
                guard !Task.isCancelled else { break }

                state.grid[step.row][step.col].value = step.value
                state.grid[step.row][step.col].isSolverFilled = true
                state.solverFillingCell = nil
                state.solverSteps.append(step)
                checkCompletion()

                filled += 1
                if state.isCompleted { break }
            }
            state.isSolverActive = false
        }
    }

    func stopSolver() {
        solverTask?.cancel()
        state.isSolverActive = false
        state.solverFillingCell = nil
    }

    func dismissCompletionDialog() { state.showCompletionDialog = false }

    // MARK: - Private

    private func startTimer() {
        timerTask?.cancel()
        timerTask = Task {
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                guard !Task.isCancelled else { break }
                state.elapsedSeconds += 1
            }
        }
    }

    private func checkCompletion() {
        if validator.isComplete(grid: state.grid) && validator.isValidSolution(grid: state.grid) {
            timerTask?.cancel()
            stopSolver()
            state.isCompleted = true
            state.showCompletionDialog = true
            recordWin()
        }
    }

    // MARK: - Persistence

    private func recordGamePlayed() {
        let count = userDefaults.integer(forKey: "games_played")
        userDefaults.set(count + 1, forKey: "games_played")
    }

    private func recordWin() {
        let won = userDefaults.integer(forKey: "games_won")
        userDefaults.set(won + 1, forKey: "games_won")
        let total = userDefaults.integer(forKey: "total_time")
        userDefaults.set(total + state.elapsedSeconds, forKey: "total_time")
        let bestKey: String
        switch state.difficulty {
        case .easy: bestKey = "best_easy"
        case .medium: bestKey = "best_medium"
        case .hard: bestKey = "best_hard"
        }
        let current = userDefaults.integer(forKey: bestKey)
        if current == 0 || state.elapsedSeconds < current {
            userDefaults.set(state.elapsedSeconds, forKey: bestKey)
        }
    }

    private func updateStreak() {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        let today = formatter.string(from: Date())
        let yesterday = formatter.string(from: Calendar.current.date(byAdding: .day, value: -1, to: Date())!)
        let lastPlayed = userDefaults.string(forKey: "last_played") ?? ""
        let streak = userDefaults.integer(forKey: "streak")
        let newStreak: Int
        if lastPlayed == today { newStreak = streak }
        else if lastPlayed == yesterday { newStreak = streak + 1 }
        else if lastPlayed.isEmpty { newStreak = 1 }
        else { newStreak = 1 }
        userDefaults.set(newStreak, forKey: "streak")
        userDefaults.set(today, forKey: "last_played")
    }
}
