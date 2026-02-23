package com.example.diplom.ui.fund

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diplom.access.EvacHospitalBindingStorage
import com.example.diplom.access.OrgDirectory
import com.example.diplom.data.AppDb
import com.example.diplom.data.MedicalRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.Collator
import java.util.Locale

enum class FundKind(val titleRu: String) {
    HOSPITALS("Госпиталя"),
    EVAC_POINTS("Эвакопункты")
}

private data class MedicineRow(val name: String, val qty: String)

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

    val bindings = remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(Unit) {
        bindings.value = EvacHospitalBindingStorage.allBindings(ctx)
    }

    val hospitals = remember(bindings.value) {
        val collator = Collator.getInstance(Locale("ru", "RU")).apply { strength = Collator.PRIMARY }
        (OrgDirectory.hospitals + bindings.value.values)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith { a, b -> collator.compare(a, b) }
    }
    val evacPoints = remember(bindings.value) {
        val collator = Collator.getInstance(Locale("ru", "RU")).apply { strength = Collator.PRIMARY }
        (OrgDirectory.evacPoints + bindings.value.keys)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith { a, b -> collator.compare(a, b) }
    }

    val list = if (kind == FundKind.HOSPITALS) hospitals else evacPoints

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
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
                color = Color(0xFF9E9E9E),
                modifier = Modifier.clickable { onLogout() }
            )
            Text(
                text = "Фонд",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.width(40.dp))
        }

        ExposedDropdownMenuBox(
            expanded = kindMenu,
            onExpandedChange = { kindMenu = !kindMenu },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = kind.titleRu,
                onValueChange = {},
                readOnly = true,
                label = { Text("Раздел") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kindMenu) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = kindMenu,
                onDismissRequest = { kindMenu = false }
            ) {
                FundKind.entries.forEach { k ->
                    DropdownMenuItem(
                        text = { Text(k.titleRu) },
                        onClick = {
                            kind = k
                            kindMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (list.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Нет данных", color = Color(0xFF666666))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(list) { name ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .clickable { onOpenTable(kind, name) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Открыть",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Divider(color = Color(0xFFEAEAEA))
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
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
    var rows by remember { mutableStateOf<List<MedicineRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(kind, locationName) {
        loading = true
        val data = withContext(Dispatchers.IO) {
            val records = db.appDao().getAllRecords()
            buildMedicineRows(records, kind, locationName)
        }
        rows = data
        loading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
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
            Text(
                text = "Лекарства: $locationName",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                text = "выход",
                fontSize = 12.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.clickable { onLogout() }
            )
        }

        Divider(color = Color(0xFFEAEAEA))

        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        if (rows.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Нет записей по лекарствам", color = Color(0xFF666666))
            }
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Препарат", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("Количество", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        Divider(color = Color(0xFFEAEAEA))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            items(rows) { r ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = r.name,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = r.qty,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
                Divider(color = Color(0xFFF0F0F0))
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
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
        val meds = parseMedicines(meta)
        if (meds.isEmpty()) continue

        when (kind) {
            FundKind.EVAC_POINTS -> {
                val authorEvac = meta.optString("authorEvacPoint", "").trim()
                if (!authorEvac.equals(loc, ignoreCase = false)) continue
                out.addAll(meds)
            }
            FundKind.HOSPITALS -> {
                val role = meta.optString("authorRole", "").trim()
                val authorHospital = meta.optString("authorHospital", "").trim()
                val boundHospital = meta.optString("boundHospital", "").trim()

                val ok = if (role == "HOSPITAL_DOCTOR") {
                    authorHospital == loc
                } else {
                    boundHospital == loc
                }
                if (!ok) continue
                out.addAll(meds)
            }
        }
    }

    val collator = Collator.getInstance(Locale("ru", "RU")).apply { strength = Collator.PRIMARY }
    return out.sortedWith { a, b ->
        val c1 = collator.compare(a.name, b.name)
        if (c1 != 0) return@sortedWith c1
        collator.compare(a.qty, b.qty)
    }
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

private fun safeJson(rawText: String?): JSONObject {
    return try {
        if (rawText.isNullOrBlank()) JSONObject() else JSONObject(rawText)
    } catch (_: Exception) {
        JSONObject()
    }
}
