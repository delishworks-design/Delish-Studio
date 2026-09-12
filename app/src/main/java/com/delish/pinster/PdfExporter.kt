package com.delish.pinster

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    data class ExportResult(
        val success: Boolean,
        val filePath: String? = null,
        val error: String? = null
    )

    fun export(
        context: Context,
        content: String,
        fileName: String,
        title: String = ""
    ): ExportResult {
        return try {
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()

            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val bodyPaint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
                isAntiAlias = true
            }

            val headerPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 13f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val page_numPaint = Paint().apply {
                color = Color.GRAY
                textSize = 9f
                isAntiAlias = true
            }

            val lineHeight = 16f
            val margin_x = 50f
            val margin_top = 60f
            val margin_bottom = 60f
            val page_width = 595f
            val page_height = 842f
            val content_width = page_width - 2 * margin_x

            val lines = content.split("\n")
            var pageNumber = 1
            var currentPage: PdfDocument.Page? = null
            var canvas: Canvas? = null
            var y = margin_top

            fun startNewPage(): PdfDocument.Page {
                currentPage?.let { doc.finishPage(it) }
                val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                canvas = page.canvas
                y = margin_top
                currentPage = page
                return page
            }

            fun drawPageNumber() {
                canvas?.drawText(
                    "$pageNumber",
                    page_width / 2 - 10,
                    page_height - 30,
                    page_numPaint
                )
            }

            startNewPage()

            if (title.isNotBlank()) {
                val titleLines = wrapText(title, titlePaint, content_width)
                for (line in titleLines) {
                    if (y > page_height - margin_bottom) {
                        drawPageNumber()
                        pageNumber++
                        startNewPage()
                    }
                    canvas?.drawText(line, margin_x, y, titlePaint)
                    y += lineHeight + 6f
                }
                y += 12f
            }

            for (line in lines) {
                val isHeader = line.startsWith("# ") || line.startsWith("## ") || line.startsWith("### ")
                val isBold = line.startsWith("**") && line.endsWith("**")
                val paint = when {
                    isHeader -> headerPaint
                    isBold -> headerPaint
                    else -> bodyPaint
                }
                val textSize = when {
                    isHeader -> 15f
                    isBold -> 12f
                    else -> 11f
                }
                paint.textSize = textSize

                val displayLine = line
                    .replace(Regex("^#{1,3}\\s*"), "")
                    .replace(Regex("\\*\\*"), "")

                val wrappedLines = wrapText(displayLine, paint, content_width)

                for (wl in wrappedLines) {
                    if (y > page_height - margin_bottom) {
                        drawPageNumber()
                        pageNumber++
                        startNewPage()
                    }
                    canvas?.drawText(wl, margin_x, y, paint)
                    y += lineHeight
                }

                if (line.isBlank()) {
                    y += lineHeight / 2
                }
            }

            drawPageNumber()
            currentPage?.let { doc.finishPage(it) }

            val outputDir = File(context.filesDir, "exports")
            outputDir.mkdirs()

            val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val outputFile = File(outputDir, "${safeFileName}.pdf")

            FileOutputStream(outputFile).use { out ->
                doc.writeTo(out)
            }

            doc.close()

            ExportResult(success = true, filePath = outputFile.absolutePath)
        } catch (e: Exception) {
            ExportResult(success = false, error = e.message)
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return listOf("")

        val words = text.split(" ")
        val result = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)

            if (width > maxWidth && currentLine.isNotEmpty()) {
                result.add(currentLine.toString())
                currentLine = StringBuilder(word)
            } else {
                currentLine = StringBuilder(testLine)
            }
        }

        if (currentLine.isNotEmpty()) {
            result.add(currentLine.toString())
        }

        return if (result.isEmpty()) listOf("") else result
    }

    fun getExportedFiles(context: Context): List<File> {
        val dir = File(context.filesDir, "exports")
        if (!dir.exists()) return emptyList()
        return dir.listFiles()?.filter { it.extension == "pdf" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}
