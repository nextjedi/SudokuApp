import { SudokuGrid } from '../types';

/**
 * Creates an empty 9x9 Sudoku grid
 */
export const createEmptyGrid = (): SudokuGrid => {
  const grid: SudokuGrid = [];
  for (let i = 0; i < 9; i++) {
    grid[i] = [];
    for (let j = 0; j < 9; j++) {
      grid[i][j] = {
        value: 0,
        isInitial: false,
        isSelected: false,
        notes: [],
      };
    }
  }
  return grid;
};

/**
 * Checks if a number is valid at a given position
 */
export const isValidMove = (
  grid: number[][],
  row: number,
  col: number,
  num: number
): boolean => {
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
};

/**
 * Solves a Sudoku puzzle using backtracking
 */
export const solveSudoku = (grid: number[][]): boolean => {
  for (let row = 0; row < 9; row++) {
    for (let col = 0; col < 9; col++) {
      if (grid[row][col] === 0) {
        for (let num = 1; num <= 9; num++) {
          if (isValidMove(grid, row, col, num)) {
            grid[row][col] = num;
            if (solveSudoku(grid)) {
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
};

/**
 * Generates a complete valid Sudoku grid
 */
export const generateCompleteGrid = (): number[][] => {
  const grid: number[][] = Array(9)
    .fill(0)
    .map(() => Array(9).fill(0));

  // Fill diagonal 3x3 boxes first (they don't affect each other)
  for (let box = 0; box < 9; box += 3) {
    fillBox(grid, box, box);
  }

  // Solve the rest
  solveSudoku(grid);
  return grid;
};

/**
 * Fills a 3x3 box with random numbers
 */
const fillBox = (grid: number[][], row: number, col: number): void => {
  const nums = [1, 2, 3, 4, 5, 6, 7, 8, 9];
  shuffleArray(nums);

  let index = 0;
  for (let i = 0; i < 3; i++) {
    for (let j = 0; j < 3; j++) {
      grid[row + i][col + j] = nums[index++];
    }
  }
};

/**
 * Shuffles an array in place
 */
const shuffleArray = <T>(array: T[]): void => {
  for (let i = array.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [array[i], array[j]] = [array[j], array[i]];
  }
};

/**
 * Removes numbers from a complete grid based on difficulty
 */
export const createPuzzle = (difficulty: 'easy' | 'medium' | 'hard'): SudokuGrid => {
  const completeGrid = generateCompleteGrid();

  // Number of cells to remove based on difficulty
  const cellsToRemove = {
    easy: 35,
    medium: 45,
    hard: 55,
  }[difficulty];

  const puzzle = createEmptyGrid();

  // Copy complete grid to puzzle
  for (let i = 0; i < 9; i++) {
    for (let j = 0; j < 9; j++) {
      puzzle[i][j].value = completeGrid[i][j];
      puzzle[i][j].isInitial = true;
    }
  }

  // Remove random cells
  let removed = 0;
  while (removed < cellsToRemove) {
    const row = Math.floor(Math.random() * 9);
    const col = Math.floor(Math.random() * 9);

    if (puzzle[row][col].value !== 0) {
      puzzle[row][col].value = 0;
      puzzle[row][col].isInitial = false;
      removed++;
    }
  }

  return puzzle;
};

/**
 * Checks if the puzzle is completely and correctly filled
 */
export const isPuzzleComplete = (grid: SudokuGrid): boolean => {
  // First check if all cells are filled
  for (let i = 0; i < 9; i++) {
    for (let j = 0; j < 9; j++) {
      if (grid[i][j].value === 0) return false;
    }
  }

  // Then check if the solution is valid
  const numGrid = grid.map(row => row.map(cell => cell.value));

  // Check all rows
  for (let i = 0; i < 9; i++) {
    const seen = new Set<number>();
    for (let j = 0; j < 9; j++) {
      if (seen.has(numGrid[i][j])) return false;
      seen.add(numGrid[i][j]);
    }
  }

  // Check all columns
  for (let j = 0; j < 9; j++) {
    const seen = new Set<number>();
    for (let i = 0; i < 9; i++) {
      if (seen.has(numGrid[i][j])) return false;
      seen.add(numGrid[i][j]);
    }
  }

  // Check all 3x3 boxes
  for (let boxRow = 0; boxRow < 3; boxRow++) {
    for (let boxCol = 0; boxCol < 3; boxCol++) {
      const seen = new Set<number>();
      for (let i = 0; i < 3; i++) {
        for (let j = 0; j < 3; j++) {
          const value = numGrid[boxRow * 3 + i][boxCol * 3 + j];
          if (seen.has(value)) return false;
          seen.add(value);
        }
      }
    }
  }

  return true;
};

/**
 * Checks if a specific move is valid
 */
export const isValidMoveInGrid = (
  grid: SudokuGrid,
  row: number,
  col: number,
  num: number
): boolean => {
  const numGrid = grid.map(row => row.map(cell => cell.value));
  return isValidMove(numGrid, row, col, num);
};
