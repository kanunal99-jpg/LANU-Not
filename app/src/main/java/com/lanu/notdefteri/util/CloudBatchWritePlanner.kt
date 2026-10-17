package com.lanu.notdefteri.util

/**
 * Firestore batched writes are capped at 500 operations per commit.
 * Keep 100 operations of headroom for possible future metadata writes,
 * and never create empty write batches.
 */
object CloudBatchWritePlanner {
    const val MAX_NOTES_PER_BATCH = 400

    fun <T> partition(items: List<T>): List<List<T>> =
        items.chunked(MAX_NOTES_PER_BATCH)
}
