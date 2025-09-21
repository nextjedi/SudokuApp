import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { GameState, SudokuGrid } from '../types';

const initialState: GameState = {
  grid: [],
  selectedCell: null,
  difficulty: 'easy',
  isCompleted: false,
  startTime: 0,
  elapsedTime: 0,
  mistakes: 0,
  maxMistakes: 3,
};

const gameSlice = createSlice({
  name: 'game',
  initialState,
  reducers: {
    setGrid: (state, action: PayloadAction<SudokuGrid>) => {
      state.grid = action.payload;
    },
    selectCell: (state, action: PayloadAction<{ row: number; col: number }>) => {
      state.selectedCell = action.payload;
    },
    setCellValue: (state, action: PayloadAction<{ row: number; col: number; value: number }>) => {
      const { row, col, value } = action.payload;
      if (state.grid[row] && state.grid[row][col] && !state.grid[row][col].isInitial) {
        state.grid[row][col].value = value;
      }
    },
    setDifficulty: (state, action: PayloadAction<'easy' | 'medium' | 'hard'>) => {
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
  resetGame,
} = gameSlice.actions;

export default gameSlice.reducer;