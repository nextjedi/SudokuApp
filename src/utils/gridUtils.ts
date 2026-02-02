import { SudokuGrid, SudokuCell } from "../types";

/**
 * Utility class for grid operations
 */
export class GridUtils {
  /**
   * Safely get a cell from the grid
   */
  static getCell(
    grid: SudokuGrid,
    row: number,
    col: number,
  ): SudokuCell | null {
    if (row < 0 || row >= 9 || col < 0 || col >= 9) return null;
    return grid[row]?.[col] || null;
  }

  /**
   * Check if a cell is in the same row as selected cell
   */
  static isSameRow(
    selectedCell: { row: number; col: number } | null,
    row: number,
  ): boolean {
    return selectedCell !== null && selectedCell.row === row;
  }

  /**
   * Check if a cell is in the same column as selected cell
   */
  static isSameColumn(
    selectedCell: { row: number; col: number } | null,
    col: number,
  ): boolean {
    return selectedCell !== null && selectedCell.col === col;
  }

  /**
   * Check if a cell is in the same box as selected cell
   */
  static isSameBox(
    selectedCell: { row: number; col: number } | null,
    row: number,
    col: number,
  ): boolean {
    if (!selectedCell) return false;
    const boxRow = Math.floor(row / 3);
    const boxCol = Math.floor(col / 3);
    const selectedBoxRow = Math.floor(selectedCell.row / 3);
    const selectedBoxCol = Math.floor(selectedCell.col / 3);
    return boxRow === selectedBoxRow && boxCol === selectedBoxCol;
  }

  /**
   * Check if a cell has the same value as selected cell
   */
  static hasSameValue(
    grid: SudokuGrid,
    selectedCell: { row: number; col: number } | null,
    row: number,
    col: number,
  ): boolean {
    if (!selectedCell) return false;
    const selectedValue = GridUtils.getCell(
      grid,
      selectedCell.row,
      selectedCell.col,
    )?.value;
    const currentValue = GridUtils.getCell(grid, row, col)?.value;
    return (
      selectedValue !== undefined &&
      currentValue !== undefined &&
      selectedValue !== 0 &&
      currentValue !== 0 &&
      selectedValue === currentValue
    );
  }

  /**
   * Check if a cell is highlighted
   */
  static isHighlighted(
    grid: SudokuGrid,
    selectedCell: { row: number; col: number } | null,
    row: number,
    col: number,
    highlightEnabled: boolean,
  ): boolean {
    if (!highlightEnabled || !selectedCell) return false;

    return (
      GridUtils.isSameRow(selectedCell, row) ||
      GridUtils.isSameColumn(selectedCell, col) ||
      GridUtils.isSameBox(selectedCell, row, col) ||
      GridUtils.hasSameValue(grid, selectedCell, row, col)
    );
  }

  /**
   * Check if a cell is a solver hint
   */
  static isSolverHint(
    solverHintCells: { row: number; col: number }[],
    row: number,
    col: number,
  ): boolean {
    return solverHintCells.some((cell) => cell.row === row && cell.col === col);
  }

  /**
   * Check if a cell is being filled by solver
   */
  static isSolverFilling(
    solverFillingCell: { row: number; col: number } | null,
    row: number,
    col: number,
  ): boolean {
    return (
      solverFillingCell !== null &&
      solverFillingCell.row === row &&
      solverFillingCell.col === col
    );
  }

  /**
   * Get reasoning for a cell from solver steps
   */
  static getCellReasoning(
    solverSteps: Array<{ cell: string; reasoning: string; step: number }>,
    row: number,
    col: number,
  ): string {
    const cellKey = `${row}-${col}`;
    const step = solverSteps.find((s) => s.cell === cellKey);
    return step ? step.reasoning : "";
  }

  /**
   * Create an empty grid
   */
  static createEmptyGrid(): SudokuGrid {
    const grid: SudokuGrid = [];
    for (let i = 0; i < 9; i++) {
      grid[i] = [];
      for (let j = 0; j < 9; j++) {
        grid[i][j] = {
          value: 0,
          isInitial: false,
          isSelected: false,
          notes: [],
          isSolverFilled: false,
        };
      }
    }
    return grid;
  }

  /**
   * Clone a grid
   */
  static cloneGrid(grid: SudokuGrid): SudokuGrid {
    return grid.map((row) => row.map((cell) => ({ ...cell })));
  }

  /**
   * Validate grid bounds
   */
  static isValidPosition(row: number, col: number): boolean {
    return row >= 0 && row < 9 && col >= 0 && col < 9;
  }
}
