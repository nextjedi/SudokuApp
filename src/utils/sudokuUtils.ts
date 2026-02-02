import { ISudokuValidator } from "../core";

/**
 * Gets possible candidates for a cell
 */
export const getCandidates = (
  grid: number[][],
  row: number,
  col: number,
  validator: ISudokuValidator,
): number[] => {
  return validator.getCandidates(grid, row, col);
};

/**
 * Gets candidates for each number in a row
 */
export const getRowCandidates = (
  grid: number[][],
  row: number,
  isInitial: boolean[][],
  validator: ISudokuValidator,
): number[][] => {
  const candidates: number[][] = Array.from({ length: 10 }, () => []);
  for (let col = 0; col < 9; col++) {
    if (grid[row][col] === 0 && !isInitial[row][col]) {
      const cellCandidates = getCandidates(grid, row, col, validator);
      cellCandidates.forEach((num) => candidates[num].push(col));
    }
  }
  return candidates;
};

/**
 * Gets candidates for each number in a column
 */
export const getColCandidates = (
  grid: number[][],
  col: number,
  isInitial: boolean[][],
  validator: ISudokuValidator,
): number[][] => {
  const candidates: number[][] = Array.from({ length: 10 }, () => []);
  for (let row = 0; row < 9; row++) {
    if (grid[row][col] === 0 && !isInitial[row][col]) {
      const cellCandidates = getCandidates(grid, row, col, validator);
      cellCandidates.forEach((num) => candidates[num].push(row));
    }
  }
  return candidates;
};

/**
 * Gets candidates for each number in a box (0-8 indexing)
 */
export const getBoxCandidates = (
  grid: number[][],
  box: number,
  isInitial: boolean[][],
  validator: ISudokuValidator,
): number[][] => {
  const candidates: number[][] = Array.from({ length: 10 }, () => []);
  const boxRow = Math.floor(box / 3) * 3;
  const boxCol = (box % 3) * 3;

  let index = 0;
  for (let i = 0; i < 3; i++) {
    for (let j = 0; j < 3; j++) {
      const row = boxRow + i;
      const col = boxCol + j;
      if (grid[row][col] === 0 && !isInitial[row][col]) {
        const cellCandidates = getCandidates(grid, row, col, validator);
        cellCandidates.forEach((num) => candidates[num].push(index));
      }
      index++;
    }
  }
  return candidates;
};

/**
 * Utility function to check if two arrays are equal
 */
export const arraysEqual = (a: number[], b: number[]): boolean => {
  if (a.length !== b.length) return false;
  return a.every((val, index) => val === b[index]);
};
