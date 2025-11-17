import React from 'react';
import { TouchableOpacity, Text, StyleSheet, ViewStyle } from 'react-native';
import { SudokuCell as CellType } from '../types';

interface CellProps {
  cell: CellType;
  row: number;
  col: number;
  isSelected: boolean;
  isHighlighted: boolean;
  onPress: (row: number, col: number) => void;
}

export const Cell: React.FC<CellProps> = ({
  cell,
  row,
  col,
  isSelected,
  isHighlighted,
  onPress,
}) => {
  const handlePress = () => {
    onPress(row, col);
  };

  const getBorderStyle = (): ViewStyle => {
    const borderStyle: ViewStyle = {
      borderTopWidth: row % 3 === 0 ? 3 : 1,
      borderLeftWidth: col % 3 === 0 ? 3 : 1,
      borderRightWidth: col === 8 ? 3 : 0,
      borderBottomWidth: row === 8 ? 3 : 0,
    };
    return borderStyle;
  };

  return (
    <TouchableOpacity
      style={[
        styles.cell,
        getBorderStyle(),
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
        {cell.value !== 0 ? cell.value : ''}
      </Text>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  cell: {
    width: 40,
    height: 40,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
    borderColor: '#000000',
  },
  selectedCell: {
    backgroundColor: '#BBE5F4',
  },
  highlightedCell: {
    backgroundColor: '#E8F4F8',
  },
  initialCell: {
    backgroundColor: '#F5F5F5',
  },
  cellText: {
    fontSize: 20,
    fontWeight: '500',
    color: '#4A90E2',
  },
  initialText: {
    color: '#000000',
    fontWeight: '700',
  },
  emptyText: {
    color: 'transparent',
  },
});
