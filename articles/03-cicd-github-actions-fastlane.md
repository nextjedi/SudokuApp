# Setting Up CI/CD for React Native: GitHub Actions + Fastlane Deep Dive

_Automating the entire build, test, and deploy pipeline so you never manually upload an AAB again_

---

## Introduction

After manually deploying my Sudoku app to the Play Store, I knew I never wanted to do it again. The process was:

1. Run local build
2. Wait 15 minutes
3. Open Play Console
4. Navigate through menus
5. Upload AAB
6. Fill in release notes
7. Submit for review

Every. Single. Time.

This article covers how I set up automated CI/CD using GitHub Actions and Fastlane — and all the gotchas I encountered along the way.

---

## Why Fastlane Over EAS?

Expo Application Services (EAS) is Expo's official build service. It's simpler but:

- **Costs money** for frequent builds
- **Less control** over the build process
- **Queue times** during peak hours
- **Vendor lock-in** to Expo's ecosystem

Fastlane, on the other hand:

- **Open source** and free
- **Runs anywhere** (local, CI/CD, self-hosted)
- **Full control** over every step
- **Industry standard** for mobile CI/CD

For a solo developer wanting maximum control without recurring costs, Fastlane was the clear choice.

---

## The Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     GitHub Repository                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Push to release/alpha  ──────►  GitHub Actions Triggered   │
│                                         │                    │
│                                         ▼                    │
│                              ┌─────────────────┐             │
│                              │  Ubuntu Runner  │             │
│                              └────────┬────────┘             │
│                                       │                      │
│                    ┌──────────────────┼──────────────────┐   │
│                    │                  │                  │   │
│                    ▼                  ▼                  ▼   │
│              Install deps      Decode Secrets      Setup JDK │
│                    │                  │                  │   │
│                    └──────────────────┼──────────────────┘   │
│                                       │                      │
│                                       ▼                      │
│                              ┌─────────────────┐             │
│                              │    Fastlane     │             │
│                              └────────┬────────┘             │
│                                       │                      │
│                    ┌──────────────────┼──────────────────┐   │
│                    ▼                  ▼                  ▼   │
│               Build APK/AAB    Sign with Key    Upload to GP │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## Step 1: Fastlane Setup

### Installing Fastlane

In the `android/` directory:

```bash
gem install fastlane -N
fastlane init
```

This creates:

```
android/
├── fastlane/
│   ├── Appfile
│   └── Fastfile
```

### The Appfile

```ruby
# android/fastlane/Appfile
json_key_file(ENV['PLAY_STORE_JSON_KEY_PATH'])
package_name("com.nextjedi.sudokustreak")
```

### The Fastfile

This is where the magic happens:

```ruby
# android/fastlane/Fastfile
default_platform(:android)

platform :android do

  desc "Build Android app for alpha testing"
  lane :build_alpha do
    gradle(
      task: "clean assembleRelease"
    )
  end

  desc "Build Android app bundle (AAB) for beta/production"
  lane :build_beta do
    gradle(
      task: "clean bundleRelease"
    )
  end

  desc "Deploy to Google Play Internal Testing Track"
  lane :deploy_alpha do
    build_alpha
    upload_to_play_store(
      track: 'internal',
      release_status: 'completed',
      apk: 'app/build/outputs/apk/release/app-release.apk',
      json_key_data: ENV['PLAY_STORE_JSON_KEY']
    )
  end

  desc "Deploy to Google Play Beta Track"
  lane :deploy_beta do
    build_beta
    upload_to_play_store(
      track: 'beta',
      release_status: 'completed',
      aab: 'app/build/outputs/bundle/release/app-release.aab',
      json_key_data: ENV['PLAY_STORE_JSON_KEY']
    )
  end

  desc "Deploy to Google Play Production"
  lane :deploy_production do
    build_beta
    upload_to_play_store(
      track: 'production',
      release_status: 'completed',
      aab: 'app/build/outputs/bundle/release/app-release.aab',
      json_key_data: ENV['PLAY_STORE_JSON_KEY']
    )
  end

end
```

---

## Step 2: GitHub Actions Workflow

### The Workflow File

```yaml
# .github/workflows/android-build.yml
name: Android Build & Deploy

on:
  workflow_dispatch:
    inputs:
      lane:
        description: "Fastlane lane to run"
        required: true
        type: choice
        options:
          - build_alpha
          - build_beta
          - deploy_alpha
          - deploy_beta
          - deploy_production

  push:
    branches:
      - "release/alpha"
      - "release/beta"
    tags:
      - "v*"

jobs:
  build:
    name: Android Build
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: "20"
          cache: "npm"

      - name: Setup Ruby
        uses: ruby/setup-ruby@v1
        with:
          ruby-version: "3.2"

      - name: Install Node dependencies
        run: npm ci --legacy-peer-deps

      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: "17"
          distribution: "temurin"
          cache: "gradle"

      - name: Cache Gradle packages
        uses: actions/cache@v3
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}

      - name: Decode Android Keystore
        run: |
          echo "${{ secrets.ANDROID_KEYSTORE_BASE64 }}" | base64 -d > android/app/release.keystore

      - name: Create keystore.properties
        run: |
          cat > android/keystore.properties << EOF
          storeFile=release.keystore
          storePassword=${{ secrets.ANDROID_KEYSTORE_PASSWORD }}
          keyAlias=${{ secrets.ANDROID_KEY_ALIAS }}
          keyPassword=${{ secrets.ANDROID_KEY_PASSWORD }}
          EOF

      - name: Install Fastlane
        run: |
          cd android
          gem install fastlane -N
          bundle init
          bundle add fastlane

      - name: Determine Fastlane lane
        id: lane
        run: |
          if [ "${{ github.event_name }}" == "workflow_dispatch" ]; then
            echo "lane=${{ github.event.inputs.lane }}" >> $GITHUB_OUTPUT
          elif [[ "${{ github.ref }}" == "refs/heads/release/alpha" ]]; then
            echo "lane=deploy_alpha" >> $GITHUB_OUTPUT
          elif [[ "${{ github.ref }}" == "refs/heads/release/beta" ]]; then
            echo "lane=deploy_beta" >> $GITHUB_OUTPUT
          elif [[ "${{ github.ref }}" == refs/tags/v* ]]; then
            echo "lane=deploy_production" >> $GITHUB_OUTPUT
          fi

      - name: Run Fastlane
        env:
          PLAY_STORE_JSON_KEY: ${{ secrets.PLAY_STORE_JSON_KEY }}
        run: |
          cd android
          bundle exec fastlane ${{ steps.lane.outputs.lane }}

      - name: Upload APK artifact
        if: contains(steps.lane.outputs.lane, 'alpha')
        uses: actions/upload-artifact@v4
        with:
          name: app-release-apk
          path: android/app/build/outputs/apk/release/app-release.apk

      - name: Upload AAB artifact
        if: contains(steps.lane.outputs.lane, 'beta') || contains(steps.lane.outputs.lane, 'production')
        uses: actions/upload-artifact@v4
        with:
          name: app-release-aab
          path: android/app/build/outputs/bundle/release/app-release.aab

      - name: Cleanup keystore
        if: always()
        run: |
          rm -f android/app/release.keystore
          rm -f android/keystore.properties
```

---

## Step 3: Secrets Configuration

### Required GitHub Secrets

| Secret Name                 | Description                  | How to Get               |
| --------------------------- | ---------------------------- | ------------------------ |
| `ANDROID_KEYSTORE_BASE64`   | Base64-encoded keystore file | `base64 -i keystore.jks` |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password            | From keystore creation   |
| `ANDROID_KEY_ALIAS`         | Key alias name               | From keystore creation   |
| `ANDROID_KEY_PASSWORD`      | Key password                 | From keystore creation   |
| `PLAY_STORE_JSON_KEY`       | Service account JSON         | Google Cloud Console     |

### Base64 Encoding the Keystore

```bash
# macOS/Linux
base64 -i android/app/sudoku-release.keystore -o keystore-base64.txt

# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("android\app\sudoku-release.keystore")) | Out-File keystore-base64.txt
```

Copy the entire contents of `keystore-base64.txt` into the GitHub secret.

### Google Play Service Account

This was the trickiest part. Google changed their UI, so the old "Settings > API Access" path no longer exists.

New process:

1. **Google Cloud Console** → Create service account
2. **Enable** Google Play Android Developer API
3. **Create JSON key** for the service account
4. **Google Play Console** → Users and permissions → Invite new user
5. Add service account email, grant release permissions
6. Copy JSON content to `PLAY_STORE_JSON_KEY` secret

---

## The Errors I Encountered

### Error 1: "Permission denied - gradlew"

```
Permission denied - /home/runner/work/SudokuApp/SudokuApp/android/gradlew
```

**Cause:** Git doesn't preserve executable permissions on Windows.

**Fix:** Update file permissions in Git:

```bash
git update-index --chmod=+x android/gradlew
git commit -m "fix: Add executable permission to gradlew"
```

### Error 2: "Couldn't find gradlew at path android/android/gradlew"

**Cause:** Double path — workflow `cd android` + Fastfile `project_dir: "android/"`.

**Fix:** Remove `project_dir` from Fastfile since we're already in android/:

```ruby
# Wrong
gradle(task: "assembleRelease", project_dir: "android/")

# Correct
gradle(task: "assembleRelease")
```

### Error 3: "Could not parse service account json"

**Cause:** `PLAY_STORE_JSON_KEY` secret not configured.

**Fix:** Add the complete JSON content to GitHub Secrets.

### Error 4: "undefined method 'json_key_data'"

**Cause:** Fastlane Appfile syntax issue.

**Fix:** Pass JSON key as environment variable, not file:

```ruby
upload_to_play_store(
  json_key_data: ENV['PLAY_STORE_JSON_KEY']  # Not json_key_file
)
```

---

## CI Workflow: Lint & Test

Separate from the build workflow, I have a CI workflow for every PR:

```yaml
# .github/workflows/ci.yml
name: CI - Lint, Type Check & Test

on:
  pull_request:
    branches: [master, "release/**"]
  push:
    branches: [master, "release/**"]

jobs:
  quality-checks:
    name: Code Quality & Tests
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: "20"
          cache: "npm"

      - run: npm ci --legacy-peer-deps

      - name: Run ESLint
        run: npm run lint

      - name: Run TypeScript type check
        run: npm run typecheck

      - name: Run unit tests
        run: npm test -- --coverage --passWithNoTests
```

---

## Branch Strategy

```
master (protected)
  │
  ├── feature/new-feature
  │     └── PR → master
  │
  ├── release/alpha (protected)
  │     └── Auto-deploys to Internal Testing
  │
  └── release/beta (protected)
        └── Auto-deploys to Beta Track
```

When I push to `release/alpha`:

1. CI runs (lint, typecheck, tests)
2. If CI passes, Android Build & Deploy runs
3. APK is built, signed, and uploaded to Play Store
4. Artifacts are saved for 30 days

---

## Smart Build Triggers: Path Filtering

One optimization I'm particularly proud of: **not every push triggers mobile builds**.

### The Problem

React Native apps often have web builds too (via Expo Web). But:

- Updating web styles shouldn't trigger a 15-minute Android build
- Editing documentation shouldn't waste CI minutes
- Changes to web-only files are irrelevant for mobile

Without path filtering, every push to `release/alpha` triggers both iOS and Android builds — even if you just fixed a typo in README.md.

### The Solution: paths and paths-ignore

GitHub Actions supports filtering workflows by which files changed:

```yaml
# .github/workflows/android-build.yml
on:
  push:
    branches:
      - "release/alpha"
      - "release/beta"
    tags:
      - "v*"
    paths:
      # Only trigger on changes that affect Android builds
      - "src/**"
      - "android/**"
      - "app.json"
      - "package.json"
      - "package-lock.json"
      - "tsconfig.json"
      - "babel.config.js"
      - "metro.config.js"
      - ".github/workflows/android-build.yml"
    paths-ignore:
      # Ignore web-only and documentation changes
      - "web/**"
      - "web-build/**"
      - "docs/**"
      - "*.md"
      - ".gitignore"
      - "articles/**"
```

### How It Works

| Files Changed               | Android Build? | iOS Build? |
| --------------------------- | -------------- | ---------- |
| `src/components/Button.tsx` | ✅ Yes         | ✅ Yes     |
| `android/app/build.gradle`  | ✅ Yes         | ❌ No      |
| `ios/Podfile`               | ❌ No          | ✅ Yes     |
| `web/index.html`            | ❌ No          | ❌ No      |
| `README.md`                 | ❌ No          | ❌ No      |
| `docs/setup.md`             | ❌ No          | ❌ No      |

### The Logic

**`paths`** = Only run if these files changed (whitelist)
**`paths-ignore`** = Never run if only these files changed (blacklist)

When both are specified:

1. First, check if changes match `paths`
2. Then, filter out anything in `paths-ignore`

### iOS Workflow Has Its Own Paths

The iOS workflow is similar but watches `ios/` instead of `android/`:

```yaml
# .github/workflows/ios-build.yml
on:
  push:
    branches:
      - "release/alpha"
      - "release/beta"
    paths:
      - "src/**"
      - "ios/**" # iOS-specific
      - "app.json"
      - "package.json"
      - "package-lock.json"
      - ".github/workflows/ios-build.yml"
    paths-ignore:
      - "web/**"
      - "docs/**"
      - "*.md"
      - "articles/**"
```

### Real-World Impact

Before path filtering:

```
Push README fix → Wait 15 min for Android → Wait 20 min for iOS
= 35 minutes wasted
```

After path filtering:

```
Push README fix → No builds triggered
= 0 minutes wasted
```

For a project with frequent documentation updates, this saves hours of CI time per month.

### When to Force a Build

Sometimes you DO want to trigger a build even without code changes (testing the pipeline, for example). Use **workflow_dispatch**:

```yaml
on:
  workflow_dispatch:  # Manual trigger always works
    inputs:
      lane:
        description: 'Fastlane lane to run'
        ...
```

Go to Actions → Select workflow → "Run workflow" → bypasses path filters.

### Pro Tips

1. **Include workflow file in paths**: Changes to the workflow itself should trigger it

   ```yaml
   paths:
     - ".github/workflows/android-build.yml"
   ```

2. **Test with a dry run**: Before relying on filters, verify they work

   ```bash
   # Push a web-only change, verify no mobile build
   echo "test" >> web/test.txt
   git add . && git commit -m "test: web only change"
   git push
   # Check Actions tab - should show no Android/iOS builds
   ```

3. **Be explicit**: List specific paths rather than using broad patterns. Easier to understand and debug.

---

## The Final Result

Now my deployment process is:

```bash
git checkout release/alpha
git merge feature/my-feature
git push
```

That's it. 15 minutes later, the new version is live on Play Store's internal testing track. No manual uploads, no clicking through menus, no remembering commands.

---

## Cost Analysis

| Service               | Cost                  |
| --------------------- | --------------------- |
| GitHub Actions        | Free (2000 min/month) |
| Fastlane              | Free (open source)    |
| Google Play Developer | $25 one-time          |
| **Total recurring**   | **$0/month**          |

Compare to EAS Build:

- Free tier: 30 builds/month
- Production: $99/month

For a solo developer shipping frequently, this self-hosted approach pays off quickly.

---

## Tips for Success

### 1. Start Simple

Begin with `build_alpha` only. Add deployment after builds work.

### 2. Test Locally First

```bash
cd android
bundle exec fastlane build_alpha
```

### 3. Use Workflow Dispatch

The `workflow_dispatch` trigger lets you manually run any lane from GitHub UI — invaluable for testing.

### 4. Check Artifacts

Always upload build artifacts. Even if deployment fails, you can download and manually upload the AAB.

### 5. Secure Your Secrets

- Never log secrets
- Use `if: always()` to clean up keystores
- Rotate keys periodically

---

## Conclusion

Setting up CI/CD took longer than I expected — about 2 days of debugging various issues. But now every release is automated, consistent, and reliable.

The combination of GitHub Actions (free CI/CD) + Fastlane (free build tool) + Google Play Console API is powerful and costs nothing beyond the initial $25 developer fee.

If you're a solo developer shipping mobile apps, invest the time to set this up. Your future self will thank you.

---

_Next article: The solo Claude developer experience — working with AI as your only teammate._

---

**Tags:** #CICD #GitHubActions #Fastlane #ReactNative #Android #DevOps #Automation #MobileDevelopment
