import { SudokuGrid } from "../types";

export interface SolverStep {
  row: number;
  col: number;
  value: number;
  strategy: string;
  hintCells?: { row: number; col: number }[];
  reasoning: string;
}

/**
 * Finds cells that can be filled using basic strategies
 */
export const findNextStep = (grid: SudokuGrid): SolverStep | null => {
  // Convert grid to number array for easier processing
  const numGrid = grid.map((row) => row.map((cell) => cell.value));

  // Only consider empty non-initial cells
  const isInitial = grid.map((row) => row.map((cell) => cell.isInitial));

  // 1. Naked Singles (cells with only one possible number)
  const nakedSingle = findNakedSingle(numGrid, isInitial);
  if (nakedSingle) {
    return {
      ...nakedSingle,
      strategy: "Naked Single",
      hintCells: [],
      reasoning: `[Naked Single] Cell (${nakedSingle.row + 1},${nakedSingle.col + 1}) can only contain ${nakedSingle.value} as it's the only valid number for this cell.`,
    };
  }

  // 2. Hidden Singles (numbers that can only go in one cell in a row/column/box)
  const hiddenSingle = findHiddenSingle(numGrid, isInitial);
  if (hiddenSingle) {
    return {
      ...hiddenSingle,
      strategy: "Hidden Single",
      hintCells: [],
      reasoning: `[Hidden Single] The number ${hiddenSingle.value} can only be placed in cell (${hiddenSingle.row + 1},${hiddenSingle.col + 1}) within its row/column/box.`,
    };
  }

  // 3. Naked Pairs
  const nakedPair = findNakedPair(numGrid, isInitial);
  if (nakedPair) {
    return {
      ...nakedPair,
      strategy: "Naked Pair",
      hintCells: nakedPair.hintCells,
      reasoning: `[Naked Pair] Cells (${nakedPair.hintCells![0].row + 1},${nakedPair.hintCells![0].col + 1}) and (${nakedPair.hintCells![1].row + 1},${nakedPair.hintCells![1].col + 1}) form a naked pair, eliminating ${nakedPair.value} from other cells in the group.`,
    };
  }

  // 4. Naked Triples
  const nakedTriple = findNakedTriple(numGrid, isInitial);
  if (nakedTriple) {
    return {
      ...nakedTriple,
      strategy: "Naked Triple",
      hintCells: nakedTriple.hintCells,
      reasoning: `[Naked Triple] Cells form a naked triple, eliminating candidates from other cells in the group.`,
    };
  }

  // 5. Pointing Pairs/Triples
  const pointingPair = findPointingPair(numGrid, isInitial);
  if (pointingPair) {
    return {
      ...pointingPair,
      strategy: "Pointing Pair/Triple",
      hintCells: pointingPair.hintCells,
      reasoning: `[Pointing Pair/Triple] The number ${pointingPair.value} is confined to a single row/column within its box, eliminating it from other cells in that row/column outside the box.`,
    };
  }

  // 6. Box-Line Reduction
  const boxLineReduction = findBoxLineReduction(numGrid, isInitial);
  if (boxLineReduction) {
    return {
      ...boxLineReduction,
      strategy: "Box-Line Reduction",
      hintCells: boxLineReduction.hintCells,
      reasoning: `[Box-Line Reduction] The number ${boxLineReduction.value} is confined to a single row/column within its box, eliminating it from other cells in that row/column outside the box.`,
    };
  }

  return null;
};

/**
 * Finds a naked single (cell with only one possible number)
 */
const findNakedSingle = (
  grid: number[][],
  isInitial: boolean[][],
): { row: number; col: number; value: number } | null => {
  for (let row = 0; row < 9; row++) {
    for (let col = 0; col < 9; col++) {
      if (grid[row][col] === 0 && !isInitial[row][col]) {
        const candidates = getCandidates(grid, row, col);
        if (candidates.length === 1) {
          return { row, col, value: candidates[0] };
        }
      }
    }
  }
  return null;
};

/**
 * Finds a hidden single
 */
const findHiddenSingle = (
  grid: number[][],
  isInitial: boolean[][],
): { row: number; col: number; value: number } | null => {
  // Check rows
  for (let row = 0; row < 9; row++) {
    const candidates = getRowCandidates(grid, row, isInitial);
    for (let num = 1; num <= 9; num++) {
      if (candidates[num].length === 1) {
        const col = candidates[num][0];
        if (!isInitial[row][col]) {
          return { row, col, value: num };
        }
      }
    }
  }

  // Check columns
  for (let col = 0; col < 9; col++) {
    const candidates = getColCandidates(grid, col, isInitial);
    for (let num = 1; num <= 9; num++) {
      if (candidates[num].length === 1) {
        const row = candidates[num][0];
        if (!isInitial[row][col]) {
          return { row, col, value: num };
        }
      }
    }
  }

  // Check boxes
  for (let box = 0; box < 9; box++) {
    const candidates = getBoxCandidates(grid, box, isInitial);
    for (let num = 1; num <= 9; num++) {
      if (candidates[num].length === 1) {
        const boxRow = Math.floor(box / 3) * 3;
        const boxCol = (box % 3) * 3;
        const cellIndex = candidates[num][0];
        const cellRow = boxRow + Math.floor(cellIndex / 3);
        const cellCol = boxCol + (cellIndex % 3);
        if (!isInitial[cellRow][cellCol]) {
          return { row: cellRow, col: cellCol, value: num };
        }
      }
    }
  }

  return null;
};

/**
 * Finds a naked pair
 */
const findNakedPair = (
  grid: number[][],
  isInitial: boolean[][],
): {
  row: number;
  col: number;
  value: number;
  hintCells: { row: number; col: number }[];
} | null => {
  // Check rows
  for (let row = 0; row < 9; row++) {
    const emptyCells = [];
    for (let col = 0; col < 9; col++) {
      if (grid[row][col] === 0 && !isInitial[row][col]) {
        emptyCells.push({
          row,
          col,
          candidates: getCandidates(grid, row, col),
        });
      }
    }

    for (let i = 0; i < emptyCells.length; i++) {
      for (let j = i + 1; j < emptyCells.length; j++) {
        const cell1 = emptyCells[i];
        const cell2 = emptyCells[j];

        if (
          cell1.candidates.length === 2 &&
          cell2.candidates.length === 2 &&
          arraysEqual(cell1.candidates, cell2.candidates)
        ) {
          // Found naked pair, eliminate these numbers from other cells in row
          const num1 = cell1.candidates[0];
          const num2 = cell1.candidates[1];

          for (let col = 0; col < 9; col++) {
            if (
              grid[row][col] === 0 &&
              col !== cell1.col &&
              col !== cell2.col
            ) {
              const candidates = getCandidates(grid, row, col);
              if (candidates.includes(num1) && candidates.includes(num2)) {
                // Can eliminate, but for simplicity, just return the pair cells
                return {
                  row: cell1.row,
                  col: cell1.col,
                  value: num1,
                  hintCells: [
                    { row: cell1.row, col: cell1.col },
                    { row: cell2.row, col: cell2.col },
                  ],
                };
              }
            }
          }
        }
      }
    }
  }

  return null;
};

/**
 * Finds a naked triple
 */
const findNakedTriple = (
  grid: number[][],
  isInitial: boolean[][],
): {
  row: number;
  col: number;
  value: number;
  hintCells: { row: number; col: number }[];
} | null => {
  // Simplified implementation - look for three cells with three candidates
  for (let row = 0; row < 9; row++) {
    const emptyCells = [];
    for (let col = 0; col < 9; col++) {
      if (grid[row][col] === 0 && !isInitial[row][col]) {
        const candidates = getCandidates(grid, row, col);
        if (candidates.length <= 3) {
          emptyCells.push({ row, col, candidates });
        }
      }
    }

    if (emptyCells.length >= 3) {
      // Check combinations of 3 cells
      for (let i = 0; i < emptyCells.length; i++) {
        for (let j = i + 1; j < emptyCells.length; j++) {
          for (let k = j + 1; k < emptyCells.length; k++) {
            const cells = [emptyCells[i], emptyCells[j], emptyCells[k]];
            const allCandidates = [
              ...new Set(cells.flatMap((c) => c.candidates)),
            ];

            if (allCandidates.length === 3) {
              // Found naked triple
              return {
                row: cells[0].row,
                col: cells[0].col,
                value: cells[0].candidates[0],
                hintCells: cells.map((c) => ({ row: c.row, col: c.col })),
              };
            }
          }
        }
      }
    }
  }

  return null;
};

/**
 * Finds pointing pairs/triples
 */
const findPointingPair = (
  grid: number[][],
  isInitial: boolean[][],
): {
  row: number;
  col: number;
  value: number;
  hintCells: { row: number; col: number }[];
} | null => {
  // Check boxes for numbers that appear only in one row or column within the box
  for (let box = 0; box < 9; box++) {
    const boxRow = Math.floor(box / 3) * 3;
    const boxCol = (box % 3) * 3;

    for (let num = 1; num <= 9; num++) {
      const positions: { row: number; col: number }[] = [];
      for (let i = 0; i < 3; i++) {
        for (let j = 0; j < 3; j++) {
          const row = boxRow + i;
          const col = boxCol + j;
          if (
            grid[row][col] === 0 &&
            !isInitial[row][col] &&
            getCandidates(grid, row, col).includes(num)
          ) {
            positions.push({ row, col });
          }
        }
      }

      if (positions.length >= 2 && positions.length <= 3) {
        // Check if all positions are in same row
        const sameRow = positions.every((p) => p.row === positions[0].row);
        if (sameRow) {
          // Pointing pair/triple in row
          const targetRow = positions[0].row;
          const hintCells = [];

          // Check other cells in the same row outside the box
          for (let col = 0; col < 9; col++) {
            if (col < boxCol || col >= boxCol + 3) {
              if (
                grid[targetRow][col] === 0 &&
                getCandidates(grid, targetRow, col).includes(num)
              ) {
                hintCells.push({ row: targetRow, col });
              }
            }
          }

          if (hintCells.length > 0) {
            return {
              row: positions[0].row,
              col: positions[0].col,
              value: num,
              hintCells: positions,
            };
          }
        }

        // Check if all positions are in same column
        const sameCol = positions.every((p) => p.col === positions[0].col);
        if (sameCol) {
          // Pointing pair/triple in column
          const targetCol = positions[0].col;
          const hintCells = [];

          // Check other cells in the same column outside the box
          for (let row = 0; row < 9; row++) {
            if (row < boxRow || row >= boxRow + 3) {
              if (
                grid[row][targetCol] === 0 &&
                getCandidates(grid, row, targetCol).includes(num)
              ) {
                hintCells.push({ row, col: targetCol });
              }
            }
          }

          if (hintCells.length > 0) {
            return {
              row: positions[0].row,
              col: positions[0].col,
              value: num,
              hintCells: positions,
            };
          }
        }
      }
    }
  }

  return null;
};

/**
 * Finds box-line reduction
 */
const findBoxLineReduction = (
  grid: number[][],
  isInitial: boolean[][],
): {
  row: number;
  col: number;
  value: number;
  hintCells: { row: number; col: number }[];
} | null => {
  // Check if a number in a box is confined to a single row or column
  for (let box = 0; box < 9; box++) {
    const boxRow = Math.floor(box / 3) * 3;
    const boxCol = (box % 3) * 3;

    for (let num = 1; num <= 9; num++) {
      const rowPositions = new Set<number>();
      const colPositions = new Set<number>();
      const positions: { row: number; col: number }[] = [];

      for (let i = 0; i < 3; i++) {
        for (let j = 0; j < 3; j++) {
          const row = boxRow + i;
          const col = boxCol + j;
          if (
            grid[row][col] === 0 &&
            !isInitial[row][col] &&
            getCandidates(grid, row, col).includes(num)
          ) {
            rowPositions.add(row);
            colPositions.add(col);
            positions.push({ row, col });
          }
        }
      }

      if (rowPositions.size === 1 && colPositions.size > 1) {
        // All candidates for this number in the box are in the same row
        const targetRow = Array.from(rowPositions)[0];
        const hintCells = [];

        // Check other cells in the same row outside the box
        for (let col = 0; col < 9; col++) {
          if (col < boxCol || col >= boxCol + 3) {
            if (
              grid[targetRow][col] === 0 &&
              getCandidates(grid, targetRow, col).includes(num)
            ) {
              hintCells.push({ row: targetRow, col });
            }
          }
        }

        if (hintCells.length > 0) {
          // Find first cell in box with this candidate
          for (let j = 0; j < 3; j++) {
            const col = boxCol + j;
            if (
              grid[targetRow][col] === 0 &&
              getCandidates(grid, targetRow, col).includes(num)
            ) {
              return {
                row: targetRow,
                col,
                value: num,
                hintCells,
              };
            }
          }
        }
      }

      if (colPositions.size === 1 && rowPositions.size > 1) {
        // All candidates for this number in the box are in the same column
        const targetCol = Array.from(colPositions)[0];
        const hintCells = [];

        // Check other cells in the same column outside the box
        for (let row = 0; row < 9; row++) {
          if (row < boxRow || row >= boxRow + 3) {
            if (
              grid[row][targetCol] === 0 &&
              getCandidates(grid, row, targetCol).includes(num)
            ) {
              hintCells.push({ row, col: targetCol });
            }
          }
        }

        if (hintCells.length > 0) {
          // Find first cell in box with this candidate
          for (let i = 0; i < 3; i++) {
            const row = boxRow + i;
            if (
              grid[row][targetCol] === 0 &&
              getCandidates(grid, row, targetCol).includes(num)
            ) {
              return {
                row,
                col: targetCol,
                value: num,
                hintCells,
              };
            }
          }
        }
      }
    }
  }

  return null;
};

/**
 * Gets possible candidates for a cell
 */
const getCandidates = (
  grid: number[][],
  row: number,
  col: number,
): number[] => {
  const candidates = [];
  for (let num = 1; num <= 9; num++) {
    if (isValidMove(grid, row, col, num)) {
      candidates.push(num);
    }
  }
  return candidates;
};

/**
 * Gets candidates for each number in a row
 */
const getRowCandidates = (
  grid: number[][],
  row: number,
  isInitial: boolean[][],
): number[][] => {
  const candidates: number[][] = Array.from({ length: 10 }, () => []);
  for (let col = 0; col < 9; col++) {
    if (grid[row][col] === 0 && !isInitial[row][col]) {
      const cellCandidates = getCandidates(grid, row, col);
      cellCandidates.forEach((num) => candidates[num].push(col));
    }
  }
  return candidates;
};

/**
 * Gets candidates for each number in a column
 */
const getColCandidates = (
  grid: number[][],
  col: number,
  isInitial: boolean[][],
): number[][] => {
  const candidates: number[][] = Array.from({ length: 10 }, () => []);
  for (let row = 0; row < 9; row++) {
    if (grid[row][col] === 0 && !isInitial[row][col]) {
      const cellCandidates = getCandidates(grid, row, col);
      cellCandidates.forEach((num) => candidates[num].push(row));
    }
  }
  return candidates;
};

/**
 * Gets candidates for each number in a box (0-8 indexing)
 */
const getBoxCandidates = (
  grid: number[][],
  box: number,
  isInitial: boolean[][],
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
        const cellCandidates = getCandidates(grid, row, col);
        cellCandidates.forEach((num) => candidates[num].push(index));
      }
      index++;
    }
  }
  return candidates;
};

/**
 * Checks if a move is valid
 */
const isValidMove = (
  grid: number[][],
  row: number,
  col: number,
  num: number,
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
 * Utility function to check if two arrays are equal
 */
const arraysEqual = (a: number[], b: number[]): boolean => {
  if (a.length !== b.length) return false;
  return a.every((val, index) => val === b[index]);
};
