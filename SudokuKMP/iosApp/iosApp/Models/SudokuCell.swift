import Foundation

struct SudokuCell {
    var value: Int
    var isInitial: Bool
    var isSolverFilled: Bool
    var notes: [Int]

    init(value: Int = 0, isInitial: Bool = false, isSolverFilled: Bool = false, notes: [Int] = []) {
        self.value = value
        self.isInitial = isInitial
        self.isSolverFilled = isSolverFilled
        self.notes = notes
    }
}

typealias SudokuGrid = [[SudokuCell]]

func emptyGrid() -> SudokuGrid {
    Array(repeating: Array(repeating: SudokuCell(), count: 9), count: 9)
}

func toIntGrid(_ grid: SudokuGrid) -> [[Int]] {
    grid.map { row in row.map { $0.value } }
}
