# Sudoku Streak - Assets Folder

This folder contains all assets and templates needed for Google Play Store deployment.

---

## 📁 Folder Structure

```
assets/
├── README.md (this file)
├── DESIGN_SPECS.md (design specifications)
├── SCREENSHOT_GUIDE.md (detailed screenshot instructions)
├── capture-screenshots.md (quick start guide)
├── icon-template.html (interactive icon template)
├── feature-graphic-template.html (interactive feature graphic template)
│
├── icon.png (1024x1024) - TO BE CREATED
├── icon-512.png (512x512) - TO BE CREATED
├── adaptive-icon.png (1024x1024) - TO BE CREATED
├── splash.png (1242x2436) - TO BE CREATED
├── feature-graphic.png (1024x500) - TO BE CREATED
│
└── screenshots/ - TO BE CREATED
    ├── 01-home-screen.png
    ├── 02-game-starting.png
    ├── 03-game-in-progress.png
    ├── 04-game-completed.png
    ├── 05-statistics.png
    └── 06-settings.png
```

---

## 🚀 Quick Start

### Step 1: Create App Icon

**Option A: Use the HTML template**
```bash
# Open in browser
start icon-template.html

# Click download buttons for different sizes
# OR right-click and "Save Image As"
```

**Option B: Use Figma/Canva**
- Read [DESIGN_SPECS.md](DESIGN_SPECS.md) for design guidelines
- Create 512x512 icon following specifications
- Export as PNG

### Step 2: Create Feature Graphic

**Option A: Use the HTML template**
```bash
# Open in browser
start feature-graphic-template.html

# Click download button
# Save as: feature-graphic.png
```

**Option B: Use Figma/Canva**
- Create 1024x500 graphic
- Follow design specs in DESIGN_SPECS.md
- Export as PNG

### Step 3: Capture Screenshots

**Quick method:**
```bash
# 1. Start Android emulator
# 2. Run app: npm run android
# 3. Follow capture-screenshots.md guide
# 4. Press Ctrl+S to capture each screen
# 5. Move files to assets/screenshots/
```

**Detailed instructions:**
- See [capture-screenshots.md](capture-screenshots.md)
- See [SCREENSHOT_GUIDE.md](SCREENSHOT_GUIDE.md)

---

## ✅ Required Assets Checklist

### For Google Play Store:

#### Essential Assets:
- [ ] **App Icon** (512x512 PNG)
- [ ] **Feature Graphic** (1024x500 PNG)
- [ ] **Screenshots** (minimum 2, recommended 6)
  - [ ] 01-home-screen.png
  - [ ] 02-game-starting.png
  - [ ] 03-game-in-progress.png
  - [ ] 04-game-completed.png
  - [ ] 05-statistics.png
  - [ ] 06-settings.png

#### For Expo/App:
- [ ] **icon.png** (1024x1024) - Main app icon
- [ ] **adaptive-icon.png** (1024x1024) - Android adaptive icon
- [ ] **splash.png** (1242x2436) - Splash screen

---

## 📐 Asset Specifications

### App Icon
- **Size**: 512x512 (Play Store) or 1024x1024 (iOS/Expo)
- **Format**: PNG
- **Background**: Can be transparent or solid color
- **Content**: Sudoku grid + fire emoji/icon
- **Style**: Modern, flat, high contrast

### Feature Graphic
- **Size**: 1024x500 pixels
- **Format**: PNG or JPEG
- **Content**: App name + key features + visual elements
- **Style**: Professional, eye-catching banner

### Screenshots
- **Size**: 1080 x 1920 (or 1080 x 2340 for modern phones)
- **Format**: PNG
- **Quantity**: Minimum 2, recommended 6-8
- **Content**: Different app screens showing features

---

## 🎨 Design Resources

### Design Tools:
- **Figma** (Free): https://figma.com
- **Canva** (Free): https://canva.com
- **GIMP** (Free): https://gimp.org
- **Photoshop** (Paid)

### Icon Generators:
- https://appicon.co
- https://easyappicon.com
- https://makeappicon.com

### Mockup Tools:
- https://mockuphone.com (add device frames to screenshots)
- https://smartmockups.com
- https://previewed.app

### Hire Designers:
- **Fiverr**: $5-$50 for icon + graphics
- **Upwork**: Professional designers
- **99designs**: Design contests

---

## 🎯 Design Guidelines

### App Icon Best Practices:
1. **Simple and recognizable** - Works at small sizes
2. **High contrast** - Stands out in app stores
3. **No text** - App name shown separately
4. **Unique** - Distinguishable from competitors
5. **Brand colors** - Use #4A90E2 (blue) as primary

### Feature Graphic Best Practices:
1. **Clear app name** - Large, readable text
2. **Key features** - Highlight 3-4 main features
3. **Professional look** - High-quality design
4. **Eye-catching** - Grabs attention in store
5. **Consistent branding** - Matches app icon colors

### Screenshot Best Practices:
1. **Tell a story** - Show user journey through app
2. **High quality** - Crisp, clear images
3. **Real data** - Play games first for realistic stats
4. **Consistent device** - Use same emulator for all
5. **Highlight features** - Each screenshot shows key feature

---

## 📝 File Naming Convention

Use this naming convention:

```
Icon files:
- icon.png (1024x1024 for Expo)
- icon-512.png (512x512 for Play Store)
- adaptive-icon.png (1024x1024 for Android)

Graphics:
- feature-graphic.png (1024x500)
- splash.png (1242x2436)

Screenshots:
- 01-home-screen.png
- 02-game-starting.png
- 03-game-in-progress.png
- 04-game-completed.png
- 05-statistics.png
- 06-settings.png
```

---

## 🔧 Troubleshooting

### Icon looks blurry:
- Ensure you're exporting at full resolution (512x512 or 1024x1024)
- Save as PNG, not JPEG
- Don't scale up a smaller image

### Screenshots are low quality:
- Use high-res emulator (1080p minimum)
- Enable "Hardware" graphics in AVD Manager
- Don't compress when saving

### Can't capture screenshots:
- Try Ctrl+S (Windows) or Cmd+S (Mac)
- Use adb command: `adb shell screencap -p /sdcard/screen.png`
- Use Windows Snipping Tool as backup

### Templates won't open:
- Open .html files in Chrome or Firefox browser
- If download doesn't work, right-click image and "Save As"

---

## 📚 Documentation

- **Design Specs**: [DESIGN_SPECS.md](DESIGN_SPECS.md)
- **Screenshot Guide**: [SCREENSHOT_GUIDE.md](SCREENSHOT_GUIDE.md)
- **Quick Start**: [capture-screenshots.md](capture-screenshots.md)
- **Deployment**: [../DEPLOYMENT_GUIDE.md](../DEPLOYMENT_GUIDE.md)

---

## 🎉 After Creating Assets

Once all assets are created:

1. ✅ Verify all files are in correct format and size
2. ✅ Check quality of each asset
3. ✅ Update `app.json` to reference new icons
4. ✅ Build app with: `eas build --platform android`
5. ✅ Upload assets to Google Play Console
6. ✅ Submit app for review

---

## 📞 Need Help?

- Check individual guide files in this folder
- Read full [DEPLOYMENT_GUIDE.md](../DEPLOYMENT_GUIDE.md)
- Review [PRODUCTION_READY.md](../PRODUCTION_READY.md)

**Good luck with your app launch! 🚀**
