#!/usr/bin/env python3
import argparse
import hashlib
import json
import pathlib
import re
import sys
import zipfile
from io import BytesIO

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "assets" / "frame_provenance_manifest.json"
DRAWABLE_DIR = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
CATALOG = ROOT / "app" / "src" / "main" / "java" / "com" / "sonharf" / "game" / "PurchasedStyleUi.kt"


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def rgba_fingerprint(data: bytes):
    with Image.open(BytesIO(data)) as image:
        rgba = image.convert("RGBA")
        return rgba.width, rgba.height, sha256(rgba.tobytes())


def verify_payload(label: str, data: bytes, spec: dict):
    actual_file = sha256(data)
    width, height, actual_rgba = rgba_fingerprint(data)
    errors = []
    if actual_file != spec["file_sha256"]:
        errors.append(f"file sha expected={spec['file_sha256']} actual={actual_file}")
    if (width, height) != (spec["width"], spec["height"]):
        errors.append(f"dimensions expected={spec['width']}x{spec['height']} actual={width}x{height}")
    if actual_rgba != spec["rgba_sha256"]:
        errors.append(f"RGBA sha expected={spec['rgba_sha256']} actual={actual_rgba}")
    if errors:
        raise AssertionError(f"{label}: " + "; ".join(errors))


def verify_source_archive(path: pathlib.Path, manifest: dict):
    raw = path.read_bytes()
    if sha256(raw) != manifest["source_archive_sha256"]:
        raise AssertionError("Source ZIP SHA-256 does not match immutable purchased archive")
    with zipfile.ZipFile(path) as archive:
        for spec in manifest["assets"]:
            info = archive.getinfo(spec["zip_entry"])
            if f"{info.CRC:08x}" != spec["crc32"]:
                raise AssertionError(f"{spec['frame_id']}: source ZIP CRC32 mismatch")
            verify_payload(f"ZIP/{spec['frame_id']}", archive.read(spec["zip_entry"]), spec)


def verify_drawables(manifest: dict):
    for spec in manifest["assets"]:
        path = DRAWABLE_DIR / spec["drawable"]
        if not path.is_file():
            raise AssertionError(f"Missing drawable: {path}")
        verify_payload(f"drawable/{spec['frame_id']}", path.read_bytes(), spec)


def verify_catalog(manifest: dict):
    source = CATALOG.read_text(encoding="utf-8")
    for spec in manifest["assets"]:
        drawable_name = pathlib.Path(spec["drawable"]).stem
        pattern = re.compile(rf"\b(?:GOLD|MINT|PURPLE|GREEN|RED|GOLD_CROWN|CHRISTMAS|HALLOWEEN)\s*->\s*R\.drawable\.{re.escape(drawable_name)}\b")
        if f'"{spec["frame_id"]}"' not in source:
            raise AssertionError(f"Catalog missing frame ID {spec['frame_id']}")
        if not pattern.search(source):
            raise AssertionError(f"Catalog mapping missing drawable {drawable_name} for {spec['frame_id']}")
    if "contentScale = ContentScale.Fit" not in source:
        raise AssertionError("Purchased frame artwork must use ContentScale.Fit")


def verify_apk(path: pathlib.Path, manifest: dict):
    with zipfile.ZipFile(path) as apk:
        by_basename = {}
        for name in apk.namelist():
            base = pathlib.PurePosixPath(name).name
            if base.startswith("style_frame_") and base.endswith(".png"):
                by_basename.setdefault(base, []).append(name)
        for spec in manifest["assets"]:
            matches = by_basename.get(spec["drawable"], [])
            if len(matches) != 1:
                raise AssertionError(f"APK must contain exactly one {spec['drawable']}; found {matches}")
            data = apk.read(matches[0])
            width, height, actual_rgba = rgba_fingerprint(data)
            if (width, height) != (spec["width"], spec["height"]):
                raise AssertionError(f"APK/{spec['frame_id']}: dimensions changed")
            if actual_rgba != spec["rgba_sha256"]:
                raise AssertionError(
                    f"APK/{spec['frame_id']}: artwork mismatch expected RGBA={spec['rgba_sha256']} actual={actual_rgba}"
                )


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-zip", type=pathlib.Path)
    parser.add_argument("--apk", type=pathlib.Path)
    args = parser.parse_args()
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    if args.source_zip:
        verify_source_archive(args.source_zip, manifest)
    verify_drawables(manifest)
    verify_catalog(manifest)
    if args.apk:
        verify_apk(args.apk, manifest)
    print("Frame provenance gate: PASS")
    print(f"Immutable source ZIP SHA-256: {manifest['source_archive_sha256']}")
    print(f"Verified assets: {len(manifest['assets'])}")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"Frame provenance gate: FAIL: {exc}", file=sys.stderr)
        raise
