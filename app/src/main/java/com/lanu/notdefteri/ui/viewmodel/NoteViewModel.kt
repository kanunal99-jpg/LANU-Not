package com.lanu.notdefteri.ui.viewmodel

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lanu.notdefteri.data.local.AppDatabase
import com.lanu.notdefteri.data.model.Note
import com.lanu.notdefteri.data.model.Tag
import com.lanu.notdefteri.data.model.NoteWithTags
import com.lanu.notdefteri.data.repository.NoteRepository
import com.lanu.notdefteri.data.model.ColorPalette
import com.lanu.notdefteri.data.model.DarkModeOption
import com.lanu.notdefteri.data.model.NoteLayoutMode
import com.lanu.notdefteri.data.model.ThemeConfig
import com.lanu.notdefteri.data.model.WallpaperPreset
import com.lanu.notdefteri.util.ReminderScheduler
import com.lanu.notdefteri.worker.LocationReminderWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.lanu.notdefteri.util.VoiceToTextParser
import com.lanu.notdefteri.util.TextToSpeechHelper
import com.lanu.notdefteri.util.ThemePreferencesManager
import com.lanu.notdefteri.util.FirestoreSyncManager
import com.lanu.notdefteri.util.SyncResult
import com.lanu.notdefteri.widget.NotesWidgetProvider
import com.lanu.notdefteri.ai.GeminiHelper
import com.lanu.notdefteri.ai.DetectedEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    val parser = VoiceToTextParser(application)
    val ttsHelper = TextToSpeechHelper(application)
    val syncManager: FirestoreSyncManager
    val themeManager = ThemePreferencesManager(application)
    val themeConfig: StateFlow<ThemeConfig> = themeManager.themeConfig
    private val geminiHelper = GeminiHelper()

    private val _detectedEvents = MutableStateFlow<List<DetectedEvent>>(emptySet<DetectedEvent>().toList())
    val detectedEvents: StateFlow<List<DetectedEvent>> = _detectedEvents.asStateFlow()

    private val _isDetecting = MutableStateFlow(false)
    val isDetecting: StateFlow<Boolean> = _isDetecting.asStateFlow()

    fun detectEvents(content: String) {
        viewModelScope.launch {
            _isDetecting.value = true
            _detectedEvents.value = geminiHelper.detectEvents(content)
            _isDetecting.value = false
        }
    }

    fun clearDetectedEvents() {
        _detectedEvents.value = emptyList()
    }

    fun setDarkMode(mode: DarkModeOption) = themeManager.setDarkMode(mode)
    fun setColorPalette(palette: ColorPalette) = themeManager.setColorPalette(palette)
    fun setWallpaper(preset: WallpaperPreset) = themeManager.setWallpaper(preset)
    fun setCustomWallpaperUri(uri: String?) = themeManager.setCustomWallpaperUri(uri)
    fun setWallpaperDim(dim: Float) = themeManager.setWallpaperDim(dim)
    fun setLayoutMode(mode: NoteLayoutMode) = themeManager.setLayoutMode(mode)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags = _selectedTags.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder = _selectedFolder.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = NoteRepository(db.noteDao(), db.tagDao())
        syncManager = FirestoreSyncManager(application, repository)
        
        // Seed default tags if empty & purge trash older than 30 days
        viewModelScope.launch {
            try {
                // Purge notes deleted > 30 days ago
                val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000L
                val cutoffTime = System.currentTimeMillis() - thirtyDaysInMillis
                repository.purgeExpiredTrashNotes(cutoffTime)

                if (db.tagDao().getAllTags().first().isEmpty()) {
                    listOf("Kişisel", "İş", "Fikirler", "Yapılacaklar").forEach { name ->
                        db.tagDao().insertTag(Tag(name = name))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val allNotesState: StateFlow<List<NoteWithTags>> = repository.allNotesWithTags
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val trashNotesState: StateFlow<List<NoteWithTags>> = repository.trashNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val trashCountState: StateFlow<Int> = repository.trashCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val allFoldersState: StateFlow<List<String>> = repository.allFolders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val folders: StateFlow<List<String>> = allFoldersState

    val notesState: StateFlow<List<NoteWithTags>> = combine(
        repository.allNotesWithTags,
        _searchQuery,
        _selectedTags,
        _selectedFolder
    ) { notesWithTags, query, tags, folder ->
        notesWithTags.filter { item ->
            val note = item.note
            val matchesQuery = query.isBlank() || 
                note.title.contains(query, ignoreCase = true) || 
                note.content.contains(query, ignoreCase = true) ||
                item.tags.any { it.name.contains(query, ignoreCase = true) }
            val matchesTag = tags.isEmpty() || item.tags.any { tags.contains(it.name) }
            val matchesFolder = folder == null || note.folder == folder
            matchesQuery && matchesTag && matchesFolder
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val tagsState: StateFlow<List<Tag>> = repository.allTags
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    suspend fun addTagAndGetId(name: String): Int {
        if (name.isBlank()) return -1
        val trimmed = name.trim()
        val existing = repository.allTags.firstOrNull()?.find { it.name.equals(trimmed, ignoreCase = true) }
        return if (existing != null) {
            existing.id
        } else {
            repository.insertTag(Tag(name = trimmed)).toInt()
        }
    }

    fun addTag(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.insertTag(Tag(name = name.trim()))
            }
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            repository.deleteTag(tag)
            _selectedTags.value = _selectedTags.value - tag.name
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleTagFilter(tag: String) {
        val current = _selectedTags.value
        if (current.contains(tag)) {
            _selectedTags.value = current - tag
        } else {
            _selectedTags.value = current + tag
        }
    }

    fun clearTagFilters() {
        _selectedTags.value = emptySet()
    }

    fun setSelectedFolder(folder: String?) {
        _selectedFolder.value = folder
    }

    fun toggleNoteLock(note: Note) {
        viewModelScope.launch {
            val updated = note.copy(isLocked = !note.isLocked)
            repository.updateNoteLock(note.id, updated.isLocked)
            syncManager.onNoteCreatedOrUpdated(updated)
            updateWidgets()
        }
    }

    fun updateNoteFolder(noteId: Int, folder: String?) {
        viewModelScope.launch {
            repository.updateNoteFolder(noteId, folder)
            updateWidgets()
        }
    }

    fun saveNote(
        title: String,
        content: String,
        id: Int = 0,
        isPinned: Boolean = false,
        tagIds: List<Int> = emptyList(),
        imagePath: String? = null,
        reminderTime: Long? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null,
        audioPath: String? = null,
        isLocked: Boolean = false,
        folder: String? = null,
        drawingPath: String? = null
    ) {
        viewModelScope.launch {
            saveNoteAndGetId(
                title, content, id, isPinned, tagIds, imagePath, reminderTime,
                latitude, longitude, locationName, audioPath, isLocked, folder, drawingPath
            )
        }
    }

    suspend fun saveNoteAndGetId(
        title: String,
        content: String,
        id: Int = 0,
        isPinned: Boolean = false,
        tagIds: List<Int> = emptyList(),
        imagePath: String? = null,
        reminderTime: Long? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        locationName: String? = null,
        audioPath: String? = null,
        isLocked: Boolean = false,
        folder: String? = null,
        drawingPath: String? = null
    ): Int {
        val note = Note(
            id = id,
            title = title.ifBlank { "Başlıksız Not" },
            content = content,
            isPinned = isPinned,
            tag = null, // Old tag field no longer used for multiple tags
            imagePath = imagePath,
            reminderTime = reminderTime,
            latitude = latitude,
            longitude = longitude,
            locationName = locationName,
            audioPath = audioPath,
            isLocked = isLocked,
            folder = folder,
            drawingPath = drawingPath
        )
        
        val resultId = repository.updateNoteWithTags(note, tagIds).toInt()
        
        // Let's re-fetch the note to be sure we have the right object for sync
        val savedNote = repository.getNoteById(resultId) ?: note.copy(id = resultId)

        // Schedule or cancel WorkManager reminder as needed
        if (reminderTime != null && reminderTime > System.currentTimeMillis()) {
            ReminderScheduler.scheduleReminder(getApplication(), savedNote, reminderTime)
        } else if (reminderTime == null && id != 0) {
            ReminderScheduler.cancelReminder(getApplication(), id)
        }

        if (latitude != null && longitude != null) {
            val locationWorkRequest = PeriodicWorkRequestBuilder<LocationReminderWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
                "lanu_location_reminder_work",
                ExistingPeriodicWorkPolicy.KEEP,
                locationWorkRequest
            )
        }

        syncManager.onNoteCreatedOrUpdated(savedNote)
        updateWidgets()
        return resultId
    }

    fun scheduleReminder(note: Note, triggerAtMillis: Long) {
        viewModelScope.launch {
            val updated = note.copy(reminderTime = triggerAtMillis)
            repository.updateReminderTime(note.id, triggerAtMillis)
            ReminderScheduler.scheduleReminder(getApplication(), updated, triggerAtMillis)
            syncManager.onNoteCreatedOrUpdated(updated)
            updateWidgets()
        }
    }

    fun cancelReminder(note: Note) {
        viewModelScope.launch {
            val updated = note.copy(reminderTime = null)
            repository.updateReminderTime(note.id, null)
            ReminderScheduler.cancelReminder(getApplication(), note.id)
            syncManager.onNoteCreatedOrUpdated(updated)
            updateWidgets()
        }
    }

    fun moveToTrash(note: Note) {
        viewModelScope.launch {
            ReminderScheduler.cancelReminder(getApplication(), note.id)
            val now = System.currentTimeMillis()
            repository.moveToTrash(note.id, now)
            syncManager.onNoteCreatedOrUpdated(note.copy(isDeleted = true, deletedTimestamp = now))
            updateWidgets()
        }
    }

    fun restoreFromTrash(note: Note) {
        viewModelScope.launch {
            repository.restoreFromTrash(note.id)
            syncManager.onNoteCreatedOrUpdated(note.copy(isDeleted = false, deletedTimestamp = null))
            updateWidgets()
        }
    }

    fun permanentlyDeleteNote(note: Note) {
        viewModelScope.launch {
            ReminderScheduler.cancelReminder(getApplication(), note.id)
            repository.deleteNote(note)
            syncManager.onNoteDeleted(note.id)
            updateWidgets()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            updateWidgets()
        }
    }

    fun deleteNote(note: Note) {
        moveToTrash(note)
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            val updated = note.copy(isPinned = !note.isPinned)
            repository.updateNote(updated)
            syncManager.onNoteCreatedOrUpdated(updated)
            updateWidgets()
        }
    }

    fun toggleNoteLock(noteId: Int, isLocked: Boolean) {
        viewModelScope.launch {
            val existing = repository.getNoteById(noteId) ?: return@launch
            val updated = existing.copy(isLocked = isLocked)
            repository.updateNote(updated)
            syncManager.onNoteCreatedOrUpdated(updated)
            updateWidgets()
        }
    }

    fun backupToCloud(onComplete: ((Result<Int>) -> Unit)? = null) {
        viewModelScope.launch {
            val res = syncManager.backupToFirestore()
            onComplete?.invoke(res)
        }
    }

    fun restoreFromCloud(onComplete: ((Result<Int>) -> Unit)? = null) {
        viewModelScope.launch {
            val res = syncManager.restoreFromFirestore()
            onComplete?.invoke(res)
        }
    }

    fun syncWithCloud(onComplete: ((Result<SyncResult>) -> Unit)? = null) {
        viewModelScope.launch {
            val res = syncManager.syncTwoWay()
            onComplete?.invoke(res)
        }
    }

    private fun updateWidgets() {
        try {
            val context = getApplication<Application>()
            val intent = Intent(context, NotesWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(ComponentName(context, NotesWidgetProvider::class.java))
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getNoteWithTagsById(id: Int): NoteWithTags? {
        return repository.getNoteWithTagsById(id)
    }

    suspend fun getNoteById(id: Int): NoteWithTags? {
        return repository.getNoteWithTagsById(id)
    }

    override fun onCleared() {
        super.onCleared()
        parser.destroy()
        ttsHelper.shutdown()
        syncManager.shutdown()
    }
}
