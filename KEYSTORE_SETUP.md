# Android Keystore Setup Guide

## Step-by-Step Instructions

### 1. Generate the Keystore

Open **PowerShell** or **Git Bash** and run:

```bash
keytool -genkeypair ^
    -v ^
    -storetype PKCS12 ^
    -keystore android/app/sudoku-release.keystore ^
    -alias sudoku-release-key ^
    -keyalg RSA ^
    -keysize 2048 ^
    -validity 10000
```

**You will be prompted for:**

1. **Keystore password**: Create a strong password (min 6 characters)
   - Example: `MySecurePass123!`
   - **SAVE THIS PASSWORD!**

2. **Key password**: Can be the same as keystore password
   - Press Enter to use same password
   - **SAVE THIS PASSWORD!**

3. **Certificate information**:

   ```
   What is your first and last name?
     [Your Name or Company]

   What is the name of your organizational unit?
     [Mobile Development]

   What is the name of your organization?
     [Sudoku Streak or Your Company]

   What is the name of your City or Locality?
     [Your City]

   What is the name of your State or Province?
     [Your State]

   What is the two-letter country code for this unit?
     [US, UK, CA, etc.]
   ```

4. Type **yes** to confirm the information

### 2. Verify Keystore Created

```bash
ls -l android/app/sudoku-release.keystore
```

You should see a file around 2-4 KB.

### 3. Save Your Credentials

**⚠️ CRITICAL: Store these in a password manager immediately!**

```
Keystore Path: android/app/sudoku-release.keystore
Keystore Password: [YOUR_PASSWORD]
Key Alias: sudoku-release-key
Key Password: [YOUR_KEY_PASSWORD]
```

### 4. Verify Keystore Details

```bash
keytool -list -v -keystore android/app/sudoku-release.keystore -alias sudoku-release-key
```

Enter your keystore password when prompted.

---

## Next Steps

After generating the keystore, you need to:

1. **Base64 encode it for GitHub Secrets**
2. **Configure build.gradle**
3. **Add secrets to GitHub**

I'll help you with these steps next!

---

## ⚠️ Security Reminders

- ✅ **DO**: Store in password manager
- ✅ **DO**: Keep a backup in secure location
- ✅ **DO**: Add to .gitignore (already done)
- ❌ **DON'T**: Commit to Git
- ❌ **DON'T**: Share publicly
- ❌ **DON'T**: Email or store in plain text

**If you lose this keystore, you cannot update your app on Play Store!**
