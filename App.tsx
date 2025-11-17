import React, { useState, useEffect } from 'react';
import { Provider } from 'react-redux';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { store } from './src/store/store';
import { HomeScreen, GameScreen, StatsScreen, SettingsScreen } from './src/screens';

type Screen = 'home' | 'game' | 'stats' | 'settings';

function AppContent() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('home');

  // Persist state to AsyncStorage
  useEffect(() => {
    const saveState = async () => {
      try {
        const state = store.getState();
        await AsyncStorage.setItem('sudoku_state', JSON.stringify(state));
      } catch (error) {
        console.error('Failed to save state:', error);
      }
    };

    const unsubscribe = store.subscribe(saveState);
    return () => unsubscribe();
  }, []);

  // Load state from AsyncStorage on mount
  useEffect(() => {
    const loadState = async () => {
      try {
        const savedState = await AsyncStorage.getItem('sudoku_state');
        if (savedState) {
          // State will be loaded through the store's preloadedState
          // For now, we'll just log it
          console.log('State loaded from storage');
        }
      } catch (error) {
        console.error('Failed to load state:', error);
      }
    };

    loadState();
  }, []);

  const renderScreen = () => {
    switch (currentScreen) {
      case 'home':
        return (
          <HomeScreen
            onNewGame={() => setCurrentScreen('game')}
            onViewStats={() => setCurrentScreen('stats')}
            onViewSettings={() => setCurrentScreen('settings')}
          />
        );
      case 'game':
        return <GameScreen onExit={() => setCurrentScreen('home')} />;
      case 'stats':
        return <StatsScreen onBack={() => setCurrentScreen('home')} />;
      case 'settings':
        return <SettingsScreen onBack={() => setCurrentScreen('home')} />;
      default:
        return null;
    }
  };

  return renderScreen();
}

export default function App() {
  return (
    <Provider store={store}>
      <AppContent />
    </Provider>
  );
}
