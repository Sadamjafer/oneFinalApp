package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils

object PdfGenerator {
    fun generatePdf(
        context: Context,
        uri: Uri,
        title: String,
        headers: List<String>,
        data: List<List<String>>,
        summary: List<String> = emptyList()
    ) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val document = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
                var page = document.startPage(pageInfo)
                var canvas = page.canvas

                val titlePaint = TextPaint().apply {
                    color = Color.BLACK
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                
                val headerPaint = TextPaint().apply {
                    color = Color.BLACK
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val textPaint = TextPaint().apply {
                    color = Color.BLACK
                    textSize = 11f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }

                val summaryPaint = TextPaint().apply {
                    color = Color.BLACK
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val linePaint = Paint().apply {
                    color = Color.LTGRAY
                    strokeWidth = 1f
                }

                val margin = 40f
                var yPos = 60f

                // Draw title using StaticLayout for perfect Arabic shaping/bidi rendering
                fun drawTitle(text: String) {
                    val width = pageInfo.pageWidth - 2 * margin
                    val builder = StaticLayout.Builder.obtain(text, 0, text.length, titlePaint, width.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .setLineSpacing(0f, 1f)
                        .setIncludePad(false)
                        .setMaxLines(1)
                    val staticLayout = builder.build()
                    
                    canvas.save()
                    canvas.translate(margin, yPos)
                    staticLayout.draw(canvas)
                    canvas.restore()
                }

                drawTitle(title)
                yPos += 50f

                val columnWidth = (pageInfo.pageWidth - 2 * margin) / headers.size.coerceAtLeast(1)

                fun drawCell(text: String, xCenter: Float, width: Float, paint: TextPaint) {
                    val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_CENTER)
                        .setLineSpacing(0f, 1f)
                        .setIncludePad(false)
                        .setMaxLines(2)
                        .setEllipsize(TextUtils.TruncateAt.END)
                    val staticLayout = builder.build()
                    
                    canvas.save()
                    canvas.translate(xCenter - width / 2f, yPos - staticLayout.height / 2f)
                    staticLayout.draw(canvas)
                    canvas.restore()
                }

                fun drawHeaders() {
                    var xPos = pageInfo.pageWidth - margin
                    for (header in headers) {
                        drawCell(header, xPos - columnWidth / 2, columnWidth, headerPaint)
                        xPos -= columnWidth
                    }
                    yPos += 20f
                    canvas.drawLine(margin, yPos, pageInfo.pageWidth - margin, yPos, linePaint)
                    yPos += 20f
                }

                if (headers.isNotEmpty()) {
                    drawHeaders()
                }

                for (row in data) {
                    if (yPos > pageInfo.pageHeight - margin - 50) {
                        document.finishPage(page)
                        page = document.startPage(pageInfo)
                        canvas = page.canvas
                        yPos = margin + 20f
                        if (headers.isNotEmpty()) drawHeaders()
                    }

                    var xPos = pageInfo.pageWidth - margin
                    for (i in row.indices) {
                        val cell = row[i]
                        drawCell(cell, xPos - columnWidth / 2, columnWidth, textPaint)
                        xPos -= columnWidth
                    }
                    yPos += 25f
                }

                yPos += 20f
                canvas.drawLine(margin, yPos - 15f, pageInfo.pageWidth - margin, yPos - 15f, linePaint)
                
                fun drawSummaryLine(text: String) {
                    val width = pageInfo.pageWidth - 2 * margin
                    val builder = StaticLayout.Builder.obtain(text, 0, text.length, summaryPaint, width.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL) // Will align right for Arabic text naturally
                        .setLineSpacing(0f, 1f)
                        .setIncludePad(false)
                        .setMaxLines(1)
                    val staticLayout = builder.build()
                    
                    canvas.save()
                    canvas.translate(margin, yPos - staticLayout.height / 2f)
                    staticLayout.draw(canvas)
                    canvas.restore()
                }

                for (line in summary) {
                    if (yPos > pageInfo.pageHeight - margin) {
                        document.finishPage(page)
                        page = document.startPage(pageInfo)
                        canvas = page.canvas
                        yPos = margin + 20f
                    }
                    drawSummaryLine(line)
                    yPos += 25f
                }

                document.finishPage(page)
                document.writeTo(outputStream)
                document.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
