// Simple script to create placeholder asset images as base64 PNG
/* eslint-disable @typescript-eslint/no-require-imports */
const fs = require("fs");
const path = require("path");

// Minimal 1x1 transparent PNG (base64)
const transparentPNG =
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

// Create a simple colored PNG (1024x1024) - Blue square
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const createColoredPNG = (_size, _color) => {
  // This is a minimal PNG - for production you'd want proper images
  return Buffer.from(transparentPNG, "base64");
};

const assetsDir = path.join(__dirname, "assets");

// Ensure assets directory exists
if (!fs.existsSync(assetsDir)) {
  fs.mkdirSync(assetsDir);
}

// Create placeholder images
const placeholderImage = Buffer.from(transparentPNG, "base64");

fs.writeFileSync(path.join(assetsDir, "icon.png"), placeholderImage);
fs.writeFileSync(path.join(assetsDir, "adaptive-icon.png"), placeholderImage);
fs.writeFileSync(path.join(assetsDir, "splash.png"), placeholderImage);
fs.writeFileSync(path.join(assetsDir, "favicon.png"), placeholderImage);

console.log("✅ Placeholder assets created successfully!");
console.log(
  "Note: These are minimal placeholders. Use icon-template.html to create proper icons.",
);
