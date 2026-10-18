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
    fun cloudTrashMetadataRoundTripsOnDeleteAndRestore() {
        val deleted = CloudNoteTrashFields.decode(
            CloudNoteTrashFields.encode(note.copy(isDeleted = true, deletedTimestamp = 7500L))
        )
        assertEquals(true, deleted.isDeleted)
        assertEquals(7500L, deleted.deletedTimestamp)

        val restored = CloudNoteTrashFields.decode(CloudNoteTrashFields.encode(note))
        assertEquals(false, restored.isDeleted)
        assertEquals(null, restored.deletedTimestamp)
    }

    @Test
    fun legacyBackupsWithoutTrashFieldsRemainReadable() {
        val old = CloudNoteTrashFields.decode(mapOf("id" to 19, "title" to "Old"))
        assertEquals(false, old.isDeleted)
        assertEquals(null, old.deletedTimestamp)
    }

    @Test
    fun zeroDeletedTimestampDoesNotInventDeletionDate() {
        assertEquals(null, CloudNoteTrashFields.decode(mapOf("deletedTimestamp" to 0L)).deletedTimestamp)
    }

    @Test
    fun invalidOrContradictoryTrashFieldsCannotResurrectNotes() {
        val invalidCloudRecords = listOf(
            mapOf<String, Any?>("isDeleted" to "true", "deletedTimestamp" to 123L),
            mapOf<String, Any?>("isDeleted" to true, "deletedTimestamp" to "invalid"),
            mapOf<String, Any?>("isDeleted" to true, "deletedTimestamp" to 0L),
            mapOf<String, Any?>("isDeleted" to false, "deletedTimestamp" to 123L),
            mapOf<String, Any?>("isDeleted" to true, "deletedTimestamp" to -5L),
            mapOf<String, Any?>("isDeleted" to true, "deletedTimestamp" to 3.4),
            mapOf<String, Any?>("isDeleted" to true, "deletedTimestamp" to Double.NaN)
        )
        invalidCloudRecords.forEach { fields ->
            try {
                CloudNoteTrashFields.decode(fields)
                org.junit.Assert.fail("A malformed cloud trash record must fail closed: $fields")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message.orEmpty().contains("bulut", ignoreCase = true))
            }
        }
    }

    @Test
    fun legacyDeletedNoteWithoutTimestampRetainsDeletedFlag() {
        val legacy = CloudNoteTrashFields.decode(mapOf("isDeleted" to true))
        assertEquals(true, legacy.isDeleted)
        assertEquals(null, legacy.deletedTimestamp)
    }

    @Test
    fun encodingRefusesDeletedNotesWithoutARealDeletionTimestamp() {
        for (invalid in listOf(null, 0L, -1L)) {
            try {
                CloudNoteTrashFields.encode(note.copy(isDeleted = true, deletedTimestamp = invalid))
                org.junit.Assert.fail("Deleted note without timestamp must not be uploaded: $invalid")
            } catch (e: IllegalStateException) {
                assertTrue(e.message.orEmpty().contains("silinme tarihi"))
            }
        }
    }

    @Test
    fun encodingRefusesActiveNotesWithStaleDeletionTimestamp() {
        try {
            CloudNoteTrashFields.encode(note.copy(isDeleted = false, deletedTimestamp = 7000L))
            org.junit.Assert.fail("Active note with an old tombstone cannot round trip")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("silinme tarihi"))
        }
    }

    @Test
    fun everyEncodedValidTrashStateIsAcceptedByDecoder() {
        val active = note.copy(isDeleted = false, deletedTimestamp = null)
        val deleted = note.copy(isDeleted = true, deletedTimestamp = 8000L)
        for (source in listOf(active, deleted)) {
            val decoded = CloudNoteTrashFields.decode(CloudNoteTrashFields.encode(source))
            assertEquals(source.isDeleted, decoded.isDeleted)
            assertEquals(source.deletedTimestamp, decoded.deletedTimestamp)
        }
    }

    @Test
    fun cloudTrashMetadataContainsNoPrivateNoteText() {
        val fields = CloudNoteTrashFields.encode(note.copy(content = "Çok özel bilgi"))
        assertTrue(fields.keys == setOf("isDeleted", "deletedTimestamp"))
        assertTrue(fields.values.none { it is String })
    }
}
