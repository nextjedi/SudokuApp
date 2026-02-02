import { SudokuGrid } from "../types";
import { ISudokuValidator } from "./SudokuValidator";

export type Difficulty = "easy" | "medium" | "hard";

/**
 * Interface for puzzle generation
 */
export interface IPuzzleGenerator {
  /**
   * Generates a puzzle of the specified difficulty
   */
  generatePuzzle(difficulty: Difficulty): SudokuGrid;
}

/**
 * Implementation of puzzle generation logic
 */
export class PuzzleGenerator implements IPuzzleGenerator {
  private validator: ISudokuValidator;

  constructor(validator: ISudokuValidator) {
    this.validator = validator;
  }

  generatePuzzle(difficulty: Difficulty): SudokuGrid {
    const completeGrid = this.generateCompleteGrid();
    return this.createPuzzleFromComplete(completeGrid, difficulty);
  }

  private generateCompleteGrid(): number[][] {
    const grid: number[][] = Array(9)
      .fill(0)
      .map(() => Array(9).fill(0));

    // Fill diagonal 3x3 boxes first (they don't affect each other)
    for (let box = 0; box < 9; box += 3) {
      this.fillBox(grid, box, box);
    }

    // Solve the rest
    this.solveSudoku(grid);
    return grid;
  }

  private solveSudoku(grid: number[][]): boolean {
    for (let row = 0; row < 9; row++) {
      for (let col = 0; col < 9; col++) {
        if (grid[row][col] === 0) {
          for (let num = 1; num <= 9; num++) {
            if (this.validator.isValidMove(grid, row, col, num)) {
              grid[row][col] = num;
              if (this.solveSudoku(grid)) {
                return true;
              }
              grid[row][col] = 0;
            }
          }
          return false;
        }
      }
    }
    return true;
  }

  private fillBox(grid: number[][], row: number, col: number): void {
    const nums = [1, 2, 3, 4, 5, 6, 7, 8, 9];
    this.shuffleArray(nums);

    let index = 0;
    for (let i = 0; i < 3; i++) {
      for (let j = 0; j < 3; j++) {
        grid[row + i][col + j] = nums[index++];
      }
    }
  }

  private shuffleArray<T>(array: T[]): void {
    for (let i = array.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [array[i], array[j]] = [array[j], array[i]];
    }
  }

  private createPuzzleFromComplete(
    completeGrid: number[][],
    difficulty: Difficulty,
  ): SudokuGrid {
    const cellsToRemove = {
      easy: 35,
      medium: 45,
      hard: 55,
    }[difficulty];

    const grid: SudokuGrid = [];
    for (let i = 0; i < 9; i++) {
      grid[i] = [];
      for (let j = 0; j < 9; j++) {
        grid[i][j] = {
          value: completeGrid[i][j],
          isInitial: true,
          isSelected: false,
          notes: [],
          isSolverFilled: false,
        };
      }
    }

    // Remove random cells
    let removed = 0;
    while (removed < cellsToRemove) {
      const row = Math.floor(Math.random() * 9);
      const col = Math.floor(Math.random() * 9);

      if (grid[row][col].value !== 0) {
        grid[row][col].value = 0;
        grid[row][col].isInitial = false;
        removed++;
      }
    }

    return grid;
  }
}
