import React, { useCallback } from 'react';
import { View, StyleSheet } from 'react-native';
import { SudokuGrid as GridType } from '../types';
import { Cell } from './Cell';

interface GridProps {
  grid: GridType;
  selectedCell: { row: number; col: number } | null;
  onCellPress: (row: number, col: number) => void;
  highlightEnabled: boolean;
}

export const Grid = React.memo<GridProps>(({
  grid,
  selectedCell,
  onCellPress,
  highlightEnabled,
}) => {
  const isHighlighted = useCallback((row: number, col: number): boolean => {
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
      grid[row][col].value === grid[selectedCell.row][selectedCell.col].value
    ) {
      return true;
    }

    return false;
  }, [highlightEnabled, selectedCell, grid]);

  if (grid.length === 0) {
    return null;
  }

  return (
    <View style={styles.container}>
      <View style={styles.grid}>
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
                onPress={onCellPress}
              />
            ))}
          </View>
        ))}
      </View>
    </View>
  );
});

Grid.displayName = 'Grid';

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  grid: {
    backgroundColor: '#000000',
    borderWidth: 3,
    borderColor: '#000000',
  },
  row: {
    flexDirection: 'row',
  },
});
