package com.lanu.notdefteri.util

import android.content.Context
import com.lanu.notdefteri.data.model.Note
import org.apache.poi.xslf.usermodel.XMLSlideShow
// import java.awt.Color
// import java.awt.geom.Rectangle2D
import com.lanu.notdefteri.awt.Color
import com.lanu.notdefteri.awt.geom.Rectangle2D
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates PowerPoint (.pptx) presentations using Apache POI XSLF library.
 * Maps note content blocks to slide titles and content bodies dynamically.
 */
object PowerPointGenerator {

    data class NoteBlock(
        val title: String,
        val body: String
    )

    /**
     * Parses note content into structured blocks mapped to slide titles and content bodies.
     */
    fun parseNoteBlocks(note: Note): List<NoteBlock> {
        val cleanContent = RichTextHelper.parseHtmlToAnnotatedString(note.content).text.trim()
        val defaultTitle = note.title.ifBlank { "Başlıksız Not" }

        if (cleanContent.isBlank()) {
            return listOf(NoteBlock(title = defaultTitle, body = ""))
        }

        val blocks = mutableListOf<NoteBlock>()

        // Split content by Markdown headers (#, ##, ###), double newlines, or section headers
        val rawSections = cleanContent.split(Regex("(?m)(?=^#{1,3}\\s+|^\n\\s*\n+|^[A-ZÖÇŞİĞÜ0-9][A-ZÖÇŞİĞÜa-zöçşığü0-9\\s]{2,40}:)"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (rawSections.isNotEmpty()) {
            for (section in rawSections) {
                val lines = section.lines().map { it.trim() }.filter { it.isNotBlank() }
                if (lines.isNotEmpty()) {
                    val firstLine = lines.first()
                    // Check if first line acts as a section header
                    val isHeaderLine = firstLine.startsWith("#") ||
                            firstLine.endsWith(":") ||
                            lines.size > 1 ||
                            firstLine.length <= 60

                    val rawTitle = firstLine
                        .replace(Regex("^#{1,3}\\s*"), "")
                        .replace(Regex("^[*\\-\\d+\\.]+\\s*"), "")
                        .replace(Regex(":$"), "")
                        .trim()

                    val blockTitle = if (rawTitle.isNotBlank() && isHeaderLine) {
                        if (rawTitle.length > 60) rawTitle.take(60) + "..." else rawTitle
                    } else {
                        defaultTitle
                    }

                    val bodyLines = if (isHeaderLine && lines.size > 1) lines.drop(1) else lines
                    val bodyText = bodyLines.joinToString("\n")

                    blocks.add(NoteBlock(title = blockTitle, body = bodyText))
                }
            }
        }

        if (blocks.isEmpty()) {
            blocks.add(NoteBlock(title = defaultTitle, body = cleanContent))
        }

        return blocks
    }

    /*
    /**
     * Generates a PowerPoint presentation for a single note by mapping note blocks to slides.
     */
    fun generatePresentationForNote(context: Context, note: Note): File? {
        return generateNotesPresentation(context, listOf(note))
    }

    /**
     * Generates a PowerPoint presentation for a list of notes using Apache POI XSLF.
     */
    fun generateNotesPresentation(context: Context, notes: List<Note>): File? {
        try {
            val ppt = XMLSlideShow()

            // 1. Cover Slide
            val coverSlide = ppt.createSlide()

            // Main Title
            val coverTitleBox = coverSlide.createTextBox()
            coverTitleBox.anchor = Rectangle2D.Double(60.0, 120.0, 600.0, 120.0)
            val coverPara = coverTitleBox.addNewTextParagraph()
            val coverRun = coverPara.addNewTextRun()
            val coverTitleText = if (notes.size == 1) {
                notes.first().title.ifBlank { "LANU NOT SUNUMU" }
            } else {
                "LANU SESLİ NOT DEFTERİ SUNUMU"
            }
            coverRun.setText(coverTitleText)
            coverRun.setFontSize(32.0)
            coverRun.setBold(true)
            coverRun.setFontColor(Color(216, 59, 1)) // PowerPoint Accent Orange-Red

            // Subtitle
            val coverSubBox = coverSlide.createTextBox()
            coverSubBox.anchor = Rectangle2D.Double(60.0, 260.0, 600.0, 140.0)
            val subPara = coverSubBox.addNewTextParagraph()
            val subRun = subPara.addNewTextRun()
            val sdfDate = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("tr", "TR"))

            val subtitleContent = buildString {
                append("Resmi Sunum Raporu • ").append(sdfDate.format(Date())).append("\n")
                append("Toplam Not Sayısı: ").append(notes.size).append("\n")
                if (notes.size == 1) {
                    val note = notes.first()
                    if (!note.tag.isNullOrBlank()) append("Kategori: ").append(note.tag).append(" • ")
                    if (!note.locationName.isNullOrBlank()) append("Konum: ").append(note.locationName)
                }
            }
            subRun.setText(subtitleContent)
            subRun.setFontSize(16.0)
            subRun.setFontColor(Color(100, 100, 100))

            // 2. Note Slides mapped block-by-block
            val sdfMeta = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
            var slideCounter = 1

            for (note in notes) {
                val blocks = parseNoteBlocks(note)

                for (block in blocks) {
                    slideCounter++
                    val slide = ppt.createSlide()

                    // Slide Title Box (Mapped from Note Block Title)
                    val titleBox = slide.createTextBox()
                    titleBox.anchor = Rectangle2D.Double(40.0, 30.0, 640.0, 55.0)

                    val titlePara = titleBox.addNewTextParagraph()
                    val titleRun = titlePara.addNewTextRun()
                    titleRun.setText(block.title)
                    titleRun.setFontSize(24.0)
                    titleRun.setBold(true)
                    titleRun.setFontColor(Color(216, 59, 1))

                    // Meta Header Box (Note Tag & Timestamp)
                    val metaBox = slide.createTextBox()
                    metaBox.anchor = Rectangle2D.Double(40.0, 85.0, 640.0, 30.0)

                    val metaPara = metaBox.addNewTextParagraph()
                    val metaRun = metaPara.addNewTextRun()
                    val tagStr = if (!note.tag.isNullOrBlank()) " [Kategori: ${note.tag}]" else ""
                    val locStr = if (!note.locationName.isNullOrBlank()) " • 📍 ${note.locationName}" else ""
                    metaRun.setText("Not: ${note.title.ifBlank { "Başlıksız" }}$tagStr • ${sdfMeta.format(Date(note.createdAt))}$locStr")
                    metaRun.setFontSize(12.0)
                    metaRun.setFontColor(Color(120, 120, 120))

                    // Slide Content Body Box (Mapped from Note Block Body)
                    val contentBox = slide.createTextBox()
                    contentBox.anchor = Rectangle2D.Double(40.0, 120.0, 640.0, 370.0)

                    val bodyLines = block.body.lines().map { it.trim() }.filter { it.isNotBlank() }

                    if (bodyLines.isNotEmpty()) {
                        for (line in bodyLines) {
                            val para = contentBox.addNewTextParagraph()

                            val isBullet = line.startsWith("-") || line.startsWith("*") || line.startsWith("•") || line.matches(Regex("^\\d+\\..*"))
                            if (isBullet) {
                                para.setBullet(true)
                            }

                            val cleanLine = line
                                .replace(Regex("^[-*•]\\s*"), "")
                                .replace(Regex("^\\d+\\.\\s*"), "")

                            val run = para.addNewTextRun()
                            run.setText(cleanLine)
                            run.setFontSize(15.0)
                            run.setFontColor(Color(30, 30, 30))
                        }
                    } else {
                        val para = contentBox.addNewTextParagraph()
                        val run = para.addNewTextRun()
                        run.setText("(İçerik detayı bulunmuyor)")
                        run.setFontSize(14.0)
                        run.setFontColor(Color(150, 150, 150))
                    }

                    // Slide Footer
                    val footerBox = slide.createTextBox()
                    footerBox.anchor = Rectangle2D.Double(40.0, 500.0, 640.0, 25.0)

                    val footerPara = footerBox.addNewTextParagraph()
                    val footerRun = footerPara.addNewTextRun()
                    footerRun.setText("LANU Sesli Not Defteri • Slayt $slideCounter")
                    footerRun.setFontSize(10.0)
                    footerRun.setFontColor(Color(160, 160, 160))
                }
            }

            // Write presentation to output file in cache directory
            val fileName = if (notes.size == 1) {
                val cleanTitle = notes.first().title.ifBlank { "isimsiz_not" }
                    .replace(Regex("[\\\\/:*?\"<>|\\s]"), "_")
                "${cleanTitle}_sunum.pptx"
            } else {
                "LANU_Notlar_Sunumu.pptx"
            }

            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            ppt.write(outputStream)

            outputStream.flush()
            outputStream.close()
            ppt.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    */
}

