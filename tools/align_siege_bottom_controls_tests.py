from pathlib import Path


def patch(path, old, new, label):
    p = Path(path)
    text = p.read_text()
    if old not in text:
        raise SystemExit(f'{label}: expected text not found')
    p.write_text(text.replace(old, new))

patch(
    'app/src/test/java/com/sonharf/game/SimpleKelimeTahtiRestoreContractTest.kt',
    'assertTrue(online.contains("modifier = Modifier.weight(1f).height(52.dp)"))',
    'assertTrue(online.contains("modifier = Modifier.weight(1f).height(40.dp)"))',
    'compact confirm contract',
)
patch(
    'app/src/test/java/com/sonharf/game/WordSiegeCaptureMotionTest.kt',
    '''        assertEquals(85L, wordSiegeCaptureStaggerDelayMs(1))
        assertEquals(255L, wordSiegeCaptureStaggerDelayMs(3))''',
    '''        assertEquals(95L, wordSiegeCaptureStaggerDelayMs(1))
        assertEquals(285L, wordSiegeCaptureStaggerDelayMs(3))''',
    'capture stagger contract',
)
patch(
    'app/src/test/java/com/sonharf/game/WordSiegeFinalRulesTest.kt',
    '''        assertTrue(practice.contains("Torba ${'$'}{state.bag.length}"))
        assertTrue(pan.contains("Torba ${'$'}{game.bag.length}"))''',
    '''        assertTrue(practice.contains("WordSiegePracticeBagButton("))
        assertTrue(pan.contains("WordSiegeOnlineBagButton("))''',
    'bag UI contract',
)
patch(
    'app/src/test/java/com/sonharf/game/WordSiegePremiumScorePenaltyContractTest.kt',
    '''        assertTrue(ui.contains("size = 50.dp"))
        assertTrue(ui.contains("modifier = modifier.height(96.dp)"))''',
    '''        assertTrue(ui.contains("size = 52.dp"))
        assertTrue(ui.contains("modifier = modifier.height(92.dp)"))''',
    'premium profile contract',
)
patch(
    'app/src/test/java/com/sonharf/game/WordSiegeStableRegressionTest.kt',
    '''        assertTrue(pan.contains("Torba ${'$'}{game.bag.length}"))''',
    '''        assertTrue(pan.contains("WordSiegeOnlineBagButton("))''',
    'stable online bag contract',
)
patch(
    'app/src/test/java/com/sonharf/game/WordSiegeStableRegressionTest.kt',
    '''        assertTrue(practice.contains("Torba ${'$'}{state.bag.length}"))''',
    '''        assertTrue(practice.contains("WordSiegePracticeBagButton("))''',
    'stable practice bag contract',
)

# Add explicit checks for the requested bottom-control redesign and slower readable flight.
p = Path('app/src/test/java/com/sonharf/game/WordSiegePremiumScorePenaltyContractTest.kt')
text = p.read_text()
needle = '        assertTrue(pan.contains("pendingRivalLossPoints"))\n'
insert = '''        assertTrue(pan.contains("pendingRivalLossPoints"))
        assertTrue(practice.contains("WordSiegeTurnStrip("))
        assertTrue(pan.contains("WordSiegeTurnStrip("))
        assertTrue(practice.contains("WordSiegePracticeBagButton("))
        assertTrue(pan.contains("WordSiegeOnlineBagButton("))
'''
if needle not in text:
    raise SystemExit('new control contract insertion point missing')
p.write_text(text.replace(needle, insert, 1))

print('Siege bottom-control tests aligned.')
