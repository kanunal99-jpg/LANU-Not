package com.lanu.notdefteri

import com.lanu.notdefteri.data.model.Note
import com.lanu.notdefteri.util.CloudNoteTrashFields
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudNoteTrashFieldsTest {
    private val note = Note(id = 19, title = "Kayıt", content = "Önemli", createdAt = 1000L)

    @Test
    fun movedToTrashRetainsDeletionStateAndTimestamp() {
        val fields = CloudNoteTrashFields.encode(note.copy(isDeleted = true, deletedTimestamp = 5000L))
        assertEquals(true, fields["isDeleted"])
        assertEquals(5000L, fields["deletedTimestamp"])
    }

    @Test
    fun restoringNoteClearsPreviouslyStoredCloudTombstone() {
        val fields = CloudNoteTrashFields.encode(note.copy(isDeleted = false, deletedTimestamp = null))
        assertEquals(false, fields["isDeleted"])
        assertEquals(0L, fields["deletedTimestamp"])
    }

    @Test
    fun cloudTrashMetadataRoundTripsThroughFirestoreRepresentation() {
        val removed = note.copy(isDeleted = true, deletedTimestamp = 7500L)
        val deleted = CloudNoteTrashFields.decode(CloudNoteTrashFields.encode(removed))
        assertEquals(true, deleted.isDeleted)
        assertEquals(7500L, deleted.deletedTimestamp)
        val restored = CloudNoteTrashFields.decode(CloudNoteTrashFields.encode(note))
        assertEquals(false, restored.isDeleted)
        assertEquals(null, restored.deletedTimestamp)
    }

    @Test
    fun legacyBackupsWithoutTrashFieldsStayReadable() {
        val old = CloudNoteTrashFields.decode(mapOf("id" to 19, "title" to "Old"))
        assertEquals(false, old.isDeleted)
        assertEquals(null, old.deletedTimestamp)
    }

    @Test
    fun malformedOrZeroDeletionTimeNeverInventsTimestamp() {
        val invalid = CloudNoteTrashFields.decode(
            mapOf("isDeleted" to true, "deletedTimestamp" to "not-a-time")
        )
        assertEquals(true, invalid.isDeleted)
        assertEquals(null, invalid.deletedTimestamp)
        assertEquals(null, CloudNoteTrashFields.decode(mapOf("deletedTimestamp" to 0L)).deletedTimestamp)
    }

    @Test
    fun cloudTrashMetadataContainsNoPrivateNoteText() {
        val fields = CloudNoteTrashFields.encode(note.copy(content = "Çok özel bilgi"))
        assertTrue(fields.keys == setOf("isDeleted", "deletedTimestamp"))
        assertTrue(fields.values.none { it is String })
    }
}
