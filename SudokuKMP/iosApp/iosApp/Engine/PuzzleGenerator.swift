import Foundation

class PuzzleGenerator {
    private let validator: SudokuValidator

    init(validator: SudokuValidator) {
        self.validator = validator
    }

    func generatePuzzle(difficulty: Difficulty) -> SudokuGrid {
        var intGrid = Array(repeating: Array(repeating: 0, count: 9), count: 9)

        // Fill diagonal 3x3 boxes (independent, no constraint conflicts)
        for box in stride(from: 0, to: 9, by: 3) {
            fillBox(&intGrid, startRow: box, startCol: box)
        }

        // Solve to fill the rest
        _ = solveSudoku(&intGrid)

        // Remove cells based on difficulty
        var cellsToRemove = difficulty.cellsToRemove
        var positions = (0..<81).map { ($0 / 9, $0 % 9) }.shuffled()

        while cellsToRemove > 0 && !positions.isEmpty {
            let (r, c) = positions.removeFirst()
            if intGrid[r][c] != 0 {
                intGrid[r][c] = 0
                cellsToRemove -= 1
            }
        }

        // Convert to SudokuGrid
        return intGrid.enumerated().map { _, row in
            row.enumerated().map { _, val in
                SudokuCell(
                    value: val,
                    isInitial: val != 0,
                    isSolverFilled: false,
                    notes: []
                )
            }
        }
    }

    private func fillBox(_ grid: inout [[Int]], startRow: Int, startCol: Int) {
        var nums = Array(1...9).shuffled()
        for r in startRow..<(startRow + 3) {
            for c in startCol..<(startCol + 3) {
                grid[r][c] = nums.removeLast()
            }
        }
    }

    private func solveSudoku(_ grid: inout [[Int]]) -> Bool {
        for r in 0..<9 {
            for c in 0..<9 {
                if grid[r][c] == 0 {
                    for num in (1...9).shuffled() {
                        if validator.isValidMove(grid: grid, row: r, col: c, num: num) {
                            grid[r][c] = num
                            if solveSudoku(&grid) { return true }
                            grid[r][c] = 0
                        }
                    }
                    return false
                }
            }
        }
        return true
    }
}
