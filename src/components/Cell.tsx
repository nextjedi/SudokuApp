import React, { useMemo, useCallback } from "react";
import {
  TouchableOpacity,
  Text,
  StyleSheet,
  ViewStyle,
  Dimensions,
  Platform,
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
  onPress: (row: number, col: number) => void;
}

export const Cell = React.memo<CellProps>(
  ({ cell, row, col, isSelected, isHighlighted, onPress }) => {
    const handlePress = useCallback(() => {
      onPress(row, col);
    }, [row, col, onPress]);

    const borderStyle = useMemo(
      (): ViewStyle => ({
        borderTopWidth: row % 3 === 0 ? 3 : 1,
        borderLeftWidth: col % 3 === 0 ? 3 : 1,
        borderRightWidth: col === 8 ? 3 : 0,
        borderBottomWidth: row === 8 ? 3 : 0,
      }),
      [row, col],
    );

    return (
      <TouchableOpacity
        style={[
          styles.cell,
          borderStyle,
          isSelected && styles.selectedCell,
          isHighlighted && !isSelected && styles.highlightedCell,
          cell.isInitial && styles.initialCell,
        ]}
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
    );
  },
  (prevProps, nextProps) => {
    // Custom comparison for optimal re-rendering
    return (
      prevProps.cell.value === nextProps.cell.value &&
      prevProps.cell.isInitial === nextProps.cell.isInitial &&
      prevProps.isSelected === nextProps.isSelected &&
      prevProps.isHighlighted === nextProps.isHighlighted
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
  selectedCell: {
    backgroundColor: "#BBE5F4",
  },
  highlightedCell: {
    backgroundColor: "#E8F4F8",
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
