#!/usr/bin/env bash
set -euo pipefail

mkdir -p qa-home-full
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell settings put system font_scale 1.0
adb shell settings put global hide_error_dialogs 1 || true
adb shell settings put global show_first_crash_dialog 0 || true
adb shell settings put global show_restart_in_crash_dialog 0 || true

for launcher in com.google.android.apps.nexuslauncher com.android.launcher3; do
  adb shell am force-stop "$launcher" >/dev/null 2>&1 || true
  adb shell pm disable-user --user 0 "$launcher" >/dev/null 2>&1 || true
done

adb shell am force-stop com.sonharf.game
adb shell am start -W -n com.sonharf.game/.HomeFullShellQaActivity
sleep 10

if ! adb shell dumpsys activity activities | grep -q 'HomeFullShellQaActivity'; then
  echo "HomeFullShellQaActivity is not active" >&2
  adb logcat -d -t 400 || true
  exit 1
fi

adb shell uiautomator dump /sdcard/home-full.xml >/dev/null 2>&1 || true
DUMP="$(adb shell cat /sdcard/home-full.xml 2>/dev/null || true)"
if printf '%s' "$DUMP" | grep -qiE "isn't responding|not responding|has stopped|keeps stopping"; then
  echo "System/app error dialog visible; refusing dirty screenshot" >&2
  exit 1
fi

adb exec-out screencap -p > qa-home-full/home-full-shell.png
test -s qa-home-full/home-full-shell.png
