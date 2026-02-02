import { describe, it, expect, jest, beforeEach } from "@jest/globals";
import {
  handleUserError,
  handleAsyncError,
  validateAndShowError,
  safeAsync,
  createErrorContext,
} from "../errorUtils";
import { ERROR_MESSAGES } from "../../constants/errorMessages";

// Mock the alert helper
jest.mock("../alertHelper", () => ({
  showNotification: jest.fn(),
}));

import { showNotification } from "../alertHelper";

describe("errorUtils", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("handleUserError", () => {
    it("should show notification for valid error key", () => {
      handleUserError("INVALID_MOVE");

      expect(showNotification).toHaveBeenCalledWith(
        ERROR_MESSAGES.INVALID_MOVE.title,
        ERROR_MESSAGES.INVALID_MOVE.message,
        undefined,
        undefined,
        ERROR_MESSAGES.INVALID_MOVE.button,
      );
    });

    it("should handle error with context", () => {
      const context = { screen: "GameScreen" };
      handleUserError("INVALID_MOVE", { context });

      expect(showNotification).toHaveBeenCalledWith(
        ERROR_MESSAGES.INVALID_MOVE.title,
        ERROR_MESSAGES.INVALID_MOVE.message,
        undefined,
        undefined,
        ERROR_MESSAGES.INVALID_MOVE.button,
      );
    });
  });

  describe("handleAsyncError", () => {
    it("should handle Error objects", () => {
      const error = new Error("Test error");
      handleAsyncError(error);

      expect(showNotification).toHaveBeenCalledWith(
        ERROR_MESSAGES.GENERAL_ERROR.title,
        ERROR_MESSAGES.GENERAL_ERROR.message,
        undefined,
        undefined,
        ERROR_MESSAGES.GENERAL_ERROR.button,
      );
    });

    it("should handle string errors", () => {
      handleAsyncError("String error" as unknown as Error);

      expect(showNotification).toHaveBeenCalledWith(
        ERROR_MESSAGES.GENERAL_ERROR.title,
        ERROR_MESSAGES.GENERAL_ERROR.message,
        undefined,
        undefined,
        ERROR_MESSAGES.GENERAL_ERROR.button,
      );
    });
  });

  describe("validateAndShowError", () => {
    it("should return true for valid conditions", () => {
      const result = validateAndShowError(true, "INVALID_MOVE");
      expect(result).toBe(true);
      expect(showNotification).not.toHaveBeenCalled();
    });

    it("should return false and show error for invalid conditions", () => {
      const result = validateAndShowError(false, "INVALID_MOVE");
      expect(result).toBe(false);
      expect(showNotification).toHaveBeenCalledWith(
        ERROR_MESSAGES.INVALID_MOVE.title,
        ERROR_MESSAGES.INVALID_MOVE.message,
        undefined,
        undefined,
        ERROR_MESSAGES.INVALID_MOVE.button,
      );
    });
  });

  describe("safeAsync", () => {
    it("should return result for successful operations", async () => {
      const operation = async () => "success";
      const result = await safeAsync(operation);

      expect(result).toBe("success");
      expect(showNotification).not.toHaveBeenCalled();
    });

    it("should return null and handle errors", async () => {
      const operation = async () => {
        throw new Error("Test error") as Error;
      };
      const result = await safeAsync(operation);

      expect(result).toBe(null);
      expect(showNotification).toHaveBeenCalledWith(
        ERROR_MESSAGES.GENERAL_ERROR.title,
        ERROR_MESSAGES.GENERAL_ERROR.message,
        undefined,
        undefined,
        ERROR_MESSAGES.GENERAL_ERROR.button,
      );
    });
  });

  describe("createErrorContext", () => {
    it("should create error context with required fields", () => {
      const context = createErrorContext("TestScreen", "testFeature");

      expect(context.screen).toBe("TestScreen");
      expect(context.feature).toBe("testFeature");
      expect(typeof context.timestamp).toBe("number");
      expect(typeof context.userAgent).toBe("string");
    });

    it("should handle undefined feature", () => {
      const context = createErrorContext("TestScreen");

      expect(context.screen).toBe("TestScreen");
      expect(context.feature).toBeUndefined();
    });
  });
});
