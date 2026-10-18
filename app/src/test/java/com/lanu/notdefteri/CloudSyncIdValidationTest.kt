package com.lanu.notdefteri

import com.lanu.notdefteri.util.CloudSyncId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSyncIdValidationTest {
    @Test
    fun validExistingAndGeneratedIdentifiersRemainUsable() {
        assertEquals("ABC123", CloudSyncId.normalizeManual(" abc123 "))
        assertEquals("LANU-1234ABCD", CloudSyncId.normalizeManual("LANU-1234ABCD"))
        assertEquals("A_B-7", CloudSyncId.normalizeManual(" a_b-7 "))
        assertTrue(CloudSyncId.normalizeManual(CloudSyncId.generate()) != null)
    }

    @Test
    fun invalidFirestorePathsAreRejected() {
        listOf("", "  ", "/", "abc/def", "abc\\def", "abc.def",
            "spaces are invalid", "abc\nxyz", "__reserved__",
            "a".repeat(129)).forEach { bad ->
            assertNull("Should reject unsafe sync ID: $bad", CloudSyncId.normalizeManual(bad))
        }
    }

    @Test
    fun oldIdIsNotImplicitlyRotatedDuringInputValidation() {
        val prior = "ABC-12345"
        assertNull(CloudSyncId.normalizeManual("broken/id"))
        assertEquals(prior, CloudSyncId.normalizeManual(prior))
    }
}
