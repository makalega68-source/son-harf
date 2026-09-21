from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected 1 exact match, found {count}")
    return text.replace(old, new, 1)


def slice_replace(text: str, start_marker: str, end_marker: str, new: str, label: str) -> str:
    start = text.find(start_marker)
    if start < 0:
        raise RuntimeError(f"{label}: start marker missing")
    end = text.find(end_marker, start + len(start_marker))
    if end < 0:
        raise RuntimeError(f"{label}: end marker missing")
    return text[:start] + new + text[end:]


path = Path("app/src/main/java/com/sonharf/game/PremierWordDuelScreen.kt")
text = path.read_text()

# Lobby hero: remove the gradient-only shell and use the real purchased panel frame.
hero_old = '''        Surface(
            modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent,
        ) {
            Column(
                Modifier.background(
                    Brush.linearGradient(listOf(PremierUi.OceanDeep, PremierUi.Ocean, PremierUi.Sky))
                ).padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
'''
hero_new = '''        PurchasedPanel(
            modifier = Modifier.fillMaxWidth(),
            asset = PurchasedUiAsset.PANEL_LARGE,
            contentPadding = PaddingValues(22.dp),
        ) {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
'''
text = replace_once(text, hero_old, hero_new, "Son Harf lobby hero")
hero_start = text.find('        PurchasedPanel(\n            modifier = Modifier.fillMaxWidth(),\n            asset = PurchasedUiAsset.PANEL_LARGE,', text.find('private fun PremierLobby'))
hero_end = text.find('        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {', hero_start)
if hero_start < 0 or hero_end < 0:
    raise RuntimeError("Son Harf lobby hero range missing")
hero = text[hero_start:hero_end]
hero = hero.replace('color = Color.White.copy(alpha = .82f)', 'color = Color(0xFF765746)')
hero = hero.replace('color = Color.White.copy(alpha = .78f)', 'color = Color(0xFF765746)')
hero = hero.replace('color = Color.White,', 'color = Color(0xFF4A2D20),')
hero = hero.replace('accent = Color.White,', 'accent = PremierUi.Ocean,')
text = text[:hero_start] + hero + text[hero_end:]

play_old = '''        Button(
            onClick = onPlay,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(68.dp),
            shape = RoundedCornerShape(21.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PremierUi.Ocean, contentColor = Color.White),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
        ) {
            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text(pt(language, "OYNA", "PLAY"), fontSize = 21.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
        }
'''
play_new = '''        PurchasedButton(
            text = pt(language, "OYNA", "PLAY"),
            onClick = onPlay,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(68.dp),
            style = PurchasedButtonStyle.PRIMARY,
            leadingAsset = PurchasedUiAsset.ICON_SWORDS,
        )
'''
text = replace_once(text, play_old, play_new, "Son Harf lobby play")

feature_start = '@Composable\nprivate fun PremierFeatureTile('
feature_end = '\n@Composable\nprivate fun PremierLanguageSwitch('
feature_new = '''@Composable
private fun PremierFeatureTile(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String, modifier: Modifier) {
    PurchasedPanel(
        modifier = modifier,
        asset = PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 10.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = PremierUi.Ocean, modifier = Modifier.size(21.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, color = Color(0xFF4A2D20), fontWeight = FontWeight.Black, fontSize = 9.sp, textAlign = TextAlign.Center, maxLines = 1)
            Text(detail, color = Color(0xFF765746), fontSize = 9.sp, textAlign = TextAlign.Center)
        }
    }
}
'''
text = slice_replace(text, feature_start, feature_end, feature_new, "Son Harf feature tile")

language_start = '@Composable\nprivate fun PremierLanguageSwitch('
language_end = '\n@Composable\nprivate fun PremierLoading('
language_new = '''@Composable
private fun PremierLanguageSwitch(language: String, onLanguage: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        PurchasedButton(
            text = "TR",
            onClick = { onLanguage("tr") },
            modifier = Modifier.width(62.dp).height(46.dp),
            style = if (language == "tr") PurchasedButtonStyle.PURPLE else PurchasedButtonStyle.SECONDARY,
        )
        PurchasedButton(
            text = "EN",
            onClick = { onLanguage("en") },
            modifier = Modifier.width(62.dp).height(46.dp),
            style = if (language == "en") PurchasedButtonStyle.PURPLE else PurchasedButtonStyle.SECONDARY,
        )
    }
}
'''
text = slice_replace(text, language_start, language_end, language_new, "Son Harf language switch")

search_old = '''        OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, PremierUi.Border)) {
            Text(pt(language, "İPTAL", "CANCEL"), color = PremierUi.Muted, fontWeight = FontWeight.Black)
        }
'''
search_new = '''        PurchasedButton(
            text = pt(language, "İPTAL", "CANCEL"),
            onClick = onCancel,
            modifier = Modifier.width(190.dp),
            style = PurchasedButtonStyle.DANGER,
            leadingAsset = PurchasedUiAsset.ICON_CLOSE,
        )
'''
text = replace_once(text, search_old, search_new, "Son Harf search cancel")

vs_card_old = '''    Surface(modifier = Modifier.fillMaxWidth().shadow(10.dp, RoundedCornerShape(23.dp)), shape = RoundedCornerShape(23.dp), color = PremierUi.Surface, border = BorderStroke(1.dp, accent.copy(alpha = .22f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
'''
vs_card_new = '''    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_MEDIUM,
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
'''
text = replace_once(text, vs_card_old, vs_card_new, "Son Harf VS player card")

# Arena header and input now sit on purchased frames; gameplay state remains untouched.
header_old = '''    Surface(shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp), color = PremierUi.Surface, shadowElevation = 8.dp, border = BorderStroke(1.dp, PremierUi.Border)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
'''
header_new = '''    PurchasedPanel(
        modifier = Modifier.fillMaxWidth(),
        asset = PurchasedUiAsset.PANEL_LARGE,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
'''
text = replace_once(text, header_old, header_new, "Son Harf arena header")

input_old = '''    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = PremierUi.Surface, border = BorderStroke(2.dp, if (myTurn) PremierUi.Ocean else PremierUi.Border), shadowElevation = if (myTurn) 5.dp else 0.dp) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
'''
input_new = '''    PurchasedPanel(
        modifier = modifier.fillMaxWidth(),
        asset = if (myTurn) PurchasedUiAsset.PANEL_MEDIUM else PurchasedUiAsset.PANEL_SMALL,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
'''
text = replace_once(text, input_old, input_new, "Son Harf input panel")

# Replace the active keyboard implementation entirely: letters + backspace + SEND only.
keyboard_start = '@Composable\nprivate fun PremierKeyboard('
keyboard_end = '\n@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nprivate fun PremierChatSheet('
keyboard_new = '''@Composable
private fun PremierKeyboard(
    language: String,
    value: String,
    enabled: Boolean,
    keyHeight: Dp,
    onInput: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    EmbeddedWordKeyboard(
        value = value,
        language = language,
        enabled = enabled,
        submitEnabled = enabled && value.length >= 2,
        maxLength = 30,
        onValueChange = onInput,
        onSubmit = onSubmit,
        modifier = Modifier.fillMaxWidth(),
        submitLabel = pt(language, "GÖNDER", "SEND"),
        keyHeight = keyHeight,
        keySound = { SonHarfSoundFx.typingClick() },
        actionSound = { SonHarfSoundFx.tap() },
    )
}
'''
text = slice_replace(text, keyboard_start, keyboard_end, keyboard_new, "Son Harf keyboard")

result_panel_old = '''        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(23.dp), color = PremierUi.Surface, border = BorderStroke(1.dp, PremierUi.Border)) {
            Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
'''
result_panel_new = '''        PurchasedPanel(
            modifier = Modifier.fillMaxWidth(),
            asset = PurchasedUiAsset.PANEL_MEDIUM,
            contentPadding = PaddingValues(20.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
'''
text = replace_once(text, result_panel_old, result_panel_new, "Son Harf result panel")

rematch_old = '''        Button(onClick = onRematch, enabled = !busy, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = PremierUi.Ocean)) {
            Icon(Icons.Rounded.Replay, null)
            Spacer(Modifier.width(7.dp))
            Text(if (busy) pt(language, "BEKLENİYOR…", "WAITING…") else pt(language, "HEMEN RÖVANŞ", "INSTANT REMATCH"), fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(9.dp))
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp), border = BorderStroke(1.dp, PremierUi.Border)) {
            Text(pt(language, "ANA MENÜ", "HOME"), color = PremierUi.Muted, fontWeight = FontWeight.Black)
        }
'''
rematch_new = '''        PurchasedButton(
            text = if (busy) pt(language, "BEKLENİYOR…", "WAITING…") else pt(language, "HEMEN RÖVANŞ", "INSTANT REMATCH"),
            onClick = onRematch,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            style = PurchasedButtonStyle.PRIMARY,
            leadingAsset = PurchasedUiAsset.ICON_REPEAT,
        )
        Spacer(Modifier.height(9.dp))
        PurchasedButton(
            text = pt(language, "ANA MENÜ", "HOME"),
            onClick = onHome,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            style = PurchasedButtonStyle.SECONDARY,
            leadingAsset = PurchasedUiAsset.NAV_HOME,
        )
'''
text = replace_once(text, rematch_old, rematch_new, "Son Harf result buttons")

if 'TEMİZLE' in text or 'CLEAR' in text[text.find('private fun PremierKeyboard'):text.find('private fun PremierChatSheet')]:
    raise RuntimeError("Son Harf active keyboard still contains clear key")
if 'EmbeddedWordKeyboard(' not in text:
    raise RuntimeError("Son Harf purchased keyboard missing")
if 'backend.submitPremierWord(active.id, candidate)' not in text:
    raise RuntimeError("Son Harf authoritative submit path unexpectedly missing")

path.write_text(text)
print("Son Harf purchased UI patch applied safely")
