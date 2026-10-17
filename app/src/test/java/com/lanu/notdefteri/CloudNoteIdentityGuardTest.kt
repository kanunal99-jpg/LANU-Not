package com.lanu.notdefteri

import com.lanu.notdefteri.util.CloudNoteIdentityGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CloudNoteIdentityGuardTest {
    @Test
    fun validCloudDocumentIdsAndLegacyPathOnlyIdsAreAccepted() {
        assertEquals(21, CloudNoteIdentityGuard.validatedId("21", 21L))
        assertEquals(21, CloudNoteIdentityGuard.validatedId("21", null))
        assertEquals(123456789L, CloudNoteIdentityGuard.validatedCreatedAt(123456789L))
    }

    @Test
    fun mismatchOrFractionalPayloadIdFailsClosed() {
        for (invalid in listOf<Any>(19L, 21.5, "21", -21L)) {
            try {
                CloudNoteIdentityGuard.validatedId("21", invalid)
                fail("Payload ID should not silently override path: $invalid")
            } catch (e: IllegalStateException) {
                assertTrue(e.message.orEmpty().contains("uyușmuyor".replace("ș", "ş")))
            }
        }
    }

    @Test
    fun negativeNonCanonicalAndOverflowingPathsAreRejected() {
        for (path in listOf("0", "-3", "001", "2147483648", "new-note", "")) {
            try {
                CloudNoteIdentityGuard.validatedId(path, null)
                fail("Invalid cloud path must not turn into a local primary key: $path")
            } catch (e: IllegalStateException) {
                assertTrue(e.message.orEmpty().contains("geçersiz"))
            }
        }
    }

    @Test
    fun missingZeroFractionalAndNegativeCreationTimeIsNeverFabricated() {
        for (timestamp in listOf<Any?>(null, 0L, -5L, 1700.5, "1700")) {
            try {
                CloudNoteIdentityGuard.validatedCreatedAt(timestamp)
                fail("Missing/corrupt cloud creation date must not be replaced by current time")
            } catch (e: IllegalStateException) {
                assertTrue(e.message.orEmpty().contains("oluşturulma tarihi"))
            }
        }
    }
}
