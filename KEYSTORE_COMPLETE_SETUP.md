# Complete Android Keystore Setup Guide

## ✅ Checklist Overview

- [ ] Step 1: Generate keystore
- [ ] Step 2: Test keystore locally
- [ ] Step 3: Base64 encode for CI/CD
- [ ] Step 4: Add GitHub Secrets
- [ ] Step 5: Test build

---

## 📦 Step 1: Generate Keystore

### Run this command:

```bash
keytool -genkeypair -v -storetype PKCS12 -keystore android/app/sudoku-release.keystore -alias sudoku-release-key -keyalg RSA -keysize 2048 -validity 10000
```

### You'll be prompted for:

1. **Keystore password**: (min 6 characters) - **SAVE THIS!**
2. **Key password**: (press Enter to use same password) - **SAVE THIS!**
3. **Certificate details**:
   - Your name: `Your Name` or `Company Name`
   - Organizational unit: `Mobile Development`
   - Organization: `Sudoku Streak`
   - City: `Your City`
   - State: `Your State`
   - Country code: `US` (or your country)
4. Confirm with: `yes`

### ⚠️ CRITICAL: Save These Credentials Now!

```
Keystore Path: android/app/sudoku-release.keystore
Keystore Password: [YOUR_PASSWORD_HERE]
Key Alias: sudoku-release-key
Key Password: [YOUR_KEY_PASSWORD_HERE]
```

**Save to password manager immediately!**

---

## 🧪 Step 2: Test Keystore Locally

### Create `android/keystore.properties`:

```bash
# Copy the example file
cp android/keystore.properties.example android/keystore.properties
```

### Edit `android/keystore.properties`:

```properties
storeFile=app/sudoku-release.keystore
storePassword=YOUR_ACTUAL_KEYSTORE_PASSWORD
keyAlias=sudoku-release-key
keyPassword=YOUR_ACTUAL_KEY_PASSWORD
```

**Replace with your actual passwords!**

### Test local build:

```bash
cd android
./gradlew assembleRelease

# Check the output
ls -lh app/build/outputs/apk/release/app-release.apk
```

If successful, you'll see an APK file ~20-50 MB.

---

## 🔐 Step 3: Base64 Encode Keystore for GitHub

### On Windows (PowerShell):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("android\app\sudoku-release.keystore")) | Out-File keystore-base64.txt
```

### On macOS/Linux:

```bash
base64 -i android/app/sudoku-release.keystore -o keystore-base64.txt
```

### On Git Bash (Windows):

```bash
base64 android/app/sudoku-release.keystore > keystore-base64.txt
```

This creates `keystore-base64.txt` with your encoded keystore.

---

## 🔑 Step 4: Add GitHub Secrets

### Navigate to GitHub:

**Settings → Secrets and variables → Actions → New repository secret**

### Add These 4 Secrets:

| Secret Name                 | Value                             | Where to Find            |
| --------------------------- | --------------------------------- | ------------------------ |
| `ANDROID_KEYSTORE_BASE64`   | Contents of `keystore-base64.txt` | Open file, copy all text |
| `ANDROID_KEYSTORE_PASSWORD` | Your keystore password            | From Step 1              |
| `ANDROID_KEY_ALIAS`         | `sudoku-release-key`              | Fixed value              |
| `ANDROID_KEY_PASSWORD`      | Your key password                 | From Step 1              |

### How to Add Each Secret:

1. Click **"New repository secret"**
2. Name: `ANDROID_KEYSTORE_BASE64`
3. Value: Paste **entire contents** of `keystore-base64.txt`
4. Click **"Add secret"**
5. Repeat for other 3 secrets

---

## 🚀 Step 5: Test GitHub Actions Build

### Trigger build:

```bash
# Push to alpha branch
git checkout release/alpha
git merge ci-cd-setup
git push origin release/alpha
```

### Monitor build:

1. Go to **Actions** tab on GitHub
2. Watch **"Android Build & Deploy"** workflow
3. Check for success ✅

### Manual test (optional):

**Actions → Android Build & Deploy → Run workflow**

- Branch: `release/alpha`
- Lane: `build_alpha`
- Click **Run**

---

## 🎯 Quick Reference Commands

```bash
# Verify keystore
keytool -list -v -keystore android/app/sudoku-release.keystore

# Test local Android build
cd android && ./gradlew assembleRelease

# Test local Android bundle (AAB)
cd android && ./gradlew bundleRelease

# Test with Fastlane
cd android && bundle exec fastlane build_alpha

# Check APK signature
jarsigner -verify -verbose -certs android/app/build/outputs/apk/release/app-release.apk
```

---

## 🛡️ Security Checklist

- [x] Keystore stored in password manager
- [x] `android/keystore.properties` added to .gitignore (already done)
- [x] `android/app/sudoku-release.keystore` added to .gitignore (already done)
- [x] Keystore backed up in secure location
- [x] GitHub Secrets configured
- [ ] Delete `keystore-base64.txt` after adding to GitHub
- [ ] Delete `android/keystore.properties` before committing (or ensure gitignored)

---

## ⚠️ If You Lose the Keystore

**You CANNOT update your app on Google Play Store!**

You would need to:

1. Create new app listing (new package name)
2. Lose all existing users/reviews/installs
3. Cannot migrate existing users

**Backup your keystore now:**

- Save to encrypted cloud storage
- Save to password manager (some support file attachments)
- Keep offline encrypted backup

---

## 🐛 Troubleshooting

### Build fails: "Keystore was tampered with"

**Solution:** Password is incorrect. Double-check your password.

### Build fails: "storeFile not found"

**Solution:** Ensure path in `keystore.properties` is correct: `app/sudoku-release.keystore`

### GitHub Action fails: "Invalid keystore format"

**Solution:** Re-encode the keystore to base64, ensure entire file is copied.

### Build succeeds but APK won't install

**Solution:** Clear app data, uninstall previous version, then reinstall.

---

## 📝 Next Steps After Setup

1. ✅ Test local build
2. ✅ Add secrets to GitHub
3. ✅ Test CI/CD pipeline
4. Create Google Play Console account
5. Generate service account JSON for auto-deployment
6. Update Fastlane Appfile with package name
7. Deploy to internal testing track

---

## 📚 Additional Resources

- [Android App Signing](https://developer.android.com/studio/publish/app-signing)
- [Fastlane Android Documentation](https://docs.fastlane.tools/getting-started/android/setup/)
- [Google Play Console](https://play.google.com/console)

---

**Created:** 2025-11-21
**Status:** Ready for use
**Pipeline:** Fastlane + GitHub Actions
