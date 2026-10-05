package com.example.passwordlocker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.passwordlocker.data.Entry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditScreen(
    vm: EntryViewModel,
    entryId: Long?,      // null = новая запись
    onBack: () -> Unit
) {
    val entries by vm.entries.collectAsState()           // ⬅️ подписываемся на список
    val existing: Entry? = remember(entryId, entries) {
        if (entryId == null) null else entries.find { it.id == entryId }
    }

    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var login by remember(existing) { mutableStateOf(existing?.login ?: "") }
    var password by remember(existing) { mutableStateOf(existing?.password ?: "") }
    var notes by remember(existing) { mutableStateOf(existing?.notes ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Новая запись" else "Редактирование") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (title.isNotBlank() && password.isNotBlank()) {
                                val newEntry = Entry(
                                    id = existing?.id ?: 0L,
                                    title = title,
                                    login = login,
                                    password = password,
                                    notes = notes
                                )
                                if (existing == null) vm.add(newEntry) else vm.update(newEntry)
                                onBack()
                            }
                        },
                        enabled = title.isNotBlank() && password.isNotBlank()
                    ) {
                        Text("Сохранить")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Ресурс (обязательно)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = login,
                onValueChange = { login = it },
                label = { Text("Логин") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль (обязательно)") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Заметки") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
        }
    }
}