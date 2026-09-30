#!/bin/bash
# Run from repo root. You must follow the on-screen instructions and switch the app on your phone when asked.
set -e
ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
SCREENSHOTS="docs/screenshots"

if [ ! -x "$ADB" ]; then
  echo "adb not found at $ADB. Set ADB= path or install Android SDK platform-tools."
  exit 1
fi
if ! "$ADB" devices | grep -q 'device$'; then
  echo "No device connected. Connect your phone with USB debugging enabled and run: $ADB devices"
  exit 1
fi

echo "=============================================="
echo "CAPTURE 1/3: MAIN SCREEN (Register / Login)"
echo "On your phone: open the app to the FIRST screen (Register and Login buttons)."
echo "I will capture in 12 seconds..."
echo "=============================================="
sleep 12
"$ADB" exec-out screencap -p > "$SCREENSHOTS/app-main.png"
echo "Saved app-main.png"

echo ""
echo "=============================================="
echo "CAPTURE 2/3: LOGIN SCREEN"
echo "On your phone: tap LOGIN so the email/password form is visible."
echo "I will capture in 12 seconds..."
echo "=============================================="
sleep 12
"$ADB" exec-out screencap -p > "$SCREENSHOTS/app-login.png"
echo "Saved app-login.png"

echo ""
echo "=============================================="
echo "CAPTURE 3/3: PROFILE SCREEN"
echo "On your phone: log in, then open PROFILE (or calendar)."
echo "I will capture in 15 seconds..."
echo "=============================================="
sleep 15
"$ADB" exec-out screencap -p > "$SCREENSHOTS/app-profile.png"
echo "Saved app-profile.png"

echo ""
echo "Done. Screenshots are in $SCREENSHOTS/"
