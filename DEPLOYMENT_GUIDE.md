# Deployment Guide
## Sudoku Streak - Complete Deployment Instructions

**Version:** 1.0.0
**Last Updated:** 2025-11-17

---

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Testing on Emulators](#testing-on-emulators)
3. [Capturing Screenshots & Videos](#capturing-screenshots--videos)
4. [Creating App Assets](#creating-app-assets)
5. [Building for Production](#building-for-production)
6. [App Store Deployment](#app-store-deployment)
7. [Google Play Deployment](#google-play-deployment)
8. [Post-Deployment](#post-deployment)

---

## Prerequisites

### Required Tools
```bash
# Node.js and npm
node --version  # Should be v16+
npm --version   # Should be v8+

# Expo CLI
npm install -g expo-cli
npm install -g eas-cli

# Verify installation
expo --version
eas --version
```

### Required Accounts
- [ ] Apple Developer Account ($99/year) - for iOS
- [ ] Google Play Developer Account ($25 one-time) - for Android
- [ ] Expo Account (free) - for EAS Build

### Development Environment
**For iOS:**
- macOS with Xcode installed
- iOS Simulator
- Or physical iPhone with Expo Go app

**For Android:**
- Any OS (Windows/Mac/Linux)
- Android Studio with emulator
- Or physical Android device with Expo Go app

---

## Testing on Emulators

### Step 1: Install Dependencies
```bash
cd /path/to/SudokuApp
npm install
```

### Step 2: Start Development Server
```bash
npm start
# or
npx expo start
```

This will open the Expo Dev Tools in your browser.

### Step 3A: Test on iOS Simulator (macOS only)
```bash
# Press 'i' in the terminal, or click "Run on iOS simulator" in dev tools
npm run ios

# Or manually:
npx expo run:ios
```

**Expected Result:**
- iOS Simulator launches
- App loads and shows Home Screen
- "Sudoku Streak" title visible
- Three difficulty buttons (Easy/Medium/Hard)

### Step 3B: Test on Android Emulator
```bash
# Start Android emulator first (via Android Studio)
# Then press 'a' in terminal, or click "Run on Android" in dev tools
npm run android

# Or manually:
npx expo run:android
```

**Expected Result:**
- Android emulator shows app
- Home screen with "Sudoku Streak" title
- Difficulty selection functional

### Step 4: Test All Functionality

#### Home Screen Tests
1. ✅ Tap each difficulty level (should highlight selected)
2. ✅ Tap "New Game" (should navigate to game screen)
3. ✅ Tap "Statistics" (should show stats screen)
4. ✅ Tap "Settings" (should show settings screen)

#### Game Screen Tests
1. ✅ Grid displays correctly (9x9, proper borders)
2. ✅ Tap a cell (should highlight row/column/box)
3. ✅ Tap a number (should fill selected cell)
4. ✅ Try invalid move (should show alert)
5. ✅ Complete puzzle (should show celebration)
6. ✅ Timer counts up (if enabled)
7. ✅ Mistake counter increments
8. ✅ Tap "Exit" (should confirm, then return home)

#### Stats Screen Tests
1. ✅ Shows games played/won
2. ✅ Win rate calculates correctly
3. ✅ Best times display properly
4. ✅ Streak shows if > 0
5. ✅ "Back to Home" button works

#### Settings Screen Tests
1. ✅ Toggle switches work
2. ✅ Highlight toggle affects game screen
3. ✅ Timer toggle shows/hides timer
4. ✅ Reset stats requires confirmation
5. ✅ "Back to Home" button works

### Step 5: Test Error Scenarios
1. ✅ Force close app (state should persist)
2. ✅ Reopen app (should show same state)
3. ✅ Complete game after 3 mistakes (game over)
4. ✅ Try to modify initial cells (should not allow)

---

## Capturing Screenshots & Videos

### iOS Screenshots

#### Required Sizes (App Store Connect)
- 6.5" iPhone (1284 x 2778) - iPhone 14 Pro Max
- 5.5" iPhone (1242 x 2208) - iPhone 8 Plus
- 12.9" iPad (2048 x 2732) - iPad Pro

#### How to Capture on iOS Simulator
```bash
# 1. Start iOS Simulator with specific device
npx expo run:ios --device "iPhone 14 Pro Max"

# 2. Navigate to desired screen
# 3. Press Cmd+S or File > Save Screen
# 4. Screenshots saved to ~/Desktop

# Alternative: Use built-in screenshot tool
# Press Cmd+Shift+4, then Space, then click simulator window
```

#### Required Screenshots (6 minimum)
1. **Home Screen** - Showing difficulty selection and streak
2. **Game Screen - Empty** - Starting a new puzzle
3. **Game Screen - In Progress** - Mid-game with some cells filled
4. **Game Screen - Completed** - Success dialog showing
5. **Statistics Screen** - Showing achievements
6. **Settings Screen** - Showing customization options

### Android Screenshots

#### Required Sizes (Google Play Console)
- Phone: 1080 x 1920 (or 1080 x 2340 for modern)
- 7" Tablet: 1200 x 1920
- 10" Tablet: 1600 x 2560

#### How to Capture on Android Emulator
```bash
# 1. Start Android emulator
npx expo run:android

# 2. Navigate to desired screen
# 3. Click camera icon in emulator toolbar
# Or use: Ctrl+S (Windows/Linux), Cmd+S (Mac)

# Screenshots saved to:
# Windows: C:\Users\<username>\.android\screenshots
# Mac: ~/Desktop
# Linux: ~/Pictures
```

#### Screenshot Tips
```bash
# For best quality, use high-resolution emulator
# In Android Studio AVD Manager:
# - Choose Pixel 6 or Pixel 7
# - Set "Graphics" to "Hardware"
# - Enable "Device Frame" for marketing screenshots
```

### Recording Demo Video

#### iOS Video Recording
```bash
# Using Simulator
# 1. Start recording: Cmd+R
# 2. Perform demo actions
# 3. Stop recording: Cmd+R again
# Video saved to ~/Desktop

# Using xcrun (command line)
xcrun simctl io booted recordVideo demo.mov
# Stop with Ctrl+C
```

#### Android Video Recording
```bash
# Using Android Studio
# 1. Click "Screen Record" in emulator toolbar
# 2. Perform demo actions
# 3. Click "Stop Recording"
# 4. Save video

# Using adb (command line)
adb shell screenrecord /sdcard/demo.mp4
# Stop with Ctrl+C
adb pull /sdcard/demo.mp4
```

#### Demo Video Script (30-60 seconds)
```
1. Show Home Screen (3s)
   - Pan to show "Sudoku Streak" title
   - Show streak counter (if >0)

2. Select Difficulty (2s)
   - Tap "Medium"
   - Show selection highlight

3. Start New Game (2s)
   - Tap "New Game" button

4. Demonstrate Gameplay (15s)
   - Tap a cell (show highlighting)
   - Enter a number
   - Show another cell with conflict (invalid move alert)
   - Erase a number
   - Fill in a few more cells

5. Show Features (5s)
   - Quick view of timer
   - Show mistake counter

6. Navigate to Stats (3s)
   - Tap Exit
   - Tap Statistics

7. Show Stats Screen (4s)
   - Show games played, win rate
   - Show streak

8. Navigate to Settings (3s)
   - Back to home
   - Tap Settings

9. Show Settings (3s)
   - Toggle a setting
   - Back to home

10. End (1s)
    - Show home screen final frame
```

---

## Creating App Assets

### App Icon (Required)

#### Design Specifications
```
Size: 1024x1024 pixels
Format: PNG (no transparency)
Color Space: RGB
File Name: icon.png
```

#### Design Recommendations
```
Theme: Sudoku grid + Fire emoji (for streak)
Colors: Primary blue (#4A90E2), White, Black
Style: Modern, minimalist, flat design

Elements to include:
- Stylized 3x3 Sudoku grid
- Fire emoji or flame icon (representing streak)
- Clean typography for number (optional)
- High contrast for visibility
```

#### Tools for Creation
```
Free Tools:
- Figma (figma.com) - Recommended
- Canva (canva.com)
- GIMP (gimp.org)

Paid Tools:
- Adobe Illustrator
- Sketch
- Affinity Designer
```

#### Design Process
```
1. Create 1024x1024 canvas
2. Design icon following iOS/Android guidelines:
   - No text (app name shown separately)
   - Recognizable at small sizes
   - Works on light and dark backgrounds
   - Distinct from competitors

3. Export as PNG
4. Place in: /SudokuApp/assets/icon.png
5. Generate adaptive icon for Android (see below)
```

#### Generating Adaptive Icon (Android)
```
Android requires adaptive icon (foreground + background)

Create two additional files:
- adaptive-icon.png (1024x1024) - Main icon
- Icon should fit in center 66% (safe zone)

Place in: /SudokuApp/assets/
```

### Splash Screen (Required)

#### Design Specifications
```
Size: 1242x2436 pixels (for iPhone X)
Format: PNG
Background: Solid color or simple gradient
File Name: splash.png
```

#### Design Recommendations
```
Background Color: #F8F9FA (light gray) or #4A90E2 (brand blue)
Content:
- "Sudoku Streak" text (centered)
- Small icon or logo (centered)
- Minimal design (loads quickly)
```

#### Configuration
```json
// app.json - already configured
"splash": {
  "image": "./assets/splash.png",
  "resizeMode": "contain",
  "backgroundColor": "#ffffff"
}
```

### Generating All Asset Sizes

#### Using Expo
```bash
# After placing icon.png and splash.png in assets/

# Method 1: Expo will auto-generate on build
npx expo prebuild

# Method 2: Manual generation
npx expo export:assets
```

#### Asset Checklist
- [ ] icon.png (1024x1024)
- [ ] adaptive-icon.png (1024x1024)
- [ ] splash.png (1242x2436)
- [ ] All assets in /assets/ folder

---

## Building for Production

### Setup EAS Build

#### 1. Install EAS CLI
```bash
npm install -g eas-cli

# Login to Expo account
eas login
```

#### 2. Configure EAS
```bash
# Initialize EAS (creates eas.json)
eas build:configure
```

#### 3. Create eas.json (if not exists)
```json
{
  "build": {
    "production": {
      "android": {
        "buildType": "apk",
        "gradleCommand": ":app:assembleRelease"
      },
      "ios": {
        "buildConfiguration": "Release"
      }
    },
    "preview": {
      "distribution": "internal"
    },
    "development": {
      "developmentClient": true,
      "distribution": "internal"
    }
  }
}
```

### Build for Android

#### Production APK
```bash
# Build production APK
eas build --platform android --profile production

# This will:
# 1. Upload code to Expo servers
# 2. Build APK on cloud
# 3. Provide download link when complete
# 4. Takes ~5-15 minutes
```

#### Android App Bundle (AAB) for Play Store
```json
// Update eas.json for AAB
{
  "build": {
    "production": {
      "android": {
        "buildType": "app-bundle"
      }
    }
  }
}
```

```bash
# Build AAB
eas build --platform android --profile production

# Download when complete
# File: sudoku-streak.aab
```

### Build for iOS

#### Production IPA
```bash
# Build iOS (requires Apple Developer account)
eas build --platform ios --profile production

# Follow prompts to:
# 1. Link Apple Developer account
# 2. Create App ID
# 3. Generate certificates
# 4. Create provisioning profiles

# Download IPA when complete
```

#### Troubleshooting iOS Builds
```bash
# If certificate errors:
eas build:configure

# If provisioning profile issues:
eas credentials

# View build logs:
eas build:list
```

---

## App Store Deployment (iOS)

### Prerequisites
- [ ] Apple Developer Account ($99/year)
- [ ] App icon (1024x1024)
- [ ] Screenshots (all required sizes)
- [ ] App built with EAS

### Step 1: Create App in App Store Connect

1. Go to [App Store Connect](https://appstoreconnect.apple.com)
2. Click "My Apps" → "+" → "New App"
3. Fill in details:
   - **Platform:** iOS
   - **Name:** Sudoku Streak
   - **Primary Language:** English
   - **Bundle ID:** com.yourdomain.sudokustreak
   - **SKU:** sudoku-streak-001
   - **User Access:** Full Access

### Step 2: Fill in App Information

#### App Information
```
Name: Sudoku Streak
Subtitle: Daily Puzzle Challenge
Privacy Policy URL: (if collecting data)
Category: Games > Puzzle
Secondary Category: Games > Board
```

#### Version Information
```
Version: 1.0.0
Copyright: 2025 Sudoku Streak Team

Description:
Build your daily puzzle-solving habit with Sudoku Streak!

Challenge yourself with classic Sudoku puzzles in three difficulty levels. Track your progress with daily streaks, maintain your win rate, and improve your best times.

FEATURES:
🔥 Daily Streak Tracking - Build and maintain your solving streak
📊 Comprehensive Statistics - Track games played, win rate, and best times
⏱️ Timer & Mistake Counter - Improve your speed and accuracy
🎯 Three Difficulty Levels - Easy, Medium, and Hard puzzles
✨ Clean, Modern Design - Intuitive interface for the best experience
💾 Offline Play - No internet required
🎨 Customizable Settings - Adjust highlighting, timer, and more

Perfect for:
- Puzzle enthusiasts
- Brain training
- Daily habit building
- Stress relief
- Improving focus

Start your Sudoku streak today!

Keywords:
sudoku, puzzle, brain, game, streak, daily, logic, number, grid, thinking
```

#### What's New (v1.0.0)
```
Initial release of Sudoku Streak!

• Classic Sudoku gameplay
• Three difficulty levels
• Daily streak tracking
• Comprehensive statistics
• Clean, modern interface
• Offline play support

Start building your puzzle habit today!
```

### Step 3: Upload Screenshots

Upload screenshots for:
- [ ] 6.5" iPhone (1284 x 2778) - Required
- [ ] 5.5" iPhone (1242 x 2208) - Required
- [ ] 12.9" iPad (2048 x 2732) - Optional but recommended

### Step 4: Upload Build

```bash
# Upload IPA to App Store Connect
# EAS automatically uploads, or manually:

# 1. Download IPA from EAS
# 2. In App Store Connect, go to your app
# 3. Click on version
# 4. Under "Build", click "+" to select build
# 5. Select the build uploaded by EAS
```

### Step 5: Submit for Review

1. Complete all required fields
2. Add age rating (4+recommended)
3. Submit for review
4. Review typically takes 24-48 hours

---

## Google Play Deployment (Android)

### Prerequisites
- [ ] Google Play Developer Account ($25 one-time)
- [ ] App icon (512x512 for Play Store)
- [ ] Feature graphic (1024x500)
- [ ] Screenshots (at least 2)
- [ ] App built as AAB

### Step 1: Create App in Play Console

1. Go to [Google Play Console](https://play.google.com/console)
2. Click "Create app"
3. Fill in details:
   - **App name:** Sudoku Streak
   - **Default language:** English (United States)
   - **App or game:** Game
   - **Free or paid:** Free

### Step 2: Complete Store Listing

#### Store Listing
```
App name: Sudoku Streak
Short description (80 chars):
Daily Sudoku puzzles with streak tracking. Build your puzzle-solving habit!

Full description (4000 chars):
Build your daily puzzle-solving habit with Sudoku Streak!

Challenge yourself with classic Sudoku puzzles in three difficulty levels. Track your progress with daily streaks, maintain your win rate, and improve your best times.

🔥 KEY FEATURES:
• Daily Streak Tracking - Build and maintain your solving streak
• Comprehensive Statistics - Track games played, win rate, and best times
• Timer & Mistake Counter - Improve your speed and accuracy
• Three Difficulty Levels - Easy, Medium, and Hard puzzles
• Clean, Modern Design - Intuitive interface for the best experience
• Offline Play - No internet required
• Customizable Settings - Adjust highlighting, timer, and more

🎯 PERFECT FOR:
• Puzzle enthusiasts
• Brain training
• Daily habit building
• Stress relief
• Improving focus and concentration

📊 TRACK YOUR PROGRESS:
• See your daily streak
• Monitor win rate
• Beat your best times
• View detailed statistics

✨ BEAUTIFUL DESIGN:
• Modern, clean interface
• Easy to use
• Smooth animations
• Responsive controls

🎮 HOW TO PLAY:
1. Select your difficulty level
2. Fill the 9x9 grid with numbers 1-9
3. Each row, column, and 3x3 box must contain all digits
4. Use highlighting to spot patterns
5. Track your time and maintain your streak!

No ads, no subscriptions, no internet required. Just pure Sudoku fun!

Start your Sudoku streak today!
```

#### Graphics
- [ ] App icon: 512x512
- [ ] Feature graphic: 1024x500
- [ ] Screenshots: Phone (min 2), Tablet (recommended)

### Step 3: Content Rating

1. Start questionnaire
2. Select "Utility, Productivity, Communication, or Other"
3. Answer questions (all "No" for this app)
4. Submit - should receive E (Everyone)

### Step 4: Upload AAB

```bash
# In Play Console
# 1. Go to "Release" → "Production"
# 2. Click "Create new release"
# 3. Upload AAB file
# 4. Add release notes (What's New)
```

#### Release Notes (v1.0.0)
```
Initial release of Sudoku Streak!

✨ Features:
- Classic Sudoku gameplay
- Three difficulty levels (Easy, Medium, Hard)
- Daily streak tracking
- Comprehensive statistics
- Timer and mistake counter
- Offline play
- Customizable settings

🔥 Start building your puzzle habit today!
```

### Step 5: Countries & Regions

- Select all countries or specific regions
- Recommended: Start with "All countries"

### Step 6: Submit for Review

1. Complete all required sections
2. Review and confirm
3. Submit for review
4. Review typically takes 2-7 days

---

## Post-Deployment

### Monitoring

#### Set Up Analytics (Recommended)
```bash
# Install Firebase
npm install @react-native-firebase/app
npm install @react-native-firebase/analytics

# Configure in app
# Track key events:
# - game_started
# - game_completed
# - streak_achieved
# - settings_changed
```

#### Set Up Crash Reporting (Recommended)
```bash
# Install Sentry
npm install @sentry/react-native

# Configure in App.tsx
# Auto-reports crashes and errors
```

### Version Updates

#### For Future Updates
```bash
# 1. Update version in app.json and package.json
# 2. Make code changes
# 3. Build new version
eas build --platform all --profile production

# 4. Upload to stores
# 5. Add "What's New" description
```

### Marketing

#### App Store Optimization (ASO)
```
Title: Keep "Sudoku Streak"
Subtitle: Vary for testing
  - "Daily Puzzle Challenge"
  - "Build Your Puzzle Habit"
  - "Brain Training Game"

Keywords: Test and optimize
  Primary: sudoku, puzzle, brain
  Secondary: daily, streak, game
  Tertiary: logic, number, grid
```

#### Social Media
```
- Create app landing page
- Share on Twitter/X
- Post on Reddit (r/sudoku, r/puzzles)
- Product Hunt launch
- App review sites
```

---

## Troubleshooting

### Common Build Issues

```bash
# Metro bundler cache issues
npx expo start --clear

# Dependency issues
rm -rf node_modules package-lock.json
npm install

# Build configuration issues
npx expo prebuild --clean

# iOS certificate issues
eas credentials

# Android signing issues
eas build:configure
```

### Common Submission Rejections

#### App Store
- Missing privacy policy (if collecting data)
- Screenshots don't match functionality
- App crashes on launch
- Missing required screenshots

#### Google Play
- Incomplete store listing
- Low-quality screenshots
- Missing content rating
- Invalid package name

---

## Checklist: Ready to Deploy

### Development Complete ✅
- [x] All features implemented
- [x] TypeScript passes
- [x] ESLint passes
- [x] Tested on iOS simulator
- [x] Tested on Android emulator
- [x] Error handling implemented
- [x] Performance optimized

### Assets Created
- [ ] App icon (1024x1024)
- [ ] Adaptive icon (Android)
- [ ] Splash screen (1242x2436)
- [ ] iOS screenshots (6.5" + 5.5")
- [ ] Android screenshots (Phone + Tablet)
- [ ] Demo video (optional)
- [ ] Feature graphic (Play Store, 1024x500)

### Store Listings Written
- [ ] App name finalized
- [ ] Description written
- [ ] Keywords researched
- [ ] "What's New" prepared
- [ ] Privacy policy (if needed)

### Builds Ready
- [ ] iOS IPA built with EAS
- [ ] Android AAB built with EAS
- [ ] Tested on real devices

### Accounts Ready
- [ ] Apple Developer Account ($99)
- [ ] Google Play Developer Account ($25)
- [ ] Expo Account (free)

### Submit!
- [ ] iOS submitted to App Store
- [ ] Android submitted to Play Store
- [ ] Monitoring set up
- [ ] Ready to iterate based on feedback

---

**Good luck with your launch! 🚀**

For questions or issues, check:
- Expo Docs: https://docs.expo.dev
- React Native Docs: https://reactnative.dev
- App Store Guidelines: https://developer.apple.com/app-store/review/guidelines/
- Play Store Policies: https://play.google.com/console/about/guides/
