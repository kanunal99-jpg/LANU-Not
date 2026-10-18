package com.lanu.notdefteri

import com.lanu.notdefteri.data.model.Note
import com.lanu.notdefteri.util.CloudNoteNumericGuard
import com.lanu.notdefteri.util.CloudNotePayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CloudNoteNumericGuardTest {
    @Test
    fun reminderTimeRejectsFractionalNonNumericAndExtremeValues() {
        for (value in listOf<Any>("1720000000000", 1000.5, Double.NaN, Double.POSITIVE_INFINITY, -1L, Long.MAX_VALUE)) {
            try {
                CloudNoteNumericGuard.reminderTime(mapOf("reminderTime" to value))
                fail("Invalid reminder time must be rejected: $value")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message.orEmpty().contains("hatırlatma"))
            }
        }
        assertEquals(1720000000000L, CloudNoteNumericGuard.reminderTime(mapOf("reminderTime" to 1720000000000L)))
        assertNull(CloudNoteNumericGuard.reminderTime(emptyMap()))
        assertNull(CloudNoteNumericGuard.reminderTime(mapOf("reminderTime" to 0L)))
    }

    @Test
    fun zeroDegreeGpsIsPreservedWhenNewPresenceFlagsAreSet() {
        val note = Note(id = 8, title = "Konum", content = "Sıfır meridyeni", latitude = 0.0, longitude = 0.0)
        val payload = CloudNotePayload.encode(note, 123L)
        assertEquals(true, payload["hasLatitude"])
        assertEquals(true, payload["hasLongitude"])
        assertEquals(0.0, CloudNoteNumericGuard.coordinate(payload, "latitude", "hasLatitude", 90.0)!!)
        assertEquals(0.0, CloudNoteNumericGuard.coordinate(payload, "longitude", "hasLongitude", 180.0)!!)
    }

    @Test
    fun legacyZeroSentinelRemainsAbsentAndOtherCoordinatesRemainValid() {
        assertNull(CloudNoteNumericGuard.coordinate(mapOf("latitude" to 0.0), "latitude", "hasLatitude", 90.0))
        assertEquals(40.9, CloudNoteNumericGuard.coordinate(mapOf("latitude" to 40.9), "latitude", "hasLatitude", 90.0)!!)
        val empty = CloudNotePayload.encode(Note(id = 9, title = "", content = ""), 123L)
        assertEquals(false, empty["hasLatitude"])
        assertNull(CloudNoteNumericGuard.coordinate(empty, "latitude", "hasLatitude", 90.0))
    }

    @Test
    fun malformedGpsOrContradictoryFlagsCannotSilentlyLoseLocation() {
        for (data in listOf(
            mapOf<String, Any?>("latitude" to "41.0"),
            mapOf<String, Any?>("latitude" to 91.0),
            mapOf<String, Any?>("latitude" to Double.NaN),
            mapOf<String, Any?>("latitude" to 0.0, "hasLatitude" to "true"),
            mapOf<String, Any?>("latitude" to 40.0, "hasLatitude" to false),
            mapOf<String, Any?>("hasLatitude" to true)
        )) {
            try {
                CloudNoteNumericGuard.coordinate(data, "latitude", "hasLatitude", 90.0)
                fail("Malformed GPS field must be rejected")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message.orEmpty().contains("koordinat"))
            }
        }
    }
}
