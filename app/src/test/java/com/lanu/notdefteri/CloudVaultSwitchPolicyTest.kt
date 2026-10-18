package com.lanu.notdefteri

import com.lanu.notdefteri.util.CloudVaultSwitchPolicy
import com.lanu.notdefteri.util.FirestoreSyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudVaultSwitchPolicyTest {
    @Test
    fun switchingVaultResetsOldSyncMetadataOnly() {
        val previous = FirestoreSyncState(
            syncId = "OLD-VAULT",
            lastSyncTime = 123456L,
            cloudNotesCount = 42,
            isAutoSyncEnabled = true,
            isOnline = false,
            customProjectId = "user-project",
            error = "previous error"
        )
        val next = CloudVaultSwitchPolicy.reset(previous, "NEW-VAULT")
        assertEquals("NEW-VAULT", next.syncId)
        assertNull(next.lastSyncTime)
        assertEquals(0, next.cloudNotesCount)
        assertNull(next.error)
        assertEquals(true, next.isAutoSyncEnabled)
        assertFalse(next.isOnline)
        assertEquals("user-project", next.customProjectId)
        assertEquals(42, previous.cloudNotesCount)
    }

    @Test
    fun switchingToAnotherVaultNeverInheritsAnOldSuccessfulSyncTime() {
        val previous = FirestoreSyncState(syncId = "A", lastSyncTime = 999L, cloudNotesCount = 100)
        val next = CloudVaultSwitchPolicy.reset(previous, "B")
        assertNull(next.lastSyncTime)
        assertEquals(0, next.cloudNotesCount)
        assertTrue(next.statusMessage.orEmpty().contains("B"))
    }
}
