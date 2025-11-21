# The Android & Play Store Deployment Journey: A Solo Developer's Tale of Persistence

_Every error message, every cryptic failure, and how I shipped anyway_

---

## Introduction

Building an app is one thing. Getting it into users' hands through the Google Play Store is an entirely different beast. What I thought would be a straightforward "build and upload" process turned into a multi-day adventure through keystores, Gradle configurations, signing issues, and Google's ever-changing console UI.

This is the unfiltered story of deploying **Sudoku Streak** to the Google Play Store as a solo developer, with Claude Code as my AI assistant.

---

## The Naive Beginning

I finished the app. It worked perfectly in the Expo Go development app. I thought:

> "Now I just need to build it and upload to Play Store. Should take an hour, maybe two."

**Narrator: It did not take an hour.**

---

## Phase 1: Ejecting from Expo Managed Workflow

### The First Hurdle

My app was built with Expo managed workflow, which is great for development but has limitations for production:

- Limited native module access
- Expo-specific build system (EAS)
- Less control over native configuration

I decided to "prebuild" (eject to bare workflow) for more control:

```bash
npx expo prebuild
```

### What This Created

```
android/
├── app/
│   ├── src/
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── gradle.properties
├── gradlew
└── settings.gradle
```

A complete Android native project. Now I had to learn Android development whether I wanted to or not.

---

## Phase 2: The Keystore Saga

### Understanding Android Signing

Every Android app must be signed with a cryptographic key before it can be installed. This isn't optional — it's how Android verifies app authenticity and updates.

**The critical part:** Once you publish an app with a keystore, you can NEVER change it. Lose the keystore = lose the ability to update your app.

### Generating the Keystore

```bash
keytool -genkeypair -v -storetype PKCS12 \
  -keystore android/app/sudoku-release.keystore \
  -alias sudoku-release-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

I was prompted for:

- Keystore password
- Key password
- Name, organization, location info

**First Lesson Learned:** Write these down IMMEDIATELY. Store them in a password manager. Back them up.

### Configuring Gradle

Here's where the fun began. I needed to tell Gradle how to use the keystore:

```gradle
// android/app/build.gradle
signingConfigs {
    release {
        def keystorePropertiesFile = rootProject.file("keystore.properties")
        if (keystorePropertiesFile.exists()) {
            def keystoreProperties = new Properties()
            keystoreProperties.load(new FileInputStream(keystorePropertiesFile))

            storeFile file(keystoreProperties['storeFile'])
            storePassword keystoreProperties['storePassword']
            keyAlias keystoreProperties['keyAlias']
            keyPassword keystoreProperties['keyPassword']
        }
    }
}
```

### The Path Problem (Error #1)

My first build failed with:

```
Keystore file 'D:\Projects\SudokuApp\android\app\app\sudoku-release.keystore' not found
```

Notice `app\app\` — a double path! The configuration was prepending "app/" to a path that already included it.

**Fix:** Remove the redundant prefix in `keystore.properties`:

```properties
# Wrong
storeFile=app/sudoku-release.keystore

# Correct
storeFile=sudoku-release.keystore
```

This took two hours to debug.

---

## Phase 3: Building the Release

### The First Successful Build

```bash
cd android
./gradlew assembleRelease
```

After 15 minutes of downloading dependencies and compiling:

```
BUILD SUCCESSFUL in 6m 23s
```

I had an APK! `android/app/build/outputs/apk/release/app-release.apk` — 65 MB of mobile game goodness.

### But Wait, There's More: AAB

Google Play Store now requires Android App Bundles (AAB), not APKs:

```bash
./gradlew bundleRelease
```

This creates `app-release.aab` — a more efficient format that lets Google optimize the download for each device.

---

## Phase 4: Google Play Console Setup

### Creating a Developer Account

Google requires a one-time $25 fee for a developer account. After payment, I was greeted by the Play Console dashboard — a maze of menus, policies, and requirements.

### Creating the App Listing

Required information:

- App name
- Short description (80 chars)
- Full description (4000 chars)
- Screenshots (phone, tablet, various sizes)
- Feature graphic (1024x500)
- App icon (512x512)
- Privacy policy URL
- App category
- Content rating questionnaire
- Target audience
- Data safety declarations

**This took an entire day.** Not the app — just the listing metadata.

### The First Upload

I uploaded my AAB to the internal testing track. Google processed it, scanned for malware, and... accepted it!

First milestone achieved.

---

## Phase 5: The Real Challenges

### Challenge 1: Screenshots

Play Store requires screenshots in specific dimensions:

- Phone: 16:9 or 9:16 aspect ratio
- Minimum 320px, maximum 3840px per side
- 2-8 screenshots required

I used Android emulator to capture them:

```bash
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png
```

### Challenge 2: Privacy Policy

Even for a simple offline game, Google requires a privacy policy. I created one covering:

- No data collection (it's offline!)
- No third-party services
- GDPR compliance statement
- Children's privacy (COPPA compliance)

Hosted it on GitHub Pages for free.

### Challenge 3: Content Rating

Google has a questionnaire about your app's content:

- Violence? (No)
- Sexual content? (No)
- Language? (No)
- User-generated content? (No)
- In-app purchases? (No)

My Sudoku game got an "Everyone" rating.

### Challenge 4: Data Safety

New Play Store requirement — declare what data your app collects:

- Personal info? No
- Financial info? No
- Location? No
- Photos? No
- Contacts? No

For an offline Sudoku game, I checked "No data collected." Sweet.

---

## Phase 6: Testing Tracks

Google Play has multiple release tracks:

```
Internal Testing → Closed Testing → Open Testing → Production
```

### Internal Testing

- Up to 100 testers
- Immediate availability (no review)
- Good for quick iteration

### Closed Testing

- Invite-only testers
- Goes through review
- 1-3 day approval time

### Open Testing

- Anyone can join via link
- Full review process
- Pre-production validation

I started with Internal Testing to verify the upload worked, then moved to Closed Testing for real feedback.

---

## The Errors I Encountered (And Solutions)

### Error: "Your app is not compliant with Google Play policies"

**Cause:** Missing privacy policy
**Solution:** Add privacy policy URL in app listing AND in the app itself

### Error: "APK signature scheme v2 required"

**Cause:** Old signing method
**Solution:** Build with latest Gradle (handled automatically with bundleRelease)

### Error: "Version code already exists"

**Cause:** Uploading same version twice
**Solution:** Increment `versionCode` in `build.gradle`:

```gradle
defaultConfig {
    versionCode 2  // Was 1
    versionName "1.0.1"
}
```

### Error: "Your app targets API level 30, which is below the minimum"

**Cause:** Google keeps increasing minimum SDK requirements
**Solution:** Update `targetSdkVersion` to 34 in `build.gradle`

---

## Tools That Helped

### Claude Code

For every error, I'd paste the message and get:

1. Explanation of what went wrong
2. The exact fix needed
3. Prevention tips for the future

### adb (Android Debug Bridge)

```bash
# Install APK for testing
adb install app-release.apk

# View logs
adb logcat | grep "SudokuStreak"

# Capture screenshots
adb shell screencap -p /sdcard/screen.png
```

### Gradle Commands

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Build release AAB
./gradlew bundleRelease

# See signing report
./gradlew signingReport
```

---

## Timeline: Reality vs Expectation

| Task                   | Expected    | Actual        |
| ---------------------- | ----------- | ------------- |
| Generate keystore      | 10 min      | 30 min        |
| Configure Gradle       | 30 min      | 3 hours       |
| First successful build | 1 hour      | 6 hours       |
| Play Console setup     | 1 hour      | 4 hours       |
| Screenshots & assets   | 30 min      | 3 hours       |
| Privacy policy         | 15 min      | 2 hours       |
| First internal release | 30 min      | 2 hours       |
| **Total**              | **4 hours** | **20+ hours** |

---

## Lessons Learned

### 1. Keystore Management is Critical

- Back it up immediately
- Use a password manager
- Never commit to git
- Store multiple copies in secure locations

### 2. Start Play Console Setup Early

- Create listing while app is in development
- Gather screenshots as you build features
- Write descriptions before final polish

### 3. Read Google's Documentation

- Requirements change frequently
- Policy violations can get your app removed
- Better to over-prepare than under-prepare

### 4. Test on Real Devices

- Emulators miss real-world issues
- Different Android versions behave differently
- Ask friends to beta test

### 5. Plan for Iteration

- First release won't be perfect
- Set up CI/CD early (see my next article)
- Make updating easy

---

## The Victory

After all the struggles, seeing "Your app is live on Google Play" was incredibly satisfying. A few hundred lines of code I wrote (with Claude's help) was now installable by anyone in the world.

The journey from `npm start` to Play Store taught me more about Android development than any tutorial could. Every error was a learning opportunity, and having Claude to explain the "why" behind each fix accelerated my understanding.

---

## What's Next

The manual deployment process was painful. Next, I set up CI/CD with GitHub Actions and Fastlane to automate the entire build and release pipeline. That's a story of its own — coming up next.

---

_Next article: Setting up CI/CD for React Native with GitHub Actions and Fastlane — automating the pain away._

---

**Tags:** #Android #PlayStore #ReactNative #MobileDevelopment #AppDeployment #GooglePlay #IndieGameDev
