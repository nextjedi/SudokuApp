#!/bin/bash
# Android Keystore Generation Script
# This script generates a release keystore for Android app signing

echo "=================================="
echo "Android Keystore Generator"
echo "=================================="
echo ""
echo "This will generate a keystore for signing your Android app."
echo "⚠️  IMPORTANT: Store credentials securely - you cannot recover them!"
echo ""

# Configuration
KEYSTORE_PATH="android/app/sudoku-release.keystore"
KEY_ALIAS="sudoku-release-key"
KEY_ALG="RSA"
KEY_SIZE="2048"
VALIDITY="10000"
STORE_TYPE="PKCS12"

# Prompt for passwords
echo "Enter keystore password (min 6 characters):"
read -s KEYSTORE_PASSWORD
echo ""
echo "Confirm keystore password:"
read -s KEYSTORE_PASSWORD_CONFIRM
echo ""

if [ "$KEYSTORE_PASSWORD" != "$KEYSTORE_PASSWORD_CONFIRM" ]; then
    echo "❌ Passwords don't match!"
    exit 1
fi

echo "Enter key password (can be same as keystore password):"
read -s KEY_PASSWORD
echo ""
echo "Confirm key password:"
read -s KEY_PASSWORD_CONFIRM
echo ""

if [ "$KEY_PASSWORD" != "$KEY_PASSWORD_CONFIRM" ]; then
    echo "❌ Passwords don't match!"
    exit 1
fi

# Prompt for certificate details
echo "Enter your name or company name:"
read CN
echo "Enter organizational unit (e.g., Mobile Dev):"
read OU
echo "Enter organization name (e.g., Sudoku Streak):"
read O
echo "Enter city:"
read L
echo "Enter state:"
read ST
echo "Enter country code (e.g., US):"
read C

echo ""
echo "🔐 Generating keystore..."
echo ""

# Generate keystore
keytool -genkeypair \
    -v \
    -storetype $STORE_TYPE \
    -keystore $KEYSTORE_PATH \
    -alias $KEY_ALIAS \
    -keyalg $KEY_ALG \
    -keysize $KEY_SIZE \
    -validity $VALIDITY \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "CN=$CN, OU=$OU, O=$O, L=$L, ST=$ST, C=$C"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Keystore generated successfully!"
    echo ""
    echo "📍 Location: $KEYSTORE_PATH"
    echo "🔑 Alias: $KEY_ALIAS"
    echo ""
    echo "⚠️  SAVE THESE CREDENTIALS SECURELY:"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Keystore Password: $KEYSTORE_PASSWORD"
    echo "Key Alias: $KEY_ALIAS"
    echo "Key Password: $KEY_PASSWORD"
    echo "Keystore Path: $KEYSTORE_PATH"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "📝 Copy these credentials to a secure location (password manager)"
    echo ""
    echo "Next steps:"
    echo "1. Add credentials to GitHub Secrets"
    echo "2. Never commit the keystore file to git"
    echo "3. Keep a backup in a secure location"
else
    echo ""
    echo "❌ Failed to generate keystore"
    exit 1
fi
