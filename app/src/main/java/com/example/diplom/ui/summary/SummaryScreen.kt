package com.example.diplom.ui.summary

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
import com.example.diplom.access.InboxKind
import com.example.diplom.pdf.Form100Storage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SummaryScreen(
    title: String,
    inboxKind: InboxKind,
    locationName: String,
    canForwardToHospital: Boolean,
    forwardHospitals: List<String>,
    onBack: () -> Unit,
    onOpenPdf: (File) -> Unit,
    onForwardToHospital: (File, String) -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current

    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var forwardFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(inboxKind, locationName) {
        val dir = when (inboxKind) {
            InboxKind.EVAC_POINT -> Form100Storage.evacInboxDir(ctx, locationName)
            InboxKind.HOSPITAL -> Form100Storage.hospitalInboxDir(ctx, locationName)
        }
        files = dir
            .listFiles { f -> f.isFile && f.extension.equals("pdf", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
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
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                Spacer(modifier = Modifier.width(40.dp))
            }

            if (files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Файлов пока нет", color = Color(0xFF666666))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(files) { f ->
                        SummaryRow(
                            file = f,
                            canForward = canForwardToHospital,
                            onOpen = { onOpenPdf(f) },
                            onForward = { forwardFile = f }
                        )
                        Divider(color = Color(0xFFEAEAEA))
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        if (forwardFile != null && canForwardToHospital) {
            HospitalPickDialog(
                hospitals = forwardHospitals,
                onDismiss = { forwardFile = null },
                onPick = { hospital ->
                    val f = forwardFile
                    forwardFile = null
                    if (f != null) onForwardToHospital(f, hospital)
                }
            )
        }
    }
}

@Composable
private fun SummaryRow(
    file: File,
    canForward: Boolean,
    onOpen: () -> Unit,
    onForward: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val dt = remember(file) { sdf.format(Date(file.lastModified())) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.nameWithoutExtension,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = dt, fontSize = 12.sp, color = Color(0xFF777777))
        }

        Text(
            text = "Открыть",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .clickable { onOpen() }
        )

        if (canForward) {
            Text(
                text = "Отправить",
                color = Color(0xFF2E7D32),
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .clickable { onForward() }
            )
        }
    }
}

@Composable
private fun HospitalPickDialog(
    hospitals: List<String>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Куда отправить?") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                hospitals.forEach { h ->
                    Text(
                        text = h,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(h) }
                            .padding(vertical = 10.dp),
                        fontSize = 15.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}
