#!/usr/bin/env bash
set -euo pipefail

mkdir -p qa-screens
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell settings put system font_scale 1.0

# Pixel emulator images can surface a launcher/Quickstep ANR in headless mode.
# The visual QA activity is started explicitly, so the launcher is not needed for capture.
adb shell settings put global hide_error_dialogs 1 || true
adb shell settings put global show_first_crash_dialog 0 || true
adb shell settings put global show_restart_in_crash_dialog 0 || true
for launcher in com.google.android.apps.nexuslauncher com.android.launcher3; do
  adb shell am force-stop "$launcher" >/dev/null 2>&1 || true
  adb shell pm disable-user --user 0 "$launcher" >/dev/null 2>&1 || true
done

clear_quickstep_dialog() {
  local dump line bounds x1 y1 x2 y2
  for _ in 1 2 3; do
    for launcher in com.google.android.apps.nexuslauncher com.android.launcher3; do
      adb shell am force-stop "$launcher" >/dev/null 2>&1 || true
    done

    adb shell uiautomator dump /sdcard/qa-window.xml >/dev/null 2>&1 || true
    dump="$(adb shell cat /sdcard/qa-window.xml 2>/dev/null || true)"
    if ! printf '%s' "$dump" | grep -qiE "Quickstep.*isn't responding|Quickstep.*not responding"; then
      break
    fi

    # Prefer closing only the known Quickstep process. Resolve the button center from UI bounds
    # rather than hard-coding coordinates so this survives emulator density changes.
    line="$(printf '%s' "$dump" | sed 's/></>\n</g' | grep -Ei 'text="Close app"|text="Wait"|text="Uygulamayı kapat"|text="Bekle"' | head -1 || true)"
    bounds="$(printf '%s' "$line" | sed -n 's/.*bounds="\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]".*/\1 \2 \3 \4/p')"
    if [ -n "$bounds" ]; then
      read -r x1 y1 x2 y2 <<< "$bounds"
      adb shell input tap $(((x1 + x2) / 2)) $(((y1 + y2) / 2)) || true
    else
      adb shell input keyevent KEYCODE_BACK || true
    fi
    sleep 1
  done

  # Fail the capture if the system ANR is still obscuring the app; never accept a dirty screenshot.
  adb shell uiautomator dump /sdcard/qa-window.xml >/dev/null 2>&1 || true
  dump="$(adb shell cat /sdcard/qa-window.xml 2>/dev/null || true)"
  if printf '%s' "$dump" | grep -qiE "Quickstep.*isn't responding|Quickstep.*not responding"; then
    echo "Quickstep ANR still visible; refusing dirty visual QA capture" >&2
    exit 1
  fi
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
