package com.sonharf.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonharf.game.data.OnlineGameBackend
import com.sonharf.game.data.ProfileDto
import com.sonharf.game.data.SupabaseProvider
import kotlinx.coroutines.delay

private enum class PremiumDestination { HOME, GAME, SIEGE, LETTER, LEAGUE, COMPETITION, SOCIAL, SHOP, PROFILE, SETTINGS, ACCOUNT, PROFILE_DETAILS, TASKS, DAILY }

@Composable
fun PremiumUnifiedProApp(onSignedOut: () -> Unit) {
    val backend = remember { OnlineGameBackend() }
    var destination by remember { mutableStateOf(PremiumDestination.HOME) }
    var isPro by remember { mutableStateOf(false) }
    val homeRequest = SonHarfUiState.homeRequest

    LaunchedEffect(Unit) {
        val id = backend.currentUserId()
        if (id != null) {
            runCatching { backend.getEquippedCosmetics() }.getOrNull()?.let(SonHarfCosmetics::apply)
            isPro = runCatching { backend.getProfile(id).isVip }.getOrDefault(false)
        }
    }
    LaunchedEffect(homeRequest) { if (homeRequest > 0) destination = PremiumDestination.HOME }
    LaunchedEffect(destination) {
        if (destination !in setOf(PremiumDestination.GAME, PremiumDestination.SIEGE, PremiumDestination.LETTER, PremiumDestination.DAILY)) {
            while (true) { runCatching { backend.setPresence("online") }; delay(55_000) }
        }
    }

    BackHandler(enabled = destination != PremiumDestination.HOME) {
        destination = when (destination) {
            PremiumDestination.SETTINGS, PremiumDestination.PROFILE_DETAILS -> PremiumDestination.PROFILE
            PremiumDestination.ACCOUNT -> PremiumDestination.SETTINGS
            PremiumDestination.DAILY -> PremiumDestination.TASKS
            else -> PremiumDestination.HOME
        }
    }

    val topLevel = destination in setOf(PremiumDestination.HOME, PremiumDestination.LEAGUE, PremiumDestination.SOCIAL, PremiumDestination.SHOP, PremiumDestination.PROFILE)
    val scheme = if (SonHarfTheme.IsDark) darkColorScheme(primary=SonHarfTheme.Primary, secondary=SonHarfTheme.Turquoise, tertiary=SonHarfTheme.Success, background=SonHarfTheme.Background, surface=SonHarfTheme.Surface, onPrimary=SonHarfTheme.OnPrimary, onBackground=SonHarfTheme.TextPrimary, onSurface=SonHarfTheme.TextPrimary, error=SonHarfTheme.Error) else lightColorScheme(primary=SonHarfTheme.Primary, secondary=SonHarfTheme.Turquoise, tertiary=SonHarfTheme.Success, background=SonHarfTheme.Background, surface=SonHarfTheme.Surface, onPrimary=SonHarfTheme.OnPrimary, onBackground=SonHarfTheme.TextPrimary, onSurface=SonHarfTheme.TextPrimary, error=SonHarfTheme.Error)

    MaterialTheme(colorScheme=scheme) {
        Scaffold(containerColor=SonHarfTheme.Background, topBar={ SonHarfTopAdBanner(isPremium=isPro) }, bottomBar={ if(topLevel) PremiumBottomBar(destination,{destination=PremiumDestination.HOME},{destination=PremiumDestination.LEAGUE},{destination=PremiumDestination.SOCIAL},{destination=PremiumDestination.SHOP},{destination=PremiumDestination.PROFILE}) }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (!SonHarfTheme.IsDark) SonHarfLeafBackdrop(Modifier.matchParentSize())
                when(destination) {
                    PremiumDestination.HOME -> PremiumHomeScreen(backend,{destination=PremiumDestination.GAME},{destination=PremiumDestination.SIEGE},{destination=PremiumDestination.LETTER},{destination=PremiumDestination.LEAGUE},{destination=PremiumDestination.COMPETITION},{destination=PremiumDestination.SOCIAL},{destination=PremiumDestination.SHOP},{destination=PremiumDestination.PROFILE},{destination=PremiumDestination.TASKS})
                    PremiumDestination.GAME -> OnlineGameScreenV6()
                    PremiumDestination.SIEGE -> WordSiegeExperienceScreen { destination=PremiumDestination.HOME }
                    PremiumDestination.LETTER -> LetterLadderGameScreen { destination=PremiumDestination.HOME }
                    PremiumDestination.LEAGUE -> LeaderboardExperienceScreen { destination=PremiumDestination.HOME }
                    PremiumDestination.COMPETITION -> CompetitionHubScreen { destination=PremiumDestination.HOME }
                    PremiumDestination.SOCIAL -> MainSocialScreen(backend=backend,onPlay={destination=PremiumDestination.GAME},onSiege={destination=PremiumDestination.SIEGE})
                    PremiumDestination.SHOP -> ShopHubScreen()
                    PremiumDestination.PROFILE -> MainPlayerProfileScreen(backend,{destination=PremiumDestination.PROFILE_DETAILS},{destination=PremiumDestination.PROFILE},{destination=PremiumDestination.SETTINGS},{destination=PremiumDestination.SOCIAL})
                    PremiumDestination.SETTINGS -> MainSettingsScreen(backend,{destination=PremiumDestination.PROFILE},{destination=PremiumDestination.ACCOUNT},onSignedOut)
                    PremiumDestination.ACCOUNT -> CompleteProfileScreen(1) { destination=PremiumDestination.SETTINGS }
                    PremiumDestination.PROFILE_DETAILS -> CompleteProfileScreen(0) { destination=PremiumDestination.PROFILE }
                    PremiumDestination.TASKS -> MainRetentionScreen(backend,{destination=PremiumDestination.HOME},{destination=PremiumDestination.SIEGE},{destination=PremiumDestination.DAILY})
                    PremiumDestination.DAILY -> DailyCipherScreen { destination=PremiumDestination.TASKS }
                }
            }
        }
    }
}

@Composable
private fun PremiumHomeScreen(backend:OnlineGameBackend,onPlay:()->Unit,onSiege:()->Unit,onLetter:()->Unit,onLeague:()->Unit,onCompetition:()->Unit,onSocial:()->Unit,onShop:()->Unit,onProfile:()->Unit,onTasks:()->Unit) {
    var profile by remember { mutableStateOf<ProfileDto?>(null) }
    LaunchedEffect(Unit) { if(SupabaseProvider.configured) profile=backend.currentUserId()?.let { runCatching { backend.getProfile(it) }.getOrNull() } }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=16.dp,vertical=14.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { PremiumHomeCommandDeck(profile,onProfile,onSiege,onTasks,onCompetition,onLeague,onSocial,onShop) }
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text(sh("DİĞER OYUNLAR","OTHER GAMES"),color=SonHarfTheme.TextPrimary,fontWeight=FontWeight.Black,fontSize=14.sp,modifier=Modifier.weight(1f)); Text(sh("Kısa meydan okumalar","Quick challenges"),color=SonHarfTheme.TextSecondary,fontSize=8.sp) }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                PremiumSecondaryMode(sh("SON HARF","LAST LETTER"),sh("Hızlı kelime düellosu","Fast word duel"),Modifier.weight(1f),onPlay)
                PremiumSecondaryMode(sh("HARF YOLU","LETTER PATH"),sh("Kelime rotanı tamamla","Complete your word path"),Modifier.weight(1f),onLetter)
            }
        }
        item { Spacer(Modifier.height(5.dp)) }
    }
}

@Composable private fun PremiumSecondaryMode(title:String,subtitle:String,modifier:Modifier,onClick:()->Unit) { Surface(modifier.height(88.dp),onClick=onClick,shape=RoundedCornerShape(20.dp),color=SonHarfTheme.Surface.copy(alpha=.96f),border=BorderStroke(1.dp,SonHarfTheme.Border)) { Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.Center) { Text(title,color=SonHarfTheme.TextPrimary,fontWeight=FontWeight.Black,fontSize=11.sp); Spacer(Modifier.height(5.dp)); Text(subtitle,color=SonHarfTheme.TextSecondary,fontSize=8.dp.value.sp) } } }

@Composable private fun PremiumBottomBar(destination:PremiumDestination,onHome:()->Unit,onLeague:()->Unit,onSocial:()->Unit,onShop:()->Unit,onProfile:()->Unit) { NavigationBar(containerColor=SonHarfTheme.NavigationSurface,tonalElevation=0.dp) { listOf(Triple(PremiumDestination.HOME,Icons.Rounded.Home,sh("ANA","HOME")) to onHome,Triple(PremiumDestination.LEAGUE,Icons.Rounded.EmojiEvents,sh("LİG","LEAGUE")) to onLeague,Triple(PremiumDestination.SOCIAL,Icons.Rounded.Groups,sh("SOSYAL","SOCIAL")) to onSocial,Triple(PremiumDestination.SHOP,Icons.Rounded.Storefront,sh("MAĞAZA","SHOP")) to onShop,Triple(PremiumDestination.PROFILE,Icons.Rounded.Person,sh("PROFİL","PROFILE")) to onProfile).forEach { pair -> val item=pair.first; NavigationBarItem(selected=destination==item.first,onClick=pair.second,icon={Icon(item.second,null)},label={Text(item.third,fontSize=8.sp,fontWeight=if(destination==item.first) FontWeight.Bold else FontWeight.Normal)},colors=NavigationBarItemDefaults.colors(selectedIconColor=SonHarfTheme.Primary,selectedTextColor=SonHarfTheme.Primary,indicatorColor=SonHarfTheme.Primary.copy(alpha=.12f),unselectedIconColor=SonHarfTheme.TextSecondary,unselectedTextColor=SonHarfTheme.TextSecondary)) } } }
