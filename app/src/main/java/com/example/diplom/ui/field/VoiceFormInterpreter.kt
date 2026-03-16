package com.example.diplom.ui.field

import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
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
        aliases = listOf(
            "время заполнения",
            "дата заполнения",
            "время и дата заполнения",
            "дата и время заполнения",
            "заполнение",
            "время оформления",
            "дата оформления",
            "время оформления карточки",
            "дата оформления карточки",
            "время заполнения формы",
            "дата заполнения формы",
            "время записи",
            "дата записи",
            "когда заполнили",
            "когда заполнено",
            "время составления",
            "дата составления"
        )
    ),
    FULL_NAME(
        label = "ФИО",
        aliases = listOf(
            "фио",
            "ф и о",
            "фамилия имя отчество",
            "полное имя",
            "полное имя бойца",
            "фамилия и имя",
            "фамилия имя",
            "имя и фамилия",
            "имя бойца",
            "как зовут",
            "данные бойца"
        )
    ),
    CALLSIGN(
        label = "Позывной",
        aliases = listOf(
            "позывной",
            "позыв",
            "радиопозывной",
            "кодовое имя",
            "имя по рации",
            "позывной бойца"
        )
    ),
    TAG_NUMBER(
        label = "Номер жетона",
        aliases = listOf(
            "номер жетона",
            "жетон",
            "личный номер",
            "личный номер бойца",
            "номер бойца",
            "ид номер",
            "айди номер",
            "айди",
            "номер айди",
            "идентификатор",
            "идентификационный номер",
            "номер военнослужащего",
            "номер бирки",
            "бирка",
            "личка",
            "номер карточки"
        )
    ),
    EVENT_AT(
        label = "Время события",
        aliases = listOf(
            "время ранения",
            "дата ранения",
            "время и дата ранения",
            "дата и время ранения",
            "когда ранен",
            "когда получил ранение",
            "время травмы",
            "дата травмы",
            "время поражения",
            "дата поражения",
            "время смерти",
            "дата смерти",
            "время и дата смерти",
            "дата и время смерти",
            "когда погиб",
            "когда умер",
            "время гибели",
            "дата гибели",
            "событие",
            "время события",
            "дата события"
        )
    ),
    INJURY_KIND(
        label = "Вид поражения",
        aliases = listOf(
            "вид поражения",
            "поражение",
            "тип поражения",
            "характер поражения",
            "вид травмы",
            "тип травмы",
            "характер травмы",
            "что за ранение",
            "какое ранение"
        )
    ),
    DIAGNOSIS(
        label = "Диагноз",
        aliases = listOf(
            "диагноз",
            "предварительный диагноз",
            "клинический диагноз",
            "заключение",
            "состояние",
            "описание состояния",
            "медицинское заключение"
        )
    ),
    LOCALIZATION(
        label = "Локализация",
        aliases = listOf(
            "локализация",
            "место ранения",
            "место поражения",
            "область поражения",
            "где ранен",
            "где повреждение",
            "зона поражения",
            "локализация ранения"
        )
    ),
    EVAC_METHOD(
        label = "Способ эвакуации",
        aliases = listOf(
            "способ эвакуации",
            "эвакуация",
            "эвак",
            "метод эвакуации",
            "как эвакуировали",
            "чем эвакуировали",
            "способ доставки",
            "вид эвакуации",
            "тип транспорта",
            "транспорт эвакуации"
        )
    ),
    MEDICINE(
        label = "Препарат и количество",
        aliases = listOf(
            "препарат и количество",
            "препарат количество",
            "количество препарата",
            "препарат",
            "лекарство",
            "медикамент",
            "лекарства",
            "препараты",
            "что ввели",
            "что дали",
            "доза",
            "дозировка",
            "введенное лекарство"
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

private data class DetectedCommand(
    val command: VoiceCommand,
    val aliasNormalized: String,
    val valueNormalized: String
)

private data class ImplicitCommandMatch(
    val command: VoiceCommand,
    val valueRaw: String,
    val confidence: Int,
    val ambiguousWith: VoiceCommand? = null
)

private data class ImplicitCandidate(
    val command: VoiceCommand,
    val valueRaw: String,
    val score: Int
)

private data class SwitchIntent(
    val command: VoiceCommand,
    val value: String
)

private data class DateParts(
    val day: Int,
    val month: Int,
    val year: Int
)

private data class TimeParts(
    val hour: Int,
    val minute: Int
)

private data class CommandMarker(
    val command: VoiceCommand,
    val tokens: List<String>
)

private data class EmbeddedCommandTransition(
    val nextCommand: VoiceCommand,
    val beforeValue: String,
    val afterValue: String
)

object VoiceFormInterpreter {
    private val ruLocale = Locale.forLanguageTag("ru-RU")
    private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    private val commandPrefixes = listOf(
        "поле",
        "запиши",
        "впиши",
        "внеси",
        "заполни",
        "укажи",
        "поставь",
        "проставь",
        "введи",
        "добавь",
        "надиктуй",
        "заполни поле",
        "укажи поле",
        "внеси в поле",
        "в поле",
        "в карточку",
        "в форму"
    )
    private val commandPrefixesNormalized = commandPrefixes
        .map { normalize(it) }
        .distinct()
        .sortedByDescending { it.length }
    private val commandMarkers: List<CommandMarker> = buildCommandMarkers()

    fun commandGrammarJson(): String {
        val words = VoiceCommand.entries
            .flatMap { command ->
                command.aliases.flatMap { alias ->
                    commandPrefixes.map { prefix -> "$prefix $alias" }
                }
            }
            .distinct()
            .toMutableList()
        words.addAll(commandPrefixes)
        words.addAll(VoiceCommand.entries.flatMap { it.aliases })
        words.add("стоп")
        words.add("[unk]")
        return JSONArray(words.distinct()).toString()
    }

    fun applyUtterance(
        status: FieldStatus,
        utteranceRaw: String,
        draft: FieldFormDraft,
        session: VoiceSessionState
    ): VoiceInterpretResult {
        val utteranceRawTrimmed = utteranceRaw.trim()
        val utteranceNormalized = normalize(utteranceRawTrimmed)
        if (utteranceNormalized.isBlank()) {
            return VoiceInterpretResult(
                draft = draft,
                session = session.copy(lastUtterance = utteranceRaw)
            )
        }

        return when (session.mode) {
            VoiceInputMode.WAIT_COMMAND -> applyInCommandMode(status, utteranceRawTrimmed, utteranceNormalized, draft, session)
            VoiceInputMode.WAIT_VALUE -> applyInValueMode(status, utteranceRawTrimmed, utteranceNormalized, draft, session)
        }
    }

    private fun applyInCommandMode(
        status: FieldStatus,
        utteranceRaw: String,
        utteranceNormalized: String,
        draft: FieldFormDraft,
        session: VoiceSessionState
    ): VoiceInterpretResult {
        val explicitMode = startsWithSwitchPrefix(utteranceNormalized)
        val explicit = if (explicitMode) detectCommand(utteranceNormalized) else null
        val direct = if (!explicitMode) detectCommand(utteranceNormalized) else null
        val implicit = if (!explicitMode) detectImplicitCommand(status, utteranceRaw, utteranceNormalized) else null

        if (explicitMode && explicit == null) {
            return VoiceInterpretResult(
                draft = draft,
                session = session.copy(
                    lastUtterance = utteranceRaw,
                    statusText = "Команда не распознана. Скажите: поле ФИО / поле позывной / поле жетон"
                )
            )
        }

        if (!explicitMode && direct == null && implicit == null) {
            return VoiceInterpretResult(
                draft = draft,
                session = session.copy(
                    lastUtterance = utteranceRaw,
                    statusText = "Скажите команду с префиксом или фразу по смыслу (например: ранение 18 февраля)"
                )
            )
        }

        val command = explicit?.command ?: direct?.command ?: implicit!!.command
        val valuePart = if (explicit != null) {
            sanitizeRecognizedValue(
                extractValuePartRaw(utteranceRaw, explicit.aliasNormalized)
                    .ifBlank { explicit.valueNormalized }
            )
        } else if (direct != null) {
            sanitizeRecognizedValue(
                extractValuePartRaw(utteranceRaw, direct.aliasNormalized)
                    .ifBlank { direct.valueNormalized }
            )
        } else {
            sanitizeRecognizedValue(implicit!!.valueRaw)
        }

        if (valuePart.isNotBlank() && isCommandOnlyPhrase(valuePart)) {
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

        if (valuePart.isNotBlank()) {
            if (isDateCommand(command) && !isDateLikeValue(valuePart)) {
                return VoiceInterpretResult(
                    draft = draft,
                    session = VoiceSessionState(
                        mode = VoiceInputMode.WAIT_VALUE,
                        activeCommand = command,
                        lastUtterance = utteranceRaw,
                        statusText = "Для поля ${command.label} продиктуйте дату и время цифрами",
                        lastApplied = command.label
                    )
                )
            }

            val updated = applyValue(status, draft, command, valuePart)
            val filledText = if (explicit != null || direct != null) {
                "Поле ${command.label} заполнено. Скажите следующее поле"
            } else {
                val alt = implicit?.ambiguousWith?.let { " Есть похожий вариант: ${it.label}." }.orEmpty()
                "Поле ${command.label} заполнено по смыслу.$alt Проверьте в форме."
            }

            return VoiceInterpretResult(
                draft = updated,
                session = VoiceSessionState(
                    mode = VoiceInputMode.WAIT_COMMAND,
                    activeCommand = null,
                    lastUtterance = utteranceRaw,
                    statusText = filledText,
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
        utteranceNormalized: String,
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

        val embeddedTransition = findEmbeddedCommandTransition(utteranceNormalized, active)
        if (embeddedTransition != null) {
            return applyEmbeddedTransition(
                status = status,
                utteranceRaw = utteranceRaw,
                draft = draft,
                active = active,
                transition = embeddedTransition
            )
        }

        val explicitSwitch = if (startsWithSwitchPrefix(utteranceNormalized)) {
            detectCommand(utteranceNormalized)
        } else {
            null
        }

        val directSwitch = if (explicitSwitch == null) {
            detectCommand(utteranceNormalized)
        } else {
            null
        }

        val implicitSwitch = if (explicitSwitch == null && directSwitch == null) {
            detectImplicitCommand(status, utteranceRaw, utteranceNormalized)
        } else {
            null
        }

        val switched = when {
            explicitSwitch != null -> {
                val value = sanitizeRecognizedValue(
                    extractValuePartRaw(utteranceRaw, explicitSwitch.aliasNormalized)
                        .ifBlank { explicitSwitch.valueNormalized }
                )
                SwitchIntent(command = explicitSwitch.command, value = value)
            }
            directSwitch != null -> {
                val value = sanitizeRecognizedValue(
                    extractValuePartRaw(utteranceRaw, directSwitch.aliasNormalized)
                        .ifBlank { directSwitch.valueNormalized }
                )
                SwitchIntent(command = directSwitch.command, value = value)
            }
            implicitSwitch != null && implicitSwitch.confidence >= 7 -> {
                SwitchIntent(
                    command = implicitSwitch.command,
                    value = sanitizeRecognizedValue(implicitSwitch.valueRaw)
                )
            }
            else -> null
        }

        if (switched != null && switched.value.isBlank()) {
            return VoiceInterpretResult(
                draft = draft,
                session = VoiceSessionState(
                    mode = VoiceInputMode.WAIT_VALUE,
                    activeCommand = switched.command,
                    lastUtterance = utteranceRaw,
                    statusText = "Диктуйте значение для поля: ${switched.command.label}",
                    lastApplied = switched.command.label
                )
            )
        }

        if (switched != null && switched.command != active) {
            val switchedValue = switched.value
            if (switchedValue.isNotBlank()) {
                if (isDateCommand(switched.command) && !isDateLikeValue(switchedValue)) {
                    return VoiceInterpretResult(
                        draft = draft,
                        session = VoiceSessionState(
                            mode = VoiceInputMode.WAIT_VALUE,
                            activeCommand = switched.command,
                            lastUtterance = utteranceRaw,
                            statusText = "Для поля ${switched.command.label} продиктуйте дату и время цифрами",
                            lastApplied = switched.command.label
                        )
                    )
                }

                val updated = applyValue(status, draft, switched.command, switchedValue)
                return VoiceInterpretResult(
                    draft = updated,
                    session = VoiceSessionState(
                        mode = VoiceInputMode.WAIT_COMMAND,
                        activeCommand = null,
                        lastUtterance = utteranceRaw,
                        statusText = "Поле ${switched.command.label} заполнено. Скажите следующее поле",
                        lastApplied = switched.command.label
                    )
                )
            }

            return VoiceInterpretResult(
                draft = draft,
                session = VoiceSessionState(
                    mode = VoiceInputMode.WAIT_VALUE,
                    activeCommand = switched.command,
                    lastUtterance = utteranceRaw,
                    statusText = "Диктуйте значение для поля: ${switched.command.label}",
                    lastApplied = switched.command.label
                )
            )
        }

        val switchedSame = if (switched != null && switched.command == active) {
            switched.value
        } else {
            ""
        }

        val valuePart = sanitizeRecognizedValue(
            if (switchedSame.isNotBlank()) switchedSame else utteranceRaw.trim()
        )

        if (isCommandOnlyPhrase(valuePart)) {
            return VoiceInterpretResult(
                draft = draft,
                session = session.copy(
                    mode = VoiceInputMode.WAIT_VALUE,
                    activeCommand = active,
                    lastUtterance = utteranceRaw,
                    statusText = "Скажите значение для поля: ${active.label}"
                )
            )
        }

        if (valuePart.isBlank()) {
            return VoiceInterpretResult(
                draft = draft,
                session = session.copy(
                    lastUtterance = utteranceRaw,
                    statusText = "Не расслышано значение для поля: ${active.label}. Повторите еще раз"
                )
            )
        }

        if (isDateCommand(active) && !isDateLikeValue(valuePart)) {
            return VoiceInterpretResult(
                draft = draft,
                session = session.copy(
                    mode = VoiceInputMode.WAIT_VALUE,
                    activeCommand = active,
                    lastUtterance = utteranceRaw,
                    statusText = "Для поля ${active.label} продиктуйте дату и время цифрами"
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

    private fun applyEmbeddedTransition(
        status: FieldStatus,
        utteranceRaw: String,
        draft: FieldFormDraft,
        active: VoiceCommand,
        transition: EmbeddedCommandTransition
    ): VoiceInterpretResult {
        var updatedDraft = draft
        val beforeValue = sanitizeRecognizedValue(transition.beforeValue)
        val savedActive = beforeValue.isNotBlank() && !isCommandOnlyPhrase(beforeValue)

        if (savedActive) {
            updatedDraft = applyValue(status, updatedDraft, active, beforeValue)
        }

        val nextCommand = transition.nextCommand
        val nextValue = sanitizeRecognizedValue(transition.afterValue)

        if (nextValue.isBlank() || isCommandOnlyPhrase(nextValue)) {
            val statusText = if (savedActive) {
                "Поле ${active.label} сохранено. Диктуйте значение для поля: ${nextCommand.label}"
            } else {
                "Диктуйте значение для поля: ${nextCommand.label}"
            }

            return VoiceInterpretResult(
                draft = updatedDraft,
                session = VoiceSessionState(
                    mode = VoiceInputMode.WAIT_VALUE,
                    activeCommand = nextCommand,
                    lastUtterance = utteranceRaw,
                    statusText = statusText,
                    lastApplied = if (savedActive) active.label else nextCommand.label
                )
            )
        }

        if (isDateCommand(nextCommand) && !isDateLikeValue(nextValue)) {
            val statusText = if (savedActive) {
                "Поле ${active.label} сохранено. Для поля ${nextCommand.label} продиктуйте дату и время"
            } else {
                "Для поля ${nextCommand.label} продиктуйте дату и время"
            }

            return VoiceInterpretResult(
                draft = updatedDraft,
                session = VoiceSessionState(
                    mode = VoiceInputMode.WAIT_VALUE,
                    activeCommand = nextCommand,
                    lastUtterance = utteranceRaw,
                    statusText = statusText,
                    lastApplied = if (savedActive) active.label else nextCommand.label
                )
            )
        }

        val nextDraft = applyValue(status, updatedDraft, nextCommand, nextValue)
        val statusText = if (savedActive) {
            "Поле ${active.label} сохранено. Поле ${nextCommand.label} заполнено. Скажите следующее поле"
        } else {
            "Поле ${nextCommand.label} заполнено. Скажите следующее поле"
        }

        return VoiceInterpretResult(
            draft = nextDraft,
            session = VoiceSessionState(
                mode = VoiceInputMode.WAIT_COMMAND,
                activeCommand = null,
                lastUtterance = utteranceRaw,
                statusText = statusText,
                lastApplied = nextCommand.label
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
            VoiceCommand.FILLED_AT -> draft.copy(filledAt = normalizeDateLikeValue(value, draft.filledAt))
            VoiceCommand.FULL_NAME -> draft.copy(fullName = titleCase(value))
            VoiceCommand.CALLSIGN -> draft.copy(callsign = value)
            VoiceCommand.TAG_NUMBER -> draft.copy(tagNumber = normalizeTagNumber(value))
            VoiceCommand.EVENT_AT -> draft.copy(eventAt = normalizeDateLikeValue(value, draft.eventAt))
            VoiceCommand.INJURY_KIND -> if (status == FieldStatus.RANEN) draft.copy(injuryKind = normalizeNumbersInText(value)) else draft
            VoiceCommand.DIAGNOSIS -> if (status == FieldStatus.RANEN) draft.copy(diagnosis = normalizeNumbersInText(value)) else draft
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

    private fun normalizeTagNumber(value: String): String {
        val extracted = extractTagLikeValue(value)
        if (extracted != null) {
            return extracted
                .replace(Regex("\\s+"), "")
                .uppercase(Locale.getDefault())
        }

        return value
            .trim()
            .replace(Regex("\\s+"), "")
            .uppercase(Locale.getDefault())
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
        val trimmed = normalizeNumbersInText(value.trim())
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

        val qtyFirst = Regex("^(\\d[\\d\\s.,]*(?:\\s*\\p{L}+){0,2})\\s+(.+)$").find(trimmed)
        if (qtyFirst != null) {
            return MedicineItemDraft(
                name = qtyFirst.groupValues[2].trim().ifBlank { "-" },
                qty = qtyFirst.groupValues[1].trim().ifBlank { "-" }
            )
        }

        return MedicineItemDraft(name = trimmed, qty = "-")
    }

    private fun normalizeNumbersInText(value: String): String {
        val normalized = normalize(value)
        if (normalized.isBlank()) return ""

        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return ""

        val out = ArrayList<String>(tokens.size)
        var i = 0

        while (i < tokens.size) {
            val token = tokens[i]
            if (token.toIntOrNull() != null) {
                out.add(token)
                i++
                continue
            }

            val asWord = wordToNumber(token)
            if (asWord != null) {
                val parsed = parseNumberAt(tokens, i)
                if (parsed != null) {
                    out.add(parsed.first.toString())
                    i = parsed.second
                    continue
                }
            }

            out.add(token)
            i++
        }

        return out.joinToString(" ").trim()
    }

    private fun normalizeDateLikeValue(value: String, currentValue: String? = null): String {
        val n = normalize(value)
        if (n == "сейчас" || n == "текущее время") return dateTimeFormat.format(Date())

        val parsed = parseDateLikeValue(value, currentValue)
        if (parsed != null) return parsed

        return value
            .replace(" точка ", ".")
            .replace(" двоеточие ", ":")
            .replace(" запятая ", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun parseDateTimeFromNumbers(value: String): String? {
        val compact = Regex("\\d").findAll(value).joinToString("") { it.value }
        if (compact.length >= 12) {
            val day = compact.substring(0, 2).toIntOrNull() ?: return null
            val month = compact.substring(2, 4).toIntOrNull() ?: return null
            val year = compact.substring(4, 8).toIntOrNull() ?: return null
            val hour = compact.substring(8, 10).toIntOrNull() ?: return null
            val minute = compact.substring(10, 12).toIntOrNull() ?: return null

            if (day in 1..31 && month in 1..12 && year in 2000..2100 && hour in 0..23 && minute in 0..59) {
                return String.format(
                    Locale.getDefault(),
                    "%02d.%02d.%04d %02d:%02d",
                    day,
                    month,
                    year,
                    hour,
                    minute
                )
            }
        }

        val numsByDigits = Regex("\\d{1,4}")
            .findAll(value)
            .mapNotNull { it.value.toIntOrNull() }
            .toList()

        val nums = if (numsByDigits.size >= 4) {
            numsByDigits
        } else {
            extractNumericValuesFromWords(value)
        }

        if (nums.size < 2) return null

        val now = Calendar.getInstance()
        val yearNow = now.get(Calendar.YEAR)
        val hourNow = now.get(Calendar.HOUR_OF_DAY)
        val minuteNow = now.get(Calendar.MINUTE)
        val normalized = normalize(value)
        val hasDateCue = containsMonthWord(normalized) ||
            containsAnyStem(normalized, listOf("дат", "числ", "месяц", "год", "ранен", "смерт", "событ", "заполн", "оформ"))

        val day: Int
        val month: Int
        val year: Int
        val hour: Int
        val minute: Int

        when {
            nums.size >= 5 -> {
                day = nums[0]
                month = nums[1]
                year = if (nums[2] < 100) 2000 + nums[2] else nums[2]
                hour = nums[3]
                minute = nums[4]
            }
            nums.size == 4 -> {
                day = nums[0]
                month = nums[1]
                year = yearNow
                hour = nums[2]
                minute = nums[3]
            }
            nums.size == 3 -> {
                day = nums[0]
                month = nums[1]
                year = if (nums[2] < 100) 2000 + nums[2] else nums[2]
                hour = hourNow
                minute = minuteNow
            }
            nums.size == 2 -> {
                val first = nums[0]
                val second = nums[1]

                val likelyDate = hasDateCue || (first >= 13 && second <= 12)
                val validTimePair = first in 0..23 && second in 0..59

                if (!likelyDate && validTimePair) {
                    return null
                }

                day = first
                month = second
                year = yearNow
                hour = hourNow
                minute = minuteNow
            }
            else -> return null
        }

        if (day !in 1..31) return null
        if (month !in 1..12) return null
        if (hour !in 0..23) return null
        if (minute !in 0..59) return null
        if (year !in 2000..2100) return null

        return String.format(
            Locale.getDefault(),
            "%02d.%02d.%04d %02d:%02d",
            day,
            month,
            year,
            hour,
            minute
        )
    }

    private fun extractNumericValuesFromWords(value: String): List<Int> {
        val tokens = normalize(value)
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        val out = ArrayList<Int>()
        var i = 0

        while (i < tokens.size) {
            val t = tokens[i]

            if (t.all { it.isDigit() }) {
                out.add(t.toIntOrNull() ?: 0)
                i++
                continue
            }

            if (isTwoToken(t) && i + 1 < tokens.size && isThousandToken(tokens[i + 1])) {
                var year = 2000
                i += 2

                val parsedTail = parseTensUnits(tokens, i)
                if (parsedTail != null) {
                    year += parsedTail.first
                    i = parsedTail.second
                }

                out.add(year)
                continue
            }

            if (isZeroToken(t) && i + 1 < tokens.size) {
                val next = wordToNumber(tokens[i + 1])
                if (next != null && next in 1..9) {
                    out.add(next)
                    i += 2
                    continue
                }
            }

            val tensUnits = parseTensUnits(tokens, i)
            if (tensUnits != null) {
                out.add(tensUnits.first)
                i = tensUnits.second
                continue
            }

            val single = wordToNumber(t)
            if (single != null) {
                out.add(single)
                i++
                continue
            }

            i++
        }

        return out
    }

    private fun parseTensUnits(tokens: List<String>, start: Int): Pair<Int, Int>? {
        if (start >= tokens.size) return null

        val first = numericFromToken(tokens[start]) ?: return null

        if (first in 20..90 && first % 10 == 0 && start + 1 < tokens.size) {
            val second = numericFromToken(tokens[start + 1])
            if (second != null && second in 1..9) {
                return (first + second) to (start + 2)
            }
        }

        if (first == 0 && start + 1 < tokens.size) {
            val second = numericFromToken(tokens[start + 1])
            if (second != null && second in 1..9) {
                return second to (start + 2)
            }
        }

        return first to (start + 1)
    }

    private fun isZeroToken(token: String): Boolean = token.startsWith("нол")

    private fun isTwoToken(token: String): Boolean =
        token == "два" || token == "две" || token.startsWith("втор")

    private fun isThousandToken(token: String): Boolean = token.startsWith("тысяч")

    private fun wordToNumber(token: String): Int? {
        return when {
            token.startsWith("тысяч") -> 1000

            token.startsWith("девятьсот") -> 900
            token.startsWith("восемьсот") -> 800
            token.startsWith("семьсот") -> 700
            token.startsWith("шестьсот") -> 600
            token.startsWith("пятьсот") -> 500
            token.startsWith("четырест") -> 400
            token.startsWith("трист") -> 300
            token.startsWith("двест") -> 200
            token.startsWith("сто") -> 100

            token.startsWith("одиннадцат") -> 11
            token.startsWith("двенадцат") -> 12
            token.startsWith("тринадцат") -> 13
            token.startsWith("четырнадцат") -> 14
            token.startsWith("пятнадцат") -> 15
            token.startsWith("шестнадцат") -> 16
            token.startsWith("семнадцат") -> 17
            token.startsWith("восемнадцат") -> 18
            token.startsWith("девятнадцат") -> 19
            token.startsWith("десят") -> 10

            token.startsWith("двадцат") -> 20
            token.startsWith("тридцат") -> 30
            token.startsWith("сорок") -> 40
            token.startsWith("пятьдесят") -> 50
            token.startsWith("шестьдесят") -> 60
            token.startsWith("семьдесят") -> 70
            token.startsWith("восемьдесят") -> 80
            token.startsWith("девяност") -> 90

            token.startsWith("нол") -> 0
            token == "один" || token == "одна" || token == "одно" || token.startsWith("перв") -> 1
            token == "два" || token == "две" || token.startsWith("втор") -> 2
            token.startsWith("три") || token.startsWith("трет") -> 3
            token.startsWith("четыр") || token.startsWith("четвер") || token.startsWith("четр") -> 4
            token.startsWith("пят") -> 5
            token.startsWith("шест") -> 6
            token.startsWith("сем") -> 7
            token.startsWith("восем") -> 8
            token.startsWith("девят") -> 9
            else -> null
        }
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

    private fun buildCommandMarkers(): List<CommandMarker> {
        val markers = ArrayList<CommandMarker>()

        for (command in VoiceCommand.entries) {
            val aliases = command.aliases
                .map { normalize(it) }
                .distinct()

            for (alias in aliases) {
                val aliasTokens = alias.split(Regex("\\s+")).filter { it.isNotBlank() }
                if (aliasTokens.isEmpty()) continue

                for (prefix in commandPrefixesNormalized) {
                    val prefixTokens = prefix.split(Regex("\\s+")).filter { it.isNotBlank() }
                    if (prefixTokens.isEmpty()) continue

                    markers.add(
                        CommandMarker(
                            command = command,
                            tokens = prefixTokens + aliasTokens
                        )
                    )
                }
            }
        }

        return markers
            .distinctBy { marker -> marker.command.name + "|" + marker.tokens.joinToString(" ") }
            .sortedByDescending { it.tokens.size }
    }

    private fun findEmbeddedCommandTransition(
        utteranceNormalized: String,
        active: VoiceCommand
    ): EmbeddedCommandTransition? {
        val tokens = utteranceNormalized
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (tokens.size < 2) return null

        var bestStart = Int.MAX_VALUE
        var bestEnd = -1
        var bestLength = -1
        var bestCommand: VoiceCommand? = null

        for (i in tokens.indices) {
            for (marker in commandMarkers) {
                if (marker.command == active) continue

                val markerTokens = marker.tokens
                val markerSize = markerTokens.size
                if (markerSize == 0) continue
                if (i + markerSize > tokens.size) continue

                var matched = true
                for (j in markerTokens.indices) {
                    if (tokens[i + j] != markerTokens[j]) {
                        matched = false
                        break
                    }
                }
                if (!matched) continue

                val better = i < bestStart || (i == bestStart && markerSize > bestLength)
                if (better) {
                    bestStart = i
                    bestEnd = i + markerSize
                    bestLength = markerSize
                    bestCommand = marker.command
                }
            }
        }

        val nextCommand = bestCommand ?: return null
        if (bestStart <= 0) return null

        val before = tokens.subList(0, bestStart).joinToString(" ").trim()
        val after = if (bestEnd <= tokens.size) {
            tokens.subList(bestEnd, tokens.size).joinToString(" ").trim()
        } else {
            ""
        }

        return EmbeddedCommandTransition(
            nextCommand = nextCommand,
            beforeValue = before,
            afterValue = after
        )
    }

    private fun detectCommand(textNormalized: String): DetectedCommand? {
        val allAliases = VoiceCommand.entries.flatMap { cmd ->
            cmd.aliases.map { alias -> cmd to normalize(alias) }
        }.sortedByDescending { it.second.length }

        val normalized = removeCommandPrefix(normalize(textNormalized))

        for ((cmd, alias) in allAliases) {
            if (normalized == alias || normalized.startsWith("$alias ")) {
                val value = normalized.removePrefix(alias).trim()
                return DetectedCommand(
                    command = cmd,
                    aliasNormalized = alias,
                    valueNormalized = value
                )
            }
        }
        return null
    }

    private fun detectImplicitCommand(
        status: FieldStatus,
        utteranceRaw: String,
        utteranceNormalized: String
    ): ImplicitCommandMatch? {
        val raw = utteranceRaw.trim()
        if (raw.isBlank()) return null

        val normalized = utteranceNormalized.ifBlank { normalize(raw) }
        val candidates = ArrayList<ImplicitCandidate>()

        buildEventCandidate(status, raw, normalized)?.let(candidates::add)
        buildFilledAtCandidate(raw, normalized)?.let(candidates::add)
        buildTagNumberCandidate(raw, normalized)?.let(candidates::add)
        buildCallsignCandidate(raw, normalized)?.let(candidates::add)
        buildFullNameCandidate(raw, normalized)?.let(candidates::add)
        buildEvacMethodCandidate(raw, normalized)?.let(candidates::add)
        buildMedicineCandidate(raw, normalized)?.let(candidates::add)

        if (status == FieldStatus.RANEN) {
            buildInjuryKindCandidate(raw, normalized)?.let(candidates::add)
            buildDiagnosisCandidate(raw, normalized)?.let(candidates::add)
            buildLocalizationCandidate(raw, normalized)?.let(candidates::add)
        }

        if (candidates.isEmpty()) return null

        val sorted = candidates.sortedByDescending { it.score }
        val top = sorted.first()
        val second = sorted.getOrNull(1)
        val ambiguousWith = if (second != null && second.command != top.command && second.score >= top.score - 1) {
            second.command
        } else {
            null
        }

        return ImplicitCommandMatch(
            command = top.command,
            valueRaw = top.valueRaw,
            confidence = top.score,
            ambiguousWith = ambiguousWith
        )
    }

    private fun buildEventCandidate(status: FieldStatus, raw: String, normalized: String): ImplicitCandidate? {
        var score = 0
        if (containsAnyStem(normalized, listOf("ранен", "ранени", "поражен", "травм", "смерт", "погиб", "гибел", "умер", "кончин", "событ"))) score += 4
        if (containsAnyStem(normalized, listOf("время", "врем", "дата", "числ", "час", "минут", "когда"))) score += 1
        if (parseDateLikeValue(raw, null) != null || containsMonthWord(normalized)) score += 4
        if (status == FieldStatus.RANEN && containsAnyStem(normalized, listOf("ранен", "ранени", "поражен", "травм"))) score += 2
        if (status == FieldStatus.POGIB && containsAnyStem(normalized, listOf("смерт", "погиб", "гибел", "умер", "кончин"))) score += 2
        if (score < 6) return null

        val value = stripLeadingMarkerWords(
            raw,
            listOf(
                "ранен",
                "ранени",
                "поражен",
                "травм",
                "смерт",
                "погиб",
                "гибел",
                "умер",
                "кончин",
                "событ",
                "время",
                "врем",
                "дата",
                "когда"
            )
        )
            .ifBlank { raw }

        return ImplicitCandidate(VoiceCommand.EVENT_AT, valueRaw = value, score = score)
    }

    private fun buildFilledAtCandidate(raw: String, normalized: String): ImplicitCandidate? {
        var score = 0
        if (containsAnyStem(normalized, listOf("заполн", "оформ", "состав", "внес", "запис"))) score += 6
        if (containsAnyStem(normalized, listOf("карточк", "форм", "бланк", "документ"))) score += 1
        if (parseDateLikeValue(raw, null) != null || containsMonthWord(normalized)) score += 3
        if (score < 7) return null

        val value = stripLeadingMarkerWords(
            raw,
            listOf("заполн", "оформ", "состав", "внес", "запис", "время", "дата", "карточк", "форм", "бланк")
        )
            .ifBlank { raw }

        return ImplicitCandidate(VoiceCommand.FILLED_AT, valueRaw = value, score = score)
    }

    private fun buildTagNumberCandidate(raw: String, normalized: String): ImplicitCandidate? {
        var score = 0
        if (containsAnyStem(normalized, listOf("жетон", "номер", "личн", "личк", "ид", "айди", "идент", "бирк", "карточ"))) score += 4

        val extracted = extractTagLikeValue(raw)
        if (extracted != null) score += 4
        if (score < 7 || extracted == null) return null

        return ImplicitCandidate(VoiceCommand.TAG_NUMBER, valueRaw = extracted, score = score)
    }

    private fun buildCallsignCandidate(raw: String, normalized: String): ImplicitCandidate? {
        var score = 0
        if (containsAnyStem(normalized, listOf("позыв", "радио", "кодов"))) score += 7
        if (score < 7) return null

        val value = stripLeadingMarkerWords(raw, listOf("позыв", "радио", "кодов", "имя", "рации"))
            .ifBlank { raw }

        return ImplicitCandidate(VoiceCommand.CALLSIGN, valueRaw = value, score = score)
    }

    private fun buildFullNameCandidate(raw: String, normalized: String): ImplicitCandidate? {
        var score = 0
        if (containsAnyStem(normalized, listOf("фио", "фамил", "имя", "отчеств", "полное", "зовут"))) score += 7
        if (score < 7) return null

        val value = stripLeadingMarkerWords(raw, listOf("фио", "фамил", "имя", "отчеств", "полное", "зовут", "данные", "бойца"))
            .ifBlank { raw }
        if (value.split(Regex("\\s+")).size < 2) return null

        return ImplicitCandidate(VoiceCommand.FULL_NAME, valueRaw = value, score = score)
    }

    private fun buildInjuryKindCandidate(raw: String, normalized: String): ImplicitCandidate? {
        var score = 0
        if (containsAnyStem(normalized, listOf("вид", "тип", "характер", "поражен", "травм", "ранен", "механизм"))) score += 3
        if (containsAnyStem(normalized, listOf("огнестр", "оскол", "минн", "ожог", "перелом", "контуз", "ушиб", "разрыв", "ампутац", "баротрав", "хим", "бак", "радиац", "псих", "отморож", "отрав"))) score += 4
        if (score < 6) return null

        val value = stripLeadingMarkerWords(raw, listOf("вид", "тип", "характер", "поражен", "травм", "ранен", "механизм"))
            .ifBlank { raw }

        return ImplicitCandidate(VoiceCommand.INJURY_KIND, valueRaw = value, score = score)
    }

    private fun buildDiagnosisCandidate(raw: String, normalized: String): ImplicitCandidate? {
        var score = 0
        if (containsAnyStem(normalized, listOf("диагноз", "диагност", "заключ", "состояни", "клинич"))) score += 9
        if (score < 8) return null

        val value = stripLeadingMarkerWords(raw, listOf("диагноз", "диагност", "заключ", "состояни", "клинич", "описан"))
            .ifBlank { raw }

        return ImplicitCandidate(VoiceCommand.DIAGNOSIS, valueRaw = value, score = score)
    }

    private fun buildLocalizationCandidate(raw: String, normalized: String): ImplicitCandidate? {
        var score = 0
        if (containsAnyStem(normalized, listOf("локализ", "место", "област", "зон", "част"))) score += 4
        if (containsAnyStem(normalized, listOf("голов", "ше", "груд", "спин", "живот", "таз", "плеч", "рук", "кист", "ног", "бедр", "колен", "стоп", "множе"))) score += 3
        if (score < 6) return null

        val value = stripLeadingMarkerWords(raw, listOf("локализ", "место", "област", "зон", "част", "где", "ранен"))
            .ifBlank { raw }

        return ImplicitCandidate(VoiceCommand.LOCALIZATION, valueRaw = value, score = score)
    }

    private fun buildEvacMethodCandidate(raw: String, normalized: String): ImplicitCandidate? {
        var score = 0
        if (containsAnyStem(normalized, listOf("эвак", "достав", "транспорт", "вывез", "перевез", "перемещ", "способ", "метод"))) score += 4
        if (containsAnyStem(normalized, listOf("самостоятель", "пеш", "носил", "санитар", "медицин", "грузов", "брон", "бтр", "вертолет", "вертол", "авиа", "машин", "авто"))) score += 4
        if (score < 6) return null

        val value = stripLeadingMarkerWords(raw, listOf("эвак", "достав", "транспорт", "вывез", "перевез", "перемещ", "способ", "метод"))
            .ifBlank { raw }

        return ImplicitCandidate(VoiceCommand.EVAC_METHOD, valueRaw = value, score = score)
    }

    private fun buildMedicineCandidate(raw: String, normalized: String): ImplicitCandidate? {
        var score = 0
        if (containsAnyStem(normalized, listOf("препарат", "лекар", "медикамент", "доз", "дозиров", "ввели", "ввел", "дали", "укол", "инъек", "таблет", "ампул", "капсул"))) score += 5
        if (containsAnyStem(normalized, listOf("амп", "мл", "мг", "табл", "доз", "капсул", "инъек", "шприц", "флакон", "капл"))) score += 3
        if (score < 6) return null

        val value = stripLeadingMarkerWords(raw, listOf("препарат", "лекар", "медикамент", "количеств", "доз", "дозиров", "ввели", "дали", "укол", "инъек"))
            .ifBlank { raw }

        return ImplicitCandidate(VoiceCommand.MEDICINE, valueRaw = value, score = score)
    }

    private fun stripLeadingMarkerWords(raw: String, markerStems: List<String>): String {
        val tokens = raw.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return ""

        var idx = 0
        while (idx < tokens.size) {
            val tokenNorm = normalize(tokens[idx])
            val marker = markerStems.any { tokenNorm.startsWith(it) }
            val filler = tokenNorm in setOf(
                "это",
                "этого",
                "в",
                "во",
                "на",
                "по",
                "при",
                "о",
                "об",
                "пожалуйста",
                "нужно",
                "надо",
                "вот",
                "тут",
                "значит",
                "поле"
            )
            if (!marker && !filler) break
            idx++
        }

        return tokens.drop(idx).joinToString(" ").trim()
    }

    private fun containsAnyStem(normalized: String, stems: List<String>): Boolean {
        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        return tokens.any { token -> stems.any { stem -> token.startsWith(stem) } }
    }

    private fun extractTagLikeValue(raw: String): String? {
        val tokenRegex = Regex("(?iu)\\b([A-Za-zА-Яа-я]{0,3}-?\\d{3,}[A-Za-zА-Яа-я0-9-]*)\\b")
        val token = tokenRegex.find(raw)?.groupValues?.getOrNull(1)?.trim()
        if (!token.isNullOrBlank()) return token

        val afterKeyword = Regex("(?iu)(?:жетон\\S*|номер\\S*|личн\\S*|личк\\S*|id\\S*|айди\\S*|ид\\S*|идент\\S*|бирк\\S*|карточ\\S*)\\s*[:\\-]?\\s*([A-Za-zА-Яа-я0-9\\-\\s]{2,})")
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        if (!afterKeyword.isNullOrBlank()) {
            val compact = afterKeyword.replace(Regex("\\s+"), "")
            if (compact.any { it.isLetter() } && compact.any { it.isDigit() }) return compact
            if (compact.all { it.isDigit() } && compact.length >= 4) return compact

            val parsedAfterKeyword = parseTagFromTokens(
                normalize(afterKeyword)
                    .split(Regex("\\s+"))
                    .filter { it.isNotBlank() }
            )
            if (!parsedAfterKeyword.isNullOrBlank()) return parsedAfterKeyword
        }

        val normalizedTokens = normalize(raw)
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        val keywordIndex = normalizedTokens.indexOfFirst { isTagKeywordToken(it) }
        if (keywordIndex >= 0) {
            val parsedAfterKeyword = parseTagFromTokens(normalizedTokens.drop(keywordIndex + 1))
            if (!parsedAfterKeyword.isNullOrBlank()) return parsedAfterKeyword
        }

        return parseTagFromTokens(normalizedTokens)
    }

    private fun isTagKeywordToken(token: String): Boolean {
        return token.startsWith("жетон") ||
            token.startsWith("номер") ||
            token.startsWith("личн") ||
            token.startsWith("личк") ||
            token == "id" ||
            token.startsWith("ид") ||
            token.startsWith("айди") ||
            token.startsWith("идент") ||
            token.startsWith("бирк") ||
            token.startsWith("карточ")
    }

    private fun parseTagFromTokens(tokens: List<String>): String? {
        if (tokens.isEmpty()) return null

        val prefix = StringBuilder()
        val digits = StringBuilder()
        var consumed = false

        for (rawToken in tokens.take(14)) {
            val token = normalize(rawToken)
            if (token.isBlank()) continue
            if (isTagKeywordToken(token) || isTagNoiseToken(token)) continue

            if (token.length == 1 && token[0].isLetter()) {
                if (digits.isEmpty() && prefix.length < 3) {
                    prefix.append(token.uppercase(Locale.getDefault()))
                    consumed = true
                    continue
                }
                if (consumed) break
                continue
            }

            if (token.all { it.isDigit() }) {
                digits.append(token)
                consumed = true
                continue
            }

            val numeric = numericFromToken(token)
            if (numeric != null) {
                when {
                    numeric in 0..9 -> digits.append(numeric)
                    numeric in 10..99 -> digits.append(numeric)
                    else -> Unit
                }
                consumed = true
                continue
            }

            if (consumed) break
        }

        if (digits.length < 3) return null
        return (prefix.toString() + digits.toString()).ifBlank { null }
    }

    private fun isTagNoiseToken(token: String): Boolean {
        return token == "это" ||
            token == "мой" ||
            token == "его" ||
            token == "ее" ||
            token == "номер" ||
            token == "номерок" ||
            token == "вот" ||
            token == "такой" ||
            token == "примерно" ||
            token == "пример" ||
            token == "тире" ||
            token == "дефис"
    }

    private fun containsMonthWord(normalized: String): Boolean {
        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        return tokens.any { monthFromToken(it) != null }
    }

    private fun monthFromToken(tokenRaw: String): Int? {
        val token = normalize(tokenRaw)
        return when {
            token.startsWith("январ") || token == "янв" -> 1
            token.startsWith("феврал") || token.startsWith("февр") || token == "фев" -> 2
            token.startsWith("март") || token.startsWith("марта") -> 3
            token.startsWith("апрел") || token == "апр" -> 4
            token == "май" || token.startsWith("мая") -> 5
            token.startsWith("июн") || token == "июн" -> 6
            token.startsWith("июл") || token == "июл" -> 7
            token.startsWith("август") || token == "авг" -> 8
            token.startsWith("сентябр") || token.startsWith("сент") -> 9
            token.startsWith("октябр") || token.startsWith("окт") -> 10
            token.startsWith("ноябр") || token.startsWith("ноя") -> 11
            token.startsWith("декабр") || token.startsWith("дек") -> 12
            else -> null
        }
    }

    private fun parseDateLikeValue(value: String, currentValue: String?): String? {
        parseDateTimeWithMonthWords(value)?.let { return it }
        parseRelativeDateTime(value)?.let { return it }
        parseDateTimeFromNumbers(value)?.let { return it }

        val dateParts = parseDateOnlyParts(value)
        val timeParts = parseTimeOnlyParts(value)
        if (dateParts == null && timeParts == null) return null

        val base = parseKnownDateTime(currentValue) ?: Calendar.getInstance()

        if (dateParts != null) {
            base.set(Calendar.DAY_OF_MONTH, dateParts.day)
            base.set(Calendar.MONTH, dateParts.month - 1)
            base.set(Calendar.YEAR, dateParts.year)
        }

        if (timeParts != null) {
            base.set(Calendar.HOUR_OF_DAY, timeParts.hour)
            base.set(Calendar.MINUTE, timeParts.minute)
        }

        base.set(Calendar.SECOND, 0)
        base.set(Calendar.MILLISECOND, 0)
        return dateTimeFormat.format(base.time)
    }

    private fun parseKnownDateTime(value: String?): Calendar? {
        if (value.isNullOrBlank()) return null
        val parsed = runCatching { dateTimeFormat.parse(value.trim()) }.getOrNull() ?: return null
        return Calendar.getInstance().apply { time = parsed }
    }

    private fun parseDateOnlyParts(value: String): DateParts? {
        parseRelativeDateCalendar(value)?.let { cal ->
            return DateParts(
                day = cal.get(Calendar.DAY_OF_MONTH),
                month = cal.get(Calendar.MONTH) + 1,
                year = cal.get(Calendar.YEAR)
            )
        }

        parseDatePartsWithSeparators(value)?.let { return it }

        val normalized = normalize(value)
        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        val monthIndex = tokens.indexOfFirst { monthFromToken(it) != null }
        if (monthIndex >= 0) {
            val month = monthFromToken(tokens[monthIndex]) ?: return null
            val day = findDayNearMonth(tokens, monthIndex) ?: return null
            val year = extractYear(tokens, value, Calendar.getInstance())
            if (day in 1..31 && month in 1..12 && year in 2000..2100) {
                return DateParts(day = day, month = month, year = year)
            }
        }

        return null
    }

    private fun parseDatePartsWithSeparators(value: String): DateParts? {
        val nowYear = Calendar.getInstance().get(Calendar.YEAR)
        val match = Regex("\\b(\\d{1,2})[./-](\\d{1,2})(?:[./-](\\d{2,4}))?\\b")
            .find(value)
            ?: return null

        val day = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull() ?: return null
        val yearRaw = match.groupValues.getOrNull(3).orEmpty()
        val year = when {
            yearRaw.isBlank() -> nowYear
            yearRaw.length == 2 -> 2000 + (yearRaw.toIntOrNull() ?: return null)
            else -> yearRaw.toIntOrNull() ?: return null
        }

        if (day !in 1..31 || month !in 1..12 || year !in 2000..2100) return null
        return DateParts(day = day, month = month, year = year)
    }

    private fun parseTimeOnlyParts(value: String): TimeParts? {
        val hm = parseHourMinute(value) ?: return null
        if (hm.first !in 0..23 || hm.second !in 0..59) return null
        return TimeParts(hour = hm.first, minute = hm.second)
    }

    private fun parseRelativeDateCalendar(value: String): Calendar? {
        val normalized = normalize(value)
        val cal = Calendar.getInstance()
        when {
            normalized.contains("сегодня") -> Unit
            normalized.contains("позавчера") -> cal.add(Calendar.DAY_OF_MONTH, -2)
            normalized.contains("вчера") -> cal.add(Calendar.DAY_OF_MONTH, -1)
            normalized.contains("послезавтра") -> cal.add(Calendar.DAY_OF_MONTH, 2)
            normalized.contains("завтра") -> cal.add(Calendar.DAY_OF_MONTH, 1)
            else -> return null
        }
        return cal
    }

    private fun parseRelativeDateTime(value: String): String? {
        val dateCal = parseRelativeDateCalendar(value) ?: return null
        val now = Calendar.getInstance()
        val time = parseHourMinute(value)
        val hour = time?.first ?: now.get(Calendar.HOUR_OF_DAY)
        val minute = time?.second ?: now.get(Calendar.MINUTE)

        dateCal.set(Calendar.HOUR_OF_DAY, hour)
        dateCal.set(Calendar.MINUTE, minute)
        dateCal.set(Calendar.SECOND, 0)
        dateCal.set(Calendar.MILLISECOND, 0)

        return dateTimeFormat.format(dateCal.time)
    }

    private fun parseDateTimeWithMonthWords(value: String): String? {
        val normalized = normalize(value)
        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        val monthIndex = tokens.indexOfFirst { monthFromToken(it) != null }
        if (monthIndex < 0) return null

        val month = monthFromToken(tokens[monthIndex]) ?: return null
        val day = findDayNearMonth(tokens, monthIndex) ?: return null

        val now = Calendar.getInstance()
        val year = extractYear(tokens, value, now)
        val time = parseHourMinute(value)
        val hour = time?.first ?: now.get(Calendar.HOUR_OF_DAY)
        val minute = time?.second ?: now.get(Calendar.MINUTE)

        if (day !in 1..31 || month !in 1..12 || year !in 2000..2100 || hour !in 0..23 || minute !in 0..59) {
            return null
        }

        return String.format(
            Locale.getDefault(),
            "%02d.%02d.%04d %02d:%02d",
            day,
            month,
            year,
            hour,
            minute
        )
    }

    private fun findDayNearMonth(tokens: List<String>, monthIndex: Int): Int? {
        if (monthIndex >= 2) {
            val leftCombined = parseNumberAt(tokens, monthIndex - 2)
            if (leftCombined != null && leftCombined.second == monthIndex && leftCombined.first in 1..31) {
                return leftCombined.first
            }
        }

        if (monthIndex >= 1) {
            val leftSingle = numericFromToken(tokens[monthIndex - 1])
            if (leftSingle != null && leftSingle in 1..31) return leftSingle
        }

        if (monthIndex + 1 <= tokens.lastIndex) {
            val rightSingle = numericFromToken(tokens[monthIndex + 1])
            if (rightSingle != null && rightSingle in 1..31) return rightSingle
        }

        if (monthIndex + 2 <= tokens.lastIndex) {
            val rightCombined = parseNumberAt(tokens, monthIndex + 1)
            if (rightCombined != null && rightCombined.second >= monthIndex + 3 && rightCombined.first in 1..31) {
                return rightCombined.first
            }
        }

        return null
    }

    private fun extractYear(tokens: List<String>, raw: String, now: Calendar): Int {
        val yearDigits = Regex("\\b(20\\d{2})\\b")
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        if (yearDigits != null) return yearDigits

        val wordNumbers = extractNumericValuesFromWords(raw)
        val yearByWords = wordNumbers.firstOrNull { it in 2000..2100 }
        if (yearByWords != null) return yearByWords

        val yearTokenIndex = tokens.indexOfFirst { it.startsWith("год") || it == "года" || it == "году" || it == "ода" }
        if (yearTokenIndex > 0) {
            val startWindow = maxOf(0, yearTokenIndex - 3)
            for (start in startWindow until yearTokenIndex) {
                val parsed = parseNumberAt(tokens, start) ?: continue
                if (parsed.second == yearTokenIndex && parsed.first in 0..99) {
                    return 2000 + parsed.first
                }
            }
        }

        if (tokens.any { it.startsWith("прошл") }) return now.get(Calendar.YEAR) - 1
        if (tokens.any { it.startsWith("следующ") }) return now.get(Calendar.YEAR) + 1

        val hasCurrentRef = tokens.any { it.startsWith("эт") || it.startsWith("текущ") }
        val hasYearWord = tokens.any { it.startsWith("год") || it == "года" || it == "году" || it == "ода" }
        if (hasCurrentRef && hasYearWord) return now.get(Calendar.YEAR)

        return now.get(Calendar.YEAR)
    }

    private fun numericFromToken(token: String): Int? {
        return token.toIntOrNull() ?: wordToNumber(token)
    }

    private fun parseNumberAt(tokens: List<String>, start: Int): Pair<Int, Int>? {
        if (start < 0 || start >= tokens.size) return null

        val direct = numericFromToken(tokens[start]) ?: return null

        if (direct in 20..90 && direct % 10 == 0 && start + 1 < tokens.size) {
            val next = numericFromToken(tokens[start + 1])
            if (next != null && next in 1..9) {
                return (direct + next) to (start + 2)
            }
        }

        if (direct == 0 && start + 1 < tokens.size) {
            val next = numericFromToken(tokens[start + 1])
            if (next != null && next in 1..9) {
                return next to (start + 2)
            }
        }

        return direct to (start + 1)
    }

    private fun parseHourMinute(raw: String): Pair<Int, Int>? {
        val hhmm = Regex("(?iu)\\b([01]?\\d|2[0-3])\\s*[:.]\\s*([0-5]?\\d)\\b")
            .find(raw)
        if (hhmm != null) {
            val h = hhmm.groupValues[1].toIntOrNull() ?: return null
            val m = hhmm.groupValues[2].toIntOrNull() ?: return null
            return h to m
        }

        val normalized = normalize(raw)
        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        val hasTimeCue = containsAnyStem(
            normalized,
            listOf("час", "мин", "врем", "утр", "вечер", "ноч", "дня", "полден")
        )

        val markerTokens = setOf("в", "во", "к", "около", "примерно")
        for (index in tokens.indices) {
            if (!markerTokens.contains(tokens[index])) continue

            val hourParsed = parseNumberAt(tokens, index + 1) ?: continue
            val hour = hourParsed.first
            if (hour !in 0..23) continue

            var pos = hourParsed.second
            while (pos < tokens.size && isTimeGlueToken(tokens[pos])) {
                pos++
            }

            val minuteParsed = parseNumberAt(tokens, pos)
            if (minuteParsed != null && minuteParsed.first in 0..59) {
                return hour to minuteParsed.first
            }

            return hour to 0
        }

        val numbers = extractNumericValuesFromWords(raw)
        if (numbers.size >= 2) {
            val h = numbers[numbers.size - 2]
            val m = numbers[numbers.size - 1]
            if (h in 0..23 && m in 0..59) {
                val hasDateCue = containsMonthWord(normalized) ||
                    containsAnyStem(normalized, listOf("дат", "числ", "месяц", "год"))
                if (hasTimeCue || hasDateCue || numbers.size == 2) {
                    return h to m
                }
            }
        }

        if (hasTimeCue && numbers.size == 1) {
            val hour = numbers.first()
            if (hour in 0..23) return hour to 0
        }

        return null
    }

    private fun isTimeGlueToken(token: String): Boolean {
        return token == "и" ||
            token == "ровно" ||
            token.startsWith("час") ||
            token.startsWith("мин") ||
            token.startsWith("утр") ||
            token.startsWith("вечер") ||
            token.startsWith("ноч") ||
            token.startsWith("дня")
    }

    private fun extractValuePartRaw(raw: String, aliasNormalized: String): String {
        val normalized = removeCommandPrefix(normalize(raw))
        if (normalized == aliasNormalized) return ""
        if (normalized.startsWith("$aliasNormalized ")) {
            return normalized.removePrefix(aliasNormalized).trim()
        }

        val tokens = aliasNormalized.split(" ").filter { it.isNotBlank() }
        if (tokens.isEmpty()) return normalized

        val aliasPattern = tokens.joinToString("\\s+") { Regex.escape(it) }
        val regex = Regex("(?iu)^\\s*$aliasPattern\\s*")
        return normalized.replaceFirst(regex, "").trim()
    }

    private fun startsWithSwitchPrefix(normalized: String): Boolean {
        val text = normalize(normalized)
        return commandPrefixesNormalized.any { prefix ->
            text == prefix || text.startsWith("$prefix ")
        }
    }

    private fun removeCommandPrefix(normalized: String): String {
        val text = normalize(normalized)
        for (prefix in commandPrefixesNormalized) {
            if (text == prefix) return ""
            if (text.startsWith("$prefix ")) {
                return text.removePrefix(prefix).trim()
            }
        }
        return text
    }

    private fun sanitizeRecognizedValue(value: String): String {
        if (value.isBlank()) return ""

        return value
            .replace(Regex("(?iu)\\[?\\s*unk\\s*]?"), " ")
            .replace(Regex("(?iu)\\bunknown\\b"), " ")
            .replace(Regex("(?iu)\\bне\\s*распознано\\b"), " ")
            .replace(Regex("(?iu)\\bнеразборчиво\\b"), " ")
            .replace(Regex("(?iu)\\bшум\\b"), " ")
            .replace(Regex("(?iu)^[-:;,\\s]+"), "")
            .replace(Regex("(?iu)[-:;,\\s]+$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun normalize(text: String): String =
        text
            .lowercase(ruLocale)
            .replace('ё', 'е')
            .replace(Regex("[^a-zа-я0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun isCommandOnlyPhrase(value: String): Boolean {
        val d = detectCommand(normalize(value)) ?: return false
        return d.valueNormalized.isBlank()
    }

    private fun isDateCommand(command: VoiceCommand): Boolean =
        command == VoiceCommand.FILLED_AT || command == VoiceCommand.EVENT_AT

    private fun isDateLikeValue(value: String): Boolean {
        val n = normalize(value)
        if (n == "сейчас" || n == "текущее время") return true
        return parseDateLikeValue(value, null) != null
    }
}
