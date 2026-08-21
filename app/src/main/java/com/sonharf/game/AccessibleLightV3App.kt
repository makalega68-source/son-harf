package com.sonharf.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sonharf.game.data.*
import com.sonharf.game.ui.home.V3HomeRoute

private enum class LightV3Screen { HOME, GAME, STORE, PROFILE, HUB, LEAGUE }

@Composable
fun AccessibleLightV3SonHarfApp() {
    val backend = remember { if (SupabaseProvider.configured) OnlineGameBackend() else null }
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
            if (screen in setOf(LightV3Screen.HOME, LightV3Screen.STORE, LightV3Screen.PROFILE)) {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(selected = screen == LightV3Screen.HOME, onClick = { screen = LightV3Screen.HOME }, icon = { Icon(Icons.Rounded.Home, "Ana Sayfa") }, label = { Text("Ana Sayfa") })
                    NavigationBarItem(selected = screen == LightV3Screen.STORE, onClick = { screen = LightV3Screen.STORE }, icon = { Icon(Icons.Rounded.ShoppingCart, "Mağaza") }, label = { Text("Mağaza") })
                    NavigationBarItem(selected = screen == LightV3Screen.PROFILE, onClick = { screen = LightV3Screen.PROFILE }, icon = { Icon(Icons.Rounded.Person, "Profil") }, label = { Text("Profil") })
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8FAFC))) {
            when (screen) {
                LightV3Screen.HOME -> V3HomeRoute(
                    onStartGameMode = { mode ->
                        when (mode) {
                            "LEAGUE" -> screen = LightV3Screen.LEAGUE
                            "PRACTICE_BOT" -> {
                                SonHarfGameModeState.mode = "normal"
                                gameKey += 1
                                screen = LightV3Screen.GAME
                            }
                            else -> { gameKey += 1; screen = LightV3Screen.GAME }
                        }
                    },
                    onOpenLeague = { screen = LightV3Screen.LEAGUE },
                    onOpenProfile = { screen = LightV3Screen.PROFILE },
                )
                LightV3Screen.GAME -> key(gameKey) {
                    Box(Modifier.fillMaxSize()) {
                        TargetNeonGameScreen()
                        Surface(
                            onClick = { screen = LightV3Screen.HOME },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = .95f),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.padding(12.dp).size(48.dp).align(Alignment.TopStart),
                        ) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Home, "Ana sayfaya dön", tint = Color(0xFF0369A1)) }
                        }
                    }
                }
                LightV3Screen.STORE -> EconomyShopScreen()
                LightV3Screen.PROFILE -> ProfileExperienceScreen()
                LightV3Screen.HUB -> MetaHubScreen()
                LightV3Screen.LEAGUE -> LeaderboardExperienceScreen { screen = LightV3Screen.HOME }
            }
        }
    }

    WinnerFireworkOverlay()
    FriendsQuickAccessOverlay()
    GameInviteOverlay()
    FriendRequestOverlay()
}
