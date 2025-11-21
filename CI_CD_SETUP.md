# CI/CD Setup Guide for Sudoku Streak

This document describes the complete CI/CD pipeline setup for automated testing, building, and deployment of the Sudoku Streak React Native application using GitHub Actions and Expo EAS.

## 📋 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Branch Strategy](#branch-strategy)
- [Environment Profiles](#environment-profiles)
- [GitHub Secrets Setup](#github-secrets-setup)
- [Workflows](#workflows)
- [Pre-commit Hooks](#pre-commit-hooks)
- [Manual Deployment](#manual-deployment)
- [Branch Protection Rules](#branch-protection-rules)
- [Troubleshooting](#troubleshooting)

---

## 🏗️ Architecture Overview

### Workflow Pipeline

```
Developer Commit
    ↓
Pre-commit Hooks (Husky + lint-staged)
    ├─ ESLint --fix
    ├─ Prettier --write
    └─ Stage formatted files
    ↓
Push to Branch
    ↓
GitHub Actions CI
    ├─ Lint (ESLint)
    ├─ Type Check (TypeScript)
    └─ Unit Tests (Jest + RNTL)
    ↓
Merge to Release Branch / Tag
    ↓
EAS Build (Android/iOS)
    ├─ Build AAB/IPA
    └─ (Optional) Submit to Stores
```

---

## 🌿 Branch Strategy

### Branch Structure

| Branch          | Purpose                | Deployment Target       | Auto-Deploy     |
| --------------- | ---------------------- | ----------------------- | --------------- |
| `master`        | Production-ready code  | Production              | On Git tag `v*` |
| `release/beta`  | Beta testing           | TestFlight / Beta track | On push         |
| `release/alpha` | Alpha/Internal testing | Internal testing        | On push         |
| `feature/*`     | Feature development    | -                       | No              |
| `bugfix/*`      | Bug fixes              | -                       | No              |

### Workflow

1. **Development**: Create feature branches from `master`

   ```bash
   git checkout -b feature/my-feature master
   ```

2. **Alpha Release**: Merge to `release/alpha` for internal testing

   ```bash
   git checkout release/alpha
   git merge feature/my-feature
   git push origin release/alpha
   ```

3. **Beta Release**: Merge to `release/beta` for beta testing

   ```bash
   git checkout release/beta
   git merge release/alpha
   git push origin release/beta
   ```

4. **Production Release**: Tag master for production deployment
   ```bash
   git checkout master
   git merge release/beta
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin master --tags
   ```

---

## 🎯 Environment Profiles

### EAS Build Profiles

Configured in `eas.json`:

| Profile       | Build Type  | Channel    | Store Track | Use Case                       |
| ------------- | ----------- | ---------- | ----------- | ------------------------------ |
| `development` | Development | -          | -           | Local development with Expo Go |
| `alpha`       | APK / IPA   | alpha      | Internal    | Internal team testing          |
| `beta`        | AAB / IPA   | beta       | Beta        | External beta testers          |
| `production`  | AAB / IPA   | production | Production  | Public release                 |

### Build Characteristics

**Alpha (Internal)**

- Fast builds (APK for Android)
- Internal distribution only
- Google Play: Internal testing track
- iOS: TestFlight internal

**Beta (Public Testing)**

- Store-ready builds (AAB for Android)
- Public beta testing
- Google Play: Beta track
- iOS: TestFlight external

**Production**

- Optimized production builds
- Full store submission
- Google Play: Production track
- iOS: App Store

---

## 🔐 GitHub Secrets Setup

### Required Secrets

Navigate to **Settings → Secrets and variables → Actions** in your GitHub repository and add:

#### 1. EXPO_TOKEN (Required)

```bash
# Generate Expo access token
npx eas login
npx eas whoami
npx eas token:create

# Add the token to GitHub Secrets as EXPO_TOKEN
```

**Purpose**: Authenticates GitHub Actions with Expo EAS for automated builds

### Optional Secrets (for enhanced notifications)

- `SLACK_WEBHOOK_URL`: For Slack notifications on build failures
- `DISCORD_WEBHOOK_URL`: For Discord notifications

---

## 🔄 Workflows

### 1. CI Workflow (`ci.yml`)

**Triggers:**

- Pull requests to `master` or `release/**`
- Pushes to `master` or `release/**`

**Jobs:**

- ✅ ESLint code quality checks
- ✅ TypeScript type checking
- ✅ Jest unit tests with coverage
- ✅ Upload coverage to Codecov (optional)

**Features:**

- Node modules caching for faster runs
- Runs in parallel for speed
- Fails fast on quality issues

### 2. Android Build Workflow (`eas-build-android.yml`)

**Triggers:**

- Push to `release/alpha` → Builds alpha profile
- Push to `release/beta` → Builds beta profile
- Git tag `v*` → Builds production profile
- Manual dispatch with profile selection

**Jobs:**

- 📦 Build Android APK/AAB with EAS
- 🚀 (Optional) Submit to Google Play Store

**Configuration:**

```yaml
# Manual trigger
workflow_dispatch:
  profile: [alpha, beta, production]
  submit: true/false
```

### 3. iOS Build Workflow (`eas-build-ios.yml`)

**Triggers:**

- Push to `release/alpha` → Builds alpha profile
- Push to `release/beta` → Builds beta profile
- Git tag `v*` → Builds production profile
- Manual dispatch with profile selection

**Jobs:**

- 📦 Build iOS IPA with EAS
- 🚀 (Optional) Submit to App Store Connect

---

## 🪝 Pre-commit Hooks

Implemented with **Husky** and **lint-staged**.

### What Runs on Commit

1. **ESLint** with auto-fix
2. **Prettier** formatting
3. Automatically stages fixed files

### Configuration

Located in `package.json`:

```json
{
  "lint-staged": {
    "*.{ts,tsx,js,jsx}": ["eslint --fix", "prettier --write"],
    "*.{json,md}": ["prettier --write"]
  }
}
```

### Skip Hooks (Emergency Only)

```bash
git commit --no-verify -m "Emergency fix"
```

**⚠️ Warning**: Bypassing hooks may cause CI failure.

---

## 🚀 Manual Deployment

### Trigger Builds Manually

#### Via GitHub Actions UI

1. Go to **Actions** tab
2. Select workflow (Android or iOS)
3. Click **Run workflow**
4. Choose:
   - Branch: `master`, `release/alpha`, or `release/beta`
   - Profile: `alpha`, `beta`, or `production`
   - Submit: Check to auto-submit to stores

#### Via Command Line (Local)

```bash
# Login to EAS
npx eas login

# Build for specific profile
npx eas build --platform android --profile alpha
npx eas build --platform ios --profile beta
npx eas build --platform all --profile production

# Submit to stores
npx eas submit --platform android --profile production
npx eas submit --platform ios --profile production
```

---

## 🛡️ Branch Protection Rules

### Master Branch Protection

**Required Settings:**

1. Navigate to **Settings → Branches → Branch protection rules**
2. Add rule for `master`

**Recommended Configuration:**

```yaml
✅ Require pull request reviews before merging
   - Required approvals: 1
✅ Require status checks to pass before merging
   - Required checks:
     - Code Quality & Tests (CI workflow)
✅ Require branches to be up to date before merging
✅ Require linear history (optional)
✅ Include administrators (optional)
❌ Allow force pushes: Disabled
❌ Allow deletions: Disabled
```

### Release Branch Protection (`release/**`)

**Recommended Configuration:**

```yaml
✅ Require pull request reviews before merging
   - Required approvals: 1
✅ Require status checks to pass before merging
   - Required checks:
     - Code Quality & Tests (CI workflow)
✅ Require branches to be up to date before merging
❌ Allow force pushes: Disabled
❌ Allow deletions: Disabled
```

### Setting Up Branch Protection via GitHub UI

1. **Go to Repository Settings**
   - Navigate to your repository
   - Click **Settings** tab

2. **Access Branch Protection**
   - Click **Branches** in the left sidebar
   - Under "Branch protection rules", click **Add rule**

3. **Configure Master Branch**
   - Branch name pattern: `master`
   - Enable required settings above
   - Click **Create** or **Save changes**

4. **Configure Release Branches**
   - Branch name pattern: `release/**`
   - Enable required settings
   - Click **Create**

---

## 📊 Monitoring & Notifications

### GitHub Actions Status

View workflow runs:

- **Actions** tab in GitHub repository
- Status badges (add to README.md):

```markdown
![CI](https://github.com/YOUR_ORG/SudokuApp/workflows/CI%20-%20Lint,%20Type%20Check%20&%20Test/badge.svg)
![EAS Build Android](https://github.com/YOUR_ORG/SudokuApp/workflows/EAS%20Build%20-%20Android/badge.svg)
![EAS Build iOS](https://github.com/YOUR_ORG/SudokuApp/workflows/EAS%20Build%20-%20iOS/badge.svg)
```

### Build Monitoring

Check EAS build status:

```bash
npx eas build:list --platform all --limit 10
npx eas build:view [BUILD_ID]
```

---

## 🐛 Troubleshooting

### Common Issues

#### 1. **CI Workflow Fails on Lint**

**Cause**: Code doesn't meet ESLint standards

**Solution**:

```bash
npm run lint:fix
git add .
git commit -m "Fix linting issues"
```

#### 2. **EAS Build Fails: Authentication Error**

**Cause**: `EXPO_TOKEN` not configured or expired

**Solution**:

1. Generate new token: `npx eas token:create`
2. Update `EXPO_TOKEN` in GitHub Secrets
3. Re-run workflow

#### 3. **Pre-commit Hook Fails**

**Cause**: Linting or formatting errors

**Solution**:

```bash
npm run lint:fix
# Fix any remaining errors manually
git add .
git commit -m "Fix code quality issues"
```

#### 4. **Dependency Installation Fails**

**Cause**: Peer dependency conflicts

**Solution**:

```bash
# Locally
npm install --legacy-peer-deps

# Update workflow if needed (already configured)
```

#### 5. **Build Profile Not Found**

**Cause**: EAS profile doesn't exist in `eas.json`

**Solution**: Verify profile name matches `eas.json` configuration

---

## 🔄 Version Management

### Semantic Versioning

Follow [Semantic Versioning](https://semver.org/):

- **Major**: Breaking changes (1.0.0 → 2.0.0)
- **Minor**: New features, backwards compatible (1.0.0 → 1.1.0)
- **Patch**: Bug fixes (1.0.0 → 1.0.1)

### Update Version

1. **Update `package.json`**:

   ```json
   {
     "version": "1.2.0"
   }
   ```

2. **Update `app.json`**:

   ```json
   {
     "expo": {
       "version": "1.2.0",
       "ios": {
         "buildNumber": "12"
       },
       "android": {
         "versionCode": 12
       }
     }
   }
   ```

3. **Commit and tag**:
   ```bash
   git add package.json app.json
   git commit -m "Bump version to 1.2.0"
   git tag -a v1.2.0 -m "Release version 1.2.0"
   git push origin master --tags
   ```

---

## 📚 Additional Resources

- [Expo EAS Build Documentation](https://docs.expo.dev/build/introduction/)
- [Expo EAS Submit Documentation](https://docs.expo.dev/submit/introduction/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [React Native Testing Library](https://callstack.github.io/react-native-testing-library/)
- [ESLint Configuration](https://eslint.org/docs/user-guide/configuring/)

---

## 🎓 Quick Reference

### Common Commands

```bash
# Local development
npm start                           # Start Expo dev server
npm run android                     # Run on Android
npm run ios                         # Run on iOS

# Testing
npm run lint                        # Run ESLint
npm run lint:fix                    # Fix ESLint issues
npm run typecheck                   # TypeScript type check
npm test                            # Run Jest tests

# EAS Commands
npx eas build --platform android --profile alpha    # Build Android alpha
npx eas build --platform ios --profile beta         # Build iOS beta
npx eas submit --platform android --latest          # Submit latest Android build
npx eas build:list                                  # List recent builds
```

---

**Last Updated**: 2025-11-21
**Maintainer**: Sudoku Streak Team
