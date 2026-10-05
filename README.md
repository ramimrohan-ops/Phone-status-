# Phone Status

Android app + home-screen widget showing battery, RAM, storage, refresh rate and temperatures.
Kotlin, Jetpack Compose, Glance (widget). Min Android 8.0 (API 26).

## Get the APK from GitHub (no Android Studio needed)

1. Create a new GitHub repo and upload everything in this folder (keep the `.github` folder).
2. Open the **Actions** tab. The `Build APK` workflow runs on every push (or press **Run workflow**).
3. When it turns green, open the run and download the **PhoneStatus-debug-apk** artifact.
4. Unzip it and install `app-debug.apk` on your phone.

If the build fails, copy the red error lines from the Actions log and send them back.

## What it shows

| Item | Source | Needs root |
|---|---|---|
| Battery level, charging, voltage, current, health | BatteryManager | No |
| Battery temperature | BatteryManager | No |
| RAM used / total, swap | ActivityManager, /proc/meminfo | No |
| Storage used / total | StatFs on /data | No |
| Refresh rate (live measured + system mode + supported rates) | Choreographer frames, Display | No |
| CPU / GPU / hottest sensor temperature | /sys/class/thermal via `su` | **Yes** |

Grant root to Phone Status in your root manager the first time. The app waits up to 15 s for you to tap Grant.

## Widget

- Sizes: 2x2 and 4x2 (resizable).
- Tap the widget to refresh it. It also refreshes whenever you open the app.
- Android limits background refresh to about every 15 minutes. The widget cannot update live.
- Add it with the button in the app, or long-press the home screen, then Widgets, then Phone Status.

## Limits

- Per-app RAM and per-core CPU usage are blocked for normal apps on Android 10+, so they are not included.
- Total storage is the data partition size, so it is lower than the advertised capacity.
