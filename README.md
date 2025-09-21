# Sudoku App

A React Native offline Sudoku game with daily streak tracking, notifications, and comprehensive statistics.

## Getting Started

### Prerequisites
- Node.js (v16 or higher)
- npm or yarn
- Expo CLI
- For iOS development: Xcode
- For Android development: Android Studio

### Installation

1. Install dependencies:
```bash
npm install
```

2. Start the development server:
```bash
npm start
```

## Available Scripts

### Development
- `npm start` - Start Expo development server
- `npm run android` - Run on Android device/emulator
- `npm run ios` - Run on iOS device/simulator
- `npm run web` - Run in web browser

### Testing & Quality
- `npm test` - Run Jest tests
- `npm run lint` - Check code quality with ESLint
- `npm run lint:fix` - Auto-fix ESLint issues
- `npm run typecheck` - Verify TypeScript types

## Features

- **Offline Sudoku Game**: Play without internet connection
- **Daily Streak System**: Track consecutive days of gameplay
- **Local Notifications**: Daily reminders to maintain streaks
- **Statistics Tracking**: Games played, win rate, best times
- **Multiple Difficulties**: Easy, Medium, Hard levels
- **Redux State Management**: Persistent game state
- **TypeScript**: Full type safety

## Project Structure

```
src/
├── components/     # Reusable UI components
├── screens/        # App screens
├── store/          # Redux store configuration
├── slices/         # Redux slices (game, stats, settings)
├── types/          # TypeScript type definitions
└── utils/          # Utility functions
```

## Tech Stack

- **React Native** - Mobile app framework
- **Expo** - Development platform
- **TypeScript** - Type safety
- **Redux Toolkit** - State management
- **AsyncStorage** - Local data persistence
- **Expo Notifications** - Local notifications
- **Jest** - Testing framework