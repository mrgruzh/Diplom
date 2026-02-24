package com.example.diplom.ui.field

import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class VoiceInputMode {
    WAIT_COMMAND,
    WAIT_VALUE
}

enum class VoiceCommand(
    val label: String,
    val aliases: List<String>
) {
    FILLED_AT(
        label = "Время заполнения",
        aliases = listOf("время заполнения", "заполнение", "дата заполнения")
    ),
    FULL_NAME(
        label = "ФИО",
        aliases = listOf("фио", "ф и о", "фамилия имя отчество")
    ),
    CALLSIGN(
        label = "Позывной",
        aliases = listOf("позывной", "позыв")
    ),
    TAG_NUMBER(
        label = "Номер жетона",
        aliases = listOf("номер жетона", "жетон", "личный номер")
    ),
    EVENT_AT(
        label = "Время события",
        aliases = listOf("время ранения", "время смерти", "дата ранения", "дата смерти", "событие")
    ),
    INJURY_KIND(
        label = "Вид поражения",
        aliases = listOf("вид поражения", "поражение")
    ),
    DIAGNOSIS(
        label = "Диагноз",
        aliases = listOf("диагноз")
    ),
    LOCALIZATION(
        label = "Локализация",
        aliases = listOf("локализация", "место ранения")
    ),
    EVAC_METHOD(
        label = "Способ эвакуации",
        aliases = listOf("способ эвакуации", "эвакуация", "эвак")
    ),
    MEDICINE(
        label = "Препарат и количество",
        aliases = listOf(
            "препарат и количество",
            "препарат количество",
            "количество препарата",
            "препарат",
            "лекарство",
            "медикамент"
        )
    )
}

data class VoiceSessionState(
    val mode: VoiceInputMode = VoiceInputMode.WAIT_COMMAND,
    val activeCommand: VoiceCommand? = null,
    val lastUtterance: String = "",
    val statusText: String = "Скажите название поля",
    val lastApplied: String = ""
)

data class VoiceInterpretResult(
    val draft: FieldFormDraft,
    val session: VoiceSessionState
)

object VoiceFormInterpreter {
    private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    fun commandGrammarJson(): String {
        val words = VoiceCommand.entries
            .flatMap { it.aliases }
            .distinct()
            .toMutableList()
        words.add("стоп")
        words.add("[unk]")
        return JSONArray(words).toString()
    }

    fun applyUtterance(
        status: FieldStatus,
        utteranceRaw: String,
        draft: FieldFormDraft,
        session: VoiceSessionState
    ): VoiceInterpretResult {
        val utterance = normalize(utteranceRaw)
        if (utterance.isBlank()) {
            return VoiceInterpretResult(
                draft = draft,
                session = session.copy(lastUtterance = utteranceRaw)
            )
        }

        return when (session.mode) {
            VoiceInputMode.WAIT_COMMAND -> applyInCommandMode(status, utteranceRaw, utterance, draft, session)
            VoiceInputMode.WAIT_VALUE -> applyInValueMode(status, utteranceRaw, utterance, draft, session)
        }
    }

    private fun applyInCommandMode(
        status: FieldStatus,
        utteranceRaw: String,
        utterance: String,
        draft: FieldFormDraft,
        session: VoiceSessionState
    ): VoiceInterpretResult {
        val detected = detectCommandWithAlias(utterance)
        if (detected == null) {
            return VoiceInterpretResult(
                draft = draft,
                session = session.copy(
                    lastUtterance = utteranceRaw,
                    statusText = "Команда не распознана. Скажите: ФИО / позывной / жетон / диагноз ..."
                )
            )
        }

        val command = detected.first
        val alias = detected.second
        val valuePart = stripFirstAlias(utterance, alias)

        if (valuePart.isNotBlank()) {
            val updated = applyValue(status, draft, command, valuePart)
            return VoiceInterpretResult(
                draft = updated,
                session = VoiceSessionState(
                    mode = VoiceInputMode.WAIT_COMMAND,
                    activeCommand = null,
                    lastUtterance = utteranceRaw,
                    statusText = "Поле ${command.label} заполнено. Скажите следующее поле",
                    lastApplied = command.label
                )
            )
        }

        return VoiceInterpretResult(
            draft = draft,
            session = VoiceSessionState(
                mode = VoiceInputMode.WAIT_VALUE,
                activeCommand = command,
                lastUtterance = utteranceRaw,
                statusText = "Диктуйте значение для поля: ${command.label}",
                lastApplied = command.label
            )
        )
    }

    private fun applyInValueMode(
        status: FieldStatus,
        utteranceRaw: String,
        utterance: String,
        draft: FieldFormDraft,
        session: VoiceSessionState
    ): VoiceInterpretResult {
        val active = session.activeCommand
        if (active == null) {
            return VoiceInterpretResult(
                draft = draft,
                session = session.copy(mode = VoiceInputMode.WAIT_COMMAND, statusText = "Скажите название поля")
            )
        }

        val switched = detectCommandWithAlias(utterance)
        if (switched != null && isOnlyAliasUtterance(utterance, switched.second)) {
            return VoiceInterpretResult(
                draft = draft,
                session = VoiceSessionState(
                    mode = VoiceInputMode.WAIT_VALUE,
                    activeCommand = switched.first,
                    lastUtterance = utteranceRaw,
                    statusText = "Диктуйте значение для поля: ${switched.first.label}",
                    lastApplied = switched.first.label
                )
            )
        }

        if (switched != null && switched.first != active) {
            val switchedValue = stripFirstAlias(utterance, switched.second)
            if (switchedValue.isNotBlank()) {
                val updated = applyValue(status, draft, switched.first, switchedValue)
                return VoiceInterpretResult(
                    draft = updated,
                    session = VoiceSessionState(
                        mode = VoiceInputMode.WAIT_COMMAND,
                        activeCommand = null,
                        lastUtterance = utteranceRaw,
                        statusText = "Поле ${switched.first.label} заполнено. Скажите следующее поле",
                        lastApplied = switched.first.label
                    )
                )
            }
        }

        val valuePart = if (switched != null) stripFirstAlias(utterance, switched.second) else utterance
        if (valuePart.isBlank()) {
            return VoiceInterpretResult(
                draft = draft,
                session = session.copy(
                    lastUtterance = utteranceRaw,
                    statusText = "Не расслышано значение для поля: ${active.label}"
                )
            )
        }

        val updated = applyValue(status, draft, active, valuePart)
        return VoiceInterpretResult(
            draft = updated,
            session = VoiceSessionState(
                mode = VoiceInputMode.WAIT_COMMAND,
                activeCommand = null,
                lastUtterance = utteranceRaw,
                statusText = "Поле ${active.label} заполнено. Скажите следующее поле",
                lastApplied = active.label
            )
        )
    }

    private fun applyValue(
        status: FieldStatus,
        draft: FieldFormDraft,
        command: VoiceCommand,
        valueRaw: String
    ): FieldFormDraft {
        val value = valueRaw.trim()
        return when (command) {
            VoiceCommand.FILLED_AT -> draft.copy(filledAt = normalizeDateLikeValue(value))
            VoiceCommand.FULL_NAME -> draft.copy(fullName = titleCase(value))
            VoiceCommand.CALLSIGN -> draft.copy(callsign = value)
            VoiceCommand.TAG_NUMBER -> draft.copy(tagNumber = value.uppercase(Locale.getDefault()))
            VoiceCommand.EVENT_AT -> draft.copy(eventAt = normalizeDateLikeValue(value))
            VoiceCommand.INJURY_KIND -> if (status == FieldStatus.RANEN) draft.copy(injuryKind = value) else draft
            VoiceCommand.DIAGNOSIS -> if (status == FieldStatus.RANEN) draft.copy(diagnosis = value) else draft
            VoiceCommand.LOCALIZATION -> {
                if (status != FieldStatus.RANEN) return draft
                val parsed = parseLocalization(value)
                draft.copy(
                    localizationSelected = parsed.first,
                    localizationOther = parsed.second
                )
            }
            VoiceCommand.EVAC_METHOD -> draft.copy(evacMethod = normalizeEvacMethod(value))
            VoiceCommand.MEDICINE -> {
                val med = parseMedicine(value)
                draft.copy(medicines = draft.medicines + med)
            }
        }
    }

    private fun parseLocalization(value: String): Pair<Set<String>, String> {
        val n = normalize(value)
        val out = linkedSetOf<String>()

        if (n.contains("голов")) out.add("голова")
        if (n.contains("ше")) out.add("шея")
        if (n.contains("груд")) out.add("грудь")
        if (n.contains("живот") || n.contains("жив")) out.add("живот")
        if (n.contains("таз")) out.add("таз")
        if (n.contains("рук")) out.add("рука")
        if (n.contains("ног")) out.add("нога")
        if (n.contains("множе")) out.add("множественные")

        if (out.isEmpty()) {
            return setOf("другое") to value
        }
        return out to ""
    }

    private fun normalizeEvacMethod(value: String): String {
        val n = normalize(value)
        return when {
            n.contains("самостоятель") -> "самостоятельно"
            n.contains("санитар") -> "санитарный транспорт"
            n.contains("грузов") -> "грузовой транспорт"
            n.contains("вертол") -> "вертолёт"
            else -> "иное"
        }
    }

    private fun parseMedicine(value: String): MedicineItemDraft {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return MedicineItemDraft("-", "-")

        val byKeyword = Regex("^(.+?)\\s+количеств\\S*\\s+(.+)$").find(trimmed)
        if (byKeyword != null) {
            return MedicineItemDraft(
                name = byKeyword.groupValues[1].trim().ifBlank { "-" },
                qty = byKeyword.groupValues[2].trim().ifBlank { "-" }
            )
        }

        val byDash = Regex("^(.+?)\\s*[-—]\\s*(.+)$").find(trimmed)
        if (byDash != null) {
            return MedicineItemDraft(
                name = byDash.groupValues[1].trim().ifBlank { "-" },
                qty = byDash.groupValues[2].trim().ifBlank { "-" }
            )
        }

        val byNum = Regex("^(.+?)\\s+(\\d[\\d\\s.,]*(?:\\s*\\p{L}+.*)?)$").find(trimmed)
        if (byNum != null) {
            return MedicineItemDraft(
                name = byNum.groupValues[1].trim().ifBlank { "-" },
                qty = byNum.groupValues[2].trim().ifBlank { "-" }
            )
        }

        return MedicineItemDraft(name = trimmed, qty = "-")
    }

    private fun normalizeDateLikeValue(value: String): String {
        val n = normalize(value)
        if (n == "сейчас" || n == "текущее время") return dateTimeFormat.format(Date())
        return value
            .replace(" точка ", ".")
            .replace(" двоеточие ", ":")
            .replace(" запятая ", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun titleCase(value: String): String =
        value
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.lowercase(Locale.getDefault()).replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                }
            }

    private fun detectCommandWithAlias(text: String): Pair<VoiceCommand, String>? {
        val normalized = normalize(text)
        val allAliases = VoiceCommand.entries.flatMap { cmd ->
            cmd.aliases.map { alias -> cmd to normalize(alias) }
        }.sortedByDescending { it.second.length }

        for ((cmd, alias) in allAliases) {
            val regex = Regex("(^|\\s)${Regex.escape(alias)}(\\s|$)")
            if (regex.containsMatchIn(normalized)) {
                return cmd to alias
            }
        }
        return null
    }

    private fun stripFirstAlias(text: String, alias: String): String {
        val normalized = normalize(text)
        val regex = Regex("(^|\\s)${Regex.escape(alias)}(\\s|$)")
        return regex.replaceFirst(normalized, " ").replace(Regex("\\s+"), " ").trim()
    }

    private fun isOnlyAliasUtterance(text: String, alias: String): Boolean =
        stripFirstAlias(text, alias).isBlank()

    private fun normalize(text: String): String =
        text
            .lowercase(Locale("ru", "RU"))
            .replace('ё', 'е')
            .replace(Regex("[^a-zа-я0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
