package com.example.diplom.pdf

import android.content.Context
import java.io.File

object Form100Storage {
    private fun rootDir(ctx: Context): File {
        val external = ctx.getExternalFilesDir("forms100")
        return external ?: File(ctx.filesDir, "forms100")
    }

    fun evacInboxDir(ctx: Context, evacPoint: String): File {
        val dir = File(rootDir(ctx), "inbox/evac/${Form100FileNamer.sanitizeFileName(evacPoint)}")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun hospitalInboxDir(ctx: Context, hospital: String): File {
        val dir = File(rootDir(ctx), "inbox/hospital/${Form100FileNamer.sanitizeFileName(hospital)}")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
