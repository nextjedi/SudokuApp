import React from "react";
import {
  TouchableOpacity,
  Text,
  StyleSheet,
  ViewStyle,
  Platform,
} from "react-native";

interface ButtonProps {
  title: string;
  onPress: () => void;
  variant?: "primary" | "secondary" | "danger";
  disabled?: boolean;
  style?: ViewStyle;
}

export const Button = React.memo<ButtonProps>(
  ({ title, onPress, variant = "primary", disabled = false, style }) => {
    return (
      <TouchableOpacity
        style={[
          styles.button,
          variant === "primary" && styles.primaryButton,
          variant === "secondary" && styles.secondaryButton,
          variant === "danger" && styles.dangerButton,
          disabled && styles.disabledButton,
          style,
        ]}
        onPress={onPress}
        disabled={disabled}
        activeOpacity={0.7}
      >
        <Text
          style={[
            styles.text,
            variant === "primary" && styles.primaryText,
            variant === "secondary" && styles.secondaryText,
            variant === "danger" && styles.dangerText,
            disabled && styles.disabledText,
          ]}
        >
          {title}
        </Text>
      </TouchableOpacity>
    );
  },
);

Button.displayName = "Button";

const styles = StyleSheet.create({
  button: {
    paddingVertical: 14,
    paddingHorizontal: 28,
    borderRadius: 12,
    alignItems: "center",
    justifyContent: "center",
    minWidth: 120,
    elevation: 2,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    ...Platform.select({
      web: {
        cursor: "pointer",
        userSelect: "none",
        transition: "transform 0.1s ease, opacity 0.1s ease",
      },
    }),
  },
  primaryButton: {
    backgroundColor: "#4A90E2",
  },
  secondaryButton: {
    backgroundColor: "#F5F5F5",
    borderWidth: 1,
    borderColor: "#DDD",
  },
  dangerButton: {
    backgroundColor: "#E74C3C",
  },
  disabledButton: {
    backgroundColor: "#CCC",
  },
  text: {
    fontSize: 16,
    fontWeight: "600",
  },
  primaryText: {
    color: "#FFFFFF",
  },
  secondaryText: {
    color: "#333",
  },
  dangerText: {
    color: "#FFFFFF",
  },
  disabledText: {
    color: "#999",
  },
});
