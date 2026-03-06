# iOS Setup Guide (Mac required)

## Prerequisites

- macOS with Xcode 15+
- Android Studio (for building the shared KMP framework)
- CocoaPods: `sudo gem install cocoapods`

## Step 1: Build the Shared Framework

From the `SudokuKMP/` directory on Mac:

```bash
./gradlew :shared:assembleDebugXCFramework
# Or for device:
./gradlew :shared:linkDebugFrameworkIosArm64
```

The framework outputs to:
`shared/build/bin/iosArm64/debugFramework/Shared.framework`
(or `shared/build/XCFrameworks/debug/Shared.xcframework` for universal)

## Step 2: Create the Xcode Project

1. Open **Xcode → File → New → Project**
2. Choose **iOS → App**
3. Configure:
   - Product Name: `iosApp`
   - Bundle Identifier: `com.nextjedi.sudokustreak`
   - Interface: **SwiftUI**
   - Language: **Swift**
   - Save to: `SudokuKMP/iosApp/`

4. **Delete** the generated `ContentView.swift` and `<AppName>App.swift`
5. **Add existing files** → select all `.swift` files from:
   - `iosApp/iosApp/iOSApp.swift`
   - `iosApp/iosApp/ContentView.swift`
   - `iosApp/iosApp/Views/` (all files)
   - `iosApp/iosApp/ViewModels/` (all files)
   - `iosApp/iosApp/Components/` (all files)

## Step 3: Add the Shared KMP Framework

1. In Xcode, select your target → **General** tab
2. Scroll to **Frameworks, Libraries, and Embedded Content**
3. Click **+** → **Add Other** → **Add Files**
4. Navigate to: `shared/build/XCFrameworks/debug/Shared.xcframework`
5. Set to **Embed & Sign**

## Step 4: Configure Build Phase (for automatic framework rebuild)

Add a **New Run Script Phase** (before "Compile Sources"):

```bash
cd "$SRCROOT/../.."
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

## Step 5: Swift Type Bridging Notes

The `GameViewModel.swift` uses `Shared.SudokuCell`, `Shared.Difficulty`, etc.
These are Kotlin classes exported to Objective-C by the KMP framework.

Key type conversions:

- Kotlin `List<SudokuCell>` → Swift `[Any]` (cast to `[SudokuCell]`)
- Kotlin `Int` → Swift `Int32` (use `Int32(...)` when calling Kotlin methods)
- Kotlin `Boolean` → Swift `Bool` (direct)

If you see "module 'Shared' not found", ensure the framework is built and
the build phase script runs before compilation.

## Step 6: Run

Select an iOS Simulator → Press ▶ Run.
