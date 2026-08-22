from pathlib import Path

mini = Path('app/src/main/java/com/sonharf/game/MiniMascot3D.kt')
text = mini.read_text()


def once(old: str, new: str, label: str):
    global text
    if new in text:
        return
    if old not in text:
        raise SystemExit(f'missing anchor: {label}')
    text = text.replace(old, new, 1)

# Legacy ArenaV10 calls do not opt in, so they render nothing. The global host passes enabled=true.
once(
'''internal fun MiniMascot3D(\n    mood: MiniMood,\n    modifier: Modifier = Modifier,\n) {\n    var renderer by remember { mutableStateOf<MiniRenderer?>(null) }''',
'''internal fun MiniMascot3D(\n    mood: MiniMood,\n    modifier: Modifier = Modifier,\n    enabled: Boolean = false,\n) {\n    if (!enabled) return\n    var renderer by remember { mutableStateOf<MiniRenderer?>(null) }''',
'opt-in mascot flag',
)

# Make the procedural character match the approved hero concept more closely:
# larger head, smaller body, larger glossy blue eyes, softer muzzle and visible smile.
once(
'''        drawSphere(0f, -0.55f + bob, 0f, 0.78f, 0.92f, 0.62f, WHITE)\n        drawSphere(0f, 0.52f + bob, 0.03f, 1.02f, 0.88f, 0.86f, WHITE, rz = sway)''',
'''        drawSphere(0f, -0.62f + bob, 0f, 0.68f, 0.82f, 0.58f, WHITE)\n        drawSphere(0f, 0.54f + bob, 0.03f, 1.16f, 0.98f, 0.92f, WHITE, rz = sway)''',
'hero proportions',
)
once(
'''        drawSphere(-0.18f, 0.28f + bob, 0.76f, 0.34f, 0.24f, 0.18f, MUZZLE)\n        drawSphere(0.18f, 0.28f + bob, 0.76f, 0.34f, 0.24f, 0.18f, MUZZLE)\n        drawSphere(0f, 0.38f + bob, 0.91f, 0.12f, 0.09f, 0.08f, NOSE)''',
'''        drawSphere(-0.19f, 0.25f + bob, 0.80f, 0.38f, 0.27f, 0.18f, MUZZLE)\n        drawSphere(0.19f, 0.25f + bob, 0.80f, 0.38f, 0.27f, 0.18f, MUZZLE)\n        drawSphere(0f, 0.37f + bob, 0.96f, 0.13f, 0.10f, 0.08f, NOSE)\n        drawSphere(0f, 0.08f + bob, 0.91f, 0.24f, 0.16f, 0.07f, MOUTH)\n        if (!sad) drawSphere(0f, 0.03f + bob, 0.98f, 0.12f, 0.08f, 0.04f, TONGUE)''',
'hero face and smile',
)
once(
'''            drawSphere(x, eyeY + bob, 0.76f, 0.30f, eyeScaleY, 0.12f, EYE_BLUE)\n            drawSphere(x, eyeY + bob, 0.86f, 0.15f, eyeScaleY * 0.62f, 0.07f, PUPIL)''',
'''            drawSphere(x, eyeY + bob, 0.80f, 0.36f, eyeScaleY * 1.12f, 0.14f, EYE_BLUE)\n            drawSphere(x, eyeY + bob, 0.92f, 0.19f, eyeScaleY * 0.68f, 0.08f, PUPIL)''',
'larger hero eyes',
)
once(
'''        private val NOSE = floatArrayOf(0.95f, 0.42f, 0.50f, 1f)\n        private val EYE_BLUE''',
'''        private val NOSE = floatArrayOf(0.95f, 0.42f, 0.50f, 1f)\n        private val MOUTH = floatArrayOf(0.18f, 0.06f, 0.10f, 1f)\n        private val TONGUE = floatArrayOf(1f, 0.46f, 0.58f, 1f)\n        private val EYE_BLUE''',
'face palette',
)

mini.write_text(text)

# Code-only mascot: SceneView is no longer required by the app source.
gradle = Path('app/build.gradle.kts')
g = gradle.read_text()
g = g.replace('    implementation("io.github.sceneview:sceneview:4.17.0")\n', '')
gradle.write_text(g)

print('hero code mascot patch applied')
