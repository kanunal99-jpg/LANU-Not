package com.lanu.notdefteri.util

import com.lanu.notdefteri.data.model.Note

/**
 * A fail-closed guard for manual Firestore backup, restore and two-way sync.
 *
 * createdAt is not a revision timestamp; never select the winner of two
 * different snapshots solely because one note was created later.
 *
 * This guard is intentionally conservative: the caller must surface an error
 * and keep BOTH copies unchanged when there is any disagreement.
 * It is not a replacement for revision-aware, transactional auto sync.
 */
object NoteSyncConflictGuard {
    /** A cloud collection can contain two different document IDs claiming the
     * same integer note ID. Never let associateBy() silently discard either. */
    fun requireUniqueRemoteIds(remote: Collection<Note>) {
        val duplicateIds = remote.groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys.sorted()
        val invalidIds = remote.filter { it.id <= 0 }.map { it.id }.distinct().sorted()
        if (duplicateIds.isNotEmpty() || invalidIds.isNotEmpty()) {
            throw IllegalStateException(
                "Bulut not kimlikleri geçersiz veya yinelenmiş (yinelenen: " +
                    "${duplicateIds.joinToString(", ")}, geçersiz: ${invalidIds.joinToString(", ")}). " +
                    "Veri kaybını önlemek için eşitleme durduruldu."
            )
        }
    }

    fun conflictingIds(local: Collection<Note>, remote: Collection<Note>): List<Int> {
        val remoteById = remote.associateBy { it.id }
        return local.asSequence()
            .filter { localNote ->
                remoteById[localNote.id]?.let { remoteNote -> remoteNote != localNote } ?: false
            }
            .map { it.id }
            .distinct()
            .sorted()
            .toList()
    }

    fun requireNoConflicts(local: Collection<Note>, remote: Collection<Note>) {
        requireUniqueRemoteIds(remote)
        val ids = conflictingIds(local, remote)
        if (ids.isNotEmpty()) {
            throw IllegalStateException(
                "Not çakışması tespit edildi (kimlikler: ${ids.joinToString(", ")}). " +
                    "Hiçbir notun üzerine yazılmadı. Yerel ve bulut kopyaları " +
                    "yedeklenip değişiklikler elle birleştirilmelidir."
            )
        }
    }
}
