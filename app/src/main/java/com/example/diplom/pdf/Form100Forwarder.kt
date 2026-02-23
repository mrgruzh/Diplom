package com.example.diplom.pdf

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Form100Forwarder {
    fun forwardEvacPdfToHospital(ctx: Context, sourcePdf: File, evacPoint: String, hospital: String): File {
        val soldierBase = extractBaseName(sourcePdf)
        val safeBase = Form100FileNamer.sanitizeFileName(Form100FileNamer.hospitalName(soldierBase, evacPoint.ifBlank { "-" }))

        val dir = Form100Storage.hospitalInboxDir(ctx, hospital)
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val out = File(dir, "${safeBase}_$ts.pdf")
        sourcePdf.copyTo(out, overwrite = false)
        return out
    }

    private fun extractBaseName(file: File): String {
        val name = file.nameWithoutExtension
        val suffix = Regex("_\\d{8}_\\d{6}$")
        return name.replace(suffix, "")
    }
}
