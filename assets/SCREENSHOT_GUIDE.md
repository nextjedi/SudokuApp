# Screenshot Capture Guide - Sudoku Streak

## Required Screenshots for Google Play Store

**Minimum**: 2 screenshots
**Recommended**: 6-8 screenshots
**Size**: 1080 x 1920 (or 1080 x 2340 for modern phones)

---

## Step-by-Step Screenshot Process

### 1. Setup Android Emulator

#### Open Android Studio AVD Manager
```bash
# Option 1: Through Android Studio
# Tools > Device Manager > Create Device

# Option 2: Command line
emulator -list-avds
```

#### Recommended Emulator Settings:
- **Device**: Pixel 6 or Pixel 7
- **Resolution**: 1080 x 2340 (FHD+)
- **Graphics**: Hardware - GLES 2.0
- **Enable Device Frame**: Yes (for marketing shots)

### 2. Start Your App

```bash
cd d:/Projects/SudokuApp

# Install dependencies
npm install

# Start the app on Android
npm run android
```

Wait for the app to load on the emulator.

---

## Screenshots to Capture

### Screenshot 1: Home Screen 🏠

**What to show:**
- "Sudoku Streak" title at top
- Three difficulty buttons (Easy/Medium/Hard)
- Streak counter (if you have games played)
- "New Game" button
- "Statistics" and "Settings" buttons

**How to capture:**
1. Open the app to home screen
2. Ensure UI is fully loaded
3. Press `Ctrl+S` (Windows) or `Cmd+S` (Mac)
4. Or click camera icon in emulator toolbar
5. Save as: `01-home-screen.png`

**Tips:**
- If streak is 0, play a game first to show streak indicator
- Make sure all buttons are visible
- Check that text is crisp and clear

---

### Screenshot 2: Game Screen - Starting 🎮

**What to show:**
- Fresh Sudoku puzzle with some pre-filled numbers
- Number pad at bottom (1-9 + Erase)
- Timer at top (showing 00:00 or just started)
- Mistake counter showing 0/3
- Clean grid with clear borders

**How to capture:**
1. From home screen, tap "Easy" difficulty
2. Tap "New Game" button
3. Wait for game screen to load
4. Don't tap anything yet - capture the fresh puzzle
5. Press `Ctrl+S` to capture
6. Save as: `02-game-starting.png`

**Tips:**
- The grid should be clear and readable
- Pre-filled numbers visible in gray
- All UI elements (timer, mistakes) visible

---

### Screenshot 3: Game Screen - In Progress ⏱️

**What to show:**
- Partially filled puzzle (about 30-50% complete)
- A cell selected (highlighted in blue)
- Related cells highlighted (row/column/box)
- Timer showing some time (e.g., 02:35)
- Maybe 1 mistake made

**How to capture:**
1. Continue from previous game or start new one
2. Fill in 10-15 numbers
3. Tap a cell to select it (shows highlighting)
4. Wait a moment so timer shows realistic time
5. Press `Ctrl+S` to capture
6. Save as: `03-game-in-progress.png`

**Tips:**
- Select a cell before capturing to show the highlighting feature
- Try to show a partially filled grid that looks interesting
- Make sure highlighting is visible

---

### Screenshot 4: Game Screen - Completed ✅

**What to show:**
- Completed puzzle (all cells filled correctly)
- Success dialog/alert showing "Congratulations!"
- Completion time displayed
- "OK" or "Continue" button on dialog

**How to capture:**
1. Complete a puzzle by filling all cells correctly
2. Wait for success dialog to appear
3. Capture immediately
4. Save as: `04-game-completed.png`

**Tips:**
- If it's hard to complete a full puzzle, you can:
  - Use the Easy difficulty
  - Refer to solution in the generator code
- Make sure the success message is fully visible
- Grid should show all numbers filled in

---

### Screenshot 5: Statistics Screen 📊

**What to show:**
- Games Played count
- Games Won count
- Win Rate percentage
- Best Times for each difficulty
- Current Streak (with fire emoji if > 0)
- Average Completion Time

**How to capture:**
1. From home screen, tap "Statistics" button
2. Let the stats screen fully load
3. Ensure all statistics are visible
4. Press `Ctrl+S` to capture
5. Save as: `05-statistics.png`

**Tips:**
- Play 3-5 games first so stats look realistic
- Complete at least one game to show meaningful data
- Make sure streak is visible (even if 0)

---

### Screenshot 6: Settings Screen ⚙️

**What to show:**
- "Settings" title
- Toggle switches for:
  - Sound Effects
  - Cell Highlighting
  - Show Timer
  - Dark Mode (if implemented)
- "Reset Statistics" button
- "Back to Home" button

**How to capture:**
1. From home screen, tap "Settings" button
2. Let settings screen load completely
3. Ensure all toggles and buttons visible
4. Press `Ctrl+S` to capture
5. Save as: `06-settings.png`

**Tips:**
- All toggles should be clearly visible
- Show some toggles ON and some OFF for variety
- Make sure text is readable

---

## Optional Screenshots (Recommended)

### Screenshot 7: Game Screen - Mistake Alert ⚠️
**Shows**: Error message when entering invalid number

### Screenshot 8: Game Over Screen 💀
**Shows**: Game over when 3 mistakes made

---

## Where Screenshots Are Saved

### Windows:
```
C:\Users\<YourUsername>\.android\screenshots\
```

### Mac:
```
~/Desktop/
```

### Linux:
```
~/Pictures/
```

---

## After Capturing Screenshots

### 1. Organize Files
```bash
# Create screenshots folder
mkdir -p d:/Projects/SudokuApp/assets/screenshots

# Move/copy screenshots to project
# Place all .png files in assets/screenshots/
```

### 2. Verify Screenshot Quality

Check each screenshot:
- [ ] Resolution is 1080 x 1920 or higher
- [ ] Text is clear and readable
- [ ] No strange artifacts or glitches
- [ ] UI elements properly visible
- [ ] Colors look good
- [ ] File size reasonable (200KB - 2MB each)

### 3. Optional: Edit Screenshots

If needed, use tools to enhance:
- **Remove device frame** (if you want clean shots)
- **Add device mockup** (for marketing, use mockuphone.com)
- **Crop/resize** if needed
- **Adjust brightness/contrast** slightly

### 4. Name Files Properly

```
01-home-screen.png
02-game-starting.png
03-game-in-progress.png
04-game-completed.png
05-statistics.png
06-settings.png
```

---

## Alternative: Capture via ADB Command

```bash
# Take screenshot via command line
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png d:/Projects/SudokuApp/assets/screenshots/

# Or use adb exec-out
adb exec-out screencap -p > screenshot.png
```

---

## Tips for Best Screenshots

1. **Use Clean UI**: No error states or loading indicators
2. **Show Real Data**: Play some games first for realistic stats
3. **Good Timing**: Capture when animations are complete
4. **Consistent Style**: Use same emulator for all shots
5. **Tell a Story**: Screenshots should flow (home → game → win → stats)
6. **Highlight Features**: Each screenshot should showcase a key feature

---

## Quick Capture Checklist

- [ ] Android emulator running (Pixel 6/7, 1080x2340)
- [ ] App installed and running
- [ ] Played 2-3 games (for realistic stats)
- [ ] Screenshot 1: Home Screen captured
- [ ] Screenshot 2: Game Starting captured
- [ ] Screenshot 3: Game In Progress captured
- [ ] Screenshot 4: Game Completed captured
- [ ] Screenshot 5: Statistics captured
- [ ] Screenshot 6: Settings captured
- [ ] All screenshots saved in assets/screenshots/
- [ ] Quality verified (resolution, clarity)
- [ ] Files properly named

---

## Next Steps After Screenshots

1. ✅ Move screenshots to `assets/screenshots/` folder
2. ✅ Review quality and retake if needed
3. ✅ Create feature graphic (1024x500)
4. ✅ Create app icon (512x512)
5. ✅ Upload all to Google Play Console when ready

---

**Need help?**
- Check if emulator screenshot feature works: Click camera icon
- Try keyboard shortcuts: Ctrl+S (Windows), Cmd+S (Mac)
- Use adb command as backup method
