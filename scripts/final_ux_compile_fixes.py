from pathlib import Path


def ensure(path: str, old: str, new: str, label: str):
    p = Path(path)
    text = p.read_text()
    if new in text:
        return
    if old not in text:
        raise SystemExit(f'missing compile fix target: {label}')
    p.write_text(text.replace(old, new, 1))
    print(f'fixed {label}')

ensure(
    'app/src/main/java/com/sonharf/game/BilBakalimFeature.kt',
    'import androidx.compose.foundation.BorderStroke\n',
    'import androidx.activity.compose.BackHandler\nimport androidx.compose.foundation.BorderStroke\n',
    'Bil Bakalim BackHandler import',
)

ensure(
    'app/src/main/java/com/sonharf/game/ComboOverlayV9.kt',
    'import androidx.compose.foundation.layout.*\n',
    'import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.lazy.items\n',
    'summary lazy items import',
)
