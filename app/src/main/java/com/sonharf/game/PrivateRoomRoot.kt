package com.sonharf.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AuroraSonHarfAppPrivateEnhanced() {
    Box(Modifier.fillMaxSize()) {
        AuroraSonHarfAppEnhanced()
        PrivateRoomWaitingLayer()
    }
}
