from pathlib import Path

p = Path('app/src/test/java/com/sonharf/game/PremierDuelUxRegressionTest.kt')
s = p.read_text(encoding='utf-8')
old = '''        // Chat remains typed/realtime and now has an unread red indicator.\n        assertTrue(screen.contains("Text(pt(language, \\\"SOHBET\\\", \\\"CHAT\\\")"))\n        assertFalse(screen.contains("enabled = !room.isBot"))\n        assertTrue(screen.contains("var hasUnreadChat by remember { mutableStateOf(false) }"))\n        assertTrue(screen.contains("if (latest != null && latest.id != previousId && latest.senderId != backend.currentUserId())"))\n        assertTrue(screen.contains("hasUnreadChat = !showQuickChat"))\n        assertTrue(screen.contains("unreadChat = hasUnreadChat"))\n        assertTrue(screen.contains("Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp).size(10.dp).clip(CircleShape).background(PremierUi.Red)"))\n        assertTrue(screen.contains("hasUnreadChat = false"))\n'''
new = '''        // User-requested arena cleanup removes secondary surrender/chat actions.\n        // Core realtime gameplay and server-authoritative word submission remain intact.\n        assertFalse(screen.contains("Text(pt(language, \\\"SOHBET\\\", \\\"CHAT\\\")"))\n        assertFalse(screen.contains("Modifier.clickable(onClick = onQuickChat)"))\n        assertFalse(screen.contains("Modifier.clickable(onClick = onForfeit)"))\n'''
if s.count(old) != 1:
    raise SystemExit(f'expected one legacy chat regression block, found {s.count(old)}')
p.write_text(s.replace(old, new, 1), encoding='utf-8')
print('updated Premier duel regression contract for requested action cleanup')
