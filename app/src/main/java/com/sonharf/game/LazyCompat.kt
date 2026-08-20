package com.sonharf.game

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable

/** Local count-based lazy helper so retention screens stay compatible across Compose BOM revisions. */
fun LazyListScope.items(count: Int, itemContent: @Composable LazyItemScope.(Int) -> Unit) {
    repeat(count.coerceAtLeast(0)) { index -> item { itemContent(index) } }
}
