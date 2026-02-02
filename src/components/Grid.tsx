import React, { useCallback } from "react";
import { View, StyleSheet, Platform } from "react-native";
import { SudokuGrid as GridType } from "../types";
import { Cell } from "./Cell";

interface GridProps {
  grid: GridType;
  selectedCell: { row: number; col: number } | null;
  onCellPress: (row: number, col: number) => void;
  highlightEnabled: boolean;
  solverHintCells?: { row: number; col: number }[];
  solverFillingCell?: { row: number; col: number } | null;
  solverSteps?: Array<{ cell: string; reasoning: string; step: number }>;
  onCellHover?: (reasoning: string) => void;
  isSolverActive?: boolean;
}

export const Grid = React.memo<GridProps>(
  ({
    grid,
    selectedCell,
    onCellPress,
    highlightEnabled,
    solverHintCells = [],
    solverFillingCell = null,
    solverSteps = [],
    onCellHover,
    isSolverActive = false,
  }) => {
    const isHighlighted = useCallback(
      (row: number, col: number): boolean => {
        if (!highlightEnabled || !selectedCell) return false;

        // Highlight same row or column
        if (row === selectedCell.row || col === selectedCell.col) return true;

        // Highlight same 3x3 box
        const boxRow = Math.floor(row / 3);
        const boxCol = Math.floor(col / 3);
        const selectedBoxRow = Math.floor(selectedCell.row / 3);
        const selectedBoxCol = Math.floor(selectedCell.col / 3);
        if (boxRow === selectedBoxRow && boxCol === selectedBoxCol) return true;

        // Highlight cells with same non-zero value
        if (
          grid[row][col].value !== 0 &&
          grid[selectedCell.row][selectedCell.col].value !== 0 &&
          grid[row][col].value ===
            grid[selectedCell.row][selectedCell.col].value
        ) {
          return true;
        }

        return false;
      },
      [highlightEnabled, selectedCell, grid],
    );

    const isSolverHint = useCallback(
      (row: number, col: number): boolean => {
        return solverHintCells.some(
          (cell) => cell.row === row && cell.col === col,
        );
      },
      [solverHintCells],
    );

    const isSolverFilling = useCallback(
      (row: number, col: number): boolean => {
        return (
          solverFillingCell !== null &&
          solverFillingCell.row === row &&
          solverFillingCell.col === col
        );
      },
      [solverFillingCell],
    );

    const getCellReasoning = useCallback(
      (row: number, col: number): string => {
        const cellKey = `${row}-${col}`;
        const step = solverSteps.find((s) => s.cell === cellKey);
        return step ? step.reasoning : "";
      },
      [solverSteps],
    );

    if (grid.length === 0) {
      return null;
    }

    return (
      <View style={styles.container}>
        <View style={[styles.grid, isSolverActive && styles.gridSolverActive]}>
          {grid.map((row, rowIndex) => (
            <View key={rowIndex} style={styles.row}>
              {row.map((cell, colIndex) => (
                <Cell
                  key={`${rowIndex}-${colIndex}`}
                  cell={cell}
                  row={rowIndex}
                  col={colIndex}
                  isSelected={
                    selectedCell !== null &&
                    selectedCell.row === rowIndex &&
                    selectedCell.col === colIndex
                  }
                  isHighlighted={isHighlighted(rowIndex, colIndex)}
                  isSolverHint={isSolverHint(rowIndex, colIndex)}
                  isSolverFilling={isSolverFilling(rowIndex, colIndex)}
                  reasoning={getCellReasoning(rowIndex, colIndex)}
                  onPress={onCellPress}
                  onHover={onCellHover}
                />
              ))}
            </View>
          ))}
        </View>
      </View>
    );
  },
);

Grid.displayName = "Grid";

const styles = StyleSheet.create({
  container: {
    alignItems: "center",
    justifyContent: "center",
  },
  grid: {
    backgroundColor: "#000000",
    borderWidth: 3,
    borderColor: "#000000",
  },
  gridSolverActive: {
    shadowColor: "#4A90E2",
    shadowOffset: {
      width: 0,
      height: 0,
    },
    shadowOpacity: 0.8,
    shadowRadius: 20,
    elevation: 10,
    borderColor: "#4A90E2",
    borderWidth: 3,
    ...Platform.select({
      web: {
        animation: "pulse 2s infinite",
        "@keyframes pulse": {
          "0%": {
            shadowOpacity: 0.8,
            transform: "scale(1)",
          },
          "50%": {
            shadowOpacity: 1,
            transform: "scale(1.02)",
          },
          "100%": {
            shadowOpacity: 0.8,
            transform: "scale(1)",
          },
        },
      },
    }),
  },
  row: {
    flexDirection: "row",
  },
});
