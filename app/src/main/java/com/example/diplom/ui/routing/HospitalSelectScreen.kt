package com.example.diplom.ui.routing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HospitalSelectScreen(
    title: String,
    options: List<String>,
    onBack: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<String?>(null) }

    val filtered = remember(query, options) {
        if (query.isBlank()) options
        else options.filter { it.contains(query, ignoreCase = true) }
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

            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Введите название"
            )

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
                onConfirm(dest)
            },
            enabled = selected != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp)
        ) {
            Text("Отправить")
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color(0xFFF3F3F3), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = "Поиск", tint = Color(0xFF6D6D6D))
        Spacer(modifier = Modifier.width(8.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color.Black),
            decorationBox = { inner ->
                Box {
                    if (value.isBlank()) {
                        Text(placeholder, fontSize = 14.sp, color = Color(0xFF9E9E9E))
                    }
                    inner()
                }
            }
        )
    }
}
