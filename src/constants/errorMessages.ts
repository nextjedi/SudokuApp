// Error messages for internationalization
// Default language: English
// Backend should support loading different language files

export const ERROR_MESSAGES = {
  // General errors
  GENERAL_ERROR: {
    title: "Oops! Something went wrong",
    message:
      "We're sorry for the inconvenience. The app encountered an unexpected error.",
    button: "Try Again",
  },

  // Game errors
  INVALID_MOVE: {
    title: "Invalid Move",
    message:
      "This number conflicts with the rules of Sudoku. Please try a different number.",
    button: "OK",
  },

  GAME_OVER: {
    title: "Game Over",
    message: "You made too many mistakes. Better luck next time!",
    button: "New Game",
  },

  PUZZLE_COMPLETE: {
    title: "Congratulations! 🎉",
    message: "You completed the puzzle successfully!",
    button: "Continue",
  },

  // Network errors
  NETWORK_ERROR: {
    title: "Connection Error",
    message:
      "Unable to connect to the server. Please check your internet connection and try again.",
    button: "Retry",
  },

  // Validation errors
  INVALID_INPUT: {
    title: "Invalid Input",
    message: "Please enter valid information.",
    button: "OK",
  },

  // Solver errors
  SOLVER_ERROR: {
    title: "Solver Error",
    message:
      "Unable to solve the puzzle with the current strategies. Try manual solving.",
    button: "OK",
  },

  // File/Storage errors
  SAVE_ERROR: {
    title: "Save Failed",
    message: "Unable to save your progress. Please try again.",
    button: "Retry",
  },

  LOAD_ERROR: {
    title: "Load Failed",
    message: "Unable to load your saved data. Starting fresh.",
    button: "OK",
  },

  // Permission errors
  PERMISSION_DENIED: {
    title: "Permission Required",
    message:
      "This feature requires additional permissions. Please grant them in settings.",
    button: "Open Settings",
  },

  // Timeout errors
  TIMEOUT_ERROR: {
    title: "Request Timeout",
    message: "The request took too long to complete. Please try again.",
    button: "Retry",
  },

  // Unknown errors
  UNKNOWN_ERROR: {
    title: "Unknown Error",
    message:
      "An unknown error occurred. Please contact support if this persists.",
    button: "OK",
  },
} as const;

export type ErrorMessageKey = keyof typeof ERROR_MESSAGES;
export type ErrorMessage = (typeof ERROR_MESSAGES)[ErrorMessageKey];
