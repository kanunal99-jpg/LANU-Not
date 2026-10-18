package com.lanu.notdefteri.util

import com.lanu.notdefteri.data.model.Note

/**
 * The canonical Firestore representation used by manual backup, two-way sync
 * and automatic updates. Keeping one codec prevents backups from silently
 * dropping fields supported by the Room Note model.
 *
 * Firestore receives explicit empty/zero values for cleared nullable fields
 * because all three callers use SetOptions.merge().
 */
object CloudNotePayload {
    fun encode(note: Note, updatedAt: Long): Map<String, Any> {
        require(note.id > 0) { "Kaydedilmemiş bir not buluta yazılamaz (id <= 0)." }
        return mapOf(
            "id" to note.id,
            "title" to note.title,
            "content" to note.content,
            "createdAt" to note.createdAt,
            "updatedAt" to updatedAt,
            "isPinned" to note.isPinned,
            "tag" to (note.tag ?: ""),
            "imagePath" to (note.imagePath ?: ""),
            "reminderTime" to (note.reminderTime ?: 0L),
            "latitude" to (note.latitude ?: 0.0),
            "longitude" to (note.longitude ?: 0.0),
            "hasLatitude" to (note.latitude != null),
            "hasLongitude" to (note.longitude != null),
            "locationName" to (note.locationName ?: ""),
            "audioPath" to (note.audioPath ?: ""),
            "isLocked" to note.isLocked,
            "folder" to (note.folder ?: ""),
            "drawingPath" to (note.drawingPath ?: "")
        ) + CloudNoteTrashFields.encode(note)
    }
}
