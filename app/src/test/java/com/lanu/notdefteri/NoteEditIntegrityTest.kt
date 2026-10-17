package com.lanu.notdefteri

import com.lanu.notdefteri.data.model.Note
import com.lanu.notdefteri.util.NoteEditIntegrity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NoteEditIntegrityTest {
    private val original = Note(
        id = 42,
        title = "İlk başlık",
        content = "İlk içerik",
        createdAt = 123456L,
        isDeleted = true,
        deletedTimestamp = 456789L,
        tag = "Arşiv"
    )

    @Test
    fun editRetainsOriginalCreationAndTrashMetadata() {
        val edited = NoteEditIntegrity.prepareForSave(
            Note(id = 42, title = "Yeni başlık", content = "Yeni içerik", createdAt = 999999L),
            original
        )
        assertEquals(123456L, edited.createdAt)
        assertTrue(edited.isDeleted)
        assertEquals(456789L, edited.deletedTimestamp)
        assertEquals("Arşiv", edited.tag)
        assertEquals("Yeni başlık", edited.title)
        assertEquals("Yeni içerik", edited.content)
    }

    @Test
    fun newNoteKeepsItsInitialCreationTimestamp() {
        val draft = Note(title = "Yeni", content = "Not", createdAt = 1111L)
        assertEquals(draft, NoteEditIntegrity.prepareForSave(draft, null))
    }

    @Test
    fun deletedOrMissingExistingNoteCannotBeResurrected() {
        try {
            NoteEditIntegrity.prepareForSave(Note(id = 42, title = "Hayalet", content = ""), null)
            fail("Missing existing note must be rejected")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("bulunamadı"))
        }
    }

    @Test
    fun inconsistentNoteIdentityIsRejected() {
        try {
            NoteEditIntegrity.prepareForSave(original.copy(id = 43), original)
            fail("Mismatched note identity must not be updated")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("kimliği"))
        }
    }

    @Test
    fun savingAnExistingLiveNoteRemainsActive() {
        val live = original.copy(isDeleted = false, deletedTimestamp = null)
        val edited = NoteEditIntegrity.prepareForSave(live.copy(content = "Güncel"), live)
        assertFalse(edited.isDeleted)
        assertEquals(null, edited.deletedTimestamp)
    }
}
