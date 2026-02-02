// Translation system for internationalization
// Default language: English
// Backend can load different language files dynamically

export interface TranslationKeys {
  // Navigation
  home: string;
  game: string;
  stats: string;
  settings: string;
  blog: string;

  // Home Screen
  sudokuStreak: string;
  buildDailyHabit: string;
  dayStreak: string;
  selectDifficulty: string;
  easy: string;
  medium: string;
  hard: string;
  newGame: string;

  // Game Screen
  difficulty: string;
  mistakes: string;
  congratulations: string;
  puzzleCompleted: string;
  continue: string;
  startSolver: string;
  stopSolver: string;
  solverSettings: string;
  speed: string;
  cells: string;
  stepsAndReasoning: string;
  invalidMove: string;
  gameOver: string;
  tooManyMistakes: string;

  // Stats Screen
  statistics: string;
  gamesPlayed: string;
  gamesWon: string;
  winRate: string;
  currentStreak: string;
  bestStreak: string;
  totalTime: string;
  averageTime: string;
  bestTimes: string;

  // Settings Screen
  soundEnabled: string;
  highlightEnabled: string;
  notesEnabled: string;
  timerEnabled: string;
  darkMode: string;
  solverSpeed: string;
  solverHelpCells: string;

  // Error Messages (already in errorMessages.ts)
  // Common UI elements
  ok: string;
  cancel: string;
  confirm: string;
  retry: string;
  save: string;
  load: string;
  reset: string;
  close: string;
  back: string;

  // Solver reasoning
  nakedSingle: string;
  hiddenSingle: string;
  nakedPair: string;
  nakedTriple: string;
  pointingPair: string;
  boxLineReduction: string;

  // Time formatting
  seconds: string;
  minutes: string;
  hours: string;

  // Confirmation messages
  confirmExit: string;
  confirmNewGame: string;
  progressWillBeLost: string;
}

export const ENGLISH_TRANSLATIONS: TranslationKeys = {
  // Navigation
  home: "Home",
  game: "Game",
  stats: "Stats",
  settings: "Settings",
  blog: "Blog",

  // Home Screen
  sudokuStreak: "Sudoku Streak",
  buildDailyHabit: "Build Your Daily Puzzle Habit",
  dayStreak: "day streak",
  selectDifficulty: "Select Difficulty",
  easy: "Easy",
  medium: "Medium",
  hard: "Hard",
  newGame: "New Game",
  difficulty: "Difficulty",
  mistakes: "Mistakes",
  congratulations: "Congratulations! 🎉",
  puzzleCompleted: "You completed the puzzle successfully!",
  continue: "Continue",
  startSolver: "Start Solver",
  stopSolver: "Stop Solver",
  solverSettings: "Solver Settings",
  speed: "Speed",
  cells: "Cells",
  stepsAndReasoning: "Steps and Reasoning",
  invalidMove: "Invalid Move",
  gameOver: "Game Over",
  tooManyMistakes: "You made too many mistakes. Better luck next time!",

  // Stats Screen
  statistics: "Statistics",
  gamesPlayed: "Games Played",
  gamesWon: "Games Won",
  winRate: "Win Rate",
  currentStreak: "Current Streak",
  bestStreak: "Best Streak",
  totalTime: "Total Time",
  averageTime: "Average Time",
  bestTimes: "Best Times",

  // Settings Screen
  soundEnabled: "Sound Enabled",
  highlightEnabled: "Highlight Enabled",
  notesEnabled: "Notes Enabled",
  timerEnabled: "Timer Enabled",
  darkMode: "Dark Mode",
  solverSpeed: "Solver Speed",
  solverHelpCells: "Solver Help Cells",

  // Common UI elements
  ok: "OK",
  cancel: "Cancel",
  confirm: "Confirm",
  retry: "Retry",
  save: "Save",
  load: "Load",
  reset: "Reset",
  close: "Close",
  back: "Back",

  // Solver reasoning
  nakedSingle: "Naked Single",
  hiddenSingle: "Hidden Single",
  nakedPair: "Naked Pair",
  nakedTriple: "Naked Triple",
  pointingPair: "Pointing Pair/Triple",
  boxLineReduction: "Box-Line Reduction",

  // Time formatting
  seconds: "s",
  minutes: "m",
  hours: "h",

  // Confirmation messages
  confirmExit: "Exit Game",
  confirmNewGame: "New Game",
  progressWillBeLost: "Are you sure? Current progress will be lost.",
};

// For now, we only support English
// In production, this would be loaded dynamically based on user language
export const TRANSLATIONS = ENGLISH_TRANSLATIONS;

// Type for translation key
export type TranslationKey = keyof TranslationKeys;
