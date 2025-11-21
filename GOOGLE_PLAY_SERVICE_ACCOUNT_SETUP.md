# Google Play Service Account Setup Guide

## Overview

This guide explains how to set up a Google Play service account for automated app deployments via Fastlane and GitHub Actions.

> **Note**: As of 2024, Google removed the "Settings/API Access" section from Play Console. Service accounts are now added via "Users and permissions".

---

## Step 1: Create Google Cloud Project & Enable API

1. Go to **[Google Cloud Console](https://console.cloud.google.com/)**
2. Create a new project or select an existing one
3. Navigate to **APIs & Services → Library**
4. Search for **"Google Play Android Developer API"**
5. Click **Enable**

---

## Step 2: Create Service Account

1. In Google Cloud Console, go to **APIs & Services → Credentials**
2. Click **"+ CREATE CREDENTIALS"** → **"Service account"**
3. Fill in:
   - **Service account name**: `play-store-upload`
   - **Service account ID**: `play-store-upload` (auto-filled)
   - **Description**: `Service account for automated Play Store uploads`
4. Click **Create and Continue**
5. Skip the optional "Grant access" steps → Click **Done**

---

## Step 3: Create JSON Key

1. In the **Service Accounts** list, click on `play-store-upload`
2. Go to the **"Keys"** tab
3. Click **"Add Key"** → **"Create new key"**
4. Select **JSON** → Click **"Create"**
5. **Save the downloaded JSON file** securely (e.g., `play-store-key.json`)

> **Important**: This JSON file contains sensitive credentials. Never commit it to version control.

---

## Step 4: Invite Service Account to Google Play Console

1. Go to **[Google Play Console](https://play.google.com/console/)**
2. In the left sidebar, click **"Users and permissions"**
3. Click **"Invite new users"**
4. **Email address**: Paste the service account email
   - Format: `play-store-upload@YOUR-PROJECT-ID.iam.gserviceaccount.com`
   - Find this in Google Cloud Console → IAM & Admin → Service Accounts
5. Set **Account permissions**:
   - Can be left as default (no account-level permissions needed)
6. Under **"App permissions"**:
   - Click **"Add app"**
   - Select your app (e.g., Sudoku Streak)
   - Grant permission: **"Release apps to production, exclude devices, and use Play App Signing"**
7. Click **"Invite"**

> **Note**: Permissions may take up to 24 hours to propagate, though usually it's immediate.

---

## Step 5: Add JSON Key to GitHub Secrets

1. Open the downloaded JSON file in a text editor
2. Copy the **entire contents**
3. Go to your GitHub repository
4. Navigate to **Settings → Secrets and variables → Actions**
5. Click **"New repository secret"**
6. Fill in:
   - **Name**: `PLAY_STORE_JSON_KEY`
   - **Value**: Paste the entire JSON content
7. Click **"Add secret"**

---

## Verification

After setup, your GitHub Actions workflow can use the service account to:

- Upload APKs/AABs to Play Store
- Manage releases on internal, alpha, beta, and production tracks
- Update app listings and metadata

---

## Troubleshooting

### Error: "Could not parse service account json"

- Ensure the entire JSON content was copied (including curly braces)
- Check for any extra whitespace or line breaks

### Error: "403 Forbidden"

- Verify the service account email was invited in Play Console
- Check that app permissions were granted correctly
- Wait 24 hours for permissions to propagate

### Error: "The current user has insufficient permissions"

- Ensure "Release apps to production" permission is granted
- Verify you selected the correct app in App permissions

---

## Security Best Practices

1. **Never commit** the JSON key file to version control
2. **Rotate keys** periodically (delete old keys, create new ones)
3. **Use minimal permissions** - only grant what's needed
4. **Monitor usage** in Google Cloud Console → IAM & Admin → Service Accounts

---

## Related Documentation

- [Google Play Developer API - Getting Started](https://developers.google.com/android-publisher/getting_started)
- [Fastlane Supply Documentation](https://docs.fastlane.tools/actions/supply/)
- [GitHub Actions Secrets](https://docs.github.com/en/actions/security-guides/encrypted-secrets)

---

**Created**: 2025-11-21
**Last Updated**: 2025-11-21
