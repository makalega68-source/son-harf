#!/usr/bin/env bash
set -euo pipefail

mkdir -p qa-home
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
adb shell am start -W -n com.sonharf.game/.HomeRealQaActivity
sleep 7

if ! adb shell dumpsys activity activities | grep -q 'HomeRealQaActivity'; then
  echo "HomeRealQaActivity is not active" >&2
  adb logcat -d -t 300 || true
  exit 1
fi

adb shell uiautomator dump /sdcard/home-qa.xml >/dev/null 2>&1 || true
DUMP="$(adb shell cat /sdcard/home-qa.xml 2>/dev/null || true)"
if printf '%s' "$DUMP" | grep -qiE "isn't responding|not responding|has stopped|keeps stopping"; then
  echo "System/app error dialog visible; refusing dirty screenshot" >&2
  exit 1
fi

adb exec-out screencap -p > qa-home/home-real.png
test -s qa-home/home-real.png
