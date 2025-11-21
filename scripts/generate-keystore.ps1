# Android Keystore Generation Script (PowerShell)
# This script generates a release keystore for Android app signing

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Android Keystore Generator" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "This will generate a keystore for signing your Android app." -ForegroundColor Yellow
Write-Host "⚠️  IMPORTANT: Store credentials securely - you cannot recover them!" -ForegroundColor Red
Write-Host ""

# Configuration
$KEYSTORE_PATH = "android/app/sudoku-release.keystore"
$KEY_ALIAS = "sudoku-release-key"
$KEY_ALG = "RSA"
$KEY_SIZE = "2048"
$VALIDITY = "10000"
$STORE_TYPE = "PKCS12"

# Function to read secure password
function Read-SecurePassword {
    param([string]$Prompt)
    $securePassword = Read-Host -Prompt $Prompt -AsSecureString
    $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $password = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
    [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR)
    return $password
}

# Prompt for passwords
$KEYSTORE_PASSWORD = Read-SecurePassword "Enter keystore password (min 6 characters)"
$KEYSTORE_PASSWORD_CONFIRM = Read-SecurePassword "Confirm keystore password"

if ($KEYSTORE_PASSWORD -ne $KEYSTORE_PASSWORD_CONFIRM) {
    Write-Host "❌ Passwords don't match!" -ForegroundColor Red
    exit 1
}

$KEY_PASSWORD = Read-SecurePassword "Enter key password (can be same as keystore password)"
$KEY_PASSWORD_CONFIRM = Read-SecurePassword "Confirm key password"

if ($KEY_PASSWORD -ne $KEY_PASSWORD_CONFIRM) {
    Write-Host "❌ Passwords don't match!" -ForegroundColor Red
    exit 1
}

# Prompt for certificate details
$CN = Read-Host "Enter your name or company name"
$OU = Read-Host "Enter organizational unit (e.g., Mobile Dev)"
$O = Read-Host "Enter organization name (e.g., Sudoku Streak)"
$L = Read-Host "Enter city"
$ST = Read-Host "Enter state"
$C = Read-Host "Enter country code (e.g., US)"

Write-Host ""
Write-Host "🔐 Generating keystore..." -ForegroundColor Yellow
Write-Host ""

# Build dname
$DNAME = "CN=$CN, OU=$OU, O=$O, L=$L, ST=$ST, C=$C"

# Generate keystore
$keytoolArgs = @(
    "-genkeypair",
    "-v",
    "-storetype", $STORE_TYPE,
    "-keystore", $KEYSTORE_PATH,
    "-alias", $KEY_ALIAS,
    "-keyalg", $KEY_ALG,
    "-keysize", $KEY_SIZE,
    "-validity", $VALIDITY,
    "-storepass", $KEYSTORE_PASSWORD,
    "-keypass", $KEY_PASSWORD,
    "-dname", $DNAME
)

try {
    & keytool $keytoolArgs

    Write-Host ""
    Write-Host "✅ Keystore generated successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📍 Location: $KEYSTORE_PATH" -ForegroundColor Cyan
    Write-Host "🔑 Alias: $KEY_ALIAS" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "⚠️  SAVE THESE CREDENTIALS SECURELY:" -ForegroundColor Red
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
    Write-Host "Keystore Password: $KEYSTORE_PASSWORD" -ForegroundColor White
    Write-Host "Key Alias: $KEY_ALIAS" -ForegroundColor White
    Write-Host "Key Password: $KEY_PASSWORD" -ForegroundColor White
    Write-Host "Keystore Path: $KEYSTORE_PATH" -ForegroundColor White
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
    Write-Host ""
    Write-Host "📝 Copy these credentials to a secure location (password manager)" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "1. Add credentials to GitHub Secrets"
    Write-Host "2. Never commit the keystore file to git"
    Write-Host "3. Keep a backup in a secure location"
}
catch {
    Write-Host ""
    Write-Host "❌ Failed to generate keystore: $_" -ForegroundColor Red
    exit 1
}
