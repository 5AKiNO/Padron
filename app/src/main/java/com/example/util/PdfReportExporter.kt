package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.Voter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportExporter {

    fun generateVotersPdf(
        context: Context,
        reportTitle: String,
        subtitle: String,
        primaryColorHex: String,
        voters: List<Voter>
    ): Uri? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 width in points (72 dpi)
        val pageHeight = 842 // A4 height in points
        val margin = 36

        var currentPageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Paints
        val headerPaint = Paint().apply {
            color = parseColorSafely(primaryColorHex, Color.parseColor("#1E3A8A"))
            style = Paint.Style.FILL
        }

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#E0E7FF")
            textSize = 11f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val metaPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 10f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val tableHeaderBgPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }

        val tableHeaderPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 9f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val mutedTextPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 8.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1f
        }

        val votedPaint = Paint().apply {
            color = Color.parseColor("#166534")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val pendingPaint = Paint().apply {
            color = Color.parseColor("#991B1B")
            textSize = 9f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val currentDateStr = dateFormat.format(Date())

        // Header Banner Background
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 90f, headerPaint)
        canvas.drawText(reportTitle.uppercase(), margin.toFloat(), 40f, titlePaint)
        canvas.drawText(subtitle.ifBlank { "Reporte Oficial del Padrón Electoral" }, margin.toFloat(), 62f, subtitlePaint)

        // Metadata section
        val totalVoted = voters.count { it.voted }
        canvas.drawText("Fecha de emisión: $currentDateStr", margin.toFloat(), 110f, metaPaint)
        canvas.drawText("Total filtrados: ${voters.size}  |  Votaron: $totalVoted  |  Pendientes: ${voters.size - totalVoted}", margin.toFloat(), 125f, metaPaint)

        // Table Header
        var yPos = 145f
        val colWidths = floatArrayOf(75f, 160f, 150f, 45f, 40f, 50f)
        val colPositions = FloatArray(colWidths.size)
        var currentX = margin.toFloat()
        for (i in colWidths.indices) {
            colPositions[i] = currentX
            currentX += colWidths[i]
        }

        fun drawTableHeader() {
            canvas.drawRect(margin.toFloat(), yPos, (pageWidth - margin).toFloat(), yPos + 22f, tableHeaderBgPaint)
            canvas.drawText("CÉDULA", colPositions[0] + 4f, yPos + 15f, tableHeaderPaint)
            canvas.drawText("NOMBRE COMPLETO", colPositions[1] + 4f, yPos + 15f, tableHeaderPaint)
            canvas.drawText("LUGAR DE VOTACIÓN", colPositions[2] + 4f, yPos + 15f, tableHeaderPaint)
            canvas.drawText("MESA", colPositions[3] + 4f, yPos + 15f, tableHeaderPaint)
            canvas.drawText("ORDEN", colPositions[4] + 4f, yPos + 15f, tableHeaderPaint)
            canvas.drawText("ESTADO", colPositions[5] + 4f, yPos + 15f, tableHeaderPaint)
            yPos += 22f
            canvas.drawLine(margin.toFloat(), yPos, (pageWidth - margin).toFloat(), yPos, linePaint)
        }

        drawTableHeader()

        // Rows
        for (voter in voters) {
            if (yPos > pageHeight - 50) {
                // Draw footer for previous page
                canvas.drawText("Página $currentPageNum", (pageWidth - margin - 40).toFloat(), (pageHeight - 20).toFloat(), mutedTextPaint)
                pdfDocument.finishPage(page)

                currentPageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                yPos = 40f
                drawTableHeader()
            }

            yPos += 18f

            val cedulaStr = voter.cedula.take(12)
            val nameStr = if (voter.fullName.length > 28) voter.fullName.take(26) + "..." else voter.fullName
            val placeStr = if (voter.votingPlace.length > 25) voter.votingPlace.take(23) + "..." else voter.votingPlace.ifBlank { "-" }
            val tableStr = voter.tableNumber.ifBlank { "-" }
            val orderStr = voter.orderNumber.ifBlank { "-" }

            canvas.drawText(cedulaStr, colPositions[0] + 4f, yPos, textPaint)
            canvas.drawText(nameStr, colPositions[1] + 4f, yPos, textPaint)
            canvas.drawText(placeStr, colPositions[2] + 4f, yPos, textPaint)
            canvas.drawText(tableStr, colPositions[3] + 4f, yPos, textPaint)
            canvas.drawText(orderStr, colPositions[4] + 4f, yPos, textPaint)

            if (voter.voted) {
                canvas.drawText("VOTÓ", colPositions[5] + 4f, yPos, votedPaint)
            } else {
                canvas.drawText("PENDIENTE", colPositions[5] + 4f, yPos, pendingPaint)
            }

            yPos += 4f
            canvas.drawLine(margin.toFloat(), yPos, (pageWidth - margin).toFloat(), yPos, linePaint)
        }

        // Draw page number on last page
        canvas.drawText("Página $currentPageNum", (pageWidth - margin - 40).toFloat(), (pageHeight - 20).toFloat(), mutedTextPaint)
        pdfDocument.finishPage(page)

        return try {
            val file = File(context.cacheDir, "reporte_votantes_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }

    private fun parseColorSafely(hex: String, defaultColor: Int): Int {
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            defaultColor
        }
    }
}
