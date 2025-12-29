import React, { useEffect, useState } from "react";
import { View, Text, StyleSheet, ScrollView, Platform } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { showConfirmation, showNotification } from "../utils/alertHelper";
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
import { Grid } from "../components/Grid";
import { NumberPad } from "../components/NumberPad";
import { Timer } from "../components/Timer";
import { Button } from "../components/Button";
import {
  createPuzzle,
  isPuzzleComplete,
  isValidMoveInGrid,
} from "../utils/sudokuGenerator";
import { getTodayString, isSameDay, isConsecutiveDay } from "../utils/helpers";

interface GameScreenProps {
  onExit: () => void;
}

export const GameScreen: React.FC<GameScreenProps> = ({ onExit }) => {
  const dispatch = useDispatch();
  const game = useSelector((state: RootState) => state.game);
  const stats = useSelector((state: RootState) => state.stats);
  const settings = useSelector((state: RootState) => state.settings);
  const [timerInterval, setTimerInterval] = useState<NodeJS.Timeout | null>(
    null,
  );

  useEffect(() => {
    const puzzle = createPuzzle(game.difficulty);
    dispatch(setGrid(puzzle));
    dispatch(startGame());
    dispatch(incrementGamesPlayed());

    // ---- streak logic ----
    const today = getTodayString();

    if (isSameDay(stats.lastPlayedDate, today)) {
      // same-day: keep streak
    } else if (isConsecutiveDay(stats.lastPlayedDate, today)) {
      dispatch(updateStreak(stats.currentStreak + 1));
    } else {
      dispatch(updateStreak(1));
    }
    dispatch(setLastPlayedDate(today));

    return () => {
      if (timerInterval) clearInterval(timerInterval);
    };
  }, []);

  useEffect(() => {
    if (!game.startTime) return;

    const interval = setInterval(() => {
      if (!game.isCompleted) {
        const elapsed = Math.floor((Date.now() - game.startTime) / 1000);
        dispatch(updateElapsedTime(elapsed));
      }
    }, 1000);

    setTimerInterval(interval);
    return () => clearInterval(interval);
  }, [game.startTime, game.isCompleted]);

  useEffect(() => {
    if (game.grid.length > 0 && isPuzzleComplete(game.grid)) {
      handleGameComplete();
    }
  }, [game.grid]);

  useEffect(() => {
    if (Platform.OS !== "web") return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key >= "1" && e.key <= "9") {
        handleNumberPress(parseInt(e.key, 10));
      } else if (e.key === "Backspace" || e.key === "Delete") {
        handleErase();
      } else if (e.key.startsWith("Arrow") && game.selectedCell) {
        e.preventDefault();
        const { row, col } = game.selectedCell;

        let newRow = row;
        let newCol = col;

        switch (e.key) {
          case "ArrowUp":
            newRow = Math.max(0, row - 1);
            break;
          case "ArrowDown":
            newRow = Math.min(8, row + 1);
            break;
          case "ArrowLeft":
            newCol = Math.max(0, col - 1);
            break;
          case "ArrowRight":
            newCol = Math.min(8, col + 1);
            break;
        }

        if (newRow !== row || newCol !== col) {
          dispatch(selectCell({ row: newRow, col: newCol }));
        }
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [game.selectedCell, game.isCompleted]);

  const handleGameComplete = () => {
    if (game.isCompleted) return;

    dispatch(completeGame());
    if (timerInterval) clearInterval(timerInterval);

    dispatch(incrementGamesWon());
    dispatch(addToTotalTime(game.elapsedTime));
    dispatch(
      updateBestTime({ difficulty: game.difficulty, time: game.elapsedTime }),
    );

    setTimeout(() => {
      showNotification(
        "Congratulations! 🎉",
        `You completed the puzzle in ${Math.floor(game.elapsedTime / 60)}:${(
          game.elapsedTime % 60
        )
          .toString()
          .padStart(2, "0")}!`,
        onExit,
      );
    }, 500);
  };

  const handleCellPress = (row: number, col: number) => {
    if (game.isCompleted) return;
    dispatch(selectCell({ row, col }));
  };

  const handleNumberPress = (num: number) => {
    if (game.isCompleted || !game.selectedCell) return;

    const { row, col } = game.selectedCell;
    const cell = game.grid[row][col];

    if (cell.isInitial) return;

    if (num !== 0 && !isValidMoveInGrid(game.grid, row, col, num)) {
      dispatch(incrementMistakes());
      if (game.mistakes + 1 >= game.maxMistakes) {
        showNotification("Game Over", "Too many mistakes!", onExit);
        return;
      } else {
        showNotification(
          "Invalid Move",
          "This number conflicts with Sudoku rules!",
        );
      }
      return;
    }

    dispatch(setCellValue({ row, col, value: num }));
  };

  const handleErase = () => {
    if (game.isCompleted || !game.selectedCell) return;

    const { row, col } = game.selectedCell;
    const cell = game.grid[row][col];

    if (cell.isInitial) return;

    dispatch(setCellValue({ row, col, value: 0 }));
  };

  const handleNewGame = () => {
    showConfirmation(
      "New Game",
      "Start a new game? Current progress will be lost.",
      () => {
        if (timerInterval) clearInterval(timerInterval);
        const puzzle = createPuzzle(game.difficulty);
        dispatch(setGrid(puzzle));
        dispatch(startGame()); // new startTime triggers timer
        dispatch(incrementGamesPlayed());
      },
    );
  };

  const handleExit = () => {
    showConfirmation("Exit Game", "Exit the current game?", () => {
      if (timerInterval) clearInterval(timerInterval);
      onExit();
    });
  };

  // ---------------------------------------------------------
  //  UI RENDER
  // ---------------------------------------------------------
  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent}>
        <View style={styles.header}>
          <View style={styles.headerInfo}>
            <Text style={styles.difficulty}>
              {game.difficulty.charAt(0).toUpperCase() +
                game.difficulty.slice(1)}
            </Text>
            <Text style={styles.mistakes}>
              ❌ {game.mistakes}/{game.maxMistakes}
            </Text>
          </View>

          {settings.timerEnabled && <Timer seconds={game.elapsedTime} />}
        </View>

        <View style={styles.gridContainer}>
          <Grid
            grid={game.grid}
            selectedCell={game.selectedCell}
            onCellPress={handleCellPress}
            highlightEnabled={settings.highlightEnabled}
          />
        </View>

        <View style={styles.controls}>
          <NumberPad
            onNumberPress={handleNumberPress}
            onErase={handleErase}
            disabled={game.isCompleted}
          />
        </View>

        <View style={styles.actions}>
          <Button
            title="New Game"
            onPress={handleNewGame}
            variant="secondary"
            style={styles.actionButton}
          />
          <Button
            title="Exit"
            onPress={handleExit}
            variant="danger"
            style={styles.actionButton}
          />
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#F8F9FA",
  },
  scrollContent: {
    flexGrow: 1,
    paddingVertical: 16,
  },
  header: {
    paddingHorizontal: 24,
    marginBottom: 20,
  },
  headerInfo: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 12,
  },
  difficulty: {
    fontSize: 24,
    fontWeight: "700",
    color: "#2C3E50",
  },
  mistakes: {
    fontSize: 18,
    fontWeight: "600",
    color: "#E74C3C",
  },
  gridContainer: {
    alignItems: "center",
    marginBottom: 20,
  },
  controls: {
    marginBottom: 20,
  },
  actions: {
    flexDirection: "row",
    paddingHorizontal: 24,
    gap: 12,
  },
  actionButton: {
    flex: 1,
  },
});
