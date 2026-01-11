package com.example.lab3_other

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "entries")
data class PasswordEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val resource: String,
    val login: String,
    val password: String,
    val notes: String
)