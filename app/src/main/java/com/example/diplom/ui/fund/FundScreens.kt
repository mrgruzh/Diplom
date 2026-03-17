package com.example.diplom.ui.fund

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diplom.access.EvacHospitalBindingStorage
import com.example.diplom.access.OrgDirectory
import com.example.diplom.auth.AuthStorage
import com.example.diplom.auth.UserProfile
import com.example.diplom.auth.UserRole
import com.example.diplom.data.AppDb
import com.example.diplom.data.MedicalRecordEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.Collator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FundKind(val titleRu: String) {
    HOSPITALS("Госпиталя"),
    EVAC_POINTS("Эвакопункты")
}

private data class MedicineRow(
    val name: String,
    val qty: String,
    val timeText: String,
    val atMillis: Long,
    val sourceLabel: String
)
private data class RecordLocations(val evacPoints: List<String>, val hospitals: List<String>)
private data class LocationTimes(val evacPoints: Map<String, Long>, val hospitals: Map<String, Long>)

private enum class DateSort {
    DESC,
    ASC
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundHomeScreen(
    db: AppDb,
    onLogout: () -> Unit,
    onOpenTable: (FundKind, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current

    var kind by remember { mutableStateOf(FundKind.HOSPITALS) }
    var kindMenu by remember { mutableStateOf(false) }
    var listSort by remember { mutableStateOf(DateSort.DESC) }

    var bindings by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    LaunchedEffect(Unit) {
        bindings = EvacHospitalBindingStorage.allBindings(ctx)
        users = AuthStorage.allUsers(ctx)
    }

    val records by db.appDao().observeAllRecords().collectAsState(initial = emptyList())
    val recordLocations = remember(records) { extractLocationsFromRecords(records) }
    val recordTimes = remember(records) { extractLocationTimes(records) }

    val collator = remember {
        Collator.getInstance(Locale("ru", "RU")).apply {
            strength = Collator.PRIMARY
        }
    }

    val registeredHospitals = remember(users) {
        users
            .mapNotNull { profile ->
                when (profile.role) {
                    UserRole.EVAC_DOCTOR,
                    UserRole.HOSPITAL_DOCTOR -> profile.hospital
                    else -> null
                }
            }
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    val registeredEvacPoints = remember(users) {
        users
            .mapNotNull { profile ->
                when (profile.role) {
                    UserRole.FELDSHER,
                    UserRole.EVAC_DOCTOR -> profile.evacPoint
                    else -> null
                }
            }
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    val hospitalsBase = remember(bindings, registeredHospitals, recordLocations) {
        (OrgDirectory.hospitals + bindings.values + registeredHospitals + recordLocations.hospitals)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale("ru", "RU")) }
    }

    val evacPointsBase = remember(bindings, registeredEvacPoints, recordLocations) {
        (OrgDirectory.evacPoints + bindings.keys + registeredEvacPoints + recordLocations.evacPoints)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale("ru", "RU")) }
    }

    val hospitals = remember(hospitalsBase, listSort, collator, recordTimes) {
        sortByTime(hospitalsBase, recordTimes.hospitals, listSort, collator)
    }

    val evacPoints = remember(evacPointsBase, listSort, collator, recordTimes) {
        sortByTime(evacPointsBase, recordTimes.evacPoints, listSort, collator)
    }

    val list = if (kind == FundKind.HOSPITALS) hospitals else evacPoints

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF8F9F5), Color.White)
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "выход",
                fontSize = 12.sp,
                color = Color(0xFF777777),
                modifier = Modifier
                    .background(Color(0x14A9A9A9), shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .clickable { onLogout() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )

            Text(
                text = "Фонд",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2F2F2F)
            )

            Spacer(modifier = Modifier.width(46.dp))
        }

        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            color = Color.White,
            tonalElevation = 1.dp,
            shadowElevation = 5.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Выберите раздел",
                    fontSize = 13.sp,
                    color = Color(0xFF6A6A6A)
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = kindMenu,
                    onExpandedChange = { kindMenu = !kindMenu },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = kind.titleRu,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = kindMenu)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = kindMenu,
                        onDismissRequest = { kindMenu = false }
                    ) {
                        FundKind.entries.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.titleRu) },
                                onClick = {
                                    kind = item
                                    kindMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (kind == FundKind.HOSPITALS)
                        "Список госпиталей обновляется по регистрации и синхронизации"
                    else
                        "Список эвакопунктов обновляется по регистрации и синхронизации",
                    fontSize = 12.sp,
                    color = Color(0xFF8A8A8A)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    val label = if (listSort == DateSort.DESC)
                        "Сортировка: новые сверху"
                    else
                        "Сортировка: старые сверху"

                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = Color(0xFF777777),
                        modifier = Modifier
                            .background(
                                Color(0x14A9A9A9),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                listSort = if (listSort == DateSort.DESC) DateSort.ASC else DateSort.DESC
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (list.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "Нет данных",
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp)
                    )
                }
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(list) { name ->
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenTable(kind, name) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF242424)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Нажмите, чтобы открыть сводку препаратов",
                                fontSize = 12.sp,
                                color = Color(0xFF8A8A8A)
                            )
                        }

                        Text(
                            text = "Открыть",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }
}

@Composable
fun FundMedicineTableScreen(
    db: AppDb,
    kind: FundKind,
    locationName: String,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val records by db.appDao().observeAllRecords().collectAsState(initial = emptyList())
    var loading by remember { mutableStateOf(true) }
    var dateSort by remember { mutableStateOf(DateSort.DESC) }

    val rows = remember(records, kind, locationName) {
        buildMedicineRows(records, kind, locationName)
    }
    LaunchedEffect(rows) {
        loading = false
    }

    val sortedRows = remember(rows, dateSort) {
        when (dateSort) {
            DateSort.DESC -> rows.sortedByDescending { it.atMillis }
            DateSort.ASC -> rows.sortedBy { it.atMillis }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF8F9F5), Color.White)
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = Color.Black)
            }
            Text(
                text = "Сводка: $locationName",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF272727)
            )
            Text(
                text = "выход",
                fontSize = 12.sp,
                color = Color(0xFF777777),
                modifier = Modifier
                    .background(Color(0x14A9A9A9), shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .clickable { onLogout() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Лекарство",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Доза",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(0.35f)
                )
                Text(
                    "Время",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(0.45f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            val title = when (dateSort) {
                DateSort.DESC -> "Сортировка: новые сверху"
                DateSort.ASC -> "Сортировка: старые сверху"
            }
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color(0xFF777777),
                modifier = Modifier
                    .background(Color(0x14A9A9A9), shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .clickable {
                        dateSort = if (dateSort == DateSort.DESC) DateSort.ASC else DateSort.DESC
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        if (sortedRows.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "Нет записей по препаратам",
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(sortedRows) { row ->
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = row.sourceLabel,
                            fontSize = 11.sp,
                            color = Color(0xFF9A9A9A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = row.name,
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                color = Color(0xFF202020)
                            )
                            Text(
                                text = row.qty,
                                modifier = Modifier.weight(0.35f),
                                fontSize = 14.sp,
                                color = Color(0xFF202020)
                            )
                            Text(
                                text = row.timeText,
                                modifier = Modifier.weight(0.45f),
                                fontSize = 13.sp,
                                color = Color(0xFF444444)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

private fun buildMedicineRows(
    records: List<MedicalRecordEntity>,
    kind: FundKind,
    locationName: String
): List<MedicineRow> {
    val loc = locationName.trim()
    if (loc.isBlank()) return emptyList()

    val out = ArrayList<MedicineRow>(64)

    for (r in records) {
        val meta = safeJson(r.rawText)
        val placement = recordPlacement(r, meta)
        val target = if (kind == FundKind.EVAC_POINTS) placement.evacPoint else placement.hospital
        if (target.isNullOrBlank() || target != loc) continue

        val meds = parseMedicines(meta)
        if (meds.isEmpty()) continue

        val filledAt = meta.optString("filledAt", "").trim()
        val atMillis = parseDateTimeMillis(filledAt) ?: r.createdAt
        val timeText = filledAt.ifBlank { formatDateTime(r.createdAt) }
        val sourceLabel = buildSourceLabel(kind, loc, meta, placement)

        for (m in meds) {
            out.add(
                MedicineRow(
                    name = m.name,
                    qty = m.qty,
                    timeText = timeText,
                    atMillis = atMillis,
                    sourceLabel = sourceLabel
                )
            )
        }
    }

    return out
}

private data class MedicineEntry(val name: String, val qty: String)

private fun parseMedicines(meta: JSONObject): List<MedicineEntry> {
    val arr = meta.optJSONArray("medicines") ?: JSONArray()
    if (arr.length() == 0) return emptyList()

    val out = ArrayList<MedicineEntry>(arr.length())
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val name = dash(o.optString("name", "").trim())
        val qty = dash(o.optString("qty", "").trim())
        out.add(MedicineEntry(name = name, qty = qty))
    }
    return out
}

private fun dash(value: String): String = if (value.isBlank()) "-" else value

private val fundDateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

private fun parseDateTimeMillis(value: String): Long? {
    if (value.isBlank()) return null
    return try {
        fundDateTimeFormat.parse(value)?.time
    } catch (_: Exception) {
        null
    }
}

private fun formatDateTime(millis: Long): String = fundDateTimeFormat.format(Date(millis))

private data class RecordPlacement(
    val evacPoint: String?,
    val hospital: String?
)

private fun recordPlacement(record: MedicalRecordEntity, meta: JSONObject): RecordPlacement {
    val authorEvac = meta.optString("authorEvacPoint", "").trim()
    val authorHospital = meta.optString("authorHospital", "").trim()
    val boundHospital = meta.optString("boundHospital", "").trim()
    val stage = record.stage.trim()
    val dest = record.destination?.trim().orEmpty()

    val evacPoint = when {
        authorEvac.isNotBlank() -> authorEvac
        stage.contains("EVAC_POINT", ignoreCase = true) || stage.equals("EVAC", ignoreCase = true) -> dest
        else -> ""
    }

    val hospital = when {
        authorHospital.isNotBlank() -> authorHospital
        boundHospital.isNotBlank() -> boundHospital
        stage.contains("HOSPITAL", ignoreCase = true) -> dest
        else -> ""
    }

    return RecordPlacement(
        evacPoint = evacPoint.ifBlank { null },
        hospital = hospital.ifBlank { null }
    )
}

private fun buildSourceLabel(
    kind: FundKind,
    locationName: String,
    meta: JSONObject,
    placement: RecordPlacement
): String {
    val authorEvac = meta.optString("authorEvacPoint", "").trim()
    val fromEvac = meta.optString("fromEvacPoint", "").trim()
    val authorHospital = meta.optString("authorHospital", "").trim()
    val boundHospital = meta.optString("boundHospital", "").trim()

    return when (kind) {
        FundKind.HOSPITALS -> {
            when {
                authorHospital.isNotBlank() && authorHospital == locationName ->
                    "Госпиталь: $authorHospital"
                boundHospital.isNotBlank() && boundHospital == locationName -> {
                    val ep = authorEvac.ifBlank { fromEvac }
                    if (ep.isNotBlank()) "Эвакопункт: $ep" else "Эвакопункт"
                }
                authorHospital.isNotBlank() -> "Госпиталь: $authorHospital"
                authorEvac.isNotBlank() || fromEvac.isNotBlank() -> {
                    val ep = authorEvac.ifBlank { fromEvac }
                    "Эвакопункт: $ep"
                }
                placement.hospital != null -> "Госпиталь: ${placement.hospital}"
                else -> "Источник: $locationName"
            }
        }

        FundKind.EVAC_POINTS -> {
            val ep = authorEvac
                .ifBlank { fromEvac }
                .ifBlank { placement.evacPoint ?: locationName }
            "Эвакопункт: $ep"
        }
    }
}

private fun extractLocationsFromRecords(records: List<MedicalRecordEntity>): RecordLocations {
    val evacPoints = LinkedHashSet<String>()
    val hospitals = LinkedHashSet<String>()

    for (r in records) {
        val meta = safeJson(r.rawText)
        val placement = recordPlacement(r, meta)
        placement.evacPoint?.let { evacPoints.add(it) }
        placement.hospital?.let { hospitals.add(it) }
    }

    return RecordLocations(
        evacPoints = evacPoints.toList(),
        hospitals = hospitals.toList()
    )
}

private fun extractLocationTimes(records: List<MedicalRecordEntity>): LocationTimes {
    val evacTimes = LinkedHashMap<String, Long>()
    val hospTimes = LinkedHashMap<String, Long>()

    for (r in records) {
        val meta = safeJson(r.rawText)
        val placement = recordPlacement(r, meta)
        val filledAt = meta.optString("filledAt", "").trim()
        val atMillis = parseDateTimeMillis(filledAt) ?: r.createdAt

        placement.evacPoint?.let { name ->
            val current = evacTimes[name] ?: 0L
            if (atMillis > current) evacTimes[name] = atMillis
        }

        placement.hospital?.let { name ->
            val current = hospTimes[name] ?: 0L
            if (atMillis > current) hospTimes[name] = atMillis
        }
    }

    return LocationTimes(
        evacPoints = evacTimes,
        hospitals = hospTimes
    )
}

private fun sortByTime(
    names: List<String>,
    times: Map<String, Long>,
    sort: DateSort,
    collator: Collator
): List<String> {
    return names.sortedWith { a, b ->
        val ta = times[a] ?: 0L
        val tb = times[b] ?: 0L

        val cmp = when {
            ta == tb -> 0
            sort == DateSort.DESC -> if (ta > tb) -1 else 1
            else -> if (ta < tb) -1 else 1
        }

        if (cmp != 0) cmp else collator.compare(a, b)
    }
}

private fun safeJson(rawText: String?): JSONObject {
    return try {
        if (rawText.isNullOrBlank()) JSONObject() else JSONObject(rawText)
    } catch (_: Exception) {
        JSONObject()
    }
}
