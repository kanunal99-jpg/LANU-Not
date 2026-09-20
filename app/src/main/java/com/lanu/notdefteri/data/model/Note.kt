package com.lanu.notdefteri.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val tag: String? = null,
    val imagePath: String? = null,
    val reminderTime: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val audioPath: String? = null,
    val isDeleted: Boolean = false,
    val deletedTimestamp: Long? = null,
    val isLocked: Boolean = false,
    val folder: String? = null,
    val drawingPath: String? = null
)
