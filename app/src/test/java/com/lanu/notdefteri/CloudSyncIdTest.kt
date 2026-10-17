package com.lanu.notdefteri

import com.lanu.notdefteri.util.CloudSyncId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSyncIdTest {
    @Test
    fun newVaultIdsContainNinetySixRandomBits() {
        repeat(25) {
            val id = CloudSyncId.generate()
            assertTrue(id.matches(Regex("LANU-[0-9A-F]{24}")))
            assertEquals(29, id.length)
        }
    }

    @Test
    fun newVaultIdsDoNotReuseTheLegacyEightCharacterSuffix() {
        val ids = (0 until 50).map { CloudSyncId.generate() }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.none { it.matches(Regex("LANU-[0-9A-F]{8}")) })
    }
}
