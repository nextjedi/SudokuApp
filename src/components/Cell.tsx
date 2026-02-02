import React, { useMemo, useCallback } from "react";
import {
  TouchableOpacity,
  Text,
  StyleSheet,
  ViewStyle,
  Dimensions,
  Platform,
  View,
} from "react-native";
import { SudokuCell as CellType } from "../types";

// Calculate responsive cell size based on screen width
const getCellSize = (): number => {
  if (Platform.OS === "web") {
    // For web, use a reasonable size that works well on most screens
    // Max 50px, min 35px, based on viewport
    const viewportWidth =
      typeof window !== "undefined" ? window.innerWidth : 400;
    return Math.min(50, Math.max(35, Math.floor((viewportWidth - 80) / 9)));
  }
  // For mobile, calculate based on screen width with padding
  const { width } = Dimensions.get("window");
  return Math.floor((width - 48) / 9);
};

const CELL_SIZE = getCellSize();

interface CellProps {
  cell: CellType;
  row: number;
  col: number;
  isSelected: boolean;
  isHighlighted: boolean;
  isSolverHint?: boolean;
  isSolverFilling?: boolean;
  reasoning?: string;
  onPress: (row: number, col: number) => void;
  onHover?: (reasoning: string) => void;
}

export const Cell = React.memo<CellProps>(
  ({
    cell,
    row,
    col,
    isSelected,
    isHighlighted,
    isSolverHint = false,
    isSolverFilling = false,
    reasoning,
    onPress,
    onHover,
  }) => {
    const handlePress = useCallback(() => {
      onPress(row, col);
    }, [row, col, onPress]);

    const handleMouseEnter = useCallback(() => {
      if (reasoning && onHover) {
        onHover(reasoning);
      }
    }, [reasoning, onHover]);

    const handleMouseLeave = useCallback(() => {
      if (onHover) {
        onHover("");
      }
    }, [onHover]);

    const borderStyle = useMemo(
      (): ViewStyle => ({
        borderTopWidth: row % 3 === 0 ? 3 : 1,
        borderLeftWidth: col % 3 === 0 ? 3 : 1,
        borderRightWidth: col === 8 ? 3 : 0,
        borderBottomWidth: row === 8 ? 3 : 0,
      }),
      [row, col],
    );

    const mouseEvents =
      Platform.OS === "web"
        ? {
            onMouseEnter: handleMouseEnter,
            onMouseLeave: handleMouseLeave,
          }
        : {};

    return (
      <View
        style={[
          styles.cell,
          borderStyle,
          isSelected && styles.selectedCell,
          isHighlighted && !isSelected && styles.highlightedCell,
          isSolverHint && styles.solverHintCell,
          isSolverFilling && styles.solverFillingCell,
          cell.isSolverFilled && styles.solverFilledCell,
          cell.isInitial && styles.initialCell,
        ]}
        {...mouseEvents}
      >
        <TouchableOpacity
          style={styles.cellTouchable}
          onPress={handlePress}
          activeOpacity={0.6}
        >
          <Text
            style={[
              styles.cellText,
              cell.isInitial && styles.initialText,
              cell.value === 0 && styles.emptyText,
            ]}
          >
            {cell.value !== 0 ? cell.value : ""}
          </Text>
        </TouchableOpacity>
      </View>
    );
  },
  (prevProps, nextProps) => {
    // Custom comparison for optimal re-rendering
    return (
      prevProps.cell.value === nextProps.cell.value &&
      prevProps.cell.isInitial === nextProps.cell.isInitial &&
      prevProps.cell.isSolverFilled === nextProps.cell.isSolverFilled &&
      prevProps.isSelected === nextProps.isSelected &&
      prevProps.isHighlighted === nextProps.isHighlighted &&
      prevProps.isSolverHint === nextProps.isSolverHint &&
      prevProps.isSolverFilling === nextProps.isSolverFilling &&
      prevProps.reasoning === nextProps.reasoning
    );
  },
);

Cell.displayName = "Cell";

const styles = StyleSheet.create({
  cell: {
    width: CELL_SIZE,
    height: CELL_SIZE,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#FFFFFF",
    borderColor: "#000000",
    ...Platform.select({
      web: {
        cursor: "pointer",
        userSelect: "none",
        transition: "background-color 0.15s ease",
      },
    }),
  },
  cellTouchable: {
    width: "100%",
    height: "100%",
    justifyContent: "center",
    alignItems: "center",
  },
  selectedCell: {
    backgroundColor: "#BBE5F4",
  },
  highlightedCell: {
    backgroundColor: "#E8F4F8",
  },
  solverHintCell: {
    backgroundColor: "#FFF3CD", // Light yellow for hint cells
  },
  solverFillingCell: {
    backgroundColor: "#D4EDDA", // Light green for cell being filled
  },
  solverFilledCell: {
    backgroundColor: "#E8F8F5", // Light teal for cells filled by solver
  },
  initialCell: {
    backgroundColor: "#F5F5F5",
  },
  cellText: {
    fontSize: Math.floor(CELL_SIZE * 0.5),
    fontWeight: "500",
    color: "#4A90E2",
  },
  initialText: {
    color: "#000000",
    fontWeight: "700",
  },
  emptyText: {
    color: "transparent",
  },
});
