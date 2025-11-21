# Sudoku Streak - Design Specifications

## App Icon (512x512 for Play Store)

### Concept
A modern, minimalist Sudoku grid with a fire/streak element

### Design Elements:
1. **Background**: Gradient from #4A90E2 (blue) to #6AB4F7 (lighter blue)
2. **Grid**: White/light 3x3 Sudoku grid outline (simplified)
3. **Numbers**: Large "9" or "1-9" in white, centered
4. **Streak Icon**: Fire emoji 🔥 or flame icon in top-right corner
5. **Style**: Flat design, high contrast, recognizable at small sizes

### Specifications:
- Size: 512x512 pixels (Play Store)
- Also need: 1024x1024 pixels (iOS/Expo)
- Format: PNG with transparent background (or solid color)
- Color Space: RGB
- File Name: icon.png

### Design Tool Recommendations:
- **Figma** (Free): https://figma.com
  1. Create 512x512 canvas
  2. Add gradient background
  3. Draw 3x3 grid with white lines
  4. Add number "9" in center
  5. Add fire emoji or icon
  6. Export as PNG

- **Canva** (Free): https://canva.com
  1. Custom size: 512x512
  2. Use "App Icon" template
  3. Customize with Sudoku theme
  4. Add fire element for streak

---

## Feature Graphic (1024x500 for Play Store)

### Concept
Horizontal banner showcasing app features

### Layout (Left to Right):
1. **Left 30%**: App icon or Sudoku grid visual
2. **Center 40%**: App name "Sudoku Streak" + Fire emoji
3. **Right 30%**: Key feature icons (timer, stats, puzzle piece)

### Design Elements:
- **Background**: Gradient #4A90E2 to #6AB4F7 or white with subtle pattern
- **Typography**:
  - Title: "Sudoku Streak" - Bold, 72-96pt
  - Subtitle: "Build Your Daily Puzzle Habit" - 36-48pt
- **Icons**: Timer ⏱️, Stats 📊, Fire 🔥
- **Style**: Clean, modern, professional

### Specifications:
- Size: 1024x500 pixels
- Format: PNG or JPEG
- File Name: feature-graphic.png

---

## Screenshots (1080x1920 or 1080x2340)

### Required Screenshots (6 minimum):

#### 1. Home Screen
- Shows: "Sudoku Streak" title, difficulty buttons, streak counter
- Highlight: Daily streak with fire emoji

#### 2. Game Screen - Starting
- Shows: Fresh Sudoku grid with pre-filled numbers
- Highlight: Clean interface, number pad visible

#### 3. Game Screen - In Progress
- Shows: Partially completed puzzle
- Highlight: Cell highlighting, timer running

#### 4. Game Screen - Completed
- Shows: Success dialog "Congratulations!"
- Highlight: Win message, time completed

#### 5. Statistics Screen
- Shows: Games played, win rate, best times, streak
- Highlight: Achievement progress

#### 6. Settings Screen
- Shows: Customization toggles
- Highlight: User control options

---

## Color Palette

### Primary Colors:
- **Blue Primary**: #4A90E2
- **Blue Light**: #6AB4F7
- **Blue Dark**: #2E5C8A

### Secondary Colors:
- **Success Green**: #4CAF50
- **Error Red**: #F44336
- **Warning Orange**: #FF9800

### Neutral Colors:
- **Background**: #F8F9FA
- **Text Dark**: #212121
- **Text Light**: #757575
- **Border**: #E0E0E0

---

## Typography

### Fonts:
- **Primary**: System default (San Francisco on iOS, Roboto on Android)
- **Fallback**: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto

### Sizes:
- **Title**: 32-48pt
- **Heading**: 24-32pt
- **Body**: 16-20pt
- **Caption**: 12-14pt

---

## How to Create Assets

### Option 1: Figma (Recommended)
1. Go to https://figma.com (free account)
2. Create new design file
3. Set canvas sizes as specified
4. Use design specs above
5. Export as PNG

### Option 2: Canva
1. Go to https://canva.com (free account)
2. Use "Custom Size" option
3. Create designs based on specs
4. Download as PNG

### Option 3: Online Icon Generator
1. https://appicon.co (upload base design)
2. https://easyappicon.com (generates all sizes)
3. Upload a 1024x1024 base icon

### Option 4: Hire Designer
- Fiverr: $5-$50 for icon + feature graphic
- Upwork: Professional designers
- 99designs: Contest-based design

---

## Files to Create

```
assets/
├── icon.png (1024x1024 for Expo/iOS)
├── icon-512.png (512x512 for Play Store)
├── adaptive-icon.png (1024x1024 for Android)
├── splash.png (1242x2436)
├── feature-graphic.png (1024x500 for Play Store)
└── screenshots/
    ├── 01-home-screen.png
    ├── 02-game-starting.png
    ├── 03-game-in-progress.png
    ├── 04-game-completed.png
    ├── 05-statistics.png
    └── 06-settings.png
```

---

## Next Steps

1. Create icon using Figma/Canva
2. Create feature graphic
3. Run app on emulator and capture screenshots
4. Place all files in `/assets/` folder
5. Update `app.json` to reference new assets
6. Build with EAS
