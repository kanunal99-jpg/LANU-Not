package com.lanu.notdefteri

import com.lanu.notdefteri.util.CloudNoteContentGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CloudNoteContentGuardTest {
    @Test
    fun validAndIntentionallyEmptyNoteTextIsPreserved() {
        val remote = mapOf<String, Any?>("title" to "", "content" to "Güvenli not")
        assertEquals("", CloudNoteContentGuard.requiredText(remote, "title"))
        assertEquals("Güvenli not", CloudNoteContentGuard.requiredText(remote, "content"))
    }

    @Test
    fun missingOrMalformedRequiredNoteTextIsRejected() {
        for (remote in listOf(
            emptyMap<String, Any?>(),
            mapOf<String, Any?>("title" to 123, "content" to "not"),
            mapOf<String, Any?>("title" to "not", "content" to null),
            mapOf<String, Any?>("title" to "not", "content" to listOf("data"))
        )) {
            try {
                CloudNoteContentGuard.requiredText(remote, "title")
                CloudNoteContentGuard.requiredText(remote, "content")
                fail("A missing or corrupt note text field must not silently become empty")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message.orEmpty().contains("alanı"))
            }
        }
    }

    @Test
    fun corruptAttachmentPathsCannotSilentlyDisappearOnRestore() {
        for (field in listOf("imagePath", "audioPath", "drawingPath", "folder", "tag", "locationName")) {
            try {
                CloudNoteContentGuard.optionalText(mapOf(field to 42), field)
                fail("Invalid $field must not be dropped")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message.orEmpty().contains(field))
            }
            assertNull(CloudNoteContentGuard.optionalText(emptyMap(), field))
            assertNull(CloudNoteContentGuard.optionalText(mapOf(field to ""), field))
        }
    }

    @Test
    fun corruptFlagTypesAreRejectedWithoutChangingLegacyDefaults() {
        assertEquals(false, CloudNoteContentGuard.boolean(emptyMap(), "isPinned"))
        assertEquals(true, CloudNoteContentGuard.boolean(mapOf("isPinned" to true), "isPinned"))
        for (field in listOf("isPinned", "isLocked")) {
            try {
                CloudNoteContentGuard.boolean(mapOf(field to "false"), field)
                fail("Invalid boolean $field must not become false")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message.orEmpty().contains(field))
            }
        }
    }
}
