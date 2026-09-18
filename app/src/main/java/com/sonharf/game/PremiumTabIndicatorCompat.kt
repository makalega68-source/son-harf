package com.sonharf.game

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.TabPosition
import androidx.compose.ui.Modifier

/** Keeps the custom premium tab indicator positioned across Material3 versions. */
internal fun Modifier.tabIndicatorOffset(currentTabPosition: TabPosition): Modifier =
    this
        .offset(x = currentTabPosition.left)
        .width(currentTabPosition.width)
