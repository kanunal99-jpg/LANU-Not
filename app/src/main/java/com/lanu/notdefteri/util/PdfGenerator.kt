package com.lanu.notdefteri.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import com.lanu.notdefteri.data.model.Note
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Android PdfDocument API kullanarak notları profesyonel A4 şablonuyla
 * PDF belgesine dönüştüren ve paylaşım kütüphanesine sunan jeneratör.
 */
object PdfGenerator {

    fun generateNotePdf(context: Context, note: Note): File? {
        try {
            val pdfDocument = PdfDocument()
            
            // Standard A4 dimensions in points (595 x 842 points)
            val pageWidth = 595
            val pageHeight = 842
            val margin = 45f
            val printableWidth = pageWidth - (margin * 2)
            val maxY = pageHeight - 65f

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            val textPaint = TextPaint().apply {
                isAntiAlias = true
                textSize = 12f
                color = Color.BLACK
            }

            var currentY = 50f

            fun drawPageHeader(canvas: Canvas) {
                // Brand Header Bar
                paint.color = Color.parseColor("#C25D23") // Terracotta Accent
                canvas.drawRect(margin, 40f, pageWidth - margin, 44f, paint)

                textPaint.apply {
                    textSize = 9f
                    color = Color.parseColor("#71717A")
                    isFakeBoldText = true
                }
                canvas.drawText("LANU NOT DEFTERİ — RESMİ IHRACAT BELGESİ", margin, 32f, textPaint)
            }

            fun drawPageFooter(canvas: Canvas, currentPage: Int) {
                paint.color = Color.parseColor("#E4E4E7")
                canvas.drawLine(margin, 790f, pageWidth - margin, 790f, paint)

                textPaint.apply {
                    textSize = 8.5f
                    color = Color.parseColor("#A1A1AA")
                    isFakeBoldText = false
                }
                canvas.drawText("Bu belge LANU Sesli Not Defteri ile profesyonel olarak oluşturulmuştur.", margin, 805f, textPaint)

                val pageStr = "Sayfa $currentPage"
                val pageStrWidth = textPaint.measureText(pageStr)
                canvas.drawText(pageStr, pageWidth - margin - pageStrWidth, 805f, textPaint)
            }

            // Draw header on Page 1
            drawPageHeader(canvas)
            currentY = 65f

            // 1. Title
            textPaint.apply {
                textSize = 22f
                color = Color.parseColor("#C25D23")
                isFakeBoldText = true
            }
            val titleText = note.title.ifBlank { "Başlıksız Not" }
            currentY = drawWrappedText(canvas, titleText, margin, currentY, textPaint, printableWidth)
            currentY += 14f

            // 2. Metadata (Creation Date & Tag Badge)
            textPaint.apply {
                textSize = 10f
                color = Color.parseColor("#52525B")
                isFakeBoldText = false
            }

            val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("tr", "TR"))
            val formattedDate = sdf.format(Date(note.createdAt))
            canvas.drawText("Oluşturulma Tarihi: $formattedDate", margin, currentY, textPaint)

            note.tag?.let { tag ->
                val tagStr = "Kategori: $tag"
                val tagWidth = textPaint.measureText(tagStr)
                canvas.drawText(tagStr, pageWidth - margin - tagWidth, currentY, textPaint)
            }
            currentY += 16f

            // Divider Line
            paint.color = Color.parseColor("#E4E4E7")
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint)
            currentY += 24f

            // 3. Optional Note Attachment Image
            note.imagePath?.let { imagePath ->
                if (imagePath.isNotBlank()) {
                    val imgFile = File(imagePath)
                    if (imgFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                        if (bitmap != null) {
                            val targetHeight = 160f
                            val targetWidth = minOf(printableWidth, (bitmap.width.toFloat() / bitmap.height.toFloat()) * targetHeight)
                            
                            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                            val destRect = android.graphics.RectF(margin, currentY, margin + targetWidth, currentY + targetHeight)
                            
                            canvas.drawBitmap(bitmap, srcRect, destRect, paint)
                            currentY += targetHeight + 20f
                        }
                    }
                }
            }

            // Optional Drawing Sketch Attachment
            note.drawingPath?.let { drawPath ->
                if (drawPath.isNotBlank()) {
                    val drawFile = File(drawPath)
                    if (drawFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(drawFile.absolutePath)
                        if (bitmap != null) {
                            val targetHeight = 160f
                            val targetWidth = minOf(printableWidth, (bitmap.width.toFloat() / bitmap.height.toFloat()) * targetHeight)
                            
                            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                            val destRect = android.graphics.RectF(margin, currentY, margin + targetWidth, currentY + targetHeight)
                            
                            canvas.drawBitmap(bitmap, srcRect, destRect, paint)
                            currentY += targetHeight + 20f
                        }
                    }
                }
            }

            // 4. Note Body Content (With automatic page break handling)
            textPaint.apply {
                textSize = 12f
                color = Color.parseColor("#18181B")
                isFakeBoldText = false
            }

            // Clean HTML tags if present for plain text rendering
            val cleanContent = RichTextHelper.parseHtmlToAnnotatedString(note.content).text
            val lines = cleanContent.split("\n")

            for (line in lines) {
                // Check if page overflow occurs
                if (currentY > maxY) {
                    drawPageFooter(canvas, pageNumber)
                    pdfDocument.finishPage(page)

                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    drawPageHeader(canvas)
                    currentY = 65f
                }

                if (line.isBlank()) {
                    currentY += 12f
                } else {
                    currentY = drawWrappedText(canvas, line, margin, currentY, textPaint, printableWidth)
                    currentY += 6f
                }
            }

            // Finish final page
            drawPageFooter(canvas, pageNumber)
            pdfDocument.finishPage(page)

            // Save PDF to cache directory
            val cleanTitle = note.title.ifBlank { "isimsiz_not" }
                .replace("[\\\\/:*?\"<>|\\s]".toRegex(), "_")
            val pdfFile = File(context.cacheDir, "$cleanTitle.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)

            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Tüm notları profesyonel bir derleme PDF dokümanına dönüştürür.
     */
    fun generateNotesPdf(context: Context, notes: List<Note>): File? {
        if (notes.isEmpty()) return null
        if (notes.size == 1) return generateNotePdf(context, notes[0])

        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 45f
            val printableWidth = pageWidth - (margin * 2)
            val maxY = pageHeight - 65f

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            val textPaint = TextPaint().apply {
                isAntiAlias = true
                textSize = 12f
                color = Color.BLACK
            }

            fun drawHeader(canvas: Canvas) {
                paint.color = Color.parseColor("#C25D23")
                canvas.drawRect(margin, 40f, pageWidth - margin, 44f, paint)
                textPaint.apply {
                    textSize = 9f
                    color = Color.parseColor("#71717A")
                    isFakeBoldText = true
                }
                canvas.drawText("LANU NOT DEFTERİ — TOPLU NOT ARŞİVİ (${notes.size} NOT)", margin, 32f, textPaint)
            }

            fun drawFooter(canvas: Canvas, pNum: Int) {
                paint.color = Color.parseColor("#E4E4E7")
                canvas.drawLine(margin, 790f, pageWidth - margin, 790f, paint)
                textPaint.apply {
                    textSize = 8.5f
                    color = Color.parseColor("#A1A1AA")
                    isFakeBoldText = false
                }
                canvas.drawText("LANU Sesli Not Defteri ile derlenmiştir.", margin, 805f, textPaint)
                val pageStr = "Sayfa $pNum"
                canvas.drawText(pageStr, pageWidth - margin - textPaint.measureText(pageStr), 805f, textPaint)
            }

            drawHeader(canvas)
            var currentY = 70f
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))

            for ((index, note) in notes.withIndex()) {
                if (currentY + 100f > maxY) {
                    drawFooter(canvas, pageNumber)
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    drawHeader(canvas)
                    currentY = 70f
                }

                // Note item title
                textPaint.apply {
                    textSize = 15f
                    color = Color.parseColor("#18181B")
                    isFakeBoldText = true
                }
                val noteTitle = "${index + 1}. " + note.title.ifBlank { "Başlıksız Not" }
                currentY = drawWrappedText(canvas, noteTitle, margin, currentY, textPaint, printableWidth)
                currentY += 8f

                // Date and Tag
                textPaint.apply {
                    textSize = 9.5f
                    color = Color.parseColor("#71717A")
                    isFakeBoldText = false
                }
                val meta = "${sdf.format(Date(note.createdAt))}${if (!note.tag.isNullOrBlank()) "  |  #${note.tag}" else ""}"
                canvas.drawText(meta, margin, currentY, textPaint)
                currentY += 16f

                // Content
                textPaint.apply {
                    textSize = 11f
                    color = Color.parseColor("#27272A")
                    isFakeBoldText = false
                }
                val cleanContent = note.content
                    .replace("<[^>]*>".toRegex(), "")
                    .replace("&nbsp;", " ")
                    .trim()

                if (cleanContent.isNotBlank()) {
                    val paragraphs = cleanContent.split("\n")
                    for (para in paragraphs) {
                        if (currentY + 25f > maxY) {
                            drawFooter(canvas, pageNumber)
                            pdfDocument.finishPage(page)
                            pageNumber++
                            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page.canvas
                            drawHeader(canvas)
                            currentY = 70f
                        }
                        if (para.isBlank()) {
                            currentY += 10f
                        } else {
                            currentY = drawWrappedText(canvas, para, margin, currentY, textPaint, printableWidth)
                            currentY += 8f
                        }
                    }
                }

                // Divider line between notes
                currentY += 10f
                paint.color = Color.parseColor("#E4E4E7")
                canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint)
                currentY += 20f
            }

            drawFooter(canvas, pageNumber)
            pdfDocument.finishPage(page)

            val pdfFile = File(context.cacheDir, "LANU_Tum_Notlar_Arsivi.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        yStart: Float,
        paint: TextPaint,
        maxWidth: Float
    ): Float {
        var y = yStart
        val words = text.split("\\s+".toRegex())
        var line = StringBuilder()
        val lineHeight = paint.descent() - paint.ascent() + 6f

        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            val measure = paint.measureText(testLine)
            if (measure > maxWidth) {
                canvas.drawText(line.toString(), x, y, paint)
                y += lineHeight
                line = StringBuilder(word)
            } else {
                line.append(if (line.isEmpty()) "" else " ").append(word)
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), x, y, paint)
            y += lineHeight
        }
        return y
    }
}
