// FieldScreens.kt
package com.example.diplom.ui.field

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.diplom.data.AppDb
import com.example.diplom.data.MedicalRecordEntity
import com.example.diplom.data.SoldierEntity
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import com.example.diplom.ui.components.IosTimePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class FieldStatus { POGIB, RANEN }

private data class MedicineItem(val name: String, val qty: String)

private val Olive     = Color(0xFF6E6A3B)
private val OliveDark = Color(0xFF5E5A2F)
private val Coral     = Color(0xFFE5625E)
private val PaleRed   = Color(0xFFFFE5E5)
private val PaleGreen = Color(0xFFEAF2E5)

private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

private fun nowText(): String =
    dateTimeFormat.format(Date())

private fun parseDateTime(value: String): Long {
    if (value.isBlank()) return System.currentTimeMillis()
    return try {
        dateTimeFormat.parse(value)?.time ?: System.currentTimeMillis()
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
}

private fun formatDateTime(millis: Long): String =
    dateTimeFormat.format(Date(millis))

private fun hourMinuteFromMillis(millis: Long): Pair<Int, Int> {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    return cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
}

private fun millisWithUpdatedTime(baseMillis: Long, hour: Int, minute: Int): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = baseMillis
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, minute)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun millisWithUpdatedDate(baseMillis: Long, selectedDateMillis: Long): Long {
    val base = Calendar.getInstance()
    base.timeInMillis = baseMillis

    val picked = Calendar.getInstance()
    picked.timeInMillis = selectedDateMillis

    base.set(Calendar.YEAR, picked.get(Calendar.YEAR))
    base.set(Calendar.MONTH, picked.get(Calendar.MONTH))
    base.set(Calendar.DAY_OF_MONTH, picked.get(Calendar.DAY_OF_MONTH))
    return base.timeInMillis
}

private val LocalizationKinds = listOf(
    "голова",
    "шея",
    "грудь",
    "живот",
    "таз",
    "рука",
    "нога",
    "множественные",
    "другое"
)

private val EvacMethods = listOf(
    "самостоятельно",
    "санитарный транспорт",
    "грузовой транспорт",
    "вертолёт",
    "иное"
)

private val EvacPointsForParamedics = listOf(
    "Эвакопункт №1",
    "Эвакопункт №2",
    "Эвакопункт №3",
    "Эвакопункт №4"
)

private data class ReviewableChange(
    val field: VoiceDraftField,
    val value: String
)

private enum class TranscriptTargetField(val label: String) {
    FILLED_AT("Время заполнения"),
    FULL_NAME("ФИО"),
    CALLSIGN("Позывной"),
    TAG_NUMBER("Номер жетона"),
    EVENT_AT("Время события"),
    INJURY_KIND("Вид поражения"),
    DIAGNOSIS("Диагноз"),
    LOCALIZATION_OTHER("Локализация (другое)"),
    EVAC_METHOD("Способ эвакуации"),
    MEDICINE("Лекарство")
}

private fun transcriptTargetsFor(status: FieldStatus): List<TranscriptTargetField> {
    val base = mutableListOf(
        TranscriptTargetField.FILLED_AT,
        TranscriptTargetField.FULL_NAME,
        TranscriptTargetField.CALLSIGN,
        TranscriptTargetField.TAG_NUMBER,
        TranscriptTargetField.EVENT_AT,
        TranscriptTargetField.EVAC_METHOD,
        TranscriptTargetField.MEDICINE
    )
    if (status == FieldStatus.RANEN) {
        base.add(TranscriptTargetField.INJURY_KIND)
        base.add(TranscriptTargetField.DIAGNOSIS)
        base.add(TranscriptTargetField.LOCALIZATION_OTHER)
    }
    return base
}

private fun collectReviewableChanges(
    status: FieldStatus,
    before: FieldFormDraft,
    after: FieldFormDraft
): List<ReviewableChange> {
    val out = ArrayList<ReviewableChange>(8)

    fun addIfChanged(field: VoiceDraftField, oldValue: String, newValue: String) {
        val oldClean = oldValue.trim()
        val newClean = newValue.trim()
        if (oldClean == newClean) return
        if (newClean.isBlank()) return
        out.add(ReviewableChange(field = field, value = newClean))
    }

    addIfChanged(VoiceDraftField.FILLED_AT, before.filledAt, after.filledAt)
    addIfChanged(VoiceDraftField.FULL_NAME, before.fullName, after.fullName)
    addIfChanged(VoiceDraftField.CALLSIGN, before.callsign, after.callsign)
    addIfChanged(VoiceDraftField.TAG_NUMBER, before.tagNumber, after.tagNumber)
    addIfChanged(VoiceDraftField.EVENT_AT, before.eventAt, after.eventAt)
    addIfChanged(VoiceDraftField.EVAC_METHOD, before.evacMethod, after.evacMethod)

    if (status == FieldStatus.RANEN) {
        addIfChanged(VoiceDraftField.INJURY_KIND, before.injuryKind, after.injuryKind)
        addIfChanged(VoiceDraftField.DIAGNOSIS, before.diagnosis, after.diagnosis)
    }

    return out
}

@Composable
fun FieldStartScreen(
    onStatusSelected: (FieldStatus) -> Unit,
    onLogout: () -> Unit = {},
    summaryTitle: String? = null,
    onSummary: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp)
    ) {
        Text(
            text = "выход",
            fontSize = 12.sp,
            color = Color(0xFF9E9E9E),
            modifier = Modifier
                .align(Alignment.TopStart)
                .clickable { onLogout() }
        )

        if (!summaryTitle.isNullOrBlank()) {
            Text(
                text = summaryTitle,
                fontSize = 12.sp,
                color = Color(0xFF6D6D6D),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clickable { onSummary() }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 20.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StatusBigButton(
                text = "ПОГИБ",
                background = Color(0xFFE57373),
                onClick = { onStatusSelected(FieldStatus.POGIB) }
            )
            StatusBigButton(
                text = "РАНЕН",
                background = Color(0xFF81C784),
                onClick = { onStatusSelected(FieldStatus.RANEN) }
            )
        }
    }
}

@Composable
fun FieldVoiceScreen(
    status: FieldStatus,
    initialDraft: FieldFormDraft? = null,
    initialReview: VoiceDraftReview = VoiceDraftReview(),
    initialTranscript: List<String> = emptyList(),
    onBack: () -> Unit,
    onManualClick: (FieldFormDraft, VoiceDraftReview, List<String>) -> Unit
) {
    val context = LocalContext.current

    val heroBackground: Color
    val accentColor: Color
    when (status) {
        FieldStatus.POGIB -> {
            heroBackground = PaleRed
            accentColor = Coral
        }
        FieldStatus.RANEN -> {
            heroBackground = PaleGreen
            accentColor = OliveDark
        }
    }

    val instructionFields = when (status) {
        FieldStatus.POGIB -> """
            время и дата заполнения, ФИО,
            позывной, номер жетона,
            время и дата смерти,
            способ эвакуации,
            препарат и количество.
        """.trimIndent()
        FieldStatus.RANEN -> """
            время и дата заполнения, ФИО,
            позывной, номер жетона,
            время и дата ранения, вид поражения,
            диагноз, локализация,
            способ эвакуации,
            препарат и количество.
        """.trimIndent()
    }

    var draft by remember(status, initialDraft) {
        mutableStateOf(
            initialDraft ?: FieldFormDraft(
                status = status,
                filledAt = nowText(),
                eventAt = nowText()
            )
        )
    }
    var review by remember(status, initialReview) { mutableStateOf(initialReview) }
    var session by remember(status, initialDraft) { mutableStateOf(VoiceSessionState()) }

    var engineState by remember { mutableStateOf(VoskCommandRecognizer.EngineState.PREPARING) }
    var engineMessage by remember { mutableStateOf<String?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var livePartialText by remember { mutableStateOf("") }
    val recognizedLog = remember(status, initialTranscript) {
        mutableStateListOf<String>().apply {
            addAll(initialTranscript.filter { it.isNotBlank() }.takeLast(120))
        }
    }
    val transcriptScrollState = rememberScrollState()

    val recognizer = remember { VoskCommandRecognizer(context, modelAssetPath = "model-ru") }

    DisposableEffect(recognizer) {
        recognizer.prepare { st, msg ->
            engineState = st
            engineMessage = msg
            if (st != VoskCommandRecognizer.EngineState.LISTENING) {
                isListening = false
            }
        }

        onDispose {
            recognizer.shutdown()
        }
    }

    fun beginRecognition() {
        livePartialText = ""

        val mode = if (session.mode == VoiceInputMode.WAIT_COMMAND) {
            VoskCommandRecognizer.ListenMode.COMMAND
        } else {
            VoskCommandRecognizer.ListenMode.FREE
        }
        val started = recognizer.start(
            mode = mode,
            onPartialText = { },
            onUtteranceText = { utterance ->
                val beforeDraft = draft
                val prevApplied = session.lastApplied
                val interpreted = VoiceFormInterpreter.applyUtterance(
                    status = status,
                    utteranceRaw = utterance,
                    draft = draft,
                    session = session
                )
                draft = interpreted.draft
                session = interpreted.session

                val changes = collectReviewableChanges(status, beforeDraft, interpreted.draft)
                for (change in changes) {
                    review = review.recordAttempt(change.field, change.value)
                }

                val appliedNow = interpreted.session.lastApplied
                if (appliedNow.isNotBlank() && appliedNow != prevApplied) {
                    Toast.makeText(context, "Записано: $appliedNow", Toast.LENGTH_SHORT).show()
                }

                if (isListening) {
                    val nextMode = if (session.mode == VoiceInputMode.WAIT_COMMAND) {
                        VoskCommandRecognizer.ListenMode.COMMAND
                    } else {
                        VoskCommandRecognizer.ListenMode.FREE
                    }
                    recognizer.switchMode(nextMode)
                }
            },
            onRawPartialText = { rawPartial ->
                livePartialText = rawPartial
            },
            onRawUtteranceText = { rawFinal ->
                val line = rawFinal
                if (line.isNotBlank()) {
                    val last = recognizedLog.lastOrNull()
                    if (last != line) {
                        recognizedLog.add(line)
                        if (recognizedLog.size > 120) recognizedLog.removeAt(0)
                    }
                }
                livePartialText = ""
            }
        )
        isListening = started
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Нужен доступ к микрофону", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        beginRecognition()
    }

    fun stopAndOpenForm() {
        recognizer.stop()
        isListening = false
        onManualClick(draft, review, recognizedLog.toList())
    }

    fun startListeningOrRequestPermission() {
        if (engineState != VoskCommandRecognizer.EngineState.READY && engineState != VoskCommandRecognizer.EngineState.LISTENING) {
            Toast.makeText(context, "Модель еще загружается", Toast.LENGTH_SHORT).show()
            return
        }

        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            beginRecognition()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(recognizedLog.size, livePartialText) {
        transcriptScrollState.scrollTo(transcriptScrollState.maxValue)
    }

    val micScale by animateFloatAsState(
        targetValue = if (isListening) 1.04f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "voice_mic_scale"
    )

    val pulseTransition = rememberInfiniteTransition(label = "voice_mic_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "voice_pulse_scale_1"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "voice_pulse_alpha_1"
    )
    val pulseScaleSecondary by pulseTransition.animateFloat(
        initialValue = 1.08f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, delayMillis = 320, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "voice_pulse_scale_2"
    )
    val pulseAlphaSecondary by pulseTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, delayMillis = 320, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "voice_pulse_alpha_2"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF8F8F4), Color.White)
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = Color.Black)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, contentDescription = "Люди", tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Люди", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Можно говорить командой \"поле ...\" или сразу по смыслу",
            fontWeight = FontWeight.SemiBold,
            fontSize = 19.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
            color = Color(0xFF3D3D3D)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = session.statusText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
            color = Color(0xFF5F5F5F)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 1.dp,
            shadowElevation = 5.dp
        ) {
            Text(
                text = instructionFields,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                color = Color(0xFF757575)
            )
        }

        if (engineState == VoskCommandRecognizer.EngineState.ERROR && !engineMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = engineMessage ?: "",
                fontSize = 12.sp,
                color = Coral,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .size(300.dp)
                .scale(micScale)
                .clickable {
                    if (isListening) stopAndOpenForm() else startListeningOrRequestPermission()
                },
            contentAlignment = Alignment.Center
        ) {
            if (isListening) {
                Box(
                    modifier = Modifier
                        .size(220.dp * pulseScale)
                        .border(
                            width = 2.dp,
                            color = accentColor.copy(alpha = pulseAlpha),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(220.dp * pulseScaleSecondary)
                        .border(
                            width = 1.dp,
                            color = accentColor.copy(alpha = pulseAlphaSecondary),
                            shape = CircleShape
                        )
                )
            }

            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = if (isListening) 0.20f else 0.12f),
                                heroBackground,
                                Color.White
                            )
                        )
                    )
                    .border(2.dp, accentColor.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(162.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (isListening) {
                                    listOf(accentColor, accentColor.copy(alpha = 0.78f))
                                } else {
                                    listOf(Color.White, heroBackground)
                                }
                            )
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.38f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Rounded.Mic else Icons.Outlined.MicNone,
                        contentDescription = if (isListening) "Остановить запись" else "Начать запись",
                        tint = if (isListening) Color.White else accentColor,
                        modifier = Modifier.size(82.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = {
                if (isListening) {
                    recognizer.stop()
                    isListening = false
                }
                onManualClick(draft, review, recognizedLog.toList())
            },
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White.copy(alpha = 0.9f),
                contentColor = accentColor
            )
        ) {
            Text("Ввести вручную", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    text = "Сырой распознанный текст (live)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF616161)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp, max = 140.dp)
                        .verticalScroll(transcriptScrollState)
                ) {
                    if (recognizedLog.isEmpty() && livePartialText.isBlank()) {
                        Text(
                            text = "Пока пусто. Нажмите микрофон и говорите.",
                            fontSize = 12.sp,
                            color = Color(0xFF9A9A9A)
                        )
                    } else {
                        recognizedLog.forEach { line ->
                            Text(
                                text = line,
                                fontSize = 13.sp,
                                color = Color(0xFF2D2D2D),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        if (livePartialText.isNotBlank()) {
                            Text(
                                text = livePartialText,
                                fontSize = 13.sp,
                                color = accentColor,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FieldFormScreen(
    db: AppDb,
    status: FieldStatus,
    initialDraft: FieldFormDraft? = null,
    initialReview: VoiceDraftReview? = null,
    initialTranscript: List<String> = emptyList(),
    onSaved: (MedicalRecordEntity) -> Unit,
    onBack: (FieldFormDraft, VoiceDraftReview) -> Unit
) {
    val startDraft = remember(status, initialDraft) {
        val base = initialDraft ?: FieldFormDraft(status = status)
        base.copy(
            filledAt = base.filledAt.ifBlank { nowText() },
            eventAt = base.eventAt.ifBlank { nowText() }
        )
    }

    var filledAt by remember(startDraft) { mutableStateOf(startDraft.filledAt) }
    var fullName by remember(startDraft) { mutableStateOf(startDraft.fullName) }
    var callsign by remember(startDraft) { mutableStateOf(startDraft.callsign) }
    var tagNumber by remember(startDraft) { mutableStateOf(startDraft.tagNumber) }
    var eventAt by remember(startDraft) { mutableStateOf(startDraft.eventAt) }

    var review by remember(startDraft, initialReview) {
        mutableStateOf(initialReview ?: VoiceDraftReview())
    }
    val transcriptLines = remember(initialTranscript) {
        initialTranscript.filter { it.isNotBlank() }
    }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showTranscriptDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    var injuryKind by remember(startDraft) { mutableStateOf(startDraft.injuryKind) }
    var diagnosis by remember(startDraft) { mutableStateOf(startDraft.diagnosis) }
    var localizationSelected by remember(startDraft) { mutableStateOf(startDraft.localizationSelected) }
    var localizationOther by remember(startDraft) { mutableStateOf(startDraft.localizationOther) }

    var evacMethod by remember(startDraft) { mutableStateOf(startDraft.evacMethod) }

    var medName by remember { mutableStateOf("") }
    var medQty by remember { mutableStateOf("") }
    val medicines = remember(startDraft) {
        mutableStateListOf<MedicineItem>().apply {
            addAll(startDraft.medicines.map { MedicineItem(it.name, it.qty) })
        }
    }

    fun resolveReview(field: VoiceDraftField, value: String) {
        review = review.resolve(field, value)
    }

    fun snapshotDraft(): FieldFormDraft {
        return FieldFormDraft(
            status = status,
            filledAt = filledAt,
            fullName = fullName,
            callsign = callsign,
            tagNumber = tagNumber,
            eventAt = eventAt,
            injuryKind = injuryKind,
            diagnosis = diagnosis,
            localizationSelected = localizationSelected,
            localizationOther = localizationOther,
            evacMethod = evacMethod,
            medicines = medicines.map { MedicineItemDraft(it.name, it.qty) }
        )
    }

    fun applyByVoiceCommand(command: VoiceCommand, text: String): FieldFormDraft {
        return VoiceFormInterpreter.applyUtterance(
            status = status,
            utteranceRaw = text,
            draft = snapshotDraft(),
            session = VoiceSessionState(
                mode = VoiceInputMode.WAIT_VALUE,
                activeCommand = command
            )
        ).draft
    }

    fun applyTranscriptToField(target: TranscriptTargetField, rawText: String) {
        val text = rawText.trim()
        if (text.isBlank()) return

        when (target) {
            TranscriptTargetField.FILLED_AT -> {
                val parsed = applyByVoiceCommand(VoiceCommand.FILLED_AT, text)
                filledAt = parsed.filledAt
                resolveReview(VoiceDraftField.FILLED_AT, filledAt)
            }

            TranscriptTargetField.FULL_NAME -> {
                val parsed = applyByVoiceCommand(VoiceCommand.FULL_NAME, text)
                fullName = parsed.fullName
                resolveReview(VoiceDraftField.FULL_NAME, fullName)
            }

            TranscriptTargetField.CALLSIGN -> {
                val parsed = applyByVoiceCommand(VoiceCommand.CALLSIGN, text)
                callsign = parsed.callsign
                resolveReview(VoiceDraftField.CALLSIGN, callsign)
            }

            TranscriptTargetField.TAG_NUMBER -> {
                val parsed = applyByVoiceCommand(VoiceCommand.TAG_NUMBER, text)
                tagNumber = parsed.tagNumber
                resolveReview(VoiceDraftField.TAG_NUMBER, tagNumber)
            }

            TranscriptTargetField.EVENT_AT -> {
                val parsed = applyByVoiceCommand(VoiceCommand.EVENT_AT, text)
                eventAt = parsed.eventAt
                resolveReview(VoiceDraftField.EVENT_AT, eventAt)
            }

            TranscriptTargetField.INJURY_KIND -> {
                val parsed = applyByVoiceCommand(VoiceCommand.INJURY_KIND, text)
                injuryKind = parsed.injuryKind
                resolveReview(VoiceDraftField.INJURY_KIND, injuryKind)
            }

            TranscriptTargetField.DIAGNOSIS -> {
                val parsed = applyByVoiceCommand(VoiceCommand.DIAGNOSIS, text)
                diagnosis = parsed.diagnosis
                resolveReview(VoiceDraftField.DIAGNOSIS, diagnosis)
            }

            TranscriptTargetField.LOCALIZATION_OTHER -> {
                localizationSelected = localizationSelected + "другое"
                localizationOther = text
            }

            TranscriptTargetField.EVAC_METHOD -> {
                val parsed = applyByVoiceCommand(VoiceCommand.EVAC_METHOD, text)
                evacMethod = parsed.evacMethod
                resolveReview(VoiceDraftField.EVAC_METHOD, evacMethod)
            }

            TranscriptTargetField.MEDICINE -> {
                val parsed = applyByVoiceCommand(VoiceCommand.MEDICINE, text)
                val last = parsed.medicines.lastOrNull()
                if (last != null) {
                    medicines.add(MedicineItem(last.name, last.qty))
                } else {
                    medicines.add(MedicineItem(name = text, qty = "-"))
                }
            }
        }
    }

    fun resetAllFields() {
        filledAt = ""
        fullName = ""
        callsign = ""
        tagNumber = ""
        eventAt = ""
        injuryKind = ""
        diagnosis = ""
        localizationSelected = emptySet()
        localizationOther = ""
        evacMethod = ""
        medName = ""
        medQty = ""
        medicines.clear()
        review = VoiceDraftReview()
    }

    fun navigateBackWithState() {
        onBack(snapshotDraft(), review)
    }

    BackHandler {
        navigateBackWithState()
    }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val dao = db.appDao()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val saveStartColor = if (status == FieldStatus.POGIB) Color(0xFFE05757) else Olive
        val saveEndColor = if (status == FieldStatus.POGIB) Color(0xFFBA2F2F) else OliveDark

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { navigateBackWithState() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = Color.Black)
                }
                Text(
                    text = if (status == FieldStatus.RANEN) "Ранен" else "Погиб",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Box {
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню", tint = Color.Black)
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Распознанный текст") },
                            onClick = {
                                showOverflowMenu = false
                                showTranscriptDialog = true
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Сбросить все поля") },
                            onClick = {
                                showOverflowMenu = false
                                showResetConfirm = true
                            }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (review.unresolvedFields.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFF4B3)
                    ) {
                        Text(
                            text = "Желтым помечены спорные поля. Нажмите на поле или иконку глаза, чтобы выбрать вариант.",
                            fontSize = 12.sp,
                            color = Color(0xFF6A5400),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                TimeField(
                    label = "Время заполнения",
                    value = filledAt,
                    onValueChange = {
                        filledAt = it
                        resolveReview(VoiceDraftField.FILLED_AT, it)
                    },
                    onNow = {
                        val now = nowText()
                        filledAt = now
                        resolveReview(VoiceDraftField.FILLED_AT, now)
                    },
                    highlightConflict = review.isUnresolved(VoiceDraftField.FILLED_AT),
                    conflictOptions = review.variantsFor(VoiceDraftField.FILLED_AT),
                    onPickConflictOption = { picked ->
                        filledAt = picked
                        resolveReview(VoiceDraftField.FILLED_AT, picked)
                    }
                )

                LabeledClearableField(
                    label = "ФИО",
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        resolveReview(VoiceDraftField.FULL_NAME, it)
                    },
                    placeholder = "Иванов Иван Иванович",
                    highlightConflict = review.isUnresolved(VoiceDraftField.FULL_NAME),
                    conflictOptions = review.variantsFor(VoiceDraftField.FULL_NAME),
                    onPickConflictOption = { picked ->
                        fullName = picked
                        resolveReview(VoiceDraftField.FULL_NAME, picked)
                    }
                )

                LabeledClearableField(
                    label = "Позывной",
                    value = callsign,
                    onValueChange = {
                        callsign = it
                        resolveReview(VoiceDraftField.CALLSIGN, it)
                    },
                    placeholder = "Стальной",
                    highlightConflict = review.isUnresolved(VoiceDraftField.CALLSIGN),
                    conflictOptions = review.variantsFor(VoiceDraftField.CALLSIGN),
                    onPickConflictOption = { picked ->
                        callsign = picked
                        resolveReview(VoiceDraftField.CALLSIGN, picked)
                    }
                )

                LabeledClearableField(
                    label = "Номер жетона / личный №",
                    value = tagNumber,
                    onValueChange = {
                        tagNumber = it
                        resolveReview(VoiceDraftField.TAG_NUMBER, it)
                    },
                    placeholder = "A-19483",
                    highlightConflict = review.isUnresolved(VoiceDraftField.TAG_NUMBER),
                    conflictOptions = review.variantsFor(VoiceDraftField.TAG_NUMBER),
                    onPickConflictOption = { picked ->
                        tagNumber = picked
                        resolveReview(VoiceDraftField.TAG_NUMBER, picked)
                    }
                )

                TimeField(
                    label = if (status == FieldStatus.POGIB) "Время смерти" else "Время ранения",
                    value = eventAt,
                    onValueChange = {
                        eventAt = it
                        resolveReview(VoiceDraftField.EVENT_AT, it)
                    },
                    onNow = {
                        val now = nowText()
                        eventAt = now
                        resolveReview(VoiceDraftField.EVENT_AT, now)
                    },
                    highlightConflict = review.isUnresolved(VoiceDraftField.EVENT_AT),
                    conflictOptions = review.variantsFor(VoiceDraftField.EVENT_AT),
                    onPickConflictOption = { picked ->
                        eventAt = picked
                        resolveReview(VoiceDraftField.EVENT_AT, picked)
                    }
                )

                if (status == FieldStatus.RANEN) {
                    LabeledClearableField(
                        label = "Вид поражения",
                        value = injuryKind,
                        onValueChange = {
                            injuryKind = it
                            resolveReview(VoiceDraftField.INJURY_KIND, it)
                        },
                        placeholder = "огнестрел / ожог / хим / бак / радиация / прочее / заболевание / отморожение / псих",
                        highlightConflict = review.isUnresolved(VoiceDraftField.INJURY_KIND),
                        conflictOptions = review.variantsFor(VoiceDraftField.INJURY_KIND),
                        onPickConflictOption = { picked ->
                            injuryKind = picked
                            resolveReview(VoiceDraftField.INJURY_KIND, picked)
                        }
                    )

                    LabeledMultilineField(
                        label = "Диагноз",
                        value = diagnosis,
                        onValueChange = {
                            diagnosis = it
                            resolveReview(VoiceDraftField.DIAGNOSIS, it)
                        },
                        placeholder = "Кратко, по сути",
                        highlightConflict = review.isUnresolved(VoiceDraftField.DIAGNOSIS),
                        conflictOptions = review.variantsFor(VoiceDraftField.DIAGNOSIS),
                        onPickConflictOption = { picked ->
                            diagnosis = picked
                            resolveReview(VoiceDraftField.DIAGNOSIS, picked)
                        }
                    )

                    ChipsMulti(
                        label = "Локализация",
                        options = LocalizationKinds,
                        selected = localizationSelected,
                        onToggle = { opt ->
                            localizationSelected =
                                if (localizationSelected.contains(opt)) localizationSelected - opt else localizationSelected + opt
                        }
                    )

                    if (localizationSelected.contains("другое")) {
                        LabeledClearableField(
                            "Локализация: другое",
                            localizationOther,
                            { localizationOther = it },
                            "Уточнение"
                        )
                    }
                }

                ChipsSingle(
                    label = "Способ эвакуации",
                    options = EvacMethods,
                    selected = evacMethod,
                    onSelect = {
                        evacMethod = it
                        resolveReview(VoiceDraftField.EVAC_METHOD, it)
                    },
                    highlightConflict = review.isUnresolved(VoiceDraftField.EVAC_METHOD),
                    conflictOptions = review.variantsFor(VoiceDraftField.EVAC_METHOD),
                    onPickConflictOption = { picked ->
                        evacMethod = picked
                        resolveReview(VoiceDraftField.EVAC_METHOD, picked)
                    }
                )

                Text("Лекарства", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF212121))
                LabeledClearableField(
                    label = "Препарат",
                    value = medName,
                    onValueChange = { medName = it },
                    placeholder = "Напр. Кеторол"
                )
                LabeledClearableField(
                    label = "Количество",
                    value = medQty,
                    onValueChange = { medQty = it },
                    placeholder = "Напр. 2 амп / 10 мл / 5 кап"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (medName.isBlank() && medQty.isBlank()) return@OutlinedButton
                            medicines.add(
                                MedicineItem(
                                    name = medName.trim().ifBlank { "-" },
                                    qty = medQty.trim().ifBlank { "-" }
                                )
                            )
                            medName = ""
                            medQty = ""
                        },
                        modifier = Modifier.height(40.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Добавить", fontSize = 14.sp)
                    }
                }

                if (medicines.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        medicines.forEachIndexed { idx, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF3F3F3), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.name} — ${item.qty}",
                                    fontSize = 14.sp,
                                    color = Color.Black,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { medicines.removeAt(idx) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Удалить", tint = Color(0xFF9E9E9E))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        if (showTranscriptDialog) {
            TranscriptAssignDialog(
                status = status,
                transcriptLines = transcriptLines,
                onDismiss = { showTranscriptDialog = false },
                onAssign = { target, selectedText ->
                    applyTranscriptToField(target, selectedText)
                }
            )
        }

        if (showResetConfirm) {
            AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                title = { Text("Сбросить все поля?") },
                text = { Text("Все введенные данные будут очищены. Вернуть их нельзя.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showResetConfirm = false
                            resetAllFields()
                        }
                    ) {
                        Text("Сбросить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirm = false }) {
                        Text("Отмена")
                    }
                }
            )
        }

        Button(
            onClick = {
                if (medName.isNotBlank() || medQty.isNotBlank()) {
                    medicines.add(
                        MedicineItem(
                            name = medName.trim().ifBlank { "-" },
                            qty = medQty.trim().ifBlank { "-" }
                        )
                    )
                    medName = ""
                    medQty = ""
                }
                scope.launch {
                    val tag = tagNumber.ifBlank { null }
                    val existingSoldier = if (tag != null) dao.getSoldierByTagNumber(tag) else null
                    val soldierId = dao.upsertSoldier(
                        SoldierEntity(
                            id = existingSoldier?.id ?: 0L,
                            fullName = fullName.ifBlank { "Неизвестный" },
                            callsign = callsign.ifBlank { null },
                            division = existingSoldier?.division,
                            gender = existingSoldier?.gender,
                            ageYears = existingSoldier?.ageYears,
                            weightKg = existingSoldier?.weightKg,
                            tagNumber = tag
                        )
                    )

                    val json = JSONObject()
                        .put("syncId", UUID.randomUUID().toString())
                        .put("syncUpdatedAt", System.currentTimeMillis())
                        .put("filledAt", filledAt)
                        .put("fullName", fullName)
                        .put("callsign", callsign)
                        .put("tagNumber", tagNumber)
                        .put("evacMethod", evacMethod)

                    val medsArr = JSONArray()
                    medicines.forEach { m ->
                        medsArr.put(
                            JSONObject()
                                .put("name", m.name)
                                .put("qty", m.qty)
                        )
                    }
                    if (medsArr.length() > 0) {
                        json.put("medicines", medsArr)
                    }

                    if (status == FieldStatus.POGIB) {
                        json.put("deathAt", eventAt)
                    } else {
                        json.put("injuryAt", eventAt)
                        json.put("injuryKind", injuryKind)
                        json.put("diagnosis", diagnosis)
                        json.put("localization", JSONArray(localizationSelected.toList()))
                        json.put("localizationOther", localizationOther)
                    }

                    val record = MedicalRecordEntity(
                        soldierId = soldierId,
                        status = if (status == FieldStatus.RANEN) "RANEN" else "POGIB",
                        location = null,
                        injuryType = if (status == FieldStatus.RANEN) injuryKind.ifBlank { null } else null,
                        medicine = if (medsArr.length() > 0) medsArr.toString() else null,
                        destination = null,
                        stage = "FIELD",
                        rawText = json.toString()
                    )

                    val recordId = dao.insertRecord(record)
                    onSaved(record.copy(id = recordId))
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .height(58.dp),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(0.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 7.dp,
                pressedElevation = 2.dp
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(saveStartColor, saveEndColor)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Сохранить и отправить",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TranscriptAssignDialog(
    status: FieldStatus,
    transcriptLines: List<String>,
    onDismiss: () -> Unit,
    onAssign: (TranscriptTargetField, String) -> Unit
) {
    val transcriptText = remember(transcriptLines) {
        transcriptLines
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n")
    }

    var transcriptValue by remember(transcriptText) {
        mutableStateOf(TextFieldValue(transcriptText))
    }
    var selectedField by remember(status) {
        mutableStateOf(transcriptTargetsFor(status).first())
    }
    var showFieldMenu by remember { mutableStateOf(false) }

    val selectedText = remember(transcriptValue) {
        val text = transcriptValue.text
        val start = transcriptValue.selection.start.coerceIn(0, text.length)
        val end = transcriptValue.selection.end.coerceIn(0, text.length)
        if (start == end) {
            ""
        } else {
            text.substring(minOf(start, end), maxOf(start, end)).trim()
        }
    }

    val targets = remember(status) { transcriptTargetsFor(status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Распознанный текст") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (transcriptText.isBlank()) {
                    Text(
                        text = "Лайв текст пока пуст. Вернитесь в голосовой экран и продиктуйте данные.",
                        fontSize = 13.sp,
                        color = Color(0xFF666666)
                    )
                } else {
                    Text(
                        text = "Выделите фрагмент и назначьте его в поле.",
                        fontSize = 13.sp,
                        color = Color(0xFF666666)
                    )

                    OutlinedTextField(
                        value = transcriptValue,
                        onValueChange = { transcriptValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 300.dp),
                        readOnly = true,
                        textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF202020)),
                        placeholder = { Text("Нет распознанного текста") }
                    )

                    Text(
                        text = if (selectedText.isBlank()) {
                            "Ничего не выделено"
                        } else {
                            "Выделено: $selectedText"
                        },
                        fontSize = 12.sp,
                        color = if (selectedText.isBlank()) Color(0xFF999999) else Color(0xFF3D3D3D)
                    )

                    Box {
                        OutlinedButton(onClick = { showFieldMenu = true }) {
                            Text("Поле: ${selectedField.label}")
                        }

                        DropdownMenu(
                            expanded = showFieldMenu,
                            onDismissRequest = { showFieldMenu = false }
                        ) {
                            targets.forEach { target ->
                                DropdownMenuItem(
                                    text = { Text(target.label) },
                                    onClick = {
                                        selectedField = target
                                        showFieldMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { onAssign(selectedField, selectedText) },
                        enabled = selectedText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Применить выделенный текст")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
private fun ConflictOptionsDialog(
    title: String,
    options: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { option ->
                    Text(
                        text = option,
                        fontSize = 14.sp,
                        color = Color(0xFF1F1F1F),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(option) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
fun EvacPointSelectScreen(
    db: AppDb,
    record: MedicalRecordEntity,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val dao = db.appDao()
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<String?>(record.destination) }

    val filtered = remember(query) {
        if (query.isBlank()) EvacPointsForParamedics
        else EvacPointsForParamedics.filter { it.contains(query, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = Color.Black)
                }
                Text("Выбор эвакопункта", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                Spacer(modifier = Modifier.width(40.dp))
            }

            LabeledClearableField("Поиск", query, { query = it }, "Введите название")

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp)
            ) {
                filtered.forEach { name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == name, onClick = { selected = name })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = name, fontSize = 16.sp, color = Color.Black, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        Button(
            onClick = {
                val dest = selected ?: return@Button
                scope.launch {
                    dao.insertRecord(record.copy(destination = dest, stage = "EVAC_POINT"))
                    onDone()
                }
            },
            enabled = selected != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Olive)
        ) {
            Text("Продолжить", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun TimeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onNow: () -> Unit,
    highlightConflict: Boolean = false,
    conflictOptions: List<String> = emptyList(),
    onPickConflictOption: ((String) -> Unit)? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showConflictDialog by remember(label, conflictOptions) { mutableStateOf(false) }

    val initialMillis = remember(value) { parseDateTime(value) }
    var workingMillis by remember(initialMillis) { mutableStateOf(initialMillis) }
    val conflictBg = Color(0xFFFFF4B3)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (highlightConflict) Color(0xFF7A6200) else Color(0xFF212121)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(if (highlightConflict) conflictBg else Color(0xFFF3F3F3), RoundedCornerShape(8.dp))
                    .clickable {
                        if (highlightConflict && conflictOptions.isNotEmpty()) {
                            showConflictDialog = true
                        } else {
                            workingMillis = initialMillis
                            showDatePicker = true
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value.ifBlank { "Нажмите для выбора даты и времени" },
                        fontSize = 14.sp,
                        color = if (value.isBlank()) Color(0xFF9E9E9E) else Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (highlightConflict && conflictOptions.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        workingMillis = initialMillis
                        showDatePicker = true
                    },
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Календ.", fontSize = 13.sp)
                }
            }

            OutlinedButton(
                onClick = onNow,
                modifier = Modifier.height(40.dp),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Сейчас", fontSize = 14.sp) }
        }
    }

    if (showConflictDialog && conflictOptions.isNotEmpty()) {
        ConflictOptionsDialog(
            title = label,
            options = conflictOptions,
            onDismiss = { showConflictDialog = false },
            onSelect = { option ->
                onValueChange(option)
                onPickConflictOption?.invoke(option)
                showConflictDialog = false
            }
        )
    }

    if (showDatePicker) {
        DatePickerWithTimeFlow(
            initialMillis = workingMillis,
            onDismiss = { showDatePicker = false },
            onDatePicked = { updatedMillis ->
                workingMillis = updatedMillis
                showDatePicker = false
                showTimePicker = true
            }
        )
    }

    if (showTimePicker) {
        val (h, m) = remember(workingMillis) { hourMinuteFromMillis(workingMillis) }
        IosTimePickerDialog(
            initialHour = h,
            initialMinute = m,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                onValueChange(formatDateTime(millisWithUpdatedTime(workingMillis, hour, minute)))
                showTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerWithTimeFlow(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onDatePicked: (Long) -> Unit
) {
    val dateState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val picked = dateState.selectedDateMillis
                    val nextMillis = if (picked != null) millisWithUpdatedDate(initialMillis, picked) else initialMillis
                    onDatePicked(nextMillis)
                }
            ) { Text("Далее") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    ) {
        DatePicker(state = dateState)
    }
}

@Composable
private fun ChipsSingle(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    highlightConflict: Boolean = false,
    conflictOptions: List<String> = emptyList(),
    onPickConflictOption: ((String) -> Unit)? = null
) {
    var showConflictDialog by remember(label, conflictOptions) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (highlightConflict) Color(0xFF7A6200) else Color(0xFF212121)
            )

            if (highlightConflict && conflictOptions.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "варианты",
                    fontSize = 12.sp,
                    color = Color(0xFF7A6200),
                    modifier = Modifier.clickable { showConflictDialog = true }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { opt ->
                FilterChip(
                    selected = selected == opt,
                    onClick = { onSelect(opt) },
                    label = { Text(opt) }
                )
            }
        }

        if (showConflictDialog && conflictOptions.isNotEmpty()) {
            ConflictOptionsDialog(
                title = label,
                options = conflictOptions,
                onDismiss = { showConflictDialog = false },
                onSelect = { option ->
                    onSelect(option)
                    onPickConflictOption?.invoke(option)
                    showConflictDialog = false
                }
            )
        }
    }
}

@Composable
private fun ChipsMulti(
    label: String,
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF212121))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { opt ->
                FilterChip(
                    selected = selected.contains(opt),
                    onClick = { onToggle(opt) },
                    label = { Text(opt) }
                )
            }
        }
    }
}

@Composable
private fun LabeledMultilineField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    highlightConflict: Boolean = false,
    conflictOptions: List<String> = emptyList(),
    onPickConflictOption: ((String) -> Unit)? = null
) {
    var showConflictDialog by remember(label, conflictOptions) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (highlightConflict) Color(0xFF7A6200) else Color(0xFF212121)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .background(if (highlightConflict) Color(0xFFFFF4B3) else Color(0xFFF3F3F3), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (highlightConflict && conflictOptions.isNotEmpty()) {
                    IconButton(
                        onClick = { showConflictDialog = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Варианты",
                            tint = Color(0xFF7A6200)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    singleLine = false,
                    textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty() && placeholder.isNotEmpty()) {
                                Text(placeholder, fontSize = 14.sp, color = Color(0xFFBDBDBD))
                            }
                            inner()
                        }
                    }
                )
            }
        }

        if (showConflictDialog && conflictOptions.isNotEmpty()) {
            ConflictOptionsDialog(
                title = label,
                options = conflictOptions,
                onDismiss = { showConflictDialog = false },
                onSelect = { option ->
                    onValueChange(option)
                    onPickConflictOption?.invoke(option)
                    showConflictDialog = false
                }
            )
        }
    }
}

@Composable
private fun StatusBigButton(
    text: String,
    background: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        colors = ButtonDefaults.buttonColors(containerColor = background, contentColor = Color.Black),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(vertical = 8.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp)
    ) {
        Text(text = text, fontSize = 48.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LabeledClearableField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    labelColor: Color = Color(0xFF212121),
    highlightConflict: Boolean = false,
    conflictOptions: List<String> = emptyList(),
    onPickConflictOption: ((String) -> Unit)? = null
) {
    var showConflictDialog by remember(label, conflictOptions) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (highlightConflict) Color(0xFF7A6200) else labelColor
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(if (highlightConflict) Color(0xFFFFF4B3) else Color(0xFFF3F3F3), RoundedCornerShape(8.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (highlightConflict && conflictOptions.isNotEmpty()) {
                    IconButton(
                        onClick = { showConflictDialog = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Варианты",
                            tint = Color(0xFF7A6200)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                    keyboardOptions = keyboardOptions,
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty() && placeholder.isNotEmpty()) {
                                Text(placeholder, fontSize = 14.sp, color = Color(0xFFBDBDBD))
                            }
                            inner()
                        }
                    }
                )

                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Очистить", tint = Color(0xFF9E9E9E))
                    }
                }
            }
        }

        if (showConflictDialog && conflictOptions.isNotEmpty()) {
            ConflictOptionsDialog(
                title = label,
                options = conflictOptions,
                onDismiss = { showConflictDialog = false },
                onSelect = { option ->
                    onValueChange(option)
                    onPickConflictOption?.invoke(option)
                    showConflictDialog = false
                }
            )
        }
    }
}
