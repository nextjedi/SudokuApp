export interface SudokuCell {
  value: number;
  isInitial: boolean;
  isSelected: boolean;
  notes: number[];
  isSolverFilled?: boolean;
}

export type SudokuGrid = SudokuCell[][];

export type Difficulty = "easy" | "medium" | "hard";

export type Position = {
  row: number;
  col: number;
};

export interface GameState {
  grid: SudokuGrid;
  selectedCell: Position | null;
  difficulty: Difficulty;
  isCompleted: boolean;
  startTime: number;
  elapsedTime: number;
  mistakes: number;
  maxMistakes: number;
  isSolverActive: boolean;
  solverHintCells: Position[];
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

export interface SolverStep {
  row: number;
  col: number;
  value: number;
  strategy: string;
  hintCells?: Position[];
  reasoning: string;
}

export interface SolverStepDisplay {
  cell: string;
  reasoning: string;
  step: number;
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
