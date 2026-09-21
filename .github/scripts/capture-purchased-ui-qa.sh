#!/usr/bin/env bash
set -euo pipefail

mkdir -p qa-screens
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell settings put system font_scale 1.0

capture() {
  local name="$1"
  local screen="$2"
  local pause_seconds="$3"

  adb shell am force-stop com.sonharf.game
  adb shell am start -W -n com.sonharf.game/.VisualQaActivity --es screen "$screen"
  sleep "$pause_seconds"

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
adb exec-out screencap -p > qa-screens/daily-reward.png
test -s qa-screens/daily-reward.png
capture pro-vip pro 6
capture settings settings 5
