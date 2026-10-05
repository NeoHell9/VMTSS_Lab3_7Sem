package com.example.passwordlocker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY id")
    suspend fun getAll(): List<EntryEntity>

    @Insert suspend fun insert(e: EntryEntity): Long
    @Update suspend fun update(e: EntryEntity)
    @Delete suspend fun delete(e: EntryEntity)
    @Query("DELETE FROM entries") suspend fun deleteAll()
}