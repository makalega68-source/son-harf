"""Exercise the production home shortcuts without starting purchases or matches."""
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


def node(root, text):
    return next(n for n in root.iter("node") if n.get("text") == text)


def center(n):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
    return ((x1 + x2) // 2, (y1 + y2) // 2)


def capture(name):
    with open(name, "wb") as f:
        f.write(adb("exec-out", "screencap", "-p"))


adb("shell", "am", "force-stop", "com.sonharf.game")
adb("logcat", "-c")
adb("shell", "am", "start", "-W", "-n", "com.sonharf.game/.UiScreenshotActivity", "--es", "review_stage", "home-dark")
time.sleep(4)
home = tree("Home-menu.xml")
games = [center(node(home, title)) for title in ("Kelime Kuşatması", "Son Harf", "Kelime Atölyesi")]
assert games[0][1] < games[1][1] < games[2][1], games
left_edges = [int(re.findall(r"\d+", node(home, title).get("bounds"))[0])
              for title in ("Kelime Kuşatması", "Son Harf", "Kelime Atölyesi")]
assert max(left_edges) - min(left_edges) < 8, left_edges
pro = center(node(home, "PRO üyelik"))
mascots = center(node(home, "Maskotlar"))
assert abs(pro[1] - mascots[1]) < 15 and pro[0] < mascots[0], (pro, mascots)
capture("Home-menu-Android.png")
adb("shell", "input", "tap", str(pro[0]), str(pro[1]))
time.sleep(3)
node(tree("Home-pro.xml"), "KELİME TAHTI PRO")
capture("Home-pro-Android.png")
adb("shell", "input", "keyevent", "4")
time.sleep(2)
returned = tree("Home-return.xml")
node(returned, "Kelime Kuşatması")
mascots = center(node(returned, "Maskotlar"))
adb("shell", "input", "tap", str(mascots[0]), str(mascots[1]))
time.sleep(4)
capture("Home-mascots-Android.png")
store = tree("Home-mascots.xml")
node(store, "Mağaza")
assert node(store, "Maskotlar").get("selected") == "true", "Mascot purchase category must open directly"
capture("Home-mascots-Android.png")
node(store, "Obi")
adb("shell", "input", "keyevent", "4")
time.sleep(2)
node(tree("Home-mascots-return.xml"), "Kelime Kuşatması")
runtime = adb("logcat", "-d", "-s", "AndroidRuntime:E")
assert b"FATAL EXCEPTION" not in runtime, runtime
with open("Home-menu-runtime.log", "wb") as f:
    f.write(runtime)
print("PASS: three stacked games, paired PRO/Mascots shortcuts, real destinations and return navigation")
