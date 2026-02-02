import { ERROR_MESSAGES, ErrorMessageKey } from "../constants/errorMessages";
import { reportUserError, ErrorContext } from "../services/errorReporting";

export type { ErrorMessageKey };

export interface ErrorOptions {
  context?: ErrorContext;
  onConfirm?: () => void;
  onCancel?: () => void;
}

/**
 * Get localized error message
 * In production, this would load from i18n service based on user language
 */
export const getErrorMessage = (
  key: ErrorMessageKey,
): (typeof ERROR_MESSAGES)[ErrorMessageKey] => {
  return ERROR_MESSAGES[key];
};

/**
 * Standardized error handler for user-facing errors
 */
export const handleUserError = (
  key: ErrorMessageKey,
  options: ErrorOptions = {},
): void => {
  const message = getErrorMessage(key);

  // Report to error tracking if it's an error (not just info)
  if (key.includes("ERROR")) {
    reportUserError(`${message.title}: ${message.message}`, options.context);
  }

  // Show alert to user
  import("../utils/alertHelper").then(({ showNotification }) => {
    showNotification(
      message.title,
      message.message,
      options.onConfirm,
      options.onCancel,
      message.button,
    );
  });
};

/**
 * Handle async operation errors
 */
export const handleAsyncError = (
  error: Error,
  fallbackKey: ErrorMessageKey = "GENERAL_ERROR",
  options: ErrorOptions = {},
): void => {
  console.error("Async operation error:", error);

  reportUserError(error.message, {
    ...options.context,
    stack: error.stack,
  });

  handleUserError(fallbackKey, options);
};

/**
 * Validate input and show error if invalid
 */
export const validateAndShowError = (
  condition: boolean,
  errorKey: ErrorMessageKey,
  options: ErrorOptions = {},
): boolean => {
  if (!condition) {
    handleUserError(errorKey, options);
    return false;
  }
  return true;
};

/**
 * Safe async operation wrapper
 */
export const safeAsync = async <T>(
  operation: () => Promise<T>,
  errorKey: ErrorMessageKey = "GENERAL_ERROR",
  options: ErrorOptions = {},
): Promise<T | null> => {
  try {
    return await operation();
  } catch (error) {
    handleAsyncError(error as Error, errorKey, options);
    return null;
  }
};

/**
 * Create error boundary context for specific screens/features
 */
export const createErrorContext = (
  screen: string,
  feature?: string,
): ErrorContext => ({
  screen,
  feature: feature || undefined,
  timestamp: Date.now(),
  userAgent: typeof navigator !== "undefined" ? navigator.userAgent : undefined,
});
