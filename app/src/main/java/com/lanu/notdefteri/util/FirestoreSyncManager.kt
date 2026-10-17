package com.lanu.notdefteri.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.lanu.notdefteri.data.model.Note
import com.lanu.notdefteri.data.repository.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class SyncStatus {
    SYNCED,
    SYNCING,
    OFFLINE,
    ERROR
}

data class FirestoreSyncState(
    val isSyncing: Boolean = false,
    val isAutoSyncEnabled: Boolean = false,
    val lastSyncTime: Long? = null,
    val syncId: String = "",
    val customProjectId: String = "",
    val cloudNotesCount: Int = 0,
    val statusMessage: String? = null,
    val error: String? = null,
    val isOnline: Boolean = true,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

data class SyncResult(
    val uploadedCount: Int,
    val downloadedCount: Int,
    val totalCount: Int
)

class FirestoreSyncManager(
    private val context: Context,
    private val repository: NoteRepository
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("lanu_firestore_sync_prefs", Context.MODE_PRIVATE)

    private val networkObserver = NetworkObserver(context)

    private val _state = MutableStateFlow(FirestoreSyncState())
    val state: StateFlow<FirestoreSyncState> = _state.asStateFlow()

    private var firestoreInstance: FirebaseFirestore? = null
    private var snapshotListener: ListenerRegistration? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        val savedSyncId = prefs.getString("sync_id", null) ?: generateNewSyncId().also {
            prefs.edit().putString("sync_id", it).apply()
        }
        val autoSync = prefs.getBoolean("auto_sync", true)
        val lastSync = if (prefs.contains("last_sync_time")) prefs.getLong("last_sync_time", 0L) else null
        val customProj = prefs.getString("custom_project_id", "") ?: ""
        val initialOnline = networkObserver.isNetworkAvailable()

        _state.update {
            val status = calculateSyncStatus(isSyncing = false, isOnline = initialOnline, error = null)
            it.copy(
                syncId = savedSyncId,
                isAutoSyncEnabled = autoSync,
                lastSyncTime = if (lastSync != 0L) lastSync else null,
                customProjectId = customProj,
                isOnline = initialOnline,
                syncStatus = status
            )
        }

        // Observe network state in real-time
        scope.launch {
            networkObserver.status.collect { netStatus ->
                val online = netStatus == NetworkStatus.AVAILABLE
                updateSyncState { current ->
                    current.copy(isOnline = online)
                }
            }
        }

        initializeFirestore()

        if (autoSync) {
            startRealtimeListener()
        }
    }

    private fun calculateSyncStatus(isSyncing: Boolean, isOnline: Boolean, error: String?): SyncStatus {
        return when {
            !isOnline -> SyncStatus.OFFLINE
            isSyncing -> SyncStatus.SYNCING
            error != null -> SyncStatus.ERROR
            else -> SyncStatus.SYNCED
        }
    }

    private fun updateSyncState(transform: (FirestoreSyncState) -> FirestoreSyncState) {
        _state.update { current ->
            val updated = transform(current)
            val newStatus = calculateSyncStatus(updated.isSyncing, updated.isOnline, updated.error)
            updated.copy(syncStatus = newStatus)
        }
    }

    private fun generateNewSyncId(): String {
        val randomPart = UUID.randomUUID().toString().take(8).uppercase()
        return "LANU-$randomPart"
    }

    fun setSyncId(newSyncId: String) {
        val trimmed = newSyncId.trim().uppercase()
        if (trimmed.isNotBlank()) {
            prefs.edit().putString("sync_id", trimmed).apply()
            updateSyncState { it.copy(syncId = trimmed, statusMessage = "Eşitleme Kimliği güncellendi: $trimmed") }
            restartRealtimeListener()
        }
    }

    fun setCustomProjectId(projectId: String) {
        val trimmed = projectId.trim()
        prefs.edit().putString("custom_project_id", trimmed).apply()
        updateSyncState { it.copy(customProjectId = trimmed) }
        firestoreInstance = null
        initializeFirestore()
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_sync", enabled).apply()
        updateSyncState { it.copy(isAutoSyncEnabled = enabled) }
        if (enabled) {
            startRealtimeListener()
        } else {
            stopRealtimeListener()
        }
    }

    private fun initializeFirestore(): FirebaseFirestore? {
        if (firestoreInstance != null) return firestoreInstance

        return try {
            val app = if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseApp.getInstance()
            } else {
                val customProj = _state.value.customProjectId.ifBlank { "lanu-not-defteri" }
                val options = FirebaseOptions.Builder()
                    .setApplicationId(context.packageName)
                    .setApiKey("AIzaSy_LanuNotes_Firestore_Sync_Client")
                    .setProjectId(customProj)
                    .build()
                FirebaseApp.initializeApp(context, options, "LanuFirestoreSync")
            }

            val db = FirebaseFirestore.getInstance(app)
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                db.firestoreSettings = settings
            } catch (e: Exception) {
                // Settings might have already been locked
            }
            firestoreInstance = db
            db
        } catch (e: Exception) {
            e.printStackTrace()
            updateSyncState { it.copy(error = "Firestore başlatılamadı: ${e.localizedMessage}") }
            null
        }
    }

    suspend fun backupToFirestore(): Result<Int> {
        updateSyncState { it.copy(isSyncing = true, statusMessage = "Notlar buluta yükleniyor...", error = null) }
        return try {
            val db = initializeFirestore() ?: throw IllegalStateException("Firestore servisi kullanılamıyor.")
            val syncId = _state.value.syncId
            val localNotes = repository.getAllNotesList()

            // A backup must never silently replace a different note in the vault.
            val existingCloudNotes = db.collection("sync_vault")
                .document(syncId).collection("notes").get().awaitTask()
                .documents.map { document ->
                    mapDocumentToNote(document) ?: throw IllegalStateException(
                        "Buluttaki not kaydı okunamadı; sessizce atlanmadı."
                    )
                }
            NoteSyncConflictGuard.requireNoConflicts(
                repository.getAllNotesIncludingTrashList(), existingCloudNotes
            )

            val batch = db.batch()
            val vaultRef = db.collection("sync_vault").document(syncId)

            // Update metadata
            val meta = hashMapOf(
                "lastSyncTime" to System.currentTimeMillis(),
                "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "notesCount" to localNotes.size
            )
            batch.set(vaultRef, meta, SetOptions.merge())

            // Upload each note
            val notesCollection = vaultRef.collection("notes")
            for (note in localNotes) {
                val noteDoc = notesCollection.document(note.id.toString())
                val noteData = hashMapOf(
                    "id" to note.id,
                    "title" to note.title,
                    "content" to note.content,
                    "createdAt" to note.createdAt,
                    "updatedAt" to System.currentTimeMillis(),
                    "isPinned" to note.isPinned,
                    "tag" to (note.tag ?: ""),
                    "imagePath" to (note.imagePath ?: ""),
                    "reminderTime" to (note.reminderTime ?: 0L)
                )
                noteData.putAll(CloudNoteTrashFields.encode(note))
                batch.set(noteDoc, noteData, SetOptions.merge())
            }

            batch.commit().awaitTask()

            val now = System.currentTimeMillis()
            prefs.edit().putLong("last_sync_time", now).apply()
            updateSyncState {
                it.copy(
                    isSyncing = false,
                    lastSyncTime = now,
                    cloudNotesCount = localNotes.size,
                    statusMessage = "${localNotes.size} not buluta başarıyla yedeklendi!",
                    error = null
                )
            }
            Result.success(localNotes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            updateSyncState {
                it.copy(
                    isSyncing = false,
                    error = "Yedekleme başarısız oldu: ${e.localizedMessage}",
                    statusMessage = null
                )
            }
            Result.failure(e)
        }
    }

    suspend fun restoreFromFirestore(): Result<Int> {
        updateSyncState { it.copy(isSyncing = true, statusMessage = "Buluttan notlar indiriliyor...", error = null) }
        return try {
            val db = initializeFirestore() ?: throw IllegalStateException("Firestore servisi kullanılamıyor.")
            val syncId = _state.value.syncId
            val snapshot = db.collection("sync_vault")
                .document(syncId)
                .collection("notes")
                .get()
                .awaitTask()

            val restoredNotes = snapshot.documents.map { doc ->
                mapDocumentToNote(doc) ?: throw IllegalStateException(
                    "Buluttaki not kaydı okunamadı; geri yükleme durduruldu."
                )
            }
            NoteSyncConflictGuard.requireUniqueRemoteIds(restoredNotes)

            // Room REPLACE would overwrite a changed local note (including trash).
            // Fail before ANY local write when a note ID has diverged.
            NoteSyncConflictGuard.requireNoConflicts(
                repository.getAllNotesIncludingTrashList(), restoredNotes
            )
            if (restoredNotes.isNotEmpty()) {
                repository.insertNotes(restoredNotes)
            }

            val now = System.currentTimeMillis()
            prefs.edit().putLong("last_sync_time", now).apply()
            updateSyncState {
                it.copy(
                    isSyncing = false,
                    lastSyncTime = now,
                    cloudNotesCount = restoredNotes.size,
                    statusMessage = "${restoredNotes.size} not buluttan başarıyla geri yüklendi!",
                    error = null
                )
            }
            Result.success(restoredNotes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            updateSyncState {
                it.copy(
                    isSyncing = false,
                    error = "Geri yükleme başarısız oldu: ${e.localizedMessage}",
                    statusMessage = null
                )
            }
            Result.failure(e)
        }
    }

    suspend fun syncTwoWay(): Result<SyncResult> {
        updateSyncState { it.copy(isSyncing = true, statusMessage = "İki yönlü eşitleme başlatılıyor...", error = null) }
        return try {
            val db = initializeFirestore() ?: throw IllegalStateException("Firestore servisi kullanılamıyor.")
            val syncId = _state.value.syncId

            // 1. Fetch remote notes
            val snapshot = db.collection("sync_vault")
                .document(syncId)
                .collection("notes")
                .get()
                .awaitTask()

            val remoteNotes = snapshot.documents.map { doc ->
                mapDocumentToNote(doc) ?: throw IllegalStateException(
                    "Buluttaki not kaydı okunamadı; eşitleme durduruldu."
                )
            }
            // Validate before associateBy; map construction would silently
            // discard the first of two cloud documents claiming the same ID.
            NoteSyncConflictGuard.requireUniqueRemoteIds(remoteNotes)
            val remoteNotesMap = remoteNotes.associateBy { it.id }

            // 2. Fetch local notes
            val localNotes = repository.getAllNotesList()
            // createdAt is not an edit timestamp. Never pick either side by
            // creation date when the same note ID has different data.
            NoteSyncConflictGuard.requireNoConflicts(
                repository.getAllNotesIncludingTrashList(), remoteNotesMap.values.toList()
            )
            val localNotesMap = localNotes.associateBy { it.id }

            var downloadedCount = 0
            var uploadedCount = 0

            val notesToInsertLocally = mutableListOf<Note>()
            val notesToUpload = mutableListOf<Note>()

            // Notes in remote but not local -> download
            for ((id, remoteNote) in remoteNotesMap) {
                val localNote = localNotesMap[id]
                if (localNote == null) {
                    notesToInsertLocally.add(remoteNote)
                    downloadedCount++
                } else if (remoteNote.createdAt > localNote.createdAt) {
                    notesToInsertLocally.add(remoteNote)
                    downloadedCount++
                }
            }

            // Notes in local but not remote or newer locally -> upload
            for ((id, localNote) in localNotesMap) {
                val remoteNote = remoteNotesMap[id]
                if (remoteNote == null || localNote.createdAt >= remoteNote.createdAt) {
                    notesToUpload.add(localNote)
                    if (remoteNote == null) uploadedCount++
                }
            }

            // Apply local changes
            if (notesToInsertLocally.isNotEmpty()) {
                repository.insertNotes(notesToInsertLocally)
            }

            // Apply remote changes
            if (notesToUpload.isNotEmpty()) {
                val batch = db.batch()
                val notesColl = db.collection("sync_vault").document(syncId).collection("notes")
                for (note in notesToUpload) {
                    val docRef = notesColl.document(note.id.toString())
                    val noteData = hashMapOf(
                        "id" to note.id,
                        "title" to note.title,
                        "content" to note.content,
                        "createdAt" to note.createdAt,
                        "updatedAt" to System.currentTimeMillis(),
                        "isPinned" to note.isPinned,
                        "tag" to (note.tag ?: ""),
                        "imagePath" to (note.imagePath ?: ""),
                        "reminderTime" to (note.reminderTime ?: 0L),
                        "latitude" to (note.latitude ?: 0.0),
                        "longitude" to (note.longitude ?: 0.0),
                        "locationName" to (note.locationName ?: ""),
                        "audioPath" to (note.audioPath ?: ""),
                        "isLocked" to note.isLocked,
                        "folder" to (note.folder ?: ""),
                        "drawingPath" to (note.drawingPath ?: "")
                    )
                    noteData.putAll(CloudNoteTrashFields.encode(note))
                    batch.set(docRef, noteData, SetOptions.merge())
                }
                batch.commit().awaitTask()
            }

            val now = System.currentTimeMillis()
            prefs.edit().putLong("last_sync_time", now).apply()
            val totalNotes = repository.getAllNotesList().size

            val result = SyncResult(
                uploadedCount = uploadedCount,
                downloadedCount = downloadedCount,
                totalCount = totalNotes
            )

            updateSyncState {
                it.copy(
                    isSyncing = false,
                    lastSyncTime = now,
                    cloudNotesCount = totalNotes,
                    statusMessage = "Eşitleme tamamlandı: $uploadedCount yüklendi, $downloadedCount indirildi.",
                    error = null
                )
            }
            Result.success(result)
        } catch (e: Exception) {
            e.printStackTrace()
            updateSyncState {
                it.copy(
                    isSyncing = false,
                    error = "Eşitleme hatası: ${e.localizedMessage}",
                    statusMessage = null
                )
            }
            Result.failure(e)
        }
    }

    fun onNoteCreatedOrUpdated(note: Note) {
        if (!_state.value.isAutoSyncEnabled) return
        scope.launch {
            try {
                val db = initializeFirestore() ?: return@launch
                val syncId = _state.value.syncId
                val noteData = hashMapOf(
                    "id" to note.id,
                    "title" to note.title,
                    "content" to note.content,
                    "createdAt" to note.createdAt,
                    "updatedAt" to System.currentTimeMillis(),
                    "isPinned" to note.isPinned,
                    "tag" to (note.tag ?: ""),
                    "imagePath" to (note.imagePath ?: ""),
                    "reminderTime" to (note.reminderTime ?: 0L),
                    "latitude" to (note.latitude ?: 0.0),
                    "longitude" to (note.longitude ?: 0.0),
                    "locationName" to (note.locationName ?: ""),
                    "audioPath" to (note.audioPath ?: ""),
                    "isLocked" to note.isLocked,
                    "folder" to (note.folder ?: ""),
                    "drawingPath" to (note.drawingPath ?: "")
                )
                noteData.putAll(CloudNoteTrashFields.encode(note))
                db.collection("sync_vault")
                    .document(syncId)
                    .collection("notes")
                    .document(note.id.toString())
                    .set(noteData, SetOptions.merge())
                    .awaitTask()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onNoteDeleted(noteId: Int) {
        if (!_state.value.isAutoSyncEnabled) return
        scope.launch {
            try {
                val db = initializeFirestore() ?: return@launch
                val syncId = _state.value.syncId
                db.collection("sync_vault")
                    .document(syncId)
                    .collection("notes")
                    .document(noteId.toString())
                    .delete()
                    .awaitTask()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startRealtimeListener() {
        stopRealtimeListener()
        try {
            val db = initializeFirestore() ?: return
            val syncId = _state.value.syncId
            snapshotListener = db.collection("sync_vault")
                .document(syncId)
                .collection("notes")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        _state.update { it.copy(cloudNotesCount = snapshot.size()) }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRealtimeListener() {
        snapshotListener?.remove()
        snapshotListener = null
    }

    private fun restartRealtimeListener() {
        stopRealtimeListener()
        if (_state.value.isAutoSyncEnabled) {
            startRealtimeListener()
        }
    }

    private fun mapDocumentToNote(doc: DocumentSnapshot): Note? {
        val data = doc.data ?: return null
        val id = (data["id"] as? Number)?.toInt() ?: doc.id.toIntOrNull() ?: return null
        val title = data["title"] as? String ?: ""
        val content = data["content"] as? String ?: ""
        val createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        val isPinned = data["isPinned"] as? Boolean ?: false
        val tag = (data["tag"] as? String)?.ifBlank { null }
        val imagePath = (data["imagePath"] as? String)?.ifBlank { null }
        val reminderTime = (data["reminderTime"] as? Number)?.toLong()?.takeIf { it > 0L }
        val latitude = (data["latitude"] as? Number)?.toDouble()?.takeIf { it != 0.0 }
        val longitude = (data["longitude"] as? Number)?.toDouble()?.takeIf { it != 0.0 }
        val locationName = (data["locationName"] as? String)?.ifBlank { null }
        val audioPath = (data["audioPath"] as? String)?.ifBlank { null }
        val isDeleted = data["isDeleted"] as? Boolean ?: false
        val deletedTimestamp = (data["deletedTimestamp"] as? Number)?.toLong()?.takeIf { it > 0L }
        val isLocked = data["isLocked"] as? Boolean ?: false
        val folder = (data["folder"] as? String)?.ifBlank { null }
        val drawingPath = (data["drawingPath"] as? String)?.ifBlank { null }

        return Note(
            id = id,
            title = title,
            content = content,
            createdAt = createdAt,
            isPinned = isPinned,
            tag = tag,
            imagePath = imagePath,
            reminderTime = reminderTime,
            latitude = latitude,
            longitude = longitude,
            locationName = locationName,
            audioPath = audioPath,
            isDeleted = isDeleted,
            deletedTimestamp = deletedTimestamp,
            isLocked = isLocked,
            folder = folder,
            drawingPath = drawingPath
        )
    }

    fun clearError() {
        updateSyncState { it.copy(error = null) }
    }

    fun shutdown() {
        stopRealtimeListener()
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { exception -> cont.resumeWithException(exception) }
    addOnCanceledListener { cont.cancel() }
}
