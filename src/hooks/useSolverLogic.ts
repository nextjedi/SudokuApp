import { useState, useRef, useCallback } from "react";
import { useDispatch, useSelector } from "react-redux";
import { RootState } from "../store/store";
import { SudokuGrid, Position, SolverStepDisplay } from "../types";
import {
  startSolver,
  stopSolver,
  setSolverHintCells,
  incrementSolverFilledCells,
  setGrid,
} from "../slices/gameSlice";
import { findNextStep } from "../utils/sudokuSolver";

export const useSolverLogic = () => {
  const dispatch = useDispatch();
  const game = useSelector((state: RootState) => state.game);
  const settings = useSelector((state: RootState) => state.settings);
  const [solverTimeout, setSolverTimeout] = useState<NodeJS.Timeout | null>(
    null,
  );
  const [solverFillingCell, setSolverFillingCell] = useState<Position | null>(
    null,
  );
  const [solverReasoning, setSolverReasoning] = useState<string>("");
  const [solverSteps, setSolverSteps] = useState<SolverStepDisplay[]>([]);
  const [lastReasoning, setLastReasoning] = useState<string>("");
  const currentGridRef = useRef<SudokuGrid | null>(null);
  const solverStepCountRef = useRef<number>(0);

  const runSolverStep = useCallback(() => {
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
    const targetGrid = currentGridRef.current || game.grid;
    if (targetGrid[step.row]?.[step.col]?.isInitial) {
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
      const newGrid = targetGrid.map((row, r) =>
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
  }, [settings.solverHelpCells, settings.solverSpeed, game.grid, dispatch]);

  const handleStartSolver = useCallback(() => {
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
    setSolverSteps([]); // Clear previous steps
    dispatch(startSolver());
    runSolverStep();
  }, [game.isSolverActive, game.grid, solverTimeout, dispatch, runSolverStep]);

  const resetSolverStates = useCallback(() => {
    setSolverReasoning("");
    setLastReasoning("");
    setSolverSteps([]);
  }, []);

  return {
    solverTimeout,
    solverFillingCell,
    solverReasoning,
    solverSteps,
    lastReasoning,
    handleStartSolver,
    setSolverTimeout,
    resetSolverStates,
  };
};
