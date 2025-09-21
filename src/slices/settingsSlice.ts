import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { SettingsState } from '../types';

const initialState: SettingsState = {
  soundEnabled: true,
  highlightEnabled: true,
  notesEnabled: true,
  timerEnabled: true,
  darkMode: false,
};

const settingsSlice = createSlice({
  name: 'settings',
  initialState,
  reducers: {
    toggleSound: (state) => {
      state.soundEnabled = !state.soundEnabled;
    },
    toggleHighlight: (state) => {
      state.highlightEnabled = !state.highlightEnabled;
    },
    toggleNotes: (state) => {
      state.notesEnabled = !state.notesEnabled;
    },
    toggleTimer: (state) => {
      state.timerEnabled = !state.timerEnabled;
    },
    toggleDarkMode: (state) => {
      state.darkMode = !state.darkMode;
    },
    updateSettings: (state, action: PayloadAction<Partial<SettingsState>>) => {
      return { ...state, ...action.payload };
    },
  },
});

export const {
  toggleSound,
  toggleHighlight,
  toggleNotes,
  toggleTimer,
  toggleDarkMode,
  updateSettings,
} = settingsSlice.actions;

export default settingsSlice.reducer;