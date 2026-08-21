package com.sonharf.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.sonharf.game.data.*
import kotlinx.coroutines.launch

private enum class LightV3Screen { HOME, GAME, STORE, FRIENDS, VIP, PROFILE, PREFERENCES, HUB, LEAGUE }

@Composable
fun AccessibleLightV3SonHarfApp() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf(LightV3Screen.HOME) }
    var authChecked by remember { mutableStateOf(false) }
    var authenticated by remember { mutableStateOf(false) }
    var gameKey by remember { mutableIntStateOf(0) }
    val lobbyRequest = SonHarfGameNavigation.lobbyRequest

    LaunchedEffect(Unit) {
        authenticated = SupabaseProvider.configured && hasVerifiedMembershipSession()
        authChecked = true
    }
    LaunchedEffect(authenticated) {
        if (!authenticated) return@LaunchedEffect
        runCatching { backend?.getPreferredGameMode() }.getOrNull()?.let { SonHarfGameModeState.mode = it }
        SonHarfCosmetics.apply(runCatching { backend?.getEquippedCosmetics() }.getOrNull())
    }
    LaunchedEffect(lobbyRequest) {
        if (authenticated && lobbyRequest > 0) {
            gameKey += 1
            screen = LightV3Screen.GAME
        }
    }

    if (!authChecked) {
        Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF0284C7))
        }
        return
    }
    if (!authenticated) {
        RequiredAuthGate { authenticated = true; screen = LightV3Screen.HOME }
        return
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        bottomBar = {
            if (screen in setOf(LightV3Screen.HOME, LightV3Screen.STORE, LightV3Screen.FRIENDS, LightV3Screen.VIP, LightV3Screen.PROFILE, LightV3Screen.PREFERENCES)) {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(selected = screen == LightV3Screen.HOME, onClick = { screen = LightV3Screen.HOME }, icon = { Icon(Icons.Rounded.Home, "Ana Sayfa") }, label = { Text("Ana Sayfa") })
                    NavigationBarItem(selected = screen == LightV3Screen.FRIENDS, onClick = { screen = LightV3Screen.FRIENDS }, icon = { Icon(Icons.Rounded.Group, "Arkadaşlar") }, label = { Text("Arkadaşlar") })
                    NavigationBarItem(selected = screen == LightV3Screen.STORE, onClick = { screen = LightV3Screen.STORE }, icon = { Icon(Icons.Rounded.ShoppingCart, "Mağaza") }, label = { Text("Mağaza") })
                    NavigationBarItem(selected = screen == LightV3Screen.VIP, onClick = { screen = LightV3Screen.VIP }, icon = { Icon(Icons.Rounded.WorkspacePremium, "VIP", tint = Color(0xFF7C3AED)) }, label = { Text("VIP") })
                    NavigationBarItem(selected = screen == LightV3Screen.PROFILE || screen == LightV3Screen.PREFERENCES, onClick = { screen = LightV3Screen.PROFILE }, icon = { Icon(Icons.Rounded.Person, "Profil") }, label = { Text("Profil") })
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8FAFC))) {
            when (screen) {
                LightV3Screen.HOME -> V6HomeRoute(
                    onStartGameMode = { mode ->
                        when (mode) {
                            "LEAGUE" -> screen = LightV3Screen.LEAGUE
                            "EXPERT_MATCH" -> scope.launch {
                                runCatching { backend?.setPreferredGameMode("expert") }
                                SonHarfPreferences.setBotDifficulty(context, "hard")
                                SonHarfGameModeState.mode = "expert"
                                gameKey += 1
                                screen = LightV3Screen.GAME
                            }
                            "NORMAL_MATCH", "PRACTICE_BOT" -> scope.launch {
                                runCatching { backend?.setPreferredGameMode("normal") }
                                if (mode == "PRACTICE_BOT") SonHarfPreferences.setBotDifficulty(context, "normal")
                                SonHarfGameModeState.mode = "normal"
                                gameKey += 1
                                screen = LightV3Screen.GAME
                            }
                            else -> scope.launch {
                                runCatching { backend?.setPreferredGameMode("normal") }
                                SonHarfGameModeState.mode = "normal"
                                gameKey += 1
                                screen = LightV3Screen.GAME
                            }
                        }
                    },
                    onOpenLeague = { screen = LightV3Screen.LEAGUE },
                    onOpenProfile = { screen = LightV3Screen.PROFILE },
                )
                LightV3Screen.GAME -> key(gameKey) {
                    Box(Modifier.fillMaxSize()) {
                        V8BattleScreenFixed(onLeaveBattle = { screen = LightV3Screen.HOME })
                        if (SonHarfGameModeState.mode == "expert") ExpertArenaOverlay()
                    }
                }
                LightV3Screen.STORE -> V4StoreScreen()
                LightV3Screen.FRIENDS -> V7FriendsScreen { screen = LightV3Screen.HOME }
                LightV3Screen.VIP -> V7VipHubScreen { screen = LightV3Screen.HOME }
                LightV3Screen.PROFILE -> V9ProfilePhotoScreen(onOpenPreferences = { screen = LightV3Screen.PREFERENCES })
                LightV3Screen.PREFERENCES -> V4PreferencesScreen(onBack = { screen = LightV3Screen.PROFILE })
                LightV3Screen.HUB -> MetaHubScreen()
                LightV3Screen.LEAGUE -> V6LeaderboardScreen { screen = LightV3Screen.HOME }
            }
        }
    }

    WinnerFireworkOverlay()
    GameInviteOverlay()
    FriendRequestOverlay()
}
