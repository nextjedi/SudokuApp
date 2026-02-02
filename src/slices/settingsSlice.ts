import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { SettingsState } from "../types";

const initialState: SettingsState = {
  soundEnabled: true,
  highlightEnabled: true,
  notesEnabled: true,
  timerEnabled: true,
  darkMode: false,
  solverSpeed: 2000, // Default to 2 seconds
  solverHelpCells: 3,
};

const settingsSlice = createSlice({
  name: "settings",
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
    setSolverSpeed: (state, action: PayloadAction<number>) => {
      state.solverSpeed = action.payload;
    },
    setSolverHelpCells: (state, action: PayloadAction<number>) => {
      state.solverHelpCells = action.payload;
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
  setSolverSpeed,
  setSolverHelpCells,
  updateSettings,
} = settingsSlice.actions;

export default settingsSlice.reducer;
