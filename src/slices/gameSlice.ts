import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { GameState, SudokuGrid } from "../types";

const initialState: GameState = {
  grid: [],
  selectedCell: null,
  difficulty: "easy",
  isCompleted: false,
  startTime: 0,
  elapsedTime: 0,
  mistakes: 0,
  maxMistakes: 3,
  isSolverActive: false,
  solverHintCells: [],
  solverFilledCells: 0,
};

const gameSlice = createSlice({
  name: "game",
  initialState,
  reducers: {
    setGrid: (state, action: PayloadAction<SudokuGrid>) => {
      state.grid = action.payload;
    },
    selectCell: (
      state,
      action: PayloadAction<{ row: number; col: number }>,
    ) => {
      state.selectedCell = action.payload;
    },
    setCellValue: (
      state,
      action: PayloadAction<{ row: number; col: number; value: number }>,
    ) => {
      const { row, col, value } = action.payload;
      if (
        state.grid[row] &&
        state.grid[row][col] &&
        !state.grid[row][col].isInitial
      ) {
        state.grid[row][col].value = value;
      }
    },
    setDifficulty: (
      state,
      action: PayloadAction<"easy" | "medium" | "hard">,
    ) => {
      state.difficulty = action.payload;
    },
    startGame: (state) => {
      state.startTime = Date.now();
      state.isCompleted = false;
      state.mistakes = 0;
      state.elapsedTime = 0;
    },
    updateElapsedTime: (state, action: PayloadAction<number>) => {
      state.elapsedTime = action.payload;
    },
    incrementMistakes: (state) => {
      state.mistakes += 1;
    },
    completeGame: (state) => {
      state.isCompleted = true;
    },
    startSolver: (state) => {
      state.isSolverActive = true;
      state.solverFilledCells = 0;
    },
    stopSolver: (state) => {
      state.isSolverActive = false;
      state.solverHintCells = [];
    },
    setSolverHintCells: (
      state,
      action: PayloadAction<{ row: number; col: number }[]>,
    ) => {
      state.solverHintCells = action.payload;
    },
    incrementSolverFilledCells: (state) => {
      state.solverFilledCells += 1;
    },
    resetGame: () => {
      return initialState;
    },
  },
});

export const {
  setGrid,
  selectCell,
  setCellValue,
  setDifficulty,
  startGame,
  updateElapsedTime,
  incrementMistakes,
  completeGame,
  startSolver,
  stopSolver,
  setSolverHintCells,
  incrementSolverFilledCells,
  resetGame,
} = gameSlice.actions;

export default gameSlice.reducer;
