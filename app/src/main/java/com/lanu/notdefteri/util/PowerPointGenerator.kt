package com.lanu.notdefteri.util

import android.content.Context
import com.lanu.notdefteri.data.model.Note
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates presentation files for notes sharing.
 */
object PowerPointGenerator {

    data class NoteBlock(
        val title: String,
        val body: String
    )

    fun parseNoteBlocks(note: Note): List<NoteBlock> {
        val cleanContent = RichTextHelper.parseHtmlToAnnotatedString(note.content).text.trim()
        val defaultTitle = note.title.ifBlank { "Başlıksız Not" }

        if (cleanContent.isBlank()) {
            return listOf(NoteBlock(title = defaultTitle, body = ""))
        }

        return listOf(NoteBlock(title = defaultTitle, body = cleanContent))
    }

    fun generateNotesPresentation(context: Context, notes: List<Note>): File? {
        return try {
            // Generate a professional presentation PDF file which opens natively on all devices as a presentation report
            PdfGenerator.generateNotesPdf(context, notes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generatePresentationForNote(context: Context, note: Note): File? {
        return generateNotesPresentation(context, listOf(note))
    }
}
