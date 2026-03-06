import Foundation
import Combine
import Shared  // KMP shared framework

// MARK: - Models mirroring Kotlin shared module

struct SudokuCellUI {
    var value: Int
    var isInitial: Bool
    var isSolverFilled: Bool
    var notes: [Int]
}

struct SolverStepUI {
    let row: Int
    let col: Int
    let value: Int
    let strategy: String
    let reasoning: String
}

struct GameUIState {
    var grid: [[SudokuCellUI]] = Self.emptyGrid()
    var selectedCell: (row: Int, col: Int)? = nil
    var difficulty: DifficultyUI = .easy
    var isCompleted: Bool = false
    var elapsedSeconds: Int = 0
    var mistakes: Int = 0
    var maxMistakes: Int = 3
    var isSolverActive: Bool = false
    var solverFillingCell: (Int, Int)? = nil
    var solverSteps: [SolverStepUI] = []
    var currentSolverReasoning: String = ""
    var isGameOver: Bool = false
    var showCompletionDialog: Bool = false

    static func emptyGrid() -> [[SudokuCellUI]] {
        Array(repeating: Array(repeating: SudokuCellUI(value: 0, isInitial: false,
            isSolverFilled: false, notes: []), count: 9), count: 9)
    }
}

enum DifficultyUI: String, CaseIterable, Identifiable {
    case easy = "Easy"
    case medium = "Medium"
    case hard = "Hard"
    var id: String { rawValue }
    var clues: Int {
        switch self { case .easy: return 46; case .medium: return 36; case .hard: return 26 }
    }
    var kmpDifficulty: Difficulty {
        switch self {
        case .easy: return Difficulty.easy
        case .medium: return Difficulty.medium
        case .hard: return Difficulty.hard
        }
    }
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

    func startNewGame(difficulty: DifficultyUI? = nil) {
        stopSolver()
        timerTask?.cancel()
        let diff = difficulty ?? state.difficulty
        let puzzle = generator.generatePuzzle(difficulty: diff.kmpDifficulty)
        state = GameUIState(
            grid: convertGrid(puzzle),
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
            let intGrid = makeIntGrid()
            intGrid[sel.row][sel.col] = 0
            if !validator.isValidMove(grid: intGrid, row: Int32(sel.row),
                                      col: Int32(sel.col), num: Int32(num)) {
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
                let kmpGrid = makeKmpGrid()
                guard let step = solver.findNextStep(grid: kmpGrid) else { break }
                await MainActor.run {
                    state.solverFillingCell = (Int(step.row), Int(step.col))
                    state.currentSolverReasoning = step.reasoning
                }
                try? await Task.sleep(nanoseconds: UInt64(stepDelayMs) * 1_000_000)
                guard !Task.isCancelled else { break }
                await MainActor.run {
                    state.grid[Int(step.row)][Int(step.col)].value = Int(step.value)
                    state.grid[Int(step.row)][Int(step.col)].isSolverFilled = true
                    state.solverFillingCell = nil
                    state.solverSteps.append(SolverStepUI(
                        row: Int(step.row), col: Int(step.col),
                        value: Int(step.value), strategy: step.strategy,
                        reasoning: step.reasoning
                    ))
                    checkCompletion()
                }
                filled += 1
                if state.isCompleted { break }
            }
            await MainActor.run { state.isSolverActive = false }
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
                await MainActor.run { state.elapsedSeconds += 1 }
            }
        }
    }

    private func checkCompletion() {
        let kmpGrid = makeKmpGrid()
        if validator.isComplete(grid: kmpGrid) && validator.isValidSolution(grid: kmpGrid) {
            timerTask?.cancel()
            stopSolver()
            state.isCompleted = true
            state.showCompletionDialog = true
            recordWin()
        }
    }

    private func convertGrid(_ kmpGrid: SudokuGrid) -> [[SudokuCellUI]] {
        (0..<9).map { r in
            (0..<9).map { c in
                let cell = kmpGrid[r][c] as! SudokuCell
                return SudokuCellUI(
                    value: Int(cell.value),
                    isInitial: cell.isInitial,
                    isSolverFilled: cell.isSolverFilled,
                    notes: []
                )
            }
        }
    }

    private func makeKmpGrid() -> SudokuGrid {
        // Build a SudokuGrid (List<List<SudokuCell>>) from our state
        return (0..<9).map { r in
            (0..<9).map { c in
                let ui = state.grid[r][c]
                return SudokuCell(
                    value: Int32(ui.value),
                    isInitial: ui.isInitial,
                    notes: [],
                    isSolverFilled: ui.isSolverFilled
                )
            }
        }
    }

    private func makeIntGrid() -> [[Int32]] {
        state.grid.map { row in row.map { Int32($0.value) } }
    }

    // MARK: - Persistence helpers

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
