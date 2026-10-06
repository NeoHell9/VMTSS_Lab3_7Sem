package com.example.passwordlocker.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.passwordlocker.crypto.CryptoManager
import com.example.passwordlocker.crypto.KeyManager
import com.example.passwordlocker.data.AppDatabase
import com.example.passwordlocker.data.Entry
import com.example.passwordlocker.data.EntryEntity
import com.example.passwordlocker.data.EntryRepository
import com.example.passwordlocker.export.ExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EntryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = EntryRepository(AppDatabase.get(app).entryDao())

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked

    // ----- Ключ -----

    fun isKeyInitialized(): Boolean =
        KeyManager.isInitialized(getApplication())

    fun setupMasterPassword(password: String) {
        val ctx = getApplication<Application>()
        val salt = KeyManager.getOrCreateSalt(ctx)
        val key = CryptoManager.deriveKey(password, salt)
        KeyManager.saveVerification(ctx, key)
        repo.setKey(key)
        _unlocked.value = true
        refresh()
    }

    fun unlock(password: String): Boolean {
        val ctx = getApplication<Application>()
        val salt = KeyManager.getOrCreateSalt(ctx)
        val key = CryptoManager.deriveKey(password, salt)
        return if (KeyManager.verify(ctx, key)) {
            repo.setKey(key)
            _unlocked.value = true
            refresh()
            true
        } else false
    }

    // ----- CRUD -----

    fun refresh() = viewModelScope.launch {
        _entries.value = withContext(Dispatchers.IO) { repo.list() }
    }

    fun add(entry: Entry) = viewModelScope.launch {
        withContext(Dispatchers.IO) { repo.add(entry) }
        refresh()
    }

    fun update(entry: Entry) = viewModelScope.launch {
        withContext(Dispatchers.IO) { repo.update(entry) }
        refresh()
    }

    fun delete(entry: Entry) = viewModelScope.launch {
        withContext(Dispatchers.IO) { repo.delete(entry) }
        refresh()
    }

    fun deleteAll() = viewModelScope.launch {
        withContext(Dispatchers.IO) { repo.deleteAll() }
        refresh()
    }

    // ----- Смена мастер-пароля -----

    fun changeMasterPassword(
        oldPassword: String,
        newPassword: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        val ctx = getApplication<Application>()
        val salt = KeyManager.getOrCreateSalt(ctx)

        val oldKey = CryptoManager.deriveKey(oldPassword, salt)
        if (!KeyManager.verify(ctx, oldKey)) {
            onResult(false)
            return
        }

        val newKey = CryptoManager.deriveKey(newPassword, salt)
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repo.rekey(newKey) }
                KeyManager.saveVerification(ctx, newKey)
                refresh()
                onResult(true)
            } catch (t: Throwable) {
                onResult(false)
            }
        }
    }

    // ----- Экспорт -----

    fun exportTo(uri: Uri, onResult: (Boolean) -> Unit = {}) = viewModelScope.launch {
        val ctx = getApplication<Application>()
        try {
            val key = repo.currentKey() ?: error("Репозиторий заблокирован")
            val list = withContext(Dispatchers.IO) { repo.list() }
            withContext(Dispatchers.IO) {
                ExportManager.export(ctx, uri, list, key)
            }
            onResult(true)
        } catch (t: Throwable) {
            onResult(false)
        }
    }

    // ----- Импорт -----

    fun importFile(
        uri: Uri,
        importPassword: String,
        onResult: (Boolean) -> Unit = {}
    ) = viewModelScope.launch {
        val ctx = getApplication<Application>()
        try {
            // Соль стабильна — она не меняется при смене мастер-пароля,
            // поэтому PBKDF2(X, salt) даст тот же ключ, что был при экспорте.
            val salt = KeyManager.getOrCreateSalt(ctx)
            val oldKey = CryptoManager.deriveKey(importPassword, salt)

            // 1. Расшифровываем файл старым ключом X
            val plain = withContext(Dispatchers.IO) {
                ExportManager.readAndDecrypt(ctx, uri, oldKey)
            }

            // 2. Достаём актуальный мастер-ключ Y
            val newKey = repo.currentKey() ?: error("Репозиторий заблокирован")

            // 3. Перешифровываем ключом Y и вставляем в БД
            val rows = plain.map {
                EntryEntity(
                    title    = CryptoManager.encrypt(it.title, newKey),
                    login    = CryptoManager.encrypt(it.login, newKey),
                    password = CryptoManager.encrypt(it.password, newKey),
                    notes    = CryptoManager.encrypt(it.notes, newKey)
                )
            }
            withContext(Dispatchers.IO) { repo.insertEncrypted(rows) }
            refresh()
            onResult(true)
        } catch (t: Throwable) {
            onResult(false)
        }
    }
}