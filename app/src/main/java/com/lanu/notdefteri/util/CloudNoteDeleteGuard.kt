package com.lanu.notdefteri.util

import com.lanu.notdefteri.data.model.Note

/**
 * A permanent delete must never remove a different cloud snapshot which
 * happens to share a device-local integer ID. Called inside a Firestore
 * transaction, so the comparison and delete are atomic.
 */
object CloudNoteDeleteGuard {
    fun requireSameSnapshot(local: Note, remote: Note) {
        if (local.id <= 0 || remote.id <= 0 || local.id != remote.id) {
            throw IllegalStateException("Bulut notu kimliği eşleşmiyor; uzaktaki not silinmedi.")
        }
        if (local != remote) {
            throw IllegalStateException(
                "Buluttaki not bu cihazdaki kopyadan farklı. " +
                    "Veri kaybını önlemek için buluttan kalıcı silme durduruldu."
            )
        }
    }

    fun trashedNotesBeforePurge(allNotes: List<Note>): List<Note> =
        allNotes.filter { it.isDeleted }
}
