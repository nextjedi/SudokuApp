import React, { useState, useEffect } from "react";
import { View, StyleSheet, Platform } from "react-native";
import { Provider } from "react-redux";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { store } from "./src/store/store";
import {
  HomeScreen,
  GameScreen,
  StatsScreen,
  SettingsScreen,
} from "./src/screens";
import { BlogScreen } from "./src/screens/BlogScreen";
import { BlogPostScreen } from "./src/screens/BlogPostScreen";
import { ErrorBoundary } from "./src/components";
import { setGlobalContext } from "./src/services/errorReporting";

type Screen = "home" | "game" | "stats" | "settings" | "blog" | "blogPost";

function AppContent() {
  const [currentScreen, setCurrentScreen] = useState<Screen>("home");
  const [currentPostId, setCurrentPostId] = useState<string | null>(null);

  // Initialize error reporting
  useEffect(() => {
    setGlobalContext({
      platform: Platform.OS,
      version: "1.0.0",
    });
  }, []);

  // Persist state to AsyncStorage
  useEffect(() => {
    const saveState = async () => {
      try {
        const state = store.getState();
        await AsyncStorage.setItem("sudoku_state", JSON.stringify(state));
      } catch (error) {
        console.warn("Failed to save state:", error);
      }
    };

    const unsubscribe = store.subscribe(saveState);
    return () => unsubscribe();
  }, []);

  // Load state from AsyncStorage on mount
  useEffect(() => {
    const loadState = async () => {
      try {
        const savedState = await AsyncStorage.getItem("sudoku_state");
        if (savedState) {
          // State loaded from storage
        }
      } catch (error) {
        console.warn("Failed to load state:", error);
      }
    };

    loadState();
  }, []);

  const renderScreen = () => {
    switch (currentScreen) {
      case "home":
        return (
          <HomeScreen
            onNewGame={() => setCurrentScreen("game")}
            onViewStats={() => setCurrentScreen("stats")}
            onViewSettings={() => setCurrentScreen("settings")}
            onViewBlog={() => setCurrentScreen("blog")}
          />
        );
      case "game":
        return <GameScreen onExit={() => setCurrentScreen("home")} />;
      case "stats":
        return <StatsScreen onBack={() => setCurrentScreen("home")} />;
      case "settings":
        return <SettingsScreen onBack={() => setCurrentScreen("home")} />;
      case "blog":
        return (
          <BlogScreen
            onBack={() => setCurrentScreen("home")}
            onPostPress={(postId: string) => {
              setCurrentPostId(postId);
              setCurrentScreen("blogPost");
            }}
          />
        );
      case "blogPost":
        return (
          <BlogPostScreen
            postId={currentPostId || ""}
            onBack={() => setCurrentScreen("blog")}
          />
        );
      default:
        return null;
    }
  };

  return (
    <View style={styles.webContainer}>
      <View style={styles.mobileContainer}>{renderScreen()}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  webContainer: {
    flex: 1,
    backgroundColor: "#f5f5f5",
    alignItems: "center",
    justifyContent: "center",
  },
  mobileContainer: {
    flex: 1,
    width: Platform.OS === "web" ? "80%" : "100%",
    backgroundColor: "#fff",
    ...(Platform.OS === "web" && {
      boxShadow: "0 0 20px rgba(0,0,0,0.1)",
    }),
  },
});

export default function App() {
  return (
    <ErrorBoundary>
      <Provider store={store}>
        <AppContent />
      </Provider>
    </ErrorBoundary>
  );
}
