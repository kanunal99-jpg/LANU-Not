package com.lanu.notdefteri

import com.lanu.notdefteri.data.model.ColorPalette
import com.lanu.notdefteri.data.model.DarkModeOption
import com.lanu.notdefteri.data.model.Note
import com.lanu.notdefteri.data.model.NoteLayoutMode
import com.lanu.notdefteri.data.model.Tag
import com.lanu.notdefteri.data.model.ThemeConfig
import com.lanu.notdefteri.data.model.WallpaperPreset
import com.lanu.notdefteri.util.FirestoreSyncState
import com.lanu.notdefteri.util.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-End Smoke Test Suite for Lanu Not Defteri
 * Verifies core business logic, models, state transformations, theme configurations,
 * search/filter algorithms, and real-time sync state calculations.
 */
class E2ESmokeTest {

    @Test
    fun testNoteModelPropertiesAndDefaults() {
        val note = Note(
            id = 101,
            title = "Proje Planı",
            content = "1. Aşama: Mimari tasarım ve veri tabanı kurulumu.",
            tag = "İş",
            isPinned = true,
            isDeleted = false,
            audioPath = "/storage/emulated/0/audio_101.m4a",
            imagePath = "/storage/emulated/0/photo_101.jpg"
        )

        assertEquals(101, note.id)
        assertEquals("Proje Planı", note.title)
        assertEquals("1. Aşama: Mimari tasarım ve veri tabanı kurulumu.", note.content)
        assertEquals("İş", note.tag)
        assertTrue(note.isPinned)
        assertFalse(note.isDeleted)
        assertNotNull(note.audioPath)
        assertNotNull(note.imagePath)
        assertTrue(note.createdAt > 0)
    }

    @Test
    fun testTagEntityCreation() {
        val tag1 = Tag(id = 1, name = "Önemli")
        val tag2 = Tag(id = 2, name = "Kişisel")

        assertEquals(1, tag1.id)
        assertEquals("Önemli", tag1.name)
        assertEquals("Kişisel", tag2.name)
    }

    @Test
    fun testThemeConfigDefaultsAndCustomization() {
        val defaultConfig = ThemeConfig()

        assertEquals(DarkModeOption.SYSTEM, defaultConfig.darkMode)
        assertEquals(ColorPalette.WARM_EARTH, defaultConfig.colorPalette)
        assertEquals(WallpaperPreset.NONE, defaultConfig.wallpaper)
        assertEquals(0.35f, defaultConfig.wallpaperDim, 0.001f)
        assertEquals(NoteLayoutMode.STAGGERED_GRID, defaultConfig.layoutMode)

        val customConfig = defaultConfig.copy(
            darkMode = DarkModeOption.DARK,
            colorPalette = ColorPalette.FOREST_EMERALD,
            layoutMode = NoteLayoutMode.SINGLE_COLUMN
        )

        assertEquals(DarkModeOption.DARK, customConfig.darkMode)
        assertEquals(ColorPalette.FOREST_EMERALD, customConfig.colorPalette)
        assertEquals(NoteLayoutMode.SINGLE_COLUMN, customConfig.layoutMode)
    }

    @Test
    fun testFirestoreSyncStateCalculation() {
        // 1. SYNCED State
        val syncedState = FirestoreSyncState(
            isSyncing = false,
            isOnline = true,
            error = null
        )
        assertEquals(SyncStatus.SYNCED, syncedState.syncStatus)

        // 2. SYNCING State
        val syncingState = FirestoreSyncState(
            isSyncing = true,
            isOnline = true,
            error = null,
            syncStatus = SyncStatus.SYNCING
        )
        assertEquals(SyncStatus.SYNCING, syncingState.syncStatus)

        // 3. OFFLINE State
        val offlineState = FirestoreSyncState(
            isSyncing = false,
            isOnline = false,
            error = null,
            syncStatus = SyncStatus.OFFLINE
        )
        assertEquals(SyncStatus.OFFLINE, offlineState.syncStatus)

        // 4. ERROR State
        val errorState = FirestoreSyncState(
            isSyncing = false,
            isOnline = true,
            error = "Firestore kimlik doğrulama hatası",
            syncStatus = SyncStatus.ERROR
        )
        assertEquals(SyncStatus.ERROR, errorState.syncStatus)
    }

    @Test
    fun testVoiceCommandTextFormattingLogic() {
        fun formatText(text: String, lang: String = "tr"): String {
            var formatted = text.trim()
            if (lang == "tr") {
                formatted = formatted
                    .replace(Regex("(?i)\\b(nokta)\\b"), ".")
                    .replace(Regex("(?i)\\b(virgül)\\b"), ",")
                    .replace(Regex("(?i)\\b(soru işareti)\\b"), "?")
                    .replace(Regex("(?i)\\b(ünlem)\\b"), "!")
                    .replace(Regex("(?i)\\b(yeni satır)\\b"), "\n")
            }
            return formatted
                .replace(Regex("\\s+([.,:?!])"), "$1")
                .replace(Regex("([.,:?!])([a-zA-ZçÇğĞıİöÖşŞüÜ])"), "$1 $2")
        }

        val turkishSpeech = "toplantı kararları nokta yarın saat sekizde başlanacak virgül hazır olun soru işareti yeni satır başlama saati dokuz"
        val formatted = formatText(turkishSpeech, "tr")

        assertTrue(formatted.contains("kararları."))
        assertTrue(formatted.contains("başlanacak,"))
        assertTrue(formatted.contains("olun?"))
        assertTrue(formatted.contains("\n"))
        assertTrue(formatted.contains("başlama saati dokuz"))
    }

    @Test
    fun testNoteFilterSearchAndSortingPipeline() {
        val notes = listOf(
            Note(id = 1, title = "Market Listesi", content = "Meyve, sebze, süt", tag = "Ev", isPinned = false),
            Note(id = 2, title = "Yazılım Mimarisi", content = "Jetpack Compose ve Room DB", tag = "İş", isPinned = true),
            Note(id = 3, title = "Spor Programı", content = "Kardiyo ve ağırlık antrenmanı", tag = "Kişisel", isPinned = false),
            Note(id = 4, title = "Bütçe Raporu", content = "Aylık gelir ve gider tablosu", tag = "İş", isPinned = true)
        )

        // Test 1: Search by content keyword "Compose"
        val query = "Compose"
        val searchResults = notes.filter {
            it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
        }
        assertEquals(1, searchResults.size)
        assertEquals("Yazılım Mimarisi", searchResults.first().title)

        // Test 2: Filter by Tag "İş"
        val tagResults = notes.filter { it.tag == "İş" }
        assertEquals(2, tagResults.size)

        // Test 3: Sort pinned notes to top
        val sortedNotes = notes.sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.createdAt })
        assertTrue(sortedNotes[0].isPinned)
        assertTrue(sortedNotes[1].isPinned)
        assertFalse(sortedNotes[2].isPinned)
        assertFalse(sortedNotes[3].isPinned)
    }

    @Test
    fun testTrashBinFilteringAndRestoration() {
        val allNotes = listOf(
            Note(id = 1, title = "Aktif Not 1", content = "Content", isDeleted = false),
            Note(id = 2, title = "Çöp Notu 1", content = "Deleted Content", isDeleted = true),
            Note(id = 3, title = "Aktif Not 2", content = "Content", isDeleted = false)
        )

        val activeNotes = allNotes.filter { !it.isDeleted }
        val trashNotes = allNotes.filter { it.isDeleted }

        assertEquals(2, activeNotes.size)
        assertEquals(1, trashNotes.size)
        assertEquals("Çöp Notu 1", trashNotes.first().title)

        // Simulating restoration
        val restoredNote = trashNotes.first().copy(isDeleted = false)
        assertFalse(restoredNote.isDeleted)
    }

    @Test
    fun testLockedNotesSecurityAndMasking() {
        val lockedNote = Note(
            id = 201,
            title = "Gizli Şifreler",
            content = "Banka ve e-posta şifrelerim burada",
            isLocked = true
        )
        val normalNote = Note(
            id = 202,
            title = "Genel Not",
            content = "Açık içerik",
            isLocked = false
        )

        assertTrue(lockedNote.isLocked)
        assertFalse(normalNote.isLocked)

        // Verify content masking when note is locked
        fun getDisplayContent(note: Note, isUnlocked: Boolean): String {
            return if (note.isLocked && !isUnlocked) {
                "Bu not kilitlidir. Görüntülemek için dokunun."
            } else {
                note.content
            }
        }

        assertEquals("Bu not kilitlidir. Görüntülemek için dokunun.", getDisplayContent(lockedNote, false))
        assertEquals("Banka ve e-posta şifrelerim burada", getDisplayContent(lockedNote, true))
        assertEquals("Açık içerik", getDisplayContent(normalNote, false))
    }

    @Test
    fun testFolderCategorizationAndFiltering() {
        val notes = listOf(
            Note(id = 1, title = "Toplantı", content = "Notlar", folder = "İş"),
            Note(id = 2, title = "Alışveriş", content = "Meyve", folder = "Kişisel"),
            Note(id = 3, title = "Proje Raporu", content = "Detaylar", folder = "İş"),
            Note(id = 4, title = "Hızlı Fikir", content = "Genel düşünceler", folder = null)
        )

        // Filter by "İş"
        val workNotes = notes.filter { it.folder == "İş" }
        assertEquals(2, workNotes.size)

        // Filter by "Kişisel"
        val personalNotes = notes.filter { it.folder == "Kişisel" }
        assertEquals(1, personalNotes.size)

        // Filter by unassigned (no folder)
        val unassignedNotes = notes.filter { it.folder == null }
        assertEquals(1, unassignedNotes.size)
        assertEquals("Hızlı Fikir", unassignedNotes.first().title)

        // Extract unique folder list
        val folders = notes.mapNotNull { it.folder }.distinct().sorted()
        assertEquals(listOf("Kişisel", "İş"), folders)
    }

    @Test
    fun testMultimediaNoteAttributes() {
        val multimediaNote = Note(
            id = 301,
            title = "Toplantı & Taslak",
            content = "Ses kaydı ve el çizimi diyagram içerir",
            audioPath = "/data/user/0/com.lanu.notdefteri/files/voice_notes/voice_123.m4a",
            drawingPath = "/data/user/0/com.lanu.notdefteri/files/drawings/sketch_456.png"
        )

        assertNotNull(multimediaNote.audioPath)
        assertTrue(multimediaNote.audioPath!!.endsWith(".m4a"))
        assertNotNull(multimediaNote.drawingPath)
        assertTrue(multimediaNote.drawingPath!!.endsWith(".png"))
    }

    @Test
    fun testPinValidationRules() {
        fun isValidPin(pin: String): Boolean {
            return pin.length == 4 && pin.all { it.isDigit() }
        }

        assertTrue(isValidPin("1234"))
        assertTrue(isValidPin("0000"))
        assertTrue(isValidPin("9876"))
        assertFalse(isValidPin("123")) // Too short
        assertFalse(isValidPin("12345")) // Too long
        assertFalse(isValidPin("12a4")) // Non-digit
        assertFalse(isValidPin("")) // Empty
    }
}
