from pathlib import Path

TARGET = Path("app/src/main/java/com/sonharf/game/MonsterExperienceApp.kt")

text = TARGET.read_text(encoding="utf-8")
original = text

replacements = {
    "    val Background = Color(0xFFF7FAFF)": "    val Background = Color(0xFF07111F)",
    "    val Surface = Color(0xFFFFFFFF)": "    val Surface = Color(0xFF111D2E)",
    "    val SurfaceRaised = Color(0xFFF0F5FC)": "    val SurfaceRaised = Color(0xFF17263A)",
    "    val SurfaceSoft = Color(0xFFE8EEF7)": "    val SurfaceSoft = Color(0xFF20324A)",
    "    val Text = Color(0xFF10213A)": "    val Text = Color(0xFFF5F8FC)",
    "    val Muted = Color(0xFF62758F)": "    val Muted = Color(0xFF9AAAC0)",
    "    val Border = Color(0xFFD5E2F0)": "    val Border = Color(0xFF2A4260)",
    "    val Accent: Color get() = if (SonHarfCosmetics.monsterBlueTheme) Color(0xFF1677FF) else Color(0xFF64748B)": "    val Accent: Color get() = if (SonHarfCosmetics.monsterBlueTheme) Color(0xFF2F9BFF) else Color(0xFF268CFF)",
    "    val Live = Color(0xFFFF4D4F)": "    val Live = Color(0xFFFF5A64)",
    "    val Coral = Color(0xFFFF6B61)": "    val Coral = Color(0xFFFF6D75)",
    "    val Orange = Color(0xFFF59E0B)": "    val Orange = Color(0xFFFFA928)",
    "    val Green = Color(0xFF168A55)": "    val Green = Color(0xFF35D28A)",
    "    val Gold = Color(0xFFD68A00)": "    val Gold = Color(0xFFFFC857)",
    "Surface(shape = RoundedCornerShape(20.dp), color = MonsterUi.Surface, border = BorderStroke(1.dp, MonsterUi.Border))": "Surface(shape = RoundedCornerShape(22.dp), color = MonsterUi.SurfaceRaised, border = BorderStroke(1.dp, MonsterUi.Border))",
    "modifier = Modifier.fillMaxWidth().height(52.dp),\n                shape = RoundedCornerShape(14.dp),": "modifier = Modifier.fillMaxWidth().height(56.dp),\n                shape = RoundedCornerShape(16.dp),",
    "Surface(modifier = modifier.height(128.dp).clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = MonsterUi.Surface, border = BorderStroke(1.dp, MonsterUi.Border))": "Surface(modifier = modifier.height(128.dp).clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = MonsterUi.SurfaceRaised, border = BorderStroke(1.dp, MonsterUi.Border))",
    "Surface(modifier = modifier, shape = RoundedCornerShape(15.dp), color = MonsterUi.Surface, border = BorderStroke(1.dp, MonsterUi.Border))": "Surface(modifier = modifier, shape = RoundedCornerShape(15.dp), color = MonsterUi.SurfaceRaised, border = BorderStroke(1.dp, MonsterUi.Border))",
    "Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), color = MonsterUi.Surface, border = BorderStroke(1.dp, MonsterUi.Border))": "Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), color = MonsterUi.SurfaceRaised, border = BorderStroke(1.dp, MonsterUi.Border))",
}

for old, new in replacements.items():
    if old not in text:
        raise SystemExit(f"Action UI contract drift: expected fragment not found: {old}")
    text = text.replace(old, new)

# Marker keeps the adaptation traceable for future licensing/due-diligence review.
marker = "// SON HARF ACTION UI ADAPTATION: original Compose implementation; no third-party binary assets bundled.\n"
if marker not in text:
    text = text.replace("internal object MonsterUi {\n", marker + "internal object MonsterUi {\n", 1)

if text != original:
    TARGET.write_text(text, encoding="utf-8")
    print("Applied Son Harf Action UI adaptation to active V1 shell.")
else:
    print("Son Harf Action UI adaptation already applied.")
