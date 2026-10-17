package com.lanu.notdefteri.util

import com.lanu.notdefteri.data.model.Note

/** A non-destructive plan computed before any manual Firestore/Room writes. */
data class NoteTwoWaySyncPlan(
    val download: List<Note>,
    val upload: List<Note>
)

/**
 * Without revision tracking we cannot determine which of two DIFFERENT copies
 * is newer. Refuse divergent snapshots; never guess using createdAt.
 *
 * Include soft-deleted notes from both sides. An identical note needs no write,
 * preventing updatedAt churn, excessive Firestore writes and stale overwrites.
 */
object NoteTwoWaySyncPlanner {
    fun plan(localIncludingTrash: List<Note>, remote: List<Note>): NoteTwoWaySyncPlan {
        NoteSyncConflictGuard.requireUniqueRemoteIds(remote)
        NoteSyncConflictGuard.requireNoConflicts(localIncludingTrash, remote)

        val localById = localIncludingTrash.associateBy { it.id }
        val remoteById = remote.associateBy { it.id }

        val download = remote.filter { it.id !in localById }
        val upload = localIncludingTrash.filter { it.id !in remoteById }

        return NoteTwoWaySyncPlan(download = download, upload = upload)
    }
}
