import React, { useCallback, useMemo } from 'react';
import { View, TouchableOpacity, Text, StyleSheet } from 'react-native';

interface NumberPadProps {
  onNumberPress: (num: number) => void;
  onErase: () => void;
  disabled?: boolean;
}

export const NumberPad = React.memo<NumberPadProps>(({
  onNumberPress,
  onErase,
  disabled = false,
}) => {
  const numbers = useMemo(() => [1, 2, 3, 4, 5, 6, 7, 8, 9], []);

  const handleNumberPress = useCallback((num: number) => {
    onNumberPress(num);
  }, [onNumberPress]);

  return (
    <View style={styles.container}>
      <View style={styles.numbersContainer}>
        {numbers.map((num) => (
          <TouchableOpacity
            key={num}
            style={[styles.numberButton, disabled && styles.disabledButton]}
            onPress={() => handleNumberPress(num)}
            disabled={disabled}
            activeOpacity={0.7}
          >
            <Text style={[styles.numberText, disabled && styles.disabledText]}>
              {num}
            </Text>
          </TouchableOpacity>
        ))}
      </View>
      <TouchableOpacity
        style={[styles.eraseButton, disabled && styles.disabledButton]}
        onPress={onErase}
        disabled={disabled}
        activeOpacity={0.7}
      >
        <Text style={[styles.eraseText, disabled && styles.disabledText]}>
          Erase
        </Text>
      </TouchableOpacity>
    </View>
  );
});

NumberPad.displayName = 'NumberPad';

const styles = StyleSheet.create({
  container: {
    width: '100%',
    paddingHorizontal: 16,
  },
  numbersContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    marginBottom: 12,
  },
  numberButton: {
    width: 50,
    height: 50,
    backgroundColor: '#4A90E2',
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
    margin: 6,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 3,
  },
  numberText: {
    fontSize: 24,
    fontWeight: '600',
    color: '#FFFFFF',
  },
  eraseButton: {
    backgroundColor: '#E74C3C',
    paddingVertical: 14,
    borderRadius: 8,
    alignItems: 'center',
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 3,
  },
  eraseText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#FFFFFF',
  },
  disabledButton: {
    backgroundColor: '#CCC',
    opacity: 0.5,
  },
  disabledText: {
    color: '#999',
  },
});
