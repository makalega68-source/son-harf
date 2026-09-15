#!/usr/bin/env python3
import base64
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PAYLOAD = ROOT / 'assets' / 'profile_frame_payloads'
DRAWABLE_NODPI = ROOT / 'app' / 'src' / 'main' / 'res' / 'drawable-nodpi'
DRAWABLE = ROOT / 'app' / 'src' / 'main' / 'res' / 'drawable'

EXPECTED = {
    'profile_frame_wing_silver.webp': 'c7b8e040f4c01d965f61f1abbc45d8c8803920c61b5c78e202936a0fcf4848b6',
    'profile_frame_wing_blue.webp': 'b1cfbc14377bb201694660748b018095dcb472c6dc95f6a213f4a01fb8fc6019',
    'profile_frame_flower_pink_blossom.webp': '49c2f5833f19b0e5ebf5cdfc5ec3a91cf1928a27f88faf54a38bddf23a7d86f1',
    'profile_frame_wing_pink.webp': '5e70bc9cb223e2339b184bd867682ffaf5ef6d8bc4bc71a274be90341d55d40a',
    'profile_frame_wing_gold.webp': 'cb95e26e2f84400dafaf02a4abce49f832ca8527ecdbaa878469f0af96bd221a',
    'profile_frame_wing_aurora.webp': '9b3aa48f7ffda366145c83e859b880fb66399b449b463f1299de52b676e9c00c',
    'profile_frame_round_golden_avatar.webp': 'ac3bd7cd4b9431d2324d756890cc8fd77d2e2d5adbe90b1c6cbbab7a6219a1da',
}

LEGACY_XML = [
    'profile_frame_wing_silver.xml',
    'profile_frame_wing_blue.xml',
    'profile_frame_flower_pink_blossom.xml',
    'profile_frame_wing_pink.xml',
    'profile_frame_wing_gold.xml',
    'profile_frame_wing_aurora.xml',
]


def load_payload(name: str) -> bytes:
    prefix = f'{name}.b64.part'
    parts = sorted(p for p in PAYLOAD.iterdir() if p.name.startswith(prefix))
    if not parts:
        raise SystemExit(f'missing payload chunks for {name}')
    encoded = ''.join(p.read_text(encoding='ascii').strip() for p in parts)
    data = base64.b64decode(encoded, validate=True)
    digest = hashlib.sha256(data).hexdigest()
    if digest != EXPECTED[name]:
        raise SystemExit(f'{name}: sha256 mismatch {digest}')
    if data[:4] != b'RIFF' or data[8:12] != b'WEBP':
        raise SystemExit(f'{name}: not a WebP payload')
    return data


def main() -> None:
    DRAWABLE_NODPI.mkdir(parents=True, exist_ok=True)
    for legacy in LEGACY_XML:
        path = DRAWABLE / legacy
        if path.exists():
            path.unlink()
    old_golden = DRAWABLE_NODPI / 'profile_frame_round_golden_avatar.png'
    if old_golden.exists():
        old_golden.unlink()
    for name in EXPECTED:
        data = load_payload(name)
        out = DRAWABLE_NODPI / name
        out.write_bytes(data)
        print(f'materialized {name} {len(data)} bytes sha256={EXPECTED[name]}')

if __name__ == '__main__':
    main()
