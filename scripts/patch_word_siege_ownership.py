from pathlib import Path
p=Path('app/src/main/java/com/sonharf/game/WordSiegeExperience.kt')
t=p.read_text()
old='import androidx.compose.foundation.BorderStroke\nimport androidx.compose.foundation.background'
new='import androidx.compose.animation.animateColorAsState\nimport androidx.compose.animation.core.tween\nimport androidx.compose.foundation.BorderStroke\nimport androidx.compose.foundation.background\nimport androidx.compose.foundation.border'
if t.count(old)!=1: raise SystemExit('import target mismatch')
t=t.replace(old,new,1)
old='''@Composable
private fun WordSiegeBoardCell(cell: WordSiegeCellDto, pendingLetter: Char?, pending: Boolean, previewArea: Boolean, myOwner: Int, enabled: Boolean, size: Dp, onClick: () -> Unit) {
    val owner = if (pending) myOwner else cell.owner
    val territory = when (owner) {
        1 -> MainUi.Blue.copy(alpha = if (pending) .30f else .17f)
        2 -> SiegePurple.copy(alpha = if (pending) .30f else .17f)
        else -> MainUi.Surface
    }
    val border = when {
        pending -> SiegeTileBorder
        previewArea -> MainUi.Green.copy(alpha = .85f)
        owner == 1 -> MainUi.Blue.copy(alpha = .45f)
        owner == 2 -> SiegePurple.copy(alpha = .45f)
        else -> MainUi.Border
    }
    val letter = pendingLetter?.toString() ?: cell.letter
    Box(Modifier.size(size).padding(1.dp).clip(RoundedCornerShape(4.dp)).background(when { previewArea && !pending -> MainUi.Green.copy(alpha = .12f); letter != null -> territory; else -> MainUi.Surface }).clickable(enabled = enabled && (cell.letter == null || pending), onClick = onClick), contentAlignment = Alignment.Center) {
        Surface(Modifier.fillMaxSize(), color = when { pending -> SiegeTile.copy(alpha = .92f); previewArea -> MainUi.Green.copy(alpha = .08f); else -> Color.Transparent }, shape = RoundedCornerShape(4.dp), border = BorderStroke(if (pending || previewArea) 1.5.dp else .7.dp, border)) {
            Box(contentAlignment = Alignment.Center) {
                if (letter != null) {
                    Text(letter, color = MainUi.Text, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text(wordSiegeLetterValue(letter), color = MainUi.Muted, fontSize = 5.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp))
                } else if (!cell.bonusUsed && cell.bonus != null) {
                    Text(cell.bonus, color = if (cell.bonus in setOf("2H", "3H")) MainUi.Blue else SiegePurple, fontSize = 7.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}'''
new='''@Composable
private fun WordSiegeBoardCell(cell: WordSiegeCellDto, pendingLetter: Char?, pending: Boolean, previewArea: Boolean, myOwner: Int, enabled: Boolean, size: Dp, onClick: () -> Unit) {
    val owner = if (pending) myOwner else cell.owner
    val relation = TrainingBotSupport.ownershipRelation(owner, myOwner)
    val targetFill = when (relation) {
        WordSiegeOwnershipRelation.SELF -> Color(TrainingBotSupport.OWN_FILL_ARGB)
        WordSiegeOwnershipRelation.OPPONENT -> Color(TrainingBotSupport.OPPONENT_FILL_ARGB)
        WordSiegeOwnershipRelation.NEUTRAL -> Color(TrainingBotSupport.NEUTRAL_FILL_ARGB)
    }
    val targetBorder = when (relation) {
        WordSiegeOwnershipRelation.SELF -> Color(TrainingBotSupport.OWN_BORDER_ARGB)
        WordSiegeOwnershipRelation.OPPONENT -> Color(TrainingBotSupport.OPPONENT_BORDER_ARGB)
        WordSiegeOwnershipRelation.NEUTRAL -> MainUi.Border
    }
    val fill by animateColorAsState(targetFill, tween(220), label = "siege-owner-fill")
    val border by animateColorAsState(targetBorder, tween(220), label = "siege-owner-border")
    val letter = pendingLetter?.toString() ?: cell.letter
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier.size(size).padding(1.dp).clip(shape)
            .background(if (letter != null) fill else MainUi.Surface)
            .border(if (owner != 0) 1.2.dp else .7.dp, if (previewArea) MainUi.Green else border, shape)
            .clickable(enabled = enabled && (cell.letter == null || pending), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (pending) Box(Modifier.fillMaxSize().background(SiegeTile.copy(alpha = .34f), shape))
        if (previewArea && !pending) Box(Modifier.fillMaxSize().background(MainUi.Green.copy(alpha = .08f), shape))
        if (letter != null) {
            Text(letter, color = Color(0xFF111827), fontSize = 14.sp, fontWeight = FontWeight.Black)
            Text(wordSiegeLetterValue(letter), color = Color(0xFF374151), fontSize = 5.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp))
            if (owner != 0 && !pending) Box(Modifier.align(Alignment.TopEnd).padding(2.dp).size(4.dp).background(border, CircleShape))
        } else if (!cell.bonusUsed && cell.bonus != null) {
            Text(cell.bonus, color = if (cell.bonus in setOf("2H", "3H")) MainUi.Blue else SiegePurple, fontSize = 7.sp, fontWeight = FontWeight.Black)
        }
    }
}'''
if t.count(old)!=1: raise SystemExit('board target mismatch')
p.write_text(t.replace(old,new,1))
