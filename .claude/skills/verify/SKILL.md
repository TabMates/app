---
name: verify
description: Build, install, and drive the TabMates Android app on the local emulator to verify UI/behavior changes end-to-end with screenshots.
---

# Verify TabMates on the Android emulator

## Build + install + launch

```bash
./gradlew :androidApp:assembleDebug
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell monkey -p de.tabmates.androidapp -c android.intent.category.LAUNCHER 1
```

- applicationId: `de.tabmates.androidapp` (module `:androidApp`; UI lives in `:composeApp`).
- Emulator usually already running as `emulator-5554` with a logged-in test account (groups "Test 2", "Group 1", "test").

## Drive + capture

```bash
adb exec-out screencap -p > shot.png        # screenshot
adb shell input tap X Y                      # coordinates are 1080x2424
adb shell input keyevent KEYCODE_BACK
adb shell input swipe 10 1200 600 1200 300   # gesture back
```

To capture mid-animation frames (transitions), slow animations instead of screenrecord:

```bash
adb shell settings put global animator_duration_scale 10
# ... navigate + screencap ...
adb shell settings put global animator_duration_scale 1   # ALWAYS restore
```

## Gotchas

- `screenrecord` output is h264; the Fedora host ffmpeg cannot decode it (openh264 soname mismatch) — use the animator-scale trick above instead.
- The emulator cannot reach the prod backend, so the app sits in offline mode: the offline banner appears after its 15s grace period, and sync/login are unavailable. **Do not log out** — you cannot log back in offline.
- Offline banner: "Offline · last synced …" strip at the very top under the status bar.
