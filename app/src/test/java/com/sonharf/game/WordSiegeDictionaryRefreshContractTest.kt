package com.sonharf.game

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSiegeDictionaryRefreshContractTest {
    @Test fun persistedSnapshotIsUsableOfflineButNeverSkipsOnlineRefresh() {
        val source = File("src/main/java/com/sonharf/game/WordSiegePracticeScreen.kt").readText()
        val restoreIndex = source.indexOf("val restored = SharedDictionaryService.restorePersisted")
        val refreshIndex = source.indexOf("SharedDictionaryService.preloadCanonical(context, state.language)")

        assertTrue(restoreIndex >= 0)
        assertTrue(refreshIndex > restoreIndex)
        assertTrue(source.contains("dictionaryReady = restored"))
        assertTrue(source.contains("dictionaryReady = true"))
    }
}
