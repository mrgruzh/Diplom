package com.example.diplom.pdf

object Form100FileNamer {
    fun withPrefix(prefix: String, base: String): String {
        val p = prefix.trim().ifBlank { "" }
        return if (p.isEmpty()) base else "${p}_$base"
    }

    fun baseName(data: Form100Data): String {
        val surname = data.fullName
            .trim()
            .split(Regex("\\s+"))
            .firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.takeIf { it != "Неизвестный" }

        val tag = data.tagNumber.trim().takeIf { it.isNotBlank() }
        val call = data.callsign.trim().takeIf { it.isNotBlank() }

        return surname ?: tag ?: call ?: "-"
    }

    fun hospitalName(base: String, fromEvacPoint: String): String =
        "$base+$fromEvacPoint"

    fun sanitizeFileName(value: String): String {
        val trimmed = value.trim().ifBlank { "-" }
        val replaced = trimmed
            .replace("/", "_")
            .replace("\\\\", "_")
            .replace(":", "_")
            .replace("*", "_")
            .replace("?", "_")
            .replace("\"", "_")
            .replace("<", "_")
            .replace(">", "_")
            .replace("|", "_")
        return replaced.take(80).ifBlank { "-" }
    }
}
