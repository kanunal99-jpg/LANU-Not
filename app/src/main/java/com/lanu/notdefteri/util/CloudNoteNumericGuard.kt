package com.lanu.notdefteri.util

import kotlin.math.abs

/**
 * Decode optional Firestore numeric values without silently truncating invalid
 * types, fractional timestamps, or impossible GPS coordinates.
 *
 * Older backups encoded nullable coordinates as numeric zero. New backups
 * include explicit presence flags so 0.0 latitude/longitude remains valid.
 */
object CloudNoteNumericGuard {
    fun reminderTime(data: Map<String, Any?>): Long? {
        val raw = data["reminderTime"] ?: return null
        if (raw !is Number) throw IllegalStateException("Geçersiz hatırlatma tarihi türü.")
        val decimal = raw.toDouble()
        val millis = raw.toLong()
        // Firestore uses epoch milliseconds; cap at year 9999 and never
        // silently truncate a fractional or out-of-range floating value.
        if (!decimal.isFinite() || decimal != millis.toDouble() ||
            millis < 0L || millis > 253402300799999L) {
            throw IllegalStateException("Geçersiz hatırlatma tarihi değeri.")
        }
        return millis.takeIf { it > 0L }
    }

    fun coordinate(
        data: Map<String, Any?>,
        field: String,
        presenceField: String,
        limit: Double
    ): Double? {
        val rawFlag = data[presenceField]
        val present = when (rawFlag) {
            null -> null // legacy backups without presence markers
            is Boolean -> rawFlag
            else -> throw IllegalStateException("Geçersiz koordinat varlık bayrağı: $presenceField")
        }
        val raw = data[field]
        if (raw == null) {
            if (present == true) throw IllegalStateException("Eksik koordinat: $field")
            return null
        }
        if (raw !is Number) throw IllegalStateException("Geçersiz koordinat türü: $field")
        val value = raw.toDouble()
        if (!value.isFinite() || abs(value) > limit) {
            throw IllegalStateException("Geçersiz koordinat değeri: $field")
        }
        if (present == false) {
            if (value != 0.0) throw IllegalStateException("Çelişkili koordinat: $field")
            return null
        }
        return if (present == true) value else value.takeIf { it != 0.0 }
    }
}
