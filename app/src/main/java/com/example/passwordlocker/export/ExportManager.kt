package com.example.passwordlocker.export

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.passwordlocker.crypto.CryptoManager
import com.example.passwordlocker.data.Entry
import com.google.gson.Gson
import com.google.gson.JsonObject
import javax.crypto.SecretKey

object ExportManager {
    private val gson = Gson()
    private const val VERSION = 1

    data class PlainEntry(
        val title: String, val login: String,
        val password: String, val notes: String
    )
    private data class PlainPayload(val entries: List<PlainEntry>)

    /** Экспорт: шифруем файл переданным ключом (актуальным мастер-ключом). */
    fun export(ctx: Context, uri: Uri, entries: List<Entry>, key: SecretKey) {
        val payload = PlainPayload(entries.map {
            PlainEntry(it.title, it.login, it.password, it.notes)
        })
        val json = gson.toJson(payload).toByteArray(Charsets.UTF_8)
        val enc = CryptoManager.encryptBytes(json, key)

        val root = JsonObject().apply {
            addProperty("version", VERSION)
            addProperty("data", Base64.encodeToString(enc, Base64.NO_WRAP))
        }
        ctx.contentResolver.openOutputStream(uri)!!.use {
            it.write(gson.toJson(root).toByteArray(Charsets.UTF_8))
        }
    }

    /** Импорт: расшифровываем файл ключом, который вывел пользователь. */
    fun readAndDecrypt(ctx: Context, uri: Uri, key: SecretKey): List<PlainEntry> {
        val text = ctx.contentResolver.openInputStream(uri)!!
            .bufferedReader().use { it.readText() }
        val root = gson.fromJson(text, JsonObject::class.java)
        val data = Base64.decode(root.get("data").asString, Base64.NO_WRAP)
        val plain = CryptoManager.decryptBytes(data, key)
        return gson.fromJson(String(plain, Charsets.UTF_8), PlainPayload::class.java).entries
    }
}