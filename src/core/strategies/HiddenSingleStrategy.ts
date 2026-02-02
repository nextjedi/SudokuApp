import { SudokuGrid, SolverStep } from "../../types";
import { ISudokuValidator } from "../SudokuValidator";
import { ISolvingStrategy } from "../SudokuSolver";

export class HiddenSingleStrategy implements ISolvingStrategy {
  getName(): string {
    return "Hidden Single";
  }

  findStep(grid: SudokuGrid, validator: ISudokuValidator): SolverStep | null {
    const numGrid = grid.map((r) => r.map((c) => c.value));

    // Check rows
    for (let row = 0; row < 9; row++) {
      const candidates = this.getRowCandidates(numGrid, grid, row, validator);
      for (let num = 1; num <= 9; num++) {
        if (candidates[num].length === 1) {
          const col = candidates[num][0];
          if (!grid[row][col].isInitial) {
            return {
              row,
              col,
              value: num,
              strategy: this.getName(),
              hintCells: [],
              reasoning: `[${this.getName()}] The number ${num} can only be placed in cell (${row + 1},${col + 1}) within its row.`,
            };
          }
        }
      }
    }

    // Check columns
    for (let col = 0; col < 9; col++) {
      const candidates = this.getColCandidates(numGrid, grid, col, validator);
      for (let num = 1; num <= 9; num++) {
        if (candidates[num].length === 1) {
          const row = candidates[num][0];
          if (!grid[row][col].isInitial) {
            return {
              row,
              col,
              value: num,
              strategy: this.getName(),
              hintCells: [],
              reasoning: `[${this.getName()}] The number ${num} can only be placed in cell (${row + 1},${col + 1}) within its column.`,
            };
          }
        }
      }
    }

    // Check boxes
    for (let box = 0; box < 9; box++) {
      const candidates = this.getBoxCandidates(numGrid, grid, box, validator);
      for (let num = 1; num <= 9; num++) {
        if (candidates[num].length === 1) {
          const boxRow = Math.floor(box / 3) * 3;
          const boxCol = (box % 3) * 3;
          const cellIndex = candidates[num][0];
          const cellRow = boxRow + Math.floor(cellIndex / 3);
          const cellCol = boxCol + (cellIndex % 3);
          if (!grid[cellRow][cellCol].isInitial) {
            return {
              row: cellRow,
              col: cellCol,
              value: num,
              strategy: this.getName(),
              hintCells: [],
              reasoning: `[${this.getName()}] The number ${num} can only be placed in cell (${cellRow + 1},${cellCol + 1}) within its box.`,
            };
          }
        }
      }
    }

    return null;
  }

  private getRowCandidates(
    numGrid: number[][],
    grid: SudokuGrid,
    row: number,
    validator: ISudokuValidator,
  ): number[][] {
    const candidates: number[][] = Array.from({ length: 10 }, () => []);
    for (let col = 0; col < 9; col++) {
      if (numGrid[row][col] === 0 && !grid[row][col].isInitial) {
        const cellCandidates = validator.getCandidates(numGrid, row, col);
        cellCandidates.forEach((num: number) => candidates[num].push(col));
      }
    }
    return candidates;
  }

  private getColCandidates(
    numGrid: number[][],
    grid: SudokuGrid,
    col: number,
    validator: ISudokuValidator,
  ): number[][] {
    const candidates: number[][] = Array.from({ length: 10 }, () => []);
    for (let row = 0; row < 9; row++) {
      if (numGrid[row][col] === 0 && !grid[row][col].isInitial) {
        const cellCandidates = validator.getCandidates(numGrid, row, col);
        cellCandidates.forEach((num: number) => candidates[num].push(row));
      }
    }
    return candidates;
  }

  private getBoxCandidates(
    numGrid: number[][],
    grid: SudokuGrid,
    box: number,
    validator: ISudokuValidator,
  ): number[][] {
    const candidates: number[][] = Array.from({ length: 10 }, () => []);
    const boxRow = Math.floor(box / 3) * 3;
    const boxCol = (box % 3) * 3;

    let index = 0;
    for (let i = 0; i < 3; i++) {
      for (let j = 0; j < 3; j++) {
        const row = boxRow + i;
        const col = boxCol + j;
        if (numGrid[row][col] === 0 && !grid[row][col].isInitial) {
          const cellCandidates = validator.getCandidates(numGrid, row, col);
          cellCandidates.forEach((num: number) => candidates[num].push(index));
        }
        index++;
      }
    }
    return candidates;
  }
}
