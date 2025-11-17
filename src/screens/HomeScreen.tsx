import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  TouchableOpacity,
} from 'react-native';
import { useDispatch, useSelector } from 'react-redux';
import { RootState } from '../store/store';
import { setDifficulty } from '../slices/gameSlice';
import { Button } from '../components/Button';

interface HomeScreenProps {
  onNewGame: () => void;
  onViewStats: () => void;
  onViewSettings: () => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  onNewGame,
  onViewStats,
  onViewSettings,
}) => {
  const dispatch = useDispatch();
  const difficulty = useSelector((state: RootState) => state.game.difficulty);
  const currentStreak = useSelector((state: RootState) => state.stats.currentStreak);

  const handleDifficultySelect = (level: 'easy' | 'medium' | 'hard') => {
    dispatch(setDifficulty(level));
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.content}>
        <View style={styles.header}>
          <Text style={styles.title}>Sudoku Streak</Text>
          <Text style={styles.subtitle}>Build Your Daily Puzzle Habit</Text>
          {currentStreak > 0 && (
            <View style={styles.streakContainer}>
              <Text style={styles.streakEmoji}>🔥</Text>
              <Text style={styles.streakText}>{currentStreak} day streak!</Text>
            </View>
          )}
        </View>

        <View style={styles.difficultySection}>
          <Text style={styles.sectionTitle}>Select Difficulty</Text>
          <View style={styles.difficultyButtons}>
            <TouchableOpacity
              style={[
                styles.difficultyButton,
                difficulty === 'easy' && styles.selectedDifficulty,
              ]}
              onPress={() => handleDifficultySelect('easy')}
              activeOpacity={0.7}
            >
              <Text
                style={[
                  styles.difficultyText,
                  difficulty === 'easy' && styles.selectedDifficultyText,
                ]}
              >
                Easy
              </Text>
              <Text style={styles.difficultySubtext}>35 clues</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[
                styles.difficultyButton,
                difficulty === 'medium' && styles.selectedDifficulty,
              ]}
              onPress={() => handleDifficultySelect('medium')}
              activeOpacity={0.7}
            >
              <Text
                style={[
                  styles.difficultyText,
                  difficulty === 'medium' && styles.selectedDifficultyText,
                ]}
              >
                Medium
              </Text>
              <Text style={styles.difficultySubtext}>45 clues</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[
                styles.difficultyButton,
                difficulty === 'hard' && styles.selectedDifficulty,
              ]}
              onPress={() => handleDifficultySelect('hard')}
              activeOpacity={0.7}
            >
              <Text
                style={[
                  styles.difficultyText,
                  difficulty === 'hard' && styles.selectedDifficultyText,
                ]}
              >
                Hard
              </Text>
              <Text style={styles.difficultySubtext}>55 clues</Text>
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.actionsSection}>
          <Button title="New Game" onPress={onNewGame} style={styles.newGameButton} />
          <View style={styles.secondaryButtons}>
            <Button
              title="Statistics"
              onPress={onViewStats}
              variant="secondary"
              style={styles.secondaryButton}
            />
            <Button
              title="Settings"
              onPress={onViewSettings}
              variant="secondary"
              style={styles.secondaryButton}
            />
          </View>
        </View>
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F8F9FA',
  },
  content: {
    flex: 1,
    paddingHorizontal: 24,
    justifyContent: 'space-around',
  },
  header: {
    alignItems: 'center',
    marginTop: 40,
  },
  title: {
    fontSize: 48,
    fontWeight: '700',
    color: '#2C3E50',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 18,
    color: '#7F8C8D',
    marginBottom: 16,
  },
  streakContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#FFF3E0',
    paddingVertical: 8,
    paddingHorizontal: 20,
    borderRadius: 20,
    marginTop: 12,
  },
  streakEmoji: {
    fontSize: 20,
    marginRight: 8,
  },
  streakText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#F57C00',
  },
  difficultySection: {
    marginVertical: 20,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: '600',
    color: '#2C3E50',
    marginBottom: 16,
    textAlign: 'center',
  },
  difficultyButtons: {
    gap: 12,
  },
  difficultyButton: {
    backgroundColor: '#FFFFFF',
    paddingVertical: 20,
    paddingHorizontal: 24,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: '#E0E0E0',
    alignItems: 'center',
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  selectedDifficulty: {
    borderColor: '#4A90E2',
    backgroundColor: '#E3F2FD',
  },
  difficultyText: {
    fontSize: 20,
    fontWeight: '600',
    color: '#2C3E50',
  },
  selectedDifficultyText: {
    color: '#4A90E2',
  },
  difficultySubtext: {
    fontSize: 14,
    color: '#7F8C8D',
    marginTop: 4,
  },
  actionsSection: {
    marginBottom: 40,
  },
  newGameButton: {
    width: '100%',
    marginBottom: 16,
  },
  secondaryButtons: {
    flexDirection: 'row',
    gap: 12,
  },
  secondaryButton: {
    flex: 1,
  },
});
