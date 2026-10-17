package com.lanu.notdefteri.util

import com.lanu.notdefteri.data.model.Note

/**
 * An edit is a new content version, not a newly created note. Keep immutable
 * identity metadata and existing trash state through editor saves.
 */
object NoteEditIntegrity {
    fun prepareForSave(draft: Note, original: Note?): Note {
        if (draft.id > 0 && original == null) {
            throw IllegalStateException("Düzenlenecek not artık bulunamadı; kayıt yapılmadı.")
        }
        if (original != null && draft.id != original.id) {
            throw IllegalArgumentException("Not kimliği tutarsız; kayıt iptal edildi.")
        }
        return if (original == null) draft else draft.copy(
            createdAt = original.createdAt,
            isDeleted = original.isDeleted,
            deletedTimestamp = original.deletedTimestamp,
            tag = original.tag
        )
    }
}
