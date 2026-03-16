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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

private data class MedicineRow(val name: String, val qty: String)

private enum class DateSort {
    DESC,
    ASC
}

private data class MedicineGroup(
    val atMillis: Long,
    val atText: String,
    val items: List<MedicineRow>
)

private sealed class FundListItem {
    data class Header(val atText: String, val atMillis: Long) : FundListItem()
    data class Row(val atMillis: Long, val name: String, val qty: String) : FundListItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundHomeScreen(
    onLogout: () -> Unit,
    onOpenTable: (FundKind, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current

    var kind by remember { mutableStateOf(FundKind.HOSPITALS) }
    var kindMenu by remember { mutableStateOf(false) }

    var bindings by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var users by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    LaunchedEffect(Unit) {
        bindings = EvacHospitalBindingStorage.allBindings(ctx)
        users = AuthStorage.allUsers(ctx)
    }

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

    val hospitals = remember(bindings, registeredHospitals) {
        (OrgDirectory.hospitals + bindings.values + registeredHospitals)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale("ru", "RU")) }
            .sortedWith { a, b -> collator.compare(a, b) }
    }

    val evacPoints = remember(bindings, registeredEvacPoints) {
        (OrgDirectory.evacPoints + bindings.keys + registeredEvacPoints)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale("ru", "RU")) }
            .sortedWith { a, b -> collator.compare(a, b) }
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
                        "Список госпиталей обновляется автоматически по регистрации"
                    else
                        "Список эвакопунктов обновляется автоматически по регистрации",
                    fontSize = 12.sp,
                    color = Color(0xFF8A8A8A)
                )
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
    var groups by remember { mutableStateOf<List<MedicineGroup>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var dateSort by remember { mutableStateOf(DateSort.DESC) }

    LaunchedEffect(kind, locationName) {
        loading = true
        val data = withContext(Dispatchers.IO) {
            val records = db.appDao().getAllRecords()
            buildMedicineGroups(records, kind, locationName)
        }
        groups = data
        loading = false
    }

    val sortedGroups = remember(groups, dateSort) {
        when (dateSort) {
            DateSort.DESC -> groups.sortedByDescending { it.atMillis }
            DateSort.ASC -> groups.sortedBy { it.atMillis }
        }
    }
    val itemsForUi = remember(sortedGroups) { flattenGroups(sortedGroups) }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Препарат", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Количество", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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

        if (itemsForUi.isEmpty()) {
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
            items(itemsForUi) { item ->
                when (item) {
                    is FundListItem.Header -> {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            color = Color(0xFFFAFAFA),
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.atText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2F2F2F),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    is FundListItem.Row -> {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            color = Color.White,
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.name,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 14.sp,
                                    color = Color(0xFF202020)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.qty,
                                    fontSize = 14.sp,
                                    color = Color(0xFF202020)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

private fun buildMedicineGroups(
    records: List<MedicalRecordEntity>,
    kind: FundKind,
    locationName: String
): List<MedicineGroup> {
    val loc = locationName.trim()
    if (loc.isBlank()) return emptyList()

    data class Acc(
        var atMillis: Long,
        val atText: String,
        val items: MutableList<MedicineRow>
    )

    val grouped = linkedMapOf<String, Acc>()

    for (r in records) {
        val meta = safeJson(r.rawText)
        val meds = parseMedicines(meta)
        if (meds.isEmpty()) continue

        val ok = when (kind) {
            FundKind.EVAC_POINTS -> {
                val authorEvac = meta.optString("authorEvacPoint", "").trim()
                authorEvac == loc
            }

            FundKind.HOSPITALS -> {
                val role = meta.optString("authorRole", "").trim()
                val authorHospital = meta.optString("authorHospital", "").trim()
                val boundHospital = meta.optString("boundHospital", "").trim()
                if (role == "HOSPITAL_DOCTOR") authorHospital == loc else boundHospital == loc
            }
        }

        if (!ok) continue

        val filledAt = meta.optString("filledAt", "").trim()
        val atMillis = parseDateTimeMillis(filledAt) ?: r.createdAt
        val atText = filledAt.ifBlank { formatDateTime(r.createdAt) }

        val acc = grouped.getOrPut(atText) {
            Acc(
                atMillis = atMillis,
                atText = atText,
                items = ArrayList(8)
            )
        }
        if (atMillis < acc.atMillis) acc.atMillis = atMillis
        acc.items.addAll(meds)
    }

    return grouped.values
        .map { MedicineGroup(atMillis = it.atMillis, atText = it.atText, items = it.items) }
}

private fun flattenGroups(groups: List<MedicineGroup>): List<FundListItem> {
    val out = ArrayList<FundListItem>(groups.size * 4)
    for (g in groups) {
        out.add(FundListItem.Header(atText = g.atText, atMillis = g.atMillis))
        for (m in g.items) {
            out.add(
                FundListItem.Row(
                    atMillis = g.atMillis,
                    name = m.name,
                    qty = m.qty
                )
            )
        }
    }
    return out
}

private fun parseMedicines(meta: JSONObject): List<MedicineRow> {
    val arr = meta.optJSONArray("medicines") ?: JSONArray()
    if (arr.length() == 0) return emptyList()

    val out = ArrayList<MedicineRow>(arr.length())
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val name = dash(o.optString("name", "").trim())
        val qty = dash(o.optString("qty", "").trim())
        out.add(MedicineRow(name = name, qty = qty))
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

private fun safeJson(rawText: String?): JSONObject {
    return try {
        if (rawText.isNullOrBlank()) JSONObject() else JSONObject(rawText)
    } catch (_: Exception) {
        JSONObject()
    }
}
