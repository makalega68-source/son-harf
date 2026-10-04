"""Exercise the Kelimelik-style home without starting purchases or matches.

One page, no bottom bar: a top bar (profile, coins, settings), one NEW GAME button, four equal
shortcuts on one row, the game lists and the two other games side by side.
"""
import re
import subprocess
import time
import xml.etree.ElementTree as ET


def adb(*args):
    return subprocess.check_output(["adb", *args], timeout=25)


def tree(name):
    for _ in range(3):
        adb("shell", "rm", "-f", "/sdcard/home-menu.xml")
        adb("shell", "uiautomator", "dump", "--compressed", "/sdcard/home-menu.xml")
        try:
            adb("pull", "/sdcard/home-menu.xml", name)
            return ET.parse(name)
        except (subprocess.CalledProcessError, ET.ParseError):
            time.sleep(2)
    raise AssertionError("Home accessibility tree unavailable")


def find(root, text):
    return next((n for n in root.iter("node") if n.get("text") == text), None)


def node(root, text):
    found = find(root, text)
    assert found is not None, f"missing on screen: {text}"
    return found


def bounds(n):
    return list(map(int, re.findall(r"\d+", n.get("bounds"))))


def center(n):
    x1, y1, x2, y2 = bounds(n)
    return ((x1 + x2) // 2, (y1 + y2) // 2)


def capture(name):
    for _ in range(2):
        try:
            with open(name, "wb") as f:
                f.write(adb("exec-out", "screencap", "-p"))
            return
        except subprocess.TimeoutExpired:
            time.sleep(3)


adb("shell", "am", "force-stop", "com.sonharf.game")
adb("logcat", "-c")
adb("shell", "am", "start", "-W", "-n", "com.sonharf.game/.UiScreenshotActivity", "--es", "review_stage", "home-dark")
time.sleep(5)
home = tree("Home-menu.xml")
new_game = center(node(home, "YENİ OYUN"))
shortcuts = [center(node(home, label)) for label in ("Arkadaşlar", "Taht", "Etkinlik", "PRO")]
# One row, left to right, below the NEW GAME button.
assert all(abs(y - shortcuts[0][1]) < 15 for _, y in shortcuts), shortcuts
assert [x for x, _ in shortcuts] == sorted(x for x, _ in shortcuts), shortcuts
assert shortcuts[0][1] > new_game[1], (new_game, shortcuts)
for title in ("SIRA SENDE", "SIRA RAKİPTE"):
    assert any((n.get("text") or "").startswith(title) for n in home.iter("node")), title
# No bottom navigation bar any more.
assert find(home, "Ana Sayfa") is None and find(home, "Oyunlarım") is None
capture("Home-menu-Android.png")

adb("shell", "input", "tap", str(shortcuts[3][0]), str(shortcuts[3][1]))
time.sleep(3)
node(tree("Home-pro.xml"), "Arkadaş listesi")
capture("Home-pro-Android.png")
adb("shell", "input", "keyevent", "4")
time.sleep(2)
node(tree("Home-return.xml"), "YENİ OYUN")

coins = center(node(home, "Son Coin"))
adb("shell", "input", "tap", str(coins[0]), str(coins[1]))
time.sleep(4)
store = tree("Home-store.xml")
node(store, "Mağaza")
capture("Home-store-Android.png")
adb("shell", "input", "keyevent", "4")
time.sleep(2)
node(tree("Home-store-return.xml"), "YENİ OYUN")

runtime = adb("logcat", "-d", "-s", "AndroidRuntime:E")
assert b"FATAL EXCEPTION" not in runtime, runtime
with open("Home-menu-runtime.log", "wb") as f:
    f.write(runtime)
print("PASS: single home page, NEW GAME, four shortcuts on one row, PRO and store open and return home")
