import Foundation

class SudokuValidator {

    func isValidMove(grid: [[Int]], row: Int, col: Int, num: Int) -> Bool {
        // Check row
        for c in 0..<9 where grid[row][c] == num {
            return false
        }
        // Check column
        for r in 0..<9 where grid[r][col] == num {
            return false
        }
        // Check 3x3 box
        let boxRow = (row / 3) * 3
        let boxCol = (col / 3) * 3
        for r in boxRow..<(boxRow + 3) {
            for c in boxCol..<(boxCol + 3) where grid[r][c] == num {
                return false
            }
        }
        return true
    }

    func getCandidates(grid: [[Int]], row: Int, col: Int) -> [Int] {
        guard grid[row][col] == 0 else { return [] }
        return (1...9).filter { isValidMove(grid: grid, row: row, col: col, num: $0) }
    }

    func isComplete(grid: SudokuGrid) -> Bool {
        grid.allSatisfy { row in row.allSatisfy { $0.value != 0 } }
    }

    func isValidSolution(grid: SudokuGrid) -> Bool {
        let intGrid = toIntGrid(grid)
        // Check all rows have 1-9
        for r in 0..<9 {
            if Set(intGrid[r]) != Set(1...9) { return false }
        }
        // Check all columns have 1-9
        for c in 0..<9 {
            let col = (0..<9).map { intGrid[$0][c] }
            if Set(col) != Set(1...9) { return false }
        }
        // Check all 3x3 boxes have 1-9
        for br in stride(from: 0, to: 9, by: 3) {
            for bc in stride(from: 0, to: 9, by: 3) {
                var box: [Int] = []
                for r in br..<(br + 3) {
                    for c in bc..<(bc + 3) {
                        box.append(intGrid[r][c])
                    }
                }
                if Set(box) != Set(1...9) { return false }
            }
        }
        return true
    }
}
