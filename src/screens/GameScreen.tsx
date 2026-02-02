import React, { useEffect, useState, useRef } from "react";
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  ScrollView,
  Platform,
  TouchableOpacity,
} from "react-native";
import { showConfirmation, showNotification } from "../utils/alertHelper";
import { useDispatch, useSelector } from "react-redux";
import { RootState } from "../store/store";
import { SudokuGrid } from "../types";
import {
  setGrid,
  selectCell,
  setCellValue,
  startGame,
  updateElapsedTime,
  completeGame,
  incrementMistakes,
  startSolver,
  stopSolver,
  setSolverHintCells,
  incrementSolverFilledCells,
} from "../slices/gameSlice";
import {
  incrementGamesPlayed,
  incrementGamesWon,
  addToTotalTime,
  updateBestTime,
  updateStreak,
  setLastPlayedDate,
} from "../slices/statsSlice";
import { setSolverSpeed, setSolverHelpCells } from "../slices/settingsSlice";
import { Grid } from "../components/Grid";
import { NumberPad } from "../components/NumberPad";
import { Timer } from "../components/Timer";
import { Button } from "../components/Button";
import {
  createPuzzle,
  isPuzzleComplete,
  isValidMoveInGrid,
} from "../utils/sudokuGenerator";
import { findNextStep } from "../utils/sudokuSolver";
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
  const [solverTimeout, setSolverTimeout] = useState<NodeJS.Timeout | null>(
    null,
  );
  const [solverFillingCell, setSolverFillingCell] = useState<{
    row: number;
    col: number;
  } | null>(null);
  const [solverReasoning, setSolverReasoning] = useState<string>("");
  // Solver steps tracking all solving actions
  const [solverSteps, setSolverSteps] = useState<
    Array<{ cell: string; reasoning: string; step: number }>
  >([]);
  const [reasoningExpanded, setReasoningExpanded] = useState<boolean>(true);
  const [lastReasoning, setLastReasoning] = useState<string>("");
  const [hoveredCellReasoning, setHoveredCellReasoning] = useState<string>("");
  const currentGridRef = useRef<SudokuGrid | null>(null);
  const solverStepCountRef = useRef<number>(0);

  useEffect(() => {
    // Start new game
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

    // Start timer
    const interval = setInterval(() => {
      if (!game.isCompleted) {
        const elapsed = Math.floor((Date.now() - game.startTime) / 1000);
        dispatch(updateElapsedTime(elapsed));
      }
    }, 1000);

    setTimerInterval(interval);

    return () => {
      if (interval) clearInterval(interval);
    };
  }, []); // Empty dependency - only run on mount

  useEffect(() => {
    // Check for completion when grid changes
    if (game.grid.length > 0 && isPuzzleComplete(game.grid)) {
      handleGameComplete();
    }
  }, [game.grid]);

  useEffect(() => {
    // Stop solver if game is completed or solver has filled enough cells
    if (
      game.isCompleted ||
      game.solverFilledCells >= settings.solverHelpCells
    ) {
      if (solverTimeout) {
        clearTimeout(solverTimeout);
        setSolverTimeout(null);
      }
      dispatch(stopSolver());
      setSolverFillingCell(null);
      // Keep the last reasoning visible
      currentGridRef.current = null;
      // Keep solverSteps persistent
    }
  }, [game.isCompleted, game.solverFilledCells, settings.solverHelpCells]);

  // Keyboard support for web
  useEffect(() => {
    if (Platform.OS !== "web") return;

    const handleKeyDown = (e: KeyboardEvent) => {
      // Number keys 1-9
      if (e.key >= "1" && e.key <= "9") {
        handleNumberPress(parseInt(e.key, 10));
      }
      // Delete/Backspace to erase
      else if (e.key === "Backspace" || e.key === "Delete") {
        handleErase();
      }
      // Arrow keys for navigation
      else if (e.key.startsWith("Arrow") && game.selectedCell) {
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
    if (game.isCompleted) return; // Prevent multiple completions

    dispatch(completeGame());
    if (timerInterval) {
      clearInterval(timerInterval);
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
        onExit,
      );
    }, 500);
  };

  const handleCellPress = (row: number, col: number) => {
    if (game.isCompleted) return;

    // Hover reasoning is handled by onMouseEnter/onMouseLeave events
    // This function is mainly for selection now

    dispatch(selectCell({ row, col }));
  };

  const handleNumberPress = (num: number) => {
    if (game.isCompleted || !game.selectedCell) return;

    const { row, col } = game.selectedCell;
    const cell = game.grid[row][col];

    if (cell.isInitial) return; // Can't change initial cells

    // Check if move is valid
    if (num !== 0 && !isValidMoveInGrid(game.grid, row, col, num)) {
      dispatch(incrementMistakes());
      if (game.mistakes + 1 >= game.maxMistakes) {
        showNotification("Game Over", "You made too many mistakes!", onExit);
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
      "Are you sure you want to start a new game? Current progress will be lost.",
      () => {
        if (timerInterval) clearInterval(timerInterval);
        const puzzle = createPuzzle(game.difficulty);
        currentGridRef.current = puzzle; // Reset current grid reference
        setSolverReasoning(""); // Clear reasoning for new game
        setLastReasoning("");
        setHoveredCellReasoning("");
        setSolverSteps([]); // Clear solver steps
        dispatch(setGrid(puzzle));
        dispatch(startGame());
        dispatch(incrementGamesPlayed());

        const interval = setInterval(() => {
          const elapsed = Math.floor((Date.now() - Date.now()) / 1000);
          dispatch(updateElapsedTime(elapsed));
        }, 1000);
        setTimerInterval(interval);
      },
      undefined,
      "New Game",
    );
  };

  const handleExit = () => {
    showConfirmation(
      "Exit Game",
      "Are you sure you want to exit? Current progress will be lost.",
      () => {
        if (timerInterval) clearInterval(timerInterval);
        if (solverTimeout) clearTimeout(solverTimeout);
        onExit();
      },
      undefined,
      "Exit",
    );
  };

  const handleStartSolver = () => {
    if (game.isSolverActive) {
      // Stop solver
      if (solverTimeout) {
        clearTimeout(solverTimeout);
        setSolverTimeout(null);
      }
      dispatch(stopSolver());
      setSolverFillingCell(null);
      return;
    }

    // Start solver
    solverStepCountRef.current = 0;
    currentGridRef.current = game.grid; // Initialize with current grid
    setSolverReasoning(""); // Clear previous reasoning
    setLastReasoning("");
    setHoveredCellReasoning("");
    setSolverSteps([]); // Clear previous steps
    dispatch(startSolver());
    runSolverStep();
  };

  const runSolverStep = () => {
    // Check if we've already filled enough cells
    if (solverStepCountRef.current >= settings.solverHelpCells) {
      dispatch(stopSolver());
      setSolverFillingCell(null);
      // Keep the last reasoning visible
      solverStepCountRef.current = 0;
      currentGridRef.current = null;
      // Don't clear solverSteps - keep the list persistent
      return;
    }

    const step = findNextStep(currentGridRef.current || game.grid);

    if (!step) {
      dispatch(stopSolver());
      setSolverFillingCell(null);
      // Keep the last reasoning visible
      return;
    }

    // Skip if the target cell is initial (shouldn't happen with updated solver, but safety check)
    if ((currentGridRef.current || game.grid)[step.row][step.col].isInitial) {
      setSolverTimeout(setTimeout(runSolverStep, 100));
      return;
    }

    // Set hint cells and reasoning
    dispatch(setSolverHintCells(step.hintCells || []));
    setSolverFillingCell({ row: step.row, col: step.col });
    setSolverReasoning(step.reasoning);
    setLastReasoning(step.reasoning);

    // Wait for visualization, then fill the cell
    const timeout = setTimeout(() => {
      // Create a modified grid with the solver-filled flag
      const newGrid = (currentGridRef.current || game.grid).map((row, r) =>
        row.map((cell, c) => {
          if (r === step.row && c === step.col) {
            return { ...cell, value: step.value, isSolverFilled: true };
          }
          return cell;
        }),
      );

      // Update the current grid reference
      currentGridRef.current = newGrid;

      // Store reasoning for this cell
      const cellKey = `${step.row}-${step.col}`;
      setSolverSteps((prev) => [
        ...prev,
        {
          cell: cellKey,
          reasoning: step.reasoning,
          step: solverStepCountRef.current + 1,
        },
      ]);

      // Update the grid with the solver-filled cell
      dispatch(setGrid(newGrid));
      dispatch(incrementSolverFilledCells());
      solverStepCountRef.current += 1;
      dispatch(setSolverHintCells([]));
      setSolverFillingCell(null);
      setSolverReasoning("");

      // Continue to next step
      setSolverTimeout(setTimeout(runSolverStep, settings.solverSpeed));
    }, settings.solverSpeed);

    setSolverTimeout(timeout);
  };

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
            solverHintCells={game.solverHintCells}
            solverFillingCell={solverFillingCell}
            solverSteps={solverSteps}
            onCellHover={setHoveredCellReasoning}
            isSolverActive={game.isSolverActive}
          />
        </View>

        <View style={styles.controls}>
          <NumberPad
            onNumberPress={handleNumberPress}
            onErase={handleErase}
            disabled={game.isCompleted}
          />
        </View>

        <View style={styles.solverControls}>
          <Text style={styles.solverControlsTitle}>Solver Settings</Text>
          <View style={styles.solverSettingsRow}>
            <View style={styles.solverSpeedContainer}>
              <Text style={styles.solverSpeedLabel}>Speed:</Text>
              <View style={styles.solverSpeedButtons}>
                <TouchableOpacity
                  style={[
                    styles.solverSpeedButton,
                    settings.solverSpeed === 500 &&
                      styles.solverSpeedButtonActive,
                  ]}
                  onPress={() => dispatch(setSolverSpeed(500))}
                >
                  <Text
                    style={[
                      styles.solverSpeedButtonText,
                      settings.solverSpeed === 500 &&
                        styles.solverSpeedButtonTextActive,
                    ]}
                  >
                    0.5s
                  </Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[
                    styles.solverSpeedButton,
                    settings.solverSpeed === 1000 &&
                      styles.solverSpeedButtonActive,
                  ]}
                  onPress={() => dispatch(setSolverSpeed(1000))}
                >
                  <Text
                    style={[
                      styles.solverSpeedButtonText,
                      settings.solverSpeed === 1000 &&
                        styles.solverSpeedButtonTextActive,
                    ]}
                  >
                    1s
                  </Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[
                    styles.solverSpeedButton,
                    settings.solverSpeed === 2000 &&
                      styles.solverSpeedButtonActive,
                  ]}
                  onPress={() => dispatch(setSolverSpeed(2000))}
                >
                  <Text
                    style={[
                      styles.solverSpeedButtonText,
                      settings.solverSpeed === 2000 &&
                        styles.solverSpeedButtonTextActive,
                    ]}
                  >
                    2s
                  </Text>
                </TouchableOpacity>
              </View>
            </View>
            <View style={styles.solverCellsContainer}>
              <Text style={styles.solverCellsLabel}>Cells:</Text>
              <View style={styles.solverCellsButtons}>
                <TouchableOpacity
                  style={[
                    styles.solverCellsButton,
                    settings.solverHelpCells === 1 &&
                      styles.solverCellsButtonActive,
                  ]}
                  onPress={() => dispatch(setSolverHelpCells(1))}
                >
                  <Text
                    style={[
                      styles.solverCellsButtonText,
                      settings.solverHelpCells === 1 &&
                        styles.solverCellsButtonTextActive,
                    ]}
                  >
                    1
                  </Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[
                    styles.solverCellsButton,
                    settings.solverHelpCells === 3 &&
                      styles.solverCellsButtonActive,
                  ]}
                  onPress={() => dispatch(setSolverHelpCells(3))}
                >
                  <Text
                    style={[
                      styles.solverCellsButtonText,
                      settings.solverHelpCells === 3 &&
                        styles.solverCellsButtonTextActive,
                    ]}
                  >
                    3
                  </Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[
                    styles.solverCellsButton,
                    settings.solverHelpCells === 5 &&
                      styles.solverCellsButtonActive,
                  ]}
                  onPress={() => dispatch(setSolverHelpCells(5))}
                >
                  <Text
                    style={[
                      styles.solverCellsButtonText,
                      settings.solverHelpCells === 5 &&
                        styles.solverCellsButtonTextActive,
                    ]}
                  >
                    5
                  </Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[
                    styles.solverCellsButton,
                    settings.solverHelpCells === 9 &&
                      styles.solverCellsButtonActive,
                  ]}
                  onPress={() => dispatch(setSolverHelpCells(9))}
                >
                  <Text
                    style={[
                      styles.solverCellsButtonText,
                      settings.solverHelpCells === 9 &&
                        styles.solverCellsButtonTextActive,
                    ]}
                  >
                    9
                  </Text>
                </TouchableOpacity>
              </View>
            </View>
          </View>
          <TouchableOpacity
            style={styles.reasoningToggle}
            onPress={() => setReasoningExpanded(!reasoningExpanded)}
          >
            <Text style={styles.reasoningToggleText}>
              {reasoningExpanded ? "▼" : "▶"} Steps and Reasoning{" "}
              {solverSteps.length > 0 && `(${solverSteps.length})`}
            </Text>
          </TouchableOpacity>
          {reasoningExpanded && (
            <View style={styles.expandedReasoningContainer}>
              {(solverReasoning || lastReasoning) && (
                <View style={styles.solverReasoning}>
                  <Text style={styles.solverReasoningText}>
                    {solverReasoning || lastReasoning}
                  </Text>
                </View>
              )}
              {solverSteps.length > 0 && (
                <View style={styles.solverStepsList}>
                  {solverSteps.map((step, index) => (
                    <View key={index} style={styles.solverStepItem}>
                      <Text style={styles.solverStepText}>
                        Step {step.step}: Cell (
                        {parseInt(step.cell.split("-")[0]) + 1},
                        {parseInt(step.cell.split("-")[1]) + 1}) -{" "}
                        {step.reasoning}
                      </Text>
                    </View>
                  ))}
                </View>
              )}
            </View>
          )}
          {hoveredCellReasoning ? (
            <View style={styles.hoveredCellReasoning}>
              <Text style={styles.hoveredCellReasoningText}>
                {hoveredCellReasoning}
              </Text>
            </View>
          ) : null}
        </View>

        <View style={styles.actions}>
          <Button
            title={game.isSolverActive ? "Stop Solver" : "Start Solver"}
            onPress={handleStartSolver}
            variant={game.isSolverActive ? "danger" : "primary"}
            style={styles.actionButton}
            disabled={game.isCompleted}
          />
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
  solverControls: {
    marginBottom: 20,
    padding: 16,
    backgroundColor: "#F8F9FA",
    borderRadius: 12,
    marginHorizontal: 24,
  },
  solverControlsTitle: {
    fontSize: 16,
    fontWeight: "600",
    color: "#2C3E50",
    marginBottom: 12,
    textAlign: "center",
  },
  solverSettingsRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 12,
  },
  solverSpeedContainer: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 8,
  },
  solverSpeedLabel: {
    fontSize: 14,
    color: "#7F8C8D",
    marginRight: 8,
  },
  solverSpeedButtons: {
    flexDirection: "row",
    gap: 8,
  },
  solverSpeedButton: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
    backgroundColor: "#FFFFFF",
    borderWidth: 1,
    borderColor: "#D1D1D6",
    minWidth: 40,
    alignItems: "center",
  },
  solverSpeedButtonActive: {
    backgroundColor: "#4A90E2",
    borderColor: "#4A90E2",
  },
  solverSpeedButtonText: {
    fontSize: 12,
    fontWeight: "600",
    color: "#2C3E50",
  },
  solverSpeedButtonTextActive: {
    color: "#FFFFFF",
  },
  solverCellsContainer: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
  },
  solverCellsLabel: {
    fontSize: 14,
    color: "#7F8C8D",
    marginRight: 8,
  },
  solverCellsButtons: {
    flexDirection: "row",
    gap: 4,
  },
  solverCellsButton: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 4,
    backgroundColor: "#FFFFFF",
    borderWidth: 1,
    borderColor: "#D1D1D6",
    minWidth: 32,
    alignItems: "center",
  },
  solverCellsButtonActive: {
    backgroundColor: "#4A90E2",
    borderColor: "#4A90E2",
  },
  solverCellsButtonText: {
    fontSize: 12,
    fontWeight: "600",
    color: "#2C3E50",
  },
  solverCellsButtonTextActive: {
    color: "#FFFFFF",
  },
  reasoningToggle: {
    paddingVertical: 8,
    paddingHorizontal: 12,
    backgroundColor: "#FFFFFF",
    borderRadius: 6,
    marginBottom: 8,
    alignSelf: "center",
    borderWidth: 1,
    borderColor: "#D1D1D6",
  },
  reasoningToggleText: {
    fontSize: 14,
    fontWeight: "600",
    color: "#4A90E2",
  },
  expandedReasoningContainer: {
    marginTop: 8,
  },
  solverReasoning: {
    backgroundColor: "#FFF3CD",
    padding: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "#FFEAA7",
    marginTop: 8,
  },
  solverReasoningText: {
    fontSize: 14,
    color: "#856404",
    textAlign: "center",
    lineHeight: 20,
  },
  selectedCellReasoning: {
    backgroundColor: "#D4EDDA",
    padding: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "#C3E6CB",
    marginTop: 8,
  },
  hoveredCellReasoning: {
    backgroundColor: "#D1ECF1",
    padding: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "#BEE5EB",
    marginTop: 8,
  },
  hoveredCellReasoningText: {
    fontSize: 14,
    color: "#0C5460",
    textAlign: "center",
    lineHeight: 20,
  },
  solverStepsList: {
    marginTop: 16,
    padding: 12,
    backgroundColor: "#F8F9FA",
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "#DEE2E6",
  },
  solverStepsTitle: {
    fontSize: 16,
    fontWeight: "600",
    color: "#2C3E50",
    marginBottom: 8,
    textAlign: "center",
  },
  solverStepItem: {
    paddingVertical: 4,
    paddingHorizontal: 8,
    backgroundColor: "#FFFFFF",
    borderRadius: 4,
    marginBottom: 4,
    borderWidth: 1,
    borderColor: "#E9ECEF",
  },
  solverStepText: {
    fontSize: 12,
    color: "#495057",
    lineHeight: 16,
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
