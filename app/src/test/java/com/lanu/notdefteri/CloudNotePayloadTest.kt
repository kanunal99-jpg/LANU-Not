package com.lanu.notdefteri

import com.lanu.notdefteri.data.model.Note
import com.lanu.notdefteri.util.CloudNotePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CloudNotePayloadTest {
    @Test
    fun savesEveryUserFieldIncludingTrashAndAttachments() {
        val local = Note(
            id = 42,
            title = "Saha notu",
            content = "Gizli içerik",
            createdAt = 100L,
            isPinned = true,
            tag = "İş",
            imagePath = "photos/42.png",
            reminderTime = 900L,
            latitude = 41.0,
            longitude = 29.0,
            locationName = "İstanbul",
            audioPath = "recordings/42.m4a",
            isDeleted = true,
            deletedTimestamp = 250L,
            isLocked = true,
            folder = "Arşiv",
            drawingPath = "sketches/42.png"
        )

        val cloud = CloudNotePayload.encode(local, updatedAt = 500L)
        assertEquals(42, cloud["id"])
        assertEquals("Saha notu", cloud["title"])
        assertEquals("Gizli içerik", cloud["content"])
        assertEquals(100L, cloud["createdAt"])
        assertEquals(500L, cloud["updatedAt"])
        assertEquals(true, cloud["isPinned"])
        assertEquals("İş", cloud["tag"])
        assertEquals("photos/42.png", cloud["imagePath"])
        assertEquals(900L, cloud["reminderTime"])
        assertEquals(41.0, cloud["latitude"])
        assertEquals(29.0, cloud["longitude"])
        assertEquals("İstanbul", cloud["locationName"])
        assertEquals("recordings/42.m4a", cloud["audioPath"])
        assertEquals(true, cloud["isDeleted"])
        assertEquals(250L, cloud["deletedTimestamp"])
        assertEquals(true, cloud["isLocked"])
        assertEquals("Arşiv", cloud["folder"])
        assertEquals("sketches/42.png", cloud["drawingPath"])
        assertEquals(18, cloud.size)
    }

    @Test
    fun clearingOptionalFieldsWritesExplicitEmptyValuesForFirestoreMerge() {
        val cloud = CloudNotePayload.encode(
            Note(id = 1, title = "Temiz", content = ""), updatedAt = 1000L
        )
        for (key in listOf("tag", "imagePath", "locationName", "audioPath", "folder", "drawingPath")) {
            assertEquals("", cloud[key])
        }
        assertEquals(0L, cloud["reminderTime"])
        assertEquals(0L, cloud["deletedTimestamp"])
        assertEquals(0.0, cloud["latitude"])
        assertEquals(0.0, cloud["longitude"])
        assertEquals(false, cloud["isDeleted"])
        assertFalse(cloud.values.any { it == null })
    }

    @Test
    fun unsavedOrInvalidNoteIdsCannotBeUploaded() {
        for (id in listOf(0, -1)) {
            try {
                CloudNotePayload.encode(Note(id = id, title = "Draft", content = ""), 2L)
                fail("Unsaved note with id=$id must be rejected")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message.orEmpty().contains("id"))
            }
        }
    }
}
