package com.lanu.notdefteri

import com.lanu.notdefteri.data.model.Note
import com.lanu.notdefteri.util.NoteSyncConflictGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NoteSyncConflictGuardTest {
    private val original = Note(
        id = 17,
        title = "Toplantı",
        content = "İlk kayıt",
        createdAt = 1000L
    )

    @Test
    fun identicalSnapshotsCanSyncWithoutConflicts() {
        assertTrue(NoteSyncConflictGuard.conflictingIds(listOf(original), listOf(original.copy())).isEmpty())
        NoteSyncConflictGuard.requireNoConflicts(listOf(original), listOf(original.copy()))
    }

    @Test
    fun changedBodyIsAConflictEvenWhenCreatedAtMatches() {
        val remote = original.copy(content = "Farklı cihazdaki son düzenleme")
        assertEquals(listOf(17), NoteSyncConflictGuard.conflictingIds(listOf(original), listOf(remote)))
        try {
            NoteSyncConflictGuard.requireNoConflicts(listOf(original), listOf(remote))
            fail("Divergent snapshots must never overwrite silently")
        } catch (e: IllegalStateException) {
            assertTrue(e.message.orEmpty().contains("17"))
        }
    }

    @Test
    fun idCollisionOrTrashedNoteCannotBeOverwritten() {
        val remote = original.copy(title = "Tamamen başka bir not", createdAt = 2000L)
        assertEquals(listOf(17), NoteSyncConflictGuard.conflictingIds(listOf(original), listOf(remote)))
        val trashed = original.copy(isDeleted = true, deletedTimestamp = 2500L)
        assertEquals(listOf(17), NoteSyncConflictGuard.conflictingIds(listOf(trashed), listOf(original)))
    }

    @Test
    fun disjointNotesCanBeUploadedOrDownloadedWithoutConflict() {
        val remote = original.copy(id = 18, title = "İkinci cihaz")
        assertTrue(NoteSyncConflictGuard.conflictingIds(listOf(original), listOf(remote)).isEmpty())
        NoteSyncConflictGuard.requireNoConflicts(listOf(original), listOf(remote))
    }

    @Test
    fun metadataAndAttachmentsAreProtectedFromSilentOverwrites() {
        val remote = original.copy(isPinned = true, imagePath = "remote-photo.png")
        assertEquals(listOf(17), NoteSyncConflictGuard.conflictingIds(listOf(original), listOf(remote)))
    }
}
