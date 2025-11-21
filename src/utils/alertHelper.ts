import { Alert, Platform } from "react-native";

interface AlertButton {
  text: string;
  onPress?: () => void;
  style?: "default" | "cancel" | "destructive";
}

/**
 * Cross-platform alert helper that works on mobile and web
 */
export const showAlert = (
  title: string,
  message: string,
  buttons: AlertButton[] = [{ text: "OK" }],
): void => {
  if (Platform.OS === "web") {
    // For web, use browser's confirm dialog for yes/no choices
    // or alert for simple notifications
    if (buttons.length === 1) {
      window.alert(`${title}\n\n${message}`);
      buttons[0].onPress?.();
    } else {
      // Find cancel and confirm buttons
      const cancelButton = buttons.find((b) => b.style === "cancel");
      const confirmButton =
        buttons.find((b) => b.style !== "cancel") ||
        buttons[buttons.length - 1];

      const result = window.confirm(`${title}\n\n${message}`);
      if (result) {
        confirmButton?.onPress?.();
      } else {
        cancelButton?.onPress?.();
      }
    }
  } else {
    Alert.alert(title, message, buttons);
  }
};

/**
 * Show a simple notification alert
 */
export const showNotification = (
  title: string,
  message: string,
  onDismiss?: () => void,
): void => {
  showAlert(title, message, [{ text: "OK", onPress: onDismiss }]);
};

/**
 * Show a confirmation dialog with Cancel and Confirm options
 */
export const showConfirmation = (
  title: string,
  message: string,
  onConfirm: () => void,
  onCancel?: () => void,
  confirmText: string = "Confirm",
  cancelText: string = "Cancel",
): void => {
  showAlert(title, message, [
    { text: cancelText, style: "cancel", onPress: onCancel },
    { text: confirmText, onPress: onConfirm },
  ]);
};
