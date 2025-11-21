# Quick Start: Capture Screenshots for Sudoku Streak

## Prerequisites Checklist

- [ ] Android Studio installed
- [ ] Android emulator created (Pixel 6/7 recommended)
- [ ] Node.js and npm installed
- [ ] Project dependencies installed (`npm install`)

---

## Step-by-Step Process

### 1. Start Android Emulator

**Option A: Through Android Studio**
```
1. Open Android Studio
2. Click on Device Manager (phone icon in toolbar)
3. Click ▶️ play button next to a device
4. Wait for emulator to fully boot
```

**Option B: Command Line**
```bash
# List available emulators
emulator -list-avds

# Start specific emulator
emulator -avd Pixel_6_API_34
```

### 2. Start Your App

```bash
cd d:/Projects/SudokuApp

# Make sure dependencies are installed
npm install

# Start the app
npm run android
```

**What to expect:**
- Metro bundler will start
- App will install on emulator
- App will open automatically
- You should see the Sudoku Streak home screen

### 3. Prepare for Screenshots

**Play 2-3 games first** so your statistics look realistic:

```
1. Select "Easy" difficulty
2. Tap "New Game"
3. Complete the puzzle (or get close)
4. Return to home
5. Repeat 2-3 times
```

This ensures your stats screen shows meaningful data!

---

## Screenshot Capture Instructions

### Screenshot 1: Home Screen 🏠

```
✅ What to capture:
   - "Sudoku Streak" title
   - Three difficulty buttons
   - Streak indicator (will show after completing games)
   - Navigation buttons

📸 How to capture:
   1. Make sure you're on the home screen
   2. Press Ctrl+S (Windows) or Cmd+S (Mac)
   3. Or click camera icon 📷 in emulator toolbar
   4. Save as: 01-home-screen.png
```

### Screenshot 2: Game Screen - Starting 🎮

```
✅ What to capture:
   - Fresh Sudoku puzzle
   - Number pad visible
   - Timer showing 00:00
   - Mistakes showing 0/3

📸 How to capture:
   1. From home, select "Medium" difficulty
   2. Tap "New Game"
   3. DON'T tap any cells yet
   4. Press Ctrl+S immediately
   5. Save as: 02-game-starting.png
```

### Screenshot 3: Game In Progress ⏱️

```
✅ What to capture:
   - Partially filled puzzle (30-50%)
   - Selected cell (highlighted blue)
   - Timer showing realistic time (e.g., 02:35)
   - Some mistakes made (1-2)

📸 How to capture:
   1. Continue playing from previous game
   2. Fill in 10-15 numbers
   3. Tap a cell to select it (shows highlighting)
   4. Wait for timer to show ~2-3 minutes
   5. Press Ctrl+S
   6. Save as: 03-game-in-progress.png
```

### Screenshot 4: Game Completed ✅

```
✅ What to capture:
   - Completed puzzle (all cells filled)
   - Success dialog showing "Congratulations!"
   - Completion time displayed

📸 How to capture:
   1. Complete the puzzle by filling all cells correctly
   2. Success dialog will pop up
   3. Press Ctrl+S while dialog is visible
   4. Save as: 04-game-completed.png

💡 Tip: If puzzle is too hard, use Easy difficulty
```

### Screenshot 5: Statistics Screen 📊

```
✅ What to capture:
   - Games Played count
   - Games Won count
   - Win Rate percentage
   - Best Times
   - Current Streak with 🔥

📸 How to capture:
   1. From home screen, tap "Statistics"
   2. Let screen fully load
   3. Press Ctrl+S
   4. Save as: 05-statistics.png

💡 Make sure you've played games first so stats show real data!
```

### Screenshot 6: Settings Screen ⚙️

```
✅ What to capture:
   - All toggle switches
   - Settings options
   - "Back to Home" button

📸 How to capture:
   1. From home screen, tap "Settings"
   2. Wait for screen to load
   3. Press Ctrl+S
   4. Save as: 06-settings.png
```

---

## After Capturing Screenshots

### 1. Locate Your Screenshots

**Windows:**
```
C:\Users\<YourUsername>\.android\screenshots\
```

**Mac:**
```
~/Desktop/
```

### 2. Move to Project Folder

```bash
# Create screenshots folder if it doesn't exist
mkdir d:/Projects/SudokuApp/assets/screenshots

# Copy your screenshots there
# Name them properly:
01-home-screen.png
02-game-starting.png
03-game-in-progress.png
04-game-completed.png
05-statistics.png
06-settings.png
```

### 3. Verify Quality

Open each screenshot and check:
- [ ] Resolution is 1080 x 1920 or higher
- [ ] Text is clear and readable
- [ ] No glitches or artifacts
- [ ] All UI elements visible
- [ ] Colors look good
- [ ] File size is reasonable (200KB - 2MB)

If any screenshot doesn't look good, **retake it!**

---

## Alternative Screenshot Methods

### Method 1: ADB Command (if Ctrl+S doesn't work)

```bash
# Take screenshot
adb shell screencap -p /sdcard/screenshot.png

# Pull to your computer
adb pull /sdcard/screenshot.png d:/Projects/SudokuApp/assets/screenshots/01-home-screen.png
```

### Method 2: Use Windows Snipping Tool

```
1. Press Windows + Shift + S
2. Select area of emulator screen
3. Save screenshot
```

### Method 3: Use Screen Recording Software

If screenshots aren't working:
1. Use OBS or similar to record screen
2. Take frames from the video
3. Export as images

---

## Troubleshooting

### Emulator won't start
```bash
# Check if emulator is already running
adb devices

# Kill all emulator processes
taskkill /F /IM qemu-system-x86_64.exe

# Restart emulator
```

### App won't install
```bash
# Clear Metro cache
npx expo start --clear

# Reinstall dependencies
rm -rf node_modules
npm install

# Try again
npm run android
```

### Screenshots are blurry
```
1. Use higher resolution emulator (1080p minimum)
2. In AVD Manager: Edit device → Advanced → Set Graphics to "Hardware"
3. Increase emulator screen size if too small
```

### Can't find camera icon
```
1. Look for 📷 icon in emulator toolbar (right side)
2. If not visible, use keyboard shortcut Ctrl+S
3. Or use adb command method above
```

---

## Final Checklist

Before moving to next step:

- [ ] 6 screenshots captured
- [ ] All screenshots are high quality
- [ ] Files named properly (01-06)
- [ ] Moved to `assets/screenshots/` folder
- [ ] Verified each screenshot looks good
- [ ] Statistics show real data (played games)
- [ ] Ready to move to icon creation

---

## What's Next?

After screenshots are done:

1. ✅ Create app icon (512x512)
   - Use `icon-template.html` in assets folder
   - Open in browser and download

2. ✅ Create feature graphic (1024x500)
   - Use `feature-graphic-template.html` in assets folder
   - Open in browser and download

3. ✅ Build the app
   - `eas build --platform android`

4. ✅ Submit to Play Store
   - Upload all assets
   - Fill in store listing
   - Submit for review

---

**Need Help?**

Common issues:
- Check [SCREENSHOT_GUIDE.md](SCREENSHOT_GUIDE.md) for detailed instructions
- Check [DESIGN_SPECS.md](DESIGN_SPECS.md) for asset specifications
- Check [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) for full deployment process
