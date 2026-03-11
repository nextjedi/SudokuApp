import Foundation

class SudokuSolver {
    private let validator: SudokuValidator
    private let strategies: [SolvingStrategy]

    init(validator: SudokuValidator, strategies: [SolvingStrategy]? = nil) {
        self.validator = validator
        self.strategies = strategies ?? [
            NakedSingleStrategy(),
            HiddenSingleStrategy()
        ]
    }

    func findNextStep(grid: SudokuGrid) -> SolverStep? {
        for strategy in strategies {
            if let step = strategy.findStep(grid: grid, validator: validator) {
                return step
            }
        }
        return nil
    }
}
