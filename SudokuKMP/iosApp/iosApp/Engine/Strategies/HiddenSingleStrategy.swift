import Foundation

class HiddenSingleStrategy: SolvingStrategy {
    let name = "Hidden Single"

    func findStep(grid: SudokuGrid, validator: SudokuValidator) -> SolverStep? {
        let intGrid = toIntGrid(grid)

        // Check rows
        for r in 0..<9 {
            if let step = findHiddenSingleInRegion(
                intGrid: intGrid, grid: grid, validator: validator,
                cells: (0..<9).map { (r, $0) }, regionName: "row \(r + 1)"
            ) {
                return step
            }
        }

        // Check columns
        for c in 0..<9 {
            if let step = findHiddenSingleInRegion(
                intGrid: intGrid, grid: grid, validator: validator,
                cells: (0..<9).map { ($0, c) }, regionName: "column \(c + 1)"
            ) {
                return step
            }
        }

        // Check 3x3 boxes
        for br in stride(from: 0, to: 9, by: 3) {
            for bc in stride(from: 0, to: 9, by: 3) {
                var cells: [(Int, Int)] = []
                for r in br..<(br + 3) {
                    for c in bc..<(bc + 3) {
                        cells.append((r, c))
                    }
                }
                let boxNum = (br / 3) * 3 + (bc / 3) + 1
                if let step = findHiddenSingleInRegion(
                    intGrid: intGrid, grid: grid, validator: validator,
                    cells: cells, regionName: "box \(boxNum)"
                ) {
                    return step
                }
            }
        }

        return nil
    }

    private func findHiddenSingleInRegion(
        intGrid: [[Int]], grid: SudokuGrid, validator: SudokuValidator,
        cells: [(Int, Int)], regionName: String
    ) -> SolverStep? {
        for num in 1...9 {
            // Skip if this number is already placed in the region
            let alreadyPlaced = cells.contains { grid[$0.0][$0.1].value == num }
            if alreadyPlaced { continue }

            // Find empty cells where this number can go
            let possibleCells = cells.filter { (r, c) in
                grid[r][c].value == 0 &&
                validator.isValidMove(grid: intGrid, row: r, col: c, num: num)
            }

            if possibleCells.count == 1 {
                let (r, c) = possibleCells[0]
                return SolverStep(
                    row: r,
                    col: c,
                    value: num,
                    strategy: name,
                    reasoning: "\(num) can only go in R\(r + 1)C\(c + 1) in \(regionName) (hidden single)"
                )
            }
        }
        return nil
    }
}
