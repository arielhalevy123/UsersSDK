# How to install the Android app

Ways to get the UsersSDK demo app onto your phone.

## Run everything (quick start)

1. **Start the backend** (from the UsersSDK repo root):
   ```bash
   cd /path/to/UsersSDK
   ./gradlew bootRun
   ```
   Leave this running. The server starts on port 8080 and creates demo users (admin, Alice, Bob) with sample appointments.

2. **Install and run the app** on your phone (Option 1, 2, or 3 below). Set the app’s backend URL to your computer’s IP, e.g. `http://192.168.x.x:8080/` (same Wi‑Fi as the phone).

3. **Demo logins** (after backend and app are running):
   - **Admin:** `admin@example.com` / `admin123`
   - **User (see/add appointments):** `alice@example.com` / `secret` or `bob@example.com` / `secret`

## Option 1: Install via USB (adb)

1. **Build the APK** (from the repo root):
   ```bash
   cd UsersSdkAndroid
   # First time only: create local.properties with your Android SDK path:
   # echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
   ./gradlew assembleDebug
   ```
   The APK is created at: `app/build/outputs/apk/debug/app-debug.apk`

2. **Connect your phone** with a USB cable and turn on **USB debugging** (Settings → Developer options).

3. **Install** (use the full path to `adb` from your Android SDK):
   ```bash
   ~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
   Or add that folder to your PATH, then you can run `adb install -r ...`.

4. Open the **UsersSDK** app on your phone. Set the backend URL in the app (e.g. your computer’s IP and port 8080) if needed.

---

## Option 2: Copy the APK to your phone

1. Build the APK (see above). Then copy **app-debug.apk** to your phone by:
   - **AirDrop** (Mac to iPhone doesn’t install Android; use cable or cloud), or
   - **USB**: connect the phone, open it as a disk, and copy `app-debug.apk` into the **Download** folder (or any folder), or
   - **Google Drive / email**: upload the APK, open the file on the phone, and tap to install.

2. On the phone, if asked, allow **Install from unknown sources** (or “Install unknown apps”) for the app you use to open the APK (e.g. Files or Gmail).

3. Open the APK file on the phone and follow the install prompt.

---

## Option 3: Run from Android Studio

1. Open the **UsersSdkAndroid** folder in Android Studio (File → Open).
2. If prompted, create `local.properties` with `sdk.dir=` your Android SDK path (e.g. `sdk.dir=/Users/arielhalevy/Library/Android/sdk`).
3. Connect your phone (USB debugging on) or start an emulator.
4. Click **Run** (green triangle) to build and install the app on the device.

---

## Backend URL

The app talks to your backend. The URL is `BuildConfig.USERS_SDK_BASE_URL`, set once in `UsersSdkAndroid/build.gradle.kts` (default: the hosted https server). For a server on your computer, build with `./gradlew assembleDebug -PusersSdkBaseUrl=http://YOUR_IP:8080/`. Use your computer’s local IP (not `localhost`) when testing on a real device so the phone can reach the server, and add that IP to `res/xml/network_security_config.xml` if it is not `192.168.1.122` (Android blocks plain http to other hosts). Rebuild and reinstall after changing the URL.
