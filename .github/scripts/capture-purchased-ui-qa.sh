#!/usr/bin/env bash
set -euo pipefail

mkdir -p qa-screens
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell settings put system font_scale 1.0
# Headless Pixel images can surface a launcher/Quickstep ANR unrelated to the app.
# Hide system error dialogs and defensively dismiss only that known system dialog.
adb shell settings put global hide_error_dialogs 1 || true
adb shell settings put global show_first_crash_dialog 0 || true
adb shell settings put global show_restart_in_crash_dialog 0 || true

clear_quickstep_dialog() {
  local dump
  for _ in 1 2 3; do
    adb shell uiautomator dump /sdcard/qa-window.xml >/dev/null 2>&1 || true
    dump="$(adb shell cat /sdcard/qa-window.xml 2>/dev/null || true)"
    if printf '%s' "$dump" | grep -qiE "Quickstep.*isn't responding|Quickstep.*not responding"; then
      adb shell input keyevent KEYCODE_BACK || true
      sleep 1
    else
      break
    fi
  done
}

capture() {
  local name="$1"
  local screen="$2"
  local pause_seconds="$3"

  adb shell am force-stop com.sonharf.game
  adb shell am start -W -n com.sonharf.game/.VisualQaActivity --es screen "$screen"
  sleep "$pause_seconds"
  clear_quickstep_dialog

  if ! adb shell dumpsys activity activities | grep -q 'VisualQaActivity'; then
    echo "VisualQaActivity is not active for screen=$screen" >&2
    adb logcat -d -t 300 || true
    exit 1
  fi

  adb exec-out screencap -p > "qa-screens/${name}.png"
  test -s "qa-screens/${name}.png"
}

capture home home 5
capture store store 6
capture profile profile 6
capture social social 6
capture siege siege 7
capture son-harf sonharf 3
capture harf-yolu harfyolu 10
capture leaderboard leaderboard 6
capture missions retention 6
adb shell input swipe 540 1800 540 700 650
sleep 1
clear_quickstep_dialog
adb exec-out screencap -p > qa-screens/daily-reward.png
test -s qa-screens/daily-reward.png
capture pro-vip pro 6
capture settings settings 5
