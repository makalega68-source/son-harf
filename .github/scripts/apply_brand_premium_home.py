from pathlib import Path
import subprocess

ROOT = Path('.')
APP = ROOT / 'app/src/main/java/com/sonharf/game'
RES = ROOT / 'app/src/main/res'
BRAND_SOURCE_COMMIT = 'c320effa8475052da6e5b86003c324382bffcbe1'


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding='utf-8')
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{path}: expected exactly one target block, found {count}')
    path.write_text(text.replace(old, new, 1), encoding='utf-8')


def copy_git_blob(spec: str, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    with destination.open('wb') as out:
        subprocess.run(['git', 'show', spec], stdout=out, check=True)


# Bring in only the approved user-facing logo asset. Launcher/startup stay vector-safe.
subprocess.run(['git', 'fetch', 'origin', 'brand/new-icon-logo-20260910'], check=True)
copy_git_blob(
    f'{BRAND_SOURCE_COMMIT}:app/src/main/res/drawable-nodpi/son_harf_brand_logo.webp',
    RES / 'drawable-nodpi/son_harf_brand_logo.webp',
)
copy_git_blob(
    f'{BRAND_SOURCE_COMMIT}:son-harf-logo.webp',
    ROOT / 'son-harf-logo.webp',
)

(APP / 'SonHarfOfficialLogo.kt').write_text('''package com.sonharf.game

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/** Approved user-facing Son Harf logo. Never used by the earliest startup frame. */
@Composable
fun SonHarfOfficialLogo(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(id = R.drawable.son_harf_brand_logo),
        contentDescription = "Son Harf",
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
''', encoding='utf-8')

# The launcher remains a vector drawable, now reduced to the requested initials-only SH monogram.
(RES / 'drawable/son_harf_app_icon_safe.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#F7F3E8"
        android:pathData="M12,4 H96 Q104,4 104,12 V96 Q104,104 96,104 H12 Q4,104 4,96 V12 Q4,4 12,4 Z" />
    <path
        android:fillColor="#416657"
        android:pathData="M16,10 H92 Q98,10 98,16 V92 Q98,98 92,98 H16 Q10,98 10,92 V16 Q10,10 16,10 Z" />
    <path
        android:fillColor="#FFF9EC"
        android:pathData="M23,27 H51 V35 H23 Z M23,35 H31 V50 H23 Z M23,46 H51 V54 H23 Z M43,54 H51 V69 H43 Z M23,69 H51 V77 H23 Z M59,27 H67 V46 H79 V27 H87 V77 H79 V54 H67 V77 H59 Z" />
</vector>
''', encoding='utf-8')

# Language selection: official logo, without the duplicate plain-text title.
stable = APP / 'StableV1App.kt'
replace_once(
    stable,
    '''            SonHarfBrandLogo(size = 82.dp)\n            Spacer(Modifier.height(24.dp))\n            Text(\n                text = "SON HARF",\n                color = MainUi.Text,\n                fontSize = 28.sp,\n                fontWeight = FontWeight.Black,\n            )\n            Spacer(Modifier.height(8.dp))\n''',
    '''            SonHarfOfficialLogo(\n                modifier = Modifier.fillMaxWidth(.66f).height(104.dp),\n            )\n            Spacer(Modifier.height(18.dp))\n''',
)

# Login landing screen: official logo is safe here because StartupLoading has already completed.
auth = APP / 'RequiredAuthGate.kt'
replace_once(
    auth,
    '''                    SonHarfBrandLogo(\n                        modifier = Modifier.fillMaxWidth(.84f).height(190.dp),\n                        size = null,\n                    )\n''',
    '''                    SonHarfOfficialLogo(\n                        modifier = Modifier.fillMaxWidth(.78f).height(174.dp),\n                    )\n''',
)

unified = APP / 'UnifiedProApp.kt'

# Home header: replace the plain SON HARF text with the approved visual identity.
replace_once(
    unified,
    '''                Column(Modifier.weight(1f)) {\n                    Text("SON HARF", color = UnifiedUi.Text, fontSize = 27.sp, fontWeight = FontWeight.Black)\n                    Text("Kelimeyi Sürdür, Rakibini Geç", color = UnifiedUi.Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold)\n                }\n''',
    '''                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {\n                    SonHarfOfficialLogo(\n                        modifier = Modifier.width(158.dp).height(44.dp),\n                    )\n                    Text(\n                        sh("Kelimeyi Sürdür, Rakibini Geç", "Keep the word going, beat your rival"),\n                        color = UnifiedUi.Blue,\n                        fontSize = 10.sp,\n                        fontWeight = FontWeight.Bold,\n                    )\n                }\n''',
)

text = unified.read_text(encoding='utf-8')
start_marker = '@Composable\nprivate fun WeeklyChampionPodium('
end_marker = '@Composable\nprivate fun UnifiedHeroMetric'
start = text.find(start_marker)
end = text.find(end_marker, start + 1)
if start < 0 or end < 0 or end <= start:
    raise SystemExit('UnifiedProApp.kt: weekly podium function region not found safely')

premium_weekly = r'''@Composable
private fun WeeklyChampionPodium(
    players: List<WeeklyPodiumPlayer>,
    loading: Boolean,
    onOpenLeague: () -> Unit,
) {
    val premiumGold = Color(0xFFF0CF75)
    val deepForest = Color(0xFF18342E)
    val deepBlue = Color(0xFF263E47)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(28.dp))
            .sonHarfPressScale(pressedScale = 0.99f)
            .clickable(onClick = onOpenLeague),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, premiumGold.copy(alpha = .52f)),
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(deepForest, deepBlue)))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = premiumGold.copy(alpha = .14f),
                    border = BorderStroke(1.dp, premiumGold.copy(alpha = .28f)),
                ) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = premiumGold,
                        modifier = Modifier.padding(9.dp).size(22.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        sh("HAFTANIN ZİRVESİ", "WEEKLY PODIUM"),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = .6.sp,
                    )
                    Text(
                        sh("Haftanın en güçlü 3 oyuncusu", "The week's top 3 players"),
                        color = Color.White.copy(alpha = .68f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = Color.White.copy(alpha = .08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .10f)),
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(sh("TÜMÜ", "ALL"), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Rounded.ChevronRight, null, tint = premiumGold, modifier = Modifier.size(14.dp))
                    }
                }
            }

            if (loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = premiumGold,
                    trackColor = Color.White.copy(alpha = .10f),
                )
            }

            if (players.isEmpty()) {
                WeeklyPodiumEmptyState(loading = loading, gold = premiumGold)
            } else {
                WeeklyChampionHero(player = players.first(), gold = premiumGold)

                val second = players.getOrNull(1)
                val third = players.getOrNull(2)
                if (second != null || third != null) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (second != null) {
                            WeeklyRunnerCard(
                                place = 2,
                                player = second,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (third != null) {
                            WeeklyRunnerCard(
                                place = 3,
                                player = third,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (second != null && third == null) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyChampionHero(
    player: WeeklyPodiumPlayer,
    gold: Color,
) {
    val profile = player.profile
    val avatarPath = if (profile?.avatarVisibility == "hidden") null else profile?.avatarPath
    val name = player.row.displayName.ifBlank { sh("Oyuncu", "Player") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = .085f),
        border = BorderStroke(1.5.dp, gold.copy(alpha = .70f)),
        shadowElevation = 5.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfilePhotoAvatarWithGender(
                avatarPath = avatarPath,
                gender = profile?.gender,
                name = name,
                size = 64.dp,
                accent = gold,
                visible = profile?.avatarVisibility != "hidden",
                showGenderBadge = false,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    sh("HAFTA ŞAMPİYONU", "WEEKLY CHAMPION"),
                    color = gold,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = .9.sp,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    name,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${player.row.rating} RP",
                        color = Color.White.copy(alpha = .76f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (profile?.isVip == true) {
                        Spacer(Modifier.width(7.dp))
                        Surface(shape = RoundedCornerShape(99.dp), color = gold.copy(alpha = .16f)) {
                            Text(
                                "PRO",
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                color = gold,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(17.dp),
                color = gold,
                shadowElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.EmojiEvents, null, tint = Color(0xFF725200), modifier = Modifier.size(17.dp))
                    Text("#1", color = Color(0xFF725200), fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun WeeklyRunnerCard(
    place: Int,
    player: WeeklyPodiumPlayer,
    modifier: Modifier,
) {
    val accent = if (place == 2) Color(0xFFC8D0D6) else Color(0xFFC98B62)
    val profile = player.profile
    val avatarPath = if (profile?.avatarVisibility == "hidden") null else profile?.avatarPath
    val name = player.row.displayName.ifBlank { sh("Oyuncu", "Player") }

    Surface(
        modifier = modifier.height(108.dp),
        shape = RoundedCornerShape(19.dp),
        color = Color.White.copy(alpha = .065f),
        border = BorderStroke(1.dp, accent.copy(alpha = .54f)),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.MilitaryTech,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    sh("$place. SIRA", "#$place PLACE"),
                    color = accent,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = .5.sp,
                )
                Spacer(Modifier.weight(1f))
                if (profile?.isVip == true) {
                    Text("PRO", color = accent, fontSize = 6.sp, fontWeight = FontWeight.Black)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfilePhotoAvatarWithGender(
                    avatarPath = avatarPath,
                    gender = profile?.gender,
                    name = name,
                    size = 40.dp,
                    accent = accent,
                    visible = profile?.avatarVisibility != "hidden",
                    showGenderBadge = false,
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        name,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${player.row.rating} RP",
                        color = Color.White.copy(alpha = .62f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyPodiumEmptyState(
    loading: Boolean,
    gold: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = .06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .10f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = gold.copy(alpha = .12f)) {
                Icon(
                    Icons.Rounded.EmojiEvents,
                    null,
                    tint = gold.copy(alpha = .82f),
                    modifier = Modifier.padding(9.dp).size(18.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (loading) sh("Sıralama yükleniyor", "Loading rankings") else sh("Haftalık sıralama hazırlanıyor", "Weekly ranking is taking shape"),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (loading) sh("Güncel ilk 3 oyuncu getiriliyor…", "Fetching the current top three…") else sh("İlk sonuçlar geldiğinde şampiyonlar burada görünecek.", "Champions will appear here as soon as results arrive."),
                    color = Color.White.copy(alpha = .58f),
                    fontSize = 8.sp,
                )
            }
        }
    }
}

'''

unified.write_text(text[:start] + premium_weekly + text[end:], encoding='utf-8')

# Safety assertions: launcher/startup must remain raster-independent.
manifest = (RES.parent / 'AndroidManifest.xml').read_text(encoding='utf-8')
if '@drawable/son_harf_app_icon_safe' not in manifest:
    raise SystemExit('Launcher icon safety contract was lost')
if 'painterResource(' in (APP / 'SonHarfBrandLogo.kt').read_text(encoding='utf-8'):
    raise SystemExit('Startup-safe SonHarfBrandLogo must remain raster-independent')
if (RES / 'drawable/son_harf_app_icon.webp').exists():
    raise SystemExit('Unsafe raster launcher drawable must not be reintroduced')
