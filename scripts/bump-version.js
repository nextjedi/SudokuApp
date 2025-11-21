#!/usr/bin/env node
/**
 * Version Bump Script
 * Synchronizes version across package.json, app.json, Android and iOS
 *
 * Usage: node scripts/bump-version.js [major|minor|patch]
 */

/* eslint-disable @typescript-eslint/no-require-imports */
const fs = require("fs");
const path = require("path");

const VERSION_TYPE = process.argv[2] || "patch";
const VALID_TYPES = ["major", "minor", "patch"];

if (!VALID_TYPES.includes(VERSION_TYPE)) {
  console.error(`❌ Invalid version type: ${VERSION_TYPE}`);
  console.error(`   Must be one of: ${VALID_TYPES.join(", ")}`);
  process.exit(1);
}

// Read current versions
const packageJsonPath = path.join(__dirname, "..", "package.json");
const appJsonPath = path.join(__dirname, "..", "app.json");

const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, "utf8"));
const appJson = JSON.parse(fs.readFileSync(appJsonPath, "utf8"));

const currentVersion = packageJson.version;
const [major, minor, patch] = currentVersion.split(".").map(Number);

// Calculate new version
let newVersion;
switch (VERSION_TYPE) {
  case "major":
    newVersion = `${major + 1}.0.0`;
    break;
  case "minor":
    newVersion = `${major}.${minor + 1}.0`;
    break;
  case "patch":
    newVersion = `${major}.${minor}.${patch + 1}`;
    break;
}

// Increment build number
const currentBuildNumber = appJson.expo.android?.versionCode || 1;
const newBuildNumber = currentBuildNumber + 1;

console.log(`📦 Bumping version: ${currentVersion} → ${newVersion}`);
console.log(
  `🔢 Bumping build number: ${currentBuildNumber} → ${newBuildNumber}`,
);

// Update package.json
packageJson.version = newVersion;
fs.writeFileSync(packageJsonPath, JSON.stringify(packageJson, null, 2) + "\n");
console.log(`✅ Updated package.json`);

// Update app.json
appJson.expo.version = newVersion;
if (appJson.expo.android) {
  appJson.expo.android.versionCode = newBuildNumber;
}
if (appJson.expo.ios) {
  appJson.expo.ios.buildNumber = newBuildNumber.toString();
}
fs.writeFileSync(appJsonPath, JSON.stringify(appJson, null, 2) + "\n");
console.log(`✅ Updated app.json`);

console.log(`\n🎉 Version bump complete!`);
console.log(`   Version: ${newVersion}`);
console.log(`   Build: ${newBuildNumber}`);
console.log(`\n💡 Next steps:`);
console.log(`   1. git add package.json app.json`);
console.log(`   2. git commit -m "chore: bump version to ${newVersion}"`);
console.log(`   3. git tag -a v${newVersion} -m "Release ${newVersion}"`);
