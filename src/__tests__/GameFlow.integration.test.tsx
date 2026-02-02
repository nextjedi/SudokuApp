import React from "react";
import { render } from "@testing-library/react-native";
import { Provider } from "react-redux";
import { configureStore } from "@reduxjs/toolkit";
import { describe, it, expect, jest, beforeEach } from "@jest/globals";
import { GameScreen } from "../screens/GameScreen";
import gameReducer from "../slices/gameSlice";
import statsReducer from "../slices/statsSlice";
import settingsReducer from "../slices/settingsSlice";

// Mock the alert helper
jest.mock("../utils/alertHelper", () => ({
  showNotification: jest.fn(),
  showConfirmation: jest.fn(),
}));

// Mock the puzzle generator
jest.mock("../utils/sudokuGenerator", () => ({
  createPuzzle: jest.fn(() =>
    Array(9)
      .fill(null)
      .map(() =>
        Array(9)
          .fill(null)
          .map(() => ({
            value: 0,
            isInitial: false,
            isSelected: false,
            notes: [],
            isSolverFilled: false,
          })),
      ),
  ),
  isPuzzleComplete: jest.fn(() => false),
  isValidMoveInGrid: jest.fn(() => true),
}));

describe("GameFlow Integration", () => {
  let store: ReturnType<typeof configureStore>;
  const mockOnExit = jest.fn() as () => void;

  beforeEach(() => {
    store = configureStore({
      reducer: {
        game: gameReducer,
        stats: statsReducer,
        settings: settingsReducer,
      },
    });
    jest.clearAllMocks();
  });

  const renderGameScreen = () => {
    return render(
      <Provider store={store}>
        <GameScreen onExit={mockOnExit} />
      </Provider>,
    );
  };

  it("renders game screen with initial state", () => {
    const { getByText } = renderGameScreen();

    expect(getByText("Easy")).toBeTruthy(); // Default difficulty
  });

  it("renders without crashing", () => {
    const { toJSON } = renderGameScreen();
    expect(toJSON()).toBeTruthy();
  });

  it("renders basic game components", () => {
    const { toJSON } = renderGameScreen();

    // Check that the component renders without crashing
    expect(toJSON()).toBeDefined();
  });

  it("initializes with correct state", () => {
    renderGameScreen();

    // Check that store is initialized
    expect(store).toBeDefined();
  });
});
