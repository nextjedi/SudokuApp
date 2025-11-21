import React, { useState, useEffect } from 'react';
import { Provider } from 'react-redux';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { store } from './src/store/store';
import { HomeScreen, GameScreen, StatsScreen, SettingsScreen } from './src/screens';
import { BlogScreen } from './src/screens/BlogScreen';
import { BlogPostScreen } from './src/screens/BlogPostScreen';
import { ErrorBoundary } from './src/components';

type Screen = 'home' | 'game' | 'stats' | 'settings' | 'blog' | 'blogPost';

function AppContent() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('home');
  const [currentPostId, setCurrentPostId] = useState<string | null>(null);

  // Persist state to AsyncStorage
  useEffect(() => {
    const saveState = async () => {
      try {
        const state = store.getState();
        await AsyncStorage.setItem('sudoku_state', JSON.stringify(state));
      } catch {
        // Failed to save state - silently ignore
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
          // State loaded from storage
        }
      } catch {
        // Failed to load state - silently ignore
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
            onViewBlog={() => setCurrentScreen('blog')}
          />
        );
      case 'game':
        return <GameScreen onExit={() => setCurrentScreen('home')} />;
      case 'stats':
        return <StatsScreen onBack={() => setCurrentScreen('home')} />;
      case 'settings':
        return <SettingsScreen onBack={() => setCurrentScreen('home')} />;
      case 'blog':
        return (
          <BlogScreen
            onBack={() => setCurrentScreen('home')}
            onPostPress={(postId: string) => {
              setCurrentPostId(postId);
              setCurrentScreen('blogPost');
            }}
          />
        );
      case 'blogPost':
        return (
          <BlogPostScreen
            postId={currentPostId || ''}
            onBack={() => setCurrentScreen('blog')}
          />
        );
      default:
        return null;
    }
  };

  return renderScreen();
}

export default function App() {
  return (
    <ErrorBoundary>
      <Provider store={store}>
        <AppContent />
      </Provider>
    </ErrorBoundary>
  );
}
