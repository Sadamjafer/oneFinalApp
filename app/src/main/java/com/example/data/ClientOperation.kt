package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "client_operations")
data class ClientOperation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val type: String, // "DEBT" or "PAYMENT"
    val amount: Double,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)
