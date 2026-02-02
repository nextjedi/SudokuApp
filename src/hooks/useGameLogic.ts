import { useEffect, useState, useCallback, useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";
import { RootState } from "../store/store";
import {
  setGrid,
  selectCell,
  setCellValue,
  startGame,
  updateElapsedTime,
  completeGame,
  incrementMistakes,
} from "../slices/gameSlice";
import {
  incrementGamesPlayed,
  incrementGamesWon,
  addToTotalTime,
  updateBestTime,
  updateStreak,
  setLastPlayedDate,
} from "../slices/statsSlice";
import {
  createPuzzle,
  isPuzzleComplete,
  isValidMoveInGrid,
} from "../utils/sudokuGenerator";
import { getTodayString, isSameDay, isConsecutiveDay } from "../utils/helpers";
import { showNotification, showConfirmation } from "../utils/alertHelper";

import { handleUserError, createErrorContext } from "../utils/errorUtils";

export const useGameLogic = () => {
  const dispatch = useDispatch();
  const game = useSelector((state: RootState) => state.game);
  const stats = useSelector((state: RootState) => state.stats);
  const [timerInterval, setTimerInterval] = useState<NodeJS.Timeout | null>(
    null,
  );

  // Memoize completion check
  const isGameComplete = useMemo(() => {
    return game.grid.length > 0 && isPuzzleComplete(game.grid);
  }, [game.grid]);

  useEffect(() => {
    // Check for completion when grid changes
    if (isGameComplete) {
      handleGameComplete();
    }
  }, [isGameComplete]);

  // Start timer when game starts
  useEffect(() => {
    if (game.startTime > 0 && !game.isCompleted) {
      if (timerInterval) clearInterval(timerInterval);
      const interval = setInterval(() => {
        const elapsed = Math.floor((Date.now() - game.startTime) / 1000);
        dispatch(updateElapsedTime(elapsed));
      }, 1000);
      setTimerInterval(interval);
    } else if (game.isCompleted && timerInterval) {
      clearInterval(timerInterval);
      setTimerInterval(null);
    }

    return () => {
      if (timerInterval) {
        clearInterval(timerInterval);
      }
    };
  }, [game.startTime, game.isCompleted, dispatch]);

  // Initialize game on mount
  useEffect(() => {
    startNewGame();
  }, []); // Empty dependency - only run on mount

  const startNewGame = useCallback(() => {
    const puzzle = createPuzzle(game.difficulty);
    dispatch(setGrid(puzzle));
    dispatch(startGame());
    dispatch(incrementGamesPlayed());

    // Update streak
    const today = getTodayString();
    if (isSameDay(stats.lastPlayedDate, today)) {
      // Already played today, keep streak
    } else if (isConsecutiveDay(stats.lastPlayedDate, today)) {
      // Consecutive day, increment streak
      dispatch(updateStreak(stats.currentStreak + 1));
    } else if (stats.lastPlayedDate === "") {
      // First time playing
      dispatch(updateStreak(1));
    } else {
      // Streak broken
      dispatch(updateStreak(1));
    }
    dispatch(setLastPlayedDate(today));
  }, [dispatch, game.difficulty, stats.lastPlayedDate, stats.currentStreak]);

  const handleGameComplete = useCallback(() => {
    if (game.isCompleted) return; // Prevent multiple completions

    dispatch(completeGame());
    if (timerInterval) {
      clearInterval(timerInterval);
      setTimerInterval(null);
    }

    dispatch(incrementGamesWon());
    dispatch(addToTotalTime(game.elapsedTime));
    dispatch(
      updateBestTime({ difficulty: game.difficulty, time: game.elapsedTime }),
    );

    setTimeout(() => {
      showNotification(
        "Congratulations! 🎉",
        `You completed the puzzle in ${Math.floor(game.elapsedTime / 60)}:${(game.elapsedTime % 60).toString().padStart(2, "0")}!`,
        () => {}, // TODO: handle exit
      );
    }, 500);
  }, [
    game.isCompleted,
    game.elapsedTime,
    game.difficulty,
    timerInterval,
    dispatch,
  ]);

  const handleCellPress = useCallback(
    (row: number, col: number) => {
      if (game.isCompleted) return;
      dispatch(selectCell({ row, col }));
    },
    [game.isCompleted, dispatch],
  );

  const handleNumberPress = useCallback(
    (num: number) => {
      if (game.isCompleted || !game.selectedCell) return;

      const { row, col } = game.selectedCell;
      const cell = game.grid[row]?.[col];

      if (!cell || cell.isInitial) return; // Can't change initial cells

      // Check if move is valid
      if (num !== 0 && !isValidMoveInGrid(game.grid, row, col, num)) {
        dispatch(incrementMistakes());
        if (game.mistakes + 1 >= game.maxMistakes) {
          showNotification(
            "Game Over",
            "You made too many mistakes!",
            () => {},
          ); // TODO
          return;
        } else {
          showNotification(
            "Invalid Move",
            "This number conflicts with the rules!",
          );
          return;
        }
      }

      dispatch(setCellValue({ row, col, value: num }));
    },
    [
      game.isCompleted,
      game.selectedCell,
      game.grid,
      game.mistakes,
      game.maxMistakes,
      dispatch,
    ],
  );

  const handleErase = useCallback(() => {
    if (game.isCompleted || !game.selectedCell) return;

    const { row, col } = game.selectedCell;
    const cell = game.grid[row]?.[col];

    if (!cell || cell.isInitial) return;

    dispatch(setCellValue({ row, col, value: 0 }));
  }, [game.isCompleted, game.selectedCell, game.grid, dispatch]);

  const handleNewGameDirect = useCallback(
    async (difficulty: "easy" | "medium" | "hard") => {
      try {
        if (timerInterval) {
          clearInterval(timerInterval);
          setTimerInterval(null);
        }
        const puzzle = createPuzzle(difficulty);
        dispatch(setGrid(puzzle));
        dispatch(startGame());
        dispatch(incrementGamesPlayed());
      } catch {
        handleUserError("GENERAL_ERROR", {
          context: createErrorContext("useGameLogic", "newGame"),
        });
      }
    },
    [timerInterval, dispatch],
  );

  const handleNewGame = useCallback(
    (resetSolverStates?: () => void) => {
      showConfirmation(
        "New Game",
        "Are you sure you want to start a new game? Current progress will be lost.",
        () => {
          resetSolverStates?.(); // Reset solver states
          handleNewGameDirect(game.difficulty);
        },
        undefined,
        "New Game",
      );
    },
    [game.difficulty, handleNewGameDirect],
  );

  const handleExit = useCallback(() => {
    showConfirmation(
      "Exit Game",
      "Are you sure you want to exit? Current progress will be lost.",
      () => {
        if (timerInterval) {
          clearInterval(timerInterval);
          setTimerInterval(null);
        }
        // TODO: handle exit
      },
      undefined,
      "Exit",
    );
  }, [timerInterval]);

  return {
    handleCellPress,
    handleNumberPress,
    handleErase,
    handleNewGame,
    handleExit,
  };
};
