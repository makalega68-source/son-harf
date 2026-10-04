"""Exercise the Kelimelik-style lobby without starting purchases or matches."""
import re
import subprocess
import time
import xml.etree.ElementTree as ET


def adb(*args):
    return subprocess.check_output(["adb", *args], timeout=60)


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


def desc(root, text):
    found = next((n for n in root.iter("node") if n.get("content-desc") == text), None)
    assert found is not None, f"missing control: {text}"
    return found


adb("shell", "am", "force-stop", "com.sonharf.game")
adb("logcat", "-c")
adb("shell", "am", "start", "-W", "-n", "com.sonharf.game/.UiScreenshotActivity", "--es", "review_stage", "home-dark")
time.sleep(5)
home = tree("Home-menu.xml")
new_game = center(node(home, "Yeni Oyun"))
my_games = center(node(home, "Oyunlarım"))
# The two big buttons sit side by side.
assert abs(new_game[1] - my_games[1]) < 15 and new_game[0] < my_games[0], (new_game, my_games)
# Bottom bar: Mağaza · Taht · Oyna · Oyunlar · Profil, left to right, below the buttons.
tabs = [center(desc(home, label)) for label in ("Mağaza", "Taht", "Oyna", "Oyunlar", "Profil")]
assert [x for x, _ in tabs] == sorted(x for x, _ in tabs), tabs
assert all(y > new_game[1] for _, y in tabs), (new_game, tabs)
# Game lists are not on the lobby any more.
assert find(home, "SIRA SENDE") is None
capture("Home-menu-Android.png")

adb("shell", "input", "tap", str(my_games[0]), str(my_games[1]))
time.sleep(3)
node(tree("Home-games.xml"), "Aktif")
capture("Home-games-Android.png")
adb("shell", "input", "keyevent", "4")
time.sleep(2)
node(tree("Home-return.xml"), "Yeni Oyun")

adb("shell", "input", "tap", str(tabs[0][0]), str(tabs[0][1]))
time.sleep(4)
node(tree("Home-store.xml"), "Mağaza")
capture("Home-store-Android.png")
adb("shell", "input", "keyevent", "4")
time.sleep(2)
node(tree("Home-store-return.xml"), "Yeni Oyun")

runtime = adb("logcat", "-d", "-s", "AndroidRuntime:E")
assert b"FATAL EXCEPTION" not in runtime, runtime
with open("Home-menu-runtime.log", "wb") as f:
    f.write(runtime)
print("PASS: lobby with Yeni Oyun / Oyunlarım, five-tab bar, My Games and store open and return home")
