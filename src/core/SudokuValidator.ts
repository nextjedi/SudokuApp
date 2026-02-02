import { SudokuGrid } from "../types";

/**
 * Interface for Sudoku validation operations
 */
export interface ISudokuValidator {
  /**
   * Checks if placing a number at a position is valid
   */
  isValidMove(grid: number[][], row: number, col: number, num: number): boolean;

  /**
   * Checks if the entire grid is valid and complete
   */
  isValidSolution(grid: SudokuGrid): boolean;

  /**
   * Checks if the grid is completely filled
   */
  isComplete(grid: SudokuGrid): boolean;

  /**
   * Gets possible candidates for a cell
   */
  getCandidates(grid: number[][], row: number, col: number): number[];
}

/**
 * Implementation of Sudoku validation logic
 */
export class SudokuValidator implements ISudokuValidator {
  isValidMove(
    grid: number[][],
    row: number,
    col: number,
    num: number,
  ): boolean {
    // Check row
    for (let x = 0; x < 9; x++) {
      if (grid[row][x] === num) return false;
    }

    // Check column
    for (let x = 0; x < 9; x++) {
      if (grid[x][col] === num) return false;
    }

    // Check 3x3 box
    const startRow = Math.floor(row / 3) * 3;
    const startCol = Math.floor(col / 3) * 3;
    for (let i = 0; i < 3; i++) {
      for (let j = 0; j < 3; j++) {
        if (grid[startRow + i][startCol + j] === num) return false;
      }
    }

    return true;
  }

  isValidSolution(grid: SudokuGrid): boolean {
    const numGrid = grid.map((row) => row.map((cell) => cell.value));

    // Check all rows
    for (let i = 0; i < 9; i++) {
      const seen = new Set<number>();
      for (let j = 0; j < 9; j++) {
        const val = numGrid[i][j];
        if (val === 0 || seen.has(val)) return false;
        seen.add(val);
      }
    }

    // Check all columns
    for (let j = 0; j < 9; j++) {
      const seen = new Set<number>();
      for (let i = 0; i < 9; i++) {
        const val = numGrid[i][j];
        if (val === 0 || seen.has(val)) return false;
        seen.add(val);
      }
    }

    // Check all 3x3 boxes
    for (let boxRow = 0; boxRow < 3; boxRow++) {
      for (let boxCol = 0; boxCol < 3; boxCol++) {
        const seen = new Set<number>();
        for (let i = 0; i < 3; i++) {
          for (let j = 0; j < 3; j++) {
            const val = numGrid[boxRow * 3 + i][boxCol * 3 + j];
            if (val === 0 || seen.has(val)) return false;
            seen.add(val);
          }
        }
      }
    }

    return true;
  }

  isComplete(grid: SudokuGrid): boolean {
    return grid.every((row) => row.every((cell) => cell.value !== 0));
  }

  getCandidates(grid: number[][], row: number, col: number): number[] {
    const candidates: number[] = [];
    for (let num = 1; num <= 9; num++) {
      if (this.isValidMove(grid, row, col, num)) {
        candidates.push(num);
      }
    }
    return candidates;
  }
}
