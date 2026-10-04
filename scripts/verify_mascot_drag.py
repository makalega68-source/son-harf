"""Verify the production companion's native drag and saved placement with adb."""
import re
import subprocess
import time
import xml.etree.ElementTree as ET


def adb(*args):
    return subprocess.check_output(["adb", *args], timeout=60)


def bounds(name):
    # UiAutomator can return a null root just after a cold activity launch.
    # Never reuse a stale dump, and keep evidence if all bounded retries fail.
    node = None
    for attempt in range(5):
        try:
            adb("shell", "rm", "-f", "/sdcard/mascot.xml")
            # The dump itself fails (exit 255, "null root node") while the window is still settling.
            adb("shell", "uiautomator", "dump", "--compressed", "/sdcard/mascot.xml")
            adb("pull", "/sdcard/mascot.xml", name)
            node = next((n for n in ET.parse(name).iter("node") if n.get("content-desc") == "Maskot"), None)
        except (subprocess.CalledProcessError, subprocess.TimeoutExpired, ET.ParseError):
            # A stalled software-GPU emulator can hang one adb call; wait for it to answer again.
            subprocess.run(["adb", "wait-for-device"], timeout=120)
        if node is not None:
            break
        time.sleep(2)
    if node is None:
        with open("Mascot-drag-failure-Android.png", "wb") as f:
            f.write(adb("exec-out", "screencap", "-p"))
        with open("Mascot-drag-runtime.log", "wb") as f:
            f.write(adb("logcat", "-d", "-s", "AndroidRuntime:E"))
        raise AssertionError("Visible mascot missing from accessibility tree")
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    return ((x1 + x2) // 2, (y1 + y2) // 2)


adb("shell", "am", "force-stop", "com.sonharf.game")
adb("logcat", "-c")
adb("shell", "am", "start", "-W", "-n", "com.sonharf.game/.UiScreenshotActivity", "--es", "review_stage", "mascot-drag", "--ez", "reset_mascot_position", "true")
time.sleep(3)
start = bounds("Mascot-drag-before.xml")
adb("shell", "input", "swipe", str(start[0]), str(start[1]), str(start[0] - 180), str(start[1] - 400), "800")
time.sleep(2)
moved = bounds("Mascot-drag-after.xml")
assert moved[0] < start[0] - 120 and moved[1] < start[1] - 340, (start, moved)
adb("shell", "am", "force-stop", "com.sonharf.game")
adb("shell", "am", "start", "-W", "-n", "com.sonharf.game/.UiScreenshotActivity", "--es", "review_stage", "mascot-drag")
time.sleep(3)
restored = bounds("Mascot-drag-restored.xml")
assert abs(restored[0] - moved[0]) < 12 and abs(restored[1] - moved[1]) < 12, (moved, restored)
with open("Mascot-drag-Android.png", "wb") as f:
    f.write(adb("exec-out", "screencap", "-p"))
runtime = adb("logcat", "-d", "-s", "AndroidRuntime:E")
assert b"FATAL EXCEPTION" not in runtime, runtime
with open("Mascot-drag-runtime.log", "wb") as f:
    f.write(runtime)
print("PASS: native mascot drag, no tap escape, saved placement survives restart", start, moved, restored)
