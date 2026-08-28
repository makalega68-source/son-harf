#!/usr/bin/env python3
import base64, gzip, json, re, struct
from pathlib import Path

root = Path("app/src/main/java/com/sonharf/game/mascotdata3")
parts = {}
for path in sorted(root.glob("ChibiChunks*.kt")):
    text = path.read_text(encoding="utf-8")
    for idx, value in re.findall(r'CHIBI_CHUNK_(\d{3})\s*=\s*"([^"]*)"', text):
        parts[int(idx)] = value
encoded = "".join(parts[i] for i in range(max(parts)+1))
glb = gzip.decompress(base64.b64decode(encoded))
json_len, json_type = struct.unpack_from("<II", glb, 12)
doc = json.loads(glb[20:20+json_len].decode("utf-8").rstrip(" \x00"))

# Find BIN chunk start.
json_padded = (json_len + 3) & ~3
bin_header = 20 + json_padded
bin_len, bin_type = struct.unpack_from("<II", glb, bin_header)
assert bin_type == 0x004E4942
bin_start = bin_header + 8
bin_blob = glb[bin_start:bin_start+bin_len]

out = Path("artifacts/chibi-textures")
out.mkdir(parents=True, exist_ok=True)
for i, image in enumerate(doc.get("images", [])):
    bv = doc["bufferViews"][image["bufferView"]]
    start = bv.get("byteOffset", 0)
    end = start + bv["byteLength"]
    ext = ".png" if image["mimeType"] == "image/png" else ".jpg"
    target = out / f"{i}_{image.get('name','image')}{ext}"
    target.write_bytes(bin_blob[start:end])
    print("EXTRACTED", target, target.stat().st_size)
