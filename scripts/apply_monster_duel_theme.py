from pathlib import Path

path = Path('app/src/main/java/com/sonharf/game/LightDuelUi.kt')
text = path.read_text(encoding='utf-8')

replacements = {
    'private val LBg = Color(0xFFF7F9FC)': 'private val LBg = Color(0xFF101114)',
    'private val LCard = Color.White': 'private val LCard = Color(0xFF181A1F)',
    'private val LCard2 = Color(0xFFF0F4F8)': 'private val LCard2 = Color(0xFF25272E)',
    'private val LText = Color(0xFF182235)': 'private val LText = Color(0xFFF7F7F8)',
    'private val LMuted = Color(0xFF718096)': 'private val LMuted = Color(0xFF8E929D)',
    'private val LBlue = Color(0xFF1769E0)': 'private val LBlue = Color(0xFFEAFB17)',
    'private val LBlueSoft = Color(0xFFE8F2FF)': 'private val LBlueSoft = Color(0xFF292D20)',
    'private val LBlue2 = Color(0xFF4A8FE7)': 'private val LBlue2 = Color(0xFFCFE900)',
    'private val LBorder = Color(0xFFDDE5EE)': 'private val LBorder = Color(0xFF2C2F36)',
    'private val LRed = Color(0xFFE24D6B)': 'private val LRed = Color(0xFFFF5B4D)',
    'private val LGold = Color(0xFFF3A81A)': 'private val LGold = Color(0xFFFFC857)',
    'private val LPurple = Color(0xFF7658D6)': 'private val LPurple = Color(0xFF9A86FF)',
    'private val LGreen = Color(0xFF22A85A)': 'private val LGreen = Color(0xFF47C77A)',
    '.background(Brush.verticalGradient(listOf(Color.White, LBg, Color(0xFFF1F6FC))))': '.background(Brush.verticalGradient(listOf(Color(0xFF0D0E11), LBg, Color(0xFF15171C))))',
    '.background(Brush.verticalGradient(listOf(Color.White, LBg, Color(0xFFF2F6FB))))': '.background(Brush.verticalGradient(listOf(Color(0xFF0D0E11), LBg, Color(0xFF15171C))))',
    'listOf(Color(0xFFF3F8FF), Color.White, Color(0xFFF8FAFD))': 'listOf(Color(0xFF202228), Color(0xFF181A1F), Color(0xFF121317))',
    'Modifier.fillMaxSize().clip(CircleShape).background(Color.White)': 'Modifier.fillMaxSize().clip(CircleShape).background(LCard)',
    'colors = CardDefaults.cardColors(containerColor = Color.White)': 'colors = CardDefaults.cardColors(containerColor = LCard)',
    'color = Color.White,\n        border = BorderStroke(3.dp, if (seconds <= 3 && !quizActive) LRed else LBlue)': 'color = LCard,\n        border = BorderStroke(3.dp, if (seconds <= 3 && !quizActive) LRed else LBlue)',
    'color = Color.White,\n        border = BorderStroke(1.dp, if (isVip) LBlue.copy(alpha = .28f) else LBorder)': 'color = LCard,\n        border = BorderStroke(1.dp, if (isVip) LBlue.copy(alpha = .28f) else LBorder)',
    'colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F2FF))': 'colors = CardDefaults.cardColors(containerColor = Color(0xFF201D2B))',
    'color = Color.White,\n        border = BorderStroke(1.5.dp, if (myTurn && !quiz) LBlue else LBorder)': 'color = LCard,\n        border = BorderStroke(1.5.dp, if (myTurn && !quiz) LBlue else LBorder)',
    'color = Color(0xFFF4F7FB),': 'color = Color(0xFF14161A),',
    'color = Color.White,\n        border = BorderStroke(1.dp, if (enabled) Color(0xFFCBD9E8) else Color(0xFFE3E8EE))': 'color = LCard2,\n        border = BorderStroke(1.dp, if (enabled) LBorder else LBorder.copy(alpha = .55f))',
    'containerColor = Color.White,\n            contentColor = LText': 'containerColor = LCard2,\n            contentColor = LText',
    'disabledContainerColor = Color(0xFFE4EAF1)': 'disabledContainerColor = LCard2',
    'focusedContainerColor = Color.White,\n                                unfocusedContainerColor = Color.White': 'focusedContainerColor = LCard,\n                                unfocusedContainerColor = LCard',
    'containerColor = if (matching) Color(0xFFFCE8ED) else LBlue,\n                        contentColor = if (matching) LRed else Color.White': 'containerColor = if (matching) LRed.copy(alpha = .14f) else LBlue,\n                        contentColor = if (matching) LRed else Color(0xFF101114)',
}

for old, new in replacements.items():
    if old in text:
        text = text.replace(old, new)

# Monster uses dark cards and a lime primary action. Keep white only where it is
# deliberately foreground text on vivid colored surfaces.
text = text.replace('colors = ButtonDefaults.buttonColors(containerColor = LBlue),', 'colors = ButtonDefaults.buttonColors(containerColor = LBlue, contentColor = Color(0xFF101114)),')
text = text.replace('Text(sh("DAVET", "INVITE"))', 'Text(sh("DAVET", "INVITE"), color = Color(0xFF101114), fontWeight = FontWeight.Black)')

path.write_text(text, encoding='utf-8')
print('Monster duel theme migration applied')
