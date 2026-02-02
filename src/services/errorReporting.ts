import { ErrorInfo } from "react";

export interface ErrorContext {
  [key: string]: string | number | boolean | null | undefined;
}

export interface ErrorReport {
  error: Error;
  errorInfo?: ErrorInfo;
  context?: ErrorContext;
  timestamp: number;
  userId?: string | null;
  version: string;
  platform: string;
  userAgent?: string | null;
}

export interface IErrorReportingService {
  reportError(
    error: Error,
    errorInfo?: ErrorInfo,
    context?: ErrorContext,
  ): void;
  reportUserError(message: string, context?: ErrorContext): void;
  setUserContext(userId: string): void;
  setGlobalContext(context: ErrorContext): void;
}

class ErrorReportingService implements IErrorReportingService {
  private userId?: string;
  private globalContext: ErrorContext = {};
  private isInitialized = false;

  constructor() {
    this.initialize();
  }

  private initialize(): void {
    if (this.isInitialized) return;

    // Get app version
    this.globalContext.version = "1.0.0"; // Should come from package.json or build config

    // Get platform info
    this.globalContext.platform = "web"; // Could be 'ios', 'android', 'web'

    // Get user agent for web
    if (typeof window !== "undefined") {
      this.globalContext.userAgent = window.navigator.userAgent;
    }

    this.isInitialized = true;
  }

  setUserContext(userId: string): void {
    this.userId = userId;
  }

  setGlobalContext(context: ErrorContext): void {
    this.globalContext = { ...this.globalContext, ...context };
  }

  reportError(
    error: Error,
    errorInfo?: ErrorInfo,
    context: ErrorContext = {},
  ): void {
    // Convert context values to strings for consistent reporting
    let stringifiedContext: Record<string, string> | undefined = undefined;
    const mergedContext = { ...this.globalContext, ...context };
    const hasContext = Object.keys(mergedContext).length > 0;

    if (hasContext) {
      const contextObj: Record<string, string> = {};
      for (const [key, value] of Object.entries(mergedContext)) {
        if (value !== null && value !== undefined) {
          contextObj[key] = String(value);
        }
      }
      stringifiedContext = contextObj;
    }

    const errorReport: ErrorReport = {
      error,
      errorInfo,
      context: stringifiedContext,
      timestamp: Date.now(),
      userId: this.userId || null,
      version: (this.globalContext.version as string) || "unknown",
      platform: (this.globalContext.platform as string) || "unknown",
      userAgent:
        typeof this.globalContext.userAgent === "string"
          ? this.globalContext.userAgent
          : null,
    };

    // Log to console in development
    if (__DEV__) {
      console.error("Error Report:", errorReport);
    }

    // Send to error tracking service
    this.sendToService(errorReport);
  }

  reportUserError(message: string, context: ErrorContext = {}): void {
    const error = new Error(message);
    this.reportError(error, undefined, { ...context, type: "user_error" });
  }

  private async sendToService(errorReport: ErrorReport): Promise<void> {
    try {
      // In production, this would send to your error tracking service
      // For now, we'll just log it

      // Example: Send to Sentry, LogRocket, or custom backend
      // await fetch('/api/errors', {
      //   method: 'POST',
      //   headers: { 'Content-Type': 'application/json' },
      //   body: JSON.stringify(errorReport),
      // });

      // For demonstration, we'll use a mock implementation
      if (!__DEV__) {
        console.log("Sending error report to service:", errorReport);
      }
    } catch (sendError) {
      // Fallback: if sending fails, log to console
      console.error("Failed to send error report:", sendError);
      console.error("Original error:", errorReport);
    }
  }
}

// Singleton instance
export const errorReporting = new ErrorReportingService();

// Helper functions for common error reporting
export const reportError = (
  error: Error,
  errorInfo?: ErrorInfo,
  context?: ErrorContext,
) => {
  errorReporting.reportError(error, errorInfo, context);
};

export const reportUserError = (message: string, context?: ErrorContext) => {
  errorReporting.reportUserError(message, context);
};

export const setUserContext = (userId: string) => {
  errorReporting.setUserContext(userId);
};

export const setGlobalContext = (context: ErrorContext) => {
  errorReporting.setGlobalContext(context);
};
