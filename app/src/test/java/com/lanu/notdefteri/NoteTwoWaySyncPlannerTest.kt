package com.lanu.notdefteri

import com.lanu.notdefteri.data.model.Note
import com.lanu.notdefteri.util.NoteTwoWaySyncPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NoteTwoWaySyncPlannerTest {
    private val first = Note(id = 41, title = "Fikir", content = "Metin", createdAt = 100L)
    private val second = Note(id = 42, title = "Görev", content = "Liste", createdAt = 200L)

    @Test
    fun identicalSnapshotsDoNotRewriteEitherSide() {
        val plan = NoteTwoWaySyncPlanner.plan(listOf(first), listOf(first.copy()))
        assertTrue(plan.download.isEmpty())
        assertTrue(plan.upload.isEmpty())
    }

    @Test
    fun uploadsAndDownloadsOnlyMissingNotes() {
        val plan = NoteTwoWaySyncPlanner.plan(listOf(first), listOf(second))
        assertEquals(listOf(first), plan.upload)
        assertEquals(listOf(second), plan.download)
    }

    @Test
    fun identicalTrashedNoteIsNotRestoredOrReuploaded() {
        val trash = first.copy(isDeleted = true, deletedTimestamp = 350L)
        val plan = NoteTwoWaySyncPlanner.plan(listOf(trash), listOf(trash.copy()))
        assertTrue(plan.download.isEmpty())
        assertTrue(plan.upload.isEmpty())
    }

    @Test
    fun aTrashedNoteOnlyOnOneSideIsPreservedInSyncPlan() {
        val trash = first.copy(isDeleted = true, deletedTimestamp = 350L)
        assertEquals(listOf(trash), NoteTwoWaySyncPlanner.plan(listOf(trash), emptyList()).upload)
        assertEquals(listOf(trash), NoteTwoWaySyncPlanner.plan(emptyList(), listOf(trash)).download)
    }

    @Test
    fun sameIdButDifferentEditsStopBeforeAnyWrite() {
        try {
            NoteTwoWaySyncPlanner.plan(listOf(first), listOf(first.copy(content = "Yeni içerik")))
            fail("Two different revisions cannot be chosen by createdAt")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().contains("41"))
        }
    }

    @Test
    fun sameIdButDifferentTrashStatusStopsBeforeAnyWrite() {
        try {
            NoteTwoWaySyncPlanner.plan(
                listOf(first.copy(isDeleted = true, deletedTimestamp = 300L)),
                listOf(first)
            )
            fail("A deleted note must not be silently resurrected")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().contains("41"))
        }
    }

    @Test
    fun duplicateCloudIdsCannotBeCollapsedByAssociateBy() {
        try {
            NoteTwoWaySyncPlanner.plan(emptyList(), listOf(first, first.copy()))
            fail("Cloud duplicate identifiers must be rejected")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message.orEmpty().contains("41"))
        }
    }
}
