#!/usr/bin/env python3
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def parse_bounds(value: str):
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", value or "")
    if not match:
        raise SystemExit("Unable to parse bounds: " + (value or "<empty>"))
    return tuple(map(int, match.groups()))


def app_edit_fields(xml_path: str):
    root = ET.parse(xml_path).getroot()
    return [
        node
        for node in root.iter("node")
        if node.attrib.get("package") == "com.sonharf.game"
        and node.attrib.get("class") == "android.widget.EditText"
    ]


def tap(xml_path: str):
    root = ET.parse(xml_path).getroot()
    candidates = []
    for node in root.iter("node"):
        if node.attrib.get("package") != "com.sonharf.game":
            continue
        cls = node.attrib.get("class", "")
        text = node.attrib.get("text", "")
        if (
            cls == "android.widget.EditText"
            or "Yoldaşına bir şey söyle" in text
            or "Say something to your companion" in text
        ):
            candidates.append(node)
    if not candidates:
        raise SystemExit("Production companion chat input is missing before IME")
    left, top, right, bottom = parse_bounds(candidates[-1].attrib.get("bounds", ""))
    if right <= left or bottom <= top:
        raise SystemExit("Chat input has invalid pre-IME bounds")
    x = (left + right) // 2
    y = (top + bottom) // 2
    subprocess.run(["adb", "shell", "input", "tap", str(x), str(y)], check=True)
    print(f"COMPANION_INPUT_TAP x={x} y={y}")


def verify(xml_path: str, log_path: str):
    editable = app_edit_fields(xml_path)
    if not editable:
        raise SystemExit("Production companion EditText is missing while IME is visible")

    left, top, right, bottom = parse_bounds(editable[-1].attrib.get("bounds", ""))
    log = Path(log_path).read_text(errors="replace")
    samples = re.findall(
        r"MASCOT_COMPANION_IME_VISIBLE=true root_height=(\d+) ime_bottom=(\d+) safe_bottom=(\d+)",
        log,
    )
    if not samples:
        raise SystemExit("IME-visible inset sample was not logged")

    root_height, ime_bottom, safe_bottom = map(int, samples[-1])
    if ime_bottom <= 0:
        raise SystemExit(f"IME reported visible with invalid inset: {ime_bottom}")
    if bottom > safe_bottom + 4:
        raise SystemExit(
            f"Chat input is obscured by IME: input_bottom={bottom}, safe_bottom={safe_bottom}"
        )
    if right <= left or bottom <= top:
        raise SystemExit("Chat input has invalid visible bounds")

    print(
        "COMPANION_IME_LAYOUT_PASS "
        f"input=[{left},{top}][{right},{bottom}] "
        f"root_height={root_height} ime_bottom={ime_bottom} safe_bottom={safe_bottom}"
    )


def main():
    if len(sys.argv) < 3:
        raise SystemExit("usage: verify_companion_ime.py tap <xml> | verify <xml> <log>")
    mode = sys.argv[1]
    if mode == "tap" and len(sys.argv) == 3:
        tap(sys.argv[2])
        return
    if mode == "verify" and len(sys.argv) == 4:
        verify(sys.argv[2], sys.argv[3])
        return
    raise SystemExit("invalid arguments")


if __name__ == "__main__":
    main()
