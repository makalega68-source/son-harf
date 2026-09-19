from __future__ import annotations

import math
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable"

FRAMES = [
    ("profile_frame_default_gray.png", 301),
    ("profile_frame_pro_gold.png", 312),
    ("profile_frame_shop_pink_blossom.png", 316),
    ("profile_frame_shop_blue_royal.png", 333),
    ("profile_frame_shop_amethyst.png", 316),
    ("profile_frame_shop_emerald.png", 324),
]

PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


def paeth(a: int, b: int, c: int) -> int:
    p = a + b - c
    pa = abs(p - a)
    pb = abs(p - b)
    pc = abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    if pb <= pc:
        return b
    return c


def read_rgba_png(path: Path) -> tuple[int, int, list[list[int]]]:
    raw = path.read_bytes()
    if not raw.startswith(PNG_SIGNATURE):
        raise AssertionError(f"{path.name}: not a PNG")

    pos = len(PNG_SIGNATURE)
    width = height = None
    bit_depth = color_type = interlace = None
    idat = bytearray()

    while pos < len(raw):
        if pos + 12 > len(raw):
            raise AssertionError(f"{path.name}: truncated PNG chunk")
        length = struct.unpack(">I", raw[pos : pos + 4])[0]
        kind = raw[pos + 4 : pos + 8]
        data = raw[pos + 8 : pos + 8 + length]
        pos += 12 + length

        if kind == b"IHDR":
            width, height, bit_depth, color_type, compression, filter_method, interlace = struct.unpack(
                ">IIBBBBB", data
            )
            if compression != 0 or filter_method != 0:
                raise AssertionError(f"{path.name}: unsupported PNG compression/filter method")
        elif kind == b"IDAT":
            idat.extend(data)
        elif kind == b"IEND":
            break

    if width is None or height is None:
        raise AssertionError(f"{path.name}: missing IHDR")
    if bit_depth != 8 or color_type != 6 or interlace != 0:
        raise AssertionError(
            f"{path.name}: expected 8-bit non-interlaced RGBA PNG, got bit_depth={bit_depth}, color_type={color_type}, interlace={interlace}"
        )

    decoded = zlib.decompress(bytes(idat))
    bpp = 4
    stride = width * bpp
    expected = height * (stride + 1)
    if len(decoded) != expected:
        raise AssertionError(f"{path.name}: unexpected decoded PNG size {len(decoded)} != {expected}")

    rows: list[bytearray] = []
    offset = 0
    prev = bytearray(stride)
    for _ in range(height):
        filter_type = decoded[offset]
        offset += 1
        scan = bytearray(decoded[offset : offset + stride])
        offset += stride
        recon = bytearray(stride)

        for i, x in enumerate(scan):
            left = recon[i - bpp] if i >= bpp else 0
            up = prev[i]
            up_left = prev[i - bpp] if i >= bpp else 0
            if filter_type == 0:
                val = x
            elif filter_type == 1:
                val = (x + left) & 0xFF
            elif filter_type == 2:
                val = (x + up) & 0xFF
            elif filter_type == 3:
                val = (x + ((left + up) // 2)) & 0xFF
            elif filter_type == 4:
                val = (x + paeth(left, up, up_left)) & 0xFF
            else:
                raise AssertionError(f"{path.name}: unsupported PNG row filter {filter_type}")
            recon[i] = val

        rows.append(recon)
        prev = recon

    alpha_rows = [[row[x * 4 + 3] for x in range(width)] for row in rows]
    return width, height, alpha_rows


def pct(value: float) -> str:
    return f"{value * 100:.2f}%"


def validate_frame(file_name: str, photo_diameter: int) -> None:
    path = DRAWABLE / file_name
    if not path.is_file():
        raise AssertionError(f"{file_name}: missing file")

    width, height, alpha = read_rgba_png(path)
    if (width, height) != (512, 512):
        raise AssertionError(f"{file_name}: expected 512x512, got {width}x{height}")

    for x, y in ((0, 0), (511, 0), (0, 511), (511, 511)):
        if alpha[y][x] > 8:
            raise AssertionError(f"{file_name}: corner {x},{y} is not transparent (alpha={alpha[y][x]})")
    if alpha[256][256] > 8:
        raise AssertionError(f"{file_name}: center is not transparent (alpha={alpha[256][256]})")

    radius = photo_diameter / 2.0
    photo_pixels = dirty = strong = 0
    visible_total = visible = 0
    edge_total = edge_visible = 0

    for y in range(512):
        for x in range(512):
            a = alpha[y][x]
            d = math.hypot(x - 255.5, y - 255.5)
            if d <= radius - 3.0:
                photo_pixels += 1
                if a > 24:
                    dirty += 1
                if a > 96:
                    strong += 1
            if x % 2 == 0 and y % 2 == 0:
                visible_total += 1
                if a > 64:
                    visible += 1
            if x < 2 or y < 2 or x >= 510 or y >= 510:
                edge_total += 1
                if a > 24:
                    edge_visible += 1

    dirty_rate = dirty / photo_pixels
    strong_rate = strong / photo_pixels
    edge_rate = edge_visible / edge_total
    visible_rate = visible / visible_total

    print(
        f"{file_name}: photo={photo_diameter}px dirty={pct(dirty_rate)} strong={pct(strong_rate)} "
        f"edge={pct(edge_rate)} visible={pct(visible_rate)}"
    )

    if dirty_rate > 0.015:
        raise AssertionError(f"{file_name}: frame overlays too much of avatar area ({pct(dirty_rate)})")
    if strong_rate > 0.003:
        raise AssertionError(f"{file_name}: opaque artwork intrudes into avatar area ({pct(strong_rate)})")
    if edge_rate > 0.01:
        raise AssertionError(f"{file_name}: artwork reaches canvas edge and may look clipped ({pct(edge_rate)})")
    if visible_rate < 0.03:
        raise AssertionError(f"{file_name}: not enough visible frame artwork ({pct(visible_rate)})")


def main() -> None:
    for file_name, diameter in FRAMES:
        validate_frame(file_name, diameter)
    print("Profile frame visual geometry validation passed")


if __name__ == "__main__":
    main()
