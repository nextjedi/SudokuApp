import { useCallback, useState, useTransition } from "react";
import { handleUserError, createErrorContext } from "../utils/errorUtils";

/**
 * Form action state
 */
export interface FormActionState<T = unknown> {
  data: T | null;
  error: string | null;
  isPending: boolean;
}

/**
 * Form action hook for handling form submissions with optimistic updates
 * @param actionFn - The action function to execute
 * @param onSuccess - Callback for successful action
 * @param onError - Callback for error handling
 * @returns Object with form action state and submit function
 */
export const useFormAction = <TData = unknown, TPayload = unknown>(
  actionFn: (payload: TPayload) => Promise<TData>,
  onSuccess?: (data: TData) => void,
  onError?: (error: Error) => void,
) => {
  const [state, setState] = useState<FormActionState<TData>>({
    data: null,
    error: null,
    isPending: false,
  });

  const [isPending, startTransition] = useTransition();

  const executeAction = useCallback(
    async (payload: TPayload): Promise<TData | null> => {
      setState((prev) => ({ ...prev, isPending: true, error: null }));

      try {
        const result = await actionFn(payload);

        setState({
          data: result,
          error: null,
          isPending: false,
        });

        onSuccess?.(result);
        return result;
      } catch (error) {
        const errorMessage =
          error instanceof Error ? error.message : "An error occurred";

        setState({
          data: null,
          error: errorMessage,
          isPending: false,
        });

        onError?.(error as Error);
        return null;
      }
    },
    [actionFn, onSuccess, onError],
  );

  const submit = useCallback(
    (payload: TPayload) => {
      startTransition(() => {
        executeAction(payload);
      });
    },
    [executeAction],
  );

  return {
    ...state,
    isPending: isPending || state.isPending,
    submit,
  };
};

/**
 * Specific form actions for the app
 */
export const formActions = {
  /**
   * Start new game action
   */
  startNewGame: async (_difficulty: "easy" | "medium" | "hard") => {
    // Simulate API call
    await new Promise((resolve) => setTimeout(resolve, 500));

    if (Math.random() > 0.95) {
      // 5% chance of error for testing
      throw new Error("Failed to generate puzzle");
    }

    return { difficulty: _difficulty, puzzleId: Math.random().toString(36) };
  },

  /**
   * Save game progress action
   */
  saveGameProgress: async (_gameData: unknown) => {
    await new Promise((resolve) => setTimeout(resolve, 300));

    if (Math.random() > 0.98) {
      // 2% chance of error
      throw new Error("Failed to save progress");
    }

    return { saved: true, timestamp: Date.now() };
  },

  /**
   * Update settings action
   */
  updateSettings: async (_settings: Record<string, unknown>) => {
    await new Promise((resolve) => setTimeout(resolve, 200));

    if (Math.random() > 0.99) {
      // 1% chance of error
      throw new Error("Failed to update settings");
    }

    return { updated: true, settings: _settings };
  },
};

/**
 * Hook for specific form actions
 */
export const useGameActions = () => {
  const newGameAction = useFormAction(
    formActions.startNewGame,
    (data) => {
      console.log("New game started:", data);
    },
    () => {
      handleUserError("GENERAL_ERROR", {
        context: createErrorContext("GameActions", "startNewGame"),
      });
    },
  );

  const saveProgressAction = useFormAction(
    formActions.saveGameProgress,
    () => {
      console.log("Progress saved");
    },
    () => {
      handleUserError("SAVE_ERROR", {
        context: createErrorContext("GameActions", "saveProgress"),
      });
    },
  );

  const updateSettingsAction = useFormAction(
    formActions.updateSettings,
    () => {
      console.log("Settings updated");
    },
    () => {
      handleUserError("GENERAL_ERROR", {
        context: createErrorContext("GameActions", "updateSettings"),
      });
    },
  );

  return {
    newGameAction,
    saveProgressAction,
    updateSettingsAction,
  };
};
