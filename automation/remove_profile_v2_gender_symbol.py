from pathlib import Path

path = Path("app/src/main/java/com/sonharf/game/ProfileExperienceV2.kt")
text = path.read_text(encoding="utf-8")
old = '''                    p?.gender?.let { gender ->
                        val female = gender.trim().lowercase() in setOf("kadın", "kadin", "female", "woman")
                        val male = gender.trim().lowercase() in setOf("erkek", "male", "man")
                        if (female || male) {
                            Surface(
                                modifier = Modifier.align(Alignment.BottomStart).size(42.dp),
                                shape = CircleShape,
                                color = if (female) Color(0xFFFF76A8) else Color(0xFF439EF2),
                                border = BorderStroke(2.dp, Color.White),
                                shadowElevation = 3.dp,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(if (female) "♀" else "♂", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
'''
if old in text:
    text = text.replace(old, "", 1)
    path.write_text(text, encoding="utf-8")
    print("Removed ProfileExperienceV2 gender symbol badge")
else:
    print("ProfileExperienceV2 gender symbol badge already absent")
