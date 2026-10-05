package com.example.passwordlocker.export

import android.content.Context
import android.net.Uri
import com.example.passwordlocker.crypto.CryptoManager
import com.example.passwordlocker.data.Entry
import com.google.gson.Gson
import com.google.gson.JsonObject
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKey

object ExportManager {
    private val gson = Gson()
    private const val VERSION = 1
    private const val ITER = 10_000

    data class PlainEntry(
        val title: String, val login: String,
        val password: String, val notes: String
    )
    private data class PlainPayload(val entries: List<PlainEntry>)

    /** Экспорт: шифрует текущим ключом (пароль пользователя уже введён). */
    fun export(ctx: Context, uri: Uri, entries: List<Entry>, password: String) {
        val salt = CryptoManager.randomSalt()
        val key  = CryptoManager.deriveKey(password, salt)

        val payload = PlainPayload(entries.map {
            PlainEntry(it.title, it.login, it.password, it.notes)
        })
        val json = gson.toJson(payload).toByteArray(Charsets.UTF_8)
        val enc  = CryptoManager.encryptBytes(json, key)

        val root = JsonObject().apply {
            addProperty("version", VERSION)
            addProperty("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            addProperty("iterations", ITER)
            addProperty("data", Base64.encodeToString(enc, Base64.NO_WRAP))
        }
        ctx.contentResolver.openOutputStream(uri)!!.use {
            it.write(gson.toJson(root).toByteArray(Charsets.UTF_8))
        }
    }

    /** Чтение файла и расшифровка указанным паролем. */
    fun readAndDecrypt(ctx: Context, uri: Uri, password: String): List<PlainEntry> {
        val text = ctx.contentResolver.openInputStream(uri)!!
            .bufferedReader().use { it.readText() }
        val root = gson.fromJson(text, JsonObject::class.java)
        val salt = Base64.decode(root.get("salt").asString, Base64.NO_WRAP)
        val data = Base64.decode(root.get("data").asString, Base64.NO_WRAP)
        val key: SecretKey = CryptoManager.deriveKey(password, salt)
        val plain = CryptoManager.decryptBytes(data, key)
        return gson.fromJson(String(plain, Charsets.UTF_8), PlainPayload::class.java).entries
    }


}