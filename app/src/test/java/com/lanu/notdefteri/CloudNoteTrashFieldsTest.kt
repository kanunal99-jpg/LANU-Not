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
    fun cloudTrashMetadataContainsNoPrivateNoteText() {
        val fields = CloudNoteTrashFields.encode(note.copy(content = "Çok özel bilgi"))
        assertTrue(fields.keys == setOf("isDeleted", "deletedTimestamp"))
        assertTrue(fields.values.none { it is String })
    }
}
