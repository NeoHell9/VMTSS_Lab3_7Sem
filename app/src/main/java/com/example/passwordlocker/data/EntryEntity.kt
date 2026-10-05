package com.example.passwordlocker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val login: String,
    val password: String,
    val notes: String
)

data class Entry(
    val id: Long = 0,
    val title: String,
    val login: String,
    val password: String,
    val notes: String
)