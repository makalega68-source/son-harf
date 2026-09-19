from __future__ import annotations

import math
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable"
PROFILE_FRAMES_SOURCE = ROOT / "app" / "src" / "main" / "java" / "com" / "sonharf" / "game" / "ProfileFramesV2.kt"

# These photo diameters include a small safety margin below the largest measured clean opening.
FRAMES = [
    ("profile_frame_default_gray.png", 301, "profile_frame_default_gray, 301f / 512f"),
    ("profile_frame_pro_gold.png", 308, "profile_frame_pro_gold, 308f / 512f"),
    ("profile_frame_shop_pink_blossom.png", 287, "profile_frame_shop_pink_blossom, 287f / 512f"),
    ("profile_frame_shop_blue_royal.png", 321, "profile_frame_shop_blue_royal, 321f / 512f"),
    ("profile_frame_shop_amethyst.png", 293, "profile_frame_shop_amethyst, 293f / 512f"),
    ("profile_frame_shop_emerald.png", 293, "profile_frame_shop_emerald, 293f / 512f"),
]

PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
MAX_DIRTY_RATE = 0.015
MAX_STRONG_RATE = 0.003
MAX_EDGE_RATE = 0.01
MIN_VISIBLE_RATE = 0.03
PHOTO_EDGE_SAFETY_PX = 3.0
MIN_DIAGNOSTIC_DIAMETER = 240


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


def photo_rates(alpha: list[list[int]], photo_diameter: int) -> tuple[float, float]:
    radius = photo_diameter / 2.0
    photo_pixels = dirty = strong = 0
    for y in range(512):
        for x in range(512):
            if math.hypot(x - 255.5, y - 255.5) <= radius - PHOTO_EDGE_SAFETY_PX:
                a = alpha[y][x]
                photo_pixels += 1
                if a > 24:
                    dirty += 1
                if a > 96:
                    strong += 1
    if photo_pixels == 0:
        return 1.0, 1.0
    return dirty / photo_pixels, strong / photo_pixels


def largest_safe_diameter(alpha: list[list[int]], configured: int) -> tuple[int, float, float] | None:
    for diameter in range(configured, MIN_DIAGNOSTIC_DIAMETER - 1, -1):
        dirty_rate, strong_rate = photo_rates(alpha, diameter)
        if dirty_rate <= MAX_DIRTY_RATE and strong_rate <= MAX_STRONG_RATE:
            return diameter, dirty_rate, strong_rate
    return None


def validate_frame(file_name: str, photo_diameter: int) -> list[str]:
    errors: list[str] = []
    path = DRAWABLE / file_name
    if not path.is_file():
        return [f"{file_name}: missing file"]

    try:
        width, height, alpha = read_rgba_png(path)
    except Exception as exc:
        return [str(exc)]

    if (width, height) != (512, 512):
        errors.append(f"{file_name}: expected 512x512, got {width}x{height}")
        return errors

    for x, y in ((0, 0), (511, 0), (0, 511), (511, 511)):
        if alpha[y][x] > 8:
            errors.append(f"{file_name}: corner {x},{y} is not transparent (alpha={alpha[y][x]})")
    if alpha[256][256] > 8:
        errors.append(f"{file_name}: center is not transparent (alpha={alpha[256][256]})")

    dirty_rate, strong_rate = photo_rates(alpha, photo_diameter)
    visible_total = visible = 0
    edge_total = edge_visible = 0

    for y in range(512):
        for x in range(512):
            a = alpha[y][x]
            if x % 2 == 0 and y % 2 == 0:
                visible_total += 1
                if a > 64:
                    visible += 1
            if x < 2 or y < 2 or x >= 510 or y >= 510:
                edge_total += 1
                if a > 24:
                    edge_visible += 1

    edge_rate = edge_visible / edge_total
    visible_rate = visible / visible_total

    safe = largest_safe_diameter(alpha, photo_diameter)
    recommendation = ""
    if safe is not None and safe[0] < photo_diameter:
        recommendation = f" recommended<={safe[0]}px (dirty={pct(safe[1])}, strong={pct(safe[2])})"

    print(
        f"{file_name}: photo={photo_diameter}px dirty={pct(dirty_rate)} strong={pct(strong_rate)} "
        f"edge={pct(edge_rate)} visible={pct(visible_rate)}{recommendation}"
    )

    if dirty_rate > MAX_DIRTY_RATE:
        errors.append(f"{file_name}: frame overlays too much of avatar area ({pct(dirty_rate)}){recommendation}")
    if strong_rate > MAX_STRONG_RATE:
        errors.append(f"{file_name}: opaque artwork intrudes into avatar area ({pct(strong_rate)}){recommendation}")
    if edge_rate > MAX_EDGE_RATE:
        errors.append(f"{file_name}: artwork reaches canvas edge and may look clipped ({pct(edge_rate)})")
    if visible_rate < MIN_VISIBLE_RATE:
        errors.append(f"{file_name}: not enough visible frame artwork ({pct(visible_rate)})")
    return errors


def main() -> None:
    errors: list[str] = []
    source = PROFILE_FRAMES_SOURCE.read_text(encoding="utf-8")
    for file_name, diameter, source_marker in FRAMES:
        if source_marker not in source:
            errors.append(f"{file_name}: runtime photo ratio is not locked to validated {diameter}px diameter")
        errors.extend(validate_frame(file_name, diameter))

    if errors:
        print("\nProfile frame visual geometry validation found issues:")
        for error in errors:
            print(f"- {error}")
        raise SystemExit(1)

    print("Profile frame visual geometry validation passed")


if __name__ == "__main__":
    main()
