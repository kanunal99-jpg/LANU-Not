package com.lanu.notdefteri.util

import com.lanu.notdefteri.data.model.Note

/**
 * Keep the trash state when a note is backed up or automatically synced.
 * Firestore stores zero for a cleared timestamp so old non-null field values
 * are overwritten when a user restores a note.
 */
object CloudNoteTrashFields {
    data class TrashState(val isDeleted: Boolean, val deletedTimestamp: Long?)

    /** Older cloud documents without deletion metadata remain readable. */
    fun decode(data: Map<String, Any?>): TrashState = TrashState(
        isDeleted = data["isDeleted"] as? Boolean ?: false,
        deletedTimestamp = (data["deletedTimestamp"] as? Number)?.toLong()?.takeIf { it > 0L }
    )

    fun encode(note: Note): Map<String, Any> = mapOf(
        "isDeleted" to note.isDeleted,
        "deletedTimestamp" to (note.deletedTimestamp ?: 0L)
    )
}
