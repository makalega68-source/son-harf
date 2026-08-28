#!/usr/bin/env python3
from PIL import Image
from pathlib import Path

white = Image.open("artifacts/lyra-white/lyra-white.png").convert("RGB").crop((180, 500, 540, 1045))
dark = Image.open("artifacts/lyra-white/neris-original.png").convert("RGB").crop((180, 500, 540, 1045))

changed_count = 0
white_sum = 0.0
dark_sum = 0.0

for wp, dp in zip(white.getdata(), dark.getdata()):
    if max(abs(wp[i] - dp[i]) for i in range(3)) <= 24:
        continue
    changed_count += 1
    white_sum += 0.2126 * wp[0] + 0.7152 * wp[1] + 0.0722 * wp[2]
    dark_sum += 0.2126 * dp[0] + 0.7152 * dp[1] + 0.0722 * dp[2]

if changed_count < 2500:
    raise SystemExit(f"Lyra texture does not differ enough from Neris: changed={changed_count}")

white_luma = white_sum / changed_count
dark_luma = dark_sum / changed_count
gain = white_luma - dark_luma

report = (
    f"LYRA_WHITE_VISUAL_PASS changed={changed_count} "
    f"white_luma={white_luma:.2f} neris_luma={dark_luma:.2f} gain={gain:.2f}"
)
Path("artifacts/lyra-white/visual-report.txt").write_text(report + "\n")
print(report)

if gain < 45:
    raise SystemExit(f"Lyra is not visibly white enough: gain={gain:.2f}")
