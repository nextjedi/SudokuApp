import React from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
} from "react-native";
import { ErrorBoundary } from "./ErrorBoundary";

interface ScreenErrorBoundaryProps {
  children: React.ReactNode;
  screenName: string;
  onRetry?: () => void;
}

export const ScreenErrorBoundary: React.FC<ScreenErrorBoundaryProps> = ({
  children,
  screenName,
  onRetry,
}) => {
  const fallbackUI = (
    <ScrollView contentContainerStyle={styles.container} bounces={false}>
      <View style={styles.content}>
        <Text style={styles.emoji}>⚠️</Text>
        <Text style={styles.title}>Something went wrong</Text>
        <Text style={styles.message}>
          We encountered an issue with the {screenName} screen. This might be
          due to a temporary problem.
        </Text>
        <Text style={styles.screenInfo}>Screen: {screenName}</Text>

        <TouchableOpacity
          style={styles.button}
          onPress={onRetry || (() => window.location.reload())}
          activeOpacity={0.7}
        >
          <Text style={styles.buttonText}>Try Again</Text>
        </TouchableOpacity>
      </View>
    </ScrollView>
  );

  return (
    <ErrorBoundary fallback={fallbackUI} context={{ screen: screenName }}>
      {children}
    </ErrorBoundary>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#F8F9FA",
    justifyContent: "center",
    alignItems: "center",
    padding: 20,
  },
  content: {
    maxWidth: 400,
    alignItems: "center",
  },
  emoji: {
    fontSize: 64,
    marginBottom: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: "700",
    color: "#2C3E50",
    marginBottom: 12,
    textAlign: "center",
  },
  message: {
    fontSize: 16,
    color: "#7F8C8D",
    textAlign: "center",
    marginBottom: 16,
    lineHeight: 24,
  },
  screenInfo: {
    fontSize: 14,
    color: "#95A5A6",
    marginBottom: 24,
    fontStyle: "italic",
  },
  button: {
    backgroundColor: "#4A90E2",
    paddingVertical: 14,
    paddingHorizontal: 32,
    borderRadius: 12,
    elevation: 2,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  buttonText: {
    color: "#FFFFFF",
    fontSize: 16,
    fontWeight: "600",
  },
});
