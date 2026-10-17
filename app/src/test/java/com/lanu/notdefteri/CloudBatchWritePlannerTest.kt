package com.lanu.notdefteri

import com.lanu.notdefteri.util.CloudBatchWritePlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudBatchWritePlannerTest {
    @Test
    fun emptyBackupDoesNotProduceEmptyFirestoreBatch() {
        assertTrue(CloudBatchWritePlanner.partition(emptyList<Int>()).isEmpty())
    }

    @Test
    fun batchBoundaryDoesNotExceedFirestoreLimit() {
        val exact = CloudBatchWritePlanner.partition((1..400).toList())
        assertEquals(1, exact.size)
        assertEquals(400, exact.first().size)
        val over = CloudBatchWritePlanner.partition((1..401).toList())
        assertEquals(listOf(400, 1), over.map { it.size })
    }

    @Test
    fun largeBackupPreservesEveryNoteWithoutDuplicatesOrLoss() {
        val input = (1..1201).toList()
        val chunks = CloudBatchWritePlanner.partition(input)
        assertEquals(listOf(400, 400, 400, 1), chunks.map { it.size })
        assertTrue(chunks.all { it.size in 1..400 })
        assertEquals(input, chunks.flatten())
        assertEquals(input.size, chunks.flatten().distinct().size)
    }
}
