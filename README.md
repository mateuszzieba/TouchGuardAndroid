# TouchGuardAndroid

**TouchGuard** is a lightweight Android app that helps prevent accidental touchscreen interactions while watching videos, reading, or using other apps. It places a transparent touch-blocking overlay over regular app content for a limited time.

No account, advertisements, analytics, or internet connection are required.

## Features

- Transparent overlay that absorbs touches intended for regular applications.
- Video playback and audio can continue while touch protection is active.
- Automatic unlock after **60 seconds** by default.
- Adjustable timeout from **15 seconds to 30 minutes**, in 15-second increments.
- Manual unlock by holding the small on-screen lock indicator for **3 seconds**.
- Quick Settings tile to toggle touch protection.
- Unlock action in the notification.
- Physical **power and volume buttons remain available**.

## How to use

1. Install and launch TouchGuard.
2. Allow **Display over other apps** when prompted.
3. Optionally allow notifications to access the notification unlock action.
4. Choose the protection duration in the app.
5. Tap the lock button in TouchGuard, or enable the Quick Settings tile and use it from another app.
6. To unlock early, hold the on-screen lock indicator for 3 seconds, use the notification action, or wait for the timeout.

To add the Quick Settings tile, use the in-app button on supported Android versions or edit the Quick Settings panel manually.

## Download APK

Get the APK from **[GitHub Releases](https://github.com/mateuszzieba/TouchGuardAndroid/releases)**.

For the first version, download `TouchGuardAndroid-v1.0.0.apk` from the **Assets** section of the release. Open it on your Android device and allow installation from your browser or file manager if Android requests permission.

> **Note:** Version 1.0.0 is a **debug-signed test build**, not a production-signed release. Updating to a build signed with a different key may require uninstalling the previous version first.

## Build on Ubuntu

The project uses Java 17, Android SDK 35, Android Gradle Plugin, and Gradle 8.9. Android Studio is not required; the build script prepares the necessary command-line tools.

From the project directory:

```bash
chmod +x build_apk_ubuntu.sh
./build_apk_ubuntu.sh
```

The APK is generated at:

```text
output/TouchGuard-debug.apk
```

You can copy the APK to your phone, download it from a GitHub Release, or install it on a connected emulator with:

```bash
./install_apk_ubuntu.sh
```

The first build requires an internet connection and may install development dependencies.

## Compatibility

- Minimum Android version: **Android 8.0 (API 26)**.
- Target SDK: **Android 15 (API 35)**.
- Some devices, especially those using Xiaomi/POCO HyperOS, may require additional permissions for overlays or background operation.

## Safety and limitations

TouchGuard is an **accidental-touch prevention tool**, not a security lock or kiosk mode.

- It cannot reliably block Android system navigation gestures, the notification shade, or protected system screens.
- It does not intercept physical power or volume buttons.
- The overlay is removed when the protection timeout expires or the service stops.
- Start testing with the default 60-second timeout and confirm that manual unlock works on your device.

Using hardware-key combinations (such as holding Power and Volume Up) is not supported, because ordinary Android applications cannot reliably intercept those system-reserved shortcuts.

## Privacy

TouchGuard stores the selected timeout locally on the device. It does not request internet access and does not collect or transmit personal data.

## Project structure

```text
app/src/main/java/pl/touchguard/
  MainActivity.java       # Main screen, permissions, timeout settings
  TouchLockService.java  # Touch-blocking overlay and unlock timer
  TouchTileService.java  # Quick Settings tile
  ToggleActivity.java     # Tile action helper
  Prefs.java              # Locally stored preferences
build_apk_ubuntu.sh       # Build APK on Ubuntu
install_apk_ubuntu.sh     # Install APK through ADB
```

## Project status

**v1.0.0 — initial test release.** The debug APK has been built and installed on an Android emulator; the overlay behavior has not yet been verified on a physical phone.

## License

No software license has been added to the repository yet. A license should be selected before inviting third-party reuse or contributions.
