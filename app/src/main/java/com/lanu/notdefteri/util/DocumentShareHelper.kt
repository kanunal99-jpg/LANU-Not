package com.lanu.notdefteri.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.lanu.notdefteri.data.model.Note
import java.io.File

object DocumentShareHelper {

    fun getFileForNoteAndExtension(context: Context, note: Note, extension: String): File? {
        return try {
            when (extension.lowercase()) {
                ".pdf" -> PdfGenerator.generateNotePdf(context, note)
                ".xlsx" -> ExcelGenerator.generateNotesExcel(context, listOf(note))
                // ".pptx" -> PowerPointGenerator.generateNotesPresentation(context, listOf(note))
                ".txt" -> {
                    val cleanTitle = note.title.ifBlank { "isimsiz_not" }
                        .replace("[\\\\/:*?\"<>|\\s]".toRegex(), "_")
                    val file = File(context.cacheDir, "${cleanTitle}.txt")
                    file.writeText("${note.title}\n\n${note.content}")
                    file
                }
                ".json" -> {
                    val cleanTitle = note.title.ifBlank { "isimsiz_not" }
                        .replace("[\\\\/:*?\"<>|\\s]".toRegex(), "_")
                    val file = File(context.cacheDir, "${cleanTitle}.json")
                    val jsonString = """
                        {
                          "title": "${note.title.replace("\"", "\\\"")}",
                          "content": "${note.content.replace("\"", "\\\"").replace("\n", "\\n")}",
                          "createdAt": ${note.createdAt},
                          "tag": "${note.tag ?: ""}"
                        }
                    """.trimIndent()
                    file.writeText(jsonString)
                    file
                }
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareViaEmail(context: Context, note: Note, extension: String) {
        try {
            val file = getFileForNoteAndExtension(context, note, extension) ?: return
            if (!file.exists()) return

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "com.lanu.notdefteri.fileprovider",
                file
            )

            // Using ACTION_SEND with message/rfc822 and selector strictly filters to Email apps
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_SUBJECT, "LANU Not Defteri - Paylaşılan Belge: ${note.title}")
                putExtra(Intent.EXTRA_TEXT, "Merhaba,\n\nLANU Sesli Not Defteri uygulamasından ekteki '${note.title}' başlıklı belge (${extension.uppercase()}) sizinle paylaşıldı.\n\nİyi günler dileriz.")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val selectorIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
            }
            emailIntent.selector = selectorIntent

            context.startActivity(Intent.createChooser(emailIntent, "E-posta ile Paylaş"))
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to standard share
            shareGeneric(context, note, extension)
        }
    }

    fun shareViaMessaging(context: Context, note: Note, extension: String) {
        try {
            val file = getFileForNoteAndExtension(context, note, extension) ?: return
            if (!file.exists()) return

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "com.lanu.notdefteri.fileprovider",
                file
            )

            val mimeType = when (extension.lowercase()) {
                ".pdf" -> "application/pdf"
                ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                // ".pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                ".txt" -> "text/plain"
                ".json" -> "application/json"
                else -> "*/*"
            }

            val msgIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "LANU Not Defteri ile paylaşıldı: ${note.title}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(msgIntent, "Mesajlaşma ile Paylaş"))
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to standard share
            shareGeneric(context, note, extension)
        }
    }

    fun shareGeneric(context: Context, note: Note, extension: String) {
        when (extension.lowercase()) {
            ".pdf" -> shareAsPdfFile(context, note)
            ".xlsx" -> shareExcelFile(context, listOf(note))
            // ".pptx" -> sharePowerPointFile(context, note)
            ".txt" -> shareAsTextFile(context, note)
            ".json" -> shareAsJsonFile(context, note)
        }
    }

    fun shareAsPlainText(context: Context, note: Note) {
        val shareBody = "${note.title}\n\n${note.content}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, note.title)
            putExtra(Intent.EXTRA_TEXT, shareBody)
        }
        context.startActivity(Intent.createChooser(intent, "Notu Paylaş"))
    }

    fun shareAsTextFile(context: Context, note: Note) {
        try {
            // Clean filename from illegal characters
            val cleanTitle = note.title.ifBlank { "isimsiz_not" }
                .replace("[\\\\/:*?\"<>|\\s]".toRegex(), "_")
            val fileName = "${cleanTitle}.txt"
            
            val file = File(context.cacheDir, fileName)
            file.writeText("${note.title}\n\n${note.content}")

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "com.lanu.notdefteri.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Metin Belgesini Paylaş"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareAsJsonFile(context: Context, note: Note) {
        try {
            val cleanTitle = note.title.ifBlank { "isimsiz_not" }
                .replace("[\\\\/:*?\"<>|\\s]".toRegex(), "_")
            val fileName = "${cleanTitle}.json"
            
            val file = File(context.cacheDir, fileName)
            val jsonString = """
                {
                  "title": "${note.title.replace("\"", "\\\"")}",
                  "content": "${note.content.replace("\"", "\\\"").replace("\n", "\\n")}",
                  "createdAt": ${note.createdAt},
                  "tag": "${note.tag ?: ""}"
                }
            """.trimIndent()
            file.writeText(jsonString)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "com.lanu.notdefteri.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "JSON Belgesini Paylaş"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareAsPdfFile(context: Context, note: Note) {
        try {
            val file = PdfGenerator.generateNotePdf(context, note)
            if (file != null && file.exists()) {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "com.lanu.notdefteri.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "PDF Belgesini Paylaş"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sharePdfFile(context: Context, notes: List<Note>) {
        try {
            val file = PdfGenerator.generateNotesPdf(context, notes)
            if (file != null && file.exists()) {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "com.lanu.notdefteri.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "LANU Not Defteri — Not Arşivi (${notes.size} Not)")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Toplu PDF Arşivini Paylaş"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareExcelFile(context: Context, notes: List<Note>) {
        try {
            val file = ExcelGenerator.generateNotesExcel(context, notes)
            if (file != null && file.exists()) {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "com.lanu.notdefteri.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Excel Raporunu Paylaş"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*
    fun sharePowerPointFile(context: Context, note: Note) {
        sharePowerPointFile(context, listOf(note))
    }

    fun sharePowerPointFile(context: Context, notes: List<Note>) {
        try {
            val file = PowerPointGenerator.generateNotesPresentation(context, notes)
            if (file != null && file.exists()) {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "com.lanu.notdefteri.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Sunum Dosyasını Paylaş"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    */
}
