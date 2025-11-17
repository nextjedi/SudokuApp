import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { formatTime } from '../utils/helpers';

interface TimerProps {
  seconds: number;
}

export const Timer: React.FC<TimerProps> = ({ seconds }) => {
  return (
    <View style={styles.container}>
      <Text style={styles.icon}>⏱️</Text>
      <Text style={styles.time}>{formatTime(seconds)}</Text>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#F5F5F5',
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 20,
  },
  icon: {
    fontSize: 18,
    marginRight: 6,
  },
  time: {
    fontSize: 18,
    fontWeight: '600',
    color: '#333',
    fontVariant: ['tabular-nums'],
  },
});
