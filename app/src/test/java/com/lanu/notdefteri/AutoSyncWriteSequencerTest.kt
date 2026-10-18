package com.lanu.notdefteri

import com.lanu.notdefteri.util.AutoSyncWriteSequencer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AutoSyncWriteSequencerTest {
    @Test
    fun queuedEarlierEditCannotReplaceNewerEdit() = runBlocking {
        val gate = AutoSyncWriteSequencer()
        val old = gate.markChanged(7)
        val newer = gate.markChanged(7)
        val writes = mutableListOf<String>()

        assertFalse(gate.runIfLatest(7, old) { writes.add("old") })
        assertTrue(gate.runIfLatest(7, newer) { writes.add("new") })
        assertEquals(listOf("new"), writes)
    }

    @Test
    fun runningOldWriteFinishesBeforeNewWriteStarts() = runBlocking {
        val gate = AutoSyncWriteSequencer()
        val old = gate.markChanged(7)
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val writes = mutableListOf<String>()
        val first = async(Dispatchers.Default) {
            gate.runIfLatest(7, old) {
                writes.add("old-start")
                started.complete(Unit)
                release.await()
                writes.add("old-finish")
            }
        }
        started.await()

        val newer = gate.markChanged(7)
        val second = async(Dispatchers.Default) {
            gate.runIfLatest(7, newer) { writes.add("new") }
        }
        release.complete(Unit)
        assertTrue(first.await())
        assertTrue(second.await())
        assertEquals(listOf("old-start", "old-finish", "new"), writes)
    }

    @Test
    fun deletionEventSupersedesQueuedEditForSameNote() = runBlocking {
        val gate = AutoSyncWriteSequencer()
        val edit = gate.markChanged(8)
        val deletion = gate.markChanged(8)
        val writes = mutableListOf<String>()
        assertFalse(gate.runIfLatest(8, edit) { writes.add("edit") })
        assertTrue(gate.runIfLatest(8, deletion) { writes.add("delete") })
        assertEquals(listOf("delete"), writes)
    }

    @Test
    fun switchingVaultOrDisablingSyncInvalidatesQueuedOperations() = runBlocking {
        val gate = AutoSyncWriteSequencer()
        val queued = gate.markChanged(9)
        gate.invalidatePending()
        var written = false
        assertFalse(gate.runIfLatest(9, queued) { written = true })
        assertFalse(written)
        val fresh = gate.markChanged(9)
        assertTrue(gate.runIfLatest(9, fresh) { written = true })
        assertTrue(written)
    }

    @Test
    fun invalidNoteIdsAndRepeatedVersionsCannotBeWritten() = runBlocking {
        val gate = AutoSyncWriteSequencer()
        try {
            gate.markChanged(0)
            fail("Unsaved note must not be sent to the cloud")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("saved"))
        }
        val current = gate.markChanged(11)
        var count = 0
        assertTrue(gate.runIfLatest(11, current) { count++ })
        assertFalse(gate.runIfLatest(11, current) { count++ })
        assertEquals(1, count)
    }
}
