from pathlib import Path

path = Path("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt")
text = path.read_text(encoding="utf-8")


def replace_once(old: str, new: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match, found {count}: {old[:100]!r}")
    text = text.replace(old, new, 1)

replace_once(
'''        val compact = maxHeight < 650.dp
        val targetSize = if (compact) 94.dp else 116.dp
''',
'''        val compactHeight = maxHeight < 650.dp
        val narrowWidth = maxWidth < 390.dp
        val compact = compactHeight || narrowWidth
        val horizontalPadding = if (narrowWidth) 8.dp else 12.dp
        val panelMinHeight = if (compact) 118.dp else 150.dp
        val targetSize = when {
            compactHeight && narrowWidth -> 82.dp
            compact -> 94.dp
            else -> 116.dp
        }
''')

replace_once(
'''                onForfeit = onForfeit,
                onQuickChat = onQuickChat,
            )
''',
'''                onForfeit = onForfeit,
                onQuickChat = onQuickChat,
                compact = compact,
                narrow = narrowWidth,
            )
''')

replace_once(
'''                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
''',
'''                contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = if (compact) 5.dp else 7.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
''')

replace_once(
'''                            entries = words.filter { it.playerId == meId }.takeLast(if (compact) 3 else 5).reversed(),
                            modifier = Modifier.weight(1f),
                        )
                        Column(
                            modifier = Modifier.width(targetSize + 16.dp),
''',
'''                            entries = words.filter { it.playerId == meId }.takeLast(if (compact) 3 else 5).reversed(),
                            modifier = Modifier.weight(1f),
                            minHeight = panelMinHeight,
                        )
                        Column(
                            modifier = Modifier.width(targetSize + if (narrowWidth) 8.dp else 16.dp),
''')

replace_once(
'''                            entries = words.takeLast(if (compact) 3 else 5).reversed(),
                            modifier = Modifier.weight(1f),
                        )
''',
'''                            entries = words.takeLast(if (compact) 3 else 5).reversed(),
                            modifier = Modifier.weight(1f),
                            minHeight = panelMinHeight,
                        )
''')

replace_once(
'''                    .padding(horizontal = 12.dp, vertical = 5.dp)
''',
'''                    .padding(horizontal = horizontalPadding, vertical = if (compact) 4.dp else 5.dp)
''')

replace_once(
'''                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
''',
'''                Modifier.fillMaxWidth().padding(start = horizontalPadding, end = horizontalPadding, bottom = if (compact) 6.dp else 8.dp),
                horizontalArrangement = Arrangement.spacedBy(if (narrowWidth) 8.dp else 10.dp),
''')

replace_once(
'''                    modifier = Modifier.weight(1f).height(50.dp),
''',
'''                    modifier = Modifier.weight(1f).height(if (compact) 46.dp else 50.dp),
''')
replace_once(
'''                        modifier = Modifier.fillMaxWidth().height(50.dp),
''',
'''                        modifier = Modifier.fillMaxWidth().height(if (compact) 46.dp else 50.dp),
''')

replace_once(
'''    onQuickChat: () -> Unit,
) {
    val myRating = me?.rating ?: 1000
''',
'''    onQuickChat: () -> Unit,
    compact: Boolean,
    narrow: Boolean,
) {
    val myRating = me?.rating ?: 1000
''')

replace_once(
'''        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
''',
'''        Modifier.fillMaxWidth().padding(horizontal = if (narrow) 8.dp else 12.dp, vertical = if (compact) 6.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
''')

replace_once(
'''            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PremierCalmPlayerCard(
''',
'''            horizontalArrangement = Arrangement.spacedBy(if (narrow) 5.dp else 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PremierCalmPlayerCard(
''')

replace_once(
'''                nameColor = SonHarfCosmetics.playerNameColor,
            )
            Surface(
                modifier = Modifier.width(108.dp),
''',
'''                nameColor = SonHarfCosmetics.playerNameColor,
                compact = compact,
            )
            Surface(
                modifier = Modifier.width(if (narrow) 88.dp else 108.dp),
''')

replace_once(
'''                    Modifier.padding(horizontal = 7.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(pt(language, "Raund ${room.roundNo} / 3", "Round ${room.roundNo} / 3"), color = PremierUi.Ink, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("$myRounds - $rivalRounds", color = PremierUi.Ink, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text(pt(language, "2 raund kazanan\\nmaçı kazanır", "First to 2 rounds\\nwins the match"), color = PremierUi.Muted, fontSize = 7.sp, lineHeight = 9.sp, textAlign = TextAlign.Center)
''',
'''                    Modifier.padding(horizontal = if (narrow) 5.dp else 7.dp, vertical = if (compact) 6.dp else 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(pt(language, "Raund ${room.roundNo} / 3", "Round ${room.roundNo} / 3"), color = PremierUi.Ink, fontSize = if (narrow) 10.sp else 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Text("$myRounds - $rivalRounds", color = PremierUi.Ink, fontSize = if (narrow) 23.sp else 27.sp, fontWeight = FontWeight.Black)
                    Text(pt(language, "2 raund kazanan\\nmaçı kazanır", "First to 2 rounds\\nwins the match"), color = PremierUi.Muted, fontSize = if (narrow) 6.sp else 7.sp, lineHeight = if (narrow) 8.sp else 9.sp, textAlign = TextAlign.Center)
''')

replace_once(
'''                bot = room.isBot,
            )
''',
'''                bot = room.isBot,
                compact = compact,
            )
''')

replace_once(
'''    bot: Boolean = false,
    nameColor: Color = PremierUi.Ink,
) {
''',
'''    bot: Boolean = false,
    nameColor: Color = PremierUi.Ink,
    compact: Boolean = false,
) {
''')

replace_once(
'''        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (bot) PremierBotAvatar(size = 42.dp, accent = accent)
                else ProfilePhotoAvatarRectWithGender(
                    avatarPath = if (visible) avatar else null,
                    gender = gender,
                    name = name,
                    width = 46.dp,
                    height = 40.dp,
                    accent = accent,
                )
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, color = nameColor, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(premierLeagueLabel(rating, language), color = accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("🏆 $rating", color = PremierUi.Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
''',
'''        Column(Modifier.padding(if (compact) 6.dp else 8.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (bot) PremierBotAvatar(size = if (compact) 36.dp else 42.dp, accent = accent)
                else ProfilePhotoAvatarRectWithGender(
                    avatarPath = if (visible) avatar else null,
                    gender = gender,
                    name = name,
                    width = if (compact) 38.dp else 46.dp,
                    height = if (compact) 34.dp else 40.dp,
                    accent = accent,
                )
                Spacer(Modifier.width(if (compact) 4.dp else 6.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, color = nameColor, fontSize = if (compact) 10.sp else 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(premierLeagueLabel(rating, language), color = accent, fontSize = if (compact) 7.sp else 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("🏆 $rating", color = PremierUi.Muted, fontSize = if (compact) 7.sp else 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
''')

replace_once(
'''                Text(score.toString(), color = PremierUi.Ink, fontSize = 21.sp, fontWeight = FontWeight.Black)
''',
'''                Text(score.toString(), color = PremierUi.Ink, fontSize = if (compact) 18.sp else 21.sp, fontWeight = FontWeight.Black)
''')

replace_once(
'''    entries: List<GameWordDto>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.heightIn(min = 150.dp),
''',
'''    entries: List<GameWordDto>,
    modifier: Modifier = Modifier,
    minHeight: Dp = 150.dp,
) {
    Surface(
        modifier = modifier.heightIn(min = minHeight),
''')

path.write_text(text, encoding="utf-8")
print("Son Harf responsive hardening applied")
