#!/usr/bin/env python3
import base64
import colorsys
import gzip
import hashlib
import io
import json
import re
import struct
from pathlib import Path
from PIL import Image

SRC = Path("app/src/main/java/com/sonharf/game/mascotdata3")
OUT = Path("app/src/main/assets/models/lyra_white_chibi.glb")

parts = {}
for path in sorted(SRC.glob("ChibiChunks*.kt")):
    text = path.read_text(encoding="utf-8")
    for idx, value in re.findall(r'CHIBI_CHUNK_(\d{3})\s*=\s*"([^"]*)"', text):
        parts[int(idx)] = value
if not parts:
    raise SystemExit("No embedded Chibi chunks found")
encoded = "".join(parts[i] for i in range(max(parts) + 1))
glb = gzip.decompress(base64.b64decode(encoded))

if glb[:4] != b"glTF":
    raise SystemExit("Invalid GLB")
version, _ = struct.unpack_from("<II", glb, 4)
json_len, json_type = struct.unpack_from("<II", glb, 12)
if version != 2 or json_type != 0x4E4F534A:
    raise SystemExit("Unsupported GLB")

json_end = 20 + json_len
doc = json.loads(glb[20:json_end].decode("utf-8").rstrip(" \x00"))
json_padded_len = (json_len + 3) & ~3
bin_header = 20 + json_padded_len
bin_len, bin_type = struct.unpack_from("<II", glb, bin_header)
if bin_type != 0x004E4942:
    raise SystemExit("Missing GLB BIN chunk")
bin_start = bin_header + 8
bin_blob = bytearray(glb[bin_start:bin_start + bin_len])

materials = doc.get("materials", [])
mage_index = next((i for i, m in enumerate(materials) if m.get("name") == "Mage_Cat"), None)
clothes_index = next((i for i, m in enumerate(materials) if m.get("name") == "Mage_Cat_Clothes"), None)
if mage_index is None or clothes_index is None:
    raise SystemExit("Expected Mage_Cat materials are missing")

mage_tex_index = materials[mage_index]["pbrMetallicRoughness"]["baseColorTexture"]["index"]
clothes_tex_index = materials[clothes_index]["pbrMetallicRoughness"]["baseColorTexture"]["index"]
if mage_tex_index != clothes_tex_index:
    raise SystemExit("Source model no longer shares the expected atlas")

source_image_index = doc["textures"][mage_tex_index]["source"]
source_image = doc["images"][source_image_index]
source_view = doc["bufferViews"][source_image["bufferView"]]
start = source_view.get("byteOffset", 0)
end = start + source_view["byteLength"]
source_bytes = bytes(bin_blob[start:end])

image = Image.open(io.BytesIO(source_bytes)).convert("RGB")
pixels = image.load()
for y in range(image.height):
    for x in range(image.width):
        r8, g8, b8 = pixels[x, y]
        r, g, b = r8 / 255.0, g8 / 255.0, b8 / 255.0
        h, s, v = colorsys.rgb_to_hsv(r, g, b)
        deg = h * 360.0

        # Preserve warm accents (inner ears / small gold-pink details) and existing highlights.
        warm_accent = (deg < 55.0 or deg > 325.0) and s > 0.38 and v > 0.35
        bright_highlight = v > 0.82 and s < 0.22

        # Fur/hair in this asset is black, neutral shadow, blue or purple.
        purple_blue = 190.0 <= deg <= 325.0
        neutral = s < 0.38
        dark = v < 0.42

        if not warm_accent and not bright_highlight and (purple_blue or neutral or dark):
            shade = int(210 + 42 * min(1.0, max(0.0, v)))
            pixels[x, y] = (
                min(255, shade + 8),
                min(255, shade + 6),
                min(255, shade + 3),
            )

buf = io.BytesIO()
image.save(buf, format="JPEG", quality=95, optimize=True)
white_jpeg = buf.getvalue()

# Append a separate white atlas. Clothes keep using the original source atlas.
while len(bin_blob) % 4:
    bin_blob.append(0)
white_offset = len(bin_blob)
bin_blob.extend(white_jpeg)
white_length = len(white_jpeg)
while len(bin_blob) % 4:
    bin_blob.append(0)

new_view_index = len(doc["bufferViews"])
doc["bufferViews"].append({
    "buffer": 0,
    "byteOffset": white_offset,
    "byteLength": white_length,
})

new_image_index = len(doc["images"])
doc["images"].append({
    "bufferView": new_view_index,
    "mimeType": "image/jpeg",
    "name": "Mage_Cat_Lyra_White",
})

new_texture_index = len(doc["textures"])
source_texture = doc["textures"][mage_tex_index]
new_texture = {"source": new_image_index}
if "sampler" in source_texture:
    new_texture["sampler"] = source_texture["sampler"]
doc["textures"].append(new_texture)

materials[mage_index]["pbrMetallicRoughness"]["baseColorTexture"]["index"] = new_texture_index
materials[mage_index]["pbrMetallicRoughness"]["baseColorFactor"] = [1.0, 1.0, 1.0, 1.0]
doc["buffers"][0]["byteLength"] = len(bin_blob)

json_bytes = json.dumps(doc, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
json_bytes += b" " * ((4 - len(json_bytes) % 4) % 4)
bin_bytes = bytes(bin_blob)
bin_bytes += b"\x00" * ((4 - len(bin_bytes) % 4) % 4)

total_len = 12 + 8 + len(json_bytes) + 8 + len(bin_bytes)
out = bytearray()
out += b"glTF"
out += struct.pack("<II", 2, total_len)
out += struct.pack("<II", len(json_bytes), 0x4E4F534A)
out += json_bytes
out += struct.pack("<II", len(bin_bytes), 0x004E4942)
out += bin_bytes

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_bytes(out)
print("LYRA_WHITE_GLB", OUT, len(out))
print("LYRA_WHITE_SHA256", hashlib.sha256(out).hexdigest())
print("WHITE_IMAGE", new_image_index, "WHITE_TEXTURE", new_texture_index, "MAGE_MATERIAL", mage_index, "CLOTHES_MATERIAL", clothes_index)
