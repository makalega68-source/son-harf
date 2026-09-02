package com.sonharf.game

import com.sonharf.game.data.SharedDictionaryService
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalDictionaryLiveContractTest {
    @After fun cleanup() = SharedDictionaryService.clearForTests()

    @Test
    fun selAndSerAreAcceptedWhenPresentInCanonicalSnapshot() {
        SharedDictionaryService.installSnapshotForTests("tr", listOf("sel", "ser"))
        assertTrue(SharedDictionaryService.isValidWordBlocking("SEL", "tr"))
        assertTrue(SharedDictionaryService.isValidWordBlocking("SER", "tr"))
    }
}
