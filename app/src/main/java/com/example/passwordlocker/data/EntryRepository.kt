package com.example.passwordlocker.data

import com.example.passwordlocker.crypto.CryptoManager
import javax.crypto.SecretKey

class EntryRepository(private val dao: EntryDao) {

    @Volatile private var key: SecretKey? = null

    fun setKey(k: SecretKey) { key = k }
    fun clearKey() { key = null }
    fun hasKey() = key != null
    fun currentKey(): SecretKey? = key

    private fun requireKey(): SecretKey = key ?: error("Key is not set")

    suspend fun list(): List<Entry> {
        val k = requireKey()
        return dao.getAll().map {
            Entry(
                id = it.id,
                title   = CryptoManager.decrypt(it.title, k),
                login   = CryptoManager.decrypt(it.login, k),
                password= CryptoManager.decrypt(it.password, k),
                notes   = CryptoManager.decrypt(it.notes, k)
            )
        }
    }

    suspend fun add(e: Entry) {
        val k = requireKey()
        dao.insert(
            EntryEntity(
                title    = CryptoManager.encrypt(e.title, k),
                login    = CryptoManager.encrypt(e.login, k),
                password = CryptoManager.encrypt(e.password, k),
                notes    = CryptoManager.encrypt(e.notes, k)
            )
        )
    }

    suspend fun update(e: Entry) {
        val k = requireKey()
        dao.update(
            EntryEntity(
                id = e.id,
                title    = CryptoManager.encrypt(e.title, k),
                login    = CryptoManager.encrypt(e.login, k),
                password = CryptoManager.encrypt(e.password, k),
                notes    = CryptoManager.encrypt(e.notes, k)
            )
        )
    }

    suspend fun delete(e: Entry) = dao.delete(
        EntryEntity(
            id = e.id,
            title = CryptoManager.encrypt(e.title, requireKey()),
            login = CryptoManager.encrypt(e.login, requireKey()),
            password = CryptoManager.encrypt(e.password, requireKey()),
            notes = CryptoManager.encrypt(e.notes, requireKey())
        )
    )

    suspend fun deleteAll() = dao.deleteAll()

    /** Смена мастер-ключа: перечитать всё старым ключом и перезаписать новым. */
    suspend fun rekey(newKey: SecretKey) {
        val oldKey = requireKey()
        val rows = dao.getAll()
        for (r in rows) {
            dao.update(
                r.copy(
                    title    = CryptoManager.encrypt(CryptoManager.decrypt(r.title, oldKey), newKey),
                    login    = CryptoManager.encrypt(CryptoManager.decrypt(r.login, oldKey), newKey),
                    password = CryptoManager.encrypt(CryptoManager.decrypt(r.password, oldKey), newKey),
                    notes    = CryptoManager.encrypt(CryptoManager.decrypt(r.notes, oldKey), newKey)
                )
            )
        }
        key = newKey
    }


    suspend fun insertEncrypted(rows: List<EntryEntity>) {
        rows.forEach { dao.insert(it.copy(id = 0)) }
    }

    suspend fun rawAll(): List<EntryEntity> = dao.getAll()
}