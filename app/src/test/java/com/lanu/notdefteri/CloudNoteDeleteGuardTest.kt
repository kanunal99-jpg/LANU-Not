package com.lanu.notdefteri

import com.lanu.notdefteri.data.model.Note
import com.lanu.notdefteri.util.CloudNoteDeleteGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CloudNoteDeleteGuardTest {
    private val trashed = Note(
        id = 31, title = "Arşiv", content = "Önemli not",
        createdAt = 900L, isDeleted = true, deletedTimestamp = 1200L
    )

    @Test
    fun matchingCloudSnapshotCanBeDeleted() {
        CloudNoteDeleteGuard.requireSameSnapshot(trashed, trashed.copy())
    }

    @Test
    fun cloudEditOrCollisionCannotBeDeleted() {
        for (remote in listOf(
            trashed.copy(content = "Başka cihazın güncel içeriği"),
            trashed.copy(createdAt = 1900L),
            trashed.copy(id = 32),
            trashed.copy(isDeleted = false, deletedTimestamp = null),
            trashed.copy(audioPath = "cloud-recording.ogg")
        )) {
            try {
                CloudNoteDeleteGuard.requireSameSnapshot(trashed, remote)
                fail("Changed cloud snapshot must not be deleted: $remote")
            } catch (e: IllegalStateException) {
                assertTrue(e.message.orEmpty().contains("silinm", ignoreCase = true))
            }
        }
    }

    @Test
    fun invalidNoteIdCannotBeDeleted() {
        try {
            CloudNoteDeleteGuard.requireSameSnapshot(
                trashed.copy(id = 0),
                trashed.copy(id = 0)
            )
            fail("Invalid ID must be rejected")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("kimliği"))
        }
    }

    @Test
    fun emptyTrashSelectionExcludesActiveNotesAndKeepsSnapshots() {
        val active = trashed.copy(id = 12, isDeleted = false, deletedTimestamp = null)
        val removed = CloudNoteDeleteGuard.trashedNotesBeforePurge(
            listOf(active, trashed, trashed.copy(id = 33))
        )
        assertEquals(listOf(31, 33), removed.map { it.id })
        assertTrue(removed.all { it.isDeleted && it.deletedTimestamp != null })
    }
}
