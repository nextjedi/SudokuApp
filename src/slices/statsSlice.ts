import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { StatsState } from '../types';

const initialState: StatsState = {
  gamesPlayed: 0,
  gamesWon: 0,
  totalTime: 0,
  bestTimes: {
    easy: Infinity,
    medium: Infinity,
    hard: Infinity,
  },
  currentStreak: 0,
  lastPlayedDate: '',
};

const statsSlice = createSlice({
  name: 'stats',
  initialState,
  reducers: {
    incrementGamesPlayed: (state) => {
      state.gamesPlayed += 1;
    },
    incrementGamesWon: (state) => {
      state.gamesWon += 1;
    },
    addToTotalTime: (state, action: PayloadAction<number>) => {
      state.totalTime += action.payload;
    },
    updateBestTime: (state, action: PayloadAction<{ difficulty: 'easy' | 'medium' | 'hard'; time: number }>) => {
      const { difficulty, time } = action.payload;
      if (time < state.bestTimes[difficulty]) {
        state.bestTimes[difficulty] = time;
      }
    },
    updateStreak: (state, action: PayloadAction<number>) => {
      state.currentStreak = action.payload;
    },
    setLastPlayedDate: (state, action: PayloadAction<string>) => {
      state.lastPlayedDate = action.payload;
    },
    resetStats: () => {
      return initialState;
    },
  },
});

export const {
  incrementGamesPlayed,
  incrementGamesWon,
  addToTotalTime,
  updateBestTime,
  updateStreak,
  setLastPlayedDate,
  resetStats,
} = statsSlice.actions;

export default statsSlice.reducer;