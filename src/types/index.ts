export interface SudokuCell {
  value: number;
  isInitial: boolean;
  isSelected: boolean;
  notes: number[];
}

export type SudokuGrid = SudokuCell[][];

export interface GameState {
  grid: SudokuGrid;
  selectedCell: { row: number; col: number } | null;
  difficulty: 'easy' | 'medium' | 'hard';
  isCompleted: boolean;
  startTime: number;
  elapsedTime: number;
  mistakes: number;
  maxMistakes: number;
}

export interface StatsState {
  gamesPlayed: number;
  gamesWon: number;
  totalTime: number;
  bestTimes: {
    easy: number;
    medium: number;
    hard: number;
  };
  currentStreak: number;
  lastPlayedDate: string;
}

export interface SettingsState {
  soundEnabled: boolean;
  highlightEnabled: boolean;
  notesEnabled: boolean;
  timerEnabled: boolean;
  darkMode: boolean;
}