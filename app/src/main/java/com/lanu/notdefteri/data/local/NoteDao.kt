package com.lanu.notdefteri.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lanu.notdefteri.data.model.Note
import com.lanu.notdefteri.data.model.NoteTagCrossRef
import com.lanu.notdefteri.data.model.NoteWithTags
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Transaction
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, createdAt DESC")
    fun getAllNotesWithTags(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteWithTagsById(id: Int): NoteWithTags?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoteTagCrossRef(crossRef: NoteTagCrossRef)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId")
    suspend fun deleteTagsForNote(noteId: Int)

    @Transaction
    suspend fun updateNoteWithTags(note: Note, tags: List<Int>): Long {
        val noteId = if (note.id == 0) insertNote(note) else { updateNote(note); note.id.toLong() }
        deleteTagsForNote(noteId.toInt())
        tags.forEach { tagId ->
            insertNoteTagCrossRef(NoteTagCrossRef(noteId.toInt(), tagId))
        }
        return noteId
    }
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, createdAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedTimestamp DESC")
    fun getTrashNotes(): Flow<List<NoteWithTags>>

    @Query("SELECT COUNT(*) FROM notes WHERE isDeleted = 1")
    fun getTrashCount(): Flow<Int>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Int): Note?

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, createdAt DESC")
    suspend fun getAllNotesList(): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<Note>): List<Long>

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    @Query("UPDATE notes SET isDeleted = 1, deletedTimestamp = :deletedAt WHERE id = :id")
    suspend fun moveToTrash(id: Int, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isDeleted = 0, deletedTimestamp = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: Int)

    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedTimestamp <= :cutoffTimestamp")
    suspend fun purgeExpiredTrashNotes(cutoffTimestamp: Long)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("UPDATE notes SET reminderTime = :reminderTime WHERE id = :noteId")
    suspend fun updateReminderTime(noteId: Int, reminderTime: Long?)

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND reminderTime IS NOT NULL AND reminderTime > :currentTime ORDER BY reminderTime ASC")
    fun getNotesWithActiveReminders(currentTime: Long): Flow<List<Note>>

    @Query("SELECT DISTINCT folder FROM notes WHERE isDeleted = 0 AND folder IS NOT NULL AND folder != '' ORDER BY folder ASC")
    fun getAllFolders(): Flow<List<String>>

    @Query("UPDATE notes SET isLocked = :isLocked WHERE id = :noteId")
    suspend fun updateNoteLock(noteId: Int, isLocked: Boolean)

    @Query("UPDATE notes SET folder = :folder WHERE id = :noteId")
    suspend fun updateNoteFolder(noteId: Int, folder: String?)
}
