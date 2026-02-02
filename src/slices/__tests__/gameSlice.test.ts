import { describe, it, expect } from "@jest/globals";
import gameReducer, {
  setGrid,
  selectCell,
  setCellValue,
  setDifficulty,
  startGame,
  updateElapsedTime,
  incrementMistakes,
  completeGame,
  resetGame,
} from "../gameSlice";
import { GameState } from "../../types";

describe("gameSlice", () => {
  const initialState: GameState = {
    grid: [],
    selectedCell: null,
    difficulty: "easy",
    isCompleted: false,
    startTime: 0,
    elapsedTime: 0,
    mistakes: 0,
    maxMistakes: 3,
    isSolverActive: false,
    solverHintCells: [],
    solverFilledCells: 0,
  };

  it("should return the initial state", () => {
    expect(gameReducer(initialState, { type: "unknown" })).toEqual(
      initialState,
    );
  });

  describe("setGrid", () => {
    it("should set the grid", () => {
      const mockGrid = [
        [
          {
            value: 1,
            isInitial: true,
            isSelected: false,
            notes: [],
            isSolverFilled: false,
          },
        ],
      ];
      const action = setGrid(mockGrid);
      const result = gameReducer(initialState, action);

      expect(result.grid).toEqual(mockGrid);
    });
  });

  describe("selectCell", () => {
    it("should set the selected cell", () => {
      const action = selectCell({ row: 1, col: 2 });
      const result = gameReducer(initialState, action);

      expect(result.selectedCell).toEqual({ row: 1, col: 2 });
    });
  });

  describe("setCellValue", () => {
    it("should set cell value for non-initial cells", () => {
      const mockGrid = [
        [
          {
            value: 0,
            isInitial: false,
            isSelected: false,
            notes: [],
            isSolverFilled: false,
          },
        ],
      ];
      const stateWithGrid = { ...initialState, grid: mockGrid };

      const action = setCellValue({ row: 0, col: 0, value: 5 });
      const result = gameReducer(stateWithGrid, action);

      expect(result.grid[0][0].value).toBe(5);
    });

    it("should not set cell value for initial cells", () => {
      const mockGrid = [
        [
          {
            value: 1,
            isInitial: true,
            isSelected: false,
            notes: [],
            isSolverFilled: false,
          },
        ],
      ];
      const stateWithGrid = { ...initialState, grid: mockGrid };

      const action = setCellValue({ row: 0, col: 0, value: 5 });
      const result = gameReducer(stateWithGrid, action);

      expect(result.grid[0][0].value).toBe(1);
    });
  });

  describe("setDifficulty", () => {
    it("should set the difficulty", () => {
      const action = setDifficulty("hard");
      const result = gameReducer(initialState, action);

      expect(result.difficulty).toBe("hard");
    });
  });

  describe("startGame", () => {
    it("should start the game with current timestamp", () => {
      const beforeTime = Date.now();
      const action = startGame();
      const result = gameReducer(initialState, action);
      const afterTime = Date.now();

      expect(result.startTime).toBeGreaterThanOrEqual(beforeTime);
      expect(result.startTime).toBeLessThanOrEqual(afterTime);
      expect(result.isCompleted).toBe(false);
      expect(result.mistakes).toBe(0);
      expect(result.elapsedTime).toBe(0);
    });
  });

  describe("updateElapsedTime", () => {
    it("should update elapsed time", () => {
      const action = updateElapsedTime(120);
      const result = gameReducer(initialState, action);

      expect(result.elapsedTime).toBe(120);
    });
  });

  describe("incrementMistakes", () => {
    it("should increment mistakes", () => {
      const action = incrementMistakes();
      const result = gameReducer(initialState, action);

      expect(result.mistakes).toBe(1);
    });
  });

  describe("completeGame", () => {
    it("should mark game as completed", () => {
      const action = completeGame();
      const result = gameReducer(initialState, action);

      expect(result.isCompleted).toBe(true);
    });
  });

  describe("resetGame", () => {
    it("should reset to initial state", () => {
      const modifiedState = {
        ...initialState,
        difficulty: "hard" as const,
        mistakes: 2,
        isCompleted: true,
      };

      const action = resetGame();
      const result = gameReducer(modifiedState, action);

      expect(result).toEqual(initialState);
    });
  });
});
