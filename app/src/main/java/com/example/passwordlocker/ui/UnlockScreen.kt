package com.example.passwordlocker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockScreen(vm: EntryViewModel, onUnlocked: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val isFirstRun = !vm.isKeyInitialized()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isFirstRun) "Создайте мастер-пароль" else "Введите мастер-пароль",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Мастер-пароль") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (isFirstRun) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; error = null },
                    label = { Text("Подтвердите пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (password.isBlank()) {
                        error = "Пароль не может быть пустым"
                        return@Button
                    }
                    if (isFirstRun) {
                        if (password != confirmPassword) {
                            error = "Пароли не совпадают"
                        } else {
                            vm.setupMasterPassword(password)
                            onUnlocked()
                        }
                    } else {
                        if (vm.unlock(password)) {
                            onUnlocked()
                        } else {
                            error = "Неверный пароль"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isFirstRun) "Создать" else "Войти")
            }
        }
    }
}