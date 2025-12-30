import React from "react";
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  Switch,
  TouchableOpacity,
  Alert,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useDispatch, useSelector } from "react-redux";
import { RootState } from "../store/store";
import {
  toggleSound,
  toggleHighlight,
  toggleTimer,
  toggleDarkMode,
} from "../slices/settingsSlice";
import { resetStats } from "../slices/statsSlice";
import { Button } from "../components/Button";

interface SettingsScreenProps {
  onBack: () => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({ onBack }) => {
  const dispatch = useDispatch();
  const settings = useSelector((state: RootState) => state.settings);

  const handleResetStats = () => {
    Alert.alert(
      "Reset Statistics",
      "Are you sure you want to reset all statistics? This action cannot be undone.",
      [
        { text: "Cancel", style: "cancel" },
        {
          text: "Reset",
          style: "destructive",
          onPress: () => {
            dispatch(resetStats());
            Alert.alert("Success", "Statistics have been reset.");
          },
        },
      ],
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.header}>
          <Text style={styles.title}>Settings</Text>
          <Text style={styles.subtitle}>Customize Your Experience</Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Game Settings</Text>

          <View style={styles.settingItem}>
            <View style={styles.settingInfo}>
              <Text style={styles.settingLabel}>Sound Effects</Text>
              <Text style={styles.settingDescription}>
                Enable sound feedback
              </Text>
            </View>
            <Switch
              value={settings.soundEnabled}
              onValueChange={() => {
                void dispatch(toggleSound());
              }}
              trackColor={{ false: "#D1D1D6", true: "#4A90E2" }}
              thumbColor="#FFFFFF"
            />
          </View>

          <View style={styles.settingItem}>
            <View style={styles.settingInfo}>
              <Text style={styles.settingLabel}>Cell Highlighting</Text>
              <Text style={styles.settingDescription}>
                Highlight related cells
              </Text>
            </View>
            <Switch
              value={settings.highlightEnabled}
              onValueChange={() => {
                void dispatch(toggleHighlight());
              }}
              trackColor={{ false: "#D1D1D6", true: "#4A90E2" }}
              thumbColor="#FFFFFF"
            />
          </View>

          <View style={styles.settingItem}>
            <View style={styles.settingInfo}>
              <Text style={styles.settingLabel}>Show Timer</Text>
              <Text style={styles.settingDescription}>Display game timer</Text>
            </View>
            <Switch
              value={settings.timerEnabled}
              onValueChange={() => {
                void dispatch(toggleTimer());
              }}
              trackColor={{ false: "#D1D1D6", true: "#4A90E2" }}
              thumbColor="#FFFFFF"
            />
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Appearance</Text>

          <View style={styles.settingItem}>
            <View style={styles.settingInfo}>
              <Text style={styles.settingLabel}>Dark Mode</Text>
              <Text style={styles.settingDescription}>
                Use dark theme (Coming soon)
              </Text>
            </View>
            <Switch
              value={settings.darkMode}
              onValueChange={() => {
                void dispatch(toggleDarkMode());
              }}
              trackColor={{ false: "#D1D1D6", true: "#4A90E2" }}
              thumbColor="#FFFFFF"
              disabled={true}
            />
          </View>
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Data</Text>

          <TouchableOpacity
            style={styles.dangerButton}
            onPress={handleResetStats}
            activeOpacity={0.7}
          >
            <Text style={styles.dangerButtonText}>Reset All Statistics</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.infoSection}>
          <Text style={styles.infoText}>Sudoku Streak v1.0.0</Text>
          <Text style={styles.infoSubtext}>
            Build your daily puzzle habit with streak tracking
          </Text>
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
  section: {
    marginBottom: 32,
  },
  sectionTitle: {
    fontSize: 20,
    fontWeight: "600",
    color: "#2C3E50",
    marginBottom: 16,
  },
  settingItem: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    backgroundColor: "#FFFFFF",
    padding: 16,
    borderRadius: 12,
    marginBottom: 12,
    elevation: 2,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  settingInfo: {
    flex: 1,
    marginRight: 16,
  },
  settingLabel: {
    fontSize: 16,
    fontWeight: "600",
    color: "#2C3E50",
    marginBottom: 4,
  },
  settingDescription: {
    fontSize: 14,
    color: "#7F8C8D",
  },
  dangerButton: {
    backgroundColor: "#FFFFFF",
    padding: 16,
    borderRadius: 12,
    alignItems: "center",
    borderWidth: 2,
    borderColor: "#E74C3C",
  },
  dangerButtonText: {
    fontSize: 16,
    fontWeight: "600",
    color: "#E74C3C",
  },
  infoSection: {
    alignItems: "center",
    marginVertical: 24,
  },
  infoText: {
    fontSize: 14,
    fontWeight: "600",
    color: "#7F8C8D",
    marginBottom: 4,
  },
  infoSubtext: {
    fontSize: 12,
    color: "#95A5A6",
    textAlign: "center",
  },
  actions: {
    marginTop: "auto",
    paddingTop: 20,
  },
});
