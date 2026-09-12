package com.sonharf.game.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryReleaseCacheCoordinatorTest {
    @Test
    fun checksumIsDeterministicAndDeduplicatesWords() {
        val a = DictionaryReleaseCacheCoordinator.deterministicChecksum(
            listOf("dog", "apple", "cat", "cat"),
        )
        val b = DictionaryReleaseCacheCoordinator.deterministicChecksum(
            listOf("cat", "dog", "apple"),
        )

        assertEquals("e89ee7ef71b37bc54ce1ad32ea50a210fcbbe44b7681af457fc2667bb19dc31e", a)
        assertEquals(a, b)
    }

    @Test
    fun checksumUsesUtf8ByteLengthForTurkishWords() {
        assertEquals(
            "a09922cdc1193d16c40695da24c581d0e4b58504668c079324de496c2fd70712",
            DictionaryReleaseCacheCoordinator.deterministicChecksum(listOf("ışık", "el", "gül", "el")),
        )
    }

    @Test
    fun cacheRefreshesOnReleaseChecksumOrLocalContentChange() {
        val release = "release-a"
        val checksum = "a".repeat(64)

        assertFalse(
            DictionaryReleaseCacheCoordinator.needsRefresh(
                storedReleaseId = release,
                storedChecksum = checksum,
                localChecksum = checksum,
                activeReleaseId = release,
                activeChecksum = checksum,
            ),
        )
        assertTrue(
            DictionaryReleaseCacheCoordinator.needsRefresh(
                storedReleaseId = "release-old",
                storedChecksum = checksum,
                localChecksum = checksum,
                activeReleaseId = release,
                activeChecksum = checksum,
            ),
        )
        assertTrue(
            DictionaryReleaseCacheCoordinator.needsRefresh(
                storedReleaseId = release,
                storedChecksum = "b".repeat(64),
                localChecksum = checksum,
                activeReleaseId = release,
                activeChecksum = checksum,
            ),
        )
        assertTrue(
            DictionaryReleaseCacheCoordinator.needsRefresh(
                storedReleaseId = release,
                storedChecksum = checksum,
                localChecksum = null,
                activeReleaseId = release,
                activeChecksum = checksum,
            ),
        )
    }
}
