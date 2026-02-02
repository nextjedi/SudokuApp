import { useCallback } from "react";
import { SudokuGrid, SolverStep } from "../types";

/**
 * Optimistic updates for solver operations
 * Provides immediate UI feedback while solver operations are processing
 * This is a fallback implementation that doesn't use React 19's useOptimistic
 */
export const useOptimisticSolver = (
  currentGrid: SudokuGrid,
  onGridUpdate: (newGrid: SudokuGrid) => void,
) => {
  // For now, just return the current grid - optimistic updates can be added later
  // when React 19 useOptimistic is properly supported
  const optimisticGrid = currentGrid;

  /**
   * Apply a solver step (without optimistic updates for now)
   * @param step - The solver step to apply
   * @returns Promise that resolves when the update is applied
   */
  const applyOptimisticStep = useCallback(
    async (step: SolverStep): Promise<void> => {
      // Create grid update
      const newGrid = currentGrid.map((row, r) =>
        row.map((cell, c) => {
          if (r === step.row && c === step.col) {
            return { ...cell, value: step.value, isSolverFilled: true };
          }
          return cell;
        }),
      );

      // Apply update immediately
      onGridUpdate(newGrid);
    },
    [currentGrid, onGridUpdate],
  );

  /**
   * Start solver (without optimistic updates for now)
   * @param solverSteps - Array of solver steps to apply
   * @param delay - Delay between steps in milliseconds
   */
  const startOptimisticSolver = useCallback(
    async (solverSteps: SolverStep[], delay: number = 500): Promise<void> => {
      for (const step of solverSteps) {
        await applyOptimisticStep(step);
        await new Promise((resolve) => setTimeout(resolve, delay));
      }
    },
    [applyOptimisticStep],
  );

  return {
    optimisticGrid,
    applyOptimisticStep,
    startOptimisticSolver,
  };
};
