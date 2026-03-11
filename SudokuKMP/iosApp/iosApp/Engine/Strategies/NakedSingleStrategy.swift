import Foundation

class NakedSingleStrategy: SolvingStrategy {
    let name = "Naked Single"

    func findStep(grid: SudokuGrid, validator: SudokuValidator) -> SolverStep? {
        let intGrid = toIntGrid(grid)
        for r in 0..<9 {
            for c in 0..<9 where grid[r][c].value == 0 {
                let candidates = validator.getCandidates(grid: intGrid, row: r, col: c)
                if candidates.count == 1 {
                    let value = candidates[0]
                    return SolverStep(
                        row: r,
                        col: c,
                        value: value,
                        strategy: name,
                        reasoning: "R\(r + 1)C\(c + 1) can only be \(value) (naked single)"
                    )
                }
            }
        }
        return nil
    }
}
