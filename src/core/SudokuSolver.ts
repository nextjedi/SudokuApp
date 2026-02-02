import { SudokuGrid, SolverStep } from "../types";
import { ISudokuValidator } from "./SudokuValidator";

/**
 * Interface for Sudoku solving strategies
 */
export interface ISolvingStrategy {
  /**
   * Attempts to find the next step using this strategy
   */
  findStep(grid: SudokuGrid, validator: ISudokuValidator): SolverStep | null;

  /**
   * Gets the name of the strategy
   */
  getName(): string;
}

/**
 * Main solver that uses various strategies
 */
export interface ISudokuSolver {
  /**
   * Finds the next solving step
   */
  findNextStep(
    grid: SudokuGrid,
    validator: ISudokuValidator,
  ): SolverStep | null;
}

/**
 * Implementation of the Sudoku solver using strategy pattern
 */
export class SudokuSolver implements ISudokuSolver {
  private strategies: ISolvingStrategy[];

  constructor(strategies: ISolvingStrategy[]) {
    this.strategies = strategies;
  }

  findNextStep(
    grid: SudokuGrid,
    validator: ISudokuValidator,
  ): SolverStep | null {
    for (const strategy of this.strategies) {
      const step = strategy.findStep(grid, validator);
      if (step) {
        return step;
      }
    }
    return null;
  }
}
