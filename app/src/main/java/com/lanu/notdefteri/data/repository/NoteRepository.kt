package com.lanu.notdefteri.data.repository

import com.lanu.notdefteri.data.local.NoteDao
import com.lanu.notdefteri.data.local.TagDao
import com.lanu.notdefteri.data.model.Note
import com.lanu.notdefteri.data.model.Tag
import com.lanu.notdefteri.data.model.NoteWithTags
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao,
    private val tagDao: TagDao
) {
    val allNotesWithTags: Flow<List<NoteWithTags>> = noteDao.getAllNotesWithTags()
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()
    val trashNotes: Flow<List<NoteWithTags>> = noteDao.getTrashNotes()
    val trashCount: Flow<Int> = noteDao.getTrashCount()
    val allTags: Flow<List<Tag>> = tagDao.getAllTags()
    val allFolders: Flow<List<String>> = noteDao.getAllFolders()

    suspend fun getNoteById(id: Int): Note? = noteDao.getNoteById(id)

    suspend fun getNoteWithTagsById(id: Int): NoteWithTags? = noteDao.getNoteWithTagsById(id)

    suspend fun getAllNotesList(): List<Note> = noteDao.getAllNotesList()

    suspend fun getAllNotesIncludingTrashList(): List<Note> = noteDao.getAllNotesIncludingTrashList()

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    suspend fun insertNotes(notes: List<Note>) = noteDao.insertNotes(notes)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun updateNoteWithTags(note: Note, tagIds: List<Int>): Long = noteDao.updateNoteWithTags(note, tagIds)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    suspend fun deleteNoteById(id: Int) = noteDao.deleteNoteById(id)

    suspend fun moveToTrash(id: Int, deletedAt: Long = System.currentTimeMillis()) = noteDao.moveToTrash(id, deletedAt)

    suspend fun restoreFromTrash(id: Int) = noteDao.restoreFromTrash(id)

    suspend fun purgeExpiredTrashNotes(cutoffTimestamp: Long) = noteDao.purgeExpiredTrashNotes(cutoffTimestamp)

    suspend fun emptyTrash() = noteDao.emptyTrash()

    suspend fun updateReminderTime(noteId: Int, reminderTime: Long?) = noteDao.updateReminderTime(noteId, reminderTime)

    fun getNotesWithActiveReminders(currentTime: Long): Flow<List<Note>> = noteDao.getNotesWithActiveReminders(currentTime)

    suspend fun insertTag(tag: Tag): Long = tagDao.insertTag(tag)

    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag)

    suspend fun updateNoteLock(noteId: Int, isLocked: Boolean) = noteDao.updateNoteLock(noteId, isLocked)

    suspend fun updateNoteFolder(noteId: Int, folder: String?) = noteDao.updateNoteFolder(noteId, folder)
}
