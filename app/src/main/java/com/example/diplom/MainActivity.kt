package com.example.diplom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import com.example.diplom.auth.AuthStorage
import com.example.diplom.auth.UserProfile
import com.example.diplom.auth.UserRole
import com.example.diplom.access.AccessControl
import com.example.diplom.access.InboxKind
import com.example.diplom.access.EvacHospitalBindingStorage
import com.example.diplom.data.AppDb
import com.example.diplom.data.MedicalRecordEntity
import com.example.diplom.ui.field.FieldFormDraft
import com.example.diplom.ui.field.FieldFormScreen
import com.example.diplom.ui.field.FieldStartScreen
import com.example.diplom.ui.field.FieldStatus
import com.example.diplom.ui.field.FieldVoiceScreen
import com.example.diplom.ui.auth.AuthScreen
import com.example.diplom.ui.theme.DiplomTheme
import com.example.diplom.pdf.Form100Mapper
import com.example.diplom.pdf.Form100PdfGenerator
import com.example.diplom.ui.summary.SummaryScreen
import com.example.diplom.ui.fund.FundHomeScreen
import com.example.diplom.ui.fund.FundKind
import com.example.diplom.ui.fund.FundMedicineTableScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    sealed class FieldScreenState {
        object Auth : FieldScreenState()
        object FundHome : FieldScreenState()
        data class FundTable(val kind: FundKind, val locationName: String) : FieldScreenState()
        object Choice : FieldScreenState()
        data class Voice(val status: FieldStatus, val draft: FieldFormDraft? = null) : FieldScreenState()
        data class ManualForm(val status: FieldStatus, val draft: FieldFormDraft? = null) : FieldScreenState()
        data class Summary(val inboxKind: InboxKind, val locationName: String) : FieldScreenState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DiplomTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val db = remember { AppDb.get(applicationContext) }

                    val ctx = LocalContext.current
                    var currentUser by remember {
                        mutableStateOf<UserProfile?>(AuthStorage.currentUser(ctx))
                    }

                    var screenState by remember {
                        mutableStateOf<FieldScreenState>(
                            if (currentUser == null) {
                                FieldScreenState.Auth
                            } else if (currentUser?.role == UserRole.FUND) {
                                FieldScreenState.FundHome
                            } else {
                                FieldScreenState.Choice
                            }
                        )
                    }

                    val scope = rememberCoroutineScope()

                    when (val state = screenState) {
                        is FieldScreenState.Auth -> {
                            AuthScreen(
                                onAuthed = {
                                    currentUser = AuthStorage.currentUser(ctx)
                                    screenState = if (currentUser?.role == UserRole.FUND) {
                                        FieldScreenState.FundHome
                                    } else {
                                        FieldScreenState.Choice
                                    }
                                }
                            )
                        }

                        is FieldScreenState.FundHome -> {
                            FundHomeScreen(
                                onLogout = {
                                    AuthStorage.logout(ctx)
                                    currentUser = null
                                    screenState = FieldScreenState.Auth
                                },
                                onOpenTable = { kind, name ->
                                    screenState = FieldScreenState.FundTable(kind, name)
                                }
                            )
                        }

                        is FieldScreenState.FundTable -> {
                            FundMedicineTableScreen(
                                db = db,
                                kind = state.kind,
                                locationName = state.locationName,
                                onBack = { screenState = FieldScreenState.FundHome },
                                onLogout = {
                                    AuthStorage.logout(ctx)
                                    currentUser = null
                                    screenState = FieldScreenState.Auth
                                }
                            )
                        }

                        is FieldScreenState.Choice -> {
                            val user = currentUser
                            val canSummary = user != null && AccessControl.canSeeSummary(user.role)
                            val summaryTitle = if (canSummary) AccessControl.summaryButtonTitle(user!!.role) else null

                            FieldStartScreen(
                                onStatusSelected = { status ->
                                    screenState = FieldScreenState.Voice(status = status)
                                },
                                onLogout = {
                                    AuthStorage.logout(ctx)
                                    currentUser = null
                                    screenState = FieldScreenState.Auth
                                },
                                summaryTitle = summaryTitle,
                                onSummary = {
                                    val u = currentUser ?: return@FieldStartScreen
                                    val kind = AccessControl.inboxKind(u.role) ?: return@FieldStartScreen
                                    val loc = AccessControl.locationName(u) ?: return@FieldStartScreen
                                    screenState = FieldScreenState.Summary(kind, loc)
                                }
                            )
                        }

                        is FieldScreenState.Voice -> {
                            FieldVoiceScreen(
                                status = state.status,
                                initialDraft = state.draft,
                                onBack = { screenState = FieldScreenState.Choice },
                                onManualClick = { draft ->
                                    screenState = FieldScreenState.ManualForm(
                                        status = state.status,
                                        draft = draft
                                    )
                                }
                            )
                        }

                        is FieldScreenState.ManualForm -> {
                            FieldFormScreen(
                                db = db,
                                status = state.status,
                                initialDraft = state.draft,
                                onSaved = { savedRecord ->
                                    val user = currentUser
                                    if (user == null) {
                                        screenState = FieldScreenState.Auth
                                        return@FieldFormScreen
                                    }

                                    when (user.role) {
                                        UserRole.FELDSHER -> {
                                            val evacPoint = user.evacPoint ?: ""

                                            scope.launch(Dispatchers.IO) {
                                                val boundHospital = EvacHospitalBindingStorage
                                                    .hospitalForEvacPoint(ctx, evacPoint)
                                                    ?.takeIf { it.isNotBlank() }

                                                val enriched = enrichRawText(
                                                    rawText = savedRecord.rawText,
                                                    user = user,
                                                    evacPoint = evacPoint,
                                                    boundHospital = boundHospital,
                                                    fromEvacPoint = evacPoint
                                                )
                                                val updated = savedRecord.copy(
                                                    destination = evacPoint.ifBlank { null },
                                                    stage = "EVAC_POINT",
                                                    rawText = enriched
                                                )

                                                db.appDao().insertRecord(updated)

                                                if (evacPoint.isNotBlank()) {
                                                    val data = Form100Mapper.fromRawText(
                                                        status = updated.status,
                                                        rawText = updated.rawText,
                                                        doctorFio = user.fio
                                                    )
                                                    Form100PdfGenerator.generateToEvacInbox(
                                                        ctx = ctx,
                                                        evacPoint = evacPoint,
                                                        prefix = "Ф",
                                                        data = data
                                                    )

                                                    if (boundHospital != null) {
                                                        Form100PdfGenerator.generateToHospitalInbox(
                                                            ctx = ctx,
                                                            hospital = boundHospital,
                                                            evacPoint = evacPoint,
                                                            prefix = "Ф",
                                                            data = data
                                                        )
                                                    }
                                                }
                                            }

                                            screenState = FieldScreenState.Choice
                                        }

                                        UserRole.EVAC_DOCTOR -> {
                                            val evacPoint = user.evacPoint ?: ""
                                            val hospital = user.hospital
                                                ?.takeIf { it.isNotBlank() }
                                                ?: EvacHospitalBindingStorage.hospitalForEvacPoint(ctx, evacPoint)
                                                ?: ""

                                            scope.launch(Dispatchers.IO) {
                                                val enriched = enrichRawText(
                                                    rawText = savedRecord.rawText,
                                                    user = user,
                                                    evacPoint = evacPoint,
                                                    boundHospital = hospital.takeIf { it.isNotBlank() },
                                                    fromEvacPoint = evacPoint
                                                )
                                                val updated = savedRecord.copy(
                                                    destination = hospital.ifBlank { null },
                                                    stage = "HOSPITAL",
                                                    rawText = enriched
                                                )

                                                db.appDao().insertRecord(updated)
                                                if (hospital.isNotBlank()) {
                                                    val data = Form100Mapper.fromRawText(
                                                        status = updated.status,
                                                        rawText = updated.rawText,
                                                        doctorFio = user.fio
                                                    )
                                                    Form100PdfGenerator.generateToHospitalInbox(
                                                        ctx = ctx,
                                                        hospital = hospital,
                                                        evacPoint = evacPoint.ifBlank { "-" },
                                                        prefix = "Э",
                                                        data = data
                                                    )
                                                }
                                            }

                                            screenState = FieldScreenState.Choice
                                        }

                                        UserRole.HOSPITAL_DOCTOR -> {
                                            val hospital = user.hospital ?: ""
                                            scope.launch(Dispatchers.IO) {
                                                val enriched = enrichRawText(
                                                    rawText = savedRecord.rawText,
                                                    user = user,
                                                    evacPoint = null,
                                                    boundHospital = null,
                                                    fromEvacPoint = null
                                                )
                                                val updated = savedRecord.copy(
                                                    destination = hospital.ifBlank { null },
                                                    stage = "HOSPITAL",
                                                    rawText = enriched
                                                )

                                                db.appDao().insertRecord(updated)
                                                if (hospital.isNotBlank()) {
                                                    val data = Form100Mapper.fromRawText(
                                                        status = updated.status,
                                                        rawText = updated.rawText,
                                                        doctorFio = user.fio
                                                    )
                                                    Form100PdfGenerator.generateToHospitalInbox(
                                                        ctx = ctx,
                                                        hospital = hospital,
                                                        evacPoint = "-",
                                                        prefix = "Э",
                                                        data = data
                                                    )
                                                }
                                            }
                                            screenState = FieldScreenState.Choice
                                        }

                                        UserRole.FUND -> {
                                            // Фонд не заполняет формы
                                            screenState = FieldScreenState.Choice
                                        }
                                    }
                                },
                                onBack = {
                                    screenState = FieldScreenState.Voice(
                                        status = state.status,
                                        draft = state.draft
                                    )
                                }
                            )
                        }

                        is FieldScreenState.Summary -> {
                            val user = currentUser
                            val canForward = false
                            val forwardHospitals = emptyList<String>()

                            SummaryScreen(
                                title = if (user != null) AccessControl.summaryButtonTitle(user.role) else "Сводка",
                                inboxKind = state.inboxKind,
                                locationName = state.locationName,
                                canForwardToHospital = canForward,
                                forwardHospitals = forwardHospitals,
                                onBack = { screenState = FieldScreenState.Choice },
                                onOpenPdf = { pdfFile ->
                                    openPdf(ctx, pdfFile)
                                },
                                onForwardToHospital = { pdfFile, hospital ->
                                    // forward disabled: each evac point is bound to one hospital
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun openPdf(ctx: android.content.Context, file: java.io.File) {
    val uri = androidx.core.content.FileProvider.getUriForFile(
        ctx,
        "${ctx.packageName}.fileprovider",
        file
    )
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        ctx.startActivity(intent)
    } catch (_: android.content.ActivityNotFoundException) {
        android.widget.Toast
            .makeText(ctx, "Нет приложения для открытия PDF", android.widget.Toast.LENGTH_LONG)
            .show()
    }
}

private fun enrichRawText(
    rawText: String?,
    user: UserProfile,
    evacPoint: String?,
    boundHospital: String?,
    fromEvacPoint: String?
): String {
    val o = try {
        if (rawText.isNullOrBlank()) JSONObject() else JSONObject(rawText)
    } catch (_: Exception) {
        JSONObject()
    }

    o.put("doctorFio", user.fio)
    o.put("authorRole", user.role.name)
    o.put("authorFio", user.fio)

    val ep = evacPoint?.trim().orEmpty()
    if (ep.isNotBlank()) o.put("authorEvacPoint", ep)

    val hosp = user.hospital?.trim().orEmpty()
    if (hosp.isNotBlank()) o.put("authorHospital", hosp)

    val bh = boundHospital?.trim().orEmpty()
    if (bh.isNotBlank()) o.put("boundHospital", bh)

    val from = fromEvacPoint?.trim().orEmpty()
    if (from.isNotBlank()) o.put("fromEvacPoint", from)

    return o.toString()
}
