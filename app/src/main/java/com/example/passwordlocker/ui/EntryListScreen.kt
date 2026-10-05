package com.example.passwordlocker.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.passwordlocker.data.Entry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryListScreen(
    vm: EntryViewModel,
    onAddEntry: () -> Unit,
    onEditEntry: (Long) -> Unit
) {
    val entries by vm.entries.collectAsState()
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current

    // Меню и диалоги
    var showMenu by remember { mutableStateOf(false) }
    var showChangePwdDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    // Импорт
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var showImportPwdDialog by remember { mutableStateOf(false) }

    // Экспорт
    var exportUri by remember { mutableStateOf<Uri?>(null) }
    var showExportPwdDialog by remember { mutableStateOf(false) }

    // ---------- SAF ----------

    // Создание файла (экспорт)
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            exportUri = uri
            showExportPwdDialog = true
        }
    }

    // Открытие файла (импорт)
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importUri = uri
            showImportPwdDialog = true
        }
    }

    // ---------- UI ----------

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Менеджер паролей") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Сменить мастер-пароль") },
                            onClick = {
                                showMenu = false
                                showChangePwdDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Экспорт") },
                            onClick = {
                                showMenu = false
                                exportLauncher.launch("passwords_backup.json")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Импорт") },
                            onClick = {
                                showMenu = false
                                importLauncher.launch(arrayOf("application/json", "*/*"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить все") },
                            onClick = {
                                showMenu = false
                                showDeleteAllDialog = true
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntry) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Нет сохраненных записей")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    EntryCard(
                        entry = entry,
                        onEdit = { onEditEntry(entry.id) },
                        onDelete = { vm.delete(entry) },
                        onCopyPassword = {
                            clipboard.setText(AnnotatedString(entry.password))
                            Toast.makeText(ctx, "Пароль скопирован", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // ---------- Диалоги ----------

    // Смена мастер-пароля
    if (showChangePwdDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePwdDialog = false },
            onConfirm = { old, new ->
                vm.changeMasterPassword(old, new) { ok ->
                    showChangePwdDialog = false
                    Toast.makeText(
                        ctx,
                        if (ok) "Пароль успешно изменён" else "Неверный старый пароль",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // Диалог пароля для экспорта
    if (showExportPwdDialog && exportUri != null) {
        ExportPasswordDialog(
            onDismiss = {
                showExportPwdDialog = false
                exportUri = null
            },
            onConfirm = { pwd ->
                val uri = exportUri!!
                showExportPwdDialog = false
                exportUri = null
                vm.exportTo(uri, pwd) { ok ->
                    Toast.makeText(
                        ctx,
                        if (ok) "Экспорт завершён" else "Ошибка экспорта",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // Диалог пароля для импорта
    if (showImportPwdDialog && importUri != null) {
        ImportPasswordDialog(
            onDismiss = {
                showImportPwdDialog = false
                importUri = null
            },
            onConfirm = { pwd ->
                val uri = importUri!!
                showImportPwdDialog = false
                importUri = null
                vm.importFile(uri, pwd) { ok ->
                    Toast.makeText(
                        ctx,
                        if (ok) "Импорт завершён" else "Неверный пароль или повреждённый файл",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    // Удаление всех записей
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Удалить все?") },
            text = { Text("Все записи будут удалены. Действие необратимо.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteAll()
                    showDeleteAllDialog = false
                    Toast.makeText(ctx, "Все записи удалены", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Отмена") }
            }
        )
    }
}

// ---------- Карточка записи ----------

@Composable
fun EntryCard(
    entry: Entry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyPassword: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = entry.title, style = MaterialTheme.typography.titleLarge)
            if (entry.login.isNotEmpty()) {
                Text(
                    text = "Логин: ${entry.login}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (passwordVisible) entry.password else "••••••••",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                        contentDescription = "Показать пароль"
                    )
                }
                IconButton(onClick = onCopyPassword) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Копировать")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (entry.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Заметки: ${entry.notes}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}