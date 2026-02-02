export interface SudokuCell {
  value: number;
  isInitial: boolean;
  isSelected: boolean;
  notes: number[];
  isSolverFilled?: boolean;
}

export type SudokuGrid = SudokuCell[][];

export interface GameState {
  grid: SudokuGrid;
  selectedCell: { row: number; col: number } | null;
  difficulty: "easy" | "medium" | "hard";
  isCompleted: boolean;
  startTime: number;
  elapsedTime: number;
  mistakes: number;
  maxMistakes: number;
  isSolverActive: boolean;
  solverHintCells: { row: number; col: number }[];
  solverFilledCells: number; // number of cells filled by solver
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
  solverSpeed: number; // in milliseconds
  solverHelpCells: number; // number of cells to help with
}
