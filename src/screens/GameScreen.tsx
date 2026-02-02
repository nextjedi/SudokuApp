import React, { useState, useCallback } from "react";
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  ScrollView,
  TouchableOpacity,
} from "react-native";
import { useDispatch, useSelector } from "react-redux";
import { RootState } from "../store/store";
import { setSolverSpeed, setSolverHelpCells } from "../slices/settingsSlice";
import { Grid } from "../components/Grid";
import { NumberPad } from "../components/NumberPad";
import { Timer } from "../components/Timer";
import { Button } from "../components/Button";
import { ScreenErrorBoundary } from "../components/ScreenErrorBoundary";
import { useGameLogic, useSolverLogic, useKeyboardSupport } from "../hooks";
import { handleUserError, createErrorContext } from "../utils/errorUtils";

/**
 * Props for the GameScreen component
 */
interface GameScreenProps {
  /** Callback function called when the user wants to exit the game */
  onExit: () => void;
}

/**
 * Main game screen component that handles Sudoku gameplay
 *
 * Features:
 * - Real-time Sudoku grid interaction
 * - Timer functionality
 * - Mistake tracking
 * - Solver with step-by-step visualization
 * - Game completion detection
 * - Error boundaries for crash protection
 *
 * @param props - Component props
 * @returns JSX.Element - The game screen UI
 */
const GameScreenContent: React.FC<GameScreenProps> = ({ onExit }) => {
  const dispatch = useDispatch();
  const game = useSelector((state: RootState) => state.game);
  const settings = useSelector((state: RootState) => state.settings);
  const [reasoningExpanded, setReasoningExpanded] = useState<boolean>(true);
  const [hoveredCellReasoning, setHoveredCellReasoning] = useState<string>("");

  const {
    handleCellPress,
    handleNumberPress,
    handleErase,
    handleNewGame: baseHandleNewGame,
    handleExit: gameHandleExit,
  } = useGameLogic();
  const {
    solverTimeout,
    solverFillingCell,
    solverReasoning,
    solverSteps,
    lastReasoning,
    handleStartSolver,
    setSolverTimeout,
    resetSolverStates,
  } = useSolverLogic();

  const handleNewGame = useCallback(() => {
    try {
      baseHandleNewGame(resetSolverStates);
    } catch {
      handleUserError("GENERAL_ERROR", {
        context: createErrorContext("GameScreen", "handleNewGame"),
      });
    }
  }, [baseHandleNewGame, resetSolverStates]);

  useKeyboardSupport({
    onNumberPress: handleNumberPress,
    onErase: handleErase,
  });

  const handleExit = useCallback(() => {
    try {
      if (solverTimeout) {
        clearTimeout(solverTimeout);
        setSolverTimeout(null);
      }
      gameHandleExit();
      onExit();
    } catch {
      handleUserError("GENERAL_ERROR", {
        context: createErrorContext("GameScreen", "handleExit"),
      });
    }
  }, [solverTimeout, setSolverTimeout, gameHandleExit, onExit]);

  return (
    <ScreenErrorBoundary screenName="GameScreen" onRetry={handleNewGame}>
      <SafeAreaView style={styles.container}>
        <ScrollView
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}
          showsHorizontalScrollIndicator={false}
          bounces={false}
        >
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
    </ScreenErrorBoundary>
  );
};

export const GameScreen: React.FC<GameScreenProps> = (props) => (
  <GameScreenContent {...props} />
);

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
