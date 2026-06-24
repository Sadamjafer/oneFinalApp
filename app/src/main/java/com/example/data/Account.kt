package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // اسم المحل التجاري أو الحساب
    val timestamp: Long = System.currentTimeMillis()
)
