package com.lanu.notdefteri.data.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["noteId", "tagId"],
    indices = [Index(value = ["tagId"])]
)
data class NoteTagCrossRef(
    val noteId: Int,
    val tagId: Int
)
