package com.lanu.notdefteri.util

import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap

/**
 * Serializes per-process automatic cloud writes and discards queued stale edits.
 *
 * The version is assigned synchronously when the user action arrives; task
 * scheduling order on Dispatchers.IO must not determine which edit wins.
 * This only protects writes from THIS process. Multi-device conflicts still
 * require revision-aware transactions and user resolution.
 */
class AutoSyncWriteSequencer {
    private val remoteWriteMutex = Mutex()
    private val latestVersions = ConcurrentHashMap<Int, Long>()
    private var nextVersion = 0L

    @Synchronized
    fun markChanged(noteId: Int): Long {
        require(noteId > 0) { "Note ID must be a saved positive ID" }
        nextVersion += 1
        latestVersions[noteId] = nextVersion
        return nextVersion
    }

    // Disabling auto sync or switching vaults invalidates queued operations.
    @Synchronized
    fun invalidatePending() {
        latestVersions.clear()
    }

    suspend fun runIfLatest(noteId: Int, version: Long, operation: suspend () -> Unit): Boolean {
        remoteWriteMutex.lock()
        try {
            if (latestVersions[noteId] != version) return false
            operation()
            return true
        } finally {
            latestVersions.remove(noteId, version)
            remoteWriteMutex.unlock()
        }
    }
}
