#!/usr/bin/env python3
from PIL import Image
import numpy as np
from pathlib import Path

white = np.asarray(Image.open("artifacts/lyra-white/lyra-white.png").convert("RGB"), dtype=np.int16)
dark = np.asarray(Image.open("artifacts/lyra-white/neris-original.png").convert("RGB"), dtype=np.int16)

# Exact 3D stage region in MascotRuntimeSmokeActivity; excludes the mascot-id text.
white = white[500:1045, 180:540]
dark = dark[500:1045, 180:540]

delta = np.abs(white - dark).max(axis=2)
changed = delta > 24
changed_count = int(changed.sum())
if changed_count < 2500:
    raise SystemExit(f"Lyra texture does not differ enough from Neris: changed={changed_count}")

def lum(rgb):
    return 0.2126 * rgb[..., 0] + 0.7152 * rgb[..., 1] + 0.0722 * rgb[..., 2]

w_lum = float(lum(white)[changed].mean())
d_lum = float(lum(dark)[changed].mean())
gain = w_lum - d_lum
report = (
    f"LYRA_WHITE_VISUAL_PASS changed={changed_count} "
    f"white_luma={w_lum:.2f} neris_luma={d_lum:.2f} gain={gain:.2f}"
)
Path("artifacts/lyra-white/visual-report.txt").write_text(report + "\n")
print(report)

if gain < 45:
    raise SystemExit(f"Lyra is not visibly white enough: gain={gain:.2f}")
