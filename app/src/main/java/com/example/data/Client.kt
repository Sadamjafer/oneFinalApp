package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val name: String,
    val linkedExpenseCategory: String,
    val timestamp: Long = System.currentTimeMillis()
)
