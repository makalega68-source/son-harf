package com.sonharf.game

import androidx.compose.runtime.Composable

/**
 * Single premium shell. ClassicPremiumApp owns the real authenticated profile,
 * league, rewards, social, shop and gameplay navigation; keeping a second static
 * HomeLobby caused the placeholder dashboard seen in production.
 */
@Composable
fun GamePortalApp() {
    ClassicPremiumApp()
}
