#!/usr/bin/env python3
import base64
import gzip
import json
import re
import struct
from pathlib import Path

root = Path("app/src/main/java/com/sonharf/game/mascotdata3")
parts = {}
for path in sorted(root.glob("ChibiChunks*.kt")):
    text = path.read_text(encoding="utf-8")
    for idx, value in re.findall(r'CHIBI_CHUNK_(\d{3})\s*=\s*"([^"]*)"', text):
        parts[int(idx)] = value

if not parts:
    raise SystemExit("No Chibi chunks found")

missing = [i for i in range(max(parts) + 1) if i not in parts]
if missing:
    raise SystemExit(f"Missing chunks: {missing}")

encoded = "".join(parts[i] for i in range(max(parts) + 1))
glb = gzip.decompress(base64.b64decode(encoded))
if glb[:4] != b"glTF":
    raise SystemExit("Decoded payload is not GLB")

version, total_len = struct.unpack_from("<II", glb, 4)
json_len, json_type = struct.unpack_from("<II", glb, 12)
if version != 2 or json_type != 0x4E4F534A:
    raise SystemExit("Unexpected GLB header")
doc = json.loads(glb[20:20+json_len].decode("utf-8").rstrip(" \x00"))

print("GLB_BYTES", len(glb))
print("MATERIAL_COUNT", len(doc.get("materials", [])))
for i, mat in enumerate(doc.get("materials", [])):
    pbr = mat.get("pbrMetallicRoughness", {})
    print("MATERIAL", i, json.dumps({
        "name": mat.get("name"),
        "baseColorFactor": pbr.get("baseColorFactor"),
        "baseColorTexture": pbr.get("baseColorTexture"),
        "metallicFactor": pbr.get("metallicFactor"),
        "roughnessFactor": pbr.get("roughnessFactor"),
        "normalTexture": mat.get("normalTexture"),
        "emissiveFactor": mat.get("emissiveFactor"),
        "emissiveTexture": mat.get("emissiveTexture"),
        "alphaMode": mat.get("alphaMode"),
    }, ensure_ascii=False))

print("IMAGE_COUNT", len(doc.get("images", [])))
for i, image in enumerate(doc.get("images", [])):
    print("IMAGE", i, json.dumps(image, ensure_ascii=False))

print("TEXTURE_COUNT", len(doc.get("textures", [])))
for i, tex in enumerate(doc.get("textures", [])):
    print("TEXTURE", i, json.dumps(tex, ensure_ascii=False))

print("MESH_COUNT", len(doc.get("meshes", [])))
for mi, mesh in enumerate(doc.get("meshes", [])):
    print("MESH", mi, mesh.get("name"))
    for pi, prim in enumerate(mesh.get("primitives", [])):
        print("PRIMITIVE", mi, pi, "material", prim.get("material"), "attrs", sorted(prim.get("attributes", {}).keys()))

print("NODE_COUNT", len(doc.get("nodes", [])))
for ni, node in enumerate(doc.get("nodes", [])):
    if "mesh" in node:
        print("NODE_MESH", ni, json.dumps({"name": node.get("name"), "mesh": node.get("mesh"), "skin": node.get("skin")}, ensure_ascii=False))

print("ANIMATIONS", [a.get("name") for a in doc.get("animations", [])])
