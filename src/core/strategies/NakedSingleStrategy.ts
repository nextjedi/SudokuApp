import { SudokuGrid, SolverStep } from "../../types";
import { ISudokuValidator } from "../SudokuValidator";
import { ISolvingStrategy } from "../SudokuSolver";

export class NakedSingleStrategy implements ISolvingStrategy {
  getName(): string {
    return "Naked Single";
  }

  findStep(grid: SudokuGrid, validator: ISudokuValidator): SolverStep | null {
    for (let row = 0; row < 9; row++) {
      for (let col = 0; col < 9; col++) {
        const cell = grid[row][col];
        if (cell.value === 0 && !cell.isInitial) {
          const numGrid = grid.map((r) => r.map((c) => c.value));
          const candidates = validator.getCandidates(numGrid, row, col);
          if (candidates.length === 1) {
            return {
              row,
              col,
              value: candidates[0],
              strategy: this.getName(),
              hintCells: [],
              reasoning: `[${this.getName()}] Cell (${row + 1},${col + 1}) can only contain ${candidates[0]} as it's the only valid number for this cell.`,
            };
          }
        }
      }
    }
    return null;
  }
}
