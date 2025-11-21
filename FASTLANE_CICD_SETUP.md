# Fastlane CI/CD Setup Guide

Complete CI/CD pipeline for React Native using **Fastlane** (open-source) and **GitHub Actions**.

## 📋 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Prerequisites](#prerequisites)
- [Android Setup](#android-setup)
- [iOS Setup](#ios-setup)
- [GitHub Secrets Configuration](#github-secrets-configuration)
- [GitHub Actions Workflows](#github-actions-workflows)
- [Branch Strategy](#branch-strategy)
- [Version Management](#version-management)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)

---

## 🏗️ Architecture Overview

### Technology Stack

- **Build Automation**: Fastlane (Ruby-based, open-source)
- **CI/CD Platform**: GitHub Actions
- **Version Control**: Git with branch-based deployment
- **Code Quality**: ESLint, TypeScript, Jest, Husky
- **Signing Management**:
  - Android: Base64-encoded keystores in GitHub Secrets
  - iOS: Fastlane Match with private Git repository

### Pipeline Flow

```
Developer Commit
    ↓
Pre-commit Hooks (Lint + Format)
    ↓
Push to Branch
    ↓
GitHub Actions CI
    ├─ Lint
    ├─ Type Check
    └─ Unit Tests
    ↓
Merge to Release Branch / Tag
    ↓
Fastlane Build & Deploy
    ├─ Android: Gradle + signing → Play Store
    └─ iOS: Xcode + Match → TestFlight/App Store
```

---

## 📦 Prerequisites

### Local Development

```bash
# Node.js & npm
node --version  # v20+
npm --version

# Ruby (for Fastlane)
ruby --version  # 3.0+
gem --version

# Android Studio + JDK 17
java --version

# Xcode (macOS only)
xcodebuild -version  # 15+
```

### Install Fastlane Locally

```bash
# Install Fastlane
gem install fastlane -N

# Or via Homebrew (macOS)
brew install fastlane
```

---

## 🤖 Android Setup

### 1. Generate Release Keystore

```bash
cd android/app

keytool -genkeypair \
  -v \
  -storetype PKCS12 \
  -keystore release.keystore \
  -alias sudoku-release-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass YOUR_KEYSTORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=Sudoku Streak, OU=Mobile, O=Your Company, L=City, ST=State, C=US"
```

**⚠️ IMPORTANT:**

- Store keystore password securely
- Never commit `release.keystore` to Git (already in .gitignore)
- Backup keystore file safely

### 2. Base64 Encode Keystore for GitHub Secrets

```bash
# On macOS/Linux
base64 -i android/app/release.keystore -o keystore.base64.txt

# On Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("android\app\release.keystore")) | Out-File keystore.base64.txt
```

### 3. Configure Android Signing in build.gradle

The keystore.properties file will be dynamically created by GitHub Actions.

Add to `android/app/build.gradle`:

```gradle
android {
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

    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### 4. Create Google Play Service Account

1. Go to [Google Play Console](https://play.google.com/console)
2. **Setup → API access**
3. **Create new service account**
4. Grant permissions: **Release to production, testing tracks**
5. Download JSON key file

---

## 🍎 iOS Setup

### 1. Create Private Git Repository for Certificates

```bash
# Create a new PRIVATE repository on GitHub
# Name: sudoku-certificates (or similar)
```

### 2. Initialize Fastlane Match

```bash
cd ios
fastlane match init
```

When prompted:

- Storage mode: **git**
- Git URL: `git@github.com:YOUR_ORG/sudoku-certificates.git`

### 3. Generate Certificates and Profiles

```bash
# Generate App Store certificates (first time)
fastlane match appstore \
  --app_identifier "com.sudokustreak.app" \
  --readonly false
```

This will:

- Generate distribution certificate
- Create App Store provisioning profile
- Encrypt and store in private Git repo

### 4. Create App Store Connect API Key

1. Go to [App Store Connect](https://appstoreconnect.apple.com)
2. **Users and Access → Keys**
3. Generate new API key with **Admin** access
4. Download `.p8` file
5. Note: **Key ID** and **Issuer ID**

```bash
# Base64 encode the P8 file
base64 -i AuthKey_XXXXXXXXXX.p8 -o appstore-key.base64.txt
```

---

## 🔐 GitHub Secrets Configuration

Navigate to **Settings → Secrets and variables → Actions** and add:

### Android Secrets

| Secret Name                 | Description                     | How to Get                              |
| --------------------------- | ------------------------------- | --------------------------------------- |
| `ANDROID_KEYSTORE_BASE64`   | Base64-encoded release keystore | `base64 -i release.keystore`            |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password               | Password you set when creating keystore |
| `ANDROID_KEY_ALIAS`         | Key alias                       | Alias from keytool command              |
| `ANDROID_KEY_PASSWORD`      | Key password                    | Key password from keytool               |
| `PLAY_STORE_JSON_KEY`       | Service account JSON            | Contents of downloaded JSON file        |

### iOS Secrets

| Secret Name                      | Description                   | How to Get                                             |
| -------------------------------- | ----------------------------- | ------------------------------------------------------ |
| `MATCH_GIT_URL`                  | Private certificates repo URL | `git@github.com:YOUR_ORG/certificates.git`             |
| `MATCH_GIT_PRIVATE_KEY`          | SSH private key (Base64)      | Generate SSH key, add public to GitHub, encode private |
| `MATCH_PASSWORD`                 | Encryption password           | Password set during match init                         |
| `APPLE_ID`                       | Apple Developer email         | Your Apple ID                                          |
| `TEAM_ID`                        | Developer Team ID             | From Apple Developer portal                            |
| `APP_IDENTIFIER`                 | Bundle ID                     | `com.sudokustreak.app`                                 |
| `APP_STORE_CONNECT_KEY_ID`       | API Key ID                    | From App Store Connect                                 |
| `APP_STORE_CONNECT_ISSUER_ID`    | Issuer ID                     | From App Store Connect                                 |
| `APP_STORE_CONNECT_KEY_CONTENT`  | API Key content (Base64)      | `base64 -i AuthKey_XXX.p8`                             |
| `PROVISIONING_PROFILE_SPECIFIER` | Profile name                  | From Apple Developer portal                            |

### Generate SSH Key for Match

```bash
# Generate SSH key pair
ssh-keygen -t ed25519 -C "github-actions-match" -f match_key

# Add public key to GitHub certificates repo
# Settings → Deploy keys → Add deploy key
cat match_key.pub

# Base64 encode private key for GitHub Secret
base64 -i match_key > match_key.base64.txt
cat match_key.base64.txt
# Copy this content to MATCH_GIT_PRIVATE_KEY secret
```

---

## 🔄 GitHub Actions Workflows

### 1. CI Workflow (`.github/workflows/ci.yml`)

**Triggers:**

- Every PR to `master` or `release/**`
- Every push to `master` or `release/**`

**Jobs:**

- ✅ ESLint
- ✅ TypeScript type check
- ✅ Jest unit tests
- ✅ Coverage upload (Codecov)

### 2. Android Build (`.github/workflows/android-build.yml`)

**Triggers:**

- Push to `release/alpha` → `deploy_alpha`
- Push to `release/beta` → `deploy_beta`
- Git tag `v*` → `deploy_production`
- Manual dispatch with lane selection

**Fastlane Lanes:**

- `build_alpha`: APK for internal testing
- `build_beta`: AAB for beta testing
- `build_production`: AAB for production
- `deploy_*`: Build + upload to Play Store

### 3. iOS Build (`.github/workflows/ios-build.yml`)

**Triggers:**

- Push to `release/alpha` → `deploy_alpha`
- Push to `release/beta` → `deploy_beta`
- Git tag `v*` → `deploy_production`
- Manual dispatch with lane selection

**Fastlane Lanes:**

- `build_alpha`: IPA for TestFlight internal
- `build_beta`: IPA for TestFlight external
- `build_production`: IPA for App Store
- `deploy_*`: Build + upload to TestFlight/App Store

---

## 🌿 Branch Strategy

| Branch          | Purpose          | Auto-Deploy  | Track                          |
| --------------- | ---------------- | ------------ | ------------------------------ |
| `master`        | Production       | Git tag `v*` | Production                     |
| `release/beta`  | Beta testing     | On push      | Beta / TestFlight External     |
| `release/alpha` | Internal testing | On push      | Internal / TestFlight Internal |
| `feature/*`     | Development      | No           | -                              |

### Deployment Flow

```bash
# 1. Alpha (Internal Testing)
git checkout release/alpha
git merge feature/my-feature
git push origin release/alpha
# → Auto-deploys to Internal track

# 2. Beta (Public Testing)
git checkout release/beta
git merge release/alpha
git push origin release/beta
# → Auto-deploys to Beta track

# 3. Production
git checkout master
git merge release/beta
npm run version:patch  # or minor/major
git add package.json app.json
git commit -m "chore: bump version to X.Y.Z"
git tag -a vX.Y.Z -m "Release X.Y.Z"
git push origin master --tags
# → Auto-deploys to Production
```

---

## 📦 Version Management

### Automated Version Bumping

```bash
# Bump patch version (1.0.0 → 1.0.1)
npm run version:patch

# Bump minor version (1.0.0 → 1.1.0)
npm run version:minor

# Bump major version (1.0.0 → 2.0.0)
npm run version:major
```

This script synchronizes version across:

- `package.json`
- `app.json` (Expo config)
- Android `versionCode`
- iOS `buildNumber`

### Manual Version Update

Edit both files:

**package.json:**

```json
{
  "version": "1.2.0"
}
```

**app.json:**

```json
{
  "expo": {
    "version": "1.2.0",
    "ios": { "buildNumber": "12" },
    "android": { "versionCode": 12 }
  }
}
```

---

## 🚀 Deployment

### Manual Deployment via GitHub Actions

1. Go to **Actions** tab
2. Select workflow (**Android Build** or **iOS Build**)
3. Click **Run workflow**
4. Select:
   - Branch: `release/alpha`, `release/beta`, or `master`
   - Lane: Choose desired Fastlane lane
5. Click **Run workflow**

### Local Deployment (Testing)

```bash
# Android
cd android
bundle exec fastlane build_alpha
bundle exec fastlane deploy_beta

# iOS (macOS only)
cd ios
bundle exec fastlane build_alpha
bundle exec fastlane deploy_beta
```

---

## 🐛 Troubleshooting

### Android Issues

#### Build fails: Keystore not found

**Solution:** Verify `ANDROID_KEYSTORE_BASE64` secret is set correctly

#### Gradle build fails

**Solution:**

```bash
cd android
./gradlew clean
./gradlew bundleRelease --stacktrace
```

#### Upload to Play Store fails

**Solution:** Verify service account has correct permissions

### iOS Issues

#### Match fails: Authentication error

**Solution:**

- Verify SSH key is added to certificates repo
- Check `MATCH_GIT_PRIVATE_KEY` is correctly Base64 encoded

#### Code signing error

**Solution:**

```bash
cd ios
bundle exec fastlane match appstore --readonly false --force_for_new_devices
```

#### Upload to TestFlight fails

**Solution:** Verify App Store Connect API key has Admin access

### General Issues

#### CI fails on lint

**Solution:**

```bash
npm run lint:fix
git add .
git commit --amend
```

#### Version mismatch

**Solution:**

```bash
npm run version:patch
git add package.json app.json
git commit -m "fix: sync versions"
```

---

## 📚 Additional Resources

- [Fastlane Documentation](https://docs.fastlane.tools/)
- [Fastlane Match Guide](https://docs.fastlane.tools/actions/match/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [React Native Docs](https://reactnative.dev/)

---

## 🎓 Quick Reference

### Fastlane Commands

```bash
# Android
cd android
bundle exec fastlane build_alpha       # Build APK
bundle exec fastlane build_beta        # Build AAB
bundle exec fastlane deploy_production # Deploy to Production

# iOS
cd ios
bundle exec fastlane setup_signing     # Sync certificates
bundle exec fastlane build_alpha       # Build for TestFlight
bundle exec fastlane deploy_beta       # Deploy to Beta
bundle exec fastlane test              # Run tests
```

### Git Commands

```bash
# Version bump and tag
npm run version:patch
git add package.json app.json
git commit -m "chore: bump version to 1.0.1"
git tag -a v1.0.1 -m "Release 1.0.1"
git push origin master --tags

# Branch management
git checkout -b release/alpha master
git push -u origin release/alpha
```

---

**Last Updated:** 2025-11-21
**Pipeline Status:** ✅ Fastlane + GitHub Actions
**Maintainer:** Sudoku Streak Team
