package com.lanu.notdefteri.util

import android.content.Context
import com.lanu.notdefteri.data.model.Note
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelGenerator {

    fun generateNotesExcel(context: Context, notes: List<Note>): File? {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Notlar Raporu")

            // 1. Title Styles
            val titleFont = workbook.createFont().apply {
                fontName = "Arial"
                fontHeightInPoints = 14.toShort()
                bold = true
                color = IndexedColors.DARK_RED.index
            }
            val titleStyle = workbook.createCellStyle().apply {
                setFont(titleFont)
            }

            // 2. Table Headers Styles
            val headerFont = workbook.createFont().apply {
                fontName = "Arial"
                fontHeightInPoints = 11.toShort()
                bold = true
                color = IndexedColors.WHITE.index
            }
            val headerStyle = workbook.createCellStyle().apply {
                setFont(headerFont)
                fillForegroundColor = IndexedColors.CORAL.index
                fillPattern = FillPatternType.SOLID_FOREGROUND
                alignment = HorizontalAlignment.CENTER
            }

            // 3. Body Text Styles
            val bodyFont = workbook.createFont().apply {
                fontName = "Arial"
                fontHeightInPoints = 10.toShort()
            }
            val bodyStyle = workbook.createCellStyle().apply {
                setFont(bodyFont)
                wrapText = true
            }

            // Write Spreadsheet Title Row
            val titleRow = sheet.createRow(0)
            val titleCell = titleRow.createCell(0)
            titleCell.setCellValue("LANU NOT DEFTERI — RESMİ VERİ TABANI")
            titleCell.cellStyle = titleStyle

            // Write metadata Row
            val metaRow = sheet.createRow(1)
            val metaCell = metaRow.createCell(0)
            val sdfMeta = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("tr", "TR"))
            metaCell.setCellValue("Oluşturulma Raporu: ${sdfMeta.format(Date())} • Toplam Not Sayısı: ${notes.size}")

            // Set Header Row (at index 3)
            val headers = listOf("ID", "BAŞLIK", "KATEGORİ", "NOT İÇERİĞİ", "OLUŞTURULMA TARİHİ", "SABİTLENDİ")
            val headerRow = sheet.createRow(3)
            for (i in headers.indices) {
                val cell = headerRow.createCell(i)
                cell.setCellValue(headers[i])
                cell.cellStyle = headerStyle
            }

            // Populate rows
            val sdfDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR"))
            var rowIndex = 4
            for (note in notes) {
                val row = sheet.createRow(rowIndex++)
                
                // Col 0: ID
                val c0 = row.createCell(0)
                c0.setCellValue(note.id.toDouble())
                c0.cellStyle = bodyStyle

                // Col 1: Title
                val c1 = row.createCell(1)
                c1.setCellValue(note.title.ifBlank { "Başlıksız Not" })
                c1.cellStyle = bodyStyle

                // Col 2: Tag
                val c2 = row.createCell(2)
                c2.setCellValue(note.tag ?: "-")
                c2.cellStyle = bodyStyle

                // Col 3: Content (with parsed HTML rich-text stripped for clean raw text spreadsheet readability)
                val c3 = row.createCell(3)
                val cleanContent = note.content.replace("<[^>]*>".toRegex(), "")
                c3.setCellValue(cleanContent)
                c3.cellStyle = bodyStyle

                // Col 4: CreatedAt
                val c4 = row.createCell(4)
                c4.setCellValue(sdfDate.format(Date(note.createdAt)))
                c4.cellStyle = bodyStyle

                // Col 5: IsPinned
                val c5 = row.createCell(5)
                c5.setCellValue(if (note.isPinned) "Evet" else "Hayır")
                c5.cellStyle = bodyStyle
            }

            // Configure columns width
            sheet.setColumnWidth(0, 2000)  // ID
            sheet.setColumnWidth(1, 5000)  // Title
            sheet.setColumnWidth(2, 4000)  // Tag
            sheet.setColumnWidth(3, 12000) // Content
            sheet.setColumnWidth(4, 5500)  // Date
            sheet.setColumnWidth(5, 3000)  // Pinned

            // Write Workbook stream to cache file
            val file = File(context.cacheDir, "LANU_Notlar_Raporu.xlsx")
            val outputStream = FileOutputStream(file)
            workbook.write(outputStream)

            outputStream.flush()
            outputStream.close()
            workbook.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
