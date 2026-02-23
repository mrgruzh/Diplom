package com.example.diplom.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Form100PdfGenerator {
    fun generateToEvacInbox(ctx: Context, evacPoint: String, prefix: String, data: Form100Data): File {
        val dir = Form100Storage.evacInboxDir(ctx, evacPoint)
        val base = Form100FileNamer.sanitizeFileName(
            Form100FileNamer.withPrefix(prefix, Form100FileNamer.baseName(data))
        )
        return generate(ctx, dir, base, data)
    }

    fun generateToHospitalInbox(
        ctx: Context,
        hospital: String,
        evacPoint: String,
        prefix: String,
        data: Form100Data
    ): File {
        val dir = Form100Storage.hospitalInboxDir(ctx, hospital)
        val base = Form100FileNamer.sanitizeFileName(
            Form100FileNamer.hospitalName(
                Form100FileNamer.withPrefix(prefix, Form100FileNamer.baseName(data)),
                evacPoint
            )
        )
        return generate(ctx, dir, base, data)
    }

    private fun generate(ctx: Context, dir: File, baseFileName: String, data: Form100Data): File {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val out = File(dir, "${baseFileName}_$ts.pdf")

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(1240, 1754, 1).create()
        val page = doc.startPage(pageInfo)

        val canvas = page.canvas
        val margin = 70f
        var y = 120f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ФОРМА 100", pageInfo.pageWidth / 2f, y, titlePaint)
        y += 46f

        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val statusRu = if (data.status == "POGIB") "ПОГИБ" else "РАНЕН"
        canvas.drawText("Медицинская карточка ($statusRu)", pageInfo.pageWidth / 2f, y, subtitlePaint)
        y += 40f

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 3f
        }
        canvas.drawLine(margin, y, pageInfo.pageWidth - margin, y, linePaint)
        y += 40f

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        fun row(label: String, value: String) {
            canvas.drawText(label, margin, y, labelPaint)
            canvas.drawText(dash(value), margin + 380f, y, valuePaint)
            y += 44f
        }

        canvas.drawText("Данные военнослужащего", margin, y, labelPaint)
        y += 48f
        row("ФИО:", data.fullName)
        row("Врач:", data.doctorFio)
        row("Позывной:", data.callsign)
        row("Номер жетона:", data.tagNumber)
        y += 16f

        canvas.drawText("Событие", margin, y, labelPaint)
        y += 48f
        row(if (data.status == "POGIB") "Время смерти:" else "Время ранения:", data.eventAt)
        row("Время заполнения:", data.filledAt)
        y += 16f

        canvas.drawText("Медицинские данные", margin, y, labelPaint)
        y += 48f
        row("Вид поражения:", data.injuryKind)

        canvas.drawText("Диагноз:", margin, y, labelPaint)
        y += 38f
        y = drawWrappedText(canvas, dash(data.diagnosis), margin + 40f, y, pageInfo.pageWidth - margin * 2f - 40f, valuePaint, 34f)
        y += 20f

        canvas.drawText("Локализация:", margin, y, labelPaint)
        y += 38f
        y = drawWrappedText(canvas, dash(data.localization), margin + 40f, y, pageInfo.pageWidth - margin * 2f - 40f, valuePaint, 34f)
        y += 20f

        canvas.drawText("Эвакуация", margin, y, labelPaint)
        y += 48f
        row("Способ эвакуации:", data.evacMethod)
        if (!data.fromEvacPoint.isNullOrBlank()) {
            row("Эвакопункт-отправитель:", data.fromEvacPoint)
        }

        y += 24f
        canvas.drawLine(margin, y, pageInfo.pageWidth - margin, y, linePaint)
        y += 40f

        val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.DKGRAY
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Сгенерировано приложением", margin, pageInfo.pageHeight - 90f, footPaint)

        doc.finishPage(page)
        FileOutputStream(out).use { doc.writeTo(it) }
        doc.close()
        return out
    }

    private fun dash(value: String): String = if (value.isBlank()) "-" else value

    private fun drawWrappedText(
        canvas: android.graphics.Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float
    ): Float {
        val words = text.split(Regex("\\s+"))
        var y = startY
        var line = ""
        for (w in words) {
            val test = if (line.isEmpty()) w else "$line $w"
            if (paint.measureText(test) <= maxWidth) {
                line = test
            } else {
                canvas.drawText(line, x, y, paint)
                y += lineHeight
                line = w
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, x, y, paint)
            y += lineHeight
        }
        return y
    }
}
