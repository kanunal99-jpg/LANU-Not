package com.lanu.notdefteri.util

/** A newly selected vault has not inherited the previous vault's sync
 * timestamps or cloud document count. Retain network/auto-sync preferences. */
object CloudVaultSwitchPolicy {
    fun reset(previous: FirestoreSyncState, newId: String): FirestoreSyncState =
        previous.copy(
            syncId = newId,
            lastSyncTime = null,
            cloudNotesCount = 0,
            error = null,
            statusMessage = "Eşitleme Kimliği güncellendi: $newId"
        )
}
