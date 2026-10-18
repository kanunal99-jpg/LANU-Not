package com.lanu.notdefteri.util

import com.lanu.notdefteri.data.model.Note

/**
 * Keep the trash state when a note is backed up or automatically synced.
 * Firestore stores zero for a cleared timestamp so old non-null field values
 * are overwritten when a user restores a note.
 */
object CloudNoteTrashFields {
    data class TrashState(val isDeleted: Boolean, val deletedTimestamp: Long?)

    /**
     * Missing deletion fields are legal for legacy backups. Present-but-invalid
     * fields are not: silently reading "true" as false can resurrect trash.
     */
    fun decode(data: Map<String, Any?>): TrashState {
        val isDeleted = when (val raw = data["isDeleted"]) {
            null -> false
            is Boolean -> raw
            else -> throw IllegalStateException("Geçersiz bulut çöp kutusu durumu (isDeleted).")
        }
        val deletedTimestamp = when (val raw = data["deletedTimestamp"]) {
            null -> null
            is Number -> {
                val asDouble = raw.toDouble()
                val asLong = raw.toLong()
                if (!asDouble.isFinite() || asDouble != asLong.toDouble() || asLong < 0L) {
                    throw IllegalStateException("Geçersiz bulut silinme tarihi (deletedTimestamp).")
                }
                asLong.takeIf { it > 0L }
            }
            else -> throw IllegalStateException("Geçersiz bulut silinme tarihi (deletedTimestamp).")
        }
        if (!isDeleted && deletedTimestamp != null) {
            throw IllegalStateException("Bulut notunda çelişkili çöp kutusu alanları.")
        }
        if (isDeleted && data.containsKey("deletedTimestamp") && deletedTimestamp == null) {
            throw IllegalStateException("Bulut notunun silinme tarihi geçersiz.")
        }
        return TrashState(isDeleted, deletedTimestamp)
    }

    fun encode(note: Note): Map<String, Any> {
        val timestamp = note.deletedTimestamp
        if (note.isDeleted && (timestamp == null || timestamp <= 0L)) {
            throw IllegalStateException(
                "Çöp kutusundaki notun geçerli silinme tarihi yok; bozuk kayıt buluta yazılmadı."
            )
        }
        if (!note.isDeleted && timestamp != null) {
            throw IllegalStateException(
                "Aktif notta silinme tarihi bulunuyor; çelişkili kayıt buluta yazılmadı."
            )
        }
        return mapOf(
            "isDeleted" to note.isDeleted,
            "deletedTimestamp" to (timestamp ?: 0L)
        )
    }
}
