import React from "react";
import { View, Text, StyleSheet, ScrollView } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useSelector } from "react-redux";
import { RootState } from "../store/store";
import { Button } from "../components/Button";
import { formatTime } from "../utils/helpers";

interface StatsScreenProps {
  onBack: () => void;
}

export const StatsScreen: React.FC<StatsScreenProps> = ({ onBack }) => {
  const stats = useSelector((state: RootState) => state.stats);

  const winRate =
    stats.gamesPlayed > 0
      ? Math.round((stats.gamesWon / stats.gamesPlayed) * 100)
      : 0;

  const averageTime =
    stats.gamesWon > 0 ? Math.floor(stats.totalTime / stats.gamesWon) : 0;

  const formatBestTime = (time: number): string => {
    if (time === Infinity) return "N/A";
    return formatTime(time);
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.header}>
          <Text style={styles.title}>Statistics</Text>
          <Text style={styles.subtitle}>Your Sudoku Journey</Text>
        </View>

        <View style={styles.statsSection}>
          <View style={styles.statCard}>
            <Text style={styles.statValue}>{stats.currentStreak}</Text>
            <Text style={styles.statLabel}>🔥 Current Streak</Text>
            <Text style={styles.statSubtext}>consecutive days</Text>
          </View>

          <View style={styles.statCard}>
            <Text style={styles.statValue}>{stats.gamesPlayed}</Text>
            <Text style={styles.statLabel}>🎮 Games Played</Text>
            <Text style={styles.statSubtext}>total games</Text>
          </View>

          <View style={styles.statCard}>
            <Text style={styles.statValue}>{stats.gamesWon}</Text>
            <Text style={styles.statLabel}>🏆 Games Won</Text>
            <Text style={styles.statSubtext}>completed successfully</Text>
          </View>

          <View style={styles.statCard}>
            <Text style={styles.statValue}>{winRate}%</Text>
            <Text style={styles.statLabel}>📊 Win Rate</Text>
            <Text style={styles.statSubtext}>success percentage</Text>
          </View>
        </View>

        <View style={styles.timeSection}>
          <Text style={styles.sectionTitle}>⏱️ Best Times</Text>

          <View style={styles.timeCard}>
            <View style={styles.timeRow}>
              <Text style={styles.timeLabel}>Easy</Text>
              <Text style={styles.timeValue}>
                {formatBestTime(stats.bestTimes.easy)}
              </Text>
            </View>
          </View>

          <View style={styles.timeCard}>
            <View style={styles.timeRow}>
              <Text style={styles.timeLabel}>Medium</Text>
              <Text style={styles.timeValue}>
                {formatBestTime(stats.bestTimes.medium)}
              </Text>
            </View>
          </View>

          <View style={styles.timeCard}>
            <View style={styles.timeRow}>
              <Text style={styles.timeLabel}>Hard</Text>
              <Text style={styles.timeValue}>
                {formatBestTime(stats.bestTimes.hard)}
              </Text>
            </View>
          </View>

          <View style={styles.averageCard}>
            <Text style={styles.averageLabel}>Average Completion Time</Text>
            <Text style={styles.averageValue}>{formatTime(averageTime)}</Text>
          </View>
        </View>

        <View style={styles.actions}>
          <Button title="Back to Home" onPress={onBack} />
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#F8F9FA",
  },
  content: {
    flexGrow: 1,
    paddingHorizontal: 24,
    paddingVertical: 20,
  },
  header: {
    alignItems: "center",
    marginBottom: 32,
  },
  title: {
    fontSize: 36,
    fontWeight: "700",
    color: "#2C3E50",
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: "#7F8C8D",
  },
  statsSection: {
    marginBottom: 32,
  },
  statCard: {
    backgroundColor: "#FFFFFF",
    padding: 20,
    borderRadius: 12,
    marginBottom: 12,
    alignItems: "center",
    elevation: 2,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  statValue: {
    fontSize: 42,
    fontWeight: "700",
    color: "#4A90E2",
    marginBottom: 8,
  },
  statLabel: {
    fontSize: 18,
    fontWeight: "600",
    color: "#2C3E50",
    marginBottom: 4,
  },
  statSubtext: {
    fontSize: 14,
    color: "#7F8C8D",
  },
  timeSection: {
    marginBottom: 32,
  },
  sectionTitle: {
    fontSize: 22,
    fontWeight: "600",
    color: "#2C3E50",
    marginBottom: 16,
  },
  timeCard: {
    backgroundColor: "#FFFFFF",
    padding: 16,
    borderRadius: 12,
    marginBottom: 8,
    elevation: 2,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  timeRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  timeLabel: {
    fontSize: 18,
    fontWeight: "600",
    color: "#2C3E50",
  },
  timeValue: {
    fontSize: 20,
    fontWeight: "700",
    color: "#4A90E2",
    fontVariant: ["tabular-nums"],
  },
  averageCard: {
    backgroundColor: "#E3F2FD",
    padding: 20,
    borderRadius: 12,
    marginTop: 16,
    alignItems: "center",
  },
  averageLabel: {
    fontSize: 16,
    fontWeight: "600",
    color: "#2C3E50",
    marginBottom: 8,
  },
  averageValue: {
    fontSize: 28,
    fontWeight: "700",
    color: "#4A90E2",
    fontVariant: ["tabular-nums"],
  },
  actions: {
    marginTop: "auto",
    paddingTop: 20,
  },
});
