import Foundation

protocol SolvingStrategy {
    var name: String { get }
    func findStep(grid: SudokuGrid, validator: SudokuValidator) -> SolverStep?
}
