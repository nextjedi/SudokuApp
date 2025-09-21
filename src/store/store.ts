import { configureStore } from '@reduxjs/toolkit';
import gameReducer from '../slices/gameSlice';
import statsReducer from '../slices/statsSlice';
import settingsReducer from '../slices/settingsSlice';

export const store = configureStore({
  reducer: {
    game: gameReducer,
    stats: statsReducer,
    settings: settingsReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;