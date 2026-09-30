# How to capture Android app screenshots

Use these steps to take screenshots from the UsersSDK demo app on your phone. The images go in `docs/screenshots/` and are used in the main [README](../README.md).

## Easiest: run the guided script

From the **project root**, with the phone connected (USB debugging on):

```bash
./scripts/capture-android-screenshots.sh
```

The script will print instructions and wait (~12–15 seconds) before each capture. **When you see each message, switch the app on your phone to the requested screen** (main → login → profile). It saves `app-main.png`, `app-login.png`, and `app-profile.png` into `docs/screenshots/`.

## Manual way (one command per screen)

### Prerequisites

1. **USB debugging** – On your Android phone: Settings → Developer options → enable **USB debugging**. Connect the phone to your Mac with a USB cable.
2. **adb** – Use the one from your Android SDK, e.g.:
   ```bash
   export ADB="$HOME/Library/Android/sdk/platform-tools/adb"
   ```
   Or add that folder to your `PATH`.
3. **App installed** – Build and run the app from Android Studio (open `UsersSdkAndroid/`) so the demo app is on the device.

Check the device is seen:
```bash
$ADB devices
# You should see your device ID (e.g. 32281JEHN21369	device)
```

## Steps (do each step, then run the command)

All commands are run from the **project root** (the `UsersSDK` folder that contains `docs/`).

### 1. Main screen (Register / Login buttons)

- On the phone: open the **UsersSDK** demo app and stay on the first screen (the one with **Register** and **Login** buttons).
- In the terminal:
  ```bash
  $ADB exec-out screencap -p > docs/screenshots/app-main.png
  ```

### 2. Login screen

- On the phone: tap **Login** so the login form (email + password) is visible.
- In the terminal:
  ```bash
  $ADB exec-out screencap -p > docs/screenshots/app-login.png
  ```

### 3. Profile (or calendar) screen

- On the phone: log in (e.g. as admin or user), then open **Profile** (or the screen that shows profile/calendar).
- In the terminal:
  ```bash
  $ADB exec-out screencap -p > docs/screenshots/app-profile.png
  ```

## One-liners (if you prefer)

From the project root, with `ADB` set as above:

```bash
# 1. Show main screen on phone, then:
$ADB exec-out screencap -p > docs/screenshots/app-main.png

# 2. Open Login on phone, then:
$ADB exec-out screencap -p > docs/screenshots/app-login.png

# 3. Open Profile (after login) on phone, then:
$ADB exec-out screencap -p > docs/screenshots/app-profile.png
```

## Optional: extra screenshots

You can add more images the same way, for example:

- **Appointments list** (admin or user): save as `docs/screenshots/app-appointments.png` and add it to the README table if you want.

After saving, the README will show the new screenshots the next time you view it (e.g. on GitHub or in your editor).
