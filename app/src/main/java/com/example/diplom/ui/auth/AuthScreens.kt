package com.example.diplom.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diplom.access.EvacHospitalBindingStorage
import com.example.diplom.auth.AuthStorage
import com.example.diplom.auth.UserRole

private enum class AuthMode { LOGIN, REGISTER }

@Composable
fun AuthScreen(
    onAuthed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(AuthMode.LOGIN) }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (mode == AuthMode.LOGIN) "Вход" else "Регистрация",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(18.dp))

            if (mode == AuthMode.LOGIN) {
                LoginForm(
                    onSuccess = onAuthed,
                    onSwitchToRegister = { mode = AuthMode.REGISTER }
                )
            } else {
                RegisterForm(
                    onSuccess = onAuthed,
                    onSwitchToLogin = { mode = AuthMode.LOGIN }
                )
            }
        }
    }
}

@Composable
private fun LoginForm(
    onSuccess: () -> Unit,
    onSwitchToRegister: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current

    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    OutlinedTextField(
        value = login,
        onValueChange = { login = it; error = null },
        label = { Text("Логин") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it; error = null },
        label = { Text("Пароль") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = PasswordVisualTransformation()
    )

    if (error != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
    }

    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = {
            val ok = AuthStorage.login(ctx, login, password)
            if (ok) onSuccess() else error = "Неверный логин или пароль"
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text("Войти")
    }
    Spacer(modifier = Modifier.height(10.dp))

    Text(
        text = "Нет аккаунта? Зарегистрироваться",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable { onSwitchToRegister() }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RegisterForm(
    onSuccess: () -> Unit,
    onSwitchToLogin: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current

    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fio by remember { mutableStateOf("") }
    var callsign by remember { mutableStateOf("") }

    var role by remember { mutableStateOf(UserRole.FELDSHER) }
    var evacPoint by remember { mutableStateOf("") }
    var hospital by remember { mutableStateOf("") }

    var roleMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    OutlinedTextField(
        value = login,
        onValueChange = { login = it; error = null },
        label = { Text("Придумайте логин") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = password,
        onValueChange = { password = it; error = null },
        label = { Text("Придумайте пароль") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = PasswordVisualTransformation()
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = fio,
        onValueChange = { fio = it; error = null },
        label = { Text("Ваше ФИО") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = callsign,
        onValueChange = { callsign = it; error = null },
        label = { Text("Позывной") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(10.dp))

    ExposedDropdownMenuBox(
        expanded = roleMenu,
        onExpandedChange = { roleMenu = !roleMenu },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = role.titleRu,
            onValueChange = {},
            readOnly = true,
            label = { Text("Ваша профессия") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleMenu) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = roleMenu,
            onDismissRequest = { roleMenu = false }
        ) {
            UserRole.entries.forEach { r ->
                DropdownMenuItem(
                    text = { Text(r.titleRu) },
                    onClick = {
                        role = r
                        roleMenu = false
                        error = null
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
    when (role) {
        UserRole.FELDSHER -> {
            OutlinedTextField(
                value = evacPoint,
                onValueChange = { evacPoint = it; error = null },
                label = { Text("Эвакопункт") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        UserRole.EVAC_DOCTOR -> {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = evacPoint,
                    onValueChange = { evacPoint = it; error = null },
                    label = { Text("Эвакопункт") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = hospital,
                    onValueChange = { hospital = it; error = null },
                    label = { Text("Госпиталь (привязка эвакопункта)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        UserRole.HOSPITAL_DOCTOR -> {
            OutlinedTextField(
                value = hospital,
                onValueChange = { hospital = it; error = null },
                label = { Text("Госпиталь") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        UserRole.FUND -> {
            // без привязок
        }
    }

    if (error != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = error ?: "", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
    }

    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = {
            val needsEvac = role == UserRole.FELDSHER || role == UserRole.EVAC_DOCTOR
            val needsHosp = role == UserRole.HOSPITAL_DOCTOR || role == UserRole.EVAC_DOCTOR
            if (needsEvac && evacPoint.isBlank()) {
                error = "Укажите эвакопункт"
                return@Button
            }
            if (needsHosp && hospital.isBlank()) {
                error = "Укажите госпиталь"
                return@Button
            }
            val ok = AuthStorage.register(
                ctx = ctx,
                login = login,
                password = password,
                fio = fio,
                callsign = callsign,
                role = role,
                evacPoint = evacPoint,
                hospital = hospital
            )
            if (ok) {
                if (role == UserRole.EVAC_DOCTOR) {
                    EvacHospitalBindingStorage.setHospitalForEvacPoint(ctx, evacPoint, hospital)
                }
                onSuccess()
            } else {
                error = "Не удалось зарегистрироваться (логин занят?)"
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text("Зарегистрироваться")
    }
    Spacer(modifier = Modifier.height(10.dp))

    Text(
        text = "Уже есть аккаунт? Войти",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable { onSwitchToLogin() }
    )
}
